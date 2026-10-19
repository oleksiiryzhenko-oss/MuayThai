package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.content.ClinchScenarios
import com.muaythai.arena.domain.content.DefenseScenarios
import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.ClinchScenario
import com.muaythai.arena.domain.model.Combination
import com.muaythai.arena.domain.model.DefenseResponse
import com.muaythai.arena.domain.model.DefenseScenario
import com.muaythai.arena.domain.model.ReactionPrompt
import com.muaythai.arena.domain.model.ReactionRank
import com.muaythai.arena.domain.model.SessionOutcome
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Strike
import com.muaythai.arena.domain.model.StrikeType
import com.muaythai.arena.domain.model.Technique
import com.muaythai.arena.domain.model.TrainingProgram
import kotlin.random.Random

private fun MutableMap<Attribute, Int>.add(attr: Attribute, amount: Int) {
    if (amount != 0) this[attr] = (this[attr] ?: 0) + amount
}

// =====================================================================================
// Technique drills, round work and programs
// =====================================================================================

object MuayThaiTrainingEngine {
    const val SET_SIZE = 10
    val roundLengths = listOf(60, 120, 180)

    fun mastery(technique: Technique, reps: Int): Int =
        (reps * 100 / technique.masteryReps).coerceIn(0, 100)

    fun categoryMastery(techniques: List<Technique>, reps: Map<String, Int>): Int =
        if (techniques.isEmpty()) 0 else techniques.sumOf { mastery(it, reps[it.id] ?: 0) } / techniques.size

    fun gainsFor(type: StrikeType, reps: Int): Map<Attribute, Int> {
        val out = LinkedHashMap<Attribute, Int>()
        when (type) {
            StrikeType.PUNCH -> { out.add(Attribute.STRIKING, reps * 6); out.add(Attribute.SPEED, reps * 2) }
            StrikeType.KICK -> { out.add(Attribute.KICKING, reps * 6); out.add(Attribute.POWER, reps * 2) }
            StrikeType.KNEE -> { out.add(Attribute.CLINCH, reps * 5); out.add(Attribute.POWER, reps * 3) }
            StrikeType.ELBOW -> { out.add(Attribute.STRIKING, reps * 5); out.add(Attribute.POWER, reps * 3) }
        }
        out.add(Attribute.CONDITIONING, reps)
        return out
    }

    fun drillOutcome(technique: Technique, reps: Int, durationSec: Int): SessionOutcome {
        val xp = technique.xpReward * reps / SET_SIZE
        return SessionOutcome(
            type = SessionType.TECHNIQUE,
            title = "${technique.name} x$reps",
            durationSec = durationSec,
            xp = xp,
            performance = (60 + reps).coerceAtMost(100),
            gains = gainsFor(technique.category.type, reps),
            reps = reps,
            tag = technique.id,
            success = reps >= SET_SIZE
        )
    }

    /** Callouts for a shadowboxing round: one combination roughly every eight seconds. */
    fun roundCallouts(roundSeconds: Int, combinations: List<Combination>, random: Random): List<String> {
        val pool = combinations.ifEmpty {
            Techniques.starterCombinations.map { Combination(name = it.first, strikes = it.second) }
        }
        val count = (roundSeconds / 8).coerceAtLeast(1)
        return List(count) { pool[random.nextInt(pool.size)].notation }
    }

    fun roundOutcome(roundSeconds: Int, completedSeconds: Int): SessionOutcome {
        val minutes = completedSeconds / 60.0
        val finished = completedSeconds >= roundSeconds
        val gains = LinkedHashMap<Attribute, Int>()
        gains.add(Attribute.CONDITIONING, (minutes * 45).toInt())
        gains.add(Attribute.STAMINA, (minutes * 45).toInt())
        gains.add(Attribute.STRIKING, (minutes * 18).toInt())
        gains.add(Attribute.KICKING, (minutes * 18).toInt())
        return SessionOutcome(
            type = SessionType.ROUND,
            title = "${roundSeconds / 60}-minute round",
            durationSec = completedSeconds,
            xp = (minutes * 45).toInt() + if (finished) 30 else 0,
            performance = (completedSeconds * 100 / roundSeconds.coerceAtLeast(1)).coerceIn(0, 100),
            gains = gains,
            reps = if (finished) 1 else 0,
            tag = roundSeconds.toString(),
            success = finished
        )
    }

