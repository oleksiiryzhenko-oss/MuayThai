package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CoachTip
import com.muaythai.arena.domain.model.FightCamp
import com.muaythai.arena.domain.model.PersonalRecord
import com.muaythai.arena.domain.model.RecordKey
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.TrainingSession

/** Rule-based coach. Every tip is derived from locally stored statistics and points at a screen that fixes it. */
object CoachEngine {

    object Dest {
        const val DEFENSE = "defense"
        const val CLINCH = "clinch"
        const val REACTION = "reaction"
        const val COMBOS = "combos"
        const val STRIKING = "striking"
        const val PROGRAMS = "programs"
        const val ROUND = "round"
        const val FIGHT_LAB = "fightlab"
        const val CAMP = "camp"
    }

    fun tips(s: PlayerStats, camp: FightCamp): List<CoachTip> {
        val out = ArrayList<CoachTip>()
        fun l(a: Attribute) = s.levels[a] ?: 0
        val weakest = s.levels.minByOrNull { it.value }
        val strongest = s.levels.maxByOrNull { it.value }

        if (s.totalSessions == 0) {
            out += CoachTip("Start here", "Wrap your hands. Begin with a Defense Lab session and one round on the timer to set your baseline.", Dest.DEFENSE, 100)
        }
        if (s.defenseAccuracy in 0..59) {
            out += CoachTip("Defense is leaking", "Your defense is currently your weakest area at ${s.defenseAccuracy}% accuracy. Complete Defense Lab before your next fight.", Dest.DEFENSE, 95)
        }
        if (s.avgFightStamina in 0..59) {
            out += CoachTip("Fading late", "Your striking is strong, but your performance drops in later rounds. Focus on conditioning.", Dest.ROUND, 90)
        }
        if (camp.active && camp.isFightDay) {
            out += CoachTip("Fight day", "Camp is done. Trust the work and go fight.", Dest.CAMP, 99)
        } else if (camp.active && FightCampEngine.isOvertrained(camp)) {
            out += CoachTip("Overtrained", "Freshness is down to ${camp.freshness}%. Spend your next camp energy on recovery or you will start the fight tired.", Dest.CAMP, 97)
        }
        if (l(Attribute.KICKING) - l(Attribute.CLINCH) >= 8) {
            out += CoachTip("One-range fighter", "Your long-range game is strong. Develop your clinch to become more complete.", Dest.CLINCH, 70)
        }
        if (l(Attribute.CLINCH) - l(Attribute.KICKING) >= 8) {
            out += CoachTip("Dangerous inside only", "You are strong in the clinch but have little to offer at range. Drill your teep and roundhouse.", Dest.STRIKING, 70)
        }
        if (s.avgReactionMs in 1..650 || l(Attribute.REACTION) >= (s.overall + 6)) {
            out += CoachTip("Fast eyes", "Your reaction speed is above average. Counter-striking challenges are recommended.", Dest.FIGHT_LAB, 55)
        }
        if (s.reactionAccuracy in 0..69) {
            out += CoachTip("Guessing, not reading", "Reaction accuracy is ${s.reactionAccuracy}%. Slow down and pick the right answer before chasing speed.", Dest.REACTION, 75)
        }
        if (s.clinchSessions >= 3 && s.clinchIq < 55) {
            out += CoachTip("Losing the neck", "Your Clinch IQ is ${s.clinchIq}. Review inside position and posture in Clinch Lab.", Dest.CLINCH, 78)
        }
        if (s.fights >= 3 && s.losses > s.wins) {
            out += CoachTip("Rebuild", "You have lost more than you have won. Run a full Fight Camp before your next bout.", Dest.CAMP, 85)
        }
        if (s.fights == 0 && s.totalSessions >= 3) {
            out += CoachTip("Time to test it", "You have trained enough to step in. Take your first fight in Fight Lab.", Dest.FIGHT_LAB, 80)
        }
        if (s.comboReps < 30 && s.totalSessions >= 2) {
            out += CoachTip("Single shots only", "You rarely chain strikes together. Build and drill a combination in Combination Lab.", Dest.COMBOS, 60)
        }
        if (s.daysSinceLastSession >= 2) {
            out += CoachTip("Ring rust", "It has been ${s.daysSinceLastSession} days since your last session. A short program gets you moving again.", Dest.PROGRAMS, 88)
        }
        if (s.streak >= 3) {
            out += CoachTip("Momentum", "${s.streak} days in a row. Keep the streak alive with one focused session today.", Dest.PROGRAMS, 40)
        }
        if (weakest != null && strongest != null && strongest.value - weakest.value >= 10) {
            out += CoachTip(
                "Close the gap",
                "${weakest.key.label} (${weakest.value}) is well behind ${strongest.key.label} (${strongest.value}). Opponents will find it.",
                destinationFor(weakest.key), 65
            )
        }
        if (out.isEmpty() && weakest != null) {
            out += CoachTip("Sharpen the weakest tool", "Everything is developing evenly. ${weakest.key.label} is your lowest attribute at ${weakest.value}.", destinationFor(weakest.key), 30)
        }
        return out.sortedByDescending { it.priority }
    }

