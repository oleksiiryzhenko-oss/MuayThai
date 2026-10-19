package com.muaythai.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.engine.AchievementEngine
import com.muaythai.arena.domain.engine.ChallengeEngine
import com.muaythai.arena.domain.engine.CoachEngine
import com.muaythai.arena.domain.engine.ProgressionEngine
import com.muaythai.arena.domain.engine.StatsEngine
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.Rank
import com.muaythai.arena.domain.model.RecordKey
import com.muaythai.arena.domain.model.TrainingSession
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.RankBadge
import com.muaythai.arena.ui.components.RopeDivider
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StadiumHeader
import com.muaythai.arena.ui.components.StatRow
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.formatDate
import com.muaythai.arena.ui.components.formatDateTime
import com.muaythai.arena.ui.components.formatDuration
import com.muaythai.arena.ui.components.formatNumber
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.arenaViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun LinkCard(title: String, detail: String, value: String, onClick: () -> Unit) {
    ArenaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("  >", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Seven bars, Monday to Sunday, for the week containing [today]. */
@Composable
fun WeekBars(sessions: List<TrainingSession>, today: Long, modifier: Modifier = Modifier) {
    val start = StatsEngine.weekStart(today)
    val xp = StatsEngine.xpByDay(sessions)
    val values = (0 until 7).map { xp[start + it] ?: 0 }
    val max = values.max().coerceAtLeast(1)
    val names = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 0 until 7) {
            val isToday = start + i == today
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight((values[i].toFloat() / max).coerceIn(0.03f, 1f))
                            .background(if (values[i] == 0) MaterialTheme.colorScheme.surfaceVariant else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(names[i], style = MaterialTheme.typography.labelSmall, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProgressScreen(go: (String) -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val stats = o.stats
    val fighter = o.state.fighter
    val req = o.rankRequirement
    val gold = MaterialTheme.colorScheme.tertiary

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        StadiumHeader {
            Text("PROGRESS", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (rank in Rank.entries) {
                    Column(Modifier.weight(1f).alpha(if (rank.ordinal <= stats.rank.ordinal) 1f else 0.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                        RankBadge(rank, size = 34.dp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stats.rank.label.uppercase() + "  ·  LEVEL ${stats.level}", style = MaterialTheme.typography.headlineSmall,
                color = if (stats.rank.elite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
            )
        }
        RopeDivider()

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Next promotion")
            ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                val next = req.next
                if (next == null) {
                    Text("CHAMPION", style = MaterialTheme.typography.headlineMedium, color = gold)
                    Text("You hold the highest rank in the arena.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(next, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(next.label.uppercase(), style = MaterialTheme.typography.headlineSmall, color = if (next.elite) gold else MaterialTheme.colorScheme.onSurface)
                            Text("Requires XP and performance milestones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LEVEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("${stats.level} / ${req.levelNeeded}", style = MaterialTheme.typography.titleMedium, color = if (req.levelOk) ArenaColors.Win else MaterialTheme.colorScheme.onSurface)
                    }
                    ProgressBar(stats.level.toFloat() / req.levelNeeded, color = if (req.levelOk) ArenaColors.Win else MaterialTheme.colorScheme.primary, height = 6.dp)
                    Spacer(Modifier.height(8.dp))
                    val points = fighter?.rankPoints ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("RANK POINTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("${formatNumber(points)} / ${formatNumber(req.pointsNeeded)}", style = MaterialTheme.typography.titleMedium, color = if (req.pointsOk) ArenaColors.Win else MaterialTheme.colorScheme.onSurface)
                    }
                    ProgressBar(points.toFloat() / req.pointsNeeded.coerceAtLeast(1), color = if (req.pointsOk) ArenaColors.Win else MaterialTheme.colorScheme.primary, height = 6.dp)
                    Spacer(Modifier.height(6.dp))
                    Text("Rank points come from fight wins and completed challenges.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionHeader("Fighter attributes", trailing = "Overall ${stats.overall}")
            ArenaCard {
                for (attr in Attribute.entries) {
                    val p = ProgressionEngine.attributeProgress(o.state.attributeXp[attr] ?: 0)
                    Row(Modifier.fillMaxWidth().clickable { go(Routes.attribute(attr.name)) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(attr.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(118.dp))
                        Column(Modifier.weight(1f)) {
                            ProgressBar(p.level / 100f, height = 7.dp)
                            Spacer(Modifier.height(2.dp))
                            ProgressBar(p.fraction, color = MaterialTheme.colorScheme.onSurfaceVariant, height = 2.dp)
                        }
                        Text("${p.level}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                    }
                }
                Text("Thick bar: level. Thin bar: XP toward the next level. Tap an attribute for its history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionHeader("Fighting style")
            ArenaCard {
                Text(stats.archetype.label.uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text(stats.archetype.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (type in Archetype.entries) {
                        StatRow(type.label, o.affinity[type] ?: 0, color = if (type == stats.archetype) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Your style follows your attributes. Train differently and it will change.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionHeader("This week")
            ArenaCard(onClick = { go(Routes.HISTORY) }) {
                WeekBars(o.state.sessions, o.today)
                Spacer(Modifier.height(6.dp))
                Text("XP earned per day. Tap for the full training history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionHeader("More")
            LinkCard("Achievements", "Milestones across every part of the game", "${o.state.achievements.size}/${AchievementEngine.all.size}") { go(Routes.ACHIEVEMENTS) }
            LinkCard("Personal records", "Your best marks, kept on this device", "${o.state.records.size}") { go(Routes.RECORDS) }
            LinkCard("Training history", "Calendar, streaks and every logged session", "${stats.totalSessions}") { go(Routes.HISTORY) }
            LinkCard("Challenges", "Daily missions and weekly objectives", "${stats.dailyCompleted + stats.weeklyCompleted}") { go(Routes.CHALLENGES) }
            LinkCard("Smart coach", "Advice based on your actual statistics", "${o.tips.size}") { go(Routes.COACH) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AttributeScreen(name: String, go: (String) -> Unit, back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val attr = Attribute.entries.firstOrNull { it.name == name }
    if (attr == null) {
        ScreenScaffold("Attribute", back) { Text("Unknown attribute.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val xp = o.state.attributeXp[attr] ?: 0
    val p = ProgressionEngine.attributeProgress(xp)
    val history = o.state.sessions.filter { (it.gains()[attr] ?: 0) > 0 }.takeLast(40).reversed()
    val destination = CoachEngine.destinationFor(attr)
    val rankAmong = Attribute.entries.sortedByDescending { o.stats.levels[it] ?: 0 }.indexOf(attr) + 1

    ScreenScaffold(attr.label, back, subtitle = "Fighter attribute") {
        ArenaCard(accent = MaterialTheme.colorScheme.primary) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${p.level}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text("CURRENT LEVEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("#$rankAmong of your ${Attribute.entries.size} attributes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(attr.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row {
                Text("XP TO LEVEL ${p.level + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("${formatNumber(p.xpIntoLevel)} / ${formatNumber(p.xpForNext)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            ProgressBar(p.fraction)
            Spacer(Modifier.height(6.dp))
            KeyValue("Total XP", formatNumber(xp))
        }
        PrimaryButton(CoachEngine.destinationLabel(destination), { go(destination) }, Modifier.fillMaxWidth())

        SectionHeader("Training history", trailing = "${history.size} sessions")
        if (history.isEmpty()) {
            ArenaCard { Text("Nothing has trained this attribute yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            ArenaCard {
                for (s in history) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("${s.type.label} · ${formatDateTime(s.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("+${s.gains()[attr] ?: 0}", style = MaterialTheme.typography.titleMedium, color = ArenaColors.Win)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val unlocked = o.state.achievements
    val sorted = AchievementEngine.all.sortedWith(compareByDescending<com.muaythai.arena.domain.model.Achievement> { it.id in unlocked }.thenBy { it.xp })
    val gold = MaterialTheme.colorScheme.tertiary

    ScreenScaffold("Achievements", back, subtitle = "${unlocked.size} of ${AchievementEngine.all.size} unlocked", scroll = false) {
        ProgressBar(unlocked.size.toFloat() / AchievementEngine.all.size, color = gold)
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sorted, key = { it.id }) { a ->
                val at = unlocked[a.id]
                val tint = if (a.gold) gold else MaterialTheme.colorScheme.primary
                ArenaCard(modifier = Modifier.alpha(if (at != null) 1f else 0.5f), accent = if (at != null) tint else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = if (at != null && a.gold) gold else MaterialTheme.colorScheme.onSurface)
                            Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (at != null) Text("Unlocked ${formatDate(at)}", style = MaterialTheme.typography.labelSmall, color = tint)
                        }
                        Spacer(Modifier.width(8.dp))
                        Pill(if (at != null) "+${a.xp} XP" else "Locked", color = if (at != null) tint else MaterialTheme.colorScheme.onSurfaceVariant, filled = at != null && !a.gold)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun RecordsScreen(back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    ScreenScaffold("Personal records", back, subtitle = "${o.state.records.size} of ${RecordKey.entries.size} set") {
        for (key in RecordKey.entries) {
            val record = o.state.records[key]
            ArenaCard(accent = if (record != null) MaterialTheme.colorScheme.tertiary else null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(key.label.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (record != null) "${record.detail} · ${formatDate(record.achievedAt)}" else "Not set yet",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (record != null) formatNumber(record.value) + (if (key.unit.isEmpty()) "" else " ${key.unit}") else "--",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (record != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text("Records are stored locally and are cleared by Reset progression in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthCalendar(month: YearMonth, activeDays: Set<Long>, today: Long, selected: Long?, onSelect: (Long) -> Unit) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1 // Monday first
    val cells: List<LocalDate?> = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Row {
        for (d in listOf("M", "T", "W", "T", "F", "S", "S")) {
            Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(4.dp))
    for (week in cells.chunked(7)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 2.dp)) {
            for (i in 0 until 7) {
                val date = week.getOrNull(i)
                if (date == null) {
                    Spacer(Modifier.weight(1f))
                } else {
                    val day = date.toEpochDay()
                    val active = day in activeDays
                    val isSelected = day == selected
                    var cell = Modifier.weight(1f).aspectRatio(1f).clip(MaterialTheme.shapes.small)
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 1f else 0.75f) else MaterialTheme.colorScheme.surfaceVariant)
                    if (isSelected) cell = cell.border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)
                    else if (day == today) cell = cell.border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.small)
                    Box(cell.clickable { onSelect(day) }, contentAlignment = Alignment.Center) {
                        Text(
                            "${date.dayOfMonth}", style = MaterialTheme.typography.labelMedium,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: TrainingSession) {
    ArenaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.type.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(s.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${formatDateTime(s.timestamp)} · ${formatDuration(s.durationSec)} · performance ${s.performance}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("+${formatNumber(s.xp)} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (s.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            for (pr in s.note.split("; ")) Pill(pr, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
fun HistoryScreen(back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val stats = o.stats
    val sessions = o.state.sessions
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableStateOf<Long?>(null) }

    val todayDate = LocalDate.ofEpochDay(o.today)
    val month = YearMonth.from(todayDate).plusMonths(monthOffset.toLong())
    val activeDays = remember(sessions) { sessions.map { it.epochDay }.toSet() }
    val monthStart = month.atDay(1).toEpochDay()
    val monthEnd = month.atEndOfMonth().toEpochDay()
    val monthSessions = sessions.filter { it.epochDay in monthStart..monthEnd }
    val shown = (selectedDay?.let { day -> sessions.filter { it.epochDay == day } } ?: sessions).reversed().take(150)

    ScreenScaffold("Training history", back, subtitle = "${stats.totalSessions} sessions logged", scroll = false) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Current streak", "${stats.streak}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
                    StatTile("Total sessions", formatNumber(stats.totalSessions), Modifier.weight(1f))
                    StatTile("Total XP", formatNumber(stats.totalXp), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Combinations", formatNumber(stats.comboReps), Modifier.weight(1f))
                    StatTile("Simulated fights", formatNumber(stats.fights), Modifier.weight(1f))
                    StatTile("Best streak", "${stats.bestStreak}", Modifier.weight(1f))
                }
            }
            item {
                SectionHeader("Weekly activity")
                Spacer(Modifier.height(8.dp))
                ArenaCard {
                    WeekBars(sessions, o.today)
                    Spacer(Modifier.height(6.dp))
                    val weekSessions = ChallengeEngine.sessionsInPeriod(true, sessions, o.today)
                    Text(
                        "${weekSessions.size} sessions · ${formatNumber(weekSessions.sumOf { it.xp })} XP · ${weekSessions.map { it.epochDay }.toSet().size} training days this week",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SectionHeader("Monthly activity")
                Spacer(Modifier.height(8.dp))
                ArenaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GhostButton("<", { monthOffset--; selectedDay = null })
                        Text(
                            "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${month.year}",
                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                        )
                        GhostButton(">", { monthOffset++; selectedDay = null }, enabled = monthOffset < 0)
                    }
                    Spacer(Modifier.height(8.dp))
                    MonthCalendar(month, activeDays, o.today, selectedDay) { day -> selectedDay = if (selectedDay == day) null else day }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${monthSessions.map { it.epochDay }.toSet().size} training days · ${monthSessions.size} sessions · ${formatNumber(monthSessions.sumOf { it.xp })} XP this month. Tap a day to filter the log.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                val day = selectedDay
                SectionHeader(
                    if (day != null) "Sessions on ${formatDate(LocalDate.ofEpochDay(day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())}" else "Session log",
                    trailing = if (day != null) "Show all" else null, onTrailingClick = { selectedDay = null }
                )
            }
            if (shown.isEmpty()) {
                item {
                    ArenaCard {
                        Text(
                            if (selectedDay != null) "No sessions on this day." else "No sessions yet. Everything you complete in Train and Fight Lab is logged here.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(shown, key = { it.id }) { s -> SessionRow(s) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun ChallengesScreen(back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val fighter = o.state.fighter
    ScreenScaffold("Challenges", back, subtitle = "Daily missions and weekly objectives") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Daily done", "${o.stats.dailyCompleted}", Modifier.weight(1f))
            StatTile("Weekly done", "${o.stats.weeklyCompleted}", Modifier.weight(1f))
            StatTile("Mission badges", "${fighter?.badges ?: 0}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.tertiary)
        }
        SectionHeader("Today's mission", trailing = "Resets at midnight")
        ArenaCard(accent = if (o.missionComplete) ArenaColors.Win else MaterialTheme.colorScheme.primary) {
            for (status in o.daily) ChallengeRow(status, showReward = true)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("COMPLETE ALL THREE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Bonus XP and a mission badge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (o.missionComplete) "CLAIMED" else "+${ChallengeEngine.SWEEP_BONUS_XP} XP",
                    style = MaterialTheme.typography.titleLarge, color = if (o.missionComplete) ArenaColors.Win else MaterialTheme.colorScheme.primary
                )
            }
        }
        SectionHeader("Weekly challenges", trailing = "Monday to Sunday")
        ArenaCard {
            for (status in o.weekly) ChallengeRow(status, showReward = true)
        }
        Text(
            "Rewards are paid automatically the moment a challenge is completed. Progress is calculated from your training log.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CoachScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val s = o.stats
    ScreenScaffold("Smart coach", back, subtitle = "Rule-based advice from your own numbers") {
        ArenaCard {
            Text("WHAT THE COACH SEES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            KeyValue("Defense accuracy", if (s.defenseAccuracy < 0) "No data yet" else "${s.defenseAccuracy}%")
            KeyValue("Reaction", if (s.avgReactionMs == 0) "No data yet" else "${s.avgReactionMs} ms average, ${s.reactionAccuracy}% accurate")
            KeyValue("Clinch IQ", if (s.clinchSessions == 0) "No data yet" else "${s.clinchIq}")
            KeyValue("Late-round stamina", if (s.avgFightStamina < 0) "No fights yet" else "${s.avgFightStamina} over your last fights")
            KeyValue("Strongest", s.levels.maxByOrNull { it.value }?.let { "${it.key.label} ${it.value}" } ?: "--")
            KeyValue("Weakest", s.levels.minByOrNull { it.value }?.let { "${it.key.label} ${it.value}" } ?: "--", valueColor = MaterialTheme.colorScheme.error)
        }
        SectionHeader("Recommendations", trailing = "${o.tips.size}")
        o.tips.forEachIndexed { index, tip ->
            ArenaCard(accent = if (index == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary) {
                if (index == 0) Pill("Top priority", color = MaterialTheme.colorScheme.tertiary)
                Text(tip.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(tip.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                GhostButton(CoachEngine.destinationLabel(tip.destination), { go(tip.destination) })
            }
        }
    }
}