    fun programOutcome(program: TrainingProgram, completedSteps: Int, durationSec: Int): SessionOutcome {
        val fraction = completedSteps.toDouble() / program.steps.size.coerceAtLeast(1)
        return SessionOutcome(
            type = SessionType.PROGRAM,
            title = program.name,
            durationSec = durationSec,
            xp = (program.xp * fraction).toInt(),
            performance = (fraction * 100).toInt(),
            gains = program.gains.mapValues { (it.value * 10 * fraction).toInt() },
            tag = program.id,
            success = completedSteps >= program.steps.size
        )
    }

    /** The technique with the lowest mastery: what the Train screen suggests next. */
    fun weakestTechnique(reps: Map<String, Int>): Technique =
        Techniques.all.minByOrNull { mastery(it, reps[it.id] ?: 0) * 10 + it.difficulty } ?: Techniques.all.first()
}

// =====================================================================================
// Combinations
// =====================================================================================

data class Milestone(val reps: Int, val xp: Int)

object CombinationEngine {
    const val MIN_STRIKES = 2
    const val MAX_STRIKES = 8
    const val MASTERY_REPS = 200

    val milestones = listOf(
        Milestone(25, 100), Milestone(50, 150), Milestone(100, 250),
        Milestone(200, 500), Milestone(400, 750), Milestone(800, 1000)
    )

    fun validate(strikes: List<Strike>): String? = when {
        strikes.size < MIN_STRIKES -> "Add at least $MIN_STRIKES strikes."
        strikes.size > MAX_STRIKES -> "A combination can hold at most $MAX_STRIKES strikes."
        else -> null
    }

    /** 1 (basic) .. 5 (expert): driven by length, the weight of each weapon and how often the weapon type changes. */
    fun difficulty(strikes: List<Strike>): Int {
        if (strikes.isEmpty()) return 1
        val weight = strikes.sumOf { it.weight }
        var switches = 0
        for (i in 1 until strikes.size) if (strikes[i].type != strikes[i - 1].type) switches++
        val score = weight + switches * 1.5 + strikes.size * 0.5
        return when {
            score < 7 -> 1
            score < 11 -> 2
            score < 15 -> 3
            score < 19 -> 4
            else -> 5
        }
    }

    fun difficultyLabel(level: Int): String = when (level) {
        1 -> "Basic"; 2 -> "Solid"; 3 -> "Advanced"; 4 -> "Expert"; else -> "Master"
    }

    /** XP for ten clean repetitions. */
    fun xpValue(strikes: List<Strike>): Int = 20 + difficulty(strikes) * 12 + strikes.size * 2

    fun mastery(reps: Int): Int = (reps * 100 / MASTERY_REPS).coerceIn(0, 100)

    fun nextMilestone(reps: Int): Milestone? = milestones.firstOrNull { it.reps > reps }

    /** Milestones reached by [reps] that have not paid out yet. */
    fun unclaimedMilestones(reps: Int, claimed: Int): List<Milestone> =
        milestones.filterIndexed { index, m -> index >= claimed && reps >= m.reps }

    fun suggestName(strikes: List<Strike>): String {
        if (strikes.isEmpty()) return "New Combination"
        val last = strikes.last()
        val prefix = when (strikes.first().type) {
            StrikeType.PUNCH -> "Hands"
            StrikeType.KICK -> "Kick"
            StrikeType.KNEE -> "Knee"
            StrikeType.ELBOW -> "Elbow"
        }
        return "$prefix to ${last.label}"
    }

    fun gains(strikes: List<Strike>, reps: Int): Map<Attribute, Int> {
        val out = LinkedHashMap<Attribute, Int>()
        if (strikes.isEmpty() || reps <= 0) return out
        val budget = reps * (4 + difficulty(strikes))
        val per = budget / strikes.size
        for (s in strikes) {
            when (s.type) {
                StrikeType.PUNCH -> out.add(Attribute.STRIKING, per)
                StrikeType.KICK -> out.add(Attribute.KICKING, per)
                StrikeType.KNEE -> out.add(Attribute.CLINCH, per)
                StrikeType.ELBOW -> { out.add(Attribute.STRIKING, per / 2); out.add(Attribute.POWER, per - per / 2) }
            }
        }
        out.add(Attribute.SPEED, reps * 2)
        out.add(Attribute.CONDITIONING, reps)
        return out
    }

