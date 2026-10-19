package com.muaythai.arena.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muaythai.arena.data.repository.GameRepository
import com.muaythai.arena.domain.content.Programs
import com.muaythai.arena.domain.content.Techniques
import com.muaythai.arena.domain.engine.ClinchEngine
import com.muaythai.arena.domain.engine.CombinationEngine
import com.muaythai.arena.domain.engine.DefenseEngine
import com.muaythai.arena.domain.engine.MuayThaiTrainingEngine
import com.muaythai.arena.domain.engine.ReactionEngine
import com.muaythai.arena.domain.engine.ReactionResult
import com.muaythai.arena.domain.engine.ReactionSummary
import com.muaythai.arena.domain.model.ClinchScenario
import com.muaythai.arena.domain.model.Combination
import com.muaythai.arena.domain.model.DefenseResponse
import com.muaythai.arena.domain.model.DefenseScenario
import com.muaythai.arena.domain.model.ReactionPrompt
import com.muaythai.arena.domain.model.RewardSummary
import com.muaythai.arena.domain.model.Strike
import com.muaythai.arena.domain.model.Technique
import com.muaythai.arena.domain.model.TrainingProgram
import com.muaythai.arena.ui.Feedback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

private fun elapsedSeconds(startedAtMs: Long): Int = ((System.currentTimeMillis() - startedAtMs) / 1000).toInt().coerceAtLeast(1)

// =====================================================================================
// Technique drill
// =====================================================================================

data class DrillUi(
    val technique: Technique? = null,
    val reps: Int = 0,
    val previousReps: Int = 0,
    val saving: Boolean = false,
    val reward: RewardSummary? = null
) {
    val mastery: Int get() = technique?.let { MuayThaiTrainingEngine.mastery(it, previousReps + reps) } ?: 0
    val setProgress: Float get() = (reps % MuayThaiTrainingEngine.SET_SIZE) / MuayThaiTrainingEngine.SET_SIZE.toFloat()
    val setsDone: Int get() = reps / MuayThaiTrainingEngine.SET_SIZE
}

class DrillViewModel(private val repository: GameRepository) : ViewModel() {
    private val _ui = MutableStateFlow(DrillUi())
    val ui: StateFlow<DrillUi> = _ui.asStateFlow()
    private var startedAt = System.currentTimeMillis()

    fun load(id: String) {
        if (_ui.value.technique?.id == id) return
        startedAt = System.currentTimeMillis()
        _ui.value = DrillUi(technique = Techniques.get(id), previousReps = repository.state.value.techniqueReps[id] ?: 0)
    }

    fun rep() = _ui.update { if (it.saving) it else it.copy(reps = it.reps + 1) }
    fun addSet() = _ui.update { if (it.saving) it else it.copy(reps = it.reps + MuayThaiTrainingEngine.SET_SIZE) }

    fun finish() {
        val current = _ui.value
        val technique = current.technique ?: return
        if (current.reps <= 0 || current.saving) return
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val reward = repository.commit(MuayThaiTrainingEngine.drillOutcome(technique, current.reps, elapsedSeconds(startedAt)))
            _ui.update { it.copy(reward = reward, saving = false) }
        }
    }
}

// =====================================================================================
// Round timer
// =====================================================================================

data class RoundUi(
    val lengthSec: Int = 180,
    val remaining: Int = 180,
    val running: Boolean = false,
    val started: Boolean = false,
    val callout: String = "",
    val saving: Boolean = false,
    val reward: RewardSummary? = null
) {
    val elapsed: Int get() = lengthSec - remaining
    val fraction: Float get() = if (lengthSec == 0) 0f else elapsed.toFloat() / lengthSec
}

class RoundViewModel(private val repository: GameRepository, private val feedback: Feedback) : ViewModel() {
    private val _ui = MutableStateFlow(RoundUi())
    val ui: StateFlow<RoundUi> = _ui.asStateFlow()
    private var ticker: Job? = null
    private var callouts: List<String> = emptyList()

