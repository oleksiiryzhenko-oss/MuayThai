package com.muaythai.arena.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.engine.FightSimulationEngine
import com.muaythai.arena.domain.engine.OpponentEngine
import com.muaythai.arena.domain.model.CornerInstruction
import com.muaythai.arena.domain.model.CornerReport
import com.muaythai.arena.domain.model.Damage
import com.muaythai.arena.domain.model.FightPhase
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.FightState
import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ChoiceCard
import com.muaythai.arena.ui.components.ChoiceState
import com.muaythai.arena.ui.components.ConfirmDialog
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.RatingRing
import com.muaythai.arena.ui.components.RewardDialog
import com.muaythai.arena.ui.components.RopeDivider
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StadiumHeader
import com.muaythai.arena.ui.components.StatRow
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.ThaiPattern
import com.muaythai.arena.ui.components.formatDate
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.FightViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

private val letters = listOf("A", "B", "C", "D")

@Composable
private fun difficultyColor(label: String): Color = when (label) {
    "BRUTAL", "HARD" -> MaterialTheme.colorScheme.error
    "EVEN" -> ArenaColors.Warn
    else -> ArenaColors.Win
}

@Composable
private fun OpponentRow(opponent: Opponent, userOverall: Int, wins: Int, losses: Int, onClick: () -> Unit) {
    val difficulty = OpponentEngine.difficultyLabel(userOverall, opponent)
    ArenaCard(accent = if (wins > 0) ArenaColors.Win else null, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(opponent.name.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("\"${opponent.nickname}\" · ${opponent.archetype.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(difficulty, color = difficultyColor(difficulty))
                    if (wins + losses > 0) Pill("${wins}W ${losses}L", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (wins > 0) Pill("Beaten", color = ArenaColors.Win)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${opponent.overall}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text("OVR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FightLabScreen(go: (String) -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val records = o.state.opponentRecords
    val fighter = o.state.fighter

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        StadiumHeader {
            Text("FIGHT LAB", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Text("Five rounds. Every decision changes the fight.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Wins", "${fighter?.wins ?: 0}", Modifier.weight(1f))
                StatTile("Losses", "${fighter?.losses ?: 0}", Modifier.weight(1f))
                StatTile("Draws", "${fighter?.draws ?: 0}", Modifier.weight(1f))
                StatTile("Streak", "${fighter?.winStreak ?: 0}", Modifier.weight(1f))
            }
        }
        RopeDivider()

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Next on your ladder")
            val next = o.nextOpponent
            ArenaCard(highlight = true, onClick = { go(Routes.opponent(next.id)) }) {
                ThaiPattern()
                Spacer(Modifier.height(8.dp))
                Text("${o.name.uppercase()}  VS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(next.name.uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("\"${next.nickname}\" · ${next.archetype.label} · ${next.origin}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Study opponent", { go(Routes.opponent(next.id)) }, Modifier.weight(1f))
                    GhostButton(if (o.state.camp.active) "Open camp" else "Fight camp", { go(Routes.CAMP) }, Modifier.weight(1f))
                }
            }

            for (status in OpponentEngine.tierStatuses(records)) {
                SectionHeader(OpponentEngine.tierName(status.tier), trailing = if (status.unlocked) "${status.beaten}/${status.total} beaten" else "Locked")
                if (!status.unlocked) {
                    ArenaCard {
                        Text("LOCKED", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Beat ${status.neededFromPrevious} more opponent(s) from ${OpponentEngine.tierName(status.tier - 1)} to earn fights at this level.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    for (opponent in Opponents.inTier(status.tier)) {
                        val record = records[opponent.id]
                        OpponentRow(opponent, o.stats.overall, record?.wins ?: 0, record?.losses ?: 0) { go(Routes.opponent(opponent.id)) }
                    }
                }
            }

            val recent = o.state.fights.takeLast(5).reversed()
            if (recent.isNotEmpty()) {
                SectionHeader("Recent fights")
                ArenaCard {
                    for (f in recent) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Pill(if (f.won) "Win" else if (f.draw) "Draw" else "Loss", color = if (f.won) ArenaColors.Win else if (f.draw) ArenaColors.Warn else MaterialTheme.colorScheme.error, filled = f.won)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.opponentName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("${f.method}, round ${f.endRound} · ${formatDate(f.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${f.overall}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun OpponentScreen(id: Int, go: (String) -> Unit, back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val opponent = Opponents.get(id)
    if (opponent == null) {
        ScreenScaffold("Opponent", back) { Text("This opponent could not be found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val unlocked = OpponentEngine.isUnlocked(opponent, o.state.opponentRecords)
    val scout = OpponentEngine.scout(opponent)
    val record = o.state.opponentRecords[opponent.id]
    val difficulty = OpponentEngine.difficultyLabel(o.stats.overall, opponent)
    val camp = o.state.camp

    ScreenScaffold(opponent.name, back, subtitle = "${OpponentEngine.tierName(opponent.tier)} · ${opponent.origin}") {
        ArenaCard(highlight = true) {
            ThaiPattern()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(opponent.archetype.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("\"${opponent.nickname}\"", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(opponent.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(10.dp))
                RatingRing(opponent.overall, "OVERALL", size = 96.dp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(difficulty, color = difficultyColor(difficulty))
                Pill("You ${o.stats.overall}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (record != null) Pill("${record.wins}W ${record.losses}L vs you", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionHeader("Style")
        ArenaCard {
            for (trait in opponent.archetype.traits) {
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text("◆", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(trait, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(6.dp))
            KeyValue("Strongest weapon", opponent.strongestWeapon)
        }

        SectionHeader("Attributes")
        ArenaCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, value) in opponent.statList()) StatRow(label, value)
            }
        }

        SectionHeader("Scouting report")
        ArenaCard(accent = MaterialTheme.colorScheme.tertiary) {
            Text(scout.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(scout.threat, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            KeyValue("Recommended", scout.recommended.joinToString(", ") { it.label }, valueColor = ArenaColors.Win)
            if (scout.avoid.isNotEmpty()) KeyValue("Avoid", scout.avoid.joinToString(", ") { it.label }, valueColor = MaterialTheme.colorScheme.error)
        }

        if (!unlocked) {
            ArenaCard {
                Text("LOCKED", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Win ${OpponentEngine.WINS_TO_UNLOCK_NEXT_TIER} fights in ${OpponentEngine.tierName(opponent.tier - 1)} to face this opponent.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            PrimaryButton("Fight now", { go(Routes.fight(opponent.id, false)) }, Modifier.fillMaxWidth())
            GhostButton(
                when {
                    camp.active && camp.opponentId == opponent.id -> "Continue fight camp (day ${camp.day})"
                    camp.active -> "Camp in progress vs ${Opponents.get(camp.opponentId)?.name ?: "another opponent"}"
                    else -> "Prepare with a 7-day fight camp"
                },
                { go(Routes.CAMP) }, Modifier.fillMaxWidth()
            )
            Text(
                "A camp gives temporary attribute bonuses for this fight and decides how fresh you start.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================================
// The fight
// =====================================================================================

@Composable
private fun DamageStrip(label: String, damage: Damage, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        for ((zone, value) in listOf("HEAD" to damage.head, "BODY" to damage.body, "LEGS" to damage.legs)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(zone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(38.dp))
                ProgressBar(value / 100f, color = if (value >= 70) MaterialTheme.colorScheme.error else ArenaColors.Warn, height = 5.dp)
            }
        }
    }
}

@Composable
private fun FightHud(state: FightState, userName: String) {
    val userRounds = state.rounds.count { r -> r.judges.count { it.first > it.second } >= 2 }
    val oppRounds = state.rounds.size - userRounds
    ArenaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(userName.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("STAMINA ${state.userStamina}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$userRounds - $oppRounds", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(state.opponent.name.substringBefore(' ').uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("STAMINA ${state.oppStamina}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressBar(state.userStamina / 100f, Modifier.weight(1f), color = if (state.userStamina < 35) MaterialTheme.colorScheme.error else ArenaColors.Win)
            ProgressBar(state.oppStamina / 100f, Modifier.weight(1f), color = if (state.oppStamina < 35) MaterialTheme.colorScheme.error else ArenaColors.Win)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DamageStrip("Damage to you", state.userDamage, Modifier.weight(1f))
            DamageStrip("Damage to them", state.oppDamage, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RoundDots(state: FightState) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (n in 1..state.totalRounds) {
            val record = state.rounds.firstOrNull { it.number == n }
            val color = when {
                record == null && n == state.round -> MaterialTheme.colorScheme.primary
                record == null -> MaterialTheme.colorScheme.surfaceVariant
                record.judges.count { it.first > it.second } >= 2 -> ArenaColors.Win
                else -> MaterialTheme.colorScheme.error
            }
            Box(Modifier.weight(1f).height(6.dp).background(color))
        }
    }
}

@Composable
private fun CornerPanel(report: CornerReport, onPick: (CornerInstruction) -> Unit) {
    ArenaCard(accent = if (report.userWonRound) ArenaColors.Win else MaterialTheme.colorScheme.error) {
        Text("CORNER · END OF ROUND ${report.roundNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(if (report.userWonRound) "YOU TOOK THE ROUND" else "YOU LOST THE ROUND", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        KeyValue("Round score", report.scoreLine)
        KeyValue("Successful attacks", "${report.successfulAttacks} landed")
        KeyValue("Your best weapon", report.userWeapon)
        KeyValue("Their best weapon", report.opponentWeapon, valueColor = MaterialTheme.colorScheme.error)
        KeyValue("Stamina", "You ${report.stamina}%  ·  them ${report.opponentStamina}%")
        KeyValue("Damage taken", "Head ${report.userDamage.head} · Body ${report.userDamage.body} · Legs ${report.userDamage.legs}")
        KeyValue("Damage dealt", "Head ${report.oppDamage.head} · Body ${report.oppDamage.body} · Legs ${report.oppDamage.legs}")
    }
    ArenaCard(accent = MaterialTheme.colorScheme.tertiary) {
        Text("TACTICAL RECOMMENDATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        Text(report.recommendation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
    Text("CORNER INSTRUCTION FOR THE NEXT ROUND:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    for (instruction in CornerInstruction.entries) {
        ChoiceCard(
            text = instruction.label, subtitle = instruction.blurb,
            state = if (instruction == report.suggested) ChoiceState.SELECTED else ChoiceState.NEUTRAL,
            trailing = if (instruction == report.suggested) "Corner's call" else null,
            onClick = { onPick(instruction) }
        )
    }
}

@Composable
private fun ResultPanel(result: FightResult, state: FightState, onDone: () -> Unit) {
    val verdict = when { result.won -> "WIN"; result.draw -> "DRAW"; else -> "LOSS" }
    val color = when { result.won -> ArenaColors.Win; result.draw -> ArenaColors.Warn; else -> MaterialTheme.colorScheme.error }
    ArenaCard(highlight = true) {
        ThaiPattern(color = color)
        Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(verdict, style = MaterialTheme.typography.displayLarge, color = color)
            Text(result.method.uppercase() + if (result.method != "Decision") " · ROUND ${result.endRound}" else "", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("vs ${result.opponentName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text("SCORECARDS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (card in result.scorecards) Text(card, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(10.dp))
        Text("ROUND RESULTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            result.roundResults.forEachIndexed { index, r ->
                Column(
                    Modifier.weight(1f).clip(MaterialTheme.shapes.small)
                        .background((if (r == "W") ArenaColors.Win else MaterialTheme.colorScheme.error).copy(alpha = 0.18f))
                        .border(1.dp, if (r == "W") ArenaColors.Win else MaterialTheme.colorScheme.error, MaterialTheme.shapes.small)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("R${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(r, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    SectionHeader("Performance")
    ArenaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingRing(result.overall, "FIGHT\nPERFORMANCE", size = 140.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow("Striking", result.striking)
                StatRow("Defense", result.defense)
                StatRow("Kicking", result.kicking)
                StatRow("Clinch", result.clinch)
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow("Fight IQ score", result.fightIq, color = MaterialTheme.colorScheme.tertiary)
            StatRow("Stamina management", result.stamina)
            StatRow("Tactical score", result.tactical)
        }
        Spacer(Modifier.height(10.dp))
        val landed = state.rounds.sumOf { it.userLanded }
        val thrown = state.rounds.sumOf { it.userThrown }.coerceAtLeast(1)
        val oppLanded = state.rounds.sumOf { it.opponentLanded }
        val oppThrown = state.rounds.sumOf { it.opponentThrown }.coerceAtLeast(1)
        val kicksLanded = state.rounds.sumOf { it.kicksLanded }
        val kicksThrown = state.rounds.sumOf { it.kicksThrown }.coerceAtLeast(1)
        val clinchTotal = state.rounds.sumOf { it.clinchTotal }
        KeyValue("Strike efficiency", "$landed of $thrown landed (${landed * 100 / thrown}%)")
        KeyValue("Defense efficiency", "${100 - oppLanded * 100 / oppThrown}% of their strikes avoided")
        KeyValue("Kick efficiency", "$kicksLanded of $kicksThrown landed (${kicksLanded * 100 / kicksThrown}%)")
        KeyValue("Clinch efficiency", if (clinchTotal == 0) "No clinch exchanges" else "${state.rounds.sumOf { it.clinchWon }} of $clinchTotal exchanges won")
        KeyValue("Main strategy", result.mainStrategy.label)
        KeyValue("Reward", "+${result.xp} XP", valueColor = MaterialTheme.colorScheme.primary)
    }

    SectionHeader("Round by round")
    for (r in state.rounds) {
        ArenaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ROUND ${r.number}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(r.judges.joinToString("  ") { "${it.first}-${it.second}" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "${r.userStrategy.label} vs ${r.opponentStrategy.label} · " +
                    when { r.matchup > 0 -> "good read"; r.matchup < 0 -> "bad match-up"; else -> "even match-up" },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            for (line in r.log.drop(1)) Text("· $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    PrimaryButton("Leave the ring", onDone, Modifier.fillMaxWidth())
}

@Composable
fun FightScreen(opponentId: Int, campFight: Boolean, back: () -> Unit) {
    val vm: FightViewModel = arenaViewModel()
    LaunchedEffect(opponentId, campFight) { vm.start(opponentId, campFight) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val dash: DashboardViewModel = arenaViewModel()
    val o by dash.overview.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    var asking by remember { mutableStateOf(false) }
    var rewardSeen by remember { mutableStateOf(false) }

    val state = ui.fight
    val inProgress = state != null && ui.result == null
    BackHandler(enabled = inProgress) { asking = true }
    if (asking) {
        ConfirmDialog(
            title = "Walk out of the fight?",
            message = "Leaving now abandons the bout. It will not count on your record and you earn nothing.",
            confirmLabel = "Abandon", destructive = true,
            onConfirm = { asking = false; back() }, onDismiss = { asking = false }
        )
    }

    // Bell at the start of each round, impact feedback on each exchange.
    LaunchedEffect(state?.round) { if (state != null && state.phase == FightPhase.PRE_ROUND) feedback.bell() }
    LaunchedEffect(state?.step) {
        val outcome = state?.lastOutcome ?: return@LaunchedEffect
        if (outcome.success) feedback.success() else feedback.error()
    }

    val reward = ui.reward
    if (reward != null && !rewardSeen) RewardDialog(reward, onDismiss = { rewardSeen = true }, buttonText = "See the scorecards")

    if (state == null) {
        ScreenScaffold("Fight", back) {
            Text(if (ui.missing) "This fight could not be set up." else "Walking to the ring...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val title = if (ui.result != null) "Fight result" else "Round ${state.round} of ${state.totalRounds}"
    ScreenScaffold(title, { if (inProgress) asking = true else back() }, subtitle = "vs ${state.opponent.name}" + if (state.isCampFight) " · camp fight" else "") {
        val result = ui.result
        if (result != null) {
            ResultPanel(result, state, back)
            return@ScreenScaffold
        }

        RoundDots(state)
        FightHud(state, o.name)

        when (state.phase) {
            FightPhase.PRE_ROUND -> {
                ArenaCard(accent = MaterialTheme.colorScheme.error) {
                    Text("OPPONENT STRATEGY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    Text(state.opponentStrategy.label.uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(state.opponentStrategy.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.corner?.let {
                        Spacer(Modifier.height(8.dp))
                        Pill("Corner said: ${it.label}", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text("CHOOSE YOUR STRATEGY FOR THIS ROUND:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (strategy in Strategy.entries) {
                    ChoiceCard(
                        text = strategy.label, subtitle = strategy.blurb,
                        trailing = if (FightSimulationEngine.cornerMatchesStrategy(state.corner, strategy)) "Corner plan" else null,
                        onClick = { vm.chooseStrategy(strategy) }
                    )
                }
            }

            FightPhase.EVENT -> {
                val event = state.currentEvent
                if (event != null) {
                    ArenaCard(highlight = true) {
                        Text("SITUATION ${state.eventIndex + 1} OF ${state.pendingEvents.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(event.text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("CHOOSE:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    event.options.forEachIndexed { index, option ->
                        ChoiceCard(text = option.label, leading = letters.getOrElse(index) { "" }, subtitle = option.tactic.label, onClick = { vm.chooseOption(index) })
                    }
                }
            }

            FightPhase.EVENT_RESULT -> {
                val outcome = state.lastOutcome
                val event = state.currentEvent
                if (outcome != null) {
                    if (event != null) Text(event.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ArenaCard(accent = if (outcome.success) ArenaColors.Win else MaterialTheme.colorScheme.error) {
                        Text(if (outcome.success) "IT WORKS" else "IT BACKFIRES", style = MaterialTheme.typography.labelLarge, color = if (outcome.success) ArenaColors.Win else MaterialTheme.colorScheme.error)
                        Text(outcome.text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Pill(
                            when (outcome.quality) { 3 -> "Ideal read"; 2 -> "Sound choice"; 1 -> "Questionable"; else -> "Wrong idea" },
                            color = if (outcome.quality >= 2) ArenaColors.Win else ArenaColors.Warn
                        )
                    }
                    state.stoppage?.let {
                        ArenaCard(highlight = true) {
                            Text("THE REFEREE WAVES IT OFF", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    PrimaryButton(
                        when {
                            state.stoppage != null -> "Go to the result"
                            state.eventIndex + 1 < state.pendingEvents.size -> "Fight on"
                            state.round >= state.totalRounds -> "Final bell"
                            else -> "End of round"
                        },
                        { vm.advance() }, Modifier.fillMaxWidth(), enabled = !ui.saving
                    )
                }
            }

            FightPhase.CORNER -> {
                val report = ui.report
                if (report != null) CornerPanel(report) { vm.corner(it) }
            }

            FightPhase.FINISHED -> {
                Text("The judges are adding up the cards...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
