package com.muaythai.arena.domain.engine

import com.muaythai.arena.domain.content.FightScenarios
import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.CombatStats
import com.muaythai.arena.domain.model.Condition
import com.muaythai.arena.domain.model.CornerInstruction
import com.muaythai.arena.domain.model.CornerReport
import com.muaythai.arena.domain.model.Damage
import com.muaythai.arena.domain.model.EventOutcome
import com.muaythai.arena.domain.model.FightOption
import com.muaythai.arena.domain.model.FightPhase
import com.muaythai.arena.domain.model.FightResult
import com.muaythai.arena.domain.model.FightScenario
import com.muaythai.arena.domain.model.FightState
import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.domain.model.OpponentStrategy
import com.muaythai.arena.domain.model.RoundRecord
import com.muaythai.arena.domain.model.SessionOutcome
import com.muaythai.arena.domain.model.SessionType
import com.muaythai.arena.domain.model.Stoppage
import com.muaythai.arena.domain.model.Strategy
import com.muaythai.arena.domain.model.Tactic
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The five-round fight simulation.
 *
 * The engine is a pure state machine: every function takes a [FightState] and returns a new one.
 * Randomness comes from the state's seed and step counter, so a fight can be replayed exactly in tests.
 *
 * Flow per round: PRE_ROUND (pick a strategy) -> EVENT / EVENT_RESULT x3 -> CORNER (pick an instruction) -> next round.
 */
object FightSimulationEngine {
    const val ROUNDS = 5
    const val EVENTS_PER_ROUND = 3
    private const val HURT = 35
    private const val STOPPAGE = 100

    // ------------------------------------------------------------------ lifecycle

    fun start(
        opponent: Opponent, user: CombatStats, seed: Long, startStamina: Int = 100, campFight: Boolean = false
    ): FightState {
        val random = Random(seed)
        val oppStrategy = OpponentEngine.chooseStrategy(opponent, 1, 100, 0, 0, null, random)
        return FightState(
            opponent = opponent, user = user, opp = OpponentEngine.combatStats(opponent),
            phase = FightPhase.PRE_ROUND, round = 1, totalRounds = ROUNDS,
            userStamina = startStamina.coerceIn(40, 100), oppStamina = 100,
            userDamage = Damage(), oppDamage = Damage(),
            opponentStrategy = oppStrategy, userStrategy = null, corner = null,
            pendingEvents = emptyList(), eventIndex = 0, lastOutcome = null, usedScenarioIds = emptySet(),
            roundEventUser = 0.0, roundEventOpp = 0.0, roundQuality = 0, roundDecisions = 0,
            roundKdFor = 0, roundKdAgainst = 0, roundLog = emptyList(), rounds = emptyList(),
            stoppage = null, seed = seed, step = 1, isCampFight = campFight
        )
    }

    private fun rng(state: FightState): Random = Random(state.seed * 31 + state.step * 7919L)

    /** PRE_ROUND -> EVENT. */
    fun chooseStrategy(state: FightState, strategy: Strategy): FightState {
        if (state.phase != FightPhase.PRE_ROUND) return state
        val random = rng(state)
        val events = pickEvents(state, random)
        // A corner instruction to recover is paid out at the start of the round.
        val recovered = if (state.corner == CornerInstruction.DEFEND_RECOVER) 10 else 0
        return state.copy(
            phase = FightPhase.EVENT, userStrategy = strategy, pendingEvents = events, eventIndex = 0, lastOutcome = null,
            usedScenarioIds = state.usedScenarioIds + events.map { it.id },
            userStamina = (state.userStamina + recovered).coerceAtMost(100),
            roundEventUser = 0.0, roundEventOpp = 0.0, roundQuality = 0, roundDecisions = 0,
            roundKdFor = 0, roundKdAgainst = 0,
            roundLog = listOf("Round ${state.round}: you fight ${strategy.label.lowercase()}, they come ${state.opponentStrategy.label.lowercase()}."),
            step = state.step + 1
        )
    }

