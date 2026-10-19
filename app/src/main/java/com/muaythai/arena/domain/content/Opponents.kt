package com.muaythai.arena.domain.content

import com.muaythai.arena.domain.model.Opponent
import com.muaythai.arena.domain.model.OpponentArchetype
import com.muaythai.arena.domain.model.OpponentArchetype.*
import com.muaythai.arena.domain.model.Tactic

/** Thirty fictional opponents across five tiers. All names, gyms and nicknames are invented. */
object Opponents {

    private data class Profile(
        val speed: Int, val power: Int, val defense: Int, val stamina: Int, val iq: Int,
        val kicking: Int, val boxing: Int, val clinch: Int, val aggression: Int, val weak: Set<Tactic>, val weapon: String
    )

    private val profiles = mapOf(
        PRESSURE_FIGHTER to Profile(0, 6, -8, 6, -2, 4, 5, 0, 82, setOf(Tactic.COUNTER, Tactic.ANGLE), "Low kicks behind constant pressure"),
        COUNTER_STRIKER to Profile(6, 2, 8, 0, 8, 0, 4, -4, 34, setOf(Tactic.LEGS, Tactic.PACE), "Counter cross off the back foot"),
        KICK_SPECIALIST to Profile(4, 4, 2, 0, 2, 12, -6, -10, 55, setOf(Tactic.CLINCH, Tactic.PRESSURE, Tactic.ELBOW), "Rear roundhouse and long teep"),
        CLINCHER to Profile(-4, 6, 0, 6, 0, -8, -2, 14, 66, setOf(Tactic.DISTANCE, Tactic.LEGS, Tactic.ANGLE), "Knees from the double collar tie"),
        ELBOW_HUNTER to Profile(2, 8, -2, 0, 2, -4, 8, 6, 72, setOf(Tactic.DISTANCE, Tactic.LEGS), "Slashing elbows at close range"),
        COMPLETE_FIGHTER to Profile(3, 3, 3, 3, 4, 3, 3, 3, 60, emptySet(), "Whatever you are weakest against")
    )

    private fun o(
        id: Int, name: String, nickname: String, origin: String, archetype: OpponentArchetype, tier: Int, bio: String
    ): Opponent {
        val p = profiles.getValue(archetype)
        val base = 32 + tier * 10
        // Small, stable per-fighter variation so no two opponents share a stat line.
        fun v(salt: Int): Int = ((id * 31 + salt * 17) % 7) - 3
        fun stat(offset: Int, salt: Int) = (base + offset + v(salt)).coerceIn(20, 99)
        return Opponent(
            id = id, name = name, nickname = nickname, origin = origin, archetype = archetype, tier = tier,
            speed = stat(p.speed, 1), power = stat(p.power, 2), defense = stat(p.defense, 3), stamina = stat(p.stamina, 4),
            fightIq = stat(p.iq, 5), kicking = stat(p.kicking, 6), boxing = stat(p.boxing, 7), clinch = stat(p.clinch, 8),
            aggression = (p.aggression + v(9) * 2).coerceIn(10, 99),
            weaknesses = p.weak, strongestWeapon = p.weapon, bio = bio
        )
    }

