package com.muaythai.arena

import android.app.Application
import android.content.Context
import com.muaythai.arena.data.local.ArenaDatabase
import com.muaythai.arena.data.prefs.SettingsStore
import com.muaythai.arena.data.repository.ClubRepository
import com.muaythai.arena.data.repository.GameRepository
import com.muaythai.arena.data.repository.ModeratorSession
import com.muaythai.arena.reminder.ReminderScheduler
import com.muaythai.arena.ui.Feedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-rolled dependency container: the app is small enough that a DI framework would only add build time. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database: ArenaDatabase = ArenaDatabase.create(context)
    val settings: SettingsStore = SettingsStore(context.applicationContext)
    val repository: GameRepository = GameRepository(database, scope)
    val clubRepository: ClubRepository = ClubRepository(database, settings, scope)
    val moderatorSession: ModeratorSession = ModeratorSession()
    val feedback: Feedback = Feedback(context.applicationContext)

    init {
        // Demo club content on first launch, and the rolling two-week timetable on every launch.
        scope.launch { clubRepository.ensureContent() }
        scope.launch {
            settings.settings.collect {
                feedback.soundEnabled = it.sound
                feedback.vibrationEnabled = it.vibration
            }
        }
    }
}

class MuayThaiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.muaythai.arena.launch.AppsFlyerHandoff.initialize(this)
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
    }
}
