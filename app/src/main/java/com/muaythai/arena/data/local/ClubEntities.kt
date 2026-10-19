package com.muaythai.arena.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "club_events", indices = [Index("epochDay")])
data class ClubEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val epochDay: Long,
    val startMinute: Int,
    val durationMin: Int,
    val coach: String,
    val location: String,
    val level: String,
    val capacity: Int,
    val description: String,
    val cancelled: Boolean,
    val deleted: Boolean,
    val createdAt: Long
)

@Entity(tableName = "event_signups", indices = [Index("eventId")])
data class EventSignupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val name: String,
    val byUser: Boolean,
    val createdAt: Long,
    val attended: Boolean
)

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val active: Boolean,
    val rewardXp: Int,
    val createdAt: Long
)

@Entity(tableName = "survey_questions", indices = [Index("surveyId")])
data class SurveyQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyId: Long,
    val position: Int,
    val text: String,
    val type: String,
    /** One option per line. */
    val options: String,
    val required: Boolean
)

@Entity(tableName = "survey_responses", indices = [Index("surveyId")])
data class SurveyResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyId: Long,
    val respondent: String,
    val byUser: Boolean,
    val createdAt: Long
)

@Entity(tableName = "survey_answers", indices = [Index("responseId")])
data class SurveyAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val responseId: Long,
    val questionId: Long,
    val answer: String
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val pinned: Boolean,
    val seen: Boolean,
    val createdAt: Long
)

@Entity(tableName = "club_requests")
data class ClubRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val message: String,
    val author: String,
    val status: String,
    val reply: String,
    val createdAt: Long,
    val updatedAt: Long
)
