package com.muaythai.arena.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FighterEntity::class,
        AttributeEntity::class,
        TrainingSessionEntity::class,
        CombinationEntity::class,
        TechniqueProgressEntity::class,
        FightResultEntity::class,
        OpponentEntity::class,
        AchievementEntity::class,
        ChallengeEntity::class,
        FightCampEntity::class,
        PersonalRecordEntity::class,
        ClubEventEntity::class,
        EventSignupEntity::class,
        SurveyEntity::class,
        SurveyQuestionEntity::class,
        SurveyResponseEntity::class,
        SurveyAnswerEntity::class,
        AnnouncementEntity::class,
        ClubRequestEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ArenaDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun clubDao(): ClubDao

    companion object {
        fun create(context: Context): ArenaDatabase =
            Room.databaseBuilder(context.applicationContext, ArenaDatabase::class.java, "muaythai_arena.db")
                // Version 1 was never published. A development install of it is rebuilt instead of crashing on open.
                // Replace this with real Migration objects before changing the schema of a released version.
                .fallbackToDestructiveMigration()
                .build()
    }
}
