package com.muaythai.arena.data.repository

import androidx.room.withTransaction
import com.muaythai.arena.data.local.AchievementEntity
import com.muaythai.arena.data.local.ArenaDatabase
import com.muaythai.arena.data.local.AttributeEntity
import com.muaythai.arena.data.local.ChallengeEntity
import com.muaythai.arena.data.local.CombinationEntity
import com.muaythai.arena.data.local.FightCampEntity
import com.muaythai.arena.data.local.FightResultEntity
import com.muaythai.arena.data.local.FighterEntity
import com.muaythai.arena.data.local.OpponentEntity
import com.muaythai.arena.data.local.PersonalRecordEntity
import com.muaythai.arena.data.local.TechniqueProgressEntity
import com.muaythai.arena.data.local.TrainingSessionEntity
import com.muaythai.arena.data.local.toDomain
import com.muaythai.arena.data.local.toEntity
import com.muaythai.arena.domain.engine.CampStep
import com.muaythai.arena.domain.engine.CombinationEngine
import com.muaythai.arena.domain.engine.GameReducer
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.domain.model.Combination
import com.muaythai.arena.domain.model.FightCamp
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.GameState
import com.muaythai.arena.domain.model.RewardSummary
import com.muaythai.arena.domain.model.SessionOutcome
import com.muaythai.arena.domain.model.Strike
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Source of "now" and "today", so game logic never reads the system clock directly. */
interface GameClock {
    fun now(): Long
    fun today(): Long
}

object SystemGameClock : GameClock {
    override fun now(): Long = System.currentTimeMillis()
    override fun today(): Long = LocalDate.now().toEpochDay()
}

/**
 * Single source of truth for everything the player owns.
 *
 * Reads: [state] is one immutable [GameState] assembled from the Room tables.
 * Writes: each action loads a fresh snapshot inside a transaction, lets the pure [GameReducer] compute the next state,
 * and stores only what changed.
 */