    fun practiceOutcome(combination: Combination, reps: Int, durationSec: Int): SessionOutcome =
        SessionOutcome(
            type = SessionType.COMBINATION,
            title = combination.name,
            durationSec = durationSec,
            xp = xpValue(combination.strikes) * reps / 10,
            performance = (55 + reps * 2).coerceAtMost(100),
            gains = gains(combination.strikes, reps),
            reps = reps,
            tag = combination.id.toString(),
            success = reps >= 5
        )
}

// =====================================================================================
// Defense Lab
// =====================================================================================

object DefenseEngine {
    const val SESSION_SIZE = 8
    const val IQ_XP_PER_CORRECT = 35

    /** Picks scenarios and shuffles the answer order, so the correct option is never in a fixed slot. */
    fun buildSession(random: Random, size: Int = SESSION_SIZE): List<DefenseScenario> =
        DefenseScenarios.all.shuffled(random).take(size).map { shuffleOptions(it, random) }

    fun shuffleOptions(scenario: DefenseScenario, random: Random): DefenseScenario {
        val correct = scenario.options[scenario.correctIndex]
        val shuffled = scenario.options.shuffled(random)
        return scenario.copy(options = shuffled, correctIndex = shuffled.indexOf(correct))
    }

    fun isCorrect(scenario: DefenseScenario, chosen: Int): Boolean = chosen == scenario.correctIndex

    fun outcome(correct: Int, total: Int, durationSec: Int): SessionOutcome {
        val accuracy = if (total == 0) 0 else correct * 100 / total
        val gains = LinkedHashMap<Attribute, Int>()
        gains.add(Attribute.FIGHT_IQ, correct * IQ_XP_PER_CORRECT)
        gains.add(Attribute.DEFENSE, correct * 22 + total * 4)
        gains.add(Attribute.REACTION, correct * 5)
        return SessionOutcome(
            type = SessionType.DEFENSE,
            title = "Defense Lab $correct/$total",
            durationSec = durationSec,
            xp = correct * 18 + (if (accuracy == 100 && total >= 5) 60 else 0) + total * 3,
            performance = accuracy,
            gains = gains,
            correct = correct,
            total = total,
            success = accuracy >= 75
        )
    }

    fun grade(accuracy: Int): String = when {
        accuracy >= 90 -> "IRON GUARD"
        accuracy >= 75 -> "SOLID DEFENSE"
        accuracy >= 50 -> "GETTING HIT"
        else -> "WIDE OPEN"
    }
}

// =====================================================================================
// Reaction Arena
// =====================================================================================

data class ReactionResult(val correct: Boolean, val ms: Int, val timedOut: Boolean = false)

data class ReactionSummary(
    val total: Int, val correct: Int, val wrong: Int, val accuracy: Int,
    val averageMs: Int, val bestMs: Int, val rank: ReactionRank
)

object ReactionEngine {
    const val SESSION_SIZE = 20
    const val TIMEOUT_MS = 2500

    /** The legend shown before a session. Every attack has exactly one correct answer. */
    val answers: List<Pair<String, DefenseResponse>> = listOf(
        "LOW KICK" to DefenseResponse.CHECK,
        "TEEP" to DefenseResponse.CATCH,
        "JAB" to DefenseResponse.PARRY,
        "ROUNDHOUSE" to DefenseResponse.BLOCK,
        "KNEE" to DefenseResponse.FRAME_EXIT,
        "CROSS" to DefenseResponse.SLIP,
        "ELBOW" to DefenseResponse.STEP_BACK,
        "FLURRY" to DefenseResponse.ANGLE_OUT
    )

    fun nextPrompt(random: Random, previousAttack: String? = null): ReactionPrompt {
        val candidates = answers.filter { it.first != previousAttack }
        val (attack, correct) = candidates[random.nextInt(candidates.size)]
        val distractors = DefenseResponse.entries.filter { it != correct }.shuffled(random).take(3)
        return ReactionPrompt(attack, (distractors + correct).shuffled(random), correct)
    }

