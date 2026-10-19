package com.muaythai.arena.data.repository

import androidx.room.withTransaction
import com.muaythai.arena.data.local.ArenaDatabase
import com.muaythai.arena.data.local.ClubEventEntity
import com.muaythai.arena.data.local.ClubRequestEntity
import com.muaythai.arena.data.local.EventSignupEntity
import com.muaythai.arena.data.local.SurveyAnswerEntity
import com.muaythai.arena.data.local.SurveyEntity
import com.muaythai.arena.data.local.SurveyQuestionEntity
import com.muaythai.arena.data.local.SurveyResponseEntity
import com.muaythai.arena.data.local.AnnouncementEntity
import com.muaythai.arena.data.local.toDomain
import com.muaythai.arena.data.local.toEntity
import com.muaythai.arena.data.prefs.SettingsStore
import com.muaythai.arena.domain.engine.ClubEngine
import com.muaythai.arena.domain.model.Announcement
import com.muaythai.arena.domain.model.ClubState
import com.muaythai.arena.domain.model.EventDraft
import com.muaythai.arena.domain.model.RequestCategory
import com.muaythai.arena.domain.model.RequestStatus
import com.muaythai.arena.domain.model.Survey
import com.muaythai.arena.domain.model.SurveyDraft
import com.muaythai.arena.domain.model.SurveyResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The gym's side of the app: timetable, sign-ups, surveys, notices and requests.
 * Every write validates through [ClubEngine] first and returns an error message, or null on success.
 */
