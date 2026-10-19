package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.GameState
import com.muaythai.arena.domain.model.Rank
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.domain.model.StrikeType
import com.muaythai.arena.domain.model.TechniqueCategory
import com.muaythai.arena.domain.model.TrainingSession

/** A flat, read-only summary of everything the player has done. Coach, achievements and UI all read from this. */
data class PlayerStats(
    val level: Int = 1,
    val rank: Rank = Rank.NOVICE,
    val overall: Int = 0,
    val levels: Map<Attribute, Int> = emptyMap(),
    val archetype: Archetype = Archetype.TECHNICAL,
    val totalXp: Int = 0,
    val coins: Int = 0,
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val trainingDays: Int = 0,
    val comboReps: Int = 0,
    val comboSessions: Int = 0,
    val customCombos: Int = 0,
    val favoriteCombos: Int = 0,
    val masteredCombos: Int = 0,
    val techniqueReps: Int = 0,
    val lowKickReps: Int = 0,
    val kickReps: Int = 0,
    val punchReps: Int = 0,
    val kneeElbowReps: Int = 0,
    val techniquesMastered: Int = 0,
    val defenseSessions: Int = 0,
    val defenseAccuracy: Int = -1,
    val bestDefenseAccuracy: Int = 0,
    val clinchSessions: Int = 0,
    val clinchWins: Int = 0,
    val clinchIq: Int = 0,
    val reactionSessions: Int = 0,
    val reactionPrompts: Int = 0,
    val reactionAccuracy: Int = -1,
    val bestReactionAccuracy: Int = 0,
    val avgReactionMs: Int = 0,
    val bestReactionMs: Int = 0,
    val rounds: Int = 0,
    val programs: Int = 0,
    val fights: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val stoppageWins: Int = 0,
    val fullFights: Int = 0,
    val bestFightIq: Int = 0,
    val bestFightScore: Int = 0,
    val counterWins: Int = 0,
    val kickWins: Int = 0,
    val clinchStrategyWins: Int = 0,
    val highStaminaFights: Int = 0,
    val avgFightStamina: Int = -1,
    val opponentsBeaten: Int = 0,
    val maxTierBeaten: Int = 0,
    val championBeaten: Boolean = false,
    val campsCompleted: Int = 0,
    val dailyCompleted: Int = 0,
    val weeklyCompleted: Int = 0,
    val categoriesTrained: Int = 0,
    val daysSinceLastSession: Int = -1
)

object StatsEngine {

    fun weekIndex(epochDay: Long): Long = Math.floorDiv(epochDay + 3, 7L)
    fun weekStart(epochDay: Long): Long = weekIndex(epochDay) * 7 - 3

    /** Consecutive training days ending today (or yesterday, so the streak survives until midnight). */
    fun currentStreak(days: Set<Long>, today: Long): Int {
        var cursor = if (today in days) today else today - 1
        var n = 0
        while (cursor in days) { n++; cursor-- }
        return n
    }

    fun longestStreak(days: Set<Long>): Int {
        var best = 0
        for (d in days) {
            if ((d - 1) in days) continue
            var n = 1
            while ((d + n) in days) n++
            if (n > best) best = n
        }
        return best
    }

    fun xpByDay(sessions: List<TrainingSession>): Map<Long, Int> =
        sessions.groupBy { it.epochDay }.mapValues { e -> e.value.sumOf { it.xp } }