    /** EVENT -> EVENT_RESULT. */
    fun resolveEvent(state: FightState, optionIndex: Int): FightState {
        val event = state.currentEvent ?: return state
        if (state.phase != FightPhase.EVENT) return state
        val option = event.options.getOrNull(optionIndex) ?: return state
        val random = rng(state)

        val chance = successChance(state, option)
        val success = random.nextDouble() < chance
        val zone = zoneFor(option.tactic)
        var userStamina = state.userStamina
        var oppStamina = state.oppStamina
        var userDamage = state.userDamage
        var oppDamage = state.oppDamage
        var eventUser = state.roundEventUser
        var eventOpp = state.roundEventOpp
        var kdFor = state.roundKdFor
        var kdAgainst = state.roundKdAgainst
        var knockdown = false

        val cost = staminaCost(option.tactic)
        if (success) {
            eventUser += 1.5 + option.quality * 0.9
            val dmg = (3 + state.user.power / 20.0 + option.quality).roundToInt()
            oppDamage = oppDamage.hit(zone, dmg)
            userStamina -= cost
            oppStamina -= when (option.tactic) { Tactic.BODY -> 7; Tactic.CLINCH -> 5; Tactic.PRESSURE -> 4; else -> 2 }
            if (option.tactic == Tactic.FINISH && oppDamage.head >= 60) {
                knockdown = true; kdFor++; eventUser += 6.0
                oppDamage = oppDamage.hit(Zone.HEAD, 10)
            }
        } else {
            eventOpp += 3.5 - option.quality * 0.8
            val dmg = (3 + state.opp.power / 20.0 + (3 - option.quality)).roundToInt()
            userDamage = userDamage.hit(opponentZone(state.opponentStrategy, random), dmg)
            userStamina -= (cost * 1.3).roundToInt().coerceAtLeast(2)
            if (option.quality == 0 && userDamage.head >= 60) {
                knockdown = true; kdAgainst++; eventOpp += 6.0
                userDamage = userDamage.hit(Zone.HEAD, 10)
            }
        }
        userStamina = userStamina.coerceIn(0, 100)
        oppStamina = oppStamina.coerceIn(0, 100)

        val text = narrate(option.tactic, success, knockdown, state.opponent.name.substringBefore(' '), random)
        val stoppage = checkStoppage(userDamage, oppDamage, state.round)
        return state.copy(
            phase = FightPhase.EVENT_RESULT,
            lastOutcome = EventOutcome(success, text, option.quality, knockdown),
            userStamina = userStamina, oppStamina = oppStamina, userDamage = userDamage, oppDamage = oppDamage,
            roundEventUser = eventUser, roundEventOpp = eventOpp,
            roundQuality = state.roundQuality + option.quality, roundDecisions = state.roundDecisions + 1,
            roundKdFor = kdFor, roundKdAgainst = kdAgainst,
            roundLog = state.roundLog + text, stoppage = stoppage, step = state.step + 1
        )
    }

    /** EVENT_RESULT -> next EVENT, or CORNER / FINISHED once the round is scored. */
    fun advance(state: FightState): FightState {
        if (state.phase != FightPhase.EVENT_RESULT) return state
        if (state.stoppage != null) return closeRound(state, forceFinish = true)
        return if (state.eventIndex + 1 < state.pendingEvents.size) {
            state.copy(phase = FightPhase.EVENT, eventIndex = state.eventIndex + 1, lastOutcome = null, step = state.step + 1)
        } else closeRound(state, forceFinish = false)
    }

    /** CORNER -> PRE_ROUND of the next round. */
    fun applyCorner(state: FightState, instruction: CornerInstruction): FightState {
        if (state.phase != FightPhase.CORNER) return state
        val random = rng(state)
        val userRecovery = 9 + state.user.stamina / 9
        val oppRecovery = 9 + state.opp.stamina / 9
        val userStamina = (state.userStamina + userRecovery).coerceAtMost(100)
        val oppStamina = (state.oppStamina + oppRecovery).coerceAtMost(100)
        val nextRound = state.round + 1
        val oppStrategy = OpponentEngine.chooseStrategy(
            state.opponent, nextRound, oppStamina, roundsBehind = roundLead(state),
            userLegDamage = state.userDamage.legs, lastUserStrategy = state.userStrategy, random = random
        )
        return state.copy(
            phase = FightPhase.PRE_ROUND, round = nextRound, corner = instruction, userStrategy = null,
            userStamina = userStamina, oppStamina = oppStamina,
            userDamage = state.userDamage.recover(), oppDamage = state.oppDamage.recover(),
            opponentStrategy = oppStrategy, pendingEvents = emptyList(), eventIndex = 0, lastOutcome = null,
            step = state.step + 1
        )
    }

    // ------------------------------------------------------------------ events

    private enum class Zone { HEAD, BODY, LEGS, NONE }

    private fun Damage.hit(zone: Zone, amount: Int): Damage = when (zone) {
        Zone.HEAD -> copy(head = (head + amount).coerceAtMost(120))
        Zone.BODY -> copy(body = (body + amount).coerceAtMost(120))
        Zone.LEGS -> copy(legs = (legs + amount).coerceAtMost(120))
        Zone.NONE -> this
    }

    private fun Damage.recover(): Damage = copy(head = (head - 6).coerceAtLeast(0), body = (body - 2).coerceAtLeast(0))

    private fun zoneFor(tactic: Tactic): Zone = when (tactic) {
        Tactic.LEGS -> Zone.LEGS
        Tactic.BODY, Tactic.CLINCH, Tactic.PRESSURE -> Zone.BODY
        Tactic.HEAD, Tactic.COUNTER, Tactic.FINISH, Tactic.ELBOW -> Zone.HEAD
        Tactic.DISTANCE -> Zone.BODY
        Tactic.DEFEND, Tactic.ANGLE, Tactic.PACE -> Zone.NONE
    }

    private fun opponentZone(strategy: OpponentStrategy, random: Random): Zone = when (strategy) {
        OpponentStrategy.LOW_KICK_HEAVY -> Zone.LEGS
        OpponentStrategy.CLINCH, OpponentStrategy.LONG_RANGE -> Zone.BODY
        OpponentStrategy.COUNTER -> Zone.HEAD
        else -> if (random.nextBoolean()) Zone.HEAD else Zone.BODY
    }

    private fun staminaCost(tactic: Tactic): Int = when (tactic) {
        Tactic.FINISH -> 9
        Tactic.PRESSURE -> 7
        Tactic.CLINCH, Tactic.HEAD -> 5
        Tactic.BODY, Tactic.ELBOW -> 4
        Tactic.LEGS -> 3
        Tactic.COUNTER, Tactic.ANGLE, Tactic.DISTANCE -> 2
        Tactic.DEFEND -> -4
        Tactic.PACE -> -6
    }

