package com.muaythai.arena.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muaythai.arena.domain.engine.AchievementEngine
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.reminder.ReminderScheduler
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ConfirmDialog
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.MessageBanner
import com.muaythai.arena.ui.components.Pill
import com.muaythai.arena.ui.components.RankBadge
import com.muaythai.arena.ui.components.RatingRing
import com.muaythai.arena.ui.components.RopeDivider
import com.muaythai.arena.ui.components.ScreenScaffold
import com.muaythai.arena.ui.components.SectionHeader
import com.muaythai.arena.ui.components.StadiumHeader
import com.muaythai.arena.ui.components.StatRow
import com.muaythai.arena.ui.components.StatTile
import com.muaythai.arena.ui.components.ThaiPattern
import com.muaythai.arena.ui.components.formatDate
import com.muaythai.arena.ui.components.formatNumber
import com.muaythai.arena.ui.nav.Routes
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.vm.DashboardViewModel
import com.muaythai.arena.ui.vm.SettingsViewModel
import com.muaythai.arena.ui.vm.arenaViewModel

@Composable
fun ProfileScreen(go: (String) -> Unit) {
    val vm: DashboardViewModel = arenaViewModel()
    val o by vm.overview.collectAsStateWithLifecycle()
    val stats = o.stats
    val fighter = o.state.fighter
    val gold = MaterialTheme.colorScheme.tertiary
    var renaming by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("FIGHTER NAME", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it.take(18) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.rename(draft); renaming = false }, enabled = draft.trim().isNotEmpty()) {
                    Text("SAVE", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false }) { Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        StadiumHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FIGHTER PROFILE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                IconButton(onClick = { go(Routes.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.name.uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                IconButton(onClick = { draft = o.name; renaming = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Change name", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(stats.rank, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("LEVEL ${stats.level}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(stats.rank.label.uppercase(), style = MaterialTheme.typography.titleMedium, color = if (stats.rank.elite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    Text("${formatNumber(stats.totalXp)} XP · ${stats.archetype.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RatingRing(stats.overall, "OVERALL", size = 92.dp)
            }
        }
        RopeDivider()

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Fight record")
            ArenaCard {
                ThaiPattern()
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Wins", "${fighter?.wins ?: 0}", Modifier.weight(1f), valueColor = ArenaColors.Win)
                    StatTile("Losses", "${fighter?.losses ?: 0}", Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.error)
                    StatTile("Draws", "${fighter?.draws ?: 0}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                KeyValue("Win streak", "${fighter?.winStreak ?: 0}  (best ${fighter?.bestWinStreak ?: 0})")
                KeyValue("Stoppage wins", "${stats.stoppageWins}")
                KeyValue("Opponents beaten", "${stats.opponentsBeaten} of 30")
                KeyValue("Fight camps", "${stats.campsCompleted} completed")
                KeyValue("Fighting since", fighter?.let { formatDate(it.createdAt) } ?: "--")
            }

            SectionHeader("Fighter archetype")
            ArenaCard(accent = MaterialTheme.colorScheme.primary, onClick = { go(Routes.PROGRESS) }) {
                Text(stats.archetype.label.uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(stats.archetype.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (fighter != null && fighter.style != stats.archetype) {
                    Spacer(Modifier.height(4.dp))
                    Text("Started as ${fighter.style.label}. Your training changed your style.", style = MaterialTheme.typography.bodySmall, color = gold)
                }
            }

            SectionHeader("Main attributes", trailing = "All progress", onTrailingClick = { go(Routes.PROGRESS) })
            ArenaCard {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    for (attr in Attribute.entries.sortedByDescending { stats.levels[it] ?: 0 }) {
                        StatRow(attr.label, stats.levels[attr] ?: 0, modifier = Modifier.clickable { go(Routes.attribute(attr.name)) })
                    }
                }
            }

            SectionHeader("Achievements", trailing = "${o.state.achievements.size}/${AchievementEngine.all.size}", onTrailingClick = { go(Routes.ACHIEVEMENTS) })
            ArenaCard(onClick = { go(Routes.ACHIEVEMENTS) }) {
                val latest = o.state.achievements.entries.sortedByDescending { it.value }.take(4).mapNotNull { AchievementEngine.get(it.key) }
                if (latest.isEmpty()) {
                    Text("No achievements yet. Your first session unlocks one.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (a in latest) {
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("◆", color = if (a.gold) gold else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            SectionHeader("Personal records", trailing = "All records", onTrailingClick = { go(Routes.RECORDS) })
            ArenaCard(onClick = { go(Routes.RECORDS) }) {
                val records = o.state.records.values.sortedByDescending { it.achievedAt }.take(4)
                if (records.isEmpty()) {
                    Text("No records set yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    for (r in records) KeyValue(r.key.label, formatNumber(r.value) + (if (r.key.unit.isEmpty()) "" else " ${r.key.unit}"), valueColor = gold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Coins", formatNumber(stats.coins), Modifier.weight(1f), valueColor = gold)
                StatTile("Mission badges", "${fighter?.badges ?: 0}", Modifier.weight(1f))
                StatTile("Rank points", formatNumber(fighter?.rankPoints ?: 0), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Training history", { go(Routes.HISTORY) }, Modifier.weight(1f))
                GhostButton("Settings", { go(Routes.SETTINGS) }, Modifier.weight(1f))
            }
            SectionHeader("Club")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Club and bookings", { go(Routes.CLUB) }, Modifier.weight(1f))
                GhostButton("Moderator panel", { go(Routes.MODERATOR) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val feedback = LocalFeedback.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { feedback.tap(); onChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(">", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsScreen(go: (String) -> Unit, back: () -> Unit) {
    val vm: SettingsViewModel = arenaViewModel()
    val s by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    var confirmProgress by remember { mutableStateOf(false) }
    var confirmHistory by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.setReminders(true)
            notice = "Reminders on. You will get one notification a day."
        } else {
            vm.setReminders(false)
            notice = "Notifications are blocked for this app, so reminders stay off. You can allow them in system settings."
        }
    }

    if (confirmProgress) {
        ConfirmDialog(
            title = "Reset progression",
            message = "Your level, rank, attributes, fight record, achievements, challenges, records, camp and combination mastery go back to the start. Your name, style and training history are kept. This cannot be undone.",
            confirmLabel = "Reset", destructive = true,
            onConfirm = { confirmProgress = false; vm.resetProgression(); notice = "Progression reset. You are a Novice again." },
            onDismiss = { confirmProgress = false }
        )
    }
    if (confirmHistory) {
        ConfirmDialog(
            title = "Reset training history",
            message = "Every logged session is deleted, along with streaks and challenge progress that depend on it. Level, rank and attributes are kept. This cannot be undone.",
            confirmLabel = "Delete history", destructive = true,
            onConfirm = { confirmHistory = false; vm.resetHistory(); notice = "Training history cleared." },
            onDismiss = { confirmHistory = false }
        )
    }

    ScreenScaffold("Settings", back) {
        notice?.let { MessageBanner(it) }

        SectionHeader("Appearance and feedback")
        ArenaCard {
            ToggleRow("Dark mode", "Swap the red and white look for a dark arena.", s.darkMode) { vm.setDarkMode(it) }
            ToggleRow("Sound effects", "Bells, hits and reward tones.", s.sound) {
                vm.setSound(it)
                if (it) { feedback.soundEnabled = true; feedback.bell() }
            }
            ToggleRow("Vibration", "Haptic feedback on strikes and answers.", s.vibration) {
                vm.setVibration(it)
                if (it) { feedback.vibrationEnabled = true; feedback.success() }
            }
        }

        SectionHeader("Training reminders")
        ArenaCard {
            ToggleRow("Daily reminder", "One local notification a day. Nothing is sent anywhere.", s.reminders) { enable ->
                if (!enable) {
                    vm.setReminders(false)
                    notice = "Reminders off."
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.canNotify(context)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.setReminders(true)
                    notice = "Reminders on. You will get one notification a day."
                }
            }
            if (s.reminders) {
                Text("REMIND ME AROUND", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (hour in listOf(7, 12, 18, 20)) {
                        val selected = s.reminderHour == hour
                        Box(
                            Modifier.weight(1f).clip(MaterialTheme.shapes.small)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                .clickable { vm.setReminderHour(hour) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${hour.toString().padStart(2, '0')}:00", style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        SectionHeader("Data")
        ArenaCard {
            LinkRow("Reset progression", { confirmProgress = true }, danger = true)
            LinkRow("Reset training history", { confirmHistory = true }, danger = true)
        }

        SectionHeader("About")
        ArenaCard {
            LinkRow("Privacy policy", { go(Routes.PRIVACY) })
            LinkRow("About", { go(Routes.ABOUT) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("Offline", color = ArenaColors.Win)
            Pill("No account", color = ArenaColors.Win)
            Pill("Data stays on device", color = ArenaColors.Win)
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    ArenaCard {
        Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PrivacyScreen(back: () -> Unit) {
    ScreenScaffold("Privacy policy", back, subtitle = "Muay Thai · com.muaythai.arena") {
        PolicySection("The short version", "Muay Thai works entirely on your device. It has no accounts, no servers and no internet permission. Nothing you do in the app is sent anywhere.")
        PolicySection("The app works offline", "Every feature, including training, the Fight Lab, Fight Camp, challenges and the smart coach, runs locally. The app does not request network access, so it cannot transmit data.")
        PolicySection("No account is required", "There is no sign-up, no login and no profile on a server. The fighter name you enter is only a label shown inside the app.")
        PolicySection("No personal information is required", "The app does not ask for your real name, email address, phone number, location, contacts, photos or health data, and it does not read them.")
        PolicySection("Club data", "Bookings, survey answers, notices and requests are saved in the same private database on this device. A moderator can type other people's names into a booking list or collect a guest's survey answers on this device; those names and answers also stay on this device. Survey answers, including the optional wellness questions, are visible to anyone who knows the moderator PIN.")
        PolicySection("Sharing from the moderator panel", "The moderator can export a booking list or survey results as plain text. That opens Android's share sheet, and the text goes only to the app the moderator picks there. The app never sends anything by itself.")
        PolicySection("Training data is stored locally", "Your fighter, attributes, sessions, combinations, fight results, achievements, challenges, Fight Camp progress and personal records are saved in a private database on this device. Settings such as theme, sound and reminders are saved in private app preferences.")
        PolicySection("No backend account system", "There is no backend. The app contains no analytics, advertising or tracking libraries, and it shares nothing with third parties.")
        PolicySection("No cloud synchronisation", "The app does not sync to a cloud service and is excluded from cloud backup. If you uninstall it or clear its storage, your progress is permanently deleted. Moving to a new phone with a direct device-to-device transfer may carry the data across.")
        PolicySection("Permissions", "Vibration: optional haptic feedback, which you can switch off in Settings.\n\nNotifications: only if you turn on the daily training reminder. The reminder is scheduled on the device and can be switched off at any time.\n\nRun at startup: used only to restore that reminder after the phone restarts.")
        PolicySection("Deleting your data", "Settings offers Reset progression and Reset training history. You can cancel a booking or delete a request yourself, and the moderator can remove any participant, survey response, survey, notice or request. Uninstalling the app removes everything.")
        PolicySection("Children", "The app collects no data from anyone, including children. It simulates a combat sport and is intended for a general audience.")
        PolicySection("Training safety", "Technique notes and scenarios are educational. They are not a substitute for a qualified coach. Train within your limits and seek medical advice before starting a new exercise programme.")
        Text("Last updated: this policy applies to version 1.0.0.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AboutScreen(back: () -> Unit) {
    ScreenScaffold("About", back) {
        ArenaCard(highlight = true) {
            ThaiPattern()
            Spacer(Modifier.height(10.dp))
            Text("MUAY THAI", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Training companion · fight simulator · fighter RPG · gym club", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            KeyValue("Version", "1.0.0")
            KeyValue("Package", "com.muaythai.arena")
            KeyValue("Storage", "On this device only")
        }
        PolicySection("Club", "Book classes, sparring, seminars and gradings, answer the gym's surveys, read the notice board and send requests to your coach. Coaches and staff manage all of it from the PIN-protected moderator panel on the same device.")
        PolicySection("The loop", "Train to raise your attributes. Build combinations. Study your opponent. Enter Fight Camp. Fight five rounds. Earn XP and rank points. Fix the weakness the fight exposed. Fight someone stronger.")
        PolicySection("How fights are decided", "Each round combines your attributes, your strategy against the opponent's, your stamina, the previous round and the decisions you make in three tactical situations. Three judges score every round on the ten-point must system. Enough accumulated damage ends the fight early.")
        PolicySection("Fictional content", "All thirty opponents, their gyms and their nicknames are invented. Any resemblance to real fighters, gyms or organisations is coincidental. The app uses no real logos or branding.")
        PolicySection("Respect the art", "Muay Thai is the national sport of Thailand, with a long tradition of discipline and respect between fighters and teachers. Train hard, and look after your training partners.")
    }
}
