package com.muaythai.arena.domain.model

/** The ten RPG attributes of the user's fighter. */
enum class Attribute(val label: String, val short: String, val blurb: String) {
    STRIKING("Striking", "STR", "Hands and elbows: accuracy, timing and volume."),
    KICKING("Kicking", "KCK", "Roundhouse, teep and low kick quality."),
    DEFENSE("Defense", "DEF", "Checks, parries, blocks and head movement."),
    CLINCH("Clinch", "CLN", "Control, knees, turns and off-balancing."),
    CONDITIONING("Conditioning", "CON", "How hard you can work round after round."),
    SPEED("Speed", "SPD", "How fast you start and finish your weapons."),
    POWER("Power", "PWR", "The damage behind each clean shot."),
    FIGHT_IQ("Fight IQ", "IQ", "Reading the fight and choosing the right tool."),
    REACTION("Reaction", "RCT", "How quickly you recognise and answer attacks."),
    STAMINA("Stamina", "STA", "Your gas tank in the late rounds.")
}

enum class Archetype(val label: String, val tagline: String) {
    PRESSURE("Pressure Fighter", "High aggression and volume."),
    TECHNICAL("Technical Fighter", "High accuracy and defense."),
    KICKER("Kicker", "Strong kicking game."),
    CLINCHER("Clincher", "Strong clinch and knees."),
    COUNTER("Counter Fighter", "Strong reaction and counter attacks."),
    COMPLETE("Complete Fighter", "Balanced development.")
}

enum class Rank(val label: String, val minLevel: Int, val minRankPoints: Int, val elite: Boolean) {
    NOVICE("Novice", 1, 0, false),
    BEGINNER("Beginner", 3, 30, false),
    AMATEUR("Amateur", 6, 100, false),
    INTERMEDIATE("Intermediate", 10, 240, false),
    ADVANCED("Advanced", 15, 500, false),
    FIGHTER("Fighter", 22, 950, false),
    ELITE("Elite Fighter", 30, 1700, true),
    CHAMPION("Champion", 40, 2800, true)
}

enum class SessionType(val label: String) {
    TECHNIQUE("Technique Drill"),
    COMBINATION("Combination Lab"),
    DEFENSE("Defense Lab"),
    CLINCH("Clinch Lab"),
    REACTION("Reaction Arena"),
    ROUND("Round Work"),
    PROGRAM("Training Program"),
    CAMP("Fight Camp"),
    FIGHT("Fight Lab"),
    CLUB("Club")
}

