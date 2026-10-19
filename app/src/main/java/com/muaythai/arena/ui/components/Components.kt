package com.muaythai.arena.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muaythai.arena.domain.model.Rank
import com.muaythai.arena.domain.model.RewardSummary
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.theme.ArenaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------------------- formatting

fun formatNumber(value: Int): String = String.format(Locale.US, "%,d", value)

fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

fun formatDate(timestamp: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))

fun formatDateTime(timestamp: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))

// ------------------------------------------------------------------------------- decoration

/** Three ring ropes. Used under headers and between major sections. */
@Composable
fun RopeDivider(modifier: Modifier = Modifier) {
    val crimson = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outline
    Canvas(modifier.fillMaxWidth().height(9.dp)) {
        val stroke = 1.5.dp.toPx()
        drawLine(crimson, Offset(0f, stroke), Offset(size.width, stroke), stroke)
        drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke)
        drawLine(crimson, Offset(0f, size.height - stroke), Offset(size.width, size.height - stroke), stroke)
    }
}

/** A strip of interlocking diamonds, after the woven patterns on Thai fight shorts and armbands. */
@Composable
fun ThaiPattern(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val h = size.height
        val step = h * 1.4f
        val stroke = 1.2.dp.toPx()
        var x = 0f
        while (x < size.width + step) {
            val path = Path().apply {
                moveTo(x, h / 2)
                lineTo(x + step / 2, 0f)
                lineTo(x + step, h / 2)
                lineTo(x + step / 2, h)
                close()
            }
            drawPath(path, color.copy(alpha = 0.75f), style = Stroke(stroke))
            drawCircle(color, radius = stroke * 1.2f, center = Offset(x + step / 2, h / 2))
            x += step
        }
    }
}

/**
 * Colours for anything drawn on a solid red surface: white becomes the "primary", tiles turn translucent.
 * Components placed inside keep working unchanged because they only read the colour scheme.
 */
@Composable
fun onRedColorScheme(): ColorScheme = MaterialTheme.colorScheme.copy(
    primary = ArenaColors.White,
    onPrimary = ArenaColors.Crimson,
    background = ArenaColors.Crimson,
    onBackground = ArenaColors.White,
    surface = ArenaColors.White.copy(alpha = 0.16f),
    onSurface = ArenaColors.White,
    surfaceVariant = ArenaColors.White.copy(alpha = 0.28f),
    onSurfaceVariant = ArenaColors.White.copy(alpha = 0.88f),
    outline = ArenaColors.White.copy(alpha = 0.45f),
    tertiary = Color(0xFFFFE08A),
    error = ArenaColors.White
)

/** Header background: solid red with two white stadium lights in the rafters. Content inside is white on red. */
@Composable
fun StadiumHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val red = ArenaColors.Crimson
    val deep = Color(0xFFB00D27)
    MaterialTheme(colorScheme = onRedColorScheme(), typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Column(
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(red, deep)))
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.18f, 0f), radius = size.width * 0.55f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.86f, 0f), radius = size.width * 0.45f
                        )
                    )
                }
                .padding(horizontal = 16.dp, vertical = 18.dp),
            content = content
        )
    }
}

@Composable
fun RankBadge(rank: Rank, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val tint = if (rank.elite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val inner = MaterialTheme.colorScheme.background
    val numeral = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII").getOrElse(rank.ordinal) { "I" }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            fun diamond(inset: Float) = Path().apply {
                moveTo(w / 2, inset)
                lineTo(w - inset, h / 2)
                lineTo(w / 2, h - inset)
                lineTo(inset, h / 2)
                close()
            }
            drawPath(diamond(0f), tint)
            drawPath(diamond(w * 0.09f), inner)
            drawPath(diamond(w * 0.15f), tint.copy(alpha = 0.22f))
            if (rank.elite) drawPath(diamond(w * 0.15f), tint, style = Stroke(w * 0.02f))
        }
        Text(numeral, color = tint, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun RatingRing(value: Int, label: String, modifier: Modifier = Modifier, size: Dp = 132.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val sweep by animateFloatAsState(value.coerceIn(0, 100) / 100f * 300f, tween(900), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, 120f, 300f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
            drawArc(color, 120f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.Center)
        }
    }
}

// ------------------------------------------------------------------------------- layout

