package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.model.Achievement
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.domain.model.ChallengeRecord
import com.muaythai.arena.domain.model.ChallengeTemplate
import com.muaythai.arena.domain.model.Combination
import com.muaythai.arena.domain.model.FightCamp
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.Fighter
import com.muaythai.arena.domain.model.GameState
import com.muaythai.arena.domain.model.OpponentRecord
import com.muaythai.arena.domain.model.PersonalRecord
import com.muaythai.arena.domain.model.RewardSummary
import com.muaythai.arena.domain.model.SessionOutcome
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.TrainingSession
import com.muaythai.arena.domain.model.encodeGains

data class Transition(val state: GameState, val reward: RewardSummary)

/**
 * The single place where progress is applied. Given the current [GameState] and something the player just did,
 * it returns the next state plus a [RewardSummary] for the UI. It is pure: the repository only has to persist the result.
 */
object GameReducer {
    const val TECHNIQUE_MASTERY_BONUS = 300
    const val STARTING_COINS = 100

    fun createFighter(name: String, style: Archetype, now: Long): GameState {
        val fighter = Fighter(
            name = name.trim().ifBlank { "Fighter" }.take(18), style = style, xp = 0, coins = STARTING_COINS, rankPoints = 0, badges = 0,
            wins = 0, losses = 0, draws = 0, winStreak = 0, bestWinStreak = 0, campsCompleted = 0, createdAt = now
        )
        val combos = Techniques.starterCombinations.mapIndexed { i, (n, strikes) ->
            Combination(name = n, strikes = strikes, favorite = i < 3, custom = false, createdAt = now + i)
        }
        return GameState(
            fighter = fighter, attributeXp = ProgressionEngine.startingAttributeXp(style), combinations = combos, loaded = true
        )
    }

    /** Keeps the name, style and training log, and wipes everything earned. */
    fun resetProgression(state: GameState, now: Long): GameState {
        val old = state.fighter ?: return state
        val fresh = createFighter(old.name, old.style, now)
        return fresh.copy(sessions = state.sessions)
    }