class GameRepository(
    private val db: ArenaDatabase,
    scope: CoroutineScope,
    private val clock: GameClock = SystemGameClock
) {
    private val dao = db.gameDao()
    private val mutex = Mutex()

    private data class Core(
        val fighter: FighterEntity?,
        val attributes: List<AttributeEntity>,
        val sessions: List<TrainingSessionEntity>,
        val combinations: List<CombinationEntity>,
        val techniques: List<TechniqueProgressEntity>
    )

    private data class Extras(
        val fights: List<FightResultEntity>,
        val opponents: List<OpponentEntity>,
        val achievements: List<AchievementEntity>,
        val challenges: List<ChallengeEntity>,
        val records: List<PersonalRecordEntity>
    )

    private val core = combine(
        dao.fighter(), dao.attributes(), dao.sessions(), dao.combinations(), dao.techniqueProgress()
    ) { fighter, attributes, sessions, combinations, techniques -> Core(fighter, attributes, sessions, combinations, techniques) }

    private val extras = combine(
        dao.fights(), dao.opponents(), dao.achievements(), dao.challenges(), dao.records()
    ) { fights, opponents, achievements, challenges, records -> Extras(fights, opponents, achievements, challenges, records) }

    val state: StateFlow<GameState> = combine(core, extras, dao.camp()) { c, e, camp -> assemble(c, e, camp) }
        .stateIn(scope, SharingStarted.Eagerly, GameState())

    fun today(): Long = clock.today()
    fun now(): Long = clock.now()

    private fun assemble(c: Core, e: Extras, camp: FightCampEntity?): GameState = GameState(
        fighter = c.fighter?.toDomain(),
        attributeXp = c.attributes.mapNotNull { a -> Attribute.entries.firstOrNull { it.name == a.attribute }?.let { it to a.xp } }.toMap(),
        sessions = c.sessions.map { it.toDomain() },
        combinations = c.combinations.map { it.toDomain() },
        techniqueReps = c.techniques.associate { it.techniqueId to it.reps },
        fights = e.fights.map { it.toDomain() },
        opponentRecords = e.opponents.associate { it.opponentId to it.toDomain() },
        achievements = e.achievements.associate { it.achievementId to it.unlockedAt },
        challenges = e.challenges.map { it.toDomain() },
        camp = camp?.toDomain() ?: FightCamp(),
        records = e.records.mapNotNull { it.toDomain() }.associateBy { it.key },
        loaded = true
    )

    private suspend fun load(): GameState = assemble(
        Core(dao.fighterOnce(), dao.attributesOnce(), dao.sessionsOnce(), dao.combinationsOnce(), dao.techniqueProgressOnce()),
        Extras(dao.fightsOnce(), dao.opponentsOnce(), dao.achievementsOnce(), dao.challengesOnce(), dao.recordsOnce()),
        dao.campOnce()
    )

    /** Writes the difference between two snapshots. New sessions and fights are recognised by their id of 0. */
    private suspend fun persist(before: GameState, after: GameState) {
        after.fighter?.let { if (it != before.fighter) dao.upsertFighter(it.toEntity()) }
        if (after.attributeXp != before.attributeXp) {
            dao.upsertAttributes(after.attributeXp.map { AttributeEntity(it.key.name, it.value) })
        }
        for (s in after.sessions) if (s.id == 0L) dao.insertSession(s.toEntity())
        for (f in after.fights) if (f.id == 0L) dao.insertFight(f.toEntity())
        val oldCombos = before.combinations.associateBy { it.id }
        for (c in after.combinations) if (c.id == 0L || oldCombos[c.id] != c) dao.upsertCombination(c.toEntity())
        for ((id, reps) in after.techniqueReps) {
            if (before.techniqueReps[id] != reps) dao.upsertTechniqueProgress(TechniqueProgressEntity(id, reps))
        }
        for ((id, record) in after.opponentRecords) {
            if (before.opponentRecords[id] != record) dao.upsertOpponent(record.toEntity())
        }
        val newAchievements = after.achievements.filterKeys { it !in before.achievements }
        if (newAchievements.isNotEmpty()) dao.upsertAchievements(newAchievements.map { AchievementEntity(it.key, it.value) })
        val oldChallenges = before.challenges.map { it.key }.toSet()
        val newChallenges = after.challenges.filter { it.key !in oldChallenges }
        if (newChallenges.isNotEmpty()) dao.upsertChallenges(newChallenges.map { it.toEntity() })
        if (after.camp != before.camp) dao.upsertCamp(after.camp.toEntity())
        val changedRecords = after.records.values.filter { before.records[it.key] != it }
        if (changedRecords.isNotEmpty()) dao.upsertRecords(changedRecords.map { it.toEntity() })
    }

    private suspend fun <T> mutate(block: suspend (GameState) -> Pair<GameState, T>): T = mutex.withLock {
        db.withTransaction {
            val before = load()
            val (after, result) = block(before)
            persist(before, after)
            result
        }
    }

    // ------------------------------------------------------------------ fighter

    suspend fun createFighter(name: String, style: Archetype) = mutex.withLock {
        db.withTransaction {
            wipeProgress()
            dao.clearSessions()
            persist(GameState(), GameReducer.createFighter(name, style, clock.now()))
        }
    }

    suspend fun renameFighter(name: String) {
        val clean = name.trim().take(18)
        if (clean.isEmpty()) return
        mutate<Unit> { s -> s.copy(fighter = s.fighter?.copy(name = clean)) to Unit }
    }

    // ------------------------------------------------------------------ training and fights

    /** Applies a finished activity: XP, attributes, challenges, achievements, records, and the log entry. */
    suspend fun commit(outcome: SessionOutcome, fight: FightResult? = null): RewardSummary = mutate { s ->
        val t = GameReducer.applySession(s, outcome, clock.today(), clock.now(), fight)
        t.state to t.reward
    }

    // ------------------------------------------------------------------ combinations

    /** Creates a combination, or updates the name and strikes of an existing one. Returns an error message or null. */
    suspend fun saveCombination(id: Long?, name: String, strikes: List<Strike>): String? {
        CombinationEngine.validate(strikes)?.let { return it }
        val clean = name.trim().take(28).ifBlank { CombinationEngine.suggestName(strikes) }
        return mutate<String?> { s ->
            val existing = s.combinations.firstOrNull { it.id == id }
            val updated = if (existing != null) {
                // Changing the strikes makes it a different combination, so mastery starts again.
                val sameStrikes = existing.strikes == strikes
                s.combinations.map {
                    if (it.id != existing.id) it
                    else it.copy(name = clean, strikes = strikes, reps = if (sameStrikes) it.reps else 0,
                        milestonesClaimed = if (sameStrikes) it.milestonesClaimed else 0, custom = true)
                }
            } else {
                s.combinations + Combination(name = clean, strikes = strikes, custom = true, createdAt = clock.now())
            }
            s.copy(combinations = updated) to null
        }
    }

    suspend fun toggleFavorite(id: Long) = mutate<Unit> { s ->
        s.copy(combinations = s.combinations.map { if (it.id == id) it.copy(favorite = !it.favorite) else it }) to Unit
    }

    suspend fun deleteCombination(id: Long) = mutex.withLock { dao.deleteCombination(id) }

    // ------------------------------------------------------------------ fight camp

    suspend fun startCamp(opponentId: Int) = mutate<Unit> { s -> GameReducer.startCamp(s, opponentId) to Unit }

    suspend fun abandonCamp() = mutate<Unit> { s -> GameReducer.abandonCamp(s) to Unit }

    suspend fun campTrain(training: CampTraining): CampStep = mutate { s -> GameReducer.campTrain(s, training) }

    suspend fun campBuyEnergy(): CampStep = mutate { s -> GameReducer.campBuyEnergy(s) }

    suspend fun campEndDay(): RewardSummary = mutate { s ->
        val t = GameReducer.campEndDay(s, clock.today(), clock.now())
        t.state to t.reward
    }

    // ------------------------------------------------------------------ resets

    private suspend fun wipeProgress() {
        dao.clearAttributes()
        dao.clearCombinations()
        dao.clearTechniqueProgress()
        dao.clearFights()
        dao.clearOpponents()
        dao.clearAchievements()
        dao.clearChallenges()
        dao.clearCamp()
        dao.clearRecords()
    }

    /** Back to a level-1 fighter with the same name and style. The training log is kept. */
    suspend fun resetProgression() = mutex.withLock {
        db.withTransaction {
            val before = load()
            if (before.fighter == null) return@withTransaction
            wipeProgress()
            val fresh = GameReducer.resetProgression(before, clock.now())
            // Everything except the log was wiped, so store the fresh state against an empty baseline.
            persist(GameState(sessions = before.sessions), fresh)
        }
    }

    /** Clears the training log. Rank, attributes and fight record are kept. */
    suspend fun resetHistory() = mutex.withLock { dao.clearSessions() }
}