    fun selectLength(seconds: Int) {
        if (_ui.value.started) return
        _ui.value = RoundUi(lengthSec = seconds, remaining = seconds)
    }

    fun start() {
        val current = _ui.value
        if (current.running || current.reward != null) return
        if (!current.started) {
            callouts = MuayThaiTrainingEngine.roundCallouts(current.lengthSec, repository.state.value.combinations.filter { it.favorite }
                .ifEmpty { repository.state.value.combinations }, Random(System.currentTimeMillis()))
            feedback.bell()
        }
        _ui.update { it.copy(running = true, started = true, callout = calloutFor(it.elapsed)) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (_ui.value.running && _ui.value.remaining > 0) {
                delay(1000)
                _ui.update { s ->
                    if (!s.running) s else {
                        val remaining = (s.remaining - 1).coerceAtLeast(0)
                        s.copy(remaining = remaining, callout = calloutFor(s.lengthSec - remaining))
                    }
                }
                val now = _ui.value
                if (now.remaining == 10) feedback.strike()
                if (now.remaining == 0) { feedback.bell(); finish() }
            }
        }
    }

    private fun calloutFor(elapsed: Int): String = callouts.getOrNull(elapsed / 8) ?: callouts.lastOrNull().orEmpty()

    fun pause() { _ui.update { it.copy(running = false) } }

    /** Ends the round early. At least thirty seconds of work is needed for it to count. */
    fun finish() {
        val current = _ui.value
        if (current.saving || current.reward != null) return
        ticker?.cancel()
        _ui.update { it.copy(running = false, saving = true) }
        viewModelScope.launch {
            val reward = if (current.elapsed >= 30) {
                repository.commit(MuayThaiTrainingEngine.roundOutcome(current.lengthSec, current.elapsed))
            } else RewardSummary("Round abandoned", 0, lines = listOf("Work for at least 30 seconds for the round to count."))
            _ui.update { it.copy(saving = false, reward = reward) }
        }
    }

    fun reset() {
        ticker?.cancel()
        val length = _ui.value.lengthSec
        _ui.value = RoundUi(lengthSec = length, remaining = length)
    }
}

// =====================================================================================
// Training programs
// =====================================================================================

data class ProgramUi(
    val program: TrainingProgram? = null,
    val stepIndex: Int = 0,
    val remaining: Int = 0,
    val running: Boolean = false,
    val completedSteps: Int = 0,
    val saving: Boolean = false,
    val reward: RewardSummary? = null
) {
    val step get() = program?.steps?.getOrNull(stepIndex)
    val isLastStep: Boolean get() = program != null && stepIndex >= program.steps.size - 1
}

class ProgramViewModel(private val repository: GameRepository, private val feedback: Feedback) : ViewModel() {
    private val _ui = MutableStateFlow(ProgramUi())
    val ui: StateFlow<ProgramUi> = _ui.asStateFlow()
    private var ticker: Job? = null
    private var startedAt = System.currentTimeMillis()

    fun load(id: String) {
        if (_ui.value.program?.id == id) return
        val program = Programs.get(id)
        startedAt = System.currentTimeMillis()
        _ui.value = ProgramUi(program = program, remaining = program?.steps?.firstOrNull()?.seconds ?: 0)
    }

    fun toggle() {
        if (_ui.value.running) { _ui.update { it.copy(running = false) }; ticker?.cancel(); return }
        if (_ui.value.reward != null || _ui.value.step == null) return
        _ui.update { it.copy(running = true) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (_ui.value.running && _ui.value.remaining > 0) {
                delay(1000)
                _ui.update { if (it.running) it.copy(remaining = (it.remaining - 1).coerceAtLeast(0)) else it }
                if (_ui.value.remaining == 0) { feedback.bell(); _ui.update { it.copy(running = false) } }
            }
        }
    }

    /** Marks the current step as done (whether or not the timer ran out) and moves on. */
    fun completeStep() {
        val current = _ui.value
        val program = current.program ?: return
        if (current.reward != null || current.saving) return
        ticker?.cancel()
        val done = current.completedSteps + 1
        if (current.isLastStep) {
            finish(done)
        } else {
            val next = current.stepIndex + 1
            _ui.update { it.copy(stepIndex = next, remaining = program.steps[next].seconds, running = false, completedSteps = done) }
        }
    }

