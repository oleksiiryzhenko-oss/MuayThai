package com.muaythai.arena.domain.model

/** Everything in the Club area: the gym's timetable, sign-ups, surveys, notices and requests to the coach. */

enum class EventType(val label: String) {
    CLASS("Group class"),
    PADS("Pad work"),
    SPARRING("Sparring"),
    SEMINAR("Seminar"),
    GRADING("Grading"),
    FIGHT_NIGHT("Fight night"),
    OPEN_MAT("Open mat")
}

data class ClubEvent(
    val id: Long = 0,
    val title: String,
    val type: EventType,
    val epochDay: Long,
    /** Minutes after midnight. */
    val startMinute: Int,
    val durationMin: Int,
    val coach: String,
    val location: String,
    val level: String,
    val capacity: Int,
    val description: String,
    val cancelled: Boolean = false,
    /** Soft delete: hidden everywhere, but remembered so the automatic timetable does not bring it back. */
    val deleted: Boolean = false,
    val createdAt: Long = 0
)

data class EventSignup(
    val id: Long = 0,
    val eventId: Long,
    val name: String,
    /** True for the owner of this device, false for a person added by the moderator. */
    val isUser: Boolean,
    val createdAt: Long,
    val attended: Boolean = false
)

enum class SignupStatus(val label: String) { NONE("Not booked"), CONFIRMED("Booked"), WAITLISTED("On the waiting list") }

enum class QuestionType(val label: String) {
    SINGLE("Single choice"),
    MULTI("Multiple choice"),
    RATING("Rating 1 to 5"),
    TEXT("Written answer")
}

data class SurveyQuestion(
    val id: Long = 0,
    val surveyId: Long = 0,
    val position: Int,
    val text: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val required: Boolean = true
)

data class Survey(
    val id: Long = 0,
    val title: String,
    val description: String,
    val active: Boolean = true,
    val rewardXp: Int = 80,
    val createdAt: Long = 0,
    val questions: List<SurveyQuestion> = emptyList()
)

/**
 * One completed survey. Answers are keyed by question id and encoded per type:
 * SINGLE = option index, MULTI = indices joined with '|', RATING = 1..5, TEXT = the text itself.
 */
data class SurveyResponse(
    val id: Long = 0,
    val surveyId: Long,
    val respondent: String,
    val isUser: Boolean,
    val createdAt: Long,
    val answers: Map<Long, String> = emptyMap()
)

data class Announcement(
    val id: Long = 0,
    val title: String,
    val body: String,
    val pinned: Boolean = false,
    val read: Boolean = false,
    val createdAt: Long = 0
)

enum class RequestCategory(val label: String) {
    PRIVATE_LESSON("Private lesson"),
    SCHEDULE("Schedule"),
    QUESTION("Question for the coach"),
    EQUIPMENT("Equipment"),
    OTHER("Other")
}

enum class RequestStatus(val label: String) { NEW("New"), IN_PROGRESS("In progress"), DONE("Done"), DECLINED("Declined") }

data class ClubRequest(
    val id: Long = 0,
    val category: RequestCategory,
    val message: String,
    val author: String,
    val status: RequestStatus = RequestStatus.NEW,
    val reply: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class ClubState(
    val events: List<ClubEvent> = emptyList(),
    val signups: List<EventSignup> = emptyList(),
    val surveys: List<Survey> = emptyList(),
    val responses: List<SurveyResponse> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val requests: List<ClubRequest> = emptyList(),
    val loaded: Boolean = false
)

/** Aggregated answers to one survey question. */
data class QuestionSummary(
    val question: SurveyQuestion,
    val answered: Int,
    /** One count per option (SINGLE, MULTI) or per star 1..5 (RATING). Empty for TEXT. */
    val counts: List<Int>,
    val average: Double,
    val texts: List<String>
)

data class EventDraft(
    val id: Long = 0,
    val title: String = "",
    val type: EventType = EventType.CLASS,
    val epochDay: Long = 0,
    val startMinute: Int = 18 * 60,
    val durationMin: Int = 60,
    val coach: String = "",
    val location: String = "",
    val level: String = "All levels",
    val capacity: Int = 12,
    val description: String = ""
)

data class QuestionDraft(
    val text: String = "",
    val type: QuestionType = QuestionType.SINGLE,
    val options: List<String> = listOf("", ""),
    val required: Boolean = true
)

data class SurveyDraft(
    val title: String = "",
    val description: String = "",
    val rewardXp: Int = 80,
    val questions: List<QuestionDraft> = listOf(QuestionDraft())
)
