package com.muaythai.arena.domain.content

import com.muaythai.arena.domain.model.Attribute
import com.muaythai.arena.domain.model.Attribute.*
import com.muaythai.arena.domain.model.ProgramStep
import com.muaythai.arena.domain.model.TrainingProgram

object Programs {
    private fun s(title: String, instruction: String, seconds: Int) = ProgramStep(title, instruction, seconds)

    private fun p(
        id: String, name: String, focus: String, level: String, description: String,
        gains: Map<Attribute, Int>, vararg steps: ProgramStep
    ): TrainingProgram {
        val list = steps.toList()
        return TrainingProgram(id, name, focus, level, description, list, gains, xp = 60 + list.sumOf { it.seconds } / 6)
    }

    val all: List<TrainingProgram> = listOf(
        p("foundations", "Stance & Rhythm", "Fundamentals", "Novice",
            "The Muay Thai stance, the bounce and the basic guard.", mapOf(STRIKING to 10, DEFENSE to 10, SPEED to 6),
            s("March in stance", "Light bounce on the lead foot, hands at eyebrow level, elbows in.", 60),
            s("Jab - Cross", "Slow and long. Rear heel turns on every cross.", 90),
            s("Lead teep", "Knee up, hip through. Return to stance every time.", 90),
            s("Check and return", "Lift the lead shin to check, land, answer with a cross.", 60)),
        p("hands101", "Thai Hands", "Boxing", "Beginner",
            "Tight punching that leaves you ready to kick or check.", mapOf(STRIKING to 16, SPEED to 8),
            s("Jab ladder", "1 jab, 2 jabs, 3 jabs, reset. Snap every one.", 60),
            s("1-2-3", "Jab, cross, lead hook. Chin behind the shoulder.", 120),
            s("Body-head", "Jab high, cross to the body, hook to the head.", 120),
            s("Burnout", "Straight punches non-stop at 70% power.", 45)),
        p("kicks101", "Shin Builder", "Kicking", "Beginner",
            "Volume kicking to build the hip turn and the conditioning behind it.", mapOf(KICKING to 18, CONDITIONING to 8),
            s("10 x rear roundhouse", "Full hip turn. Arm swings down, other hand guards.", 90),
            s("10 x lead roundhouse", "Small switch, turn the standing foot.", 90),
            s("Low kick chop", "Step out at 45 degrees, chop down above the knee.", 90),
            s("Kick sprints", "Alternate kicks as fast as you can hold form.", 60)),
        p("teepgame", "The Long Weapon", "Teep", "Intermediate",
            "Own the range with the teep as jab, shield and attack.", mapOf(KICKING to 12, DEFENSE to 8, FIGHT_IQ to 8),
            s("Lead teep on entry", "Imagine a partner stepping in. Stop them every time.", 90),
            s("Teep - cross", "Teep to set the distance, cross as the foot lands.", 90),
            s("Rear teep spear", "Fake the rear kick, turn it into a teep.", 90),
            s("Teep and angle", "Teep, land, step off to the outside.", 60)),
        p("lowkick", "Leg Destroyer", "Low kick", "Intermediate",
            "Set up and land the low kick without eating the counter.", mapOf(KICKING to 14, POWER to 10),
            s("Jab - low kick", "Jab to blind, kick as the hand returns.", 90),
            s("Cross - hook - low kick", "Turn them onto the lead leg with the hook, then chop.", 120),
            s("Inside then outside", "Inside low kick, plant, outside low kick.", 90),
            s("Check drill", "After each kick, lift the shin to check the return.", 60)),
        p("defense", "Iron Guard", "Defense", "Beginner",
            "The four core defenses: check, parry, block, lean.", mapOf(DEFENSE to 20, REACTION to 8),
            s("Check walk", "Walk forward lifting each shin to the elbow.", 60),
            s("Parry - cross", "Small parry on the jab, straight answer.", 90),
            s("High block - hook", "Long block against the kick, counter hook.", 90),
            s("Lean back - kick", "Lean away from the high kick, return a roundhouse.", 90)),
        p("counter", "Make Them Pay", "Counter striking", "Advanced",
            "Timing drills for fighters who win by answering.", mapOf(REACTION to 14, FIGHT_IQ to 12, DEFENSE to 8),
            s("Catch and sweep", "Catch the body kick, step through, dump.", 90),
            s("Check - cross - hook", "Check, set the foot down, fire two punches.", 120),
            s("Slip - body hook", "Slip outside the cross, liver hook.", 90),
            s("Teep the kicker", "Teep the standing leg hip as they chamber.", 90)),
        p("clinch101", "Neck Wrestling", "Clinch", "Intermediate",
            "Grips, posture and the first knees.", mapOf(CLINCH to 20, POWER to 6, CONDITIONING to 6),
            s("Pummel for inside", "Swim each hand to the inside position.", 90),
            s("Double collar posture", "Elbows tight, forearms on the collarbones, head high.", 60),
            s("Straight knees", "Hips in, pull down as the knee rises.", 120),
            s("Turn and knee", "Step back, pull them around, knee as they land.", 90)),
        p("elbows", "Close Quarters", "Elbows", "Advanced",
            "Short weapons for when the fight gets tight.", mapOf(STRIKING to 12, POWER to 10, CLINCH to 6),
            s("Horizontal elbow", "Step in first. Cut across at eyebrow height.", 90),
            s("Parry - elbow", "Parry the jab down, same-side elbow over the top.", 90),
            s("Uppercut elbow", "Split the guard straight up the middle.", 90),
            s("Elbow - knee", "Elbow high, grab, knee low.", 90)),
        p("engine", "Five Round Engine", "Conditioning", "Intermediate",
            "Interval work that mirrors the pace of a fight.", mapOf(CONDITIONING to 18, STAMINA to 16),
            s("Round pace", "Steady shadowboxing at fight pace.", 180),
            s("Sprint", "Knees on the bag or in the air, maximum pace.", 30),
            s("Active rest", "Walk, breathe through the nose, hands up.", 45),
            s("Sprint", "Fast kicks, alternating legs.", 30),
            s("Round pace", "Back to controlled work. Stay long.", 120)),
        p("speed", "Fast Hands, Fast Shins", "Speed", "Intermediate",
            "Short, sharp bursts to make the first strike land first.", mapOf(SPEED to 18, REACTION to 8),
            s("Jab bursts", "Ten jabs as fast as possible. Rest ten seconds. Repeat.", 60),
            s("Switch kick bursts", "Five fast switch kicks, reset.", 60),
            s("1-2 explosions", "From stillness to full speed with no tell.", 60),
            s("Shadow at 120%", "Everything faster than feels comfortable.", 60)),
        p("power", "Heavy Leather", "Power", "Advanced",
            "Fewer strikes, full commitment, perfect mechanics.", mapOf(POWER to 18, STRIKING to 8),
            s("Single cross", "One at a time. Full hip, full weight.", 90),
            s("Single rear kick", "Step, turn, kick through the bag.", 90),
            s("Knee drive", "Grab, pull, drive the knee through.", 90),
            s("Finishing combo", "Cross, hook, rear low kick at 100%.", 60)),
        p("ringcraft", "Ringcraft", "Fight IQ", "Advanced",
            "Cutting off the ring, escaping the ropes and stealing rounds.", mapOf(FIGHT_IQ to 20, DEFENSE to 8),
            s("Cut the ring", "Step across, not after. Mirror an imaginary opponent.", 90),
            s("Off the ropes", "Back to the ropes, pivot out behind a hook.", 90),
            s("Last thirty", "Flurry in the final thirty seconds of a round.", 30),
            s("Score and move", "One clean kick, then angle away. Repeat.", 120)),
        p("southpaw", "Open Stance Tactics", "Strategy", "Advanced",
            "Weapons and footwork against the opposite stance.", mapOf(FIGHT_IQ to 12, KICKING to 12),
            s("Outside foot", "Step your lead foot outside theirs before every attack.", 90),
            s("Rear body kick", "Into the open side, again and again.", 120),
            s("Rear straight", "Down the middle past their lead hand.", 90),
            s("Check the open kick", "Rear-leg check, answer immediately.", 60)),
        p("recovery", "Active Recovery", "Recovery", "All levels",
            "Loosen the hips and shoulders and get ready for tomorrow.", mapOf(STAMINA to 10, CONDITIONING to 4),
            s("Hip circles", "Slow, wide circles in both directions.", 60),
            s("Shadow at 30%", "Flow through every weapon with no power.", 120),
            s("Kick swings", "Loose leg swings, front and side.", 60),
            s("Breathing", "In for four, hold four, out for six.", 60)),
        p("fightweek", "Fight Week Sharpener", "Peaking", "Advanced",
            "Short, crisp and confident. Nothing heavy.", mapOf(SPEED to 8, FIGHT_IQ to 8, REACTION to 8, STAMINA to 6),
            s("Sharp round", "One round, only your three best weapons.", 180),
            s("Visualise", "Close your eyes. See the walkout, the first exchange, the hand raised.", 60),
            s("Counter round", "Only counters. Wait, answer, move.", 120),
            s("Cool down", "Light movement and stretching.", 60))
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): TrainingProgram? = byId[id]
}
