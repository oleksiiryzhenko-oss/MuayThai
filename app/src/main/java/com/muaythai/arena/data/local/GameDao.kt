package com.muaythai.arena.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // ---- fighter
    @Query("SELECT * FROM fighter WHERE id = 1")
    fun fighter(): Flow<FighterEntity?>

    @Query("SELECT * FROM fighter WHERE id = 1")
    suspend fun fighterOnce(): FighterEntity?

    @Upsert
    suspend fun upsertFighter(entity: FighterEntity)

    // ---- attributes
    @Query("SELECT * FROM attributes")
    fun attributes(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM attributes")
    suspend fun attributesOnce(): List<AttributeEntity>

    @Upsert
    suspend fun upsertAttributes(entities: List<AttributeEntity>)

    @Query("DELETE FROM attributes")
    suspend fun clearAttributes()

    // ---- training sessions
    @Query("SELECT * FROM training_sessions ORDER BY timestamp ASC, id ASC")
    fun sessions(): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions ORDER BY timestamp ASC, id ASC")
    suspend fun sessionsOnce(): List<TrainingSessionEntity>

    @Insert
    suspend fun insertSession(entity: TrainingSessionEntity): Long

    @Query("DELETE FROM training_sessions")
    suspend fun clearSessions()

    // ---- combinations
    @Query("SELECT * FROM combinations ORDER BY createdAt ASC, id ASC")
    fun combinations(): Flow<List<CombinationEntity>>

    @Query("SELECT * FROM combinations ORDER BY createdAt ASC, id ASC")
    suspend fun combinationsOnce(): List<CombinationEntity>

    @Upsert
    suspend fun upsertCombination(entity: CombinationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombinations(entities: List<CombinationEntity>)

    @Query("DELETE FROM combinations WHERE id = :id")
    suspend fun deleteCombination(id: Long)

    @Query("DELETE FROM combinations")
    suspend fun clearCombinations()

    // ---- technique progress
    @Query("SELECT * FROM technique_progress")
    fun techniqueProgress(): Flow<List<TechniqueProgressEntity>>

    @Query("SELECT * FROM technique_progress")
    suspend fun techniqueProgressOnce(): List<TechniqueProgressEntity>

    @Upsert
    suspend fun upsertTechniqueProgress(entity: TechniqueProgressEntity)

    @Query("DELETE FROM technique_progress")
    suspend fun clearTechniqueProgress()

    // ---- fight results
    @Query("SELECT * FROM fight_results ORDER BY timestamp ASC, id ASC")
    fun fights(): Flow<List<FightResultEntity>>

    @Query("SELECT * FROM fight_results ORDER BY timestamp ASC, id ASC")
    suspend fun fightsOnce(): List<FightResultEntity>

    @Insert
    suspend fun insertFight(entity: FightResultEntity): Long

    @Query("DELETE FROM fight_results")
    suspend fun clearFights()

    // ---- opponents
    @Query("SELECT * FROM opponents")
    fun opponents(): Flow<List<OpponentEntity>>

    @Query("SELECT * FROM opponents")
    suspend fun opponentsOnce(): List<OpponentEntity>

    @Upsert
    suspend fun upsertOpponent(entity: OpponentEntity)

    @Query("DELETE FROM opponents")
    suspend fun clearOpponents()

    // ---- achievements
    @Query("SELECT * FROM achievements")
    fun achievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun achievementsOnce(): List<AchievementEntity>

    @Upsert
    suspend fun upsertAchievements(entities: List<AchievementEntity>)

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()

    // ---- challenges
    @Query("SELECT * FROM challenges")
    fun challenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun challengesOnce(): List<ChallengeEntity>

    @Upsert
    suspend fun upsertChallenges(entities: List<ChallengeEntity>)

    @Query("DELETE FROM challenges")
    suspend fun clearChallenges()

    // ---- fight camp
    @Query("SELECT * FROM fight_camp WHERE id = 1")
    fun camp(): Flow<FightCampEntity?>

    @Query("SELECT * FROM fight_camp WHERE id = 1")
    suspend fun campOnce(): FightCampEntity?

    @Upsert
    suspend fun upsertCamp(entity: FightCampEntity)

    @Query("DELETE FROM fight_camp")
    suspend fun clearCamp()

    // ---- personal records
    @Query("SELECT * FROM personal_records")
    fun records(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records")
    suspend fun recordsOnce(): List<PersonalRecordEntity>

    @Upsert
    suspend fun upsertRecords(entities: List<PersonalRecordEntity>)

    @Query("DELETE FROM personal_records")
    suspend fun clearRecords()
}