    val all: List<Opponent> = listOf(
        o(1, "Dao Ruangsiri", "Little Bull", "Red Lotus Gym", PRESSURE_FIGHTER, 1, "Walks forward from the first bell. Tough, but easy to hit."),
        o(2, "Milo Hartwell", "The Clerk", "Northgate Fight Club", COUNTER_STRIKER, 1, "Careful and tidy. Waits all night for one mistake."),
        o(3, "Kanok Preedee", "Long Shin", "Riverbank Camp", KICK_SPECIALIST, 1, "All legs. Gets uncomfortable when you stand on his toes."),
        o(4, "Bruno Tavares", "The Vine", "Costa Verde Muay Thai", CLINCHER, 1, "Grabs everything. Slow feet on the outside."),
        o(5, "Yusuf Demirel", "Razor", "Old Port Gym", ELBOW_HUNTER, 1, "Throws elbows from too far away, but the ones that land cut."),
        o(6, "Tomas Ilves", "The Tractor", "Nordhavn Fighters", PRESSURE_FIGHTER, 1, "One gear: forward. Fades when the pace is controlled."),
        o(7, "Arthit Wongsa", "Thunder Leg", "Stone Tiger Camp", KICK_SPECIALIST, 2, "A sharp left kick that scores every time you stand still."),
        o(8, "Elias Brandt", "Mirror", "Eisenwerk Kampfsport", COUNTER_STRIKER, 2, "Copies your rhythm, then breaks it."),
        o(9, "Somsak Ketkaew", "The Anchor", "Rope & Iron Gym", CLINCHER, 2, "Once he has your neck you carry his weight for the round."),
        o(10, "Viktor Hrabal", "Bulldozer", "Praha Combat Hall", PRESSURE_FIGHTER, 2, "Heavy low kicks, heavier hands, no reverse."),
        o(11, "Reza Kamali", "The Tailor", "Silk Road Fight Team", ELBOW_HUNTER, 2, "Measures you with the jab and stitches you with elbows."),
        o(12, "Nattapong Srisuk", "Whip", "Golden Bell Camp", KICK_SPECIALIST, 2, "Fast switch kicks and a teep that never stops."),
        o(13, "Julien Marchetti", "Le Fantome", "Atelier Boxe Thai", COUNTER_STRIKER, 3, "You hit air, he hits you. Hates having his legs kicked."),
        o(14, "Pichit Charoenrat", "Python", "Black Elephant Gym", CLINCHER, 3, "Squeezes the fight out of people. Avoid the ropes."),
        o(15, "Kenji Aramaki", "The Scholar", "Kita Dojo", COMPLETE_FIGHTER, 3, "Does everything well and nothing recklessly."),
        o(16, "Oleksa Bondar", "Iron Wave", "Dnipro Fight Academy", PRESSURE_FIGHTER, 3, "Volume that buries people in rounds three and four."),
        o(17, "Chatri Phumiphat", "The Sickle", "Nine Ropes Camp", ELBOW_HUNTER, 3, "Closes distance behind a high guard and cuts."),
        o(18, "Lucas Ferreyra", "El Latigo", "Sur Muay Thai", KICK_SPECIALIST, 3, "Southpaw body kicks that turn arms purple."),
        o(19, "Thanakorn Yodrak", "The Vice", "Black Elephant Gym", CLINCHER, 4, "The best neck in his division. Never lets go first."),
        o(20, "Aleksander Nowik", "Cold Read", "Baltic Strikers", COUNTER_STRIKER, 4, "Has an answer ready for every weapon you own."),
        o(21, "Sakda Meechai", "The Professor", "Golden Bell Camp", COMPLETE_FIGHTER, 4, "Adjusts mid-round. What worked once will not work twice."),
        o(22, "Dragan Petrovic", "The Furnace", "Balkan Thai Team", PRESSURE_FIGHTER, 4, "Relentless. Makes strong fighters look for the exit."),
        o(23, "Wichai Kongsak", "Sky Kick", "Stone Tiger Camp", KICK_SPECIALIST, 4, "Head kicks from nowhere and a teep like a battering ram."),
        o(24, "Idris Okonkwo", "The Surgeon", "Lagos Elite Muay Thai", ELBOW_HUNTER, 4, "Precise, patient and merciless inside."),
        o(25, "Rattana Sitthichai", "The Emperor", "Nine Ropes Camp", COMPLETE_FIGHTER, 5, "A stadium veteran with no visible weakness."),
        o(26, "Mikael Lindqvist", "Ghost Hand", "Nordhavn Fighters", COUNTER_STRIKER, 5, "Elite timing. Reckless entries end the night early."),
        o(27, "Prasert Kaewmanee", "Iron Embrace", "Rope & Iron Gym", CLINCHER, 5, "Wins rounds without throwing a punch."),
        o(28, "Darius Vance", "Guillotine", "Harbor City Muay Thai", KICK_SPECIALIST, 5, "The hardest kicker on the roster. Do not block with the arm."),
        o(29, "Santiago Quiroga", "El Martillo", "Sur Muay Thai", PRESSURE_FIGHTER, 5, "Five rounds of forward violence at a pace nobody matches."),
        o(30, "Anan Thongchai", "The Lion of the Arena", "Red Lotus Gym", COMPLETE_FIGHTER, 5, "The champion. Beat him and there is nobody left.")
    )

    private val byId = all.associateBy { it.id }
    fun get(id: Int): Opponent? = byId[id]
    fun inTier(tier: Int): List<Opponent> = all.filter { it.tier == tier }
    const val MAX_TIER = 5
    const val CHAMPION_ID = 30
}
