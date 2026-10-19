package com.muaythai.arena.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.engine.FightCampEngine
import com.muaythai.arena.domain.engine.OpponentEngine
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ChoiceCard
import com.muaythai.arena.ui.components.ChoiceState
import com.muaythai.arena.ui.components.ConfirmDialog
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.MessageBanner
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.RewardDialog
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StatRow
import com.muaythai.arena.ui.components.ThaiPattern
import com.muaythai.arena.ui.components.formatNumber
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.CampViewModel
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

@Composable
fun CampScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: CampViewModel = arenaViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val reward by vm.reward.collectAsStateWithLifecycle()
    val dash: DashboardViewModel = arenaViewModel()
    val o by dash.overview.collectAsStateWithLifecycle()
    var abandoning by remember { mutableStateOf(false) }
    val camp = ui.camp

    reward?.let { RewardDialog(it, onDismiss = { vm.dismissReward() }, buttonText = "Next morning") }
    if (abandoning) {
        ConfirmDialog(
            title = "Abandon camp?",
            message = "The fight bonuses you built this camp will be lost. Attribute XP you already earned is kept.",
            confirmLabel = "Abandon", destructive = true,
            onConfirm = { abandoning = false; vm.abandon() }, onDismiss = { abandoning = false }
        )
    }

    // ---------------------------------------------------------------- no camp yet
    if (!camp.active) {
        ScreenScaffold("Fight Camp", back, subtitle = "Seven days to get ready") {
            ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                ThaiPattern()
                Spacer(Modifier.height(8.dp))
                Text("HOW CAMP WORKS", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Days 1 to 6: you have ${FightCampEngine.ENERGY_PER_DAY} energy each day. Every session adds a bonus for the fight and earns permanent attribute XP, but costs freshness.\n\n" +
                        "Repeat the same session in one day and it gives less. Let freshness drop under ${FightCampEngine.OVERTRAINED_BELOW}% and you are overtrained: sessions are half as effective and you start the fight tired.\n\n" +
                        "Day 7: fight day. Your freshness becomes your starting stamina.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SectionHeader("Choose your opponent")
            val unlocked = Opponents.all.filter { OpponentEngine.isUnlocked(it, o.state.opponentRecords) }
            val candidates = (listOf(ui.nextOpponent) + unlocked.filter { it.id != ui.nextOpponent.id }.takeLast(5).reversed())
            for (opponent in candidates) {
                val isNext = opponent.id == ui.nextOpponent.id
                ChoiceCard(
                    text = opponent.name,
                    subtitle = "\"${opponent.nickname}\" · ${opponent.archetype.label} · overall ${opponent.overall}",
                    state = if (isNext) ChoiceState.SELECTED else ChoiceState.NEUTRAL,
                    trailing = if (isNext) "Next on ladder" else OpponentEngine.difficultyLabel(o.stats.overall, opponent),
                    onClick = { vm.start(opponent.id) }
                )
            }
            Text("Tap an opponent to open camp against them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val opponent = ui.opponent
    val overtrained = FightCampEngine.isOvertrained(camp)
    val freshColor = when {
        overtrained -> MaterialTheme.colorScheme.error
        camp.freshness < 65 -> ArenaColors.Warn
        else -> ArenaColors.Win
    }

    ScreenScaffold(
        if (camp.isFightDay) "Fight day" else "Camp · day ${camp.day} of ${FightCampEngine.DAYS}", back,
        subtitle = "vs ${opponent?.name ?: "your opponent"}"
    ) {
        // day tracker
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (day in 1..FightCampEngine.DAYS) {
                val color = when {
                    day < camp.day -> ArenaColors.Win
                    day == camp.day -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(6.dp).background(color))
                    Spacer(Modifier.height(3.dp))
                    Text(if (day == FightCampEngine.DAYS) "FIGHT" else "D$day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        ArenaCard(accent = freshColor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FRESHNESS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Pill(FightCampEngine.freshnessLabel(camp.freshness), color = freshColor)
                Spacer(Modifier.width(8.dp))
                Text("${camp.freshness}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(camp.freshness / 100f, color = freshColor)
            Spacer(Modifier.height(10.dp))
            StatRow("Camp readiness", camp.readiness)
            Spacer(Modifier.height(8.dp))
            KeyValue("Starting stamina", "${FightCampEngine.startStamina(camp)}% on fight day")
            if (!camp.isFightDay) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("ENERGY TODAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(128.dp))
                    for (i in 1..maxOf(FightCampEngine.ENERGY_PER_DAY, camp.energy)) {
                        Box(
                            Modifier.padding(end = 5.dp).size(width = 22.dp, height = 12.dp)
                                .background(if (i <= camp.energy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    Text("${camp.energy} left", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        message?.let { MessageBanner(it, color = if (overtrained) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }

        if (camp.bonuses.isNotEmpty()) {
            SectionHeader("Fight bonuses earned")
            ArenaCard {
                for ((attr, bonus) in camp.bonuses) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(attr.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("${o.stats.levels[attr] ?: 0}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("  +$bonus", style = MaterialTheme.typography.titleMedium, color = ArenaColors.Win)
                    }
                }
            }
        }

        if (camp.isFightDay) {
            ArenaCard(highlight = true) {
                ThaiPattern()
                Spacer(Modifier.height(8.dp))
                Text("THE WORK IS DONE", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "You walk out against ${opponent?.name ?: "your opponent"} with ${FightCampEngine.startStamina(camp)}% stamina and every bonus above.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Walk out", { go(Routes.fight(camp.opponentId, true)) }, Modifier.fillMaxWidth())
            }
        } else {
            SectionHeader("Train", trailing = "${formatNumber(ui.coins)} coins")
            if (ui.suggested.isNotEmpty()) {
                Text(
                    "Suggested against ${opponent?.archetype?.label ?: "this opponent"}: " + ui.suggested.joinToString(", ") { it.label },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary
                )
            }
            for (training in CampTraining.entries) {
                val attr = training.attribute
                val gain = FightCampEngine.projectedGain(camp, training)
                val subtitle = if (attr == null) "${training.blurb}  Freshness +${-training.fatigue}"
                else "${training.blurb}  ${attr.label} +$gain · freshness -${training.fatigue + if (overtrained) 5 else 0}"
                ChoiceCard(
                    text = training.label, subtitle = subtitle,
                    state = when {
                        camp.energy <= 0 -> ChoiceState.DIMMED
                        training in ui.suggested -> ChoiceState.SELECTED
                        else -> ChoiceState.NEUTRAL
                    },
                    enabled = camp.energy > 0,
                    trailing = if (training in ui.suggested) "Suggested" else null,
                    onClick = { vm.train(training) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(
                    "Extra session (${FightCampEngine.EXTRA_ENERGY_COST} coins)", { vm.buyEnergy() }, Modifier.weight(1f),
                    enabled = !camp.boughtEnergyToday && ui.coins >= FightCampEngine.EXTRA_ENERGY_COST
                )
                PrimaryButton("End day", { vm.endDay() }, Modifier.weight(1f))
            }
            Text(
                "Unused energy turns into extra rest overnight.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionHeader("Camp log")
        ArenaCard {
            for (line in camp.log.takeLast(8).reversed()) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
        GhostButton("Abandon camp", { abandoning = true }, Modifier.fillMaxWidth())
    }
}
