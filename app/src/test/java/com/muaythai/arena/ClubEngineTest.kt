package com.muaythai.arena

import com.muaythai.arena.domain.engine.ClubEngine
import com.muaythai.arena.domain.engine.GameReducer
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.ClubEvent
import com.muaythai.arena.domain.model.ClubState
import com.muaythai.arena.domain.model.EventDraft
import com.muaythai.arena.domain.model.EventSignup
import com.muaythai.arena.domain.model.EventType
import com.muaythai.arena.domain.model.QuestionDraft
import com.muaythai.arena.domain.model.QuestionType
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.SignupStatus
import com.muaythai.arena.domain.model.Survey
import com.muaythai.arena.domain.model.SurveyDraft
import com.muaythai.arena.domain.model.SurveyResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClubEngineTest {
    // 2022-01-03, a Monday
    private val monday = 18_995L
    private val now = 1_700_000_000_000L

    private fun event(capacity: Int = 2, day: Long = monday + 1) = ClubEvent(
        id = 7, title = "Pad Work", type = EventType.PADS, epochDay = day, startMinute = 19 * 60, durationMin = 75,
        coach = "Coach Dev", location = "Mat area", level = "Intermediate", capacity = capacity, description = ""
    )

    private fun signup(id: Long, name: String, user: Boolean = false, at: Long = id) = EventSignup(id, 7, name, user, at)

    /** Gives a seeded survey real ids, the way the database would. */
    private fun withIds(survey: Survey, id: Long = 3): Survey =
        survey.copy(id = id, questions = survey.questions.mapIndexed { i, q -> q.copy(id = id * 100 + i, surveyId = id) })

    @Test fun weekdaysStartOnMonday() {
        assertEquals(0, ClubEngine.weekday(monday))
        assertEquals(6, ClubEngine.weekday(monday + 6))
        assertEquals(0, ClubEngine.weekday(monday + 7))
        assertEquals("07:05", ClubEngine.formatTime(7 * 60 + 5))
        assertEquals("19:00 - 20:15", ClubEngine.timeRange(event()))
    }

    @Test fun placesAreGivenInOrderAndTheRestWait() {
        val e = event(capacity = 2)
        val list = listOf(signup(1, "Ana"), signup(2, "Bo"), signup(3, "Rostyk", user = true))
        assertEquals(listOf("Ana", "Bo"), ClubEngine.confirmed(e, list).map { it.name })
        assertEquals(0, ClubEngine.seatsLeft(e, list))
        assertEquals(SignupStatus.WAITLISTED, ClubEngine.userStatus(e, list))
        assertEquals(1, ClubEngine.waitlistPosition(list[2], e, list))
        // someone ahead cancels: the user moves up without anyone doing anything
        val after = list.filter { it.name != "Ana" }
        assertEquals(SignupStatus.CONFIRMED, ClubEngine.userStatus(e, after))
        assertEquals(SignupStatus.NONE, ClubEngine.userStatus(e, emptyList()))
    }

    @Test fun signUpIsRefusedForDuplicatesPastAndCancelledEvents() {
        val e = event()
        val list = listOf(signup(1, "Ana"))
        assertNull(ClubEngine.signupError(e, "Bo", list, monday))
        assertNotNull(ClubEngine.signupError(e, "  ana ", list, monday))
        assertNotNull(ClubEngine.signupError(e, " ", list, monday))
        assertNotNull(ClubEngine.signupError(e.copy(cancelled = true), "Bo", list, monday))
        assertNotNull(ClubEngine.signupError(e, "Bo", list, monday + 5))
    }

    @Test fun checkInOnlyOnTheDayWithAConfirmedPlace() {
        val e = event(capacity = 1, day = monday)
        val mine = signup(1, "Rostyk", user = true)
        assertTrue(ClubEngine.canCheckIn(e, listOf(mine), monday))
        assertFalse(ClubEngine.canCheckIn(e, listOf(mine), monday - 1))
        assertFalse(ClubEngine.canCheckIn(e, listOf(mine.copy(attended = true)), monday))
        assertFalse(ClubEngine.canCheckIn(e, listOf(signup(1, "Ana"), signup(2, "Rostyk", user = true)), monday))
        assertFalse(ClubEngine.canCheckIn(e.copy(cancelled = true), listOf(mine), monday))
        val outcome = ClubEngine.checkInOutcome(e)
        assertEquals(SessionType.CLUB, outcome.type)
        assertTrue(outcome.xp > 0 && outcome.gains.isNotEmpty())
    }

    @Test fun attendingAClassAwardsProgressThroughTheNormalPath() {
        val state = GameReducer.createFighter("Rostyk", Archetype.KICKER, now)
        val t = GameReducer.applySession(state, ClubEngine.checkInOutcome(event()), monday, now)
        assertEquals(1, t.state.sessions.count { it.type == SessionType.CLUB })
        assertTrue(t.state.fighter!!.xp > 0)
    }

    @Test fun timetableFillsTwoWeeksOnceAndRespectsRemovedEvents() {
        val first = ClubEngine.timetableTopUp(emptyList(), monday, now)
        // per week: conditioning x2, fundamentals x3, pads x2, sparring x1, open mat x1
        assertEquals(18, first.size)
        assertTrue(first.all { it.epochDay in monday until monday + 14 })
        assertEquals(6, first.count { it.title == "Muay Thai Fundamentals" })
        assertTrue(first.filter { it.type == EventType.SPARRING }.all { ClubEngine.weekday(it.epochDay) == 5 })
        // nothing is duplicated on the next start
        assertTrue(ClubEngine.timetableTopUp(first, monday, now).isEmpty())
        // a class the moderator deleted stays deleted
        val removed = first.mapIndexed { i, e -> if (i == 0) e.copy(deleted = true) else e }
        assertTrue(ClubEngine.timetableTopUp(removed, monday, now).isEmpty())
        // a day later, exactly one new day is added at the far end
        val nextDay = ClubEngine.timetableTopUp(first, monday + 1, now)
        assertTrue(nextDay.isNotEmpty() && nextDay.all { it.epochDay == monday + 14 })
        assertTrue(ClubEngine.upcoming(removed, monday).none { it.deleted })
    }

    @Test fun eventDraftsAreValidated() {
        val good = EventDraft(title = "Seminar", epochDay = monday + 2, capacity = 20)
        assertNull(ClubEngine.validateEvent(good, monday))
        assertNotNull(ClubEngine.validateEvent(good.copy(title = "x"), monday))
        assertNotNull(ClubEngine.validateEvent(good.copy(epochDay = monday - 1), monday))
        assertNotNull(ClubEngine.validateEvent(good.copy(capacity = 0), monday))
        assertNotNull(ClubEngine.validateEvent(good.copy(durationMin = 5), monday))
        val event = ClubEngine.toEvent(good.copy(title = "  Seminar  "), null, now)
        assertEquals("Seminar", event.title)
        assertEquals(good.copy(title = "Seminar"), ClubEngine.toDraft(event))
    }

    @Test fun seedContentIsCompleteAndValid() {
        val surveys = ClubEngine.seedSurveys(now)
        assertTrue(surveys.size >= 4)
        for (s in surveys) {
            assertTrue(s.questions.isNotEmpty())
            assertEquals(s.questions.indices.toList(), s.questions.map { it.position })
            for (q in s.questions) {
                val choice = q.type == QuestionType.SINGLE || q.type == QuestionType.MULTI
                assertEquals("${s.title}: ${q.text}", choice, q.options.size >= 2)
            }
        }
        assertEquals(3, ClubEngine.seedSpecialEvents(monday, now).size)
        assertTrue(ClubEngine.seedAnnouncements(now).any { it.pinned })
    }

    @Test fun answersAreValidatedPerQuestionType() {
        val survey = withIds(ClubEngine.seedSurveys(now).first())
        val q = survey.questions
        assertNotNull(ClubEngine.validateAnswers(survey, emptyMap()))
        val answers = mapOf(q[0].id to "2", q[1].id to "1", q[2].id to ClubEngine.encodeMulti(listOf(3, 0)), q[3].id to "4")
        assertNull("the written question is optional", ClubEngine.validateAnswers(survey, answers))
        assertNotNull(ClubEngine.validateAnswers(survey, answers + (q[0].id to "9")))
        assertNotNull(ClubEngine.validateAnswers(survey, answers + (q[3].id to "6")))
        assertNotNull(ClubEngine.validateAnswers(survey, answers + (q[2].id to "")))
        assertNotNull(ClubEngine.validateAnswers(survey.copy(active = false), answers))
        assertEquals("0|3", answers[q[2].id])
        assertEquals("Pad work, Conditioning", ClubEngine.describeAnswer(q[2], answers[q[2].id]))
        assertEquals("Compete in a fight", ClubEngine.describeAnswer(q[0], "2"))
        assertEquals("No answer", ClubEngine.describeAnswer(q[4], null))
    }

    @Test fun resultsAreAggregatedAcrossResponses() {
        val survey = withIds(ClubEngine.seedSurveys(now).first())
        val q = survey.questions
        fun r(id: Long, name: String, user: Boolean, single: String, multi: String, stars: String, text: String? = null) = SurveyResponse(
            id, survey.id, name, user, now,
            buildMap { put(q[0].id, single); put(q[1].id, "1"); put(q[2].id, multi); put(q[3].id, stars); if (text != null) put(q[4].id, text) }
        )
        val responses = listOf(
            r(1, "Rostyk", true, "2", "0|1", "4", "Left knee is sore"),
            r(2, "Ana", false, "2", "1", "2"),
            r(3, "Bo", false, "0", "1|4", "3"),
            SurveyResponse(4, 99, "Other survey", false, now, mapOf(q[0].id to "1"))
        )
        val summary = ClubEngine.summarize(survey, responses)
        assertEquals(listOf(1, 0, 2, 0), summary[0].counts)
        assertEquals(3, summary[0].answered)
        assertEquals(listOf(1, 3, 0, 0, 1), summary[2].counts)
        assertEquals(3.0, summary[3].average, 0.001)
        assertEquals(listOf(0, 1, 1, 1, 0), summary[3].counts)
        assertEquals(listOf("Left knee is sore"), summary[4].texts)
        assertEquals(1, summary[4].answered)

        assertNotNull(ClubEngine.userResponse(survey.id, responses))
        assertNotNull("one answer per member", ClubEngine.responseError(survey, "Rostyk", true, responses))
        assertNotNull(ClubEngine.responseError(survey, "ana", false, responses))
        assertNull(ClubEngine.responseError(survey, "Cy", false, responses))

        val text = ClubEngine.exportSurvey(survey, responses)
        assertTrue(text.contains("3 response(s)"))
        assertTrue(text.contains("Compete in a fight: 2 (66%)"))
        assertTrue(text.contains("Average: 3.0 / 5"))
        assertTrue(text.contains("- Left knee is sore"))
    }

    @Test fun surveyBuilderRejectsIncompleteSurveys() {
        val ok = SurveyDraft(
            title = "Gym kit", questions = listOf(
                QuestionDraft("Which gloves do you use?", QuestionType.SINGLE, listOf("12 oz", " 16 oz ", "16 oz", "")),
                QuestionDraft("Rate the changing rooms", QuestionType.RATING),
                QuestionDraft("Other comments", QuestionType.TEXT, required = false)
            )
        )
        assertNull(ClubEngine.validateSurvey(ok))
        assertNotNull(ClubEngine.validateSurvey(ok.copy(title = "")))
        assertNotNull(ClubEngine.validateSurvey(ok.copy(questions = emptyList())))
        assertNotNull(ClubEngine.validateSurvey(ok.copy(questions = listOf(QuestionDraft("Pick one", QuestionType.MULTI, listOf("Only one", ""))))))
        assertNotNull(ClubEngine.validateSurvey(ok.copy(questions = listOf(QuestionDraft("", QuestionType.RATING)))))
        val survey = ClubEngine.toSurvey(ok, now)
        assertEquals(listOf("12 oz", "16 oz"), survey.questions[0].options)
        assertTrue(survey.questions[1].options.isEmpty())
        assertFalse(survey.questions[2].required)
        assertTrue(survey.active)
    }

    @Test fun eventExportListsBookedAndWaiting() {
        val e = event(capacity = 1)
        val text = ClubEngine.exportEvent(e, listOf(signup(1, "Ana").copy(attended = true), signup(2, "Bo")), "Tue 4 Jan")
        assertTrue(text.contains("BOOKED 1/1"))
        assertTrue(text.contains("1. Ana  [attended]"))
        assertTrue(text.contains("WAITING LIST 1"))
    }

    @Test fun pinIsFourDigitsAndStoredOnlyAsAHash() {
        assertTrue(ClubEngine.isValidPin("0412"))
        assertFalse(ClubEngine.isValidPin("412"))
        assertFalse(ClubEngine.isValidPin("12a4"))
        val hash = ClubEngine.hashPin("0412")
        assertEquals(64, hash.length)
        assertEquals(hash, ClubEngine.hashPin("0412"))
        assertFalse(hash == ClubEngine.hashPin("0413"))
        assertFalse(hash.contains("0412"))
    }

    @Test fun dashboardCountsReflectTheState() {
        val survey = withIds(ClubEngine.seedSurveys(now).first())
        val e = event(day = monday + 1)
        val state = ClubState(
            events = listOf(e, e.copy(id = 8, cancelled = true), e.copy(id = 9, epochDay = monday - 3)),
            signups = listOf(signup(1, "Rostyk", user = true), signup(2, "Ana")),
            surveys = listOf(survey, survey.copy(id = 4, active = false)),
            announcements = ClubEngine.seedAnnouncements(now)
        )
        val c = ClubEngine.counts(state, monday)
        assertEquals(1, c.upcomingEvents)
        assertEquals(1, c.myBookings)
        assertEquals(2, c.totalSignups)
        assertEquals(1, c.openSurveys)
        assertEquals(3, c.unreadNotices)
        assertEquals(0, c.openRequests)
    }
}
