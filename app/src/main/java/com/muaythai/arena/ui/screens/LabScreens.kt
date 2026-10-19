package com.muaythai.arena.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.engine.ClinchEngine
import com.muaythai.arena.domain.engine.DefenseEngine
import com.muaythai.arena.domain.engine.ReactionEngine
import com.muaythai.arena.domain.model.DefenseResponse
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
import com.muaythai.arena.ui.components.RewardDialog
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.ClinchViewModel
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.DefenseViewModel
import com.muaythai.arena.ui.vm.ReactionPhase
import com.muaythai.arena.ui.vm.ReactionViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

private val letters = listOf("A", "B", "C", "D", "E")

/** Shared "leave and lose this session?" guard for the labs. */
@Composable
private fun LeaveGuard(active: Boolean, what: String, back: () -> Unit): () -> Unit {
    var asking by remember { mutableStateOf(false) }
    BackHandler(enabled = active) { asking = true }
    if (asking) {
        ConfirmDialog(
            title = "Leave $what?",
            message = "The session is not finished, so it will not be recorded.",
            confirmLabel = "Leave", destructive = true,
            onConfirm = { asking = false; back() },
            onDismiss = { asking = false }
        )
    }
    return { if (active) asking = true else back() }
}

// =====================================================================================
// Defense Lab
// =====================================================================================