data class Fighter(
    val name: String,
    val style: Archetype,
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

/** One logged piece of training. Everything on the History screen and most statistics derive from these. */
data class TrainingSession(
    val id: Long = 0,
    val timestamp: Long,
    val epochDay: Long,
    val type: SessionType,
    val title: String,
    val durationSec: Int,
    val xp: Int,
    /** 0..100 quality of the session. */
    val performance: Int,
    val reps: Int = 0,
    val correct: Int = 0,
    val total: Int = 0,
    /** Free-form key used by challenges: technique id, strategy name, opponent id... */
    val tag: String = "",
    val success: Boolean = false,
    /** Average reaction time in ms for reaction sessions, otherwise 0. */
    val valueMs: Int = 0,
    /** "STRIKING:20,SPEED:5" */
    val attributeGains: String = "",
    /** Personal-record changes caused by this session. */
    val note: String = ""
) {
    fun gains(): Map<Attribute, Int> = parseGains(attributeGains)
}

fun parseGains(raw: String): Map<Attribute, Int> {
    if (raw.isBlank()) return emptyMap()
    val out = LinkedHashMap<Attribute, Int>()
    for (part in raw.split(',')) {
        val kv = part.split(':')
        if (kv.size != 2) continue
        val attr = Attribute.entries.firstOrNull { it.name == kv[0] } ?: continue
        val v = kv[1].toIntOrNull() ?: continue
        out[attr] = (out[attr] ?: 0) + v
    }
    return out
}

fun encodeGains(gains: Map<Attribute, Int>): String =
    gains.filterValues { it != 0 }.entries.joinToString(",") { "${it.key.name}:${it.value}" }

// ---------------------------------------------------------------- techniques & combinations

enum class StrikeType(val label: String) { PUNCH("Punch"), KICK("Kick"), KNEE("Knee"), ELBOW("Elbow") }

enum class TechniqueCategory(val label: String, val type: StrikeType) {
    JAB("Jab", StrikeType.PUNCH),
    CROSS("Cross", StrikeType.PUNCH),
    HOOK("Hook", StrikeType.PUNCH),
    UPPERCUT("Uppercut", StrikeType.PUNCH),
    TEEP("Teep", StrikeType.KICK),
    ROUNDHOUSE("Roundhouse Kick", StrikeType.KICK),
    LOW_KICK("Low Kick", StrikeType.KICK),
    BODY_KICK("Body Kick", StrikeType.KICK),
    HIGH_KICK("High Kick", StrikeType.KICK),
    SWITCH_KICK("Switch Kick", StrikeType.KICK),
    KNEE("Knee", StrikeType.KNEE),
    ELBOW("Elbow", StrikeType.ELBOW)
}

data class Technique(
    val id: String,
    val category: TechniqueCategory,
    val name: String,
    /** 1..5 */
    val difficulty: Int,
    val target: String,
    val range: String,
    val setup: String,
    val mistake: String,
    val cue: String,
    /** XP for one set of ten repetitions. */
    val xpReward: Int,
    val masteryReps: Int
)

/** A single weapon that can be placed in a combination. */
enum class Strike(val label: String, val category: TechniqueCategory, val weight: Int, val leftSide: Boolean) {
    JAB("Jab", TechniqueCategory.JAB, 1, true),
    CROSS("Cross", TechniqueCategory.CROSS, 1, false),
    LEFT_HOOK("Left Hook", TechniqueCategory.HOOK, 2, true),
    RIGHT_HOOK("Right Hook", TechniqueCategory.HOOK, 2, false),
    LEFT_UPPERCUT("Left Uppercut", TechniqueCategory.UPPERCUT, 2, true),
    RIGHT_UPPERCUT("Right Uppercut", TechniqueCategory.UPPERCUT, 2, false),
    TEEP("Teep", TechniqueCategory.TEEP, 2, true),
    REAR_TEEP("Rear Teep", TechniqueCategory.TEEP, 2, false),
    LEFT_ROUNDHOUSE("Left Roundhouse", TechniqueCategory.ROUNDHOUSE, 3, true),
    RIGHT_ROUNDHOUSE("Right Roundhouse", TechniqueCategory.ROUNDHOUSE, 3, false),
    LEFT_LOW_KICK("Left Low Kick", TechniqueCategory.LOW_KICK, 2, true),
    RIGHT_LOW_KICK("Right Low Kick", TechniqueCategory.LOW_KICK, 2, false),
    LEFT_BODY_KICK("Left Body Kick", TechniqueCategory.BODY_KICK, 3, true),
    RIGHT_BODY_KICK("Right Body Kick", TechniqueCategory.BODY_KICK, 3, false),
    LEFT_HIGH_KICK("Left High Kick", TechniqueCategory.HIGH_KICK, 4, true),
    RIGHT_HIGH_KICK("Right High Kick", TechniqueCategory.HIGH_KICK, 4, false),
    SWITCH_KICK("Switch Kick", TechniqueCategory.SWITCH_KICK, 4, true),
    LEFT_KNEE("Left Knee", TechniqueCategory.KNEE, 3, true),
    RIGHT_KNEE("Right Knee", TechniqueCategory.KNEE, 3, false),
    LEFT_ELBOW("Left Elbow", TechniqueCategory.ELBOW, 3, true),
    RIGHT_ELBOW("Right Elbow", TechniqueCategory.ELBOW, 3, false);

    val type: StrikeType get() = category.type
}

data class Combination(
    val id: Long = 0,
    val name: String,
    val strikes: List<Strike>,
    val favorite: Boolean = false,
    val reps: Int = 0,
    val custom: Boolean = true,
    /** How many repetition milestones have already paid out. */
    val milestonesClaimed: Int = 0,
    val createdAt: Long = 0
) {
    val notation: String get() = strikes.joinToString(" → ") { it.label }
}

// ---------------------------------------------------------------- labs

data class DefenseScenario(
    val id: Int,
    val attack: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ClinchOption(
    val label: String,
    /** 0 = punished, 1 = neutral, 2 = good, 3 = best. */
    val quality: Int,
    val outcome: String
)

data class ClinchScenario(
    val id: Int,
    val topic: String,
    val situation: String,
    val options: List<ClinchOption>
)

enum class DefenseResponse(val label: String) {
    CHECK("Check"), PARRY("Parry"), BLOCK("Block"), SLIP("Slip"),
    STEP_BACK("Step Back"), ANGLE_OUT("Angle Out"), CATCH("Catch"), FRAME_EXIT("Frame / Exit")
}

data class ReactionPrompt(
    val attack: String,
    val options: List<DefenseResponse>,
    val correct: DefenseResponse
)

enum class ReactionRank(val label: String) {
    BEGINNER("Beginner"), FAST("Fast"), SHARP("Sharp"), ELITE("Elite"), LIGHTNING("Lightning")
}

data class ProgramStep(val title: String, val instruction: String, val seconds: Int)

data class TrainingProgram(
    val id: String,
    val name: String,
    val focus: String,
    val level: String,
    val description: String,
    val steps: List<ProgramStep>,
    val gains: Map<Attribute, Int>,
    val xp: Int
) {
    val totalSeconds: Int get() = steps.sumOf { it.seconds }
}

// ---------------------------------------------------------------- fight lab

/** Tactical flavour of a decision. Used for opponent weaknesses and to pick the attributes that decide an exchange. */
enum class Tactic(val label: String) {
    LEGS("Leg attack"), BODY("Body attack"), HEAD("Head hunting"), CLINCH("Clinch"),
    DISTANCE("Distance management"), COUNTER("Counter striking"), DEFEND("Defense"),
    PRESSURE("Pressure"), ANGLE("Angles"), FINISH("Finish"), PACE("Pace control"), ELBOW("Elbows")
}

enum class Strategy(val label: String, val blurb: String, val tactic: Tactic) {
    PRESSURE("Pressure", "Walk them down and take away their space.", Tactic.PRESSURE),
    COUNTER("Counter", "Make them miss, make them pay.", Tactic.COUNTER),
    LONG_RANGE("Long Range", "Teeps and kicks from the outside.", Tactic.DISTANCE),
    LOW_KICK("Low Kick Focus", "Chop the lead leg until it stops working.", Tactic.LEGS),
    BODY_ATTACK("Body Attack", "Invest in the body to drain the gas tank.", Tactic.BODY),
    CLINCH("Clinch", "Tie up, control the neck, score with knees.", Tactic.CLINCH),
    DEFENSIVE("Defensive", "Tight guard, low risk, save energy.", Tactic.DEFEND),
    BALANCED("Balanced", "A bit of everything, no big gamble.", Tactic.PACE)
}

enum class OpponentStrategy(val label: String, val blurb: String) {
    PRESSURE("Pressure", "Coming forward behind a high guard."),
    COUNTER("Counter", "Waiting for you to commit first."),
    LONG_RANGE("Long range", "Holding you on the end of the teep."),
    CLINCH("Clinch", "Looking to tie up and knee."),
    LOW_KICK_HEAVY("Low kick heavy", "Targeting your lead leg."),
    HIGH_VOLUME("High volume", "Throwing in bunches to steal the round.")
}

enum class CornerInstruction(val label: String, val blurb: String) {
    INCREASE_PRESSURE("Increase pressure", "More forward movement, less respect."),
    TARGET_LEGS("Target legs", "Every exchange ends with a low kick."),
    TARGET_BODY("Target body", "Body kicks and knees to slow them down."),
    STAY_LONG("Stay long", "Teep, kick, and do not let them in."),
    COUNTER_MORE("Counter more", "Let them lead, answer everything."),
    CLINCH_MORE("Clinch more", "Close the gap and lock up."),
    DEFEND_RECOVER("Defend and recover", "Breathe, protect, get the legs back."),
    INCREASE_VOLUME("Increase volume", "Out-work them for the judges.")
}

enum class OpponentArchetype(val label: String, val traits: List<String>) {
    PRESSURE_FIGHTER("The Pressure Fighter", listOf("Constant forward movement", "High volume", "Strong low kicks", "Lower defense")),
    COUNTER_STRIKER("The Counter Striker", listOf("Patient", "High counter accuracy", "Dangerous when you attack recklessly")),
    KICK_SPECIALIST("The Kick Specialist", listOf("Strong roundhouse", "Strong teep", "Long range", "Weak inside")),
    CLINCHER("The Clincher", listOf("Excellent clinch", "Strong knees", "Weak at long range")),
    ELBOW_HUNTER("The Elbow Hunter", listOf("Dangerous at close range", "High elbow efficiency", "Weak against distance management")),
    COMPLETE_FIGHTER("The Complete Fighter", listOf("Balanced attributes", "No major weakness", "High difficulty"))
}

data class Opponent(
    val id: Int,
    val name: String,
    val nickname: String,
    val origin: String,
    val archetype: OpponentArchetype,
    /** 1..5 */
    val tier: Int,
    val speed: Int,
    val power: Int,
    val defense: Int,
    val stamina: Int,
    val fightIq: Int,
    val kicking: Int,
    val boxing: Int,
    val clinch: Int,
    val aggression: Int,
    val weaknesses: Set<Tactic>,
    val strongestWeapon: String,
    val bio: String
) {
    val overall: Int get() = (speed + power + defense + stamina + fightIq + kicking + boxing + clinch) / 8
    fun statList(): List<Pair<String, Int>> = listOf(
        "Speed" to speed, "Power" to power, "Defense" to defense, "Stamina" to stamina, "Fight IQ" to fightIq,
        "Kicking" to kicking, "Boxing" to boxing, "Clinch" to clinch, "Aggression" to aggression
    )
}

data class OpponentRecord(val opponentId: Int, val wins: Int, val losses: Int, val lastFought: Long)

enum class Condition {
    ANY, EARLY, LATE, USER_TIRED, OPP_TIRED, USER_LEG_HURT, OPP_LEG_HURT, OPP_BODY_HURT,
    USER_HURT, OPP_HURT, WINNING, LOSING, OPP_PRESSURE, OPP_COUNTER, OPP_LONG, OPP_CLINCH, OPP_LOW_KICK, OPP_VOLUME
}

data class FightOption(val label: String, val tactic: Tactic, /** 0 worst .. 3 best */ val quality: Int)

data class FightScenario(val id: Int, val text: String, val condition: Condition, val options: List<FightOption>)

/** The numbers the simulation uses for either corner. */
data class CombatStats(
    val speed: Int, val power: Int, val defense: Int, val stamina: Int, val fightIq: Int,
    val kicking: Int, val boxing: Int, val clinch: Int, val aggression: Int, val reaction: Int
)

data class RoundRecord(
    val number: Int,
    val userStrategy: Strategy,
    val opponentStrategy: OpponentStrategy,
    val userPoints: Double,
    val opponentPoints: Double,
    /** Three judges, each a pair (user, opponent). */
    val judges: List<Pair<Int, Int>>,
    val userLanded: Int,
    val userThrown: Int,
    val opponentLanded: Int,
    val opponentThrown: Int,
    val kicksLanded: Int,
    val kicksThrown: Int,
    val clinchWon: Int,
    val clinchTotal: Int,
    val userStaminaEnd: Int,
    val opponentStaminaEnd: Int,
    val matchup: Int,
    val decisionQuality: Int,
    val decisions: Int,
    val knockdownsFor: Int,
    val knockdownsAgainst: Int,
    val log: List<String>
) {
    val userWon: Boolean get() = userPoints > opponentPoints
}

data class EventOutcome(val success: Boolean, val text: String, val quality: Int, val knockdown: Boolean)

enum class FightPhase { PRE_ROUND, EVENT, EVENT_RESULT, CORNER, FINISHED }

data class Damage(val head: Int = 0, val body: Int = 0, val legs: Int = 0) {
    val total: Int get() = head + body + legs
}

data class FightState(
    val opponent: Opponent,
    val user: CombatStats,
    val opp: CombatStats,
    val phase: FightPhase,
    val round: Int,
    val totalRounds: Int,
    val userStamina: Int,
    val oppStamina: Int,
    val userDamage: Damage,
    val oppDamage: Damage,
    val opponentStrategy: OpponentStrategy,
    val userStrategy: Strategy?,
    val corner: CornerInstruction?,
    val pendingEvents: List<FightScenario>,
    val eventIndex: Int,
    val lastOutcome: EventOutcome?,
    val usedScenarioIds: Set<Int>,
    // accumulators for the round in progress
    val roundEventUser: Double,
    val roundEventOpp: Double,
    val roundQuality: Int,
    val roundDecisions: Int,
    val roundKdFor: Int,
    val roundKdAgainst: Int,
    val roundLog: List<String>,
    val rounds: List<RoundRecord>,
    val stoppage: Stoppage?,
    val seed: Long,
    val step: Int,
    val isCampFight: Boolean
) {
    val currentEvent: FightScenario? get() = pendingEvents.getOrNull(eventIndex)
}

data class Stoppage(val userWon: Boolean, val method: String, val round: Int)

data class CornerReport(
    val roundNumber: Int,
    val scoreLine: String,
    val userWonRound: Boolean,
    val successfulAttacks: Int,
    val opponentWeapon: String,
    val userWeapon: String,
    val stamina: Int,
    val opponentStamina: Int,
    val userDamage: Damage,
    val oppDamage: Damage,
    val recommendation: String,
    val suggested: CornerInstruction
)

data class FightResult(
    val id: Long = 0,
    val timestamp: Long,
    val opponentId: Int,
    val opponentName: String,
    val opponentTier: Int,
    val won: Boolean,
    val draw: Boolean,
    /** "Decision", "KO", "TKO (leg kicks)"... */
    val method: String,
    val endRound: Int,
    /** "49-46" x3, user score first. Empty for stoppages before any card matters. */
    val scorecards: List<String>,
    /** One of W / L / D per completed round. */
    val roundResults: List<String>,
    val striking: Int,
    val defense: Int,
    val kicking: Int,
    val clinch: Int,
    val fightIq: Int,
    val stamina: Int,
    val tactical: Int,
    val overall: Int,
    val xp: Int,
    val mainStrategy: Strategy,
    val campFight: Boolean
)

// ---------------------------------------------------------------- fight camp

enum class CampTraining(val label: String, val blurb: String, val attribute: Attribute?, val gain: Int, val fatigue: Int) {
    TECHNIQUE("Technique", "Sharp, fast pad rounds on clean mechanics.", Attribute.SPEED, 5, 9),
    CONDITIONING("Conditioning", "Roadwork and bag sprints.", Attribute.CONDITIONING, 4, 13),
    DEFENSE("Defense", "Check, parry and block drills.", Attribute.DEFENSE, 6, 8),
    KICKING("Kicking", "Hundreds of kicks on the heavy bag.", Attribute.KICKING, 5, 11),
    BOXING("Boxing", "Hands-only sparring rounds.", Attribute.STRIKING, 5, 10),
    CLINCH("Clinch", "Thirty minutes of neck wrestling.", Attribute.CLINCH, 5, 12),
    FIGHT_IQ("Fight IQ", "Film study of your opponent.", Attribute.FIGHT_IQ, 7, 3),
    RECOVERY("Recovery", "Massage, stretching, sleep.", null, 0, -22)
}

data class FightCamp(
    val active: Boolean = false,
    /** 1..7, day 7 is fight day. */
    val day: Int = 1,
    val energy: Int = 3,
    /** 0..100, how fresh the fighter is. Becomes starting stamina on fight day. */
    val freshness: Int = 80,
    val opponentId: Int = 0,
    val bonuses: Map<Attribute, Int> = emptyMap(),
    val log: List<String> = emptyList(),
    val boughtEnergyToday: Boolean = false,
    val trainedToday: List<CampTraining> = emptyList()
) {
    val isFightDay: Boolean get() = active && day >= 7
    val readiness: Int get() = (bonuses.values.sum() * 1.4 + freshness * 0.55).toInt().coerceIn(0, 100)
}

// ---------------------------------------------------------------- challenges, achievements, records

enum class Metric {
    COMBO_REPS, COMBO_SESSIONS, REACTION_PROMPTS, REACTION_SESSIONS, DEFENSE_ACCURACY, DEFENSE_SESSIONS,
    ROUNDS, FIGHT_WINS, FIGHTS, ROUND_WINS, LOW_KICK_REPS, TECHNIQUE_REPS, KICK_STRATEGY_WIN, COUNTER_STRATEGY_WIN,
    CLINCH_WINS, CLINCH_SESSIONS, SESSIONS, CATEGORIES, XP, IQ_SESSIONS, PROGRAMS, CAMP_DAYS, REACTION_ACCURACY,
    KNEE_ELBOW_REPS, PUNCH_REPS, TRAINING_DAYS, MINUTES
}

data class ChallengeTemplate(
    val id: String,
    val weekly: Boolean,
    val title: String,
    val metric: Metric,
    val target: Int,
    val xp: Int,
    val coins: Int,
    val rankPoints: Int
)

data class ChallengeRecord(val key: String, val templateId: String, val weekly: Boolean, val periodKey: String, val completedAt: Long)

data class ChallengeStatus(val template: ChallengeTemplate, val progress: Int, val completed: Boolean) {
    val fraction: Float get() = if (template.target <= 0) 1f else (progress.toFloat() / template.target).coerceIn(0f, 1f)
}

data class Achievement(val id: String, val title: String, val description: String, val xp: Int, val gold: Boolean = false)

enum class RecordKey(val label: String, val unit: String, val lowerIsBetter: Boolean = false) {
    FASTEST_REACTION("Fastest reaction", "ms", true),
    HIGHEST_FIGHT_IQ("Highest Fight IQ score", "", false),
    HIGHEST_DEFENSE_ACCURACY("Highest defense accuracy", "%", false),
    LONGEST_WIN_STREAK("Longest win streak", "", false),
    MOST_XP_DAY("Most XP in one day", "XP", false),
    MOST_COMBOS("Most combinations in a session", "reps", false),
    HIGHEST_FIGHT_SCORE("Highest fight score", "", false),
    LONGEST_TRAINING_STREAK("Most consecutive training days", "days", false),
    HIGHEST_ATTRIBUTE("Highest attribute", "", false),
    BEST_CAMP("Best Fight Camp result", "", false)
}

data class PersonalRecord(val key: RecordKey, val value: Int, val detail: String, val achievedAt: Long)

data class CoachTip(val title: String, val message: String, val destination: String, val priority: Int)

// ---------------------------------------------------------------- aggregate state

/** Everything persisted about the player, loaded as one immutable snapshot. */
data class GameState(
    val fighter: Fighter? = null,
    val attributeXp: Map<Attribute, Int> = emptyMap(),
    val sessions: List<TrainingSession> = emptyList(),
    val combinations: List<Combination> = emptyList(),
    val techniqueReps: Map<String, Int> = emptyMap(),
    val fights: List<FightResult> = emptyList(),
    val opponentRecords: Map<Int, OpponentRecord> = emptyMap(),
    val achievements: Map<String, Long> = emptyMap(),
    val challenges: List<ChallengeRecord> = emptyList(),
    val camp: FightCamp = FightCamp(),
    val records: Map<RecordKey, PersonalRecord> = emptyMap(),
    val loaded: Boolean = false
)

/** What a finished activity hands to the repository. */
data class SessionOutcome(
    val type: SessionType,
    val title: String,
    val durationSec: Int,
    val xp: Int,
    val performance: Int,
    val gains: Map<Attribute, Int>,
    val reps: Int = 0,
    val correct: Int = 0,
    val total: Int = 0,
    val tag: String = "",
    val success: Boolean = false,
    val valueMs: Int = 0
)

/** Everything the reward overlay shows after an activity. */
data class RewardSummary(
    val title: String,
    val xp: Int,
    val bonusXp: Int = 0,
    val coins: Int = 0,
    val rankPoints: Int = 0,
    val gains: Map<Attribute, Int> = emptyMap(),
    val attributeLevelUps: List<Pair<Attribute, Int>> = emptyList(),
    val levelBefore: Int = 1,
    val levelAfter: Int = 1,
    val rankBefore: Rank = Rank.NOVICE,
    val rankAfter: Rank = Rank.NOVICE,
    val achievements: List<Achievement> = emptyList(),
    val challenges: List<ChallengeTemplate> = emptyList(),
    val records: List<String> = emptyList(),
    val milestones: List<String> = emptyList(),
    val lines: List<String> = emptyList()
) {
    val totalXp: Int get() = xp + bonusXp
    val rankedUp: Boolean get() = rankAfter.ordinal > rankBefore.ordinal
    val leveledUp: Boolean get() = levelAfter > levelBefore
}