    fun finishEarly() = finish(_ui.value.completedSteps)

    private fun finish(completed: Int) {
        val program = _ui.value.program ?: return
        if (_ui.value.saving || _ui.value.reward != null) return
        ticker?.cancel()
        _ui.update { it.copy(running = false, saving = true, completedSteps = completed) }
        viewModelScope.launch {
            val reward = if (completed > 0) {
                repository.commit(MuayThaiTrainingEngine.programOutcome(program, completed, elapsedSeconds(startedAt)))
            } else RewardSummary(program.name, 0, lines = listOf("Complete at least one step for the session to count."))
            _ui.update { it.copy(saving = false, reward = reward) }
        }
    }
}

// =====================================================================================
// Combination Lab
// =====================================================================================

data class BuilderUi(
    val id: Long = 0,
    val name: String = "",
    val strikes: List<Strike> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
    val loaded: Boolean = false
) {
    val difficulty: Int get() = CombinationEngine.difficulty(strikes)
    val xpValue: Int get() = CombinationEngine.xpValue(strikes)
}

class CombinationViewModel(private val repository: GameRepository) : ViewModel() {
    val combinations: StateFlow<List<Combination>> = repository.state
        .map { s -> s.combinations.sortedWith(compareByDescending<Combination> { it.favorite }.thenByDescending { it.custom }.thenBy { it.name }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _builder = MutableStateFlow(BuilderUi())
    val builder: StateFlow<BuilderUi> = _builder.asStateFlow()

    fun startEdit(id: Long) {
        if (_builder.value.loaded && _builder.value.id == id) return
        val existing = repository.state.value.combinations.firstOrNull { it.id == id }
        _builder.value = if (existing != null) BuilderUi(existing.id, existing.name, existing.strikes, loaded = true) else BuilderUi(loaded = true)
    }

    fun setName(name: String) = _builder.update { it.copy(name = name.take(28), error = null) }

    fun add(strike: Strike) = _builder.update {
        if (it.strikes.size >= CombinationEngine.MAX_STRIKES) it.copy(error = "A combination can hold at most ${CombinationEngine.MAX_STRIKES} strikes.")
        else it.copy(strikes = it.strikes + strike, error = null)
    }

    fun removeAt(index: Int) = _builder.update {
        if (index !in it.strikes.indices) it else it.copy(strikes = it.strikes.filterIndexed { i, _ -> i != index }, error = null)
    }

    fun move(index: Int, delta: Int) = _builder.update {
        val target = index + delta
        if (index !in it.strikes.indices || target !in it.strikes.indices) it else {
            val list = it.strikes.toMutableList()
            val item = list.removeAt(index)
            list.add(target, item)
            it.copy(strikes = list)
        }
    }

    fun clear() = _builder.update { it.copy(strikes = emptyList(), error = null) }

    fun save() {
        val b = _builder.value
        viewModelScope.launch {
            val error = repository.saveCombination(if (b.id == 0L) null else b.id, b.name, b.strikes)
            _builder.update { it.copy(error = error, saved = error == null) }
        }
    }

    fun toggleFavorite(id: Long) { viewModelScope.launch { repository.toggleFavorite(id) } }
    fun delete(id: Long) { viewModelScope.launch { repository.deleteCombination(id) } }
    fun rename(combination: Combination, name: String) {
        viewModelScope.launch { repository.saveCombination(combination.id, name, combination.strikes) }
    }
}

data class PracticeUi(
    val combination: Combination? = null,
    val strikeIndex: Int = 0,
    val reps: Int = 0,
    val saving: Boolean = false,
    val reward: RewardSummary? = null
) {
    val totalReps: Int get() = (combination?.reps ?: 0) + reps
    val mastery: Int get() = CombinationEngine.mastery(totalReps)
}

class ComboPracticeViewModel(private val repository: GameRepository) : ViewModel() {
    private val _ui = MutableStateFlow(PracticeUi())
    val ui: StateFlow<PracticeUi> = _ui.asStateFlow()
    private var startedAt = System.currentTimeMillis()

    fun load(id: Long) {
        if (_ui.value.combination?.id == id) return
        startedAt = System.currentTimeMillis()
        _ui.value = PracticeUi(combination = repository.state.value.combinations.firstOrNull { it.id == id })
    }

    /** One tap throws the next strike in the sequence. Returns true when the tap completed a repetition. */
    fun hit(): Boolean {
        val current = _ui.value
        val combo = current.combination ?: return false
        if (current.saving || current.reward != null || combo.strikes.isEmpty()) return false
        val next = current.strikeIndex + 1
        val completed = next >= combo.strikes.size
        _ui.value = if (completed) current.copy(strikeIndex = 0, reps = current.reps + 1) else current.copy(strikeIndex = next)
        return completed
    }

    fun addReps(count: Int) = _ui.update { if (it.saving || it.reward != null) it else it.copy(reps = it.reps + count, strikeIndex = 0) }

    fun finish() {
        val current = _ui.value
        val combo = current.combination ?: return
        if (current.reps <= 0 || current.saving) return
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val reward = repository.commit(CombinationEngine.practiceOutcome(combo, current.reps, elapsedSeconds(startedAt)))
            _ui.update { it.copy(saving = false, reward = reward) }
        }
    }
}

// =====================================================================================
// Defense Lab
// =====================================================================================

data class DefenseUi(
    val scenarios: List<DefenseScenario> = emptyList(),
    val index: Int = 0,
    val chosen: Int? = null,
    val correct: Int = 0,
    val answered: Int = 0,
    val saving: Boolean = false,
    val reward: RewardSummary? = null
) {
    val current: DefenseScenario? get() = scenarios.getOrNull(index)
    val started: Boolean get() = scenarios.isNotEmpty()
    val isLast: Boolean get() = index >= scenarios.size - 1
    val accuracy: Int get() = if (answered == 0) 0 else correct * 100 / answered
}

class DefenseViewModel(private val repository: GameRepository) : ViewModel() {
    private val _ui = MutableStateFlow(DefenseUi())
    val ui: StateFlow<DefenseUi> = _ui.asStateFlow()
    private var startedAt = System.currentTimeMillis()