    /** The pair (user attribute, opponent attribute) that decides an exchange of the given flavour. */
    private fun contest(tactic: Tactic, u: CombatStats, o: CombatStats): Pair<Int, Int> = when (tactic) {
        Tactic.LEGS -> u.kicking to o.defense
        Tactic.BODY -> (u.kicking + u.boxing) / 2 to o.defense
        Tactic.HEAD -> u.boxing to (o.defense + o.fightIq) / 2
        Tactic.CLINCH -> u.clinch to o.clinch
        Tactic.DISTANCE -> (u.kicking + u.speed) / 2 to (o.aggression + o.speed) / 2
        Tactic.COUNTER -> (u.reaction + u.fightIq) / 2 to (o.speed + o.fightIq) / 2
        Tactic.DEFEND -> u.defense to (o.power + o.boxing) / 2
        Tactic.PRESSURE -> (u.boxing + u.stamina) / 2 to (o.defense + o.stamina) / 2
        Tactic.ANGLE -> (u.speed + u.fightIq) / 2 to (o.speed + o.aggression) / 2
        Tactic.FINISH -> (u.power + u.boxing) / 2 to (o.defense + o.stamina) / 2
        Tactic.PACE -> u.fightIq to o.aggression
        Tactic.ELBOW -> (u.boxing + u.power) / 2 to o.defense
    }

    fun successChance(state: FightState, option: FightOption): Double {
        val (mine, theirs) = contest(option.tactic, state.user, state.opp)
        var p = 0.26 + option.quality * 0.14 + (mine - theirs) / 90.0
        if (option.tactic in state.opponent.weaknesses) p += 0.10
        if (state.userStrategy?.tactic == option.tactic) p += 0.05
        if (state.userStamina < 35 && staminaCost(option.tactic) >= 5) p -= 0.14
        if (state.oppStamina < 35) p += 0.06
        return p.coerceIn(0.06, 0.95)
    }

    fun conditionHolds(condition: Condition, state: FightState): Boolean = when (condition) {
        Condition.ANY -> true
        Condition.EARLY -> state.round <= 2
        Condition.LATE -> state.round >= 4
        Condition.USER_TIRED -> state.userStamina < 45
        Condition.OPP_TIRED -> state.oppStamina < 45
        Condition.USER_LEG_HURT -> state.userDamage.legs >= HURT
        Condition.OPP_LEG_HURT -> state.oppDamage.legs >= HURT
        Condition.OPP_BODY_HURT -> state.oppDamage.body >= HURT
        Condition.USER_HURT -> state.userDamage.head >= 40
        Condition.OPP_HURT -> state.oppDamage.head >= 40
        Condition.WINNING -> state.rounds.isNotEmpty() && roundLead(state) >= 1
        Condition.LOSING -> state.rounds.isNotEmpty() && roundLead(state) <= -1
        Condition.OPP_PRESSURE -> state.opponentStrategy == OpponentStrategy.PRESSURE
        Condition.OPP_COUNTER -> state.opponentStrategy == OpponentStrategy.COUNTER
        Condition.OPP_LONG -> state.opponentStrategy == OpponentStrategy.LONG_RANGE
        Condition.OPP_CLINCH -> state.opponentStrategy == OpponentStrategy.CLINCH
        Condition.OPP_LOW_KICK -> state.opponentStrategy == OpponentStrategy.LOW_KICK_HEAVY
        Condition.OPP_VOLUME -> state.opponentStrategy == OpponentStrategy.HIGH_VOLUME
    }

    /** Situational scenarios are preferred, so the round reflects what is actually happening in the fight. */
    private fun pickEvents(state: FightState, random: Random): List<FightScenario> {
        val available = FightScenarios.all.filter { it.id !in state.usedScenarioIds && conditionHolds(it.condition, state) }
        val situational = available.filter { it.condition != Condition.ANY }.shuffled(random)
        val general = available.filter { it.condition == Condition.ANY }.shuffled(random)
        val picked = (situational.take(2) + general).take(EVENTS_PER_ROUND).toMutableList()
        if (picked.size < EVENTS_PER_ROUND) {
            // Everything fitting has been used already; allow repeats rather than a short round.
            val fallback = FightScenarios.all.filter { conditionHolds(it.condition, state) && it !in picked }.shuffled(random)
            picked += fallback.take(EVENTS_PER_ROUND - picked.size)
        }
        return picked.shuffled(random)
    }

