package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.domain.model.FightCamp
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.domain.model.SessionOutcome
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Tactic

data class CampStep(val camp: FightCamp, val message: String, val applied: Boolean)

/** Seven in-game days of preparation: six training days with limited energy, then fight day. */
object FightCampEngine {
    const val DAYS = 7
    const val ENERGY_PER_DAY = 3
    const val EXTRA_ENERGY_COST = 120
    const val OVERTRAINED_BELOW = 35
    private const val PERMANENT_XP_PER_POINT = 14

    fun start(opponentId: Int): FightCamp = FightCamp(
        active = true, day = 1, energy = ENERGY_PER_DAY, freshness = 80, opponentId = opponentId,
        log = listOf("Day 1: camp opens. Six days of work, then you fight.")
    )

    fun isOvertrained(camp: FightCamp): Boolean = camp.freshness < OVERTRAINED_BELOW

    /** What one more session of [training] would give right now, after diminishing returns and fatigue. */
    fun projectedGain(camp: FightCamp, training: CampTraining): Int {
        if (training.attribute == null) return 0
        val repeats = camp.trainedToday.count { it == training }
        var gain = training.gain shr repeats
        if (isOvertrained(camp)) gain /= 2
        return gain.coerceAtLeast(if (repeats >= 3) 0 else 1)
    }

    fun train(camp: FightCamp, training: CampTraining): CampStep {
        if (!camp.active || camp.isFightDay) return CampStep(camp, "It is fight day. No more training.", false)
        if (camp.energy <= 0) return CampStep(camp, "No energy left today. End the day to recover.", false)

        if (training == CampTraining.RECOVERY) {
            val freshness = (camp.freshness - training.fatigue).coerceAtMost(100)
            val msg = "Recovery session. Freshness ${camp.freshness}% -> $freshness%."
            return CampStep(
                camp.copy(energy = camp.energy - 1, freshness = freshness, trainedToday = camp.trainedToday + training,
                    log = camp.log + "Day ${camp.day}: $msg"), msg, true
            )
        }

        val attr = training.attribute ?: return CampStep(camp, "Nothing to train.", false)
        val overtrained = isOvertrained(camp)
        val gain = projectedGain(camp, training)
        val fatigue = training.fatigue + if (overtrained) 5 else 0
        val freshness = (camp.freshness - fatigue).coerceAtLeast(0)
        val bonuses = camp.bonuses + (attr to ((camp.bonuses[attr] ?: 0) + gain))
        val msg = buildString {
            append("${training.label}: ${attr.label} +$gain for the fight.")
            if (overtrained) append(" You are overtrained, so the session was half as effective.")
            else if (freshness < OVERTRAINED_BELOW) append(" Careful: you are now overtrained. Recover before training again.")
        }
        return CampStep(
            camp.copy(energy = camp.energy - 1, freshness = freshness, bonuses = bonuses,
                trainedToday = camp.trainedToday + training, log = camp.log + "Day ${camp.day}: $msg"), msg, true
        )
    }

    fun buyEnergy(camp: FightCamp, coins: Int): CampStep = when {
        !camp.active || camp.isFightDay -> CampStep(camp, "There is no training day in progress.", false)
        camp.boughtEnergyToday -> CampStep(camp, "You already booked an extra session today.", false)
        coins < EXTRA_ENERGY_COST -> CampStep(camp, "You need $EXTRA_ENERGY_COST coins for an extra session.", false)
        else -> CampStep(camp.copy(energy = camp.energy + 1, boughtEnergyToday = true,
            log = camp.log + "Day ${camp.day}: booked an extra pad holder (+1 energy)."), "Extra session booked. +1 energy.", true)
    }

    /** Closes the day. Unused energy and a night's sleep restore freshness. */
    fun endDay(camp: FightCamp): FightCamp {
        if (!camp.active || camp.isFightDay) return camp
        val rest = 12 + camp.energy * 5
        val freshness = (camp.freshness + rest).coerceAtMost(100)
        val nextDay = camp.day + 1
        val line = if (nextDay >= DAYS) "Day $DAYS: FIGHT DAY. Freshness $freshness%." else "Day $nextDay begins. Freshness $freshness%."
        return camp.copy(
            day = nextDay, energy = ENERGY_PER_DAY, freshness = freshness, boughtEnergyToday = false,
            trainedToday = emptyList(), log = camp.log + line
        )
    }

    /** The training-log entry for a finished camp day, including the permanent attribute XP the work earned. */
    fun dayOutcome(camp: FightCamp): SessionOutcome? {
        val work = camp.trainedToday
        if (work.isEmpty()) return null
        val gains = LinkedHashMap<Attribute, Int>()
        for (t in work.distinct()) {
            val attr = t.attribute ?: continue
            val repeats = work.count { it == t }
            var points = 0
            for (i in 0 until repeats) points += t.gain shr i
            gains[attr] = (gains[attr] ?: 0) + points * PERMANENT_XP_PER_POINT
        }
        if (work.any { it == CampTraining.RECOVERY }) gains[Attribute.STAMINA] = (gains[Attribute.STAMINA] ?: 0) + 30
        return SessionOutcome(
            type = SessionType.CAMP,
            title = "Fight Camp day ${camp.day}: " + work.joinToString(", ") { it.label },
            durationSec = work.size * 20 * 60,
            xp = 40 + work.size * 30,
            performance = camp.freshness.coerceIn(0, 100),
            gains = gains,
            reps = work.size,
            tag = camp.day.toString(),
            success = !isOvertrained(camp)
        )
    }

    /** Starting stamina on fight day: a fresh fighter starts full, an overtrained one starts in a hole. */
    fun startStamina(camp: FightCamp): Int = (55 + camp.freshness * 0.45).toInt().coerceIn(55, 100)

    fun campScore(result: FightResult, camp: FightCamp): Int =
        (result.overall * 0.7 + camp.readiness * 0.3 + if (result.won) 5 else 0).toInt().coerceIn(0, 100)

    fun freshnessLabel(freshness: Int): String = when {
        freshness >= 85 -> "PEAKING"
        freshness >= 65 -> "FRESH"
        freshness >= OVERTRAINED_BELOW -> "TIRED"
        else -> "OVERTRAINED"
    }

    /** Camp advice tailored to the opponent's weaknesses. */
    fun suggestedTraining(opponent: Opponent): List<CampTraining> {
        val out = LinkedHashSet<CampTraining>()
        for (w in opponent.weaknesses) when (w) {
            Tactic.CLINCH -> out += CampTraining.CLINCH
            Tactic.LEGS, Tactic.DISTANCE -> out += CampTraining.KICKING
            Tactic.COUNTER, Tactic.ANGLE -> { out += CampTraining.DEFENSE; out += CampTraining.FIGHT_IQ }
            Tactic.PRESSURE, Tactic.ELBOW -> { out += CampTraining.BOXING; out += CampTraining.CONDITIONING }
            Tactic.PACE -> out += CampTraining.FIGHT_IQ
            else -> {}
        }
        if (out.isEmpty()) { out += CampTraining.FIGHT_IQ; out += CampTraining.CONDITIONING; out += CampTraining.DEFENSE }
        return out.toList()
    }
}
