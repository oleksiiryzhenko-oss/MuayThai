package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CombatStats
import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.domain.model.OpponentArchetype
import com.muaythai.arena.domain.model.OpponentRecord
import com.muaythai.arena.domain.model.OpponentStrategy
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.domain.model.Tactic
import kotlin.random.Random

data class TierStatus(val tier: Int, val unlocked: Boolean, val beaten: Int, val total: Int, val neededFromPrevious: Int)

data class ScoutingReport(val recommended: List<Strategy>, val avoid: List<Strategy>, val summary: String, val threat: String)

object OpponentEngine {
    const val WINS_TO_UNLOCK_NEXT_TIER = 3

    fun userCombatStats(levels: Map<Attribute, Int>, archetype: Archetype, bonuses: Map<Attribute, Int> = emptyMap()): CombatStats {
        fun l(a: Attribute) = ((levels[a] ?: 1) + (bonuses[a] ?: 0)).coerceIn(1, 99)
        val aggression = when (archetype) {
            Archetype.PRESSURE -> 80; Archetype.CLINCHER -> 65; Archetype.KICKER -> 55
            Archetype.COMPLETE -> 55; Archetype.TECHNICAL -> 45; Archetype.COUNTER -> 35
        }
        return CombatStats(
            speed = l(Attribute.SPEED), power = l(Attribute.POWER), defense = l(Attribute.DEFENSE),
            stamina = (l(Attribute.STAMINA) + l(Attribute.CONDITIONING)) / 2, fightIq = l(Attribute.FIGHT_IQ),
            kicking = l(Attribute.KICKING), boxing = l(Attribute.STRIKING), clinch = l(Attribute.CLINCH),
            aggression = aggression, reaction = l(Attribute.REACTION)
        )
    }

    fun combatStats(o: Opponent): CombatStats = CombatStats(
        speed = o.speed, power = o.power, defense = o.defense, stamina = o.stamina, fightIq = o.fightIq,
        kicking = o.kicking, boxing = o.boxing, clinch = o.clinch, aggression = o.aggression,
        reaction = (o.speed + o.fightIq) / 2
    )

    // ---- unlocking

    fun beatenInTier(tier: Int, records: Map<Int, OpponentRecord>): Int =
        Opponents.inTier(tier).count { (records[it.id]?.wins ?: 0) > 0 }

    fun isTierUnlocked(tier: Int, records: Map<Int, OpponentRecord>): Boolean =
        tier <= 1 || beatenInTier(tier - 1, records) >= WINS_TO_UNLOCK_NEXT_TIER

    fun isUnlocked(opponent: Opponent, records: Map<Int, OpponentRecord>): Boolean = isTierUnlocked(opponent.tier, records)

    fun tierStatuses(records: Map<Int, OpponentRecord>): List<TierStatus> = (1..Opponents.MAX_TIER).map { tier ->
        val unlocked = isTierUnlocked(tier, records)
        val needed = if (unlocked) 0 else WINS_TO_UNLOCK_NEXT_TIER - beatenInTier(tier - 1, records)
        TierStatus(tier, unlocked, beatenInTier(tier, records), Opponents.inTier(tier).size, needed)
    }

    fun tierName(tier: Int): String = when (tier) {
        1 -> "Local Circuit"; 2 -> "Regional Card"; 3 -> "National Stadium"; 4 -> "Elite Division"; else -> "Championship"
    }

    /** The strongest unlocked opponent the player has not beaten yet: the natural "next fight". */
    fun nextOpponent(records: Map<Int, OpponentRecord>): Opponent =
        Opponents.all.firstOrNull { isUnlocked(it, records) && (records[it.id]?.wins ?: 0) == 0 }
            ?: Opponents.all.last { isUnlocked(it, records) }

    fun difficultyLabel(userOverall: Int, opponent: Opponent): String {
        val diff = opponent.overall - userOverall
        return when {
            diff >= 14 -> "BRUTAL"
            diff >= 7 -> "HARD"
            diff >= -3 -> "EVEN"
            diff >= -10 -> "FAVOURED"
            else -> "EASY"
        }
    }

    // ---- strategy

    /** How the user's strategy fares against the opponent's: -2 (bad idea) .. +2 (perfect read). */
    fun matchup(user: Strategy, opp: OpponentStrategy): Int {
        val row = when (user) {
            //                        PRESSURE COUNTER LONG CLINCH LOWKICK VOLUME
            Strategy.PRESSURE -> intArrayOf(0, -2, 2, -1, 0, 1)
            Strategy.COUNTER -> intArrayOf(2, 0, -1, 0, 1, 2)
            Strategy.LONG_RANGE -> intArrayOf(-1, 1, 0, 2, 0, 1)
            Strategy.LOW_KICK -> intArrayOf(1, 2, 0, -1, 0, 1)
            Strategy.BODY_ATTACK -> intArrayOf(1, -1, 0, 0, 0, 2)
            Strategy.CLINCH -> intArrayOf(0, 0, 2, 0, 1, 1)
            Strategy.DEFENSIVE -> intArrayOf(1, -1, -1, 0, -1, -2)
            Strategy.BALANCED -> intArrayOf(0, 0, 0, 0, 0, 0)
        }
        return row[opp.ordinal]
    }

