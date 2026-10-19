package com.muaythai.arena.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.muaythai.arena.domain.engine.ProgressionEngine
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Rank
import com.muaythai.arena.ui.components.ArenaCard
import com.muaythai.arena.ui.components.ChoiceCard
import com.muaythai.arena.ui.components.ChoiceState
import com.muaythai.arena.ui.components.GhostButton
import com.muaythai.arena.ui.components.KeyValue
import com.muaythai.arena.ui.components.PrimaryButton
import com.muaythai.arena.ui.components.RankBadge
import com.muaythai.arena.ui.components.RopeDivider
import com.muaythai.arena.ui.components.StadiumHeader
import com.muaythai.arena.ui.components.ThaiPattern
import com.muaythai.arena.ui.theme.ArenaColors

private val startingStyles = listOf(Archetype.PRESSURE, Archetype.TECHNICAL, Archetype.KICKER, Archetype.CLINCHER, Archetype.COUNTER)

private fun styleShortName(style: Archetype): String = when (style) {
    Archetype.PRESSURE -> "Pressure"
    Archetype.TECHNICAL -> "Technical"
    Archetype.KICKER -> "Kicker"
    Archetype.CLINCHER -> "Clincher"
    Archetype.COUNTER -> "Counter"
    Archetype.COMPLETE -> "Complete"
}

@Composable
fun OnboardingScreen(onFinish: (String, Archetype) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var styleName by rememberSaveable { mutableStateOf(Archetype.TECHNICAL.name) }
    var name by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val style = Archetype.entries.firstOrNull { it.name == styleName } ?: Archetype.TECHNICAL

    BackHandler(enabled = page > 0 && !submitted) { page-- }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        // The red header runs up behind the status bar.
        Box(Modifier.fillMaxWidth().background(ArenaColors.Crimson).statusBarsPadding()) {
          StadiumHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(Rank.NOVICE, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("MUAY THAI", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("STEP ${page + 1} OF 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
          }
        }
        RopeDivider()

        Box(Modifier.fillMaxWidth().weight(1f)) {
            Crossfade(targetState = page, label = "onboarding") { current ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (current) {
                        0 -> {
                            Spacer(Modifier.height(24.dp))
                            Text("WELCOME TO\nMUAY THAI", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text("Train smarter.", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            ThaiPattern(Modifier.padding(vertical = 8.dp))
                            Text(
                                "Build a fighter. Drill technique, design combinations, sharpen your defense and clinch, " +
                                    "then test every decision over five rounds in the Fight Lab.",
                                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Everything stays on this device. No account, no sign-in, no internet.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        1 -> {
                            Text("CHOOSE YOUR INITIAL STYLE", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                "Your style boosts three starting attributes. Training will keep shaping it.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            for (option in startingStyles) {
                                val boosted = ProgressionEngine.styleAttributes(option).joinToString(" · ") { it.label }
                                ChoiceCard(
                                    text = styleShortName(option).uppercase(),
                                    subtitle = "${option.tagline}  +$boosted",
                                    onClick = { styleName = option.name },
                                    state = if (option == style) ChoiceState.SELECTED else ChoiceState.NEUTRAL,
                                    trailing = if (option == style) "Selected" else null
                                )
                            }
                        }
                        2 -> {
                            Text("SET FIGHTER NAME", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                "This is the name on your fight card. You can change it later in Profile.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(18) },
                                singleLine = true,
                                label = { Text("Fighter name") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${name.trim().length}/18", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            Spacer(Modifier.height(12.dp))
                            Text("YOUR FIGHT CAMP\nSTARTS NOW", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                            ThaiPattern(Modifier.padding(vertical = 6.dp))
                            ArenaCard(accent = MaterialTheme.colorScheme.primary) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RankBadge(Rank.NOVICE, size = 52.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(name.trim().ifBlank { "Fighter" }.uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text("NOVICE · LEVEL 1", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                KeyValue("Style", style.label)
                                KeyValue("Strengths", ProgressionEngine.styleAttributes(style).joinToString(", ") { it.label })
                                KeyValue("First goal", "Finish today's mission and win a fight on the Local Circuit.")
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page > 0) GhostButton("Back", { page-- }, Modifier.weight(1f), enabled = !submitted)
            val canContinue = page != 2 || name.trim().isNotEmpty()
            PrimaryButton(
                text = when (page) { 0 -> "Get started"; 3 -> "Enter the gym"; else -> "Continue" },
                onClick = {
                    if (page < 3) page++ else if (!submitted) { submitted = true; onFinish(name.trim(), style) }
                },
                modifier = Modifier.weight(2f),
                enabled = canContinue && !submitted
            )
        }
    }
}