@Composable
fun DefenseScreen(back: () -> Unit) {
    val vm: DefenseViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val dash: DashboardViewModel = arenaViewModel()
    val o by dash.overview.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val leave = LeaveGuard(ui.started && ui.reward == null, "Defense Lab", back)

    ui.reward?.let { RewardDialog(it, onDismiss = back) }

    ScreenScaffold("Defense Lab", leave, subtitle = if (ui.started) "Scenario ${ui.index + 1} of ${ui.scenarios.size}" else "Recognise the attack, choose the answer") {
        val scenario = ui.current
        if (scenario == null) {
            ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                Text("READ. DECIDE. DEFEND.", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${DefenseEngine.SESSION_SIZE} incoming attacks. Pick the correct response for each one and get immediate feedback from the coach.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                KeyValue("Each correct read", "+${DefenseEngine.IQ_XP_PER_CORRECT} Fight IQ XP, plus Defense")
                KeyValue("Perfect session", "+60 bonus XP")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Sessions", "${o.stats.defenseSessions}", Modifier.weight(1f))
                StatTile("Accuracy", if (o.stats.defenseAccuracy < 0) "--" else "${o.stats.defenseAccuracy}%", Modifier.weight(1f))
                StatTile("Best", if (o.stats.bestDefenseAccuracy == 0) "--" else "${o.stats.bestDefenseAccuracy}%", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.tertiary)
            }
            PrimaryButton("Start session", { vm.start() }, Modifier.fillMaxWidth())
            return@ScreenScaffold
        }

        ProgressBar((ui.index + if (ui.chosen != null) 1 else 0).toFloat() / ui.scenarios.size, color = ArenaColors.Win, height = 6.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("${ui.correct} correct", color = ArenaColors.Win)
            Pill("${ui.answered - ui.correct} missed", color = MaterialTheme.colorScheme.error)
        }

        ArenaCard(highlight = true) {
            Text("INCOMING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(scenario.attack.uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(scenario.prompt, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("CHOOSE:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        scenario.options.forEachIndexed { index, option ->
            val chosen = ui.chosen
            val state = when {
                chosen == null -> ChoiceState.NEUTRAL
                index == scenario.correctIndex -> ChoiceState.CORRECT
                index == chosen -> ChoiceState.WRONG
                else -> ChoiceState.DIMMED
            }
            ChoiceCard(
                text = option, leading = letters[index], state = state, enabled = chosen == null,
                onClick = { if (vm.choose(index)) feedback.success() else feedback.error() }
            )
        }

        val chosen = ui.chosen
        if (chosen != null) {
            val right = chosen == scenario.correctIndex
            ArenaCard(accent = if (right) ArenaColors.Win else MaterialTheme.colorScheme.error) {
                Text(if (right) "CORRECT" else "CORRECT ANSWER", style = MaterialTheme.typography.labelMedium, color = if (right) ArenaColors.Win else MaterialTheme.colorScheme.error)
                Text(scenario.options[scenario.correctIndex].uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(scenario.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (right) {
                    Spacer(Modifier.height(8.dp))
                    Pill("+${DefenseEngine.IQ_XP_PER_CORRECT} Fight IQ XP", color = ArenaColors.Win)
                }
            }
            PrimaryButton(if (ui.isLast) "Finish session" else "Next attack", { vm.next() }, Modifier.fillMaxWidth(), enabled = !ui.saving)
        }
    }
}

// =====================================================================================
// Clinch Lab
// =====================================================================================

@Composable
fun ClinchScreen(back: () -> Unit) {
    val vm: ClinchViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val dash: DashboardViewModel = arenaViewModel()
    val o by dash.overview.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val leave = LeaveGuard(ui.started && ui.reward == null, "Clinch Lab", back)

    ui.reward?.let { RewardDialog(it, onDismiss = back) }

    ScreenScaffold("Clinch Lab", leave, subtitle = if (ui.started) "${ui.current?.topic ?: ""} · ${ui.index + 1} of ${ui.scenarios.size}" else "Control the neck, control the fight") {
        val scenario = ui.current
        if (scenario == null) {
            ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                Text("CLINCH IQ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (o.stats.clinchSessions == 0) "--" else "${o.stats.clinchIq}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.padding(4.dp))
                    Text(ClinchEngine.iqLabel(o.stats.clinchIq).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                }
                ProgressBar(o.stats.clinchIq / 100f)
                Spacer(Modifier.height(6.dp))
                Text("Average decision quality over your last ten clinch sessions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ArenaCard {
                Text(
                    "${ClinchEngine.SESSION_SIZE} positions covering entries, position control, knee selection, balance, turns, " +
                        "off-balancing, escapes and defensive posture. Every option has its own outcome: some dominate, some get you kneed.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                KeyValue("Win the challenge", "Score ${ClinchEngine.WIN_THRESHOLD}% or better")
                KeyValue("Record", "${o.stats.clinchWins} won of ${o.stats.clinchSessions}")
            }
            PrimaryButton("Lock up", { vm.start() }, Modifier.fillMaxWidth())
            return@ScreenScaffold
        }

        ProgressBar((ui.index + if (ui.chosen != null) 1 else 0).toFloat() / ui.scenarios.size, color = ArenaColors.Win, height = 6.dp)
        ArenaCard(highlight = true) {
            Text(scenario.topic.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(scenario.situation, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }

        val chosen = ui.chosen
        scenario.options.forEachIndexed { index, option ->
            val state = when {
                chosen == null -> ChoiceState.NEUTRAL
                index == chosen && option.quality >= 2 -> ChoiceState.CORRECT
                index == chosen -> ChoiceState.WRONG
                option.quality == 3 -> ChoiceState.SELECTED
                else -> ChoiceState.DIMMED
            }
            ChoiceCard(
                text = option.label, state = state, enabled = chosen == null,
                trailing = if (chosen != null && (index == chosen || option.quality == 3)) ClinchEngine.qualityLabel(option.quality) else null,
                onClick = { if (vm.choose(index) >= 2) feedback.success() else feedback.error() }
            )
        }

        if (chosen != null) {
            val picked = scenario.options[chosen]
            val best = scenario.options.firstOrNull { it.quality == 3 }
            ArenaCard(accent = if (picked.quality >= 2) ArenaColors.Win else MaterialTheme.colorScheme.error) {
                Text("OUTCOME: ${ClinchEngine.qualityLabel(picked.quality)}", style = MaterialTheme.typography.labelMedium, color = if (picked.quality >= 2) ArenaColors.Win else MaterialTheme.colorScheme.error)
                Text(picked.outcome, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (best != null && best != picked) {
                    Spacer(Modifier.height(8.dp))
                    Text("BEST OPTION: ${best.label.uppercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(best.outcome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SESSION SCORE ${ui.score}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            PrimaryButton(if (ui.isLast) "Finish session" else "Next position", { vm.next() }, Modifier.fillMaxWidth(), enabled = !ui.saving)
        }
    }
}

// =====================================================================================
// Reaction Arena
// =====================================================================================

@Composable
private fun ResponseButton(response: DefenseResponse, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .heightIn(min = 84.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(response.label.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
    }
}

@Composable
fun ReactionScreen(back: () -> Unit) {
    val vm: ReactionViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val dash: DashboardViewModel = arenaViewModel()
    val o by dash.overview.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val active = ui.phase != ReactionPhase.IDLE && ui.phase != ReactionPhase.DONE
    val leave = LeaveGuard(active, "Reaction Arena") { vm.abandon(); back() }
    var rewardSeen by remember { mutableStateOf(false) }

    // One sound per answer.
    LaunchedEffect(ui.results.size) {
        val last = ui.results.lastOrNull() ?: return@LaunchedEffect
        if (last.correct) feedback.success() else feedback.error()
    }
    LaunchedEffect(ui.phase) { if (ui.phase == ReactionPhase.WAITING && ui.index == 0) rewardSeen = false }

    val reward = ui.reward
    if (reward != null && !rewardSeen) RewardDialog(reward, onDismiss = { rewardSeen = true }, buttonText = "See results")

    ScreenScaffold(
        "Reaction Arena", leave,
        subtitle = if (active) "Attack ${ui.index + 1} of ${ui.total}" else "See it, answer it, beat the clock"
    ) {
        when (ui.phase) {
            ReactionPhase.IDLE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Rank", if (o.stats.reactionSessions == 0) "--" else ReactionEngine.rank(o.stats.avgReactionMs, o.stats.reactionAccuracy.coerceAtLeast(0)).label, Modifier.weight(1.3f), valueColor = MaterialTheme.colorScheme.primary)
                    StatTile("Average", if (o.stats.avgReactionMs == 0) "--" else "${o.stats.avgReactionMs} ms", Modifier.weight(1f))
                    StatTile("Best", if (o.stats.bestReactionMs == 0) "--" else "${o.stats.bestReactionMs} ms", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.tertiary)
                }
                SectionHeader("Know your answers")
                ArenaCard {
                    for ((attack, response) in ReactionEngine.answers) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(attack, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(response.label.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                    }
                }
                Text(
                    "${ReactionEngine.SESSION_SIZE} attacks appear at random intervals. Tap the correct defense as fast as you can. " +
                        "You have ${ReactionEngine.TIMEOUT_MS / 1000.0} seconds for each.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PrimaryButton("Enter the arena", { vm.start() }, Modifier.fillMaxWidth())
            }

            ReactionPhase.WAITING, ReactionPhase.PROMPT, ReactionPhase.FEEDBACK -> {
                ProgressBar(ui.results.size.toFloat() / ui.total, color = ArenaColors.Win, height = 6.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("${ui.correct} correct", color = ArenaColors.Win)
                    Pill("${ui.results.size - ui.correct} wrong", color = MaterialTheme.colorScheme.error)
                    if (ui.liveAverage > 0) Pill("avg ${ui.liveAverage} ms", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val pulse by rememberInfiniteTransition(label = "wait").animateFloat(
                    initialValue = 0.35f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "pulse"
                )
                val prompt = ui.prompt
                val last = ui.last
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 170.dp).clip(MaterialTheme.shapes.medium)
                        .background(
                            when {
                                ui.phase == ReactionPhase.PROMPT -> MaterialTheme.colorScheme.primary
                                ui.phase == ReactionPhase.FEEDBACK && last != null && last.correct -> ArenaColors.Win.copy(alpha = 0.25f)
                                ui.phase == ReactionPhase.FEEDBACK -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        ui.phase == ReactionPhase.PROMPT && prompt != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("INCOMING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                            Text(prompt.attack, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
                        }
                        ui.phase == ReactionPhase.FEEDBACK && last != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when { last.correct -> "${last.ms} MS"; last.timedOut -> "TOO SLOW"; else -> "WRONG" },
                                style = MaterialTheme.typography.displaySmall,
                                color = if (last.correct) ArenaColors.Win else MaterialTheme.colorScheme.error
                            )
                            if (!last.correct && prompt != null) {
                                Text("${prompt.attack} → ${prompt.correct.label.uppercase()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        else -> Text("GET READY", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.alpha(pulse))
                    }
                }

                // The four answers keep their place while a prompt is showing; between prompts they are greyed out.
                val options = prompt?.options
                if (options != null && options.size == 4) {
                    val live = ui.phase == ReactionPhase.PROMPT
                    for (rowIndex in 0..1) {
                        Row(Modifier.alpha(if (live) 1f else 0.4f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResponseButton(options[rowIndex * 2], { vm.answer(options[rowIndex * 2]) }, Modifier.weight(1f), enabled = live)
                            ResponseButton(options[rowIndex * 2 + 1], { vm.answer(options[rowIndex * 2 + 1]) }, Modifier.weight(1f), enabled = live)
                        }
                    }
                } else {
                    Text("Watch the panel above. The attack can come at any moment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
                }
            }

            ReactionPhase.DONE -> {
                val s = ui.summary
                if (s != null) {
                    ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                        Text("REACTION RANK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s.rank.label.uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        Text("Ranks: Beginner · Fast · Sharp · Elite · Lightning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Accuracy", "${s.accuracy}%", Modifier.weight(1f))
                        StatTile("Average", if (s.averageMs == 0) "--" else "${s.averageMs} ms", Modifier.weight(1f))
                        StatTile("Best", if (s.bestMs == 0) "--" else "${s.bestMs} ms", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.tertiary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Correct", "${s.correct}", Modifier.weight(1f), valueColor = ArenaColors.Win)
                        StatTile("Wrong", "${s.wrong}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.error)
                        StatTile("All-time best", if (o.stats.bestReactionMs == 0) "--" else "${o.stats.bestReactionMs} ms", Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Done", back, Modifier.weight(1f))
                    PrimaryButton("Go again", { vm.start() }, Modifier.weight(1f))
                }
            }
        }
    }
}