    fun destinationFor(attribute: Attribute): String = when (attribute) {
        Attribute.STRIKING, Attribute.KICKING, Attribute.POWER -> Dest.STRIKING
        Attribute.SPEED -> Dest.COMBOS
        Attribute.DEFENSE, Attribute.FIGHT_IQ -> Dest.DEFENSE
        Attribute.CLINCH -> Dest.CLINCH
        Attribute.REACTION -> Dest.REACTION
        Attribute.CONDITIONING, Attribute.STAMINA -> Dest.ROUND
    }

    fun destinationLabel(destination: String): String = when (destination) {
        Dest.DEFENSE -> "Open Defense Lab"
        Dest.CLINCH -> "Open Clinch Lab"
        Dest.REACTION -> "Open Reaction Arena"
        Dest.COMBOS -> "Open Combination Lab"
        Dest.STRIKING -> "Open Striking"
        Dest.PROGRAMS -> "Open Programs"
        Dest.ROUND -> "Start Round Work"
        Dest.FIGHT_LAB -> "Open Fight Lab"
        Dest.CAMP -> "Open Fight Camp"
        else -> "Go"
    }
}

/** Personal records: decides which records a new result breaks. */
object RecordEngine {

    data class Candidate(val key: RecordKey, val value: Int, val detail: String)

    fun candidates(
        stats: PlayerStats, session: TrainingSession?, fight: FightResult?, xpToday: Int, campScore: Int?
    ): List<Candidate> {
        val out = ArrayList<Candidate>()
        if (session != null) {
            when (session.type.name) {
                "REACTION" -> session.tag.toIntOrNull()?.takeIf { it > 0 }?.let { out += Candidate(RecordKey.FASTEST_REACTION, it, "Reaction Arena") }
                "DEFENSE" -> if (session.total >= 5) out += Candidate(RecordKey.HIGHEST_DEFENSE_ACCURACY, session.performance, "${session.correct}/${session.total} correct")
                "COMBINATION" -> out += Candidate(RecordKey.MOST_COMBOS, session.reps, session.title)
            }
        }
        if (fight != null) {
            out += Candidate(RecordKey.HIGHEST_FIGHT_IQ, fight.fightIq, "vs ${fight.opponentName}")
            out += Candidate(RecordKey.HIGHEST_FIGHT_SCORE, fight.overall, "vs ${fight.opponentName}")
        }
        out += Candidate(RecordKey.LONGEST_WIN_STREAK, stats.bestWinStreak, "consecutive wins")
        out += Candidate(RecordKey.MOST_XP_DAY, xpToday, "in a single day")
        out += Candidate(RecordKey.LONGEST_TRAINING_STREAK, stats.bestStreak, "days in a row")
        stats.levels.maxByOrNull { it.value }?.let { out += Candidate(RecordKey.HIGHEST_ATTRIBUTE, it.value, it.key.label) }
        if (campScore != null) out += Candidate(RecordKey.BEST_CAMP, campScore, fight?.let { "vs ${it.opponentName}" } ?: "Fight Camp")
        return out.filter { it.value > 0 }
    }

    fun beats(candidate: Candidate, current: PersonalRecord?): Boolean = when {
        current == null -> true
        candidate.key.lowerIsBetter -> candidate.value < current.value
        else -> candidate.value > current.value
    }

    /** Quiet records are stored but only announced once there is a previous value to beat. */
    private val quietOnFirst = setOf(
        RecordKey.MOST_XP_DAY, RecordKey.LONGEST_TRAINING_STREAK, RecordKey.HIGHEST_ATTRIBUTE, RecordKey.LONGEST_WIN_STREAK
    )

    fun shouldAnnounce(candidate: Candidate, current: PersonalRecord?): Boolean =
        current != null || candidate.key !in quietOnFirst

    fun describe(candidate: Candidate): String {
        val unit = if (candidate.key.unit.isEmpty()) "" else " ${candidate.key.unit}"
        return "${candidate.key.label}: ${candidate.value}$unit"
    }
}
