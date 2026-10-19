package com.muaythai.arena.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muaythai.arena.data.prefs.SettingsStore
import com.muaythai.arena.data.repository.ClubRepository
import com.muaythai.arena.data.repository.GameRepository
import com.muaythai.arena.data.repository.ModeratorSession
import com.muaythai.arena.domain.engine.ClubEngine
import com.muaythai.arena.domain.model.ClubEvent
import com.muaythai.arena.domain.model.ClubState
import com.muaythai.arena.domain.model.EventDraft
import com.muaythai.arena.domain.model.RequestCategory
import com.muaythai.arena.domain.model.RequestStatus
import com.muaythai.arena.domain.model.RewardSummary
import com.muaythai.arena.domain.model.Survey
import com.muaythai.arena.domain.model.SurveyDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A one-line result shown under a form or a button. */
data class UiMessage(val text: String, val error: Boolean = false)

private val emptyCounts = ClubEngine.ClubCounts(0, 0, 0, 0, 0, 0, 0, 0)

data class ClubUi(
    val club: ClubState = ClubState(),
    val userName: String = "Member",
    val today: Long = 0,
    val counts: ClubEngine.ClubCounts = emptyCounts
)

/** The member's side of the Club: booking, surveys, notices and requests. */
class ClubViewModel(private val club: ClubRepository, private val game: GameRepository) : ViewModel() {
    val ui: StateFlow<ClubUi> = combine(club.state, game.state) { c, g ->
        val today = club.today()
        ClubUi(c, g.fighter?.name ?: "Member", today, ClubEngine.counts(c, today))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClubUi(club.state.value, game.state.value.fighter?.name ?: "Member", club.today(), ClubEngine.counts(club.state.value, club.today())))

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val _reward = MutableStateFlow<RewardSummary?>(null)
    val reward: StateFlow<RewardSummary?> = _reward.asStateFlow()

    /** Goes up by one each time a form is submitted successfully, so a screen can clear itself. */
    private val _submitted = MutableStateFlow(0)
    val submitted: StateFlow<Int> = _submitted.asStateFlow()

    fun clearMessage() { _message.value = null }
    fun dismissReward() { _reward.value = null }

    fun signUp(event: ClubEvent) {
        viewModelScope.launch {
            val wasFull = ClubEngine.seatsLeft(event, club.state.value.signups) <= 0
            val error = club.signUp(event.id, ui.value.userName, isUser = true)
            _message.value = when {
                error != null -> UiMessage(error, true)
                wasFull -> UiMessage("You are on the waiting list. You move up automatically if a place opens.")
                else -> UiMessage("You are booked. See you on the mats.")
            }
        }
    }

    fun cancelBooking(signupId: Long) {
        viewModelScope.launch {
            club.removeSignup(signupId)
            _message.value = UiMessage("Your booking has been cancelled.")
        }
    }

    /** Marks the member as attended and logs the class as a training session. */
    fun checkIn(event: ClubEvent) {
        viewModelScope.launch {
            val state = club.state.value
            if (!ClubEngine.canCheckIn(event, state.signups, club.today())) {
                _message.value = UiMessage("Check-in opens on the day of the event for confirmed bookings.", true)
                return@launch
            }
            val mine = ClubEngine.userSignup(event, state.signups) ?: return@launch
            club.setAttended(mine.id, true)
            _reward.value = game.commit(ClubEngine.checkInOutcome(event))
        }
    }

    /** [guestName] is null when the device owner answers, or the name of a person answering on this device. */
    fun submitSurvey(survey: Survey, answers: Map<Long, String>, guestName: String?) {
        viewModelScope.launch {
            val isUser = guestName == null
            val name = guestName ?: ui.value.userName
            val error = club.submitResponse(survey.id, name, isUser, answers)
            if (error != null) {
                _message.value = UiMessage(error, true)
                return@launch
            }
            if (isUser) _reward.value = game.commit(ClubEngine.surveyOutcome(survey))
            else _message.value = UiMessage("Thank you, ${name.trim()}. Your answers have been recorded.")
            _submitted.update { it + 1 }
        }
    }

    fun markNoticesRead() { viewModelScope.launch { club.markAnnouncementsRead() } }

    fun sendRequest(category: RequestCategory, text: String) {
        viewModelScope.launch {
            val error = club.createRequest(category, text, ui.value.userName)
            if (error != null) _message.value = UiMessage(error, true)
            else { _message.value = UiMessage("Sent. The reply will appear here."); _submitted.update { it + 1 } }
        }
    }

    fun deleteRequest(id: Long) { viewModelScope.launch { club.deleteRequest(id) } }
}

