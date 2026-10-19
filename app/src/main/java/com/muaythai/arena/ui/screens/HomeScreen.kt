package com.muaythai.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.engine.ChallengeEngine
import com.muaythai.arena.domain.engine.ClubEngine
import com.muaythai.arena.domain.engine.CoachEngine
import com.muaythai.arena.domain.engine.FightCampEngine
import com.muaythai.arena.domain.model.ChallengeStatus
import com.muaythai.arena.domain.model.RecordKey
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.RankBadge
import com.muaythai.arena.ui.components.RatingRing
import com.muaythai.arena.ui.components.RopeDivider
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StadiumHeader
import com.muaythai.arena.ui.components.StatRow
import com.muaythai.arena.ui.components.formatDay
import com.muaythai.arena.ui.components.formatNumber
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.ClubViewModel
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

/** One challenge with a tick box and a progress bar. Shared by Home and the Challenges screen. */
@Composable
fun ChallengeRow(status: ChallengeStatus, modifier: Modifier = Modifier, showReward: Boolean = false) {
    val done = status.completed
    Column(modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(18.dp).background(if (done) ArenaColors.Win else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (done) Text("✓", style = MaterialTheme.typography.labelMedium, color = ArenaColors.Black)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                status.template.title, style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (done) "DONE" else "${status.progress}/${status.template.target}",
                style = MaterialTheme.typography.labelMedium,
                color = if (done) ArenaColors.Win else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(5.dp))
        ProgressBar(status.fraction, color = if (done) ArenaColors.Win else MaterialTheme.colorScheme.primary, height = 4.dp)
        if (showReward) {
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("+${status.template.xp} XP")
                Pill("+${status.template.coins} coins", color = MaterialTheme.colorScheme.tertiary)
                Pill("+${status.template.rankPoints} rank pts", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val clubVm: ClubViewModel = arenaViewModel()
    val club by clubVm.ui.collectAsStateWithLifecycle()
    val stats = o.stats
    val gold = MaterialTheme.colorScheme.tertiary

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        StadiumHeader {
            Text("MUAY THAI RANK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(stats.rank, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stats.rank.label.uppercase(), style = MaterialTheme.typography.headlineLarge,
                        color = if (stats.rank.elite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
                    )
                    Text("LEVEL ${o.level.level}  ·  ${o.name.uppercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    "${formatNumber(o.level.xpIntoLevel)} / ${formatNumber(o.level.xpForNext)}",
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(5.dp))
            ProgressBar(o.level.fraction, height = 10.dp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${stats.streak} day streak", filled = stats.streak > 0)
                Pill("${formatNumber(stats.coins)} coins", color = MaterialTheme.colorScheme.tertiary)
                Pill("${stats.wins}W ${stats.losses}L", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RopeDivider()

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ---- club: sign-ups, surveys, notices
            SectionHeader("Club", trailing = "Open club", onTrailingClick = { go(Routes.CLUB) })
            ArenaCard(accent = MaterialTheme.colorScheme.primary, onClick = { go(Routes.CLUB) }) {
                val booking = ClubEngine.upcoming(club.club.events, club.today).firstOrNull { !it.cancelled && ClubEngine.userSignup(it, club.club.signups) != null }
                if (booking != null) {
                    Text("YOUR NEXT BOOKING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(booking.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("${formatDay(booking.epochDay, club.today)} · ${ClubEngine.timeRange(booking)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("SIGN UP FOR A CLASS", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("${club.counts.upcomingEvents} classes and events are open for booking in the next two weeks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("${club.counts.openSurveys} open surveys", filled = club.counts.openSurveys > 0)
                    Pill("${club.counts.unreadNotices} new notices", filled = club.counts.unreadNotices > 0)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Sign up", { go(Routes.SCHEDULE) }, Modifier.weight(1f))
                GhostButton("Take a survey", { go(Routes.SURVEYS) }, Modifier.weight(1f))
            }

            // ---- today's mission
            SectionHeader("Today's mission", trailing = "All challenges", onTrailingClick = { go(Routes.CHALLENGES) })
            ArenaCard(accent = if (o.missionComplete) ArenaColors.Win else MaterialTheme.colorScheme.primary, highlight = !o.missionComplete) {
                Text("COMPLETE:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (status in o.daily) ChallengeRow(status)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("REWARD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(
                        if (o.missionComplete) "CLAIMED" else "+${ChallengeEngine.SWEEP_BONUS_XP} XP",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (o.missionComplete) ArenaColors.Win else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ---- fight readiness
            SectionHeader("Fight readiness")
            ArenaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingRing(o.readiness.overall, o.readiness.label)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatRow("Technique", o.readiness.technique)
                        StatRow("Defense", o.readiness.defense)
                        StatRow("Conditioning", o.readiness.conditioning)
                        StatRow("Fight IQ", o.readiness.fightIq)
                        StatRow("Clinch", o.readiness.clinch)
                    }
                }
            }

            // ---- weekly objective
            val weekly = o.weekly.firstOrNull { !it.completed } ?: o.weekly.firstOrNull()
            if (weekly != null) {
                SectionHeader("Weekly objective")
                ArenaCard(onClick = { go(Routes.CHALLENGES) }) {
                    ChallengeRow(weekly, showReward = true)
                }
            }

            // ---- fight camp
            SectionHeader("Fight camp")
            val camp = o.state.camp
            if (camp.active) {
                val opponent = Opponents.get(camp.opponentId)
                ArenaCard(accent = MaterialTheme.colorScheme.primary, onClick = { go(Routes.CAMP) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (camp.isFightDay) "FIGHT DAY" else "DAY ${camp.day} OF ${FightCampEngine.DAYS}",
                                style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("vs ${opponent?.name ?: "your opponent"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Pill(FightCampEngine.freshnessLabel(camp.freshness), color = if (FightCampEngine.isOvertrained(camp)) MaterialTheme.colorScheme.error else ArenaColors.Win)
                    }
                    Spacer(Modifier.height(8.dp))
                    StatRow("Camp readiness", camp.readiness)
                }
            } else {
                ArenaCard(onClick = { go(Routes.CAMP) }) {
                    Text("NO CAMP IN PROGRESS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Seven days to prepare for ${o.nextOpponent.name}. Balance training and recovery, then fight.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("OPEN FIGHT CAMP  >", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            // ---- recommendation
            val tip = o.tips.firstOrNull()
            if (tip != null) {
                SectionHeader("Training recommendation", trailing = "Smart coach", onTrailingClick = { go(Routes.COACH) })
                ArenaCard(accent = gold) {
                    Text(tip.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(tip.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    GhostButton(CoachEngine.destinationLabel(tip.destination), { go(tip.destination) })
                }
            }

            // ---- personal record
            SectionHeader("Personal record", trailing = "All records", onTrailingClick = { go(Routes.RECORDS) })
            val record = o.state.records[RecordKey.HIGHEST_FIGHT_SCORE] ?: o.state.records[RecordKey.FASTEST_REACTION]
                ?: o.state.records.values.maxByOrNull { it.achievedAt }
            ArenaCard(onClick = { go(Routes.RECORDS) }) {
                if (record == null) {
                    Text("NO RECORDS YET", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Finish a session and your first record goes on the board.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(record.key.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(record.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${formatNumber(record.value)}${if (record.key.unit.isEmpty()) "" else " " + record.key.unit}",
                            style = MaterialTheme.typography.headlineMedium, color = gold
                        )
                    }
                }
            }

            // ---- quick actions
            SectionHeader("Quick actions")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Next fight", { go(Routes.opponent(o.nextOpponent.id)) }, Modifier.weight(1f))
                GhostButton("Reaction", { go(Routes.REACTION) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Combinations", { go(Routes.COMBOS) }, Modifier.weight(1f))
                GhostButton("Defense Lab", { go(Routes.DEFENSE) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("3-min round", { go(Routes.ROUND) }, Modifier.weight(1f))
                GhostButton("Clinch Lab", { go(Routes.CLINCH) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
