package com.muaythai.arena.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.engine.CombinationEngine
import com.muaythai.arena.domain.model.Combination
import com.muaythai.arena.domain.model.Strike
import com.muaythai.arena.domain.model.StrikeType
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ConfirmDialog
import com.muaythai.arena.ui.components.DifficultyDots
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.MessageBanner
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.ProgressBar
import com.muaythai.arena.ui.components.RewardDialog
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.formatNumber
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.ComboPracticeViewModel
import com.muaythai.arena.ui.vm.CombinationViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

private enum class ComboFilter(val label: String) { ALL("All"), FAVORITES("Favorites"), MINE("My builds") }

@Composable
private fun MasteryBlock(reps: Int) {
    val mastery = CombinationEngine.mastery(reps)
    val next = CombinationEngine.nextMilestone(reps)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("MASTERY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("$mastery%", style = MaterialTheme.typography.titleLarge, color = if (mastery >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
    }
    Spacer(Modifier.height(4.dp))
    ProgressBar(mastery / 100f, color = if (mastery >= 100) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, height = 6.dp)
    Spacer(Modifier.height(4.dp))
    Text(
        if (next != null) "${formatNumber(reps)} repetitions · next milestone ${next.reps} · reward +${next.xp} XP"
        else "${formatNumber(reps)} repetitions · every milestone reached",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CombinationCard(
    combo: Combination, onPractice: () -> Unit, onEdit: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit
) {
    val difficulty = CombinationEngine.difficulty(combo.strikes)
    ArenaCard(accent = if (combo.favorite) MaterialTheme.colorScheme.primary else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(combo.name.uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyDots(difficulty)
                    Text(CombinationEngine.difficultyLabel(difficulty).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+${CombinationEngine.xpValue(combo.strikes)} XP / 10 REPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (combo.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (combo.favorite) "Remove from favorites" else "Mark as favorite",
                    tint = if (combo.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(combo.notation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        MasteryBlock(combo.reps)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Practice", onPractice, Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit or rename", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun CombosScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: CombinationViewModel = arenaViewModel()
    val combos by vm.combinations.collectAsStateWithLifecycle()
    var filterName by rememberSaveable { mutableStateOf(ComboFilter.ALL.name) }
    val filter = ComboFilter.entries.firstOrNull { it.name == filterName } ?: ComboFilter.ALL
    var deleting by remember { mutableStateOf<Combination?>(null) }

    deleting?.let { combo ->
        ConfirmDialog(
            title = "Delete combination",
            message = "Delete \"${combo.name}\"? Its ${combo.reps} repetitions of mastery will be lost.",
            confirmLabel = "Delete", destructive = true,
            onConfirm = { vm.delete(combo.id); deleting = null },
            onDismiss = { deleting = null }
        )
    }

    val shown = when (filter) {
        ComboFilter.ALL -> combos
        ComboFilter.FAVORITES -> combos.filter { it.favorite }
        ComboFilter.MINE -> combos.filter { it.custom }
    }

    ScreenScaffold("Combination Lab", back, subtitle = "${combos.size} combinations · ${formatNumber(combos.sumOf { it.reps })} reps", scroll = false) {
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                PrimaryButton("Build a new combination", { go(Routes.comboEdit(0)) }, Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (f in ComboFilter.entries) {
                        val selected = f == filter
                        Box(
                            Modifier.weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                .clickable { filterName = f.name }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(f.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    ArenaCard {
                        Text(
                            when (filter) {
                                ComboFilter.FAVORITES -> "No favorites yet. Tap the heart on any combination to pin it here and use it for round call-outs."
                                ComboFilter.MINE -> "You have not built a combination yet. Start with two or three strikes you already throw well."
                                ComboFilter.ALL -> "No combinations. Build your first one."
                            },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(shown, key = { it.id }) { combo ->
                CombinationCard(
                    combo,
                    onPractice = { go(Routes.comboPractice(combo.id)) },
                    onEdit = { go(Routes.comboEdit(combo.id)) },
                    onFavorite = { vm.toggleFavorite(combo.id) },
                    onDelete = { deleting = combo }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboBuilderScreen(id: Long, back: () -> Unit) {
    val vm: CombinationViewModel = arenaViewModel()
    LaunchedEffect(id) { vm.startEdit(id) }
    val b by vm.builder.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    val feedback = LocalFeedback.current

    LaunchedEffect(b.saved) { if (b.saved) { feedback.success(); back() } }

    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
                Text("ADD A STRIKE", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("${b.strikes.size}/${CombinationEngine.MAX_STRIKES} in the combination. Tap to add; the sheet stays open.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (type in StrikeType.entries) {
                    Spacer(Modifier.height(12.dp))
                    Text("${type.label.uppercase()}S", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    for (pair in Strike.entries.filter { it.type == type }.chunked(2)) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (strike in pair) {
                                Box(
                                    Modifier.weight(1f)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { feedback.strike(); vm.add(strike) }
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(strike.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Done", { picking = false }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    ScreenScaffold(if (id == 0L) "New combination" else "Edit combination", back, subtitle = "Combination Lab") {
        OutlinedTextField(
            value = b.name,
            onValueChange = { vm.setName(it) },
            singleLine = true,
            label = { Text("Name") },
            placeholder = { Text(CombinationEngine.suggestName(b.strikes)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Strikes", "${b.strikes.size}", Modifier.weight(1f))
            StatTile("Difficulty", CombinationEngine.difficultyLabel(b.difficulty), Modifier.weight(1.4f))
            StatTile("XP / 10 reps", "+${b.xpValue}", Modifier.weight(1.2f), valueColor = MaterialTheme.colorScheme.primary)
        }

        SectionHeader("Sequence", trailing = if (b.strikes.isNotEmpty()) "Clear" else null, onTrailingClick = { vm.clear() })
        if (b.strikes.isEmpty()) {
            ArenaCard {
                Text("Empty. Add at least ${CombinationEngine.MIN_STRIKES} strikes. A good combination changes level or weapon at least once.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        b.strikes.forEachIndexed { index, strike ->
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text((index + 1).toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(strike.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(strike.type.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.move(index, -1) }, enabled = index > 0) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move earlier")
                }
                IconButton(onClick = { vm.move(index, 1) }, enabled = index < b.strikes.size - 1) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move later")
                }
                IconButton(onClick = { vm.removeAt(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove strike", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        GhostButton("Add strike", { picking = true }, Modifier.fillMaxWidth(), enabled = b.strikes.size < CombinationEngine.MAX_STRIKES)

        if (b.strikes.isNotEmpty()) {
            ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                Text("PREVIEW", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(b.strikes.joinToString(" → ") { it.label }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        b.error?.let { MessageBanner(it, color = MaterialTheme.colorScheme.error) }
        if (id != 0L) {
            Text("Renaming keeps your mastery. Changing the strikes makes it a new combination, so repetitions start again.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryButton("Save combination", { vm.save() }, Modifier.fillMaxWidth(), enabled = b.strikes.size >= CombinationEngine.MIN_STRIKES)
    }
}

@Composable
fun ComboPracticeScreen(id: Long, back: () -> Unit) {
    val vm: ComboPracticeViewModel = arenaViewModel()
    LaunchedEffect(id) { vm.load(id) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val combo = ui.combination

    val leave: () -> Unit = { if (ui.reps > 0 && ui.reward == null) vm.finish() else back() }
    BackHandler(enabled = ui.reps > 0 && ui.reward == null) { vm.finish() }
    ui.reward?.let { RewardDialog(it, onDismiss = back) }

    if (combo == null) {
        ScreenScaffold("Practice", back) { Text("This combination no longer exists.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }

    ScreenScaffold(combo.name, leave, subtitle = "Practice · ${CombinationEngine.difficultyLabel(CombinationEngine.difficulty(combo.strikes))}") {
        ArenaCard(highlight = true) {
            combo.strikes.forEachIndexed { index, strike ->
                val active = index == ui.strikeIndex
                val thrown = index < ui.strikeIndex
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (thrown) "✓" else (index + 1).toString(), style = MaterialTheme.typography.titleMedium,
                        color = if (thrown) ArenaColors.Win else MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp)
                    )
                    Text(
                        strike.label.uppercase(),
                        style = if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StrikePad(
                label = combo.strikes.getOrNull(ui.strikeIndex)?.label ?: "Throw",
                caption = "tap to throw",
                onTap = { if (vm.hit()) feedback.success() else feedback.strike() },
                enabled = !ui.saving
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("This session", "${ui.reps}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
            StatTile("All time", formatNumber(ui.totalReps), Modifier.weight(1f))
            StatTile("XP so far", "+${CombinationEngine.xpValue(combo.strikes) * ui.reps / 10}", Modifier.weight(1f))
        }
        ArenaCard { MasteryBlock(ui.totalReps) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("+10 bag reps", { vm.addReps(10) }, Modifier.weight(1f), enabled = !ui.saving)
            PrimaryButton("Finish", { vm.finish() }, Modifier.weight(1f), enabled = ui.reps > 0 && !ui.saving)
        }
        Pill("Each full run through the sequence is one repetition", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
