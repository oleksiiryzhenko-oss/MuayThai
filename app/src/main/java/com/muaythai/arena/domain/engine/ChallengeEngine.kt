package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.model.ChallengeRecord
import com.muaythai.arena.domain.model.ChallengeStatus
import com.muaythai.arena.domain.model.ChallengeTemplate
import com.muaythai.arena.domain.model.Metric
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.domain.model.StrikeType
import com.muaythai.arena.domain.model.TechniqueCategory
import com.muaythai.arena.domain.model.TrainingSession
import kotlin.random.Random

/** Daily missions and weekly objectives. Progress is always computed from the training log, never stored. */
object ChallengeEngine {
    const val DAILY_COUNT = 3
    const val WEEKLY_COUNT = 3
    const val SWEEP_BONUS_XP = 250
    const val SWEEP_ID = "sweep"

    private fun d(id: String, title: String, metric: Metric, target: Int, xp: Int, coins: Int = 20, rp: Int = 5) =
        ChallengeTemplate(id, false, title, metric, target, xp, coins, rp)

    private fun w(id: String, title: String, metric: Metric, target: Int, xp: Int, coins: Int = 120, rp: Int = 25) =
        ChallengeTemplate(id, true, title, metric, target, xp, coins, rp)

    val daily: List<ChallengeTemplate> = listOf(
        d("d_combo5", "Complete 5 combinations", Metric.COMBO_REPS, 5, 80),
        d("d_combo20", "Complete 20 combinations", Metric.COMBO_REPS, 20, 140, 35),
        d("d_roundwin", "Win 1 Fight Lab round", Metric.ROUND_WINS, 1, 120, 30, 8),
        d("d_def85", "Achieve 85% defense accuracy", Metric.DEFENSE_ACCURACY, 85, 130, 30),
        d("d_react20", "Complete 20 reaction scenarios", Metric.REACTION_PROMPTS, 20, 100),
        d("d_lowkick50", "Perform 50 low-kick repetitions", Metric.LOW_KICK_REPS, 50, 150, 35),
        d("d_kickwin", "Win a fight using a kicking strategy", Metric.KICK_STRATEGY_WIN, 1, 220, 60, 15),
        d("d_clinch", "Complete a clinch challenge", Metric.CLINCH_SESSIONS, 1, 90),
        d("d_beat", "Win a simulated fight", Metric.FIGHT_WINS, 1, 240, 60, 15),
        d("d_round", "Complete 1 full round on the round timer", Metric.ROUNDS, 1, 110, 25),
        d("d_rounds3", "Complete 3 rounds of round work", Metric.ROUNDS, 3, 200, 45, 8),
        d("d_tech30", "Drill 30 technique repetitions", Metric.TECHNIQUE_REPS, 30, 100),
        d("d_punch40", "Throw 40 punch repetitions", Metric.PUNCH_REPS, 40, 110),
        d("d_knee30", "Land 30 knee or elbow repetitions", Metric.KNEE_ELBOW_REPS, 30, 120, 25),
        d("d_sessions3", "Finish 3 training sessions", Metric.SESSIONS, 3, 120, 30),
        d("d_xp400", "Earn 400 XP today", Metric.XP, 400, 130, 30),
        d("d_iq2", "Complete 2 Fight IQ sessions", Metric.IQ_SESSIONS, 2, 130, 30),
        d("d_program", "Finish a training program", Metric.PROGRAMS, 1, 110, 25),
        d("d_react90", "Hit 90% reaction accuracy", Metric.REACTION_ACCURACY, 90, 150, 35, 8),
        d("d_clinchwin", "Win a clinch challenge", Metric.CLINCH_WINS, 1, 130, 30),
        d("d_counterwin", "Win a fight using a counter strategy", Metric.COUNTER_STRATEGY_WIN, 1, 220, 60, 15),
        d("d_cat3", "Train 3 different skill categories", Metric.CATEGORIES, 3, 150, 35, 8),
        d("d_min15", "Train for 15 minutes", Metric.MINUTES, 15, 140, 30)
    )