    fun start() {
        startedAt = System.currentTimeMillis()
        _ui.value = DefenseUi(scenarios = DefenseEngine.buildSession(Random(startedAt)))
    }

    /** Returns true when the answer was correct, so the screen can play the right feedback. */
    fun choose(option: Int): Boolean {
        val current = _ui.value
        val scenario = current.current ?: return false
        if (current.chosen != null) return false
        val right = DefenseEngine.isCorrect(scenario, option)
        _ui.value = current.copy(chosen = option, correct = current.correct + if (right) 1 else 0, answered = current.answered + 1)
        return right
    }

    fun next() {
        val current = _ui.value
        if (current.chosen == null || current.saving) return
        if (!current.isLast) { _ui.value = current.copy(index = current.index + 1, chosen = null); return }
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val reward = repository.commit(DefenseEngine.outcome(current.correct, current.answered, elapsedSeconds(startedAt)))
            _ui.update { it.copy(saving = false, reward = reward) }
        }
    }
}

// =====================================================================================
// Clinch Lab
// =====================================================================================

data class ClinchUi(
    val scenarios: List<ClinchScenario> = emptyList(),
    val index: Int = 0,
    val chosen: Int? = null,
    val qualities: List<Int> = emptyList(),
    val saving: Boolean = false,
    val reward: RewardSummary? = null
) {
    val current: ClinchScenario? get() = scenarios.getOrNull(index)
    val started: Boolean get() = scenarios.isNotEmpty()
    val isLast: Boolean get() = index >= scenarios.size - 1
    val score: Int get() = ClinchEngine.score(qualities)
}