    fun compute(state: GameState, today: Long): PlayerStats {
        val fighter = state.fighter ?: return PlayerStats()
        val levels = ProgressionEngine.levels(state.attributeXp)
        val level = ProgressionEngine.fighterLevel(fighter.xp)
        val sessions = state.sessions
        val days = sessions.map { it.epochDay }.toSet()

        fun of(type: SessionType) = sessions.filter { it.type == type }
        val defense = of(SessionType.DEFENSE)
        val clinch = of(SessionType.CLINCH)
        val reaction = of(SessionType.REACTION)
        val combo = of(SessionType.COMBINATION)

        fun accuracy(list: List<TrainingSession>): Int {
            val total = list.sumOf { it.total }
            return if (total == 0) -1 else list.sumOf { it.correct } * 100 / total
        }

        var lowKick = 0; var kick = 0; var punch = 0; var kneeElbow = 0; var mastered = 0
        for ((id, reps) in state.techniqueReps) {
            val t = Techniques.get(id) ?: continue
            when (t.category.type) {
                StrikeType.KICK -> kick += reps
                StrikeType.PUNCH -> punch += reps
                else -> kneeElbow += reps
            }
            if (t.category == TechniqueCategory.LOW_KICK) lowKick += reps
            if (reps >= t.masteryReps) mastered++
        }

        val fights = state.fights
        val wins = fights.filter { it.won }
        val beaten = state.opponentRecords.values.filter { it.wins > 0 }
        val recentReaction = reaction.sortedByDescending { it.timestamp }.take(5).filter { it.valueMs > 0 }
        val recentClinch = clinch.sortedByDescending { it.timestamp }.take(10)

        return PlayerStats(
            level = level,
            rank = ProgressionEngine.rank(level, fighter.rankPoints),
            overall = ProgressionEngine.overall(levels),
            levels = levels,
            archetype = ProgressionEngine.archetype(levels, fighter.style),
            totalXp = fighter.xp,
            coins = fighter.coins,
            totalSessions = sessions.size,
            totalMinutes = sessions.sumOf { it.durationSec } / 60,
            streak = currentStreak(days, today),
            bestStreak = longestStreak(days),
            trainingDays = days.size,
            comboReps = state.combinations.sumOf { it.reps },
            comboSessions = combo.size,
            customCombos = state.combinations.count { it.custom },
            favoriteCombos = state.combinations.count { it.favorite },
            masteredCombos = state.combinations.count { CombinationEngine.mastery(it.reps) >= 100 },
            techniqueReps = state.techniqueReps.values.sum(),
            lowKickReps = lowKick,
            kickReps = kick,
            punchReps = punch,
            kneeElbowReps = kneeElbow,
            techniquesMastered = mastered,
            defenseSessions = defense.size,
            defenseAccuracy = accuracy(defense),
            bestDefenseAccuracy = defense.filter { it.total >= 5 }.maxOfOrNull { it.performance } ?: 0,
            clinchSessions = clinch.size,
            clinchWins = clinch.count { it.success },
            clinchIq = if (recentClinch.isEmpty()) 0 else recentClinch.sumOf { it.performance } / recentClinch.size,
            reactionSessions = reaction.size,
            reactionPrompts = reaction.sumOf { it.total },
            reactionAccuracy = accuracy(reaction),
            bestReactionAccuracy = reaction.filter { it.total >= 10 }.maxOfOrNull { it.correct * 100 / it.total } ?: 0,
            avgReactionMs = if (recentReaction.isEmpty()) 0 else recentReaction.sumOf { it.valueMs } / recentReaction.size,
            bestReactionMs = reaction.filter { it.valueMs > 0 }.minOfOrNull { it.valueMs } ?: 0,
            rounds = of(SessionType.ROUND).sumOf { it.reps },
            programs = of(SessionType.PROGRAM).size,
            fights = fights.size,
            wins = fighter.wins,
            losses = fighter.losses,
            winStreak = fighter.winStreak,
            bestWinStreak = fighter.bestWinStreak,
            stoppageWins = wins.count { it.method != "Decision" },
            fullFights = fights.count { it.endRound >= 5 && it.method == "Decision" },
            bestFightIq = fights.maxOfOrNull { it.fightIq } ?: 0,
            bestFightScore = fights.maxOfOrNull { it.overall } ?: 0,
            counterWins = wins.count { it.mainStrategy == Strategy.COUNTER },
            kickWins = wins.count { it.mainStrategy == Strategy.LONG_RANGE || it.mainStrategy == Strategy.LOW_KICK },
            clinchStrategyWins = wins.count { it.mainStrategy == Strategy.CLINCH },
            highStaminaFights = fights.count { it.stamina >= 80 },
            avgFightStamina = if (fights.isEmpty()) -1 else fights.takeLast(5).sumOf { it.stamina } / fights.takeLast(5).size,
            opponentsBeaten = beaten.size,
            maxTierBeaten = beaten.maxOfOrNull { Opponents.get(it.opponentId)?.tier ?: 0 } ?: 0,
            championBeaten = beaten.any { it.opponentId == Opponents.CHAMPION_ID },
            campsCompleted = fighter.campsCompleted,
            dailyCompleted = state.challenges.count { !it.weekly },
            weeklyCompleted = state.challenges.count { it.weekly },
            categoriesTrained = sessions.map { it.type }.toSet().size,
            daysSinceLastSession = days.maxOrNull()?.let { (today - it).toInt() } ?: -1
        )
    }
}
