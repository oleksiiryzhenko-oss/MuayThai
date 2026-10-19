package com.muaythai.arena.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muaythai.arena.data.repository.GameRepository
import com.muaythai.arena.domain.content.Opponents
import com.muaythai.arena.domain.engine.FightCampEngine
import com.muaythai.arena.domain.engine.FightSimulationEngine
import com.muaythai.arena.domain.engine.OpponentEngine
import com.muaythai.arena.domain.engine.ProgressionEngine
import com.muaythai.arena.domain.model.CampTraining
import com.muaythai.arena.domain.model.CornerInstruction
import com.muaythai.arena.domain.model.CornerReport
import com.muaythai.arena.domain.model.FightCamp
import com.muaythai.arena.domain.model.FightPhase
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.FightState
import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.domain.model.RewardSummary
import com.muaythai.arena.domain.model.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FightUi(
    val fight: FightState? = null,
    val report: CornerReport? = null,
    val result: FightResult? = null,
    val reward: RewardSummary? = null,
    val saving: Boolean = false,
    val missing: Boolean = false
)

class FightViewModel(private val repository: GameRepository) : ViewModel() {
    private val _ui = MutableStateFlow(FightUi())
    val ui: StateFlow<FightUi> = _ui.asStateFlow()
    private var startedAt = System.currentTimeMillis()

    fun start(opponentId: Int, campFight: Boolean) {
        if (_ui.value.fight != null || _ui.value.missing) return
        val state = repository.state.value
        val fighter = state.fighter
        val opponent = Opponents.get(opponentId)
        if (fighter == null || opponent == null) { _ui.value = FightUi(missing = true); return }
        val levels = ProgressionEngine.levels(state.attributeXp)
        val camp = state.camp
        val useCamp = campFight && camp.active
        val user = OpponentEngine.userCombatStats(
            levels, ProgressionEngine.archetype(levels, fighter.style), if (useCamp) camp.bonuses else emptyMap()
        )
        startedAt = System.currentTimeMillis()
        _ui.value = FightUi(
            fight = FightSimulationEngine.start(
                opponent, user, seed = startedAt, startStamina = if (useCamp) FightCampEngine.startStamina(camp) else 100, campFight = useCamp
            )
        )
    }

    fun chooseStrategy(strategy: Strategy) = _ui.update { ui ->
        ui.fight?.let { ui.copy(fight = FightSimulationEngine.chooseStrategy(it, strategy)) } ?: ui
    }

    fun chooseOption(index: Int) = _ui.update { ui ->
        ui.fight?.let { ui.copy(fight = FightSimulationEngine.resolveEvent(it, index)) } ?: ui
    }

    fun advance() {
        val fight = _ui.value.fight ?: return
        if (fight.phase != FightPhase.EVENT_RESULT) return
        val next = FightSimulationEngine.advance(fight)
        _ui.update { it.copy(fight = next, report = if (next.phase == FightPhase.CORNER) FightSimulationEngine.cornerReport(next) else null) }
        if (next.phase == FightPhase.FINISHED) finish(next)
    }

    fun corner(instruction: CornerInstruction) = _ui.update { ui ->
        ui.fight?.let { ui.copy(fight = FightSimulationEngine.applyCorner(it, instruction), report = null) } ?: ui
    }

    private fun finish(fight: FightState) {
        if (_ui.value.saving || _ui.value.result != null) return
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = FightSimulationEngine.result(fight, repository.now())
            val seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(60)
            val reward = repository.commit(FightSimulationEngine.sessionOutcome(result, seconds), result)
            _ui.update { it.copy(saving = false, result = result, reward = reward) }
        }
    }
}

data class CampUi(
    val camp: FightCamp = FightCamp(),
    val opponent: Opponent? = null,
    val coins: Int = 0,
    val suggested: List<CampTraining> = emptyList(),
    val nextOpponent: Opponent = Opponents.all.first()
)

class CampViewModel(private val repository: GameRepository) : ViewModel() {
    val ui: StateFlow<CampUi> = repository.state.map { s ->
        val opponent = if (s.camp.active) Opponents.get(s.camp.opponentId) else null
        CampUi(
            camp = s.camp, opponent = opponent, coins = s.fighter?.coins ?: 0,
            suggested = opponent?.let { FightCampEngine.suggestedTraining(it) } ?: emptyList(),
            nextOpponent = OpponentEngine.nextOpponent(s.opponentRecords)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CampUi())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _reward = MutableStateFlow<RewardSummary?>(null)
    val reward: StateFlow<RewardSummary?> = _reward.asStateFlow()

    fun start(opponentId: Int) { viewModelScope.launch { repository.startCamp(opponentId); _message.value = null } }
    fun abandon() { viewModelScope.launch { repository.abandonCamp(); _message.value = null } }
    fun train(training: CampTraining) { viewModelScope.launch { _message.value = repository.campTrain(training).message } }
    fun buyEnergy() { viewModelScope.launch { _message.value = repository.campBuyEnergy().message } }
    fun endDay() { viewModelScope.launch { _reward.value = repository.campEndDay(); _message.value = null } }
    fun dismissReward() { _reward.value = null }
}
