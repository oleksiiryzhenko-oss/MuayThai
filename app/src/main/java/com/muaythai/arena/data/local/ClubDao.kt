package com.muaythai.arena.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubDao {

    // ---- events
    @Query("SELECT * FROM club_events")
    fun events(): Flow<List<ClubEventEntity>>

    @Query("SELECT * FROM club_events")
    suspend fun eventsOnce(): List<ClubEventEntity>

    @Query("SELECT * FROM club_events WHERE id = :id")
    suspend fun eventById(id: Long): ClubEventEntity?

    @Upsert
    suspend fun upsertEvent(entity: ClubEventEntity): Long

    @Insert
    suspend fun insertEvents(entities: List<ClubEventEntity>)

    @Query("UPDATE club_events SET cancelled = :cancelled WHERE id = :id")
    suspend fun setEventCancelled(id: Long, cancelled: Boolean)

    @Query("UPDATE club_events SET deleted = 1 WHERE id = :id")
    suspend fun softDeleteEvent(id: Long)

    // ---- sign-ups
    @Query("SELECT * FROM event_signups")
    fun signups(): Flow<List<EventSignupEntity>>

    @Query("SELECT * FROM event_signups WHERE eventId = :eventId")
    suspend fun signupsForEvent(eventId: Long): List<EventSignupEntity>

    @Insert
    suspend fun insertSignup(entity: EventSignupEntity): Long

    @Query("UPDATE event_signups SET attended = :attended WHERE id = :id")
    suspend fun setAttended(id: Long, attended: Boolean)

    @Query("DELETE FROM event_signups WHERE id = :id")
    suspend fun deleteSignup(id: Long)

    @Query("DELETE FROM event_signups WHERE eventId = :eventId")
    suspend fun deleteSignupsForEvent(eventId: Long)

    // ---- surveys
    @Query("SELECT * FROM surveys")
    fun surveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun surveyById(id: Long): SurveyEntity?

    @Query("SELECT * FROM survey_questions")
    fun questions(): Flow<List<SurveyQuestionEntity>>

    @Query("SELECT * FROM survey_questions WHERE surveyId = :surveyId")
    suspend fun questionsForSurvey(surveyId: Long): List<SurveyQuestionEntity>

    @Insert
    suspend fun insertSurvey(entity: SurveyEntity): Long

    @Insert
    suspend fun insertQuestions(entities: List<SurveyQuestionEntity>)

    @Query("UPDATE surveys SET active = :active WHERE id = :id")
    suspend fun setSurveyActive(id: Long, active: Boolean)

    @Query("DELETE FROM surveys WHERE id = :id")
    suspend fun deleteSurvey(id: Long)

    @Query("DELETE FROM survey_questions WHERE surveyId = :surveyId")
    suspend fun deleteQuestions(surveyId: Long)

    // ---- responses
    @Query("SELECT * FROM survey_responses")
    fun responses(): Flow<List<SurveyResponseEntity>>

    @Query("SELECT * FROM survey_responses WHERE surveyId = :surveyId")
    suspend fun responsesForSurvey(surveyId: Long): List<SurveyResponseEntity>

    @Query("SELECT * FROM survey_answers")
    fun answers(): Flow<List<SurveyAnswerEntity>>

    @Insert
    suspend fun insertResponse(entity: SurveyResponseEntity): Long

    @Insert
    suspend fun insertAnswers(entities: List<SurveyAnswerEntity>)

    @Query("DELETE FROM survey_answers WHERE responseId IN (SELECT id FROM survey_responses WHERE surveyId = :surveyId)")
    suspend fun deleteAnswersForSurvey(surveyId: Long)

    @Query("DELETE FROM survey_responses WHERE surveyId = :surveyId")
    suspend fun deleteResponsesForSurvey(surveyId: Long)

    @Query("DELETE FROM survey_answers WHERE responseId = :responseId")
    suspend fun deleteAnswersForResponse(responseId: Long)

    @Query("DELETE FROM survey_responses WHERE id = :responseId")
    suspend fun deleteResponse(responseId: Long)

    // ---- announcements
    @Query("SELECT * FROM announcements")
    fun announcements(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE id = :id")
    suspend fun announcementById(id: Long): AnnouncementEntity?

    @Upsert
    suspend fun upsertAnnouncement(entity: AnnouncementEntity): Long

    @Insert
    suspend fun insertAnnouncements(entities: List<AnnouncementEntity>)

    @Query("UPDATE announcements SET seen = 1")
    suspend fun markAllAnnouncementsRead()

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: Long)

    // ---- requests
    @Query("SELECT * FROM club_requests")
    fun requests(): Flow<List<ClubRequestEntity>>

    @Insert
    suspend fun insertRequest(entity: ClubRequestEntity): Long

    @Query("UPDATE club_requests SET status = :status, reply = :reply, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRequest(id: Long, status: String, reply: String, updatedAt: Long)

    @Query("DELETE FROM club_requests WHERE id = :id")
    suspend fun deleteRequest(id: Long)
}