    private fun baseWeights(archetype: OpponentArchetype): MutableMap<OpponentStrategy, Int> = when (archetype) {
        OpponentArchetype.PRESSURE_FIGHTER -> mutableMapOf(OpponentStrategy.PRESSURE to 5, OpponentStrategy.LOW_KICK_HEAVY to 3, OpponentStrategy.HIGH_VOLUME to 3)
        OpponentArchetype.COUNTER_STRIKER -> mutableMapOf(OpponentStrategy.COUNTER to 7, OpponentStrategy.LONG_RANGE to 2, OpponentStrategy.LOW_KICK_HEAVY to 1)
        OpponentArchetype.KICK_SPECIALIST -> mutableMapOf(OpponentStrategy.LONG_RANGE to 6, OpponentStrategy.LOW_KICK_HEAVY to 3, OpponentStrategy.COUNTER to 1)
        OpponentArchetype.CLINCHER -> mutableMapOf(OpponentStrategy.CLINCH to 7, OpponentStrategy.PRESSURE to 2, OpponentStrategy.HIGH_VOLUME to 1)
        OpponentArchetype.ELBOW_HUNTER -> mutableMapOf(OpponentStrategy.PRESSURE to 5, OpponentStrategy.CLINCH to 3, OpponentStrategy.COUNTER to 2)
        OpponentArchetype.COMPLETE_FIGHTER -> OpponentStrategy.entries.associateWith { 2 }.toMutableMap()
    }

    fun chooseStrategy(
        opponent: Opponent, round: Int, opponentStamina: Int, roundsBehind: Int, userLegDamage: Int,
        lastUserStrategy: Strategy?, random: Random
    ): OpponentStrategy {
        // Smart opponents adapt to what the user just did.
        if (lastUserStrategy != null && round > 1) {
            val adaptChance = ((opponent.fightIq - 45) / 100.0).coerceIn(0.0, 0.5) +
                if (opponent.archetype == OpponentArchetype.COMPLETE_FIGHTER) 0.25 else 0.0
            if (random.nextDouble() < adaptChance) {
                return OpponentStrategy.entries.minByOrNull { matchup(lastUserStrategy, it) } ?: OpponentStrategy.PRESSURE
            }
        }
        val weights = baseWeights(opponent.archetype)
        fun bump(s: OpponentStrategy, by: Int) { weights[s] = (weights[s] ?: 0) + by }
        if (opponentStamina < 40) {
            bump(OpponentStrategy.COUNTER, 5)
            weights[OpponentStrategy.HIGH_VOLUME] = (weights[OpponentStrategy.HIGH_VOLUME] ?: 0) / 3
            weights[OpponentStrategy.PRESSURE] = (weights[OpponentStrategy.PRESSURE] ?: 0) / 2
        }
        if (roundsBehind >= 2 && round >= 3) { bump(OpponentStrategy.PRESSURE, 4); bump(OpponentStrategy.HIGH_VOLUME, 3) }
        if (userLegDamage >= 30) bump(OpponentStrategy.LOW_KICK_HEAVY, 4)
        val total = weights.values.sum().coerceAtLeast(1)
        var roll = random.nextInt(total)
        for ((s, w) in weights) {
            if (roll < w) return s
            roll -= w
        }
        return weights.keys.first()
    }

    // ---- scouting

    private fun strategiesFor(tactic: Tactic): List<Strategy> = when (tactic) {
        Tactic.COUNTER -> listOf(Strategy.COUNTER)
        Tactic.ANGLE -> listOf(Strategy.COUNTER, Strategy.LONG_RANGE)
        Tactic.LEGS -> listOf(Strategy.LOW_KICK)
        Tactic.PACE -> listOf(Strategy.BALANCED, Strategy.LOW_KICK)
        Tactic.CLINCH -> listOf(Strategy.CLINCH)
        Tactic.PRESSURE -> listOf(Strategy.PRESSURE)
        Tactic.ELBOW -> listOf(Strategy.PRESSURE)
        Tactic.DISTANCE -> listOf(Strategy.LONG_RANGE)
        Tactic.BODY -> listOf(Strategy.BODY_ATTACK)
        else -> emptyList()
    }

    fun scout(opponent: Opponent): ScoutingReport {
        val recommended = opponent.weaknesses.flatMap { strategiesFor(it) }.distinct()
        val signature = baseWeights(opponent.archetype).maxByOrNull { it.value }?.key ?: OpponentStrategy.PRESSURE
        val avoid = Strategy.entries.filter { matchup(it, signature) < 0 }
        val summary = if (opponent.weaknesses.isEmpty()) {
            "No clear weakness. Win the tactical battle round by round and manage your stamina."
        } else {
            "Vulnerable to " + opponent.weaknesses.joinToString(", ") { it.label.lowercase() } + "."
        }
        return ScoutingReport(
            recommended = recommended.ifEmpty { listOf(Strategy.BALANCED, Strategy.COUNTER) },
            avoid = avoid,
            summary = summary,
            threat = "Expect ${signature.label.lowercase()}: ${signature.blurb.lowercase()}"
        )
    }
}
