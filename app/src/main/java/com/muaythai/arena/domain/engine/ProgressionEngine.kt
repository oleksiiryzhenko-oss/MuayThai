package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.Rank

data class LevelProgress(val level: Int, val xpIntoLevel: Int, val xpForNext: Int) {
    val fraction: Float get() = if (xpForNext <= 0) 1f else (xpIntoLevel.toFloat() / xpForNext).coerceIn(0f, 1f)
}

data class Readiness(val technique: Int, val defense: Int, val conditioning: Int, val fightIq: Int, val clinch: Int, val overall: Int) {
    val label: String
        get() = when {
            overall >= 85 -> "ELITE READY"
            overall >= 70 -> "FIGHT READY"
            overall >= 55 -> "SHARPENING"
            overall >= 40 -> "IN CAMP"
            else -> "BUILDING"
        }
}

data class RankRequirement(val next: Rank?, val levelNeeded: Int, val pointsNeeded: Int, val levelOk: Boolean, val pointsOk: Boolean)

/** Fighter level, attribute levels, ranks and archetype. Pure maths, no storage. */
object ProgressionEngine {
    const val MAX_FIGHTER_LEVEL = 60
    const val MAX_ATTRIBUTE_LEVEL = 99
    const val BASE_ATTRIBUTE_LEVEL = 36
    const val STYLE_BONUS = 8

    // ---- fighter level

    fun xpForLevel(level: Int): Int = 150 + 65 * (level - 1)

    fun fighterProgress(totalXp: Int): LevelProgress {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        while (level < MAX_FIGHTER_LEVEL && remaining >= xpForLevel(level)) {
            remaining -= xpForLevel(level)
            level++
        }
        return LevelProgress(level, remaining, if (level >= MAX_FIGHTER_LEVEL) 0 else xpForLevel(level))
    }

    fun fighterLevel(totalXp: Int): Int = fighterProgress(totalXp).level

    // ---- attributes

    private fun attributeStep(level: Int): Int = 80 + level * 4

    fun attributeXpForLevel(level: Int): Int {
        var total = 0
        for (l in 1 until level.coerceIn(1, MAX_ATTRIBUTE_LEVEL)) total += attributeStep(l)
        return total
    }

    fun attributeProgress(xp: Int): LevelProgress {
        var level = 1
        var remaining = xp.coerceAtLeast(0)
        while (level < MAX_ATTRIBUTE_LEVEL && remaining >= attributeStep(level)) {
            remaining -= attributeStep(level)
            level++
        }
        return LevelProgress(level, remaining, if (level >= MAX_ATTRIBUTE_LEVEL) 0 else attributeStep(level))
    }

    fun attributeLevel(xp: Int): Int = attributeProgress(xp).level

    fun levels(attributeXp: Map<Attribute, Int>): Map<Attribute, Int> =
        Attribute.entries.associateWith { attributeLevel(attributeXp[it] ?: 0) }

    fun styleAttributes(style: Archetype): List<Attribute> = when (style) {
        Archetype.PRESSURE -> listOf(Attribute.STRIKING, Attribute.CONDITIONING, Attribute.POWER)
        Archetype.TECHNICAL -> listOf(Attribute.DEFENSE, Attribute.FIGHT_IQ, Attribute.STRIKING)
        Archetype.KICKER -> listOf(Attribute.KICKING, Attribute.SPEED, Attribute.POWER)
        Archetype.CLINCHER -> listOf(Attribute.CLINCH, Attribute.POWER, Attribute.STAMINA)
        Archetype.COUNTER -> listOf(Attribute.REACTION, Attribute.DEFENSE, Attribute.FIGHT_IQ)
        Archetype.COMPLETE -> emptyList()
    }