    private val successLines: Map<Tactic, List<String>> = mapOf(
        Tactic.LEGS to listOf("Your shin chops into the thigh. %s's stance buckles for a moment.", "A clean low kick lands above the knee. %s limps a step."),
        Tactic.BODY to listOf("The kick thuds into the ribs and %s exhales hard.", "You dig to the body. %s's elbows drop to cover."),
        Tactic.HEAD to listOf("A straight shot snaps %s's head back.", "Your punches find the gap in the guard. %s blinks."),
        Tactic.CLINCH to listOf("You lock the neck and drive a knee into the body. %s grunts.", "Inside position is yours. Two knees score before the break."),
        Tactic.DISTANCE to listOf("The teep lands flush on the stomach and shoves %s back.", "You keep %s on the end of your leg. They cannot get in."),
        Tactic.COUNTER to listOf("%s commits and you answer first. A clean counter lands.", "You make them miss and punish it. The judges saw that."),
        Tactic.DEFEND to listOf("Everything %s throws lands on gloves and shins. You breathe.", "Tight guard, small steps. The storm passes without damage."),
        Tactic.PRESSURE to listOf("You march %s to the ropes and unload. The crowd is on its feet.", "Relentless. %s is forced to cover and give ground."),
        Tactic.ANGLE to listOf("One pivot and %s is punching at empty space.", "You step off the line and take the centre of the ring."),
        Tactic.FINISH to listOf("A huge shot lands and %s's legs dip!", "You open up and %s is in real trouble."),
        Tactic.PACE to listOf("You slow the fight to your tempo and get your breath back.", "Smart. You take the sting out of the round and recover."),
        Tactic.ELBOW to listOf("A short elbow slices through the guard. %s touches their brow.", "The elbow lands over the top. That one cut.")
    )

    private val failLines: Map<Tactic, List<String>> = mapOf(
        Tactic.LEGS to listOf("%s checks it shin on shin. That hurt you more than them.", "%s reads the low kick and fires a cross over it."),
        Tactic.BODY to listOf("%s catches the kick and dumps you off balance.", "Your body attack lands on the elbow. %s answers immediately."),
        Tactic.HEAD to listOf("You swing and %s counters cleanly through the middle.", "The exchange goes badly. %s lands the harder shots."),
        Tactic.CLINCH to listOf("%s wins the grip fight and turns you. Knees score against you.", "You reach for the neck and eat an elbow on the way in."),
        Tactic.DISTANCE to listOf("%s parries the teep and steps inside with punches.", "The teep is caught. You hop on one leg and take a kick."),
        Tactic.COUNTER to listOf("You wait too long. %s's lead lands before your answer.", "The feint draws your counter and %s punishes it."),
        Tactic.DEFEND to listOf("You cover up, but %s finds the body around the guard.", "Standing still behind the guard, you give the round away."),
        Tactic.PRESSURE to listOf("You walk straight onto a counter. %s was waiting for that.", "The pressure costs you energy and %s slips away."),
        Tactic.ANGLE to listOf("%s cuts off the exit and traps you.", "You pivot late and a kick catches you mid-step."),
        Tactic.FINISH to listOf("You load up and miss. %s makes you pay.", "The big shot never lands and your arms are heavier for it."),
        Tactic.PACE to listOf("You ease off and %s takes the initiative.", "While you coast, %s steals the exchange."),
        Tactic.ELBOW to listOf("You throw the elbow from too far and %s clinches.", "The elbow misses and you are squared up in front of %s.")
    )

    private fun narrate(tactic: Tactic, success: Boolean, knockdown: Boolean, name: String, random: Random): String {
        val pool = (if (success) successLines else failLines)[tactic] ?: listOf("The exchange is even.")
        val line = pool[random.nextInt(pool.size)].replace("%s", name)
        return when {
            knockdown && success -> "$line KNOCKDOWN! $name beats the count."
            knockdown -> "$line You are DOWN, and up at eight."
            else -> line
        }
    }

    // ------------------------------------------------------------------ round scoring

    private fun weighted(vararg parts: Pair<Int, Double>): Double = parts.sumOf { it.first * it.second }

    private fun userOffense(s: Strategy, u: CombatStats): Double = when (s) {
        Strategy.PRESSURE -> weighted(u.boxing to .40, u.stamina to .25, u.power to .20, u.aggression to .15)
        Strategy.COUNTER -> weighted(u.reaction to .35, u.fightIq to .30, u.defense to .20, u.boxing to .15)
        Strategy.LONG_RANGE -> weighted(u.kicking to .50, u.speed to .25, u.fightIq to .25)
        Strategy.LOW_KICK -> weighted(u.kicking to .50, u.power to .30, u.speed to .20)
        Strategy.BODY_ATTACK -> weighted(u.kicking to .30, u.boxing to .30, u.power to .25, u.stamina to .15)
        Strategy.CLINCH -> weighted(u.clinch to .60, u.power to .20, u.stamina to .20)
        Strategy.DEFENSIVE -> weighted(u.defense to .50, u.fightIq to .30, u.reaction to .20) * 0.72
        Strategy.BALANCED -> weighted(u.boxing to .25, u.kicking to .25, u.defense to .25, u.fightIq to .25)
    }

    private fun opponentOffense(s: OpponentStrategy, o: CombatStats): Double = when (s) {
        OpponentStrategy.PRESSURE -> weighted(o.boxing to .40, o.stamina to .25, o.power to .20, o.aggression to .15)
        OpponentStrategy.COUNTER -> weighted(o.fightIq to .35, o.defense to .25, o.speed to .20, o.boxing to .20)
        OpponentStrategy.LONG_RANGE -> weighted(o.kicking to .55, o.speed to .25, o.fightIq to .20)
        OpponentStrategy.CLINCH -> weighted(o.clinch to .60, o.power to .20, o.stamina to .20)
        OpponentStrategy.LOW_KICK_HEAVY -> weighted(o.kicking to .50, o.power to .30, o.aggression to .20)
        OpponentStrategy.HIGH_VOLUME -> weighted(o.boxing to .35, o.stamina to .35, o.speed to .30)
    }

