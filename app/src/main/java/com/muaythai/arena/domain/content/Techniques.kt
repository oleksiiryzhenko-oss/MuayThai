package com.muaythai.arena.domain.content

import com.muaythai.arena.domain.model.Strike
import com.muaythai.arena.domain.model.Strike.*
import com.muaythai.arena.domain.model.Technique
import com.muaythai.arena.domain.model.TechniqueCategory
import com.muaythai.arena.domain.model.TechniqueCategory as C

object Techniques {

    private fun t(
        id: String, c: TechniqueCategory, name: String, diff: Int, target: String, range: String,
        setup: String, mistake: String, cue: String
    ) = Technique(id, c, name, diff, target, range, setup, mistake, cue, xpReward = 18 + diff * 8, masteryReps = 200 + diff * 100)

    val all: List<Technique> = listOf(
        t("jab", C.JAB, "Jab", 1, "Head", "Long", "Feint the lead teep, then snap the jab.",
            "Dropping the rear hand as the jab goes out.", "Snap it out and back along the same line."),
        t("jab_body", C.JAB, "Body Jab", 2, "Solar plexus", "Long", "Show a high jab first to lift the guard.",
            "Bending at the waist instead of the knees.", "Level change with the legs, eyes up."),
        t("double_jab", C.JAB, "Double Jab", 2, "Head", "Long", "Step in on the second jab to close distance.",
            "Second jab is a push with no snap.", "First to blind, second to hit."),
        t("cross", C.CROSS, "Cross", 1, "Head", "Mid", "Jab to occupy the guard, cross down the middle.",
            "Rear heel stays flat, so the hip never turns.", "Pivot the back foot, chin behind the shoulder."),
        t("cross_body", C.CROSS, "Body Cross", 2, "Body", "Mid", "Jab high, then drop the cross under the elbow.",
            "Leaning forward with the chin exposed.", "Sit down on the punch, head off the centre line."),
        t("hook_lead", C.HOOK, "Lead Hook", 2, "Head", "Close", "Cross to turn their head into the hook.",
            "Looping wide and telegraphing with the elbow.", "Elbow up, turn the lead foot and hip together."),
        t("hook_body", C.HOOK, "Liver Hook", 3, "Liver", "Close", "Jab high so the right elbow lifts.",
            "Reaching with the arm instead of stepping left.", "Step out, dig up and under the ribs."),
        t("hook_rear", C.HOOK, "Rear Hook", 3, "Head", "Close", "Lead hook first to open the far side.",
            "Squaring the stance and losing balance.", "Short arc, stay over your feet."),
        t("uppercut_rear", C.UPPERCUT, "Rear Uppercut", 3, "Chin", "Close", "Jab to the body to drop their hands.",
            "Winding up by dropping the hand to the hip.", "Bend the knees, drive up through the centre."),
        t("uppercut_lead", C.UPPERCUT, "Lead Uppercut", 3, "Chin", "Close", "Slip outside the jab, come up underneath.",
            "Throwing it from too far away.", "Only when you can touch their chest."),
        t("teep_lead", C.TEEP, "Lead Teep", 1, "Stomach", "Long", "Use it as your jab every time they step in.",
            "Leaning back and pushing instead of stabbing.", "Knee up first, hips through, ball of the foot."),
        t("teep_rear", C.TEEP, "Rear Teep", 2, "Stomach / hip", "Long", "Fake the rear roundhouse to freeze the guard.",
            "Slow chamber that gets caught.", "Drive the hip like a spear, recover to stance."),
        t("teep_face", C.TEEP, "Face Teep", 4, "Face", "Long", "Body teeps first so the hands drop.",
            "Throwing it without the flexibility to recover.", "High knee, snap, hands stay up."),
        t("round_rear", C.ROUNDHOUSE, "Rear Roundhouse", 2, "Ribs / arm", "Mid", "Jab or cross so the arm lifts away from the ribs.",
            "Kicking with the knee instead of turning the hip over.", "Step out, swing the arm, shin through the target."),
        t("round_lead", C.ROUNDHOUSE, "Lead Roundhouse", 3, "Ribs", "Mid", "Small switch step hidden behind a jab.",
            "No hip rotation, so it lands as a slap.", "Switch, turn the standing foot, hip over."),
        t("low_outside", C.LOW_KICK, "Outside Low Kick", 2, "Lead thigh", "Mid", "Finish every punch combination with it.",
            "Kicking upward with the foot instead of chopping down.", "Angle out, chop down above the knee."),
        t("low_inside", C.LOW_KICK, "Inside Low Kick", 2, "Inner thigh", "Mid", "Throw it as they step forward with the lead leg.",
            "Standing square in front after the kick.", "Flick it fast and move off line."),
        t("low_calf", C.LOW_KICK, "Calf Kick", 3, "Calf", "Long", "Use it against heavy lead-leg stances.",
            "Hitting the shin bone when they turn the leg out.", "Low and fast, land below the knee."),
        t("body_rear", C.BODY_KICK, "Rear Body Kick", 2, "Liver / ribs", "Mid", "Feint the low kick, then go to the body.",
            "Dropping the guard-side hand.", "Kick through the body, not at it."),
        t("body_lead", C.BODY_KICK, "Lead Body Kick", 3, "Open side ribs", "Mid", "Against a southpaw, fire it straight after the jab.",
            "Kicking into the elbow.", "Aim just under the elbow, turn the hip fully."),
        t("high_rear", C.HIGH_KICK, "Rear High Kick", 4, "Neck / head", "Mid", "Hit the body twice, then same motion upstairs.",
            "Telegraphing by looking up and leaning back.", "Same start as the body kick, finish over the shoulder."),
        t("high_lead", C.HIGH_KICK, "Lead High Kick", 5, "Head", "Mid", "Cross to pull their guard across, lead leg behind it.",
            "No pivot, so the kick dies at shoulder height.", "Full pivot, hip over, hand guards the chin."),
        t("switch", C.SWITCH_KICK, "Switch Kick", 3, "Ribs / arm", "Mid", "Throw it immediately after your cross.",
            "A big jumping switch that gives it away.", "Tiny switch, almost on the spot, then explode."),
        t("switch_low", C.SWITCH_KICK, "Switch Low Kick", 3, "Inner thigh", "Mid", "Use when they shell up against punches.",
            "Switching too far forward into punching range.", "Switch back, not forward."),
        t("knee_straight", C.KNEE, "Straight Knee", 2, "Stomach", "Close", "Long guard to post on the shoulder, then knee.",
            "Leaning back so the hips never come through.", "Up on the toes, hips forward, pull them in."),
        t("knee_diag", C.KNEE, "Diagonal Knee", 3, "Ribs", "Clinch", "Turn them in the clinch to open the ribs.",
            "Kneeing with the thigh instead of the point.", "Swing the hip out then in."),
        t("knee_step", C.KNEE, "Step-in Knee", 3, "Solar plexus", "Mid", "Catch their guard with both hands as they cover.",
            "Coming in with the chin up.", "Hands frame the head, drive through."),
        t("elbow_horizontal", C.ELBOW, "Horizontal Elbow", 3, "Temple / brow", "Close", "Parry the jab down and step in.",
            "Throwing from punching range and missing.", "Step in first, cut across like a blade."),
        t("elbow_up", C.ELBOW, "Uppercut Elbow", 4, "Chin", "Close", "Split a tight high guard up the middle.",
            "Flaring the body open while lifting.", "Hand to the back of your own head, drive up."),
        t("elbow_spin", C.ELBOW, "Spinning Elbow", 5, "Temple", "Close", "Miss a lead hook on purpose and keep turning.",
            "Spinning without seeing the target.", "Head turns first, elbow follows.")
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Technique? = byId[id]
    fun inCategory(c: TechniqueCategory): List<Technique> = all.filter { it.category == c }

    /** Starter combinations seeded into the Combination Lab on first launch. */
    val starterCombinations: List<Pair<String, List<Strike>>> = listOf(
        "The Classic" to listOf(JAB, CROSS, LEFT_HOOK, RIGHT_LOW_KICK),
        "One-Two Chop" to listOf(JAB, CROSS, RIGHT_LOW_KICK),
        "Teep Entry" to listOf(TEEP, CROSS, LEFT_HOOK, RIGHT_ROUNDHOUSE),
        "Inside Trade" to listOf(JAB, RIGHT_ELBOW, LEFT_KNEE, RIGHT_LOW_KICK),
        "Dutch Opener" to listOf(JAB, CROSS, LEFT_HOOK, RIGHT_LOW_KICK, LEFT_HOOK),
        "Body Snatcher" to listOf(JAB, CROSS, LEFT_HOOK, RIGHT_BODY_KICK),
        "Switch Surprise" to listOf(JAB, CROSS, SWITCH_KICK),
        "Double Up" to listOf(JAB, JAB, CROSS, LEFT_BODY_KICK),
        "Liver Line" to listOf(CROSS, LEFT_HOOK, RIGHT_LOW_KICK),
        "Knee Finish" to listOf(JAB, CROSS, RIGHT_KNEE),
        "Elbow Split" to listOf(JAB, CROSS, LEFT_ELBOW, RIGHT_ELBOW),
        "High Low" to listOf(JAB, RIGHT_LOW_KICK, RIGHT_HIGH_KICK),
        "Teep Wall" to listOf(TEEP, REAR_TEEP, RIGHT_ROUNDHOUSE),
        "Southpaw Killer" to listOf(CROSS, LEFT_HOOK, RIGHT_BODY_KICK, RIGHT_BODY_KICK),
        "Uppercut Lift" to listOf(JAB, RIGHT_UPPERCUT, LEFT_HOOK, RIGHT_LOW_KICK),
        "Clinch Bridge" to listOf(JAB, CROSS, LEFT_KNEE, RIGHT_KNEE),
        "Counter Check" to listOf(CROSS, LEFT_HOOK, RIGHT_ROUNDHOUSE),
        "Inside Leg" to listOf(JAB, LEFT_LOW_KICK, CROSS, RIGHT_LOW_KICK),
        "Head Hunter" to listOf(JAB, CROSS, LEFT_HOOK, RIGHT_HIGH_KICK),
        "Four Corners" to listOf(JAB, CROSS, LEFT_BODY_KICK, RIGHT_LOW_KICK),
        "Pressure Train" to listOf(JAB, CROSS, LEFT_HOOK, CROSS, RIGHT_LOW_KICK),
        "Step-in Elbow" to listOf(TEEP, JAB, RIGHT_ELBOW),
        "Hook Kick Flow" to listOf(LEFT_HOOK, RIGHT_LOW_KICK, LEFT_HOOK, RIGHT_BODY_KICK),
        "Rear Guard" to listOf(REAR_TEEP, JAB, CROSS, LEFT_ROUNDHOUSE),
        "Body Body Head" to listOf(RIGHT_BODY_KICK, RIGHT_BODY_KICK, RIGHT_HIGH_KICK),
        "Short Range" to listOf(LEFT_UPPERCUT, RIGHT_UPPERCUT, LEFT_ELBOW, RIGHT_KNEE),
        "Stadium Special" to listOf(JAB, RIGHT_LOW_KICK, CROSS, LEFT_HOOK, SWITCH_KICK),
        "Angle Out" to listOf(JAB, CROSS, LEFT_HOOK, LEFT_LOW_KICK),
        "Check Counter" to listOf(CROSS, LEFT_HOOK, CROSS, RIGHT_ROUNDHOUSE),
        "Knee Storm" to listOf(RIGHT_KNEE, LEFT_KNEE, RIGHT_ELBOW),
        "Long Guard" to listOf(TEEP, JAB, CROSS, RIGHT_BODY_KICK),
        "The Closer" to listOf(JAB, CROSS, LEFT_HOOK, RIGHT_UPPERCUT, LEFT_HOOK, RIGHT_LOW_KICK)
    )
}
