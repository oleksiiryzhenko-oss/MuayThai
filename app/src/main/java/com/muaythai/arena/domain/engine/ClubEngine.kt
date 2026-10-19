package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.model.Announcement
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.ClubEvent
import com.muaythai.arena.domain.model.ClubRequest
import com.muaythai.arena.domain.model.ClubState
import com.muaythai.arena.domain.model.EventDraft
import com.muaythai.arena.domain.model.EventSignup
import com.muaythai.arena.domain.model.EventType
import com.muaythai.arena.domain.model.QuestionDraft
import com.muaythai.arena.domain.model.QuestionSummary
import com.muaythai.arena.domain.model.QuestionType
import com.muaythai.arena.domain.model.RequestStatus
import com.muaythai.arena.domain.model.SessionOutcome
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.SignupStatus
import com.muaythai.arena.domain.model.Survey
import com.muaythai.arena.domain.model.SurveyDraft
import com.muaythai.arena.domain.model.SurveyQuestion
import com.muaythai.arena.domain.model.SurveyResponse
import java.security.MessageDigest

/** Rules for the Club area. Pure Kotlin: no storage, no Android. */
object ClubEngine {
    const val MAX_CAPACITY = 200
    const val MAX_QUESTIONS = 12
    const val MAX_OPTIONS = 8
    const val TIMETABLE_DAYS = 14
    const val PIN_LENGTH = 4

    // ------------------------------------------------------------------ time

