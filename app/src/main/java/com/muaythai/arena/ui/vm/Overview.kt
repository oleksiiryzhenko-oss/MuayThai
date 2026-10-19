package com.muaythai.arena.ui.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muaythai.arena.AppContainer
import com.muaythai.arena.data.prefs.AppSettings
import com.muaythai.arena.data.prefs.SettingsStore
import com.muaythai.arena.data.repository.GameRepository
import com.muaythai.arena.domain.engine.ChallengeEngine
import com.muaythai.arena.domain.engine.CoachEngine
import com.muaythai.arena.domain.engine.LevelProgress
import com.muaythai.arena.domain.engine.OpponentEngine
import com.muaythai.arena.domain.engine.PlayerStats
import com.muaythai.arena.domain.engine.ProgressionEngine
import com.muaythai.arena.domain.engine.RankRequirement
import com.muaythai.arena.domain.engine.Readiness
import com.muaythai.arena.domain.engine.StatsEngine
import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.model.Archetype
import com.muaythai.arena.domain.model.ChallengeStatus
import com.muaythai.arena.domain.model.CoachTip
import com.muaythai.arena.domain.model.GameState
import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the dashboard-style screens need, derived from one [GameState] snapshot. */
data class Overview(
    val state: GameState = GameState(),
    val stats: PlayerStats = PlayerStats(),
    val level: LevelProgress = LevelProgress(1, 0, ProgressionEngine.xpForLevel(1)),
    val readiness: Readiness = Readiness(0, 0, 0, 0, 0, 0),
    val rankRequirement: RankRequirement = ProgressionEngine.nextRankRequirement(1, 0),
    val daily: List<ChallengeStatus> = emptyList(),
    val weekly: List<ChallengeStatus> = emptyList(),
    val missionComplete: Boolean = false,
    val tips: List<CoachTip> = emptyList(),
    val nextOpponent: Opponent = Opponents.all.first(),
    val affinity: Map<Archetype, Int> = emptyMap(),
    val today: Long = 0
) {
    val name: String get() = state.fighter?.name ?: ""
}

fun buildOverview(state: GameState, today: Long): Overview {
    val fighter = state.fighter ?: return Overview(state = state, today = today)
    val stats = StatsEngine.compute(state, today)
    val daily = ChallengeEngine.dailyStatuses(state.sessions, state.challenges, today)
    return Overview(
        state = state,
        stats = stats,
        level = ProgressionEngine.fighterProgress(fighter.xp),
        readiness = ProgressionEngine.readiness(stats.levels),
        rankRequirement = ProgressionEngine.nextRankRequirement(stats.level, fighter.rankPoints),
        daily = daily,
        weekly = ChallengeEngine.weeklyStatuses(state.sessions, state.challenges, today),
        missionComplete = daily.isNotEmpty() && daily.all { it.completed },
        tips = CoachEngine.tips(stats, state.camp),
        nextOpponent = OpponentEngine.nextOpponent(state.opponentRecords),
        affinity = ProgressionEngine.archetypeAffinity(stats.levels),
        today = today
    )
}

/** Read-only view model shared by Home, Train, Fight Lab, Progress and Profile. */
class DashboardViewModel(private val repository: GameRepository) : ViewModel() {
    val overview: StateFlow<Overview> = repository.state
        .map { buildOverview(it, repository.today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildOverview(repository.state.value, repository.today()))

    fun rename(name: String) { viewModelScope.launch { repository.renameFighter(name) } }
}

data class AppUi(val ready: Boolean = false, val onboarded: Boolean = false, val settings: AppSettings = AppSettings())

class AppViewModel(private val repository: GameRepository, private val settings: SettingsStore) : ViewModel() {
    val ui: StateFlow<AppUi> = combine(repository.state, settings.settings) { state, s ->
        AppUi(ready = state.loaded, onboarded = s.onboardingDone && state.fighter != null, settings = s)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUi())

    fun completeOnboarding(name: String, style: Archetype) {
        viewModelScope.launch {
            repository.createFighter(name, style)
            settings.setOnboardingDone(true)
        }
    }
}

class SettingsViewModel(
    private val repository: GameRepository,
    private val settings: SettingsStore,
    private val appContext: android.content.Context
) : ViewModel() {
    val ui: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setDarkMode(v: Boolean) { viewModelScope.launch { settings.setDarkMode(v) } }
    fun setSound(v: Boolean) { viewModelScope.launch { settings.setSound(v) } }
    fun setVibration(v: Boolean) { viewModelScope.launch { settings.setVibration(v) } }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminders(enabled)
            if (enabled) ReminderScheduler.schedule(appContext, ui.value.reminderHour) else ReminderScheduler.cancel(appContext)
        }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch {
            settings.setReminderHour(hour)
            if (ui.value.reminders) ReminderScheduler.schedule(appContext, hour)
        }
    }

    fun resetProgression() { viewModelScope.launch { repository.resetProgression() } }
    fun resetHistory() { viewModelScope.launch { repository.resetHistory() } }
}

class ArenaViewModelFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(c.repository, c.settings)
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(c.repository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(c.repository, c.settings, c.appContext)
            modelClass.isAssignableFrom(DrillViewModel::class.java) -> DrillViewModel(c.repository)
            modelClass.isAssignableFrom(RoundViewModel::class.java) -> RoundViewModel(c.repository, c.feedback)
            modelClass.isAssignableFrom(ProgramViewModel::class.java) -> ProgramViewModel(c.repository, c.feedback)
            modelClass.isAssignableFrom(CombinationViewModel::class.java) -> CombinationViewModel(c.repository)
            modelClass.isAssignableFrom(ComboPracticeViewModel::class.java) -> ComboPracticeViewModel(c.repository)
            modelClass.isAssignableFrom(DefenseViewModel::class.java) -> DefenseViewModel(c.repository)
            modelClass.isAssignableFrom(ClinchViewModel::class.java) -> ClinchViewModel(c.repository)
            modelClass.isAssignableFrom(ReactionViewModel::class.java) -> ReactionViewModel(c.repository)
            modelClass.isAssignableFrom(FightViewModel::class.java) -> FightViewModel(c.repository)
            modelClass.isAssignableFrom(CampViewModel::class.java) -> CampViewModel(c.repository)
            modelClass.isAssignableFrom(ClubViewModel::class.java) -> ClubViewModel(c.clubRepository, c.repository)
            modelClass.isAssignableFrom(ModeratorViewModel::class.java) -> ModeratorViewModel(c.clubRepository, c.settings, c.moderatorSession)
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
        return vm as T
    }
}

val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> { error("ViewModel factory not provided") }

/** A view model scoped to the current navigation entry, built by the app's factory. */
@Composable
inline fun <reified T : ViewModel> arenaViewModel(): T = viewModel(factory = LocalViewModelFactory.current)
