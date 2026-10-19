package com.muaythai.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.engine.ClubEngine
import com.muaythai.arena.domain.model.ClubEvent
import com.muaythai.arena.domain.model.EventSignup
import com.muaythai.arena.domain.model.QuestionSummary
import com.muaythai.arena.domain.model.QuestionType
import com.muaythai.arena.domain.model.RequestCategory
import com.muaythai.arena.domain.model.RequestStatus
import com.muaythai.arena.domain.model.SignupStatus
import com.muaythai.arena.domain.model.Survey
import com.muaythai.arena.domain.model.SurveyQuestion
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ArenaTextField
import com.muaythai.arena.ui.components.ChipRow
import com.muaythai.arena.ui.components.ChoiceCard
import com.muaythai.arena.ui.components.ChoiceState
import com.muaythai.arena.ui.components.ConfirmDialog
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.MessageBanner
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.ResultBar
import com.muaythai.arena.ui.components.RewardDialog
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.formatDateTime
import com.muaythai.arena.ui.components.formatDay
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.ClubViewModel
import com.muaythai.arena.ui.vm.UiMessage
import com.muaythai.arena.ui.vm.arenaViewModel
import java.util.Locale

@Composable
fun MessageLine(message: UiMessage?) {
    if (message != null) MessageBanner(message.text, color = if (message.error) MaterialTheme.colorScheme.error else ArenaColors.Win)
}

@Composable
private fun HubCard(title: String, detail: String, badge: String, highlight: Boolean, onClick: () -> Unit) {
    ArenaCard(accent = if (highlight) MaterialTheme.colorScheme.primary else null, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Pill(badge, filled = highlight)
        }
    }
}

