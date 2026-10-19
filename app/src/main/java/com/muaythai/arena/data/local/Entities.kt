package com.muaythai.arena.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "fighter")
data class FighterEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val style: String,
    val xp: Int,
    val coins: Int,
    val rankPoints: Int,
    val badges: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val winStreak: Int,
    val bestWinStreak: Int,
    val campsCompleted: Int,
    val createdAt: Long
)

@Entity(tableName = "attributes")
data class AttributeEntity(
    @PrimaryKey val attribute: String,
    val xp: Int
)

@Entity(tableName = "training_sessions", indices = [Index("epochDay")])
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val epochDay: Long,
    val type: String,
    val title: String,
    val durationSec: Int,
    val xp: Int,
    val performance: Int,
    val reps: Int,
    val correct: Int,
    val total: Int,
    val tag: String,
    val success: Boolean,
    val valueMs: Int,
    val attributeGains: String,
    val note: String
)

@Entity(tableName = "combinations")
data class CombinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val strikes: String,
    val favorite: Boolean,
    val reps: Int,
    val custom: Boolean,
    val milestonesClaimed: Int,
    val createdAt: Long
)

@Entity(tableName = "technique_progress")
data class TechniqueProgressEntity(
    @PrimaryKey val techniqueId: String,
    val reps: Int
)

@Entity(tableName = "fight_results")
data class FightResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val opponentId: Int,
    val opponentName: String,
    val opponentTier: Int,
    val won: Boolean,
    val draw: Boolean,
    val method: String,
    val endRound: Int,
    val scorecards: String,
    val roundResults: String,
    val striking: Int,
    val defense: Int,
    val kicking: Int,
    val clinch: Int,
    val fightIq: Int,
    val stamina: Int,
    val tactical: Int,
    val overall: Int,
    val xp: Int,
    val mainStrategy: String,
    val campFight: Boolean
)

/** The player's record against each opponent on the roster. */
@Entity(tableName = "opponents")
data class OpponentEntity(
    @PrimaryKey val opponentId: Int,
    val name: String,
    val tier: Int,
    val wins: Int,
    val losses: Int,
    val lastFought: Long
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val challengeKey: String,
    val templateId: String,
    val weekly: Boolean,
    val periodKey: String,
    val completedAt: Long
)

@Entity(tableName = "fight_camp")
data class FightCampEntity(
    @PrimaryKey val id: Int = 1,
    val active: Boolean,
    val day: Int,
    val energy: Int,
    val freshness: Int,
    val opponentId: Int,
    val bonuses: String,
    val campLog: String,
    val boughtEnergyToday: Boolean,
    val trainedToday: String
)

@Entity(tableName = "personal_records")
data class PersonalRecordEntity(
    @PrimaryKey val recordKey: String,
    val value: Int,
    val detail: String,
    val achievedAt: Long
)