class ClubRepository(
    private val db: ArenaDatabase,
    private val settings: SettingsStore,
    scope: CoroutineScope,
    private val clock: GameClock = SystemGameClock
) {
    private val dao = db.clubDao()
    private val mutex = Mutex()

    private data class Schedule(val events: List<ClubEventEntity>, val signups: List<EventSignupEntity>)
    private data class Polls(
        val surveys: List<SurveyEntity>, val questions: List<SurveyQuestionEntity>,
        val responses: List<SurveyResponseEntity>, val answers: List<SurveyAnswerEntity>
    )
    private data class Board(val announcements: List<AnnouncementEntity>, val requests: List<ClubRequestEntity>)

    private val schedule = combine(dao.events(), dao.signups()) { e, s -> Schedule(e, s) }
    private val polls = combine(dao.surveys(), dao.questions(), dao.responses(), dao.answers()) { s, q, r, a -> Polls(s, q, r, a) }
    private val board = combine(dao.announcements(), dao.requests()) { a, r -> Board(a, r) }

    val state: StateFlow<ClubState> = combine(schedule, polls, board) { s, p, b ->
        val questions = p.questions.map { it.toDomain() }.groupBy { it.surveyId }
        val answers = p.answers.groupBy { it.responseId }
        ClubState(
            events = s.events.filter { !it.deleted }.map { it.toDomain() },
            signups = s.signups.map { it.toDomain() },
            surveys = p.surveys.map { it.toDomain(questions[it.id].orEmpty()) }.sortedBy { it.createdAt },
            responses = p.responses.map { r ->
                SurveyResponse(r.id, r.surveyId, r.respondent, r.byUser, r.createdAt, answers[r.id].orEmpty().associate { it.questionId to it.answer })
            },
            announcements = b.announcements.map { it.toDomain() },
            requests = b.requests.map { it.toDomain() },
            loaded = true
        )
    }.stateIn(scope, SharingStarted.Eagerly, ClubState())

    fun today(): Long = clock.today()

    // ------------------------------------------------------------------ first run and timetable

    /** Seeds the demo surveys, notices and special events once, and keeps the weekly timetable two weeks ahead. */
    suspend fun ensureContent() = mutex.withLock {
        val prefs = settings.settings.first()
        val today = clock.today()
        val now = clock.now()
        db.withTransaction {
            if (!prefs.clubSeeded) {
                dao.insertEvents(ClubEngine.seedSpecialEvents(today, now).map { it.toEntity() })
                for (survey in ClubEngine.seedSurveys(now)) insertSurvey(survey)
                dao.insertAnnouncements(ClubEngine.seedAnnouncements(now).map { it.toEntity() })
            }
            if (prefs.autoTimetable) {
                val existing = dao.eventsOnce().map { it.toDomain() }
                val fresh = ClubEngine.timetableTopUp(existing, today, now)
                if (fresh.isNotEmpty()) dao.insertEvents(fresh.map { it.toEntity() })
            }
        }
        if (!prefs.clubSeeded) settings.setClubSeeded(true)
    }

    private suspend fun insertSurvey(survey: Survey): Long {
        val id = dao.insertSurvey(survey.toEntity().copy(id = 0))
        dao.insertQuestions(survey.questions.map { it.toEntity(id) })
        return id
    }

    // ------------------------------------------------------------------ events and sign-ups

    suspend fun signUp(eventId: Long, name: String, isUser: Boolean): String? = mutex.withLock {
        db.withTransaction {
            val event = dao.eventById(eventId)?.toDomain() ?: return@withTransaction "This event no longer exists."
            val signups = dao.signupsForEvent(eventId).map { it.toDomain() }
            if (isUser && signups.any { it.isUser }) return@withTransaction "You are already on the list."
            ClubEngine.signupError(event, name, signups, clock.today())?.let { return@withTransaction it }
            dao.insertSignup(EventSignupEntity(eventId = eventId, name = name.trim().take(32), byUser = isUser, createdAt = clock.now(), attended = false))
            null
        }
    }

    suspend fun removeSignup(signupId: Long) = mutex.withLock { dao.deleteSignup(signupId) }

    suspend fun setAttended(signupId: Long, attended: Boolean) = mutex.withLock { dao.setAttended(signupId, attended) }

    suspend fun saveEvent(draft: EventDraft): String? = mutex.withLock {
        ClubEngine.validateEvent(draft, clock.today())?.let { return@withLock it }
        val existing = if (draft.id == 0L) null else dao.eventById(draft.id)?.toDomain()
        dao.upsertEvent(ClubEngine.toEvent(draft, existing, clock.now()).toEntity())
        null
    }

    suspend fun setEventCancelled(eventId: Long, cancelled: Boolean) = mutex.withLock { dao.setEventCancelled(eventId, cancelled) }

    suspend fun deleteEvent(eventId: Long) = mutex.withLock {
        db.withTransaction {
            dao.deleteSignupsForEvent(eventId)
            dao.softDeleteEvent(eventId)
        }
    }

    // ------------------------------------------------------------------ surveys

    suspend fun createSurvey(draft: SurveyDraft): String? = mutex.withLock {
        ClubEngine.validateSurvey(draft)?.let { return@withLock it }
        db.withTransaction { insertSurvey(ClubEngine.toSurvey(draft, clock.now())) }
        null
    }

    suspend fun setSurveyActive(surveyId: Long, active: Boolean) = mutex.withLock { dao.setSurveyActive(surveyId, active) }

    suspend fun deleteSurvey(surveyId: Long) = mutex.withLock {
        db.withTransaction {
            dao.deleteAnswersForSurvey(surveyId)
            dao.deleteResponsesForSurvey(surveyId)
            dao.deleteQuestions(surveyId)
            dao.deleteSurvey(surveyId)
        }
    }

    suspend fun submitResponse(surveyId: Long, respondent: String, isUser: Boolean, answers: Map<Long, String>): String? = mutex.withLock {
        db.withTransaction {
            val entity = dao.surveyById(surveyId) ?: return@withTransaction "This survey no longer exists."
            val survey = entity.toDomain(dao.questionsForSurvey(surveyId).map { it.toDomain() })
            val previous = dao.responsesForSurvey(surveyId).map { SurveyResponse(it.id, it.surveyId, it.respondent, it.byUser, it.createdAt) }
            ClubEngine.responseError(survey, respondent, isUser, previous)?.let { return@withTransaction it }
            ClubEngine.validateAnswers(survey, answers)?.let { return@withTransaction it }
            val responseId = dao.insertResponse(
                SurveyResponseEntity(surveyId = surveyId, respondent = respondent.trim().take(32), byUser = isUser, createdAt = clock.now())
            )
            val valid = survey.questions.filter { ClubEngine.isAnswered(it, answers[it.id]) }
            dao.insertAnswers(valid.map { SurveyAnswerEntity(responseId = responseId, questionId = it.id, answer = answers.getValue(it.id).trim().take(500)) })
            null
        }
    }

    suspend fun deleteResponse(responseId: Long) = mutex.withLock {
        db.withTransaction {
            dao.deleteAnswersForResponse(responseId)
            dao.deleteResponse(responseId)
        }
    }

    // ------------------------------------------------------------------ notice board

    suspend fun saveAnnouncement(id: Long, title: String, body: String, pinned: Boolean): String? = mutex.withLock {
        val cleanTitle = title.trim().take(60)
        val cleanBody = body.trim().take(800)
        if (cleanTitle.length < 3) return@withLock "Give the notice a title of at least 3 characters."
        if (cleanBody.isEmpty()) return@withLock "Write the notice text."
        val existing = if (id == 0L) null else dao.announcementById(id)
        // An edited notice becomes unread again so members see the change.
        dao.upsertAnnouncement(
            Announcement(id = existing?.id ?: 0, title = cleanTitle, body = cleanBody, pinned = pinned, read = false, createdAt = existing?.createdAt ?: clock.now()).toEntity()
        )
        null
    }

    suspend fun deleteAnnouncement(id: Long) = mutex.withLock { dao.deleteAnnouncement(id) }

    suspend fun markAnnouncementsRead() = mutex.withLock { dao.markAllAnnouncementsRead() }

    // ------------------------------------------------------------------ requests

    suspend fun createRequest(category: RequestCategory, message: String, author: String): String? = mutex.withLock {
        val clean = message.trim().take(500)
        if (clean.length < 5) return@withLock "Describe your request in a few words."
        val now = clock.now()
        dao.insertRequest(
            ClubRequestEntity(category = category.name, message = clean, author = author.trim().take(32).ifBlank { "Member" },
                status = RequestStatus.NEW.name, reply = "", createdAt = now, updatedAt = now)
        )
        null
    }

    suspend fun updateRequest(id: Long, status: RequestStatus, reply: String) = mutex.withLock {
        dao.updateRequest(id, status.name, reply.trim().take(500), clock.now())
    }

    suspend fun deleteRequest(id: Long) = mutex.withLock { dao.deleteRequest(id) }
}

/** Whether the moderator panel is unlocked. Held in memory only, so it locks itself when the app is closed. */
class ModeratorSession {
    private val _unlocked = kotlinx.coroutines.flow.MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked
    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }
}