@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Red bar, white type: the same red-and-white signature as the tab headers.
        Row(
            Modifier.fillMaxWidth().background(ArenaColors.Crimson).heightIn(min = 56.dp)
                .padding(start = if (onBack != null) 4.dp else 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ArenaColors.White)
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    title.uppercase(), style = MaterialTheme.typography.titleLarge, color = ArenaColors.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(subtitle.uppercase(), style = MaterialTheme.typography.labelSmall, color = ArenaColors.White.copy(alpha = 0.88f), maxLines = 1)
                }
            }
            if (action != null) {
                MaterialTheme(colorScheme = onRedColorScheme(), typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) { action() }
            }
        }
        val base = Modifier.fillMaxWidth().weight(1f)
        Column(
            (if (scroll) base.verticalScroll(rememberScrollState()) else base).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailingClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        if (trailing != null) {
            val m = if (onTrailingClick != null) Modifier.clickable(onClick = onTrailingClick) else Modifier
            Text(trailing.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = m.padding(4.dp))
        }
    }
}

@Composable
fun ArenaCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val border = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    var m = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, border, shape)
    if (accent != null) {
        m = m.drawBehind { drawRect(accent, size = Size(4.dp.toPx(), size.height)) }
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(start = if (accent != null) 18.dp else 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), content = content)
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, height: Dp = 8.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(650), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(color))
    }
}

