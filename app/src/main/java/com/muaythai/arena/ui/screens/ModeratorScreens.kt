package com.muaythai.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.engine.ClubEngine
import com.muaythai.arena.domain.model.EventDraft
import com.muaythai.arena.domain.model.EventType
import com.muaythai.arena.domain.model.QuestionDraft
import com.muaythai.arena.domain.model.QuestionType
import com.muaythai.arena.domain.model.RequestStatus
import com.muaythai.arena.domain.model.SurveyDraft
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ArenaTextField
import com.muaythai.arena.ui.components.ChipRow
import com.muaythai.arena.ui.components.ConfirmDialog
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.MessageBanner
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.Stepper
import com.muaythai.arena.ui.components.formatDateTime
import com.muaythai.arena.ui.components.formatDay
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.shareText
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.ModeratorUi
import com.muaythai.arena.ui.vm.ModeratorViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

// =====================================================================================
// PIN gate
// =====================================================================================

@Composable
private fun PinPad(onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val feedback = LocalFeedback.current
    val rows = listOf("123", "456", "789", " 0<")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (key in row) {
                    if (key == ' ') {
                        Spacer(Modifier.weight(1f))
                    } else {
                        Box(
                            Modifier.weight(1f).height(60.dp).clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                .clickable { feedback.tap(); if (key == '<') onDelete() else onDigit(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (key == '<') "DELETE" else key.toString(), style = if (key == '<') MaterialTheme.typography.labelMedium else MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/** Asks for the PIN, or for a new PIN twice when [creating] is true. Calls [onPin] with four digits. */
@Composable
private fun PinEntry(creating: Boolean, error: String?, onPin: (String) -> Unit, onTyping: () -> Unit) {
    var entered by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableStateOf(false) }

    ArenaCard(highlight = true) {
        Text(
            when {
                !creating -> "ENTER THE MODERATOR PIN"
                first == null -> "CHOOSE A 4-DIGIT PIN"
                else -> "ENTER THE SAME PIN AGAIN"
            },
            style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            if (creating) "The PIN keeps members out of the moderator tools on this device." else "Only coaches and gym staff should have it.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for (i in 0 until ClubEngine.PIN_LENGTH) {
                Box(
                    Modifier.padding(horizontal = 8.dp).size(22.dp).clip(MaterialTheme.shapes.small)
                        .background(if (i < entered.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PinPad(
            onDigit = { digit ->
                mismatch = false
                onTyping()
                if (entered.length < ClubEngine.PIN_LENGTH) entered += digit
                if (entered.length == ClubEngine.PIN_LENGTH) {
                    val pin = entered
                    entered = ""
                    val previous = first
                    when {
                        !creating -> onPin(pin)
                        previous == null -> first = pin
                        previous == pin -> { first = null; onPin(pin) }
                        else -> { first = null; mismatch = true }
                    }
                }
            },
            onDelete = { entered = entered.dropLast(1) }
        )
    }
    if (mismatch) MessageBanner("The two PINs did not match. Start again.", color = MaterialTheme.colorScheme.error)
    if (error != null) MessageBanner(error, color = MaterialTheme.colorScheme.error)
}

/** Shown by every moderator sub-screen when the panel is locked. Returns true when the caller should stop. */
@Composable
private fun lockedOut(ui: ModeratorUi, title: String, back: () -> Unit): Boolean {
    if (ui.unlocked) return false
    ScreenScaffold(title, back) {
        if (ui.ready) {
            MessageBanner("The moderator panel is locked. Go back and enter the PIN.", color = MaterialTheme.colorScheme.error)
            PrimaryButton("Back", back, Modifier.fillMaxWidth())
        }
    }
    return true
}

// =====================================================================================
// Dashboard
// =====================================================================================

@Composable
private fun ManageCard(title: String, detail: String, badge: String, alert: Boolean, onClick: () -> Unit) {
    ArenaCard(accent = if (alert) MaterialTheme.colorScheme.primary else null, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Pill(badge, filled = alert)
        }
    }
}

@Composable
fun ModeratorScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var changingPin by remember { mutableStateOf(false) }

    if (!ui.unlocked) {
        ScreenScaffold("Moderator", back, subtitle = if (ui.hasPin) "Locked" else "First-time setup") {
            if (ui.ready) {
                PinEntry(
                    creating = !ui.hasPin,
                    error = message?.takeIf { it.error }?.text,
                    onPin = { pin -> if (ui.hasPin) vm.unlock(pin) else vm.createPin(pin) },
                    onTyping = { vm.clearMessage() }
                )
                Text(
                    "The PIN is stored on this device only, as a hash. If it is forgotten, the only way back in is to clear the app's storage in Android settings, which also erases all progress.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (changingPin) {
        ScreenScaffold("Change PIN", { changingPin = false; vm.clearMessage() }, subtitle = "Moderator") {
            PinEntry(creating = true, error = message?.takeIf { it.error }?.text, onPin = { pin -> vm.createPin(pin); changingPin = false }, onTyping = { vm.clearMessage() })
        }
        return
    }

    val c = ui.counts
    val club = ui.club
    ScreenScaffold("Moderator panel", back, subtitle = "Unlocked · gym management") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Upcoming events", "${c.upcomingEvents}", Modifier.weight(1f))
            StatTile("Sign-ups", "${c.totalSignups}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Survey responses", "${c.totalResponses}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
            StatTile("New requests", "${c.newRequests}", Modifier.weight(1f), valueColor = if (c.newRequests > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        message?.let { MessageLine(it) }

        SectionHeader("Manage")
        ManageCard("Events and sign-ups", "Create events, see who is coming, add walk-ins, mark attendance.", "${c.upcomingEvents}", false) { go(Routes.MOD_EVENTS) }
        ManageCard("Surveys", "Build surveys, collect answers on this device, read the results.", "${club.surveys.size}", false) { go(Routes.MOD_SURVEYS) }
        ManageCard("Notice board", "Post, pin, edit and remove notices.", "${club.announcements.size}", false) { go(Routes.MOD_NOTICES) }
        ManageCard("Requests inbox", "Answer members and track what is done.", if (c.newRequests > 0) "${c.newRequests} new" else "${c.openRequests} open", c.newRequests > 0) { go(Routes.MOD_REQUESTS) }

        SectionHeader("Timetable")
        ArenaCard {
            Text("AUTOMATIC WEEKLY TIMETABLE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "When on, the regular classes (conditioning, fundamentals, pad work, sparring, open mat) are created two weeks ahead each time the app starts. Cancelled or deleted classes are not recreated.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("On", "Off"), if (ui.autoTimetable) 0 else 1, { vm.setAutoTimetable(it == 0) })
        }

        SectionHeader("Reports")
        ArenaCard {
            Text("Share the booking lists for every upcoming event as plain text through any app on this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            GhostButton("Share all booking lists", {
                val events = ClubEngine.upcoming(club.events, ui.today).filter { !it.cancelled }
                val text = events.joinToString("\n\n----------\n\n") { ClubEngine.exportEvent(it, club.signups, formatDay(it.epochDay, ui.today)) }
                shareText(context, "Booking lists", text.ifBlank { "No upcoming events." })
            }, Modifier.fillMaxWidth())
        }

        SectionHeader("Access")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Change PIN", { vm.clearMessage(); changingPin = true }, Modifier.weight(1f))
            PrimaryButton("Lock panel", { vm.lock(); back() }, Modifier.weight(1f))
        }
        Text("The panel also locks itself whenever the app is closed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// =====================================================================================
// Events
// =====================================================================================

@Composable
fun ModEventsScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    if (lockedOut(ui, "Events", back)) return
    val club = ui.club
    val upcoming = ClubEngine.upcoming(club.events, ui.today)
    val past = ClubEngine.past(club.events, ui.today).take(12)

    ScreenScaffold("Events", back, subtitle = "${upcoming.size} upcoming · ${ui.counts.totalSignups} sign-ups") {
        PrimaryButton("New event", { go(Routes.modEventEdit(0)) }, Modifier.fillMaxWidth())
        SectionHeader("Upcoming")
        if (upcoming.isEmpty()) ArenaCard { Text("No upcoming events. Create one, or turn the automatic timetable on.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (event in upcoming) EventCard(event, club.signups, ui.today, onClick = { go(Routes.modEvent(event.id)) })
        if (past.isNotEmpty()) {
            SectionHeader("Recently finished")
            for (event in past) EventCard(event, club.signups, ui.today, onClick = { go(Routes.modEvent(event.id)) })
        }
    }
}

@Composable
fun ModEventScreen(id: Long, go: (String) -> Unit, back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var walkIn by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    if (lockedOut(ui, "Event", back)) return

    LaunchedEffect(saved) { if (saved > 0) walkIn = "" }
    val club = ui.club
    val event = club.events.firstOrNull { it.id == id }
    if (event == null) {
        // Either deleted a moment ago from this screen, or it never existed.
        LaunchedEffect(removed) { if (removed) back() }
        ScreenScaffold("Event", back) { Text("This event has been removed.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val booked = ClubEngine.confirmed(event, club.signups)
    val waiting = ClubEngine.waitlist(event, club.signups)
    val dateLabel = formatDay(event.epochDay, ui.today)

    if (deleting) {
        ConfirmDialog(
            "Delete event", "Remove \"${event.title}\" and its ${booked.size + waiting.size} sign-up(s)? Members will no longer see it. To keep it visible as cancelled, use Cancel instead.",
            "Delete", onConfirm = { deleting = false; removed = true; vm.deleteEvent(event.id) }, onDismiss = { deleting = false }, destructive = true
        )
    }

    ScreenScaffold(event.title, back, subtitle = "$dateLabel · ${ClubEngine.timeRange(event)}") {
        ArenaCard(accent = if (event.cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(event.type.label)
                Pill("${booked.size}/${event.capacity} booked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (waiting.isNotEmpty()) Pill("${waiting.size} waiting", color = ArenaColors.Warn)
                if (event.cancelled) Pill("Cancelled", color = MaterialTheme.colorScheme.error, filled = true)
            }
            Spacer(Modifier.height(8.dp))
            KeyValue("Coach", event.coach.ifBlank { "--" })
            KeyValue("Location", event.location.ifBlank { "--" })
            KeyValue("Level", event.level)
            KeyValue("Attended", "${booked.count { it.attended }} of ${booked.size}")
            if (event.description.isNotBlank()) Text(event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Edit", { go(Routes.modEventEdit(event.id)) }, Modifier.weight(1f))
            GhostButton(if (event.cancelled) "Restore" else "Cancel event", { vm.setCancelled(event.id, !event.cancelled) }, Modifier.weight(1f))
        }

        SectionHeader("Add a participant")
        ArenaCard {
            ArenaTextField(walkIn, { walkIn = it.take(32); vm.clearMessage() }, "Name")
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Add to the list", { vm.addParticipant(event.id, walkIn) }, Modifier.fillMaxWidth(), enabled = walkIn.isNotBlank())
            Text("For walk-ins and phone bookings. If the event is full they join the waiting list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        message?.let { MessageLine(it) }

        SectionHeader("Booked", trailing = "${booked.size}/${event.capacity}")
        if (booked.isEmpty()) ArenaCard { Text("Nobody has signed up yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        booked.forEachIndexed { index, s ->
            ArenaCard(accent = if (s.attended) ArenaColors.Win else null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            (if (s.isUser) "Signed up in the app" else "Added by moderator") + " · ${formatDateTime(s.createdAt)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (s.attended) "ATTENDED ✓" else "MARK ATTENDED", style = MaterialTheme.typography.labelSmall,
                        color = if (s.attended) ArenaColors.Win else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { vm.setAttended(s.id, !s.attended) }.padding(6.dp)
                    )
                    Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { vm.removeParticipant(s.id) }.padding(6.dp))
                }
            }
        }
        if (waiting.isNotEmpty()) {
            SectionHeader("Waiting list", trailing = "${waiting.size}")
            waiting.forEachIndexed { index, s ->
                ArenaCard(accent = ArenaColors.Warn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", style = MaterialTheme.typography.titleLarge, color = ArenaColors.Warn, modifier = Modifier.width(32.dp))
                        Text(s.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { vm.removeParticipant(s.id) }.padding(6.dp))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Share list", { shareText(context, event.title, ClubEngine.exportEvent(event, club.signups, dateLabel)) }, Modifier.weight(1f))
            GhostButton("Delete event", { deleting = true }, Modifier.weight(1f))
        }
    }
}

@Composable
fun ModEventEditScreen(id: Long, back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf<EventDraft?>(null) }
    if (lockedOut(ui, "Event", back)) return

    LaunchedEffect(ui.club.loaded, id) {
        if (draft == null && ui.club.loaded) {
            val existing = ui.club.events.firstOrNull { it.id == id }
            draft = if (existing != null) ClubEngine.toDraft(existing) else EventDraft(epochDay = ui.today + 1)
        }
    }
    LaunchedEffect(saved) { if (saved > 0) back() }

    val d = draft
    ScreenScaffold(if (id == 0L) "New event" else "Edit event", back, subtitle = "Moderator") {
        if (d == null) return@ScreenScaffold
        fun update(next: EventDraft) { draft = next; vm.clearMessage() }

        ArenaTextField(d.title, { update(d.copy(title = it.take(48))) }, "Title", placeholder = "For example: Clinch Workshop")
        Text("TYPE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRow(EventType.entries.map { it.label }, d.type.ordinal, { update(d.copy(type = EventType.entries[it])) })

        ArenaCard {
            Stepper("Date", formatDay(d.epochDay, ui.today), { update(d.copy(epochDay = d.epochDay - 1)) }, { update(d.copy(epochDay = d.epochDay + 1)) }, minusEnabled = d.epochDay > ui.today)
            Stepper("Starts", ClubEngine.formatTime(d.startMinute), { update(d.copy(startMinute = (d.startMinute - 30).coerceAtLeast(0))) }, { update(d.copy(startMinute = (d.startMinute + 30).coerceAtMost(23 * 60 + 30))) })
            Stepper("Duration", "${d.durationMin} min", { update(d.copy(durationMin = (d.durationMin - 15).coerceAtLeast(15))) }, { update(d.copy(durationMin = (d.durationMin + 15).coerceAtMost(600))) })
            val step = if (d.capacity >= 20) 5 else 1
            Stepper("Places", "${d.capacity}", { update(d.copy(capacity = (d.capacity - (if (d.capacity > 20) 5 else 1)).coerceAtLeast(1))) }, { update(d.copy(capacity = (d.capacity + step).coerceAtMost(ClubEngine.MAX_CAPACITY))) })
        }

        ArenaTextField(d.coach, { update(d.copy(coach = it.take(32))) }, "Coach")
        ArenaTextField(d.location, { update(d.copy(location = it.take(32))) }, "Location", placeholder = "Main ring, bag room, mat area")
        ArenaTextField(d.level, { update(d.copy(level = it.take(24))) }, "Level")
        ArenaTextField(d.description, { update(d.copy(description = it.take(400))) }, "Description", singleLine = false, minLines = 3)

        message?.let { MessageLine(it) }
        PrimaryButton("Save event", { vm.saveEvent(d) }, Modifier.fillMaxWidth())
        if (id != 0L) Text("Lowering the number of places moves the most recent sign-ups to the waiting list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// =====================================================================================
// Surveys
// =====================================================================================

@Composable
fun ModSurveysScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    if (lockedOut(ui, "Surveys", back)) return
    val club = ui.club

    ScreenScaffold("Surveys", back, subtitle = "${club.surveys.size} surveys · ${club.responses.size} responses") {
        PrimaryButton("New survey", { go(Routes.MOD_SURVEY_NEW) }, Modifier.fillMaxWidth())
        if (club.surveys.isEmpty()) ArenaCard { Text("No surveys yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (survey in club.surveys.sortedByDescending { it.createdAt }) {
            val count = club.responses.count { it.surveyId == survey.id }
            ArenaCard(accent = if (survey.active) MaterialTheme.colorScheme.primary else null, onClick = { go(Routes.modSurvey(survey.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(survey.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("${survey.questions.size} questions · created ${formatDateTime(survey.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$count", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("RESPONSES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Pill(if (survey.active) "Open" else "Closed", color = if (survey.active) ArenaColors.Win else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ModSurveyScreen(id: Long, go: (String) -> Unit, back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    if (lockedOut(ui, "Survey", back)) return

    val club = ui.club
    val survey = club.surveys.firstOrNull { it.id == id }
    if (survey == null) {
        LaunchedEffect(removed) { if (removed) back() }
        ScreenScaffold("Survey", back) { Text("This survey has been removed.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val responses = club.responses.filter { it.surveyId == survey.id }.sortedByDescending { it.createdAt }

    if (deleting) {
        ConfirmDialog(
            "Delete survey", "Delete \"${survey.title}\" together with its ${responses.size} response(s)? This cannot be undone. To stop new answers but keep the results, close it instead.",
            "Delete", onConfirm = { deleting = false; removed = true; vm.deleteSurvey(survey.id) }, onDismiss = { deleting = false }, destructive = true
        )
    }

    ScreenScaffold(survey.title, back, subtitle = "Results · ${responses.size} response(s)") {
        ArenaCard(accent = if (survey.active) ArenaColors.Win else null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (survey.active) "OPEN FOR ANSWERS" else "CLOSED", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("${survey.questions.size} questions · reward +${survey.rewardXp} XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ChipRow(listOf("Open", "Closed"), if (survey.active) 0 else 1, { vm.setSurveyActive(survey.id, it == 0) }, Modifier.width(170.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Collect an answer", { go(Routes.survey(survey.id, true)) }, Modifier.weight(1f), enabled = survey.active)
            GhostButton("Share results", { shareText(context, survey.title, ClubEngine.exportSurvey(survey, club.responses)) }, Modifier.weight(1f))
        }
        Text("Collect an answer opens the survey for another person on this device: hand it over, they enter their name and answer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionHeader("Results")
        for (summary in ClubEngine.summarize(survey, club.responses)) QuestionResult(summary)

        SectionHeader("Respondents", trailing = "${responses.size}")
        if (responses.isEmpty()) ArenaCard { Text("No responses yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (response in responses) {
            ArenaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(response.respondent, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text((if (response.isUser) "Device owner" else "Collected on this device") + " · ${formatDateTime(response.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { vm.deleteResponse(response.id) }.padding(6.dp))
                }
                Spacer(Modifier.height(4.dp))
                for (question in survey.questions.sortedBy { it.position }) {
                    Text("${question.position + 1}. ${ClubEngine.describeAnswer(question, response.answers[question.id])}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        GhostButton("Delete survey", { deleting = true }, Modifier.fillMaxWidth())
    }
}

@Composable
fun ModSurveyNewScreen(back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(SurveyDraft()) }
    if (lockedOut(ui, "New survey", back)) return
    LaunchedEffect(saved) { if (saved > 0) back() }

    fun update(next: SurveyDraft) { draft = next; vm.clearMessage() }
    fun updateQuestion(index: Int, next: QuestionDraft) = update(draft.copy(questions = draft.questions.mapIndexed { i, q -> if (i == index) next else q }))

    ScreenScaffold("New survey", back, subtitle = "Moderator") {
        ArenaTextField(draft.title, { update(draft.copy(title = it.take(60))) }, "Survey title", placeholder = "For example: Seminar feedback")
        ArenaTextField(draft.description, { update(draft.copy(description = it.take(240))) }, "Short introduction", singleLine = false, minLines = 2)
        ArenaCard {
            Stepper("Reward", "+${draft.rewardXp} XP", { update(draft.copy(rewardXp = (draft.rewardXp - 20).coerceAtLeast(0))) }, { update(draft.copy(rewardXp = (draft.rewardXp + 20).coerceAtMost(300))) })
            Text("XP a member earns for completing it in the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        draft.questions.forEachIndexed { index, question ->
            SectionHeader("Question ${index + 1}", trailing = if (draft.questions.size > 1) "Remove" else null, onTrailingClick = {
                update(draft.copy(questions = draft.questions.filterIndexed { i, _ -> i != index }))
            })
            ArenaCard {
                ArenaTextField(question.text, { updateQuestion(index, question.copy(text = it.take(160))) }, "Question")
                Spacer(Modifier.height(8.dp))
                ChipRow(QuestionType.entries.map { it.label }, question.type.ordinal, { updateQuestion(index, question.copy(type = QuestionType.entries[it])) })
                Spacer(Modifier.height(8.dp))
                ChipRow(listOf("Required", "Optional"), if (question.required) 0 else 1, { updateQuestion(index, question.copy(required = it == 0)) })

                if (question.type == QuestionType.SINGLE || question.type == QuestionType.MULTI) {
                    question.options.forEachIndexed { optionIndex, option ->
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ArenaTextField(option, { text ->
                                updateQuestion(index, question.copy(options = question.options.mapIndexed { i, o -> if (i == optionIndex) text.take(60) else o }))
                            }, "Option ${optionIndex + 1}", Modifier.weight(1f))
                            if (question.options.size > 2) {
                                Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { updateQuestion(index, question.copy(options = question.options.filterIndexed { i, _ -> i != optionIndex })) }.padding(8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    GhostButton("Add option", { updateQuestion(index, question.copy(options = question.options + "")) }, enabled = question.options.size < ClubEngine.MAX_OPTIONS)
                } else {
                    Text(
                        if (question.type == QuestionType.RATING) "Members tap a number from 1 (poor) to 5 (excellent)." else "Members type their answer.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        GhostButton("Add question", { update(draft.copy(questions = draft.questions + QuestionDraft())) }, Modifier.fillMaxWidth(), enabled = draft.questions.size < ClubEngine.MAX_QUESTIONS)

        message?.let { MessageLine(it) }
        PrimaryButton("Publish survey", { vm.createSurvey(draft) }, Modifier.fillMaxWidth())
        Text("A published survey cannot be edited, so that answers already given stay meaningful. You can close or delete it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// =====================================================================================
// Notice board
// =====================================================================================

@Composable
fun ModNoticesScreen(back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var editingId by remember { mutableStateOf(0L) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Long?>(null) }
    if (lockedOut(ui, "Notice board", back)) return
    LaunchedEffect(saved) { if (saved > 0) { editingId = 0; title = ""; body = ""; pinned = false } }

    deleting?.let { id ->
        ConfirmDialog("Delete notice", "Remove this notice from the board?", "Delete", onConfirm = { vm.deleteNotice(id); deleting = null }, onDismiss = { deleting = null }, destructive = true)
    }

    val list = ClubEngine.sortedAnnouncements(ui.club.announcements)
    ScreenScaffold("Notice board", back, subtitle = "Moderator · ${list.size} notices") {
        SectionHeader(if (editingId == 0L) "New notice" else "Edit notice", trailing = if (editingId != 0L) "Cancel edit" else null, onTrailingClick = {
            editingId = 0; title = ""; body = ""; pinned = false; vm.clearMessage()
        })
        ArenaCard {
            ArenaTextField(title, { title = it.take(60); vm.clearMessage() }, "Title")
            Spacer(Modifier.height(8.dp))
            ArenaTextField(body, { body = it.take(800); vm.clearMessage() }, "Text", singleLine = false, minLines = 3)
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("Normal", "Pinned to the top"), if (pinned) 1 else 0, { pinned = it == 1 })
            Spacer(Modifier.height(10.dp))
            PrimaryButton(if (editingId == 0L) "Publish notice" else "Save changes", { vm.saveNotice(editingId, title, body, pinned) }, Modifier.fillMaxWidth())
        }
        message?.let { MessageLine(it) }

        SectionHeader("Published")
        if (list.isEmpty()) ArenaCard { Text("The board is empty.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (notice in list) {
            ArenaCard(accent = if (notice.pinned) MaterialTheme.colorScheme.primary else null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (notice.pinned) Pill("Pinned", filled = true)
                    Pill(if (notice.read) "Read by member" else "Unread", color = if (notice.read) MaterialTheme.colorScheme.onSurfaceVariant else ArenaColors.Warn)
                    Pill(formatDateTime(notice.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(notice.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(notice.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Edit", { editingId = notice.id; title = notice.title; body = notice.body; pinned = notice.pinned; vm.clearMessage() }, Modifier.weight(1f))
                    GhostButton("Delete", { deleting = notice.id }, Modifier.weight(1f))
                }
            }
        }
    }
}

// =====================================================================================
// Requests inbox
// =====================================================================================

@Composable
fun ModRequestsScreen(back: () -> Unit) {
    val vm: ModeratorViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var openId by remember { mutableStateOf(0L) }
    var reply by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(RequestStatus.NEW) }
    if (lockedOut(ui, "Requests", back)) return
    LaunchedEffect(saved) { if (saved > 0) openId = 0 }

    val requests = ClubEngine.sortedRequests(ui.club.requests)
    ScreenScaffold("Requests inbox", back, subtitle = "${ui.counts.newRequests} new · ${ui.counts.openRequests} open") {
        message?.let { MessageLine(it) }
        if (requests.isEmpty()) ArenaCard { Text("No requests from members yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        for (request in requests) {
            val open = openId == request.id
            ArenaCard(accent = requestStatusColor(request.status), highlight = open) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(request.status.label, color = requestStatusColor(request.status), filled = request.status == RequestStatus.NEW)
                    Pill(request.category.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(request.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("From ${request.author} · ${formatDateTime(request.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (request.reply.isNotBlank() && !open) {
                    Spacer(Modifier.height(6.dp))
                    Text("YOUR REPLY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(request.reply, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                if (open) {
                    ChipRow(RequestStatus.entries.map { it.label }, status.ordinal, { status = RequestStatus.entries[it] })
                    Spacer(Modifier.height(8.dp))
                    ArenaTextField(reply, { reply = it.take(500) }, "Reply to the member", singleLine = false, minLines = 2)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton("Close", { openId = 0 }, Modifier.weight(1f))
                        PrimaryButton("Save", { vm.updateRequest(request.id, status, reply) }, Modifier.weight(1f))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton("Reply or update", { openId = request.id; reply = request.reply; status = if (request.status == RequestStatus.NEW) RequestStatus.IN_PROGRESS else request.status; vm.clearMessage() }, Modifier.weight(1f))
                        GhostButton("Delete", { vm.deleteRequest(request.id) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
