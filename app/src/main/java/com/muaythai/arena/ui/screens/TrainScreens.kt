package com.muaythai.arena.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.content.Programs
import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.engine.ClinchEngine
import com.muaythai.arena.domain.engine.MuayThaiTrainingEngine
import com.muaythai.arena.domain.engine.ReactionEngine
import com.muaythai.arena.domain.model.TechniqueCategory
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.DifficultyDots
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.RewardDialog
import com.muaythai.arena.ui.components.RopeDivider
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StadiumHeader
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.formatDuration
import com.muaythai.arena.ui.components.formatNumber
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.DrillViewModel
import com.muaythai.arena.ui.vm.ProgramViewModel
import com.muaythai.arena.ui.vm.RoundViewModel
import com.muaythai.arena.ui.vm.arenaViewModel
import kotlinx.coroutines.launch

/** The big round pad the player hits to count a strike. It recoils on every tap. */
@Composable
fun StrikePad(label: String, caption: String, onTap: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(190.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .border(3.dp, color, CircleShape)
            .clickable(enabled = enabled) {
                onTap()
                scope.launch {
                    scale.snapTo(0.9f)
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                Text(label.uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
                Text(caption.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SkillCard(index: Int, title: String, blurb: String, stat: String, onClick: () -> Unit) {
    ArenaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(index.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(48.dp))
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Pill(stat)
        }
    }
}

@Composable
fun TrainScreen(go: (String) -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val s = o.stats
    val reps = o.state.techniqueReps
    val suggested = MuayThaiTrainingEngine.weakestTechnique(reps)
    val strikingMastery = MuayThaiTrainingEngine.categoryMastery(Techniques.all, reps)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        StadiumHeader {
            Text("TRAIN", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Text("Seven skill areas. Every session feeds your fighter's attributes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Sessions", formatNumber(s.totalSessions), Modifier.weight(1f))
                StatTile("Reps drilled", formatNumber(s.techniqueReps + s.comboReps), Modifier.weight(1f))
                StatTile("Minutes", formatNumber(s.totalMinutes), Modifier.weight(1f))
            }
        }
        RopeDivider()
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Suggested drill")
            ArenaCard(accent = MaterialTheme.colorScheme.tertiary, onClick = { go(Routes.technique(suggested.id)) }) {
                Text(suggested.name.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Your least developed weapon: ${MuayThaiTrainingEngine.mastery(suggested, reps[suggested.id] ?: 0)}% mastery. " + suggested.cue,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionHeader("Skill categories")
            SkillCard(1, "Striking", "Twelve weapons from jab to elbow, each with its own mastery.", "$strikingMastery%") { go(Routes.STRIKING) }
            SkillCard(2, "Combination Lab", "Build, save and master your own combinations.", "${formatNumber(s.comboReps)} reps") { go(Routes.COMBOS) }
            SkillCard(3, "Defense Lab", "Read the incoming attack and choose the right answer.", if (s.defenseAccuracy < 0) "New" else "${s.defenseAccuracy}%") { go(Routes.DEFENSE) }
            SkillCard(4, "Clinch Lab", "Entries, control, knees, turns and escapes.", if (s.clinchSessions == 0) "New" else "IQ ${s.clinchIq}") { go(Routes.CLINCH) }
            SkillCard(5, "Reaction Arena", "Beat the clock with the correct defense.",
                if (s.reactionSessions == 0) "New" else ReactionEngine.rank(s.avgReactionMs, s.reactionAccuracy.coerceAtLeast(0)).label) { go(Routes.REACTION) }
            SkillCard(6, "Round Work", "Timed rounds with combination call-outs.", "${s.rounds} rounds") { go(Routes.ROUND) }
            SkillCard(7, "Programs", "${Programs.all.size} guided sessions for specific goals.", "${s.programs} done") { go(Routes.PROGRAMS) }

            if (s.clinchSessions > 0) {
                Text("Clinch IQ: ${ClinchEngine.iqLabel(s.clinchIq)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun StrikingScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val reps = o.state.techniqueReps

    ScreenScaffold("Striking", back, subtitle = "${Techniques.all.size} techniques in ${TechniqueCategory.entries.size} categories") {
        for (category in TechniqueCategory.entries) {
            val techniques = Techniques.inCategory(category)
            val mastery = MuayThaiTrainingEngine.categoryMastery(techniques, reps)
            ArenaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(category.label.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(category.type.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("$mastery%", style = MaterialTheme.typography.headlineSmall, color = if (mastery >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(mastery / 100f, height = 5.dp)
                Spacer(Modifier.height(8.dp))
                for (t in techniques) {
                    val m = MuayThaiTrainingEngine.mastery(t, reps[t.id] ?: 0)
                    Row(
                        Modifier.fillMaxWidth().clickable { go(Routes.technique(t.id)) }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("${t.target} · ${t.range} range · +${t.xpReward} XP per set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DifficultyDots(t.difficulty)
                        Spacer(Modifier.width(10.dp))
                        Text("$m%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
fun TechniqueScreen(id: String, back: () -> Unit) {
    val vm: DrillViewModel = arenaViewModel()
    LaunchedEffect(id) { vm.load(id) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val technique = ui.technique

    // Leaving with unsaved reps saves them instead of throwing the work away.
    val leave: () -> Unit = { if (ui.reps > 0 && ui.reward == null) vm.finish() else back() }
    BackHandler(enabled = ui.reps > 0 && ui.reward == null) { vm.finish() }

    ui.reward?.let { RewardDialog(it, onDismiss = back) }

    if (technique == null) {
        ScreenScaffold("Technique", back) { Text("This technique could not be found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }

    ScreenScaffold(technique.name, leave, subtitle = technique.category.label) {
        ArenaCard(accent = MaterialTheme.colorScheme.primary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MASTERY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("${ui.mastery}%", style = MaterialTheme.typography.headlineMedium, color = if (ui.mastery >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(ui.mastery / 100f)
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatNumber(ui.previousReps + ui.reps)} / ${formatNumber(technique.masteryReps)} repetitions",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ArenaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DIFFICULTY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(128.dp))
                DifficultyDots(technique.difficulty)
            }
            Spacer(Modifier.height(4.dp))
            KeyValue("Target area", technique.target)
            KeyValue("Range", technique.range)
            KeyValue("Recommended setup", technique.setup)
            KeyValue("Common mistake", technique.mistake, valueColor = MaterialTheme.colorScheme.error)
            KeyValue("XP reward", "+${technique.xpReward} XP per set of ${MuayThaiTrainingEngine.SET_SIZE}")
        }

        SectionHeader("Drill")
        ArenaCard(highlight = true) {
            Text("COACH'S CUE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(technique.cue, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StrikePad(
                    label = ui.reps.toString(), caption = "tap each rep",
                    onTap = {
                        vm.rep()
                        if ((ui.reps + 1) % MuayThaiTrainingEngine.SET_SIZE == 0) feedback.success() else feedback.strike()
                    },
                    enabled = !ui.saving
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SET ${ui.setsDone + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("${ui.reps % MuayThaiTrainingEngine.SET_SIZE}/${MuayThaiTrainingEngine.SET_SIZE}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            ProgressBar(ui.setProgress, color = ArenaColors.Win, height = 6.dp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("+10 bag reps", { vm.addSet() }, Modifier.weight(1f), enabled = !ui.saving)
                PrimaryButton("Finish drill", { vm.finish() }, Modifier.weight(1f), enabled = ui.reps > 0 && !ui.saving)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap the pad for each repetition you throw, or log ten at a time after a bag round.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoundScreen(back: () -> Unit) {
    val vm: RoundViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()

    BackHandler(enabled = ui.started && ui.reward == null) { vm.finish() }
    ui.reward?.let { reward ->
        RewardDialog(reward, onDismiss = { vm.reset() }, buttonText = "Another round")
    }

    ScreenScaffold("Round Work", { if (ui.started && ui.reward == null) vm.finish() else back() }, subtitle = "Shadowboxing or bag rounds with call-outs") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (length in MuayThaiTrainingEngine.roundLengths) {
                val selected = ui.lengthSec == length
                Box(
                    Modifier.weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        .clickable(enabled = !ui.started) { vm.selectLength(length) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${length / 60} MIN", style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        ArenaCard(highlight = ui.running) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when { ui.running -> "FIGHT"; ui.started -> "PAUSED"; else -> "READY" },
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary
                )
                Text(formatDuration(ui.remaining), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                ProgressBar(ui.fraction, color = if (ui.remaining <= 10 && ui.started) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, height = 10.dp)
            }
        }

        ArenaCard(accent = MaterialTheme.colorScheme.tertiary) {
            Text("CALL-OUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                if (ui.started) ui.callout else "Start the round and throw each combination as it appears. A new one comes every eight seconds.",
                style = if (ui.started) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ui.running) GhostButton("Pause", { vm.pause() }, Modifier.weight(1f))
            else PrimaryButton(if (ui.started) "Resume" else "Start round", { vm.start() }, Modifier.weight(1f), enabled = !ui.saving)
            GhostButton("End round", { vm.finish() }, Modifier.weight(1f), enabled = ui.started && !ui.saving)
        }
        Text(
            "Call-outs come from your favourite combinations. A finished round builds Conditioning and Stamina; " +
                "ending early still counts if you worked for at least 30 seconds.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProgramsScreen(go: (String) -> Unit, back: () -> Unit) {
    ScreenScaffold("Programs", back, subtitle = "${Programs.all.size} guided sessions") {
        for (p in Programs.all) {
            ArenaCard(onClick = { go(Routes.program(p.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Pill(p.level)
                }
                Text(p.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(p.focus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Pill("${p.steps.size} steps · ${formatDuration(p.totalSeconds)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Pill("+${p.xp} XP")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Builds " + p.gains.keys.joinToString(", ") { it.label },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProgramRunScreen(id: String, back: () -> Unit) {
    val vm: ProgramViewModel = arenaViewModel()
    LaunchedEffect(id) { vm.load(id) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val program = ui.program
    val inProgress = ui.completedSteps > 0 && ui.reward == null

    BackHandler(enabled = inProgress) { vm.finishEarly() }
    ui.reward?.let { RewardDialog(it, onDismiss = back) }

    if (program == null) {
        ScreenScaffold("Program", back) { Text("This program could not be found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val step = ui.step

    ScreenScaffold(program.name, { if (inProgress) vm.finishEarly() else back() }, subtitle = "${program.focus} · ${program.level}") {
        ProgressBar(ui.completedSteps.toFloat() / program.steps.size, color = ArenaColors.Win)
        if (step != null) {
            ArenaCard(highlight = true) {
                Text("STEP ${ui.stepIndex + 1} OF ${program.steps.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(step.title.uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(step.instruction, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatDuration(ui.remaining), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                ProgressBar(1f - ui.remaining.toFloat() / step.seconds.coerceAtLeast(1), height = 8.dp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(if (ui.running) "Pause" else if (ui.remaining == 0) "Time is up" else "Start timer", { vm.toggle() }, Modifier.weight(1f), enabled = ui.remaining > 0 && !ui.saving)
                    PrimaryButton(if (ui.isLastStep) "Finish program" else "Step done", { vm.completeStep() }, Modifier.weight(1f), enabled = !ui.saving)
                }
            }
        }

        SectionHeader("Session plan")
        ArenaCard {
            program.steps.forEachIndexed { index, s ->
                val done = index < ui.completedSteps
                val current = index == ui.stepIndex && ui.reward == null
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (done) "✓" else (index + 1).toString(), style = MaterialTheme.typography.titleMedium,
                        color = if (done) ArenaColors.Win else if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        s.title, style = MaterialTheme.typography.titleMedium,
                        color = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatDuration(s.seconds), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        ArenaCard {
            Text("REWARD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+${program.xp} XP for the full session", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            for ((attr, points) in program.gains) KeyValue(attr.label, "+${points * 10} attribute XP")
            Text("Partial sessions earn a proportional share.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (inProgress) GhostButton("Finish early and bank ${ui.completedSteps} step(s)", { vm.finishEarly() }, Modifier.fillMaxWidth(), enabled = !ui.saving)
    }
}