    fun applySession(state: GameState, outcome: SessionOutcome, today: Long, now: Long, fight: FightResult? = null): Transition {
        val fighter0 = state.fighter ?: return Transition(state, RewardSummary(outcome.title, 0))
        val levelBefore = ProgressionEngine.fighterLevel(fighter0.xp)
        val rankBefore = ProgressionEngine.rank(levelBefore, fighter0.rankPoints)
        val levelsBefore = ProgressionEngine.levels(state.attributeXp)

        var bonusXp = 0
        var coins = 0
        var rankPoints = 0
        var badges = 0
        val milestones = ArrayList<String>()
        val lines = ArrayList<String>()

        // 1. attributes
        val attributeXp = Attribute.entries.associateWith { (state.attributeXp[it] ?: 0) + (outcome.gains[it] ?: 0) }
        val levelsAfter = ProgressionEngine.levels(attributeXp)
        val levelUps = Attribute.entries.filter { (levelsAfter[it] ?: 0) > (levelsBefore[it] ?: 0) }.map { it to (levelsAfter[it] ?: 0) }

        // 2. technique and combination mastery
        var techniqueReps = state.techniqueReps
        if (outcome.type == SessionType.TECHNIQUE) {
            Techniques.get(outcome.tag)?.let { t ->
                val before = techniqueReps[t.id] ?: 0
                val after = before + outcome.reps
                techniqueReps = techniqueReps + (t.id to after)
                if (before < t.masteryReps && after >= t.masteryReps) {
                    bonusXp += TECHNIQUE_MASTERY_BONUS
                    milestones += "${t.name} mastered  +$TECHNIQUE_MASTERY_BONUS XP"
                }
            }
        }
        var combinations = state.combinations
        if (outcome.type == SessionType.COMBINATION) {
            val id = outcome.tag.toLongOrNull()
            combinations = combinations.map { c ->
                if (c.id != id) c else {
                    val reps = c.reps + outcome.reps
                    val due = CombinationEngine.unclaimedMilestones(reps, c.milestonesClaimed)
                    for (m in due) {
                        bonusXp += m.xp
                        milestones += "${c.name}: ${m.reps} repetitions  +${m.xp} XP"
                    }
                    c.copy(reps = reps, milestonesClaimed = c.milestonesClaimed + due.size)
                }
            }
        }

        // 3. fight bookkeeping
        var fighter = fighter0
        var fights = state.fights
        var opponentRecords = state.opponentRecords
        var camp = state.camp
        var campScore: Int? = null
        if (fight != null) {
            fights = fights + fight
            val streak = if (fight.won) fighter.winStreak + 1 else 0
            fighter = fighter.copy(
                wins = fighter.wins + if (fight.won) 1 else 0,
                losses = fighter.losses + if (!fight.won && !fight.draw) 1 else 0,
                draws = fighter.draws + if (fight.draw) 1 else 0,
                winStreak = streak,
                bestWinStreak = maxOf(fighter.bestWinStreak, streak)
            )
            coins += FightSimulationEngine.coins(fight)
            rankPoints += FightSimulationEngine.rankPoints(fight)
            val old = opponentRecords[fight.opponentId] ?: OpponentRecord(fight.opponentId, 0, 0, 0)
            val firstWin = fight.won && old.wins == 0
            opponentRecords = opponentRecords + (fight.opponentId to old.copy(
                wins = old.wins + if (fight.won) 1 else 0,
                losses = old.losses + if (!fight.won && !fight.draw) 1 else 0,
                lastFought = now
            ))
            if (firstWin) lines += "New opponent defeated: ${fight.opponentName}"
            val tier = fight.opponentTier
            if (firstWin && tier < Opponents.MAX_TIER &&
                !OpponentEngine.isTierUnlocked(tier + 1, state.opponentRecords) && OpponentEngine.isTierUnlocked(tier + 1, opponentRecords)
            ) {
                lines += "Unlocked: ${OpponentEngine.tierName(tier + 1)}"
            }
            if (fight.campFight && camp.active) {
                campScore = FightCampEngine.campScore(fight, camp)
                fighter = fighter.copy(campsCompleted = fighter.campsCompleted + 1)
                lines += "Fight Camp complete. Camp score $campScore"
                camp = FightCamp()
            }
        }

        // 4. the log entry
        var session = TrainingSession(
            timestamp = now, epochDay = today, type = outcome.type, title = outcome.title, durationSec = outcome.durationSec,
            xp = outcome.xp + bonusXp, performance = outcome.performance, reps = outcome.reps, correct = outcome.correct,
            total = outcome.total, tag = outcome.tag, success = outcome.success, valueMs = outcome.valueMs,
            attributeGains = encodeGains(outcome.gains)
        )
        val sessions = state.sessions + session

        // 5. challenges
        val completed: List<ChallengeTemplate> = ChallengeEngine.newlyCompleted(sessions, state.challenges, today)
        var challenges = state.challenges
        for (t in completed) {
            challenges = challenges + ChallengeRecord(ChallengeEngine.recordKey(t, today), t.id, t.weekly, ChallengeEngine.periodKey(t.weekly, today), now)
            bonusXp += t.xp; coins += t.coins; rankPoints += t.rankPoints
        }
        if (ChallengeEngine.sweepEarned(challenges.map { it.key }.toSet(), today)) {
            challenges = challenges + ChallengeRecord(ChallengeEngine.sweepKey(today), ChallengeEngine.SWEEP_ID, false, ChallengeEngine.periodKey(false, today), now)
            bonusXp += ChallengeEngine.SWEEP_BONUS_XP
            badges += 1
            lines += "Today's mission complete  +${ChallengeEngine.SWEEP_BONUS_XP} XP and a mission badge"
        }

        // 6. achievements (repeat, because achievement XP can itself push the level over a threshold)
        var achievements = state.achievements
        val unlocked = ArrayList<Achievement>()
        var achievementXp = 0
        fun build(): GameState = state.copy(
            fighter = fighter.copy(
                xp = fighter0.xp + outcome.xp + bonusXp + achievementXp,
                coins = fighter0.coins + coins, rankPoints = fighter0.rankPoints + rankPoints, badges = fighter0.badges + badges
            ),
            attributeXp = attributeXp, sessions = sessions, combinations = combinations, techniqueReps = techniqueReps,
            fights = fights, opponentRecords = opponentRecords, achievements = achievements, challenges = challenges, camp = camp
        )
        repeat(3) {
            val stats = StatsEngine.compute(build(), today)
            val fresh = AchievementEngine.newlyUnlocked(stats, achievements.keys)
            if (fresh.isEmpty()) return@repeat
            unlocked += fresh
            achievementXp += fresh.sumOf { it.xp }
            achievements = achievements + fresh.associate { it.id to now }
        }

        // 7. personal records
        var next = build()
        val stats = StatsEngine.compute(next, today)
        val xpToday = sessions.filter { it.epochDay == today }.sumOf { it.xp }
        var records = state.records
        val announced = ArrayList<String>()
        for (c in RecordEngine.candidates(stats, session, fight, xpToday, campScore)) {
            val current = records[c.key]
            if (!RecordEngine.beats(c, current)) continue
            if (RecordEngine.shouldAnnounce(c, current)) announced += RecordEngine.describe(c)
            records = records + (c.key to PersonalRecord(c.key, c.value, c.detail, now))
        }
        if (announced.isNotEmpty()) {
            session = session.copy(note = announced.joinToString("; ") { "PR $it" })
            next = next.copy(sessions = state.sessions + session)
        }
        next = next.copy(records = records)

        val finalFighter = next.fighter ?: fighter
        val levelAfter = ProgressionEngine.fighterLevel(finalFighter.xp)
        val reward = RewardSummary(
            title = outcome.title,
            xp = outcome.xp,
            bonusXp = bonusXp + achievementXp,
            coins = coins,
            rankPoints = rankPoints,
            gains = outcome.gains.filterValues { it > 0 },
            attributeLevelUps = levelUps,
            levelBefore = levelBefore,
            levelAfter = levelAfter,
            rankBefore = rankBefore,
            rankAfter = ProgressionEngine.rank(levelAfter, finalFighter.rankPoints),
            achievements = unlocked,
            challenges = completed,
            records = announced,
            milestones = milestones,
            lines = lines
        )
        return Transition(next, reward)
    }