    private fun staminaFactor(stamina: Int): Double = 0.55 + 0.45 * stamina / 100.0

    private fun userRoundCost(s: Strategy): Int = when (s) {
        Strategy.PRESSURE -> 20; Strategy.CLINCH -> 18; Strategy.BODY_ATTACK -> 16; Strategy.LOW_KICK -> 14
        Strategy.LONG_RANGE -> 13; Strategy.BALANCED -> 13; Strategy.COUNTER -> 11; Strategy.DEFENSIVE -> 5
    }

    private fun opponentRoundCost(s: OpponentStrategy): Int = when (s) {
        OpponentStrategy.HIGH_VOLUME -> 21; OpponentStrategy.PRESSURE -> 19; OpponentStrategy.CLINCH -> 17
        OpponentStrategy.LOW_KICK_HEAVY -> 14; OpponentStrategy.LONG_RANGE -> 13; OpponentStrategy.COUNTER -> 10
    }

    fun cornerMatchesStrategy(corner: CornerInstruction?, strategy: Strategy): Boolean = when (corner) {
        CornerInstruction.INCREASE_PRESSURE -> strategy == Strategy.PRESSURE
        CornerInstruction.TARGET_LEGS -> strategy == Strategy.LOW_KICK
        CornerInstruction.TARGET_BODY -> strategy == Strategy.BODY_ATTACK
        CornerInstruction.STAY_LONG -> strategy == Strategy.LONG_RANGE
        CornerInstruction.COUNTER_MORE -> strategy == Strategy.COUNTER
        CornerInstruction.CLINCH_MORE -> strategy == Strategy.CLINCH
        CornerInstruction.DEFEND_RECOVER -> strategy == Strategy.DEFENSIVE || strategy == Strategy.COUNTER
        CornerInstruction.INCREASE_VOLUME -> strategy == Strategy.PRESSURE || strategy == Strategy.BALANCED || strategy == Strategy.BODY_ATTACK
        null -> false
    }

    /** Positive when the user has won more rounds than the opponent so far. */
    fun roundLead(state: FightState): Int = state.rounds.count { it.userWon } - state.rounds.count { !it.userWon }