    /** 0 = Monday .. 6 = Sunday. Epoch day 0 was a Thursday. */
    fun weekday(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt()

    fun formatTime(startMinute: Int): String {
        val m = startMinute.coerceIn(0, 24 * 60 - 1)
        return "${(m / 60).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}"
    }

    fun timeRange(event: ClubEvent): String =
        "${formatTime(event.startMinute)} - ${formatTime((event.startMinute + event.durationMin).coerceAtMost(24 * 60 - 1))}"

    // ------------------------------------------------------------------ events and sign-ups

    fun signupsFor(eventId: Long, signups: List<EventSignup>): List<EventSignup> =
        signups.filter { it.eventId == eventId }.sortedWith(compareBy<EventSignup> { it.createdAt }.thenBy { it.id })

    /** The first [ClubEvent.capacity] people to sign up hold a place. */
    fun confirmed(event: ClubEvent, signups: List<EventSignup>): List<EventSignup> =
        signupsFor(event.id, signups).take(event.capacity)

    /** Everyone after that waits, and moves up automatically when someone ahead of them cancels. */
    fun waitlist(event: ClubEvent, signups: List<EventSignup>): List<EventSignup> =
        signupsFor(event.id, signups).drop(event.capacity)

    fun seatsLeft(event: ClubEvent, signups: List<EventSignup>): Int =
        (event.capacity - signupsFor(event.id, signups).size).coerceAtLeast(0)

    fun userSignup(event: ClubEvent, signups: List<EventSignup>): EventSignup? =
        signupsFor(event.id, signups).firstOrNull { it.isUser }

    fun statusOf(signup: EventSignup?, event: ClubEvent, signups: List<EventSignup>): SignupStatus = when {
        signup == null -> SignupStatus.NONE
        confirmed(event, signups).any { it.id == signup.id } -> SignupStatus.CONFIRMED
        else -> SignupStatus.WAITLISTED
    }

    fun userStatus(event: ClubEvent, signups: List<EventSignup>): SignupStatus =
        statusOf(userSignup(event, signups), event, signups)

    fun waitlistPosition(signup: EventSignup, event: ClubEvent, signups: List<EventSignup>): Int =
        waitlist(event, signups).indexOfFirst { it.id == signup.id } + 1

    fun isPast(event: ClubEvent, today: Long): Boolean = event.epochDay < today

    fun upcoming(events: List<ClubEvent>, today: Long): List<ClubEvent> =
        events.filter { !it.deleted && it.epochDay >= today }.sortedWith(compareBy<ClubEvent> { it.epochDay }.thenBy { it.startMinute })

    fun past(events: List<ClubEvent>, today: Long): List<ClubEvent> =
        events.filter { !it.deleted && it.epochDay < today }.sortedWith(compareByDescending<ClubEvent> { it.epochDay }.thenByDescending { it.startMinute })

    /** Why a person cannot sign up, or null when they can. */
    fun signupError(event: ClubEvent, name: String, signups: List<EventSignup>, today: Long): String? {
        val clean = name.trim()
        return when {
            clean.isEmpty() -> "Enter a name."
            event.cancelled -> "This event has been cancelled."
            isPast(event, today) -> "This event has already taken place."
            signupsFor(event.id, signups).any { it.name.equals(clean, ignoreCase = true) } -> "$clean is already on the list."
            else -> null
        }
    }

    fun canCheckIn(event: ClubEvent, signups: List<EventSignup>, today: Long): Boolean {
        val mine = userSignup(event, signups) ?: return false
        return event.epochDay == today && !event.cancelled && !mine.attended && statusOf(mine, event, signups) == SignupStatus.CONFIRMED
    }

    fun validateEvent(draft: EventDraft, today: Long): String? = when {
        draft.title.trim().length < 3 -> "Give the event a title of at least 3 characters."
        draft.epochDay < today -> "The date is in the past."
        draft.startMinute !in 0 until 24 * 60 -> "Pick a valid start time."
        draft.durationMin !in 15..600 -> "Duration must be between 15 minutes and 10 hours."
        draft.capacity !in 1..MAX_CAPACITY -> "Capacity must be between 1 and $MAX_CAPACITY."
        else -> null
    }

    fun toEvent(draft: EventDraft, existing: ClubEvent?, now: Long): ClubEvent = ClubEvent(
        id = draft.id, title = draft.title.trim().take(48), type = draft.type, epochDay = draft.epochDay,
        startMinute = draft.startMinute, durationMin = draft.durationMin, coach = draft.coach.trim().take(32),
        location = draft.location.trim().take(32), level = draft.level.trim().take(24).ifBlank { "All levels" },
        capacity = draft.capacity, description = draft.description.trim().take(400),
        cancelled = existing?.cancelled ?: false, deleted = false, createdAt = existing?.createdAt ?: now
    )

    fun toDraft(event: ClubEvent): EventDraft = EventDraft(
        event.id, event.title, event.type, event.epochDay, event.startMinute, event.durationMin, event.coach,
        event.location, event.level, event.capacity, event.description
    )

    /** Attending a booked event counts as training. */
    fun checkInOutcome(event: ClubEvent): SessionOutcome {
        val minutes = event.durationMin
        val gains: Map<Attribute, Int> = when (event.type) {
            EventType.CLASS -> mapOf(Attribute.CONDITIONING to minutes, Attribute.STRIKING to minutes / 2, Attribute.STAMINA to minutes / 2)
            EventType.PADS -> mapOf(Attribute.STRIKING to minutes, Attribute.KICKING to minutes / 2, Attribute.CLINCH to minutes / 2)
            EventType.SPARRING -> mapOf(Attribute.DEFENSE to minutes, Attribute.FIGHT_IQ to minutes / 2, Attribute.REACTION to minutes / 2)
            EventType.SEMINAR -> mapOf(Attribute.FIGHT_IQ to minutes, Attribute.KICKING to minutes / 3)
            EventType.GRADING -> mapOf(Attribute.FIGHT_IQ to minutes / 2, Attribute.STRIKING to minutes / 2, Attribute.DEFENSE to minutes / 2)
            EventType.FIGHT_NIGHT -> mapOf(Attribute.FIGHT_IQ to minutes / 2, Attribute.STAMINA to minutes / 3)
            EventType.OPEN_MAT -> mapOf(Attribute.CONDITIONING to minutes / 2, Attribute.SPEED to minutes / 3)
        }
        return SessionOutcome(
            type = SessionType.CLUB, title = "Attended: ${event.title}", durationSec = minutes * 60,
            xp = 80 + minutes, performance = 100, gains = gains, tag = event.type.name, success = true
        )
    }

    // ------------------------------------------------------------------ automatic weekly timetable

    private data class Slot(
        val days: Set<Int>, val startMinute: Int, val title: String, val type: EventType, val durationMin: Int,
        val coach: String, val location: String, val level: String, val capacity: Int, val description: String
    )

    private val weeklySlots = listOf(
        Slot(setOf(0, 3), 7 * 60, "Morning Conditioning", EventType.CLASS, 45, "Coach Mara", "Bag room", "All levels", 20,
            "Skipping, bag rounds and bodyweight circuits before work. Bring a towel and water."),
        Slot(setOf(0, 2, 4), 18 * 60 + 30, "Muay Thai Fundamentals", EventType.CLASS, 60, "Kru Niran", "Main ring", "Beginner", 16,
            "Stance, guard, the basic strikes and the first checks. The right class if you are new."),
        Slot(setOf(1, 3), 19 * 60, "Pad Work & Clinch", EventType.PADS, 75, "Coach Dev", "Mat area", "Intermediate", 12,
            "Partner pad rounds followed by thirty minutes of clinch position and knees. Shin guards optional."),
        Slot(setOf(5), 11 * 60, "Open Sparring", EventType.SPARRING, 90, "Kru Niran", "Main ring", "Intermediate+", 10,
            "Controlled technical sparring. 16 oz gloves, shin guards and a mouthguard are required."),
        Slot(setOf(6), 10 * 60, "Open Mat", EventType.OPEN_MAT, 120, "Self-directed", "Mat area", "All levels", 24,
            "The gym is open for your own work: drilling, bags, stretching. A coach is on the floor for questions.")
    )

    private fun slotEvent(slot: Slot, day: Long, now: Long) = ClubEvent(
        title = slot.title, type = slot.type, epochDay = day, startMinute = slot.startMinute, durationMin = slot.durationMin,
        coach = slot.coach, location = slot.location, level = slot.level, capacity = slot.capacity,
        description = slot.description, createdAt = now
    )

    /**
     * The regular classes for the next two weeks that do not exist yet. [existing] must include cancelled and
     * soft-deleted events, so that something the moderator removed is not created again.
     */
    fun timetableTopUp(existing: List<ClubEvent>, today: Long, now: Long, days: Int = TIMETABLE_DAYS): List<ClubEvent> {
        val known = existing.map { Triple(it.epochDay, it.startMinute, it.title) }.toSet()
        val out = ArrayList<ClubEvent>()
        for (offset in 0 until days) {
            val day = today + offset
            for (slot in weeklySlots) {
                if (weekday(day) !in slot.days) continue
                if (Triple(day, slot.startMinute, slot.title) in known) continue
                out += slotEvent(slot, day, now)
            }
        }
        return out
    }

    // ------------------------------------------------------------------ first-run content

    fun seedSpecialEvents(today: Long, now: Long): List<ClubEvent> = listOf(
        ClubEvent(title = "Low Kick Masterclass", type = EventType.SEMINAR, epochDay = today + 5, startMinute = 14 * 60, durationMin = 120,
            coach = "Kru Niran", location = "Main ring", level = "All levels", capacity = 20,
            description = "Two hours on setting up, landing and defending the low kick: angles, timing and the check.", createdAt = now),
        ClubEvent(title = "Armband Grading", type = EventType.GRADING, epochDay = today + 9, startMinute = 12 * 60, durationMin = 90,
            coach = "Coach Mara", location = "Mat area", level = "By invitation", capacity = 8,
            description = "Technique assessment for the next armband. Speak to your coach before signing up.", createdAt = now),
        ClubEvent(title = "Interclub Fight Night", type = EventType.FIGHT_NIGHT, epochDay = today + 12, startMinute = 18 * 60, durationMin = 180,
            coach = "All coaches", location = "Main ring", level = "Fighters and supporters", capacity = 40,
            description = "Friendly interclub bouts. Sign up to compete or to help in the corner. Weigh-in is one hour before.", createdAt = now)
    )

    private fun q(position: Int, text: String, type: QuestionType, vararg options: String, required: Boolean = true) =
        SurveyQuestion(position = position, text = text, type = type, options = options.toList(), required = required)

    fun seedSurveys(now: Long): List<Survey> = listOf(
        Survey(title = "Your training goals", description = "Help the coaches plan sessions around what members actually want.", rewardXp = 100, createdAt = now, questions = listOf(
            q(0, "What is your main goal right now?", QuestionType.SINGLE, "Fitness and conditioning", "Learn solid technique", "Compete in a fight", "Self-defence and confidence"),
            q(1, "How many times a week can you realistically train?", QuestionType.SINGLE, "Once", "Twice", "Three times", "Four or more"),
            q(2, "Which areas do you want more of?", QuestionType.MULTI, "Pad work", "Clinch", "Sparring", "Conditioning", "Defence drills"),
            q(3, "How confident do you feel in sparring?", QuestionType.RATING),
            q(4, "Anything the coaches should know about you?", QuestionType.TEXT, required = false)
        )),
        Survey(title = "Class feedback", description = "Two minutes, honest answers. It goes straight to the coaching team.", rewardXp = 80, createdAt = now + 1, questions = listOf(
            q(0, "How would you rate the quality of coaching?", QuestionType.RATING),
            q(1, "How was the pace of your last class?", QuestionType.SINGLE, "Too easy", "About right", "Too hard"),
            q(2, "How clean and well equipped is the gym?", QuestionType.RATING),
            q(3, "What should we improve first?", QuestionType.TEXT, required = false)
        )),
        Survey(title = "Timetable preferences", description = "We are reviewing the timetable. Tell us when you want to train.", rewardXp = 80, createdAt = now + 2, questions = listOf(
            q(0, "Which times suit you?", QuestionType.MULTI, "Early morning", "Lunchtime", "Early evening", "Late evening", "Weekend morning"),
            q(1, "Which new class would you join?", QuestionType.SINGLE, "Women-only class", "Kids and teens", "Fighters' sparring", "Strength for fighters", "None of these"),
            q(2, "Would you come to a monthly seminar?", QuestionType.SINGLE, "Yes, every month", "Sometimes", "Probably not")
        )),
        Survey(title = "Weekly wellness check", description = "A quick check so training load can be adjusted before small problems grow.", rewardXp = 60, createdAt = now + 3, questions = listOf(
            q(0, "How recovered do you feel today?", QuestionType.RATING),
            q(1, "How well have you slept this week?", QuestionType.RATING),
            q(2, "Any soreness or injury?", QuestionType.MULTI, "None", "Shins", "Knees", "Shoulders", "Wrists or hands", "Back"),
            q(3, "Details the coach should know", QuestionType.TEXT, required = false)
        ))
    )

    fun seedAnnouncements(now: Long): List<Announcement> = listOf(
        Announcement(title = "Welcome to the club board", pinned = true, createdAt = now,
            body = "This is where the gym posts news. Use Schedule to book classes, Surveys to tell the coaches what you think, and Requests to ask for a private lesson or anything else."),
        Announcement(title = "Sparring rules", createdAt = now - 60_000,
            body = "Open Sparring is technical, not a fight. 16 oz gloves, shin guards and a mouthguard are mandatory. Match your partner's pace and look after each other."),
        Announcement(title = "Bring your own wraps", createdAt = now - 120_000,
            body = "For hygiene, hand wraps are no longer lent out at the desk. A pair is available to buy if you forget yours.")
    )

    // ------------------------------------------------------------------ surveys

    fun encodeMulti(indices: Collection<Int>): String = indices.sorted().joinToString("|")

    fun decodeMulti(raw: String): Set<Int> = raw.split('|').mapNotNull { it.toIntOrNull() }.toSet()

    fun isAnswered(question: SurveyQuestion, raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return when (question.type) {
            QuestionType.SINGLE -> raw.toIntOrNull()?.let { it in question.options.indices } == true
            QuestionType.MULTI -> decodeMulti(raw).let { set -> set.isNotEmpty() && set.all { it in question.options.indices } }
            QuestionType.RATING -> raw.toIntOrNull()?.let { it in 1..5 } == true
            QuestionType.TEXT -> true
        }
    }

    /** The first problem with a set of answers, or null if the survey can be submitted. */
    fun validateAnswers(survey: Survey, answers: Map<Long, String>): String? {
        if (!survey.active) return "This survey is closed."
        for (question in survey.questions.sortedBy { it.position }) {
            if (question.required && !isAnswered(question, answers[question.id])) {
                return "Please answer question ${question.position + 1}: ${question.text}"
            }
        }
        return null
    }

    fun userResponse(surveyId: Long, responses: List<SurveyResponse>): SurveyResponse? =
        responses.firstOrNull { it.surveyId == surveyId && it.isUser }

    fun responseError(survey: Survey, respondent: String, isUser: Boolean, responses: List<SurveyResponse>): String? {
        val clean = respondent.trim()
        val mine = responses.filter { it.surveyId == survey.id }
        return when {
            clean.isEmpty() -> "Enter the name of the person answering."
            isUser && mine.any { it.isUser } -> "You have already completed this survey."
            !isUser && mine.any { !it.isUser && it.respondent.equals(clean, ignoreCase = true) } -> "$clean has already answered this survey."
            else -> null
        }
    }

    fun summarize(survey: Survey, responses: List<SurveyResponse>): List<QuestionSummary> {
        val mine = responses.filter { it.surveyId == survey.id }
        return survey.questions.sortedBy { it.position }.map { question ->
            val raw = mine.mapNotNull { it.answers[question.id] }.filter { isAnswered(question, it) }
            when (question.type) {
                QuestionType.SINGLE -> {
                    val picks = raw.mapNotNull { it.toIntOrNull() }
                    QuestionSummary(question, raw.size, question.options.indices.map { i -> picks.count { it == i } }, 0.0, emptyList())
                }
                QuestionType.MULTI -> {
                    val sets = raw.map { decodeMulti(it) }
                    QuestionSummary(question, raw.size, question.options.indices.map { i -> sets.count { i in it } }, 0.0, emptyList())
                }
                QuestionType.RATING -> {
                    val stars = raw.mapNotNull { it.toIntOrNull() }
                    QuestionSummary(question, raw.size, (1..5).map { s -> stars.count { it == s } }, if (stars.isEmpty()) 0.0 else stars.average(), emptyList())
                }
                QuestionType.TEXT -> QuestionSummary(question, raw.size, emptyList(), 0.0, raw.map { it.trim() })
            }
        }
    }

    /** The answer as a person would read it. */
    fun describeAnswer(question: SurveyQuestion, raw: String?): String {
        if (!isAnswered(question, raw) || raw == null) return "No answer"
        return when (question.type) {
            QuestionType.SINGLE -> question.options.getOrNull(raw.toIntOrNull() ?: -1) ?: "No answer"
            QuestionType.MULTI -> decodeMulti(raw).sorted().mapNotNull { question.options.getOrNull(it) }.joinToString(", ")
            QuestionType.RATING -> "$raw out of 5"
            QuestionType.TEXT -> raw.trim()
        }
    }

    fun validateSurvey(draft: SurveyDraft): String? {
        if (draft.title.trim().length < 3) return "Give the survey a title of at least 3 characters."
        if (draft.questions.isEmpty()) return "Add at least one question."
        if (draft.questions.size > MAX_QUESTIONS) return "A survey can have at most $MAX_QUESTIONS questions."
        draft.questions.forEachIndexed { index, question ->
            if (question.text.trim().length < 3) return "Question ${index + 1} needs its text."
            if (question.type == QuestionType.SINGLE || question.type == QuestionType.MULTI) {
                val options = cleanOptions(question)
                if (options.size < 2) return "Question ${index + 1} needs at least two different options."
            }
        }
        return null
    }

    private fun cleanOptions(question: QuestionDraft): List<String> =
        question.options.map { it.trim().replace('\n', ' ').take(60) }.filter { it.isNotEmpty() }.distinct().take(MAX_OPTIONS)

    fun toSurvey(draft: SurveyDraft, now: Long): Survey = Survey(
        title = draft.title.trim().take(60), description = draft.description.trim().take(240), active = true,
        rewardXp = draft.rewardXp.coerceIn(0, 300), createdAt = now,
        questions = draft.questions.mapIndexed { index, question ->
            val choice = question.type == QuestionType.SINGLE || question.type == QuestionType.MULTI
            SurveyQuestion(
                position = index, text = question.text.trim().take(160), type = question.type,
                options = if (choice) cleanOptions(question) else emptyList(), required = question.required
            )
        }
    )

    fun surveyOutcome(survey: Survey): SessionOutcome = SessionOutcome(
        type = SessionType.CLUB, title = "Survey: ${survey.title}", durationSec = 60 + survey.questions.size * 15,
        xp = survey.rewardXp, performance = 100, gains = mapOf(Attribute.FIGHT_IQ to 20), tag = "survey", success = true
    )

    // ------------------------------------------------------------------ dashboard numbers

    data class ClubCounts(
        val upcomingEvents: Int, val myBookings: Int, val openSurveys: Int, val unreadNotices: Int,
        val openRequests: Int, val totalSignups: Int, val totalResponses: Int, val newRequests: Int
    )

    fun counts(state: ClubState, today: Long): ClubCounts {
        val upcoming = upcoming(state.events, today).filter { !it.cancelled }
        return ClubCounts(
            upcomingEvents = upcoming.size,
            myBookings = upcoming.count { userSignup(it, state.signups) != null },
            openSurveys = state.surveys.count { it.active && userResponse(it.id, state.responses) == null },
            unreadNotices = state.announcements.count { !it.read },
            openRequests = state.requests.count { it.status == RequestStatus.NEW || it.status == RequestStatus.IN_PROGRESS },
            totalSignups = upcoming.sumOf { signupsFor(it.id, state.signups).size },
            totalResponses = state.responses.size,
            newRequests = state.requests.count { it.status == RequestStatus.NEW }
        )
    }

    fun sortedAnnouncements(list: List<Announcement>): List<Announcement> =
        list.sortedWith(compareByDescending<Announcement> { it.pinned }.thenByDescending { it.createdAt })

    fun sortedRequests(list: List<ClubRequest>): List<ClubRequest> =
        list.sortedWith(compareBy<ClubRequest> { it.status.ordinal }.thenByDescending { it.createdAt })

    // ------------------------------------------------------------------ moderator PIN

    fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    /** The PIN is never stored, only this hash. It keeps casual users out of the moderator panel; it is not a security boundary. */
    fun hashPin(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("muaythai-arena-moderator:$pin".toByteArray())
            .joinToString("") { ((it.toInt() and 0xff) + 0x100).toString(16).substring(1) }

    // ------------------------------------------------------------------ plain-text exports

    fun exportEvent(event: ClubEvent, signups: List<EventSignup>, dateLabel: String): String = buildString {
        appendLine(event.title.uppercase())
        appendLine("$dateLabel, ${timeRange(event)} · ${event.type.label}")
        if (event.coach.isNotBlank()) appendLine("Coach: ${event.coach}")
        if (event.location.isNotBlank()) appendLine("Location: ${event.location}")
        if (event.cancelled) appendLine("STATUS: CANCELLED")
        val booked = confirmed(event, signups)
        val waiting = waitlist(event, signups)
        appendLine()
        appendLine("BOOKED ${booked.size}/${event.capacity}")
        booked.forEachIndexed { i, s -> appendLine("${i + 1}. ${s.name}${if (s.attended) "  [attended]" else ""}") }
        if (waiting.isNotEmpty()) {
            appendLine()
            appendLine("WAITING LIST ${waiting.size}")
            waiting.forEachIndexed { i, s -> appendLine("${i + 1}. ${s.name}") }
        }
    }.trimEnd()

    fun exportSurvey(survey: Survey, responses: List<SurveyResponse>): String = buildString {
        val mine = responses.filter { it.surveyId == survey.id }
        appendLine(survey.title.uppercase())
        appendLine("${mine.size} response(s)")
        for (summary in summarize(survey, responses)) {
            val question = summary.question
            appendLine()
            appendLine("${question.position + 1}. ${question.text}  (${summary.answered} answered)")
            when (question.type) {
                QuestionType.SINGLE, QuestionType.MULTI -> question.options.forEachIndexed { i, option ->
                    val count = summary.counts.getOrElse(i) { 0 }
                    val share = if (summary.answered == 0) 0 else count * 100 / summary.answered
                    appendLine("   $option: $count ($share%)")
                }
                QuestionType.RATING -> {
                    appendLine("   Average: ${"%.1f".format(java.util.Locale.US, summary.average)} / 5")
                    summary.counts.forEachIndexed { i, count -> appendLine("   ${i + 1} star: $count") }
                }
                QuestionType.TEXT -> summary.texts.forEach { appendLine("   - $it") }
            }
        }
    }.trimEnd()
}