    // ---- fight camp

    fun startCamp(state: GameState, opponentId: Int): GameState = state.copy(camp = FightCampEngine.start(opponentId))

    fun abandonCamp(state: GameState): GameState = state.copy(camp = FightCamp())

    fun campTrain(state: GameState, training: CampTraining): Pair<GameState, CampStep> {
        val step = FightCampEngine.train(state.camp, training)
        return state.copy(camp = step.camp) to step
    }

    fun campBuyEnergy(state: GameState): Pair<GameState, CampStep> {
        val fighter = state.fighter ?: return state to CampStep(state.camp, "No fighter.", false)
        val step = FightCampEngine.buyEnergy(state.camp, fighter.coins)
        if (!step.applied) return state to step
        return state.copy(camp = step.camp, fighter = fighter.copy(coins = fighter.coins - FightCampEngine.EXTRA_ENERGY_COST)) to step
    }

    /** Ends the camp day: logs the day's work as a session (if any) and moves to the next morning. */
    fun campEndDay(state: GameState, today: Long, now: Long): Transition {
        val camp = state.camp
        if (!camp.active || camp.isFightDay) return Transition(state, RewardSummary("Fight Camp", 0))
        val outcome = FightCampEngine.dayOutcome(camp)
        val advanced = FightCampEngine.endDay(camp)
        if (outcome == null) {
            return Transition(state.copy(camp = advanced), RewardSummary("Rest day", 0, lines = listOf("You rested. Freshness is now ${advanced.freshness}%.")))
        }
        val t = applySession(state, outcome, today, now)
        return Transition(t.state.copy(camp = advanced), t.reward.copy(lines = t.reward.lines + "Freshness is now ${advanced.freshness}%."))
    }
}