data class ModeratorUi(
    val club: ClubState = ClubState(),
    val unlocked: Boolean = false,
    val hasPin: Boolean = false,
    val autoTimetable: Boolean = true,
    val today: Long = 0,
    val counts: ClubEngine.ClubCounts = emptyCounts,
    val ready: Boolean = false
)

/** The moderator's side: PIN gate, and everything that creates or manages club content. */
class ModeratorViewModel(
    private val club: ClubRepository,
    private val settings: SettingsStore,
    private val session: ModeratorSession
) : ViewModel() {
    val ui: StateFlow<ModeratorUi> = combine(club.state, settings.settings, session.unlocked) { c, s, unlocked ->
        val today = club.today()
        ModeratorUi(c, unlocked, s.moderatorPinHash.isNotEmpty(), s.autoTimetable, today, ClubEngine.counts(c, today), ready = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ModeratorUi())

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val _saved = MutableStateFlow(0)
    val saved: StateFlow<Int> = _saved.asStateFlow()

    private var pinHash: String = ""

    init {
        viewModelScope.launch { settings.settings.collect { pinHash = it.moderatorPinHash } }
    }

    fun clearMessage() { _message.value = null }

    private fun finish(error: String?, success: String) {
        if (error != null) _message.value = UiMessage(error, true)
        else { _message.value = UiMessage(success); _saved.update { it + 1 } }
    }

    // ---- access

    fun createPin(pin: String) {
        if (!ClubEngine.isValidPin(pin)) { _message.value = UiMessage("The PIN must be ${ClubEngine.PIN_LENGTH} digits.", true); return }
        viewModelScope.launch {
            settings.setModeratorPinHash(ClubEngine.hashPin(pin))
            session.unlock()
            _message.value = UiMessage("PIN saved. Moderator panel unlocked.")
        }
    }

    fun unlock(pin: String) {
        if (pinHash.isNotEmpty() && ClubEngine.hashPin(pin) == pinHash) {
            session.unlock()
            _message.value = null
        } else _message.value = UiMessage("Wrong PIN.", true)
    }

    fun lock() { session.lock(); _message.value = null }

    fun setAutoTimetable(enabled: Boolean) {
        viewModelScope.launch {
            settings.setAutoTimetable(enabled)
            if (enabled) club.ensureContent()
            _message.value = UiMessage(if (enabled) "Regular classes will be kept two weeks ahead." else "Automatic timetable off. Existing events are kept.")
        }
    }

    // ---- events

    fun saveEvent(draft: EventDraft) { viewModelScope.launch { finish(club.saveEvent(draft), "Event saved.") } }
    fun setCancelled(eventId: Long, cancelled: Boolean) { viewModelScope.launch { club.setEventCancelled(eventId, cancelled) } }
    fun deleteEvent(eventId: Long) { viewModelScope.launch { club.deleteEvent(eventId); _saved.update { it + 1 } } }
    fun addParticipant(eventId: Long, name: String) { viewModelScope.launch { finish(club.signUp(eventId, name, isUser = false), "${name.trim()} added.") } }
    fun removeParticipant(signupId: Long) { viewModelScope.launch { club.removeSignup(signupId) } }
    fun setAttended(signupId: Long, attended: Boolean) { viewModelScope.launch { club.setAttended(signupId, attended) } }

    // ---- surveys

    fun createSurvey(draft: SurveyDraft) { viewModelScope.launch { finish(club.createSurvey(draft), "Survey published.") } }
    fun setSurveyActive(surveyId: Long, active: Boolean) { viewModelScope.launch { club.setSurveyActive(surveyId, active) } }
    fun deleteSurvey(surveyId: Long) { viewModelScope.launch { club.deleteSurvey(surveyId); _saved.update { it + 1 } } }
    fun deleteResponse(responseId: Long) { viewModelScope.launch { club.deleteResponse(responseId) } }

    // ---- notices and requests

    fun saveNotice(id: Long, title: String, body: String, pinned: Boolean) {
        viewModelScope.launch { finish(club.saveAnnouncement(id, title, body, pinned), "Notice published.") }
    }
    fun deleteNotice(id: Long) { viewModelScope.launch { club.deleteAnnouncement(id) } }

    fun updateRequest(id: Long, status: RequestStatus, reply: String) {
        viewModelScope.launch { club.updateRequest(id, status, reply); _message.value = UiMessage("Request updated."); _saved.update { it + 1 } }
    }
    fun deleteRequest(id: Long) { viewModelScope.launch { club.deleteRequest(id) } }
}