    /** Random wait before the attack appears, so the player cannot fall into a rhythm. */
    fun delayMs(random: Random): Long = 700L + random.nextInt(1300)

    fun rank(averageMs: Int, accuracy: Int): ReactionRank = when {
        averageMs <= 0 -> ReactionRank.BEGINNER
        averageMs < 480 && accuracy >= 85 -> ReactionRank.LIGHTNING
        averageMs < 620 && accuracy >= 80 -> ReactionRank.ELITE
        averageMs < 780 && accuracy >= 70 -> ReactionRank.SHARP
        averageMs < 980 && accuracy >= 60 -> ReactionRank.FAST
        else -> ReactionRank.BEGINNER
    }

    fun summarize(results: List<ReactionResult>): ReactionSummary {
        val correct = results.filter { it.correct }
        val accuracy = if (results.isEmpty()) 0 else correct.size * 100 / results.size
        val avg = if (correct.isEmpty()) 0 else correct.sumOf { it.ms } / correct.size
        val best = correct.minOfOrNull { it.ms } ?: 0
        return ReactionSummary(results.size, correct.size, results.size - correct.size, accuracy, avg, best, rank(avg, accuracy))
    }

    fun outcome(results: List<ReactionResult>, durationSec: Int): SessionOutcome {
        val s = summarize(results)
        val speedBonus = when (s.rank) {
            ReactionRank.LIGHTNING -> 120; ReactionRank.ELITE -> 80; ReactionRank.SHARP -> 50; ReactionRank.FAST -> 25
            ReactionRank.BEGINNER -> 0
        }
        val gains = LinkedHashMap<Attribute, Int>()
        gains.add(Attribute.REACTION, s.correct * 14 + speedBonus)
        gains.add(Attribute.SPEED, s.correct * 4)
        gains.add(Attribute.DEFENSE, s.correct * 5)
        return SessionOutcome(
            type = SessionType.REACTION,
            title = "Reaction Arena ${s.correct}/${s.total}",
            durationSec = durationSec,
            xp = s.correct * 8 + speedBonus,
            performance = s.accuracy,
            gains = gains,
            correct = s.correct,
            total = s.total,
            success = s.accuracy >= 80,
            valueMs = s.averageMs,
            tag = s.bestMs.toString()
        )
    }
}

// =====================================================================================
// Clinch Lab
// =====================================================================================

object ClinchEngine {
    const val SESSION_SIZE = 6
    const val WIN_THRESHOLD = 67

    fun buildSession(random: Random, size: Int = SESSION_SIZE): List<ClinchScenario> =
        ClinchScenarios.all.shuffled(random).take(size).map { it.copy(options = it.options.shuffled(random)) }

    fun qualityLabel(quality: Int): String = when (quality) {
        3 -> "DOMINANT"; 2 -> "GOOD"; 1 -> "NEUTRAL"; else -> "PUNISHED"
    }

    fun score(qualities: List<Int>): Int =
        if (qualities.isEmpty()) 0 else qualities.sum() * 100 / (qualities.size * 3)

    fun iqLabel(clinchIq: Int): String = when {
        clinchIq >= 90 -> "Clinch Master"
        clinchIq >= 75 -> "Neck Wrestler"
        clinchIq >= 55 -> "Learning the Grips"
        clinchIq > 0 -> "Getting Turned"
        else -> "Untested"
    }

    fun outcome(qualities: List<Int>, durationSec: Int): SessionOutcome {
        val score = score(qualities)
        val points = qualities.sum()
        val gains = LinkedHashMap<Attribute, Int>()
        gains.add(Attribute.CLINCH, points * 18 + qualities.size * 5)
        gains.add(Attribute.FIGHT_IQ, points * 8)
        gains.add(Attribute.POWER, points * 3)
        return SessionOutcome(
            type = SessionType.CLINCH,
            title = "Clinch Lab $score%",
            durationSec = durationSec,
            xp = points * 12 + if (score >= WIN_THRESHOLD) 50 else 0,
            performance = score,
            gains = gains,
            correct = qualities.count { it == 3 },
            total = qualities.size,
            success = score >= WIN_THRESHOLD
        )
    }
}