@Composable
fun StatRow(label: String, value: Int, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, suffix: String = "") {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("$value$suffix", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(value / 100f, color = color, height = 6.dp)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(128.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, modifier = Modifier.weight(1f))
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, filled: Boolean = false) {
    val shape = RoundedCornerShape(2.dp)
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (filled) MaterialTheme.colorScheme.onPrimary else color,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.5f), shape)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
fun DifficultyDots(level: Int, modifier: Modifier = Modifier, max: Int = 5) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..max) {
            Box(
                Modifier.size(width = 12.dp, height = 5.dp)
                    .background(if (i <= level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun MessageBanner(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(12.dp)
    )
}

// ------------------------------------------------------------------------------- buttons

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val feedback = LocalFeedback.current
    Button(
        onClick = { feedback.tap(); onClick() },
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val feedback = LocalFeedback.current
    OutlinedButton(
        onClick = { feedback.tap(); onClick() },
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

enum class ChoiceState { NEUTRAL, SELECTED, CORRECT, WRONG, DIMMED }

/** A tactical card: the standard way to pick between options in the labs and in a fight. */
@Composable
fun ChoiceCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ChoiceState = ChoiceState.NEUTRAL,
    enabled: Boolean = true,
    subtitle: String? = null,
    leading: String? = null,
    trailing: String? = null
) {
    val feedback = LocalFeedback.current
    val scheme = MaterialTheme.colorScheme
    val tint = when (state) {
        ChoiceState.CORRECT -> ArenaColors.Win
        ChoiceState.WRONG -> scheme.error
        ChoiceState.SELECTED -> scheme.primary
        else -> scheme.outline
    }
    val fill = when (state) {
        ChoiceState.CORRECT, ChoiceState.WRONG, ChoiceState.SELECTED -> tint.copy(alpha = 0.16f)
        else -> scheme.surface
    }
    val shape = MaterialTheme.shapes.medium
    var m = modifier
        .fillMaxWidth()
        .alpha(if (state == ChoiceState.DIMMED) 0.45f else 1f)
        .clip(shape)
        .background(fill)
        .border(if (state == ChoiceState.NEUTRAL || state == ChoiceState.DIMMED) 1.dp else 2.dp, tint, shape)
    if (enabled) m = m.clickable { feedback.tap(); onClick() }
    Row(m.heightIn(min = 54.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            Text(leading, style = MaterialTheme.typography.titleLarge, color = scheme.primary, modifier = Modifier.width(30.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(trailing.uppercase(), style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

// ------------------------------------------------------------------------------- dialogs

@Composable
fun ConfirmDialog(
    title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel.uppercase(), style = MaterialTheme.typography.labelLarge,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = { Text(title.uppercase(), style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RewardLine(label: String, text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Pill(label, color = color)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

/** The pay-off after every activity: counting XP, level and rank-ups, and everything else the session unlocked. */
@Composable
fun RewardDialog(reward: RewardSummary, onDismiss: () -> Unit, buttonText: String = "Continue") {
    val feedback = LocalFeedback.current
    var target by remember(reward) { mutableIntStateOf(0) }
    var revealed by remember(reward) { mutableStateOf(false) }
    LaunchedEffect(reward) {
        target = reward.totalXp
        revealed = true
        if (reward.totalXp > 0) feedback.reward()
    }
    val shownXp by animateIntAsState(target, tween(1100), label = "xp")
    val pop by animateFloatAsState(
        if (revealed) 1f else 0.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "pop"
    )
    val gold = MaterialTheme.colorScheme.tertiary

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
        ) {
            ThaiPattern(Modifier.padding(top = 10.dp))
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(reward.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("+${formatNumber(shownXp)} XP", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                if (reward.bonusXp > 0) {
                    Text(
                        "${formatNumber(reward.xp)} session + ${formatNumber(reward.bonusXp)} bonus",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reward.coins > 0 || reward.rankPoints > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (reward.coins > 0) Pill("+${reward.coins} coins", color = gold)
                        if (reward.rankPoints > 0) Pill("+${reward.rankPoints} rank points")
                    }
                }

                if (reward.rankedUp) {
                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier.fillMaxWidth().scale(pop).clip(MaterialTheme.shapes.medium)
                            .background((if (reward.rankAfter.elite) gold else MaterialTheme.colorScheme.primary).copy(alpha = 0.14f))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RankBadge(reward.rankAfter, size = 72.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("RANK UP", style = MaterialTheme.typography.labelLarge, color = if (reward.rankAfter.elite) gold else MaterialTheme.colorScheme.primary)
                        Text(reward.rankAfter.label.uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (reward.leveledUp) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "LEVEL ${reward.levelBefore}  >  LEVEL ${reward.levelAfter}",
                        style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.scale(pop)
                    )
                }

                if (reward.gains.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    RopeDivider()
                    Spacer(Modifier.height(8.dp))
                    val ups = reward.attributeLevelUps.toMap()
                    for ((attr, amount) in reward.gains) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(attr.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            val level = ups[attr]
                            if (level != null) {
                                Pill("Level $level", color = ArenaColors.Win)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("+$amount", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                val hasExtras = reward.challenges.isNotEmpty() || reward.achievements.isNotEmpty() || reward.records.isNotEmpty() ||
                    reward.milestones.isNotEmpty() || reward.lines.isNotEmpty()
                if (hasExtras) {
                    Spacer(Modifier.height(10.dp))
                    RopeDivider()
                    Spacer(Modifier.height(6.dp))
                }
                for (c in reward.challenges) RewardLine(if (c.weekly) "Weekly" else "Mission", "${c.title}  +${c.xp} XP")
                for (a in reward.achievements) RewardLine("Achievement", "${a.title}  +${a.xp} XP", if (a.gold) gold else MaterialTheme.colorScheme.primary)
                for (r in reward.records) RewardLine("Record", r, ArenaColors.Win)
                for (m in reward.milestones) RewardLine("Milestone", m)
                for (l in reward.lines) RewardLine("Note", l, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryButton(buttonText, onDismiss, Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp))
        }
    }
}

// ------------------------------------------------------------------------------- forms

fun formatDay(epochDay: Long, today: Long): String {
    val text = java.time.LocalDate.ofEpochDay(epochDay).format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    return when (epochDay - today) {
        0L -> "Today · $text"
        1L -> "Tomorrow · $text"
        else -> text
    }
}

@Composable
fun ArenaTextField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, minLines: Int = 1, placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

/** A row of mutually exclusive choices that scrolls sideways when it does not fit. */
@Composable
fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val feedback = LocalFeedback.current
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            val on = index == selected
            Text(
                option.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                    .clickable { feedback.tap(); onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

/** Minus / value / plus, for numbers and dates where a keyboard would be slower. */
@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier, minusEnabled: Boolean = true, plusEnabled: Boolean = true) {
    val feedback = LocalFeedback.current
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        for (plus in listOf(false, true)) {
            if (plus) {
                Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
            }
            val enabled = if (plus) plusEnabled else minusEnabled
            Box(
                Modifier.size(44.dp).clip(MaterialTheme.shapes.small)
                    .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = enabled) { feedback.tap(); if (plus) onPlus() else onMinus() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (plus) "+" else "−", style = MaterialTheme.typography.headlineSmall, color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A horizontal bar with a label and a count, used for survey results. */
@Composable
fun ResultBar(label: String, count: Int, total: Int, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val share = if (total <= 0) 0 else count * 100 / total
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("$count · $share%", style = MaterialTheme.typography.labelMedium, color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(3.dp))
        ProgressBar(if (total <= 0) 0f else count.toFloat() / total, height = 7.dp)
    }
}