/** One event as it appears in a list, for members and for the moderator. */
@Composable
fun EventCard(event: ClubEvent, signups: List<EventSignup>, today: Long, onClick: () -> Unit, showDay: Boolean = true) {
    val booked = ClubEngine.signupsFor(event.id, signups).size
    val status = ClubEngine.userStatus(event, signups)
    val full = booked >= event.capacity
    ArenaCard(
        modifier = Modifier.alpha(if (event.cancelled) 0.6f else 1f),
        accent = when {
            event.cancelled -> null
            status == SignupStatus.CONFIRMED -> ArenaColors.Win
            status == SignupStatus.WAITLISTED -> ArenaColors.Warn
            else -> MaterialTheme.colorScheme.primary
        },
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (showDay) Text(formatDay(event.epochDay, today).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(event.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    listOf(event.type.label, event.coach, event.location).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(ClubEngine.formatTime(event.startMinute), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("${event.durationMin} MIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(booked.toFloat() / event.capacity.coerceAtLeast(1), color = if (full) ArenaColors.Warn else MaterialTheme.colorScheme.primary, height = 5.dp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("${minOf(booked, event.capacity)}/${event.capacity} booked", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (booked > event.capacity) Pill("${booked - event.capacity} waiting", color = ArenaColors.Warn)
            when {
                event.cancelled -> Pill("Cancelled", color = MaterialTheme.colorScheme.error, filled = true)
                status == SignupStatus.CONFIRMED -> Pill("You are booked", color = ArenaColors.Win)
                status == SignupStatus.WAITLISTED -> Pill("Waiting list", color = ArenaColors.Warn)
                full -> Pill("Full", color = ArenaColors.Warn)
            }
        }
    }
}

// =====================================================================================
// Club hub
// =====================================================================================

@Composable
fun ClubScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = ui.counts
    val club = ui.club

    ScreenScaffold("Club", back, subtitle = "Book, answer, ask") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("My bookings", "${c.myBookings}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
            StatTile("Open surveys", "${c.openSurveys}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
            StatTile("Unread notices", "${c.unreadNotices}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
        }

        val nextBooking = ClubEngine.upcoming(club.events, ui.today).firstOrNull { !it.cancelled && ClubEngine.userSignup(it, club.signups) != null }
        SectionHeader("Your next booking")
        if (nextBooking != null) {
            EventCard(nextBooking, club.signups, ui.today, onClick = { go(Routes.event(nextBooking.id)) })
        } else {
            ArenaCard(onClick = { go(Routes.SCHEDULE) }) {
                Text("NOTHING BOOKED", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("${c.upcomingEvents} classes and events are open for sign-up in the next two weeks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("OPEN THE SCHEDULE  >", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }

        SectionHeader("Club services")
        HubCard("Schedule and sign-up", "Classes, sparring, seminars, gradings and fight nights.", "${c.upcomingEvents} upcoming", c.myBookings == 0) { go(Routes.SCHEDULE) }
        HubCard("Surveys", "Tell the coaches what you think and earn XP for every survey.", if (c.openSurveys > 0) "${c.openSurveys} open" else "All done", c.openSurveys > 0) { go(Routes.SURVEYS) }
        HubCard("Notice board", "News and rules from the gym.", if (c.unreadNotices > 0) "${c.unreadNotices} new" else "Up to date", c.unreadNotices > 0) { go(Routes.NOTICES) }
        HubCard("Requests", "Ask for a private lesson, a schedule change or anything else.", "${club.requests.size} sent", false) { go(Routes.REQUESTS) }

        val pinned = ClubEngine.sortedAnnouncements(club.announcements).firstOrNull()
        if (pinned != null) {
            SectionHeader(if (pinned.pinned) "Pinned notice" else "Latest notice")
            ArenaCard(onClick = { go(Routes.NOTICES) }) {
                Text(pinned.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(pinned.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
        }

        SectionHeader("For coaches and gym staff")
        ArenaCard {
            Text("Create events and surveys, see who signed up, mark attendance, read survey results and answer requests.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            GhostButton("Moderator panel", { go(Routes.MODERATOR) }, Modifier.fillMaxWidth())
        }
    }
}

// =====================================================================================
// Schedule and sign-up
// =====================================================================================

@Composable
fun ScheduleScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val club = ui.club

    val upcoming = ClubEngine.upcoming(club.events, ui.today)
    val shown = when (filter) {
        1 -> upcoming.filter { ClubEngine.userSignup(it, club.signups) != null }
        2 -> ClubEngine.past(club.events, ui.today).take(40)
        else -> upcoming
    }
    val days = shown.groupBy { it.epochDay }.toList()

    ScreenScaffold("Schedule", back, subtitle = "${upcoming.count { !it.cancelled }} events open for sign-up", scroll = false) {
        ChipRow(listOf("Upcoming", "My bookings", "Past"), filter, { filter = it })
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (shown.isEmpty()) {
                item {
                    ArenaCard {
                        Text(
                            when (filter) {
                                1 -> "You have no bookings. Open Upcoming and tap an event to sign up."
                                2 -> "No past events yet."
                                else -> "No events are scheduled. A moderator can add them in the moderator panel."
                            },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            for ((day, events) in days) {
                item(key = "day$day") { SectionHeader(formatDay(day, ui.today), trailing = "${events.size} event(s)") }
                items(events, key = { it.id }) { event ->
                    EventCard(event, club.signups, ui.today, onClick = { go(Routes.event(event.id)) }, showDay = false)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun EventScreen(id: Long, back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val reward by vm.reward.collectAsStateWithLifecycle()
    var cancelling by remember { mutableStateOf(false) }
    val club = ui.club
    val event = club.events.firstOrNull { it.id == id }

    reward?.let { RewardDialog(it, onDismiss = { vm.dismissReward() }) }

    if (event == null) {
        ScreenScaffold("Event", back) {
            Text(if (club.loaded) "This event is no longer on the schedule." else "Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val mine = ClubEngine.userSignup(event, club.signups)
    val status = ClubEngine.statusOf(mine, event, club.signups)
    val booked = ClubEngine.confirmed(event, club.signups).size
    val waiting = ClubEngine.waitlist(event, club.signups).size
    val past = ClubEngine.isPast(event, ui.today)

    if (cancelling && mine != null) {
        ConfirmDialog(
            title = "Cancel booking",
            message = "Give up your place at ${event.title}? If the event is full, the next person on the waiting list gets it.",
            confirmLabel = "Cancel booking", destructive = true,
            onConfirm = { cancelling = false; vm.cancelBooking(mine.id) }, onDismiss = { cancelling = false }
        )
    }

    ScreenScaffold(event.title, back, subtitle = event.type.label) {
        ArenaCard(highlight = true) {
            Text(formatDay(event.epochDay, ui.today).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(ClubEngine.timeRange(event), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            if (event.coach.isNotBlank()) KeyValue("Coach", event.coach)
            if (event.location.isNotBlank()) KeyValue("Location", event.location)
            KeyValue("Level", event.level)
            KeyValue("Duration", "${event.durationMin} minutes")
            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(event.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        ArenaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PLACES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("$booked / ${event.capacity}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(booked.toFloat() / event.capacity.coerceAtLeast(1), color = if (booked >= event.capacity) ArenaColors.Warn else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    booked < event.capacity -> "${event.capacity - booked} place(s) left."
                    waiting == 0 -> "Fully booked. You can join the waiting list."
                    else -> "Fully booked. $waiting on the waiting list."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            event.cancelled -> MessageBanner("This event has been cancelled by the gym.", color = MaterialTheme.colorScheme.error)
            mine != null && mine.attended -> MessageBanner("Attendance confirmed. This session is in your training history.", color = ArenaColors.Win)
            status == SignupStatus.CONFIRMED -> MessageBanner("You are booked as ${mine?.name ?: ui.userName}.", color = ArenaColors.Win)
            status == SignupStatus.WAITLISTED && mine != null ->
                MessageBanner("You are number ${ClubEngine.waitlistPosition(mine, event, club.signups)} on the waiting list. You move up automatically when a place opens.", color = ArenaColors.Warn)
        }
        MessageLine(message)

        if (!event.cancelled) {
            when {
                mine == null && !past -> PrimaryButton(if (booked >= event.capacity) "Join the waiting list" else "Sign up", { vm.signUp(event) }, Modifier.fillMaxWidth())
                mine != null && !mine.attended -> {
                    if (ClubEngine.canCheckIn(event, club.signups, ui.today)) {
                        PrimaryButton("Check in: I am here", { vm.checkIn(event) }, Modifier.fillMaxWidth())
                        Text("Checking in logs the class as training and awards XP.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (!past && status == SignupStatus.CONFIRMED) {
                        Text("Check-in opens here on the day of the event.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!past) GhostButton("Cancel my booking", { cancelling = true }, Modifier.fillMaxWidth())
                }
                past && mine == null -> Text("This event has already taken place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// =====================================================================================
// Surveys
// =====================================================================================

@Composable
fun SurveysScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val club = ui.club
    val open = club.surveys.filter { it.active && ClubEngine.userResponse(it.id, club.responses) == null }
    val done = club.surveys.filter { ClubEngine.userResponse(it.id, club.responses) != null }

    ScreenScaffold("Surveys", back, subtitle = "${open.size} waiting for your answer") {
        SectionHeader("Open", trailing = "${open.size}")
        if (open.isEmpty()) {
            ArenaCard { Text("You have answered everything. New surveys from the gym appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        for (survey in open) {
            ArenaCard(accent = MaterialTheme.colorScheme.primary, onClick = { go(Routes.survey(survey.id, false)) }) {
                Text(survey.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(survey.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("${survey.questions.size} questions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Pill("+${survey.rewardXp} XP")
                }
            }
        }
        if (done.isNotEmpty()) {
            SectionHeader("Completed", trailing = "${done.size}")
            for (survey in done) {
                ArenaCard(accent = ArenaColors.Win, onClick = { go(Routes.survey(survey.id, false)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(survey.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tap to see your answers and the results so far.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Pill("Done", color = ArenaColors.Win)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionInput(question: SurveyQuestion, raw: String?, onAnswer: (String) -> Unit) {
    when (question.type) {
        QuestionType.SINGLE -> question.options.forEachIndexed { index, option ->
            val on = raw == index.toString()
            ChoiceCard(text = option, state = if (on) ChoiceState.SELECTED else ChoiceState.NEUTRAL, trailing = if (on) "Selected" else null, onClick = { onAnswer(index.toString()) }, modifier = Modifier.padding(top = 6.dp))
        }
        QuestionType.MULTI -> {
            val picked = ClubEngine.decodeMulti(raw.orEmpty())
            question.options.forEachIndexed { index, option ->
                val on = index in picked
                ChoiceCard(
                    text = option, state = if (on) ChoiceState.SELECTED else ChoiceState.NEUTRAL, trailing = if (on) "✓" else null,
                    onClick = { onAnswer(ClubEngine.encodeMulti(if (on) picked - index else picked + index)) }, modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text("Choose all that apply.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        QuestionType.RATING -> {
            val value = raw?.toIntOrNull() ?: 0
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (star in 1..5) {
                    val on = star <= value
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(MaterialTheme.shapes.small)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                            .clickable { onAnswer(star.toString()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$star", style = MaterialTheme.typography.headlineSmall, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Poor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("Excellent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        QuestionType.TEXT -> ArenaTextField(raw.orEmpty(), { onAnswer(it.take(500)) }, "Your answer", Modifier.padding(top = 8.dp), singleLine = false, minLines = 3)
    }
}

/** Aggregated answers for one question. Shared by the member view and the moderator results screen. */
@Composable
fun QuestionResult(summary: QuestionSummary, ownAnswer: String? = null) {
    val question = summary.question
    ArenaCard {
        Text("QUESTION ${question.position + 1} · ${question.type.label.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(question.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        when (question.type) {
            QuestionType.SINGLE, QuestionType.MULTI -> {
                val own = if (question.type == QuestionType.SINGLE) setOfNotNull(ownAnswer?.toIntOrNull()) else ClubEngine.decodeMulti(ownAnswer.orEmpty())
                question.options.forEachIndexed { index, option ->
                    ResultBar(if (index in own) "$option  (you)" else option, summary.counts.getOrElse(index) { 0 }, summary.answered, highlight = index in own)
                }
            }
            QuestionType.RATING -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (summary.answered == 0) "--" else String.format(Locale.US, "%.1f", summary.average), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text("  / 5 average", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                val own = ownAnswer?.toIntOrNull()
                for (star in 5 downTo 1) ResultBar(if (star == own) "$star  (you)" else "$star", summary.counts.getOrElse(star - 1) { 0 }, summary.answered, highlight = star == own)
            }
            QuestionType.TEXT -> {
                if (summary.texts.isEmpty()) Text("No written answers yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (text in summary.texts) Text("“$text”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("${summary.answered} answered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** [guest] is true when the moderator hands the device to someone else to answer. */
@Composable
fun SurveyScreen(id: Long, guest: Boolean, back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val reward by vm.reward.collectAsStateWithLifecycle()
    val submitted by vm.submitted.collectAsStateWithLifecycle()
    var answers by remember { mutableStateOf(mapOf<Long, String>()) }
    var guestName by remember { mutableStateOf("") }
    val club = ui.club
    val survey = club.surveys.firstOrNull { it.id == id }

    // A successful guest submission clears the form for the next person.
    LaunchedEffect(submitted) { if (submitted > 0) { answers = emptyMap(); guestName = "" } }

    reward?.let { RewardDialog(it, onDismiss = { vm.dismissReward() }, buttonText = "See the results") }

    if (survey == null) {
        ScreenScaffold("Survey", back) { Text(if (club.loaded) "This survey is no longer available." else "Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val mine = if (guest) null else ClubEngine.userResponse(survey.id, club.responses)
    val questions = survey.questions.sortedBy { it.position }

    ScreenScaffold(survey.title, back, subtitle = if (guest) "Guest answer · hand the device over" else "Survey · +${survey.rewardXp} XP") {
        if (mine != null) {
            MessageBanner("You completed this survey on ${formatDateTime(mine.createdAt)}. Thank you.", color = ArenaColors.Win)
            SectionHeader("Results so far", trailing = "${club.responses.count { it.surveyId == survey.id }} response(s)")
            for (summary in ClubEngine.summarize(survey, club.responses)) QuestionResult(summary, mine.answers[summary.question.id])
            return@ScreenScaffold
        }
        if (!survey.active) {
            MessageBanner("This survey is closed.", color = MaterialTheme.colorScheme.error)
            return@ScreenScaffold
        }

        if (survey.description.isNotBlank()) Text(survey.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val answered = questions.count { ClubEngine.isAnswered(it, answers[it.id]) }
        ProgressBar(answered.toFloat() / questions.size.coerceAtLeast(1), color = ArenaColors.Win, height = 6.dp)
        Text("$answered of ${questions.size} answered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (guest) ArenaTextField(guestName, { guestName = it.take(32) }, "Name of the person answering")

        for (question in questions) {
            ArenaCard(accent = if (ClubEngine.isAnswered(question, answers[question.id])) ArenaColors.Win else null) {
                Text(
                    "QUESTION ${question.position + 1}" + if (question.required) "" else " · OPTIONAL",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary
                )
                Text(question.text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                QuestionInput(question, answers[question.id]) { value -> answers = answers + (question.id to value); vm.clearMessage() }
            }
        }
        MessageLine(message)
        PrimaryButton(if (guest) "Submit answers" else "Submit and earn +${survey.rewardXp} XP", { vm.submitSurvey(survey, answers, if (guest) guestName else null) }, Modifier.fillMaxWidth())
    }
}

// =====================================================================================
// Notice board
// =====================================================================================

@Composable
fun NoticesScreen(back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val club = ui.club
    // Remember what was unread when the board was opened, so the NEW labels stay visible during this visit.
    var unreadAtOpen by remember { mutableStateOf<Set<Long>?>(null) }
    LaunchedEffect(club.loaded) {
        if (club.loaded && unreadAtOpen == null) {
            unreadAtOpen = club.announcements.filter { !it.read }.map { it.id }.toSet()
            vm.markNoticesRead()
        }
    }
    val list = ClubEngine.sortedAnnouncements(club.announcements)

    ScreenScaffold("Notice board", back, subtitle = "${list.size} notices from the gym") {
        if (list.isEmpty()) ArenaCard { Text("Nothing has been posted yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (notice in list) {
            val fresh = unreadAtOpen?.contains(notice.id) == true
            ArenaCard(accent = if (notice.pinned) MaterialTheme.colorScheme.primary else null, highlight = fresh) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (notice.pinned) Pill("Pinned", filled = true)
                    if (fresh) Pill("New", color = ArenaColors.Win)
                    Pill(formatDateTime(notice.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(notice.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(notice.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// =====================================================================================
// Requests to the coach
// =====================================================================================

@Composable
fun requestStatusColor(status: RequestStatus) = when (status) {
    RequestStatus.NEW -> MaterialTheme.colorScheme.primary
    RequestStatus.IN_PROGRESS -> ArenaColors.Warn
    RequestStatus.DONE -> ArenaColors.Win
    RequestStatus.DECLINED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun RequestsScreen(back: () -> Unit) {
    val vm: ClubViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val submitted by vm.submitted.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(submitted) { if (submitted > 0) text = "" }

    deleting?.let { id ->
        ConfirmDialog("Delete request", "Remove this request and its reply?", "Delete", onConfirm = { vm.deleteRequest(id); deleting = null }, onDismiss = { deleting = null }, destructive = true)
    }

    val requests = ClubEngine.sortedRequests(ui.club.requests)
    ScreenScaffold("Requests", back, subtitle = "Ask your coach or the front desk") {
        SectionHeader("New request")
        ArenaCard {
            ChipRow(RequestCategory.entries.map { it.label }, category, { category = it })
            Spacer(Modifier.height(8.dp))
            ArenaTextField(text, { text = it.take(500); vm.clearMessage() }, "What do you need?", singleLine = false, minLines = 3,
                placeholder = "For example: a private lesson on clinch next week, evenings")
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Send request", { vm.sendRequest(RequestCategory.entries[category], text) }, Modifier.fillMaxWidth(), enabled = text.trim().length >= 5)
        }
        MessageLine(message)

        SectionHeader("Your requests", trailing = "${requests.size}")
        if (requests.isEmpty()) ArenaCard { Text("Nothing sent yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (request in requests) {
            ArenaCard(accent = requestStatusColor(request.status)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(request.status.label, color = requestStatusColor(request.status))
                    Pill(request.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("DELETE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { deleting = request.id }.padding(4.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(request.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Sent ${formatDateTime(request.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (request.reply.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("REPLY FROM THE GYM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(request.reply, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