class ClinchViewModel(private val repository: GameRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ClinchUi())
    val ui: StateFlow<ClinchUi> = _ui.asStateFlow()
    private var startedAt = System.currentTimeMillis()

    fun start() {
        startedAt = System.currentTimeMillis()
        _ui.value = ClinchUi(scenarios = ClinchEngine.buildSession(Random(startedAt)))
    }

    /** Returns the quality (0..3) of the chosen option, or -1 if the tap was ignored. */
    fun choose(option: Int): Int {
        val current = _ui.value
        val scenario = current.current ?: return -1
        if (current.chosen != null) return -1
        val quality = scenario.options.getOrNull(option)?.quality ?: return -1
        _ui.value = current.copy(chosen = option, qualities = current.qualities + quality)
        return quality
    }

    fun next() {
        val current = _ui.value
        if (current.chosen == null || current.saving) return
        if (!current.isLast) { _ui.value = current.copy(index = current.index + 1, chosen = null); return }
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val reward = repository.commit(ClinchEngine.outcome(current.qualities, elapsedSeconds(startedAt)))
            _ui.update { it.copy(saving = false, reward = reward) }
        }
    }
}

// =====================================================================================
// Reaction Arena
// =====================================================================================

enum class ReactionPhase { IDLE, WAITING, PROMPT, FEEDBACK, DONE }

data class ReactionUi(
    val phase: ReactionPhase = ReactionPhase.IDLE,
    val prompt: ReactionPrompt? = null,
    val index: Int = 0,
    val total: Int = ReactionEngine.SESSION_SIZE,
    val results: List<ReactionResult> = emptyList(),
    val last: ReactionResult? = null,
    val summary: ReactionSummary? = null,
    val reward: RewardSummary? = null
) {
    val correct: Int get() = results.count { it.correct }
    val liveAverage: Int get() = results.filter { it.correct }.let { c -> if (c.isEmpty()) 0 else c.sumOf { it.ms } / c.size }
}

class ReactionViewModel(private val repository: GameRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ReactionUi())
    val ui: StateFlow<ReactionUi> = _ui.asStateFlow()
    private var loop: Job? = null
    private var pending: CompletableDeferred<DefenseResponse>? = null

    fun start() {
        loop?.cancel()
        val random = Random(System.currentTimeMillis())
        val startedAt = System.currentTimeMillis()
        _ui.value = ReactionUi(phase = ReactionPhase.WAITING)
        loop = viewModelScope.launch {
            var previous: String? = null
            val results = ArrayList<ReactionResult>()
            for (i in 0 until ReactionEngine.SESSION_SIZE) {
                _ui.update { it.copy(phase = ReactionPhase.WAITING, index = i, prompt = null) }
                delay(ReactionEngine.delayMs(random))
                val prompt = ReactionEngine.nextPrompt(random, previous)
                previous = prompt.attack
                val answer = CompletableDeferred<DefenseResponse>()
                pending = answer
                val shownAt = System.nanoTime()
                _ui.update { it.copy(phase = ReactionPhase.PROMPT, prompt = prompt) }
                val chosen = withTimeoutOrNull(ReactionEngine.TIMEOUT_MS.toLong()) { answer.await() }
                pending = null
                val ms = ((System.nanoTime() - shownAt) / 1_000_000).toInt()
                val result = if (chosen == null) ReactionResult(false, ReactionEngine.TIMEOUT_MS, timedOut = true)
                else ReactionResult(chosen == prompt.correct, ms)
                results += result
                _ui.update { it.copy(phase = ReactionPhase.FEEDBACK, last = result, results = results.toList()) }
                delay(if (result.correct) 420 else 900)
            }
            val reward = repository.commit(ReactionEngine.outcome(results, elapsedSeconds(startedAt)))
            _ui.update { it.copy(phase = ReactionPhase.DONE, summary = ReactionEngine.summarize(results), reward = reward, prompt = null) }
        }
    }

    fun answer(response: DefenseResponse) { pending?.complete(response) }

    /** Leaving mid-session discards it: a reaction score from half a session would not be comparable. */
    fun abandon() {
        loop?.cancel()
        pending = null
        _ui.value = ReactionUi()
    }
}
