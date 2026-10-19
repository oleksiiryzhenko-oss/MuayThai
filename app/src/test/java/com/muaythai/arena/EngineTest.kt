package com.muaythai.arena

import com.muaythai.arena.domain.content.ClinchScenarios
import com.muaythai.arena.domain.content.DefenseScenarios
import com.muaythai.arena.domain.content.FightScenarios
import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.content.Programs
import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.engine.AchievementEngine
import com.muaythai.arena.domain.engine.ChallengeEngine
import com.muaythai.arena.domain.engine.ClinchEngine
import com.muaythai.arena.domain.engine.CoachEngine
import com.muaythai.arena.domain.engine.CombinationEngine
import com.muaythai.arena.domain.engine.DefenseEngine
import com.muaythai.arena.domain.engine.FightCampEngine
import com.muaythai.arena.domain.engine.FightSimulationEngine
import com.muaythai.arena.domain.engine.GameReducer
import com.muaythai.arena.domain.engine.MuayThaiTrainingEngine
import com.muaythai.arena.domain.engine.OpponentEngine
import com.muaythai.arena.domain.engine.ProgressionEngine
import com.muaythai.arena.domain.engine.ReactionEngine
import com.muaythai.arena.domain.engine.ReactionResult
import com.muaythai.arena.domain.engine.StatsEngine
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.domain.model.CornerInstruction
import com.muaythai.arena.domain.model.FightPhase
import com.muaythai.arena.domain.model.FightState
import com.muaythai.arena.domain.model.GameState
import com.muaythai.arena.domain.model.Rank
import com.muaythai.arena.domain.model.ReactionRank
import com.muaythai.arena.domain.model.RecordKey
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.domain.model.Strike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EngineTest {
    private val today = 20_000L
    private val now = 1_700_000_000_000L

    private fun newGame(style: Archetype = Archetype.KICKER): GameState = GameReducer.createFighter("Rostyk", style, now)

    private fun playFight(state: FightState, pickBest: Boolean): FightState {
        var s = state
        var guard = 0
        while (s.phase != FightPhase.FINISHED && guard++ < 400) {
            s = when (s.phase) {
                FightPhase.PRE_ROUND -> FightSimulationEngine.chooseStrategy(s, if (pickBest) {
                    Strategy.entries.maxByOrNull { OpponentEngine.matchup(it, s.opponentStrategy) } ?: Strategy.BALANCED
                } else Strategy.BALANCED)
                FightPhase.EVENT -> {
                    val options = s.currentEvent!!.options
                    val index = if (pickBest) options.indices.maxByOrNull { options[it].quality }!! else options.indices.minByOrNull { options[it].quality }!!
                    FightSimulationEngine.resolveEvent(s, index)
                }
                FightPhase.EVENT_RESULT -> FightSimulationEngine.advance(s)
                FightPhase.CORNER -> FightSimulationEngine.applyCorner(s, FightSimulationEngine.cornerReport(s)?.suggested ?: CornerInstruction.INCREASE_VOLUME)
                FightPhase.FINISHED -> s
            }
        }
        return s
    }

    // ---------------------------------------------------------------- content

    @Test fun demoContentMeetsTheRequiredVolume() {
        assertEquals(30, Opponents.all.size)
        assertTrue(FightScenarios.all.size >= 50)
        assertTrue(DefenseScenarios.all.size >= 40)
        assertTrue(ClinchScenarios.all.size >= 30)
        assertTrue(Techniques.starterCombinations.size >= 30)
        assertTrue(ChallengeEngine.daily.size >= 20)
        assertTrue(ChallengeEngine.weekly.size >= 20)
        assertTrue(AchievementEngine.all.size >= 40)
        assertTrue(Programs.all.size >= 15)
    }

    @Test fun contentIdsAreUnique() {
        assertEquals(Opponents.all.size, Opponents.all.map { it.id }.toSet().size)
        assertEquals(Techniques.all.size, Techniques.all.map { it.id }.toSet().size)
        assertEquals(AchievementEngine.all.size, AchievementEngine.all.map { it.id }.toSet().size)
        assertEquals(FightScenarios.all.size, FightScenarios.all.map { it.id }.toSet().size)
        val challenges = ChallengeEngine.daily + ChallengeEngine.weekly
        assertEquals(challenges.size, challenges.map { it.id }.toSet().size)
    }

    @Test fun everyScenarioHasExactlyOneBestAnswerAvailable() {
        for (s in FightScenarios.all) {
            assertEquals(4, s.options.size)
            assertTrue("scenario ${s.id} needs a best option", s.options.any { it.quality == 3 })
        }
        for (s in ClinchScenarios.all) assertEquals("clinch ${s.id}", 1, s.options.count { it.quality == 3 })
        for (s in DefenseScenarios.all) assertEquals(4, s.options.toSet().size)
    }

    @Test fun opponentsGetStrongerByTier() {
        for (tier in 1 until Opponents.MAX_TIER) {
            val lower = Opponents.inTier(tier).maxOf { it.overall }
            val higher = Opponents.inTier(tier + 1).minOf { it.overall }
            assertTrue("tier ${tier + 1} must be stronger than tier $tier", higher > lower)
        }
    }

    // ---------------------------------------------------------------- progression

    @Test fun fighterLevelsFollowTheXpCurve() {
        assertEquals(1, ProgressionEngine.fighterLevel(0))
        assertEquals(1, ProgressionEngine.fighterLevel(149))
        assertEquals(2, ProgressionEngine.fighterLevel(150))
        val p = ProgressionEngine.fighterProgress(150 + 215 + 10)
        assertEquals(3, p.level)
        assertEquals(10, p.xpIntoLevel)
    }

    @Test fun attributeLevelRoundTrips() {
        for (level in listOf(1, 2, 36, 44, 70, 99)) {
            assertEquals(level, ProgressionEngine.attributeLevel(ProgressionEngine.attributeXpForLevel(level)))
        }
    }

    @Test fun startingStyleBoostsItsAttributes() {
        val levels = ProgressionEngine.levels(ProgressionEngine.startingAttributeXp(Archetype.CLINCHER))
        assertEquals(ProgressionEngine.BASE_ATTRIBUTE_LEVEL + ProgressionEngine.STYLE_BONUS, levels[Attribute.CLINCH])
        assertEquals(ProgressionEngine.BASE_ATTRIBUTE_LEVEL, levels[Attribute.KICKING])
        assertEquals(Archetype.CLINCHER, ProgressionEngine.archetype(levels, Archetype.CLINCHER))
    }

    @Test fun rankNeedsBothLevelAndRankPoints() {
        assertEquals(Rank.NOVICE, ProgressionEngine.rank(1, 0))
        assertEquals(Rank.NOVICE, ProgressionEngine.rank(30, 0))
        assertEquals(Rank.BEGINNER, ProgressionEngine.rank(3, 30))
        assertEquals(Rank.CHAMPION, ProgressionEngine.rank(40, 2800))
        assertNull(ProgressionEngine.nextRankRequirement(40, 2800).next)
    }

    @Test fun trainingChangesTheArchetypeOverTime() {
        val xp = ProgressionEngine.startingAttributeXp(Archetype.COUNTER).toMutableMap()
        xp[Attribute.KICKING] = ProgressionEngine.attributeXpForLevel(80)
        xp[Attribute.SPEED] = ProgressionEngine.attributeXpForLevel(70)
        assertEquals(Archetype.KICKER, ProgressionEngine.archetype(ProgressionEngine.levels(xp), Archetype.COUNTER))
    }

    // ---------------------------------------------------------------- training engines

    @Test fun combinationDifficultyGrowsWithComplexity() {
        val simple = listOf(Strike.JAB, Strike.CROSS)
        val hard = listOf(Strike.JAB, Strike.RIGHT_ELBOW, Strike.LEFT_KNEE, Strike.RIGHT_HIGH_KICK, Strike.SWITCH_KICK)
        assertTrue(CombinationEngine.difficulty(hard) > CombinationEngine.difficulty(simple))
        assertTrue(CombinationEngine.xpValue(hard) > CombinationEngine.xpValue(simple))
        assertNotNull(CombinationEngine.validate(listOf(Strike.JAB)))
        assertNull(CombinationEngine.validate(simple))
    }

    @Test fun combinationMilestonesPayOnce() {
        assertEquals(2, CombinationEngine.unclaimedMilestones(60, 0).size)
        assertEquals(0, CombinationEngine.unclaimedMilestones(60, 2).size)
        assertEquals(200, CombinationEngine.nextMilestone(146)?.reps)
        assertEquals(73, CombinationEngine.mastery(146))
    }

    @Test fun defenseSessionKeepsTheCorrectAnswerAfterShuffling() {
        val original = DefenseScenarios.all.associateBy { it.id }
        for (s in DefenseEngine.buildSession(Random(7), 20)) {
            val source = original.getValue(s.id)
            assertEquals(source.options[source.correctIndex], s.options[s.correctIndex])
            assertTrue(DefenseEngine.isCorrect(s, s.correctIndex))
        }
        val outcome = DefenseEngine.outcome(6, 8, 90)
        assertEquals(75, outcome.performance)
        assertEquals(6 * DefenseEngine.IQ_XP_PER_CORRECT, outcome.gains[Attribute.FIGHT_IQ])
    }

    @Test fun reactionRanksRewardSpeedAndAccuracy() {
        assertEquals(ReactionRank.LIGHTNING, ReactionEngine.rank(400, 95))
        assertEquals(ReactionRank.BEGINNER, ReactionEngine.rank(400, 40))
        assertEquals(ReactionRank.BEGINNER, ReactionEngine.rank(1500, 100))
        val prompt = ReactionEngine.nextPrompt(Random(3))
        assertEquals(4, prompt.options.size)
        assertTrue(prompt.correct in prompt.options)
        val summary = ReactionEngine.summarize(listOf(ReactionResult(true, 500), ReactionResult(true, 700), ReactionResult(false, 300)))
        assertEquals(600, summary.averageMs)
        assertEquals(500, summary.bestMs)
        assertEquals(66, summary.accuracy)
    }

    @Test fun clinchScoreReflectsDecisionQuality() {
        assertEquals(100, ClinchEngine.score(listOf(3, 3, 3)))
        assertEquals(0, ClinchEngine.score(listOf(0, 0)))
        assertTrue(ClinchEngine.outcome(listOf(3, 3, 2, 3, 2, 3), 60).success)
        assertFalse(ClinchEngine.outcome(listOf(0, 1, 1, 0, 2, 1), 60).success)
    }

    // ---------------------------------------------------------------- fight simulation

    @Test fun fightIsDeterministicForASeed() {
        val user = OpponentEngine.userCombatStats(ProgressionEngine.levels(newGame().attributeXp), Archetype.KICKER)
        val a = FightSimulationEngine.result(playFight(FightSimulationEngine.start(Opponents.get(3)!!, user, 99), true), now)
        val b = FightSimulationEngine.result(playFight(FightSimulationEngine.start(Opponents.get(3)!!, user, 99), true), now)
        assertEquals(a, b)
    }

    @Test fun everyFightReachesAResultWithValidScorecards() {
        val user = OpponentEngine.userCombatStats(Attribute.entries.associateWith { 60 }, Archetype.TECHNICAL)
        for (opponent in Opponents.all) for (seed in 1L..8L) {
            val end = playFight(FightSimulationEngine.start(opponent, user, seed), seed % 2 == 0L)
            assertEquals(FightPhase.FINISHED, end.phase)
            val result = FightSimulationEngine.result(end, now)
            assertTrue(result.endRound in 1..FightSimulationEngine.ROUNDS)
            assertEquals(3, result.scorecards.size)
            assertTrue(result.overall in 15..99)
            if (result.method == "Decision") assertEquals(FightSimulationEngine.ROUNDS, end.rounds.size)
            assertFalse(result.won && result.draw)
        }
    }

    @Test fun goodDecisionsBeatBadDecisions() {
        val user = OpponentEngine.userCombatStats(Attribute.entries.associateWith { 44 }, Archetype.TECHNICAL)
        var smart = 0
        var careless = 0
        for (opponent in Opponents.inTier(1)) for (seed in 1L..20L) {
            if (FightSimulationEngine.result(playFight(FightSimulationEngine.start(opponent, user, seed), true), now).won) smart++
            if (FightSimulationEngine.result(playFight(FightSimulationEngine.start(opponent, user, seed), false), now).won) careless++
        }
        assertTrue("smart=$smart careless=$careless", smart > careless + 60)
    }

    @Test fun strategyMatchupsAreNotSymmetricNoise() {
        assertTrue(OpponentEngine.matchup(Strategy.COUNTER, com.muaythai.arena.domain.model.OpponentStrategy.PRESSURE) > 0)
        assertTrue(OpponentEngine.matchup(Strategy.PRESSURE, com.muaythai.arena.domain.model.OpponentStrategy.COUNTER) < 0)
        for (o in Opponents.all) assertTrue(OpponentEngine.scout(o).recommended.isNotEmpty())
    }

    @Test fun tiersUnlockAfterThreeWins() {
        var state = newGame()
        assertTrue(OpponentEngine.isTierUnlocked(1, state.opponentRecords))
        assertFalse(OpponentEngine.isTierUnlocked(2, state.opponentRecords))
        val user = OpponentEngine.userCombatStats(Attribute.entries.associateWith { 70 }, Archetype.TECHNICAL)
        for (id in 1..3) {
            var result = FightSimulationEngine.result(playFight(FightSimulationEngine.start(Opponents.get(id)!!, user, 5), true), now)
            var seed = 6L
            while (!result.won) result = FightSimulationEngine.result(playFight(FightSimulationEngine.start(Opponents.get(id)!!, user, seed++), true), now)
            state = GameReducer.applySession(state, FightSimulationEngine.sessionOutcome(result, 300), today, now, result).state
        }
        assertTrue(OpponentEngine.isTierUnlocked(2, state.opponentRecords))
        assertEquals(3, state.fighter!!.wins)
        assertEquals(3, state.fighter!!.winStreak)
    }

    // ---------------------------------------------------------------- reducer

    @Test fun sessionAwardsXpAttributesAndLogsIt() {
        val start = newGame()
        val technique = Techniques.get("low_outside")!!
        val t = GameReducer.applySession(start, MuayThaiTrainingEngine.drillOutcome(technique, 20, 120), today, now)
        val after = t.state
        assertEquals(1, after.sessions.size)
        assertEquals(20, after.techniqueReps["low_outside"])
        assertTrue(after.attributeXp.getValue(Attribute.KICKING) > start.attributeXp.getValue(Attribute.KICKING))
        assertTrue(after.fighter!!.xp >= t.reward.xp)
        assertEquals(after.fighter!!.xp, t.reward.totalXp)
        assertTrue("first session unlocks an achievement", t.reward.achievements.any { it.id == "first_session" })
    }

    @Test fun combinationRepsAccumulateAndPayMilestones() {
        var state = newGame()
        val combo = state.combinations.first().copy(id = 5)
        state = state.copy(combinations = listOf(combo))
        val t = GameReducer.applySession(state, CombinationEngine.practiceOutcome(combo, 30, 200), today, now)
        val updated = t.state.combinations.single()
        assertEquals(30, updated.reps)
        assertEquals(1, updated.milestonesClaimed)
        assertEquals(1, t.reward.milestones.size)
        val again = GameReducer.applySession(t.state, CombinationEngine.practiceOutcome(updated, 5, 30), today, now)
        assertTrue(again.reward.milestones.isEmpty())
    }

    @Test fun dailyChallengesCompleteOnceAndSweepPaysABadge() {
        var state = newGame()
        val missions = ChallengeEngine.activeDaily(today)
        assertEquals(3, missions.size)
        assertEquals(missions, ChallengeEngine.activeDaily(today))
        // Grind every activity until all three of today's missions are done.
        val combo = state.combinations.first().copy(id = 1)
        state = state.copy(combinations = listOf(combo) + state.combinations.drop(1))
        val user = OpponentEngine.userCombatStats(Attribute.entries.associateWith { 80 }, Archetype.TECHNICAL)
        var rewards = 0
        repeat(6) { i ->
            val outcomes = listOf(
                CombinationEngine.practiceOutcome(combo, 20, 600),
                DefenseEngine.outcome(8, 8, 60),
                ReactionEngine.outcome(List(20) { ReactionResult(true, 450) }, 60),
                ClinchEngine.outcome(List(6) { 3 }, 60),
                MuayThaiTrainingEngine.roundOutcome(180, 180),
                MuayThaiTrainingEngine.drillOutcome(Techniques.get("low_outside")!!, 50, 60),
                MuayThaiTrainingEngine.drillOutcome(Techniques.get("jab")!!, 40, 60),
                MuayThaiTrainingEngine.drillOutcome(Techniques.get("knee_straight")!!, 30, 60),
                MuayThaiTrainingEngine.programOutcome(Programs.all.first(), Programs.all.first().steps.size, 300)
            )
            for (o in outcomes) {
                val t = GameReducer.applySession(state, o, today, now + i)
                rewards += t.reward.challenges.size
                state = t.state
            }
            for (strategy in listOf(Strategy.LOW_KICK, Strategy.COUNTER)) {
                var seed = 1L
                var fight = FightSimulationEngine.result(playFight(FightSimulationEngine.start(Opponents.get(1)!!, user, seed), true), now)
                while (!fight.won) fight = FightSimulationEngine.result(playFight(FightSimulationEngine.start(Opponents.get(1)!!, user, ++seed), true), now)
                fight = fight.copy(mainStrategy = strategy)
                val t = GameReducer.applySession(state, FightSimulationEngine.sessionOutcome(fight, 300), today, now, fight)
                rewards += t.reward.challenges.size
                state = t.state
            }
        }
        val statuses = ChallengeEngine.dailyStatuses(state.sessions, state.challenges, today)
        assertTrue(statuses.toString(), statuses.all { it.completed })
        assertEquals(1, state.fighter!!.badges)
        assertEquals(1, state.challenges.count { it.templateId == ChallengeEngine.SWEEP_ID })
        // every challenge is paid exactly once
        assertEquals(state.challenges.size, state.challenges.map { it.key }.toSet().size)
        assertEquals(rewards, state.challenges.count { it.templateId != ChallengeEngine.SWEEP_ID })
        // a new day brings a fresh, unfinished set
        assertTrue(ChallengeEngine.dailyStatuses(state.sessions, state.challenges, today + 1).none { it.completed })
    }

    @Test fun personalRecordsOnlyImprove() {
        var state = newGame()
        state = GameReducer.applySession(state, ReactionEngine.outcome(List(20) { ReactionResult(true, 600) }, 60), today, now).state
        assertEquals(600, state.records.getValue(RecordKey.FASTEST_REACTION).value)
        state = GameReducer.applySession(state, ReactionEngine.outcome(List(20) { ReactionResult(true, 800) }, 60), today, now).state
        assertEquals(600, state.records.getValue(RecordKey.FASTEST_REACTION).value)
        val t = GameReducer.applySession(state, ReactionEngine.outcome(List(20) { ReactionResult(true, 420) }, 60), today, now)
        assertEquals(420, t.state.records.getValue(RecordKey.FASTEST_REACTION).value)
        assertTrue(t.reward.records.any { it.contains("Fastest reaction") })
        assertTrue(t.state.sessions.last().note.contains("PR"))
    }

    @Test fun streaksCountConsecutiveDays() {
        assertEquals(3, StatsEngine.currentStreak(setOf(10L, 11L, 12L), 12))
        assertEquals(3, StatsEngine.currentStreak(setOf(10L, 11L, 12L), 13))
        assertEquals(0, StatsEngine.currentStreak(setOf(10L, 11L, 12L), 14))
        assertEquals(4, StatsEngine.longestStreak(setOf(1L, 2L, 3L, 4L, 9L, 10L)))
        // 2022-01-03 (epoch day 18995) was a Monday: the week runs Monday .. Sunday.
        assertEquals(StatsEngine.weekIndex(18_995), StatsEngine.weekIndex(19_001))
        assertFalse(StatsEngine.weekIndex(19_001) == StatsEngine.weekIndex(19_002))
        assertEquals(18_995L, StatsEngine.weekStart(18_998))
    }

    // ---------------------------------------------------------------- camp and coach

    @Test fun overtrainingCostsFreshnessAndRecoveryRestoresIt() {
        var camp = FightCampEngine.start(1)
        repeat(3) { camp = FightCampEngine.train(camp, CampTraining.CONDITIONING).camp }
        assertEquals(0, camp.energy)
        assertFalse(FightCampEngine.train(camp, CampTraining.CLINCH).applied)
        val tired = camp.freshness
        camp = FightCampEngine.endDay(camp)
        assertEquals(2, camp.day)
        repeat(3) { camp = FightCampEngine.train(camp, CampTraining.CONDITIONING).camp }
        assertTrue(FightCampEngine.isOvertrained(camp))
        assertTrue(FightCampEngine.startStamina(camp) < 80)
        camp = FightCampEngine.endDay(camp)
        val before = camp.freshness
        camp = FightCampEngine.train(camp, CampTraining.RECOVERY).camp
        assertTrue(camp.freshness > before)
        assertTrue(tired < 80)
        // the same session three times in a day gives diminishing returns
        val first = FightCampEngine.projectedGain(FightCampEngine.start(1), CampTraining.FIGHT_IQ)
        val repeated = FightCampEngine.projectedGain(FightCampEngine.start(1).copy(trainedToday = listOf(CampTraining.FIGHT_IQ, CampTraining.FIGHT_IQ)), CampTraining.FIGHT_IQ)
        assertTrue(repeated < first)
    }

    @Test fun fullCampEndsWithAFightAndIsRecorded() {
        var state = GameReducer.startCamp(newGame(), 2)
        for (day in 1..6) {
            state = GameReducer.campTrain(state, CampTraining.FIGHT_IQ).first
            state = GameReducer.campTrain(state, CampTraining.DEFENSE).first
            state = GameReducer.campEndDay(state, today + day, now + day).state
        }
        assertTrue(state.camp.isFightDay)
        assertEquals(6, state.sessions.count { it.type == SessionType.CAMP })
        val levels = ProgressionEngine.levels(state.attributeXp)
        val user = OpponentEngine.userCombatStats(levels, Archetype.KICKER, state.camp.bonuses)
        assertTrue(user.fightIq > levels.getValue(Attribute.FIGHT_IQ))
        val end = playFight(FightSimulationEngine.start(Opponents.get(2)!!, user, 11, FightCampEngine.startStamina(state.camp), true), true)
        val result = FightSimulationEngine.result(end, now)
        val t = GameReducer.applySession(state, FightSimulationEngine.sessionOutcome(result, 300), today + 7, now, result)
        assertFalse(t.state.camp.active)
        assertEquals(1, t.state.fighter!!.campsCompleted)
        assertNotNull(t.state.records[RecordKey.BEST_CAMP])
        assertTrue(t.reward.achievements.any { it.id == "first_camp" })
    }

    @Test fun coachReactsToTheActualStatistics() {
        var state = newGame()
        val fresh = CoachEngine.tips(StatsEngine.compute(state, today), state.camp)
        assertTrue(fresh.first().title == "Start here")
        repeat(2) { state = GameReducer.applySession(state, DefenseEngine.outcome(2, 8, 60), today, now).state }
        val tips = CoachEngine.tips(StatsEngine.compute(state, today), state.camp)
        assertTrue(tips.any { it.destination == CoachEngine.Dest.DEFENSE && it.message.contains("weakest area") })
        repeat(6) { state = GameReducer.applySession(state, DefenseEngine.outcome(8, 8, 60), today, now).state }
        val later = CoachEngine.tips(StatsEngine.compute(state, today), state.camp)
        assertFalse(later.any { it.message.contains("weakest area") })
    }

    @Test fun resetKeepsTheNameAndTheLogButWipesProgress() {
        var state = newGame()
        state = GameReducer.applySession(state, DefenseEngine.outcome(8, 8, 60), today, now).state
        val reset = GameReducer.resetProgression(state, now)
        assertEquals("Rostyk", reset.fighter!!.name)
        assertEquals(0, reset.fighter!!.xp)
        assertEquals(1, reset.sessions.size)
        assertTrue(reset.achievements.isEmpty())
    }
}