    val weekly: List<ChallengeTemplate> = listOf(
        w("w_wins3", "Win 3 simulated fights", Metric.FIGHT_WINS, 3, 700, 200, 40),
        w("w_combo100", "Complete 100 combinations", Metric.COMBO_REPS, 100, 500),
        w("w_def90", "Reach 90% defense accuracy", Metric.DEFENSE_ACCURACY, 90, 450),
        w("w_iq5", "Complete 5 Fight IQ sessions", Metric.IQ_SESSIONS, 5, 450),
        w("w_all", "Train every major skill category", Metric.CATEGORIES, 6, 600, 150, 30),
        w("w_sessions12", "Finish 12 training sessions", Metric.SESSIONS, 12, 500),
        w("w_days5", "Train on 5 different days", Metric.TRAINING_DAYS, 5, 650, 160, 35),
        w("w_rounds10", "Complete 10 rounds of round work", Metric.ROUNDS, 10, 550),
        w("w_lowkick200", "Perform 200 low-kick repetitions", Metric.LOW_KICK_REPS, 200, 500),
        w("w_react100", "Complete 100 reaction scenarios", Metric.REACTION_PROMPTS, 100, 450),
        w("w_clinch3", "Win 3 clinch challenges", Metric.CLINCH_WINS, 3, 450),
        w("w_fights5", "Fight 5 times", Metric.FIGHTS, 5, 600, 150, 30),
        w("w_roundwins10", "Win 10 Fight Lab rounds", Metric.ROUND_WINS, 10, 550, 140, 30),
        w("w_xp3000", "Earn 3,000 XP this week", Metric.XP, 3000, 500),
        w("w_tech300", "Drill 300 technique repetitions", Metric.TECHNIQUE_REPS, 300, 500),
        w("w_programs4", "Finish 4 training programs", Metric.PROGRAMS, 4, 450),
        w("w_camp", "Complete 6 Fight Camp training days", Metric.CAMP_DAYS, 6, 600, 150, 30),
        w("w_kickwins2", "Win 2 fights with a kicking strategy", Metric.KICK_STRATEGY_WIN, 2, 600, 150, 30),
        w("w_counterwins2", "Win 2 fights with a counter strategy", Metric.COUNTER_STRATEGY_WIN, 2, 600, 150, 30),
        w("w_punch300", "Throw 300 punch repetitions", Metric.PUNCH_REPS, 300, 450),
        w("w_min90", "Train for 90 minutes", Metric.MINUTES, 90, 550),
        w("w_knee150", "Land 150 knee or elbow repetitions", Metric.KNEE_ELBOW_REPS, 150, 450)
    )

    private val byId = (daily + weekly).associateBy { it.id }
    fun template(id: String): ChallengeTemplate? = byId[id]

    fun periodKey(weekly: Boolean, today: Long): String =
        if (weekly) "W${StatsEngine.weekIndex(today)}" else "D$today"

    fun recordKey(template: ChallengeTemplate, today: Long): String = "${periodKey(template.weekly, today)}:${template.id}"
    fun sweepKey(today: Long): String = "${periodKey(false, today)}:$SWEEP_ID"

    /** The same three missions for the whole day, a different set tomorrow. */
    fun activeDaily(today: Long): List<ChallengeTemplate> = daily.shuffled(Random(today * 7919 + 17)).take(DAILY_COUNT)

    fun activeWeekly(today: Long): List<ChallengeTemplate> =
        weekly.shuffled(Random(StatsEngine.weekIndex(today) * 104729 + 5)).take(WEEKLY_COUNT)

    private fun techniqueReps(sessions: List<TrainingSession>, predicate: (TechniqueCategory) -> Boolean): Int =
        sessions.filter { it.type == SessionType.TECHNIQUE }
            .sumOf { s -> if (Techniques.get(s.tag)?.let { predicate(it.category) } == true) s.reps else 0 }

