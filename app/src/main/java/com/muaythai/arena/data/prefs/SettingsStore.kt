package com.muaythai.arena.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val darkMode: Boolean = false,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val reminders: Boolean = false,
    val reminderHour: Int = 18,
    val onboardingDone: Boolean = false,
    val clubSeeded: Boolean = false,
    /** Keep the regular weekly classes generated two weeks ahead. */
    val autoTimetable: Boolean = true,
    /** SHA-256 of the moderator PIN, or empty when no PIN has been set. */
    val moderatorPinHash: String = ""
)

/** Lightweight preferences. Game progress lives in Room, not here. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val REMINDERS = booleanPreferencesKey("reminders")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CLUB_SEEDED = booleanPreferencesKey("club_seeded")
        val AUTO_TIMETABLE = booleanPreferencesKey("auto_timetable")
        val MODERATOR_PIN = stringPreferencesKey("moderator_pin_hash")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppSettings(
                darkMode = p[Keys.DARK_MODE] ?: false,
                sound = p[Keys.SOUND] ?: true,
                vibration = p[Keys.VIBRATION] ?: true,
                reminders = p[Keys.REMINDERS] ?: false,
                reminderHour = p[Keys.REMINDER_HOUR] ?: 18,
                onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
                clubSeeded = p[Keys.CLUB_SEEDED] ?: false,
                autoTimetable = p[Keys.AUTO_TIMETABLE] ?: true,
                moderatorPinHash = p[Keys.MODERATOR_PIN] ?: ""
            )
        }

    suspend fun setDarkMode(value: Boolean) { context.settingsDataStore.edit { it[Keys.DARK_MODE] = value } }
    suspend fun setSound(value: Boolean) { context.settingsDataStore.edit { it[Keys.SOUND] = value } }
    suspend fun setVibration(value: Boolean) { context.settingsDataStore.edit { it[Keys.VIBRATION] = value } }
    suspend fun setReminders(value: Boolean) { context.settingsDataStore.edit { it[Keys.REMINDERS] = value } }
    suspend fun setReminderHour(value: Int) { context.settingsDataStore.edit { it[Keys.REMINDER_HOUR] = value.coerceIn(0, 23) } }
    suspend fun setOnboardingDone(value: Boolean) { context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = value } }
    suspend fun setClubSeeded(value: Boolean) { context.settingsDataStore.edit { it[Keys.CLUB_SEEDED] = value } }
    suspend fun setAutoTimetable(value: Boolean) { context.settingsDataStore.edit { it[Keys.AUTO_TIMETABLE] = value } }
    suspend fun setModeratorPinHash(value: String) { context.settingsDataStore.edit { it[Keys.MODERATOR_PIN] = value } }
}
