package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.model.Achievement
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.Rank

object AchievementEngine {

    private class Rule(val achievement: Achievement, val test: (PlayerStats) -> Boolean)

    private fun a(id: String, title: String, description: String, xp: Int, gold: Boolean = false, test: (PlayerStats) -> Boolean) =
        Rule(Achievement(id, title, description, xp, gold), test)

    private val rules: List<Rule> = listOf(
        // fights
        a("first_blood", "First Blood", "Win your first simulated fight", 150) { it.wins >= 1 },
        a("five_round", "Five Round Warrior", "Complete a full 5-round fight", 150) { it.fullFights >= 1 },
        a("wins5", "On the Card", "Win 5 fights", 250) { it.wins >= 5 },
        a("wins15", "Stadium Regular", "Win 15 fights", 500) { it.wins >= 15 },
        a("wins30", "Headliner", "Win 30 fights", 900, true) { it.wins >= 30 },
        a("stoppage", "Lights Out", "Win a fight by stoppage", 250) { it.stoppageWins >= 1 },
        a("stoppage5", "Finisher", "Win 5 fights by stoppage", 600) { it.stoppageWins >= 5 },
        a("tactical_mind", "Tactical Mind", "Score 90+ Fight IQ in a fight", 400) { it.bestFightIq >= 90 },
        a("score85", "Masterclass", "Score 85+ overall fight performance", 400) { it.bestFightScore >= 85 },
        a("counter_master", "Counter Master", "Win 10 fights using counter strategy", 700) { it.counterWins >= 10 },
        a("kick_artist", "Kick Artist", "Win 10 fights using a kicking strategy", 700) { it.kickWins >= 10 },
        a("neck_breaker", "Neck Breaker", "Win 5 fights using clinch strategy", 500) { it.clinchStrategyWins >= 5 },
        a("iron_gas", "Iron Gas Tank", "Complete a fight with a stamina score of 80+", 300) { it.highStaminaFights >= 1 },
        a("streak3", "Hot Streak", "Win 3 fights in a row", 250) { it.bestWinStreak >= 3 },
        a("unbeaten", "Unbeaten", "Win 10 consecutive fights", 1000, true) { it.bestWinStreak >= 10 },
        a("tier2", "Regional Contender", "Beat an opponent from the Regional Card", 300) { it.maxTierBeaten >= 2 },
        a("tier3", "National Level", "Beat an opponent from the National Stadium tier", 500) { it.maxTierBeaten >= 3 },
        a("tier4", "Elite Company", "Beat an opponent from the Elite Division", 800, true) { it.maxTierBeaten >= 4 },
        a("roster10", "Ladder Climber", "Beat 10 different opponents", 600) { it.opponentsBeaten >= 10 },
        a("roster30", "Cleaned Out the Division", "Beat all 30 opponents", 2500, true) { it.opponentsBeaten >= 30 },
        a("king", "King of the Arena", "Defeat the champion", 2000, true) { it.championBeaten },
        // camp
        a("first_camp", "First Camp", "Complete your first Fight Camp", 300) { it.campsCompleted >= 1 },
        a("camp5", "Camp Veteran", "Complete 5 Fight Camps", 700) { it.campsCompleted >= 5 },
        // technique
        a("low_kick", "Low Kick Specialist", "Complete 500 low-kick repetitions", 500) { it.lowKickReps >= 500 },
        a("reps100", "First Hundred", "Drill 100 technique repetitions", 150) { it.techniqueReps >= 100 },
        a("reps1000", "Thousand Strikes", "Drill 1,000 technique repetitions", 600) { it.techniqueReps >= 1000 },
        a("reps5000", "Five Thousand Strikes", "Drill 5,000 technique repetitions", 1500, true) { it.techniqueReps >= 5000 },
        a("hands", "Heavy Hands", "Throw 500 punch repetitions", 400) { it.punchReps >= 500 },
        a("short_range", "Knees and Elbows", "Land 300 knee or elbow repetitions", 400) { it.kneeElbowReps >= 300 },
        a("master1", "Signature Weapon", "Master one technique", 500) { it.techniquesMastered >= 1 },
        a("master5", "Arsenal", "Master 5 techniques", 1200, true) { it.techniquesMastered >= 5 },
        // combinations
        a("combo_builder", "Combination Builder", "Save your own combination", 100) { it.customCombos >= 1 },
        a("combo100", "Flow State", "Complete 100 combination repetitions", 250) { it.comboReps >= 100 },
        a("combo1000", "Muscle Memory", "Complete 1,000 combination repetitions", 900) { it.comboReps >= 1000 },
        a("combo_master", "Combination Master", "Bring one combination to 100% mastery", 500) { it.masteredCombos >= 1 },
        // labs
        a("clinch_king", "Clinch King", "Win 25 clinch challenges", 800, true) { it.clinchWins >= 25 },
        a("clinch_first", "Inside Position", "Win your first clinch challenge", 120) { it.clinchWins >= 1 },
        a("reaction_machine", "Reaction Machine", "Achieve 90% reaction accuracy in a session", 300) { it.bestReactionAccuracy >= 90 },
        a("lightning", "Lightning", "Record a reaction under 400 ms", 400) { it.bestReactionMs in 1..399 },
        a("react500", "Seen It All", "Complete 500 reaction scenarios", 600) { it.reactionPrompts >= 500 },
        a("iron_guard", "Iron Guard", "Score 100% in a Defense Lab session", 300) { it.bestDefenseAccuracy >= 100 },
        a("defense10", "Student of Defense", "Complete 10 Defense Lab sessions", 300) { it.defenseSessions >= 10 },
        // habit
        a("first_session", "Wrapped Up", "Complete your first training session", 50) { it.totalSessions >= 1 },
        a("sessions50", "Gym Rat", "Complete 50 training sessions", 500) { it.totalSessions >= 50 },
        a("sessions200", "Lives at the Gym", "Complete 200 training sessions", 1500, true) { it.totalSessions >= 200 },
        a("streak7", "Seven Day Camp", "Train 7 days in a row", 500) { it.bestStreak >= 7 },
        a("streak30", "Iron Discipline", "Train 30 days in a row", 2000, true) { it.bestStreak >= 30 },
        a("rounds25", "Round Machine", "Complete 25 rounds of round work", 500) { it.rounds >= 25 },
        a("programs10", "Programmed", "Finish 10 training programs", 400) { it.programs >= 10 },
        a("daily10", "Mission Driven", "Complete 10 daily challenges", 400) { it.dailyCompleted >= 10 },
        a("weekly5", "Long Game", "Complete 5 weekly challenges", 600) { it.weeklyCompleted >= 5 },
        a("all_round", "All-Rounder", "Train in every skill category", 300) { it.categoriesTrained >= 8 },
        // progression
        a("level10", "Double Digits", "Reach level 10", 300) { it.level >= 10 },
        a("level25", "Seasoned", "Reach level 25", 800) { it.level >= 25 },
        a("complete_fighter", "Complete Fighter", "Reach level 50", 2500, true) { it.level >= 50 },
        a("attr70", "Specialist", "Raise any attribute to 70", 500) { s -> s.levels.values.any { it >= 70 } },
        a("attr90", "World Class", "Raise any attribute to 90", 1500, true) { s -> s.levels.values.any { it >= 90 } },
        a("balanced60", "No Weakness", "Raise every attribute to 60", 1500, true) { s ->
            s.levels.size == Attribute.entries.size && s.levels.values.all { it >= 60 }
        },
        a("rank_fighter", "Fighter", "Reach the rank of Fighter", 800) { it.rank.ordinal >= Rank.FIGHTER.ordinal },
        a("champion", "Champion", "Reach the highest rank", 3000, true) { it.rank == Rank.CHAMPION }
    )

    val all: List<Achievement> = rules.map { it.achievement }
    private val byId = all.associateBy { it.id }
    fun get(id: String): Achievement? = byId[id]

    /** Achievements whose condition holds now and which are not in [unlocked] yet. */
    fun newlyUnlocked(stats: PlayerStats, unlocked: Set<String>): List<Achievement> =
        rules.filter { it.achievement.id !in unlocked && it.test(stats) }.map { it.achievement }
}
