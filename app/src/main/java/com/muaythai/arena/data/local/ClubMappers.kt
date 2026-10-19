package com.muaythai.arena.data.local

import com.muaythai.arena.domain.model.Announcement
import com.muaythai.arena.domain.model.ClubEvent
import com.muaythai.arena.domain.model.ClubRequest
import com.muaythai.arena.domain.model.EventSignup
import com.muaythai.arena.domain.model.EventType
import com.muaythai.arena.domain.model.QuestionType
import com.muaythai.arena.domain.model.RequestCategory
import com.muaythai.arena.domain.model.RequestStatus
import com.muaythai.arena.domain.model.Survey
import com.muaythai.arena.domain.model.SurveyQuestion

fun ClubEventEntity.toDomain(): ClubEvent = ClubEvent(
    id = id, title = title, type = EventType.entries.firstOrNull { it.name == type } ?: EventType.CLASS, epochDay = epochDay,
    startMinute = startMinute, durationMin = durationMin, coach = coach, location = location, level = level,
    capacity = capacity, description = description, cancelled = cancelled, deleted = deleted, createdAt = createdAt
)

fun ClubEvent.toEntity(): ClubEventEntity = ClubEventEntity(
    id = id, title = title, type = type.name, epochDay = epochDay, startMinute = startMinute, durationMin = durationMin,
    coach = coach, location = location, level = level, capacity = capacity, description = description,
    cancelled = cancelled, deleted = deleted, createdAt = createdAt
)

fun EventSignupEntity.toDomain(): EventSignup = EventSignup(id, eventId, name, byUser, createdAt, attended)

fun SurveyQuestionEntity.toDomain(): SurveyQuestion = SurveyQuestion(
    id = id, surveyId = surveyId, position = position, text = text,
    type = QuestionType.entries.firstOrNull { it.name == type } ?: QuestionType.TEXT,
    options = if (options.isEmpty()) emptyList() else options.split('\n'), required = required
)

fun SurveyQuestion.toEntity(surveyId: Long): SurveyQuestionEntity = SurveyQuestionEntity(
    id = 0, surveyId = surveyId, position = position, text = text, type = type.name,
    options = options.joinToString("\n") { it.replace('\n', ' ') }, required = required
)

fun SurveyEntity.toDomain(questions: List<SurveyQuestion>): Survey =
    Survey(id, title, description, active, rewardXp, createdAt, questions.sortedBy { it.position })

fun Survey.toEntity(): SurveyEntity = SurveyEntity(id, title, description, active, rewardXp, createdAt)

fun AnnouncementEntity.toDomain(): Announcement = Announcement(id, title, body, pinned, seen, createdAt)

fun Announcement.toEntity(): AnnouncementEntity = AnnouncementEntity(id, title, body, pinned, read, createdAt)

fun ClubRequestEntity.toDomain(): ClubRequest = ClubRequest(
    id = id, category = RequestCategory.entries.firstOrNull { it.name == category } ?: RequestCategory.OTHER,
    message = message, author = author, status = RequestStatus.entries.firstOrNull { it.name == status } ?: RequestStatus.NEW,
    reply = reply, createdAt = createdAt, updatedAt = updatedAt
)