    private fun closeRound(state: FightState, forceFinish: Boolean): FightState {
        val strategy = state.userStrategy ?: Strategy.BALANCED
        val oppStrategy = state.opponentStrategy
        val random = rng(state)
        val u = state.user
        val o = state.opp
        val matchup = OpponentEngine.matchup(strategy, oppStrategy)

        val userDefense = weighted(u.defense to .50, u.fightIq to .20, u.reaction to .30)
        val oppDefense = weighted(o.defense to .60, o.fightIq to .25, o.speed to .15)

        var userBase = 10 + (userOffense(strategy, u) * staminaFactor(state.userStamina) - oppDefense * 0.55) * 0.45
        var oppBase = 10 + (opponentOffense(oppStrategy, o) * staminaFactor(state.oppStamina) - userDefense * 0.55) * 0.45
        userBase += matchup * 1.6
        oppBase -= matchup * 1.6
        if (strategy.tactic in state.opponent.weaknesses) userBase += 3.0
        if (strategy == Strategy.DEFENSIVE) oppBase -= 3.0
        if (cornerMatchesStrategy(state.corner, strategy)) userBase += 1.8
        if (state.corner == CornerInstruction.INCREASE_VOLUME || state.corner == CornerInstruction.INCREASE_PRESSURE) userBase += 1.0
        state.rounds.lastOrNull()?.let { prev ->
            val momentum = (prev.userPoints - prev.opponentPoints).coerceIn(-8.0, 8.0) * 0.12
            userBase += momentum
        }
        userBase -= state.userDamage.total / 45.0
        oppBase -= state.oppDamage.total / 45.0
        userBase += random.nextDouble(-2.5, 2.5)
        oppBase += random.nextDouble(-2.5, 2.5)
        userBase = userBase.coerceAtLeast(2.0)
        oppBase = oppBase.coerceAtLeast(2.0)

        // Damage dealt by the general flow of the round.
        val userPower = 0.7 + u.power / 160.0
        val oppPower = (0.7 + o.power / 160.0) * (1.0 - userDefense / 260.0) * (if (strategy == Strategy.DEFENSIVE) 0.55 else 1.0)
        fun d(base: Double, share: Double, power: Double) = (base * share * power).roundToInt()
        var oppDamage = state.oppDamage
        var userDamage = state.userDamage
        if (!forceFinish) {
            oppDamage = when (strategy) {
                Strategy.LOW_KICK -> oppDamage.hit(Zone.LEGS, d(userBase, .60, userPower))
                Strategy.BODY_ATTACK -> oppDamage.hit(Zone.BODY, d(userBase, .55, userPower))
                Strategy.CLINCH -> oppDamage.hit(Zone.BODY, d(userBase, .42, userPower))
                Strategy.PRESSURE -> oppDamage.hit(Zone.HEAD, d(userBase, .38, userPower)).hit(Zone.BODY, d(userBase, .20, userPower))
                Strategy.COUNTER -> oppDamage.hit(Zone.HEAD, d(userBase, .42, userPower))
                Strategy.LONG_RANGE -> oppDamage.hit(Zone.BODY, d(userBase, .30, userPower)).hit(Zone.LEGS, d(userBase, .15, userPower))
                Strategy.BALANCED -> oppDamage.hit(Zone.HEAD, d(userBase, .18, userPower)).hit(Zone.BODY, d(userBase, .18, userPower)).hit(Zone.LEGS, d(userBase, .18, userPower))
                Strategy.DEFENSIVE -> oppDamage.hit(Zone.HEAD, d(userBase, .10, userPower))
            }
            userDamage = when (oppStrategy) {
                OpponentStrategy.LOW_KICK_HEAVY -> userDamage.hit(Zone.LEGS, d(oppBase, .60, oppPower))
                OpponentStrategy.CLINCH -> userDamage.hit(Zone.BODY, d(oppBase, .48, oppPower))
                OpponentStrategy.PRESSURE -> userDamage.hit(Zone.HEAD, d(oppBase, .36, oppPower)).hit(Zone.BODY, d(oppBase, .20, oppPower))
                OpponentStrategy.COUNTER -> userDamage.hit(Zone.HEAD, d(oppBase, .40, oppPower))
                OpponentStrategy.LONG_RANGE -> userDamage.hit(Zone.BODY, d(oppBase, .36, oppPower)).hit(Zone.LEGS, d(oppBase, .10, oppPower))
                OpponentStrategy.HIGH_VOLUME -> userDamage.hit(Zone.HEAD, d(oppBase, .26, oppPower)).hit(Zone.BODY, d(oppBase, .22, oppPower))
            }
        }

        val userPoints = userBase + state.roundEventUser
        val oppPoints = oppBase + state.roundEventOpp

        // Stamina spent on the round itself.
        val userEfficiency = 1.25 - u.stamina / 200.0
        val oppEfficiency = 1.25 - o.stamina / 200.0
        var userCost = userRoundCost(strategy) * userEfficiency + (userDamage.body - state.userDamage.body) / 3.0
        if (state.corner == CornerInstruction.INCREASE_VOLUME || state.corner == CornerInstruction.INCREASE_PRESSURE) userCost += 4
        val oppCost = opponentRoundCost(oppStrategy) * oppEfficiency + (oppDamage.body - state.oppDamage.body) / 3.0
        val userStamina = (state.userStamina - userCost.roundToInt()).coerceIn(0, 100)
        val oppStamina = (state.oppStamina - oppCost.roundToInt()).coerceIn(0, 100)

        // Three judges, ten-point must.
        val judges = List(3) {
            val margin = userPoints - oppPoints + random.nextDouble(-2.0, 2.0)
            when {
                margin > 0 && (margin > 19 || state.roundKdFor > state.roundKdAgainst) -> 10 to 8
                margin > 0 -> 10 to 9
                margin <= 0 && (margin < -19 || state.roundKdAgainst > state.roundKdFor) -> 8 to 10
                else -> 9 to 10
            }
        }

        // Strike counts for the statistics screens.
        val share = userPoints / (userPoints + oppPoints)
        val userThrown = (26 + userRoundCost(strategy) * 1.6 + random.nextInt(8)).roundToInt()
        val oppThrown = (26 + opponentRoundCost(oppStrategy) * 1.6 + random.nextInt(8)).roundToInt()
        val userLanded = (userThrown * (0.16 + share * 0.52).coerceIn(0.12, 0.78)).roundToInt()
        val oppLanded = (oppThrown * (0.16 + (1 - share) * 0.52).coerceIn(0.12, 0.78)).roundToInt()
        val kickShare = when (strategy) {
            Strategy.LONG_RANGE, Strategy.LOW_KICK -> 0.62
            Strategy.BODY_ATTACK -> 0.45
            Strategy.CLINCH -> 0.15
            Strategy.PRESSURE -> 0.25
            else -> 0.35
        }
        val kicksThrown = (userThrown * kickShare).roundToInt().coerceAtLeast(3)
        val kickAccuracy = (0.20 + (u.kicking - o.defense) / 150.0 + share * 0.45).coerceIn(0.12, 0.85)
        val kicksLanded = (kicksThrown * kickAccuracy).roundToInt()
        val clinchTotal = when {
            strategy == Strategy.CLINCH && oppStrategy == OpponentStrategy.CLINCH -> 8
            strategy == Strategy.CLINCH || oppStrategy == OpponentStrategy.CLINCH -> 6
            else -> 2
        }
        val clinchRate = (0.5 + (u.clinch - o.clinch) / 90.0 + (if (strategy == Strategy.CLINCH) 0.08 else 0.0)).coerceIn(0.1, 0.9)
        val clinchWon = (0 until clinchTotal).count { random.nextDouble() < clinchRate }

        val record = RoundRecord(
            number = state.round, userStrategy = strategy, opponentStrategy = oppStrategy,
            userPoints = userPoints, opponentPoints = oppPoints, judges = judges,
            userLanded = userLanded, userThrown = userThrown, opponentLanded = oppLanded, opponentThrown = oppThrown,
            kicksLanded = kicksLanded, kicksThrown = kicksThrown, clinchWon = clinchWon, clinchTotal = clinchTotal,
            userStaminaEnd = userStamina, opponentStaminaEnd = oppStamina, matchup = matchup,
            decisionQuality = state.roundQuality, decisions = state.roundDecisions,
            knockdownsFor = state.roundKdFor, knockdownsAgainst = state.roundKdAgainst, log = state.roundLog
        )
        val rounds = state.rounds + record
        val stoppage = state.stoppage ?: checkStoppage(userDamage, oppDamage, state.round)
            ?: refereeStoppage(userPoints - oppPoints, userDamage, oppDamage, state.round)
        val finished = stoppage != null || state.round >= state.totalRounds
        return state.copy(
            phase = if (finished) FightPhase.FINISHED else FightPhase.CORNER,
            rounds = rounds, userStamina = userStamina, oppStamina = oppStamina,
            userDamage = userDamage, oppDamage = oppDamage, stoppage = stoppage, lastOutcome = null,
            step = state.step + 1
        )
    }