    fun progress(metric: Metric, sessions: List<TrainingSession>): Int {
        fun of(type: SessionType) = sessions.filter { it.type == type }
        return when (metric) {
            Metric.COMBO_REPS -> of(SessionType.COMBINATION).sumOf { it.reps }
            Metric.COMBO_SESSIONS -> of(SessionType.COMBINATION).size
            Metric.REACTION_PROMPTS -> of(SessionType.REACTION).sumOf { it.total }
            Metric.REACTION_SESSIONS -> of(SessionType.REACTION).size
            Metric.REACTION_ACCURACY -> of(SessionType.REACTION).filter { it.total >= 10 }.maxOfOrNull { it.performance } ?: 0
            Metric.DEFENSE_ACCURACY -> of(SessionType.DEFENSE).filter { it.total >= 5 }.maxOfOrNull { it.performance } ?: 0
            Metric.DEFENSE_SESSIONS -> of(SessionType.DEFENSE).size
            Metric.ROUNDS -> of(SessionType.ROUND).sumOf { it.reps }
            Metric.FIGHT_WINS -> of(SessionType.FIGHT).count { it.success }
            Metric.FIGHTS -> of(SessionType.FIGHT).size
            Metric.ROUND_WINS -> of(SessionType.FIGHT).sumOf { it.correct }
            Metric.LOW_KICK_REPS -> techniqueReps(sessions) { it == TechniqueCategory.LOW_KICK }
            Metric.TECHNIQUE_REPS -> of(SessionType.TECHNIQUE).sumOf { it.reps }
            Metric.PUNCH_REPS -> techniqueReps(sessions) { it.type == StrikeType.PUNCH }
            Metric.KNEE_ELBOW_REPS -> techniqueReps(sessions) { it.type == StrikeType.KNEE || it.type == StrikeType.ELBOW }
            Metric.KICK_STRATEGY_WIN -> of(SessionType.FIGHT).count { it.success && (it.tag == Strategy.LONG_RANGE.name || it.tag == Strategy.LOW_KICK.name) }
            Metric.COUNTER_STRATEGY_WIN -> of(SessionType.FIGHT).count { it.success && it.tag == Strategy.COUNTER.name }
            Metric.CLINCH_WINS -> of(SessionType.CLINCH).count { it.success }
            Metric.CLINCH_SESSIONS -> of(SessionType.CLINCH).size
            Metric.SESSIONS -> sessions.size
            Metric.CATEGORIES -> sessions.map { it.type }.toSet().size
            Metric.XP -> sessions.sumOf { it.xp }
            Metric.IQ_SESSIONS -> sessions.count { it.type == SessionType.DEFENSE || it.type == SessionType.CLINCH || it.type == SessionType.FIGHT }
            Metric.PROGRAMS -> of(SessionType.PROGRAM).count { it.success }
            Metric.CAMP_DAYS -> of(SessionType.CAMP).size
            Metric.TRAINING_DAYS -> sessions.map { it.epochDay }.toSet().size
            Metric.MINUTES -> sessions.sumOf { it.durationSec } / 60
        }
    }

    fun sessionsInPeriod(weekly: Boolean, sessions: List<TrainingSession>, today: Long): List<TrainingSession> =
        if (weekly) {
            val start = StatsEngine.weekStart(today)
            sessions.filter { it.epochDay >= start && it.epochDay < start + 7 }
        } else sessions.filter { it.epochDay == today }

    fun statuses(templates: List<ChallengeTemplate>, sessions: List<TrainingSession>, records: List<ChallengeRecord>, today: Long): List<ChallengeStatus> {
        val done = records.map { it.key }.toSet()
        return templates.map { t ->
            val p = progress(t.metric, sessionsInPeriod(t.weekly, sessions, today))
            ChallengeStatus(t, p.coerceAtMost(t.target), recordKey(t, today) in done || p >= t.target)
        }
    }

    fun dailyStatuses(sessions: List<TrainingSession>, records: List<ChallengeRecord>, today: Long) =
        statuses(activeDaily(today), sessions, records, today)

    fun weeklyStatuses(sessions: List<TrainingSession>, records: List<ChallengeRecord>, today: Long) =
        statuses(activeWeekly(today), sessions, records, today)

    /** Challenges whose target is met but which have not been paid out yet. */
    fun newlyCompleted(sessions: List<TrainingSession>, records: List<ChallengeRecord>, today: Long): List<ChallengeTemplate> {
        val done = records.map { it.key }.toSet()
        return (activeDaily(today) + activeWeekly(today)).filter { t ->
            recordKey(t, today) !in done && progress(t.metric, sessionsInPeriod(t.weekly, sessions, today)) >= t.target
        }
    }

    /** True when all of today's missions are complete and the sweep bonus has not been granted. */
    fun sweepEarned(completedKeys: Set<String>, today: Long): Boolean =
        sweepKey(today) !in completedKeys && activeDaily(today).all { recordKey(it, today) in completedKeys }
}