    /** Attribute XP a brand-new fighter starts with for the chosen style. */
    fun startingAttributeXp(style: Archetype): Map<Attribute, Int> {
        val boosted = styleAttributes(style)
        return Attribute.entries.associateWith { attr ->
            val level = when {
                style == Archetype.COMPLETE -> BASE_ATTRIBUTE_LEVEL + 3
                attr in boosted -> BASE_ATTRIBUTE_LEVEL + STYLE_BONUS
                else -> BASE_ATTRIBUTE_LEVEL
            }
            attributeXpForLevel(level)
        }
    }

    fun overall(levels: Map<Attribute, Int>): Int =
        if (levels.isEmpty()) 0 else levels.values.sum() / levels.size

    fun readiness(levels: Map<Attribute, Int>): Readiness {
        fun l(a: Attribute) = levels[a] ?: 1
        return Readiness(
            technique = (l(Attribute.STRIKING) + l(Attribute.KICKING) + l(Attribute.SPEED) + l(Attribute.POWER)) / 4,
            defense = (l(Attribute.DEFENSE) * 2 + l(Attribute.REACTION)) / 3,
            conditioning = (l(Attribute.CONDITIONING) + l(Attribute.STAMINA)) / 2,
            fightIq = l(Attribute.FIGHT_IQ),
            clinch = l(Attribute.CLINCH),
            overall = overall(levels)
        )
    }

    // ---- ranks

    fun rank(level: Int, rankPoints: Int): Rank {
        var current = Rank.NOVICE
        for (r in Rank.entries) {
            if (level >= r.minLevel && rankPoints >= r.minRankPoints) current = r else break
        }
        return current
    }

    fun nextRankRequirement(level: Int, rankPoints: Int): RankRequirement {
        val current = rank(level, rankPoints)
        val next = Rank.entries.getOrNull(current.ordinal + 1)
            ?: return RankRequirement(null, current.minLevel, current.minRankPoints, levelOk = true, pointsOk = true)
        return RankRequirement(next, next.minLevel, next.minRankPoints, level >= next.minLevel, rankPoints >= next.minRankPoints)
    }

    // ---- archetype

    /** Affinity 0..100 for each archetype, derived from the fighter's current attribute levels. */
    fun archetypeAffinity(levels: Map<Attribute, Int>): Map<Archetype, Int> {
        fun l(a: Attribute) = (levels[a] ?: 1).toDouble()
        val raw = mapOf(
            Archetype.PRESSURE to (l(Attribute.STRIKING) + l(Attribute.CONDITIONING) + l(Attribute.POWER)) / 3,
            Archetype.TECHNICAL to (l(Attribute.DEFENSE) + l(Attribute.FIGHT_IQ) + l(Attribute.STRIKING)) / 3,
            Archetype.KICKER to (l(Attribute.KICKING) * 2 + l(Attribute.SPEED) + l(Attribute.POWER)) / 4,
            Archetype.CLINCHER to (l(Attribute.CLINCH) * 2 + l(Attribute.POWER) + l(Attribute.STAMINA)) / 4,
            Archetype.COUNTER to (l(Attribute.REACTION) + l(Attribute.DEFENSE) + l(Attribute.FIGHT_IQ)) / 3
        )
        val values = levels.values
        val spread = if (values.isEmpty()) 99 else (values.max() - values.min())
        val avg = overall(levels)
        // Balanced development: a small spread between best and worst attribute, on a decent base.
        val complete = (avg + (6 - spread) * 1.2).coerceIn(0.0, 100.0)
        return raw.mapValues { it.value.toInt().coerceIn(0, 100) } + (Archetype.COMPLETE to complete.toInt())
    }

    fun archetype(levels: Map<Attribute, Int>, initial: Archetype): Archetype {
        val affinity = archetypeAffinity(levels)
        val best = affinity.maxByOrNull { it.value } ?: return initial
        val initialScore = affinity[initial] ?: 0
        // Stay with the chosen style until training clearly pulls the fighter elsewhere.
        return if (best.value - initialScore >= 2) best.key else initial
    }
}