    private fun checkStoppage(user: Damage, opp: Damage, round: Int): Stoppage? = when {
        opp.head >= STOPPAGE -> Stoppage(true, "KO", round)
        opp.body >= STOPPAGE -> Stoppage(true, "TKO (body shots)", round)
        opp.legs >= STOPPAGE -> Stoppage(true, "TKO (leg kicks)", round)
        user.head >= STOPPAGE -> Stoppage(false, "KO", round)
        user.body >= STOPPAGE -> Stoppage(false, "TKO (body shots)", round)
        user.legs >= STOPPAGE -> Stoppage(false, "TKO (leg kicks)", round)
        else -> null
    }

    private fun refereeStoppage(margin: Double, user: Damage, opp: Damage, round: Int): Stoppage? = when {
        margin > 20 && opp.total >= 170 -> Stoppage(true, "TKO (referee stoppage)", round)
        margin < -20 && user.total >= 170 -> Stoppage(false, "TKO (referee stoppage)", round)
        else -> null
    }

    // ------------------------------------------------------------------ corner

    fun cornerReport(state: FightState): CornerReport? {
        val round = state.rounds.lastOrNull() ?: return null
        val userCards = round.judges.count { it.first > it.second }
        val card = round.judges.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: (10 to 9)
        val (suggested, reason) = recommend(state)
        val userWeapon = when (round.userStrategy) {
            Strategy.PRESSURE -> "Combinations coming forward"
            Strategy.COUNTER -> "Counter punches"
            Strategy.LONG_RANGE -> "Teep and rear kick"
            Strategy.LOW_KICK -> "Low kicks"
            Strategy.BODY_ATTACK -> "Body kicks and hooks"
            Strategy.CLINCH -> "Knees in the clinch"
            Strategy.DEFENSIVE -> "Your guard"
            Strategy.BALANCED -> if (round.kicksLanded * 2 >= round.userLanded) "Kicks" else "Hands"
        }
        val oppWeapon = when (round.opponentStrategy) {
            OpponentStrategy.PRESSURE -> "Forward pressure and hooks"
            OpponentStrategy.COUNTER -> "Counters when you lead"
            OpponentStrategy.LONG_RANGE -> "Long kicks and the teep"
            OpponentStrategy.CLINCH -> "Clinch knees"
            OpponentStrategy.LOW_KICK_HEAVY -> "Low kicks to your lead leg"
            OpponentStrategy.HIGH_VOLUME -> "Punches in bunches"
        }
        return CornerReport(
            roundNumber = round.number,
            scoreLine = "${card.first}-${card.second}  ($userCards of 3 judges for you)",
            userWonRound = userCards >= 2,
            successfulAttacks = round.userLanded,
            opponentWeapon = oppWeapon,
            userWeapon = userWeapon,
            stamina = state.userStamina,
            opponentStamina = state.oppStamina,
            userDamage = state.userDamage,
            oppDamage = state.oppDamage,
            recommendation = reason,
            suggested = suggested
        )
    }

    private fun recommend(state: FightState): Pair<CornerInstruction, String> {
        val opp = state.opponent
        val name = opp.name.substringBefore(' ')
        return when {
            state.userStamina < 35 ->
                CornerInstruction.DEFEND_RECOVER to "Your tank is at ${state.userStamina}%. Take a quieter round or you will have nothing left for the finish."
            state.oppDamage.legs >= HURT ->
                CornerInstruction.TARGET_LEGS to "$name's lead leg is compromised. Keep chopping and it will go."
            state.oppDamage.body >= HURT || state.oppStamina < 40 ->
                CornerInstruction.TARGET_BODY to "$name is slowing down. Go to the body and take the rest of the gas."
            state.userDamage.legs >= HURT ->
                CornerInstruction.CLINCH_MORE to "Your lead leg is taking damage. Close the distance where low kicks cannot reach."
            Tactic.CLINCH in opp.weaknesses ->
                CornerInstruction.CLINCH_MORE to "$name is weak on the inside. Get past the kicks and lock up."
            Tactic.DISTANCE in opp.weaknesses ->
                CornerInstruction.STAY_LONG to "$name needs to be close. Stay long and make them walk through the teep."
            Tactic.COUNTER in opp.weaknesses ->
                CornerInstruction.COUNTER_MORE to "$name over-commits. Let them lead and answer every time."
            Tactic.LEGS in opp.weaknesses ->
                CornerInstruction.TARGET_LEGS to "Single low kicks are the safe score against $name. Keep landing them."
            roundLead(state) < 0 ->
                CornerInstruction.INCREASE_VOLUME to "You are behind on the cards. Out-work them and make the judges notice."
            state.opponentStrategy == OpponentStrategy.PRESSURE || state.opponentStrategy == OpponentStrategy.HIGH_VOLUME ->
                CornerInstruction.COUNTER_MORE to "They are coming forward recklessly. Counter and turn."
            state.opponentStrategy == OpponentStrategy.COUNTER ->
                CornerInstruction.TARGET_LEGS to "Do not feed the counters. Score with low kicks and get out."
            else ->
                CornerInstruction.INCREASE_VOLUME to "It is close. Raise the work rate and steal the round late."
        }
    }

    // ------------------------------------------------------------------ result

    private fun pct(value: Double): Int = value.roundToInt().coerceIn(15, 99)

    fun result(state: FightState, timestamp: Long): FightResult {
        val rounds = state.rounds
        val stoppage = state.stoppage
        val judgeTotals = (0 until 3).map { j ->
            rounds.sumOf { it.judges[j].first } to rounds.sumOf { it.judges[j].second }
        }
        val cardsFor = judgeTotals.count { it.first > it.second }
        val cardsAgainst = judgeTotals.count { it.first < it.second }
        val won: Boolean
        val draw: Boolean
        val method: String
        if (stoppage != null) {
            won = stoppage.userWon; draw = false; method = stoppage.method
        } else {
            won = cardsFor >= 2
            draw = !won && cardsAgainst < 2
            method = "Decision"
        }

        val thrown = rounds.sumOf { it.userThrown }.coerceAtLeast(1)
        val oppThrown = rounds.sumOf { it.opponentThrown }.coerceAtLeast(1)
        val kicksThrown = rounds.sumOf { it.kicksThrown }.coerceAtLeast(1)
        val clinchTotal = rounds.sumOf { it.clinchTotal }
        val decisions = rounds.sumOf { it.decisions }.coerceAtLeast(1)
        val striking = pct(38 + rounds.sumOf { it.userLanded } * 100.0 / thrown)
        val defense = pct(118 - rounds.sumOf { it.opponentLanded } * 150.0 / oppThrown)
        val kicking = pct(36 + rounds.sumOf { it.kicksLanded } * 105.0 / kicksThrown)
        val clinch = if (clinchTotal == 0) 50 else pct(28 + rounds.sumOf { it.clinchWon } * 72.0 / clinchTotal)
        val fightIq = pct(28 + rounds.sumOf { it.decisionQuality } * 72.0 / (decisions * 3))
        val stamina = pct(rounds.sumOf { it.userStaminaEnd }.toDouble() / rounds.size.coerceAtLeast(1) + 22)
        val tactical = pct(58 + rounds.sumOf { it.matchup } * 20.0 / rounds.size.coerceAtLeast(1))
        val overall = (striking + defense + kicking + clinch + fightIq * 2 + stamina + tactical) / 8

        val tier = state.opponent.tier
        val xp = 220 + overall * 3 + (if (won) 250 + tier * 70 else 0) + (if (won && stoppage != null) 100 else 0) + (if (draw) 100 else 0)
        val mainStrategy = rounds.groupingBy { it.userStrategy }.eachCount().maxByOrNull { it.value }?.key ?: Strategy.BALANCED

        return FightResult(
            timestamp = timestamp, opponentId = state.opponent.id, opponentName = state.opponent.name, opponentTier = tier,
            won = won, draw = draw, method = method, endRound = stoppage?.round ?: rounds.size,
            scorecards = judgeTotals.map { "${it.first}-${it.second}" },
            roundResults = rounds.map { r ->
                val cards = r.judges.count { it.first > it.second }
                if (cards >= 2) "W" else "L"
            },
            striking = striking, defense = defense, kicking = kicking, clinch = clinch, fightIq = fightIq,
            stamina = stamina, tactical = tactical, overall = overall, xp = xp, mainStrategy = mainStrategy,
            campFight = state.isCampFight
        )
    }

    fun rankPoints(result: FightResult): Int = when {
        result.won -> 20 + result.opponentTier * 12 + (if (result.method != "Decision") 8 else 0)
        result.draw -> 8
        else -> 3
    }

    fun coins(result: FightResult): Int = if (result.won) 40 + result.opponentTier * 20 else 15

    /** What a finished fight adds to the training log and the fighter's attributes. */
    fun sessionOutcome(result: FightResult, durationSec: Int): SessionOutcome {
        val gains = linkedMapOf(
            Attribute.STRIKING to result.striking,
            Attribute.KICKING to result.kicking,
            Attribute.DEFENSE to result.defense,
            Attribute.CLINCH to result.clinch * 7 / 10,
            Attribute.FIGHT_IQ to result.fightIq * 3 / 2,
            Attribute.STAMINA to result.stamina,
            Attribute.CONDITIONING to result.stamina * 8 / 10,
            Attribute.POWER to 30 + (if (result.method != "Decision" && result.won) 40 else 0),
            Attribute.SPEED to 35,
            Attribute.REACTION to 35
        )
        val verdict = when { result.won -> "Win"; result.draw -> "Draw"; else -> "Loss" }
        return SessionOutcome(
            type = SessionType.FIGHT,
            title = "$verdict vs ${result.opponentName} (${result.method})",
            durationSec = durationSec,
            xp = result.xp,
            performance = result.overall,
            gains = gains,
            reps = result.roundResults.count { it == "W" },
            correct = result.roundResults.count { it == "W" },
            total = result.roundResults.size,
            tag = result.mainStrategy.name,
            success = result.won
        )
    }
}
