package com.muaythai.arena.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import com.muaythai.arena.ui.theme.ArenaColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.screens.AboutScreen
import com.muaythai.arena.ui.screens.AchievementsScreen
import com.muaythai.arena.ui.screens.AttributeScreen
import com.muaythai.arena.ui.screens.CampScreen
import com.muaythai.arena.ui.screens.ChallengesScreen
import com.muaythai.arena.ui.screens.ClinchScreen
import com.muaythai.arena.ui.screens.ClubScreen
import com.muaythai.arena.ui.screens.EventScreen
import com.muaythai.arena.ui.screens.ModEventEditScreen
import com.muaythai.arena.ui.screens.ModEventScreen
import com.muaythai.arena.ui.screens.ModEventsScreen
import com.muaythai.arena.ui.screens.ModNoticesScreen
import com.muaythai.arena.ui.screens.ModRequestsScreen
import com.muaythai.arena.ui.screens.ModSurveyNewScreen
import com.muaythai.arena.ui.screens.ModSurveyScreen
import com.muaythai.arena.ui.screens.ModSurveysScreen
import com.muaythai.arena.ui.screens.ModeratorScreen
import com.muaythai.arena.ui.screens.NoticesScreen
import com.muaythai.arena.ui.screens.RequestsScreen
import com.muaythai.arena.ui.screens.ScheduleScreen
import com.muaythai.arena.ui.screens.SurveyScreen
import com.muaythai.arena.ui.screens.SurveysScreen
import com.muaythai.arena.ui.screens.CoachScreen
import com.muaythai.arena.ui.screens.ComboBuilderScreen
import com.muaythai.arena.ui.screens.ComboPracticeScreen
import com.muaythai.arena.ui.screens.CombosScreen
import com.muaythai.arena.ui.screens.DefenseScreen
import com.muaythai.arena.ui.screens.FightLabScreen
import com.muaythai.arena.ui.screens.FightScreen
import com.muaythai.arena.ui.screens.HistoryScreen
import com.muaythai.arena.ui.screens.HomeScreen
import com.muaythai.arena.ui.screens.OpponentScreen
import com.muaythai.arena.ui.screens.PrivacyScreen
import com.muaythai.arena.ui.screens.ProfileScreen
import com.muaythai.arena.ui.screens.ProgramRunScreen
import com.muaythai.arena.ui.screens.ProgramsScreen
import com.muaythai.arena.ui.screens.ProgressScreen
import com.muaythai.arena.ui.screens.ReactionScreen
import com.muaythai.arena.ui.screens.RecordsScreen
import com.muaythai.arena.ui.screens.RoundScreen
import com.muaythai.arena.ui.screens.SettingsScreen
import com.muaythai.arena.ui.screens.StrikingScreen
import com.muaythai.arena.ui.screens.TechniqueScreen
import com.muaythai.arena.ui.screens.TrainScreen

object Routes {
    const val HOME = "home"
    const val TRAIN = "train"
    const val FIGHT_LAB = "fightlab"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    const val STRIKING = "striking"
    const val COMBOS = "combos"
    const val DEFENSE = "defense"
    const val CLINCH = "clinch"
    const val REACTION = "reaction"
    const val ROUND = "round"
    const val PROGRAMS = "programs"
    const val CAMP = "camp"
    const val ACHIEVEMENTS = "achievements"
    const val RECORDS = "records"
    const val HISTORY = "history"
    const val CHALLENGES = "challenges"
    const val COACH = "coach"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"

    const val CLUB = "club"
    const val SCHEDULE = "schedule"
    const val SURVEYS = "surveys"
    const val NOTICES = "notices"
    const val REQUESTS = "requests"
    const val MODERATOR = "moderator"
    const val MOD_EVENTS = "mod_events"
    const val MOD_SURVEYS = "mod_surveys"
    const val MOD_SURVEY_NEW = "mod_survey_new"
    const val MOD_NOTICES = "mod_notices"
    const val MOD_REQUESTS = "mod_requests"

    fun technique(id: String) = "technique/$id"
    fun comboEdit(id: Long) = "combo_edit/$id"
    fun comboPractice(id: Long) = "combo_practice/$id"
    fun program(id: String) = "program/$id"
    fun opponent(id: Int) = "opponent/$id"
    fun fight(id: Int, camp: Boolean) = "fight/$id/${if (camp) 1 else 0}"
    fun attribute(name: String) = "attribute/$name"
    fun event(id: Long) = "event/$id"
    fun survey(id: Long, guest: Boolean) = "survey/$id/${if (guest) 1 else 0}"
    fun modEvent(id: Long) = "mod_event/$id"
    fun modEventEdit(id: Long) = "mod_event_edit/$id"
    fun modSurvey(id: Long) = "mod_survey/$id"

    val tabs = listOf(HOME, TRAIN, FIGHT_LAB, PROGRESS, PROFILE)
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.TRAIN, "Train", Icons.Filled.Build),
    Tab(Routes.FIGHT_LAB, "Fight Lab", Icons.Filled.Star),
    Tab(Routes.PROGRESS, "Progress", Icons.Filled.DateRange),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun ArenaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val feedback = LocalFeedback.current

    // Tabs switch like tabs; everything else is pushed on top of the current tab.
    val go: (String) -> Unit = { route ->
        if (route in Routes.tabs) navController.openTab(route) else navController.navigate(route) { launchSingleTop = true }
    }
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in Routes.tabs) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    for (tab in tabs) {
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { feedback.tap(); navController.openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label.uppercase(), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Paint the strip behind the status bar red so every screen's red header runs to the top edge.
        val statusBar = padding.calculateTopPadding()
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .drawBehind { drawRect(ArenaColors.Crimson, size = Size(size.width, statusBar.toPx())) }
                .padding(padding).consumeWindowInsets(padding).imePadding()
        ) {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) { HomeScreen(go) }
                composable(Routes.TRAIN) { TrainScreen(go) }
                composable(Routes.FIGHT_LAB) { FightLabScreen(go) }
                composable(Routes.PROGRESS) { ProgressScreen(go) }
                composable(Routes.PROFILE) { ProfileScreen(go) }

                composable(Routes.STRIKING) { StrikingScreen(go, back) }
                composable("technique/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    TechniqueScreen(entry.arguments?.getString("id").orEmpty(), back)
                }
                composable(Routes.COMBOS) { CombosScreen(go, back) }
                composable("combo_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    ComboBuilderScreen(entry.arguments?.getLong("id") ?: 0L, back)
                }
                composable("combo_practice/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    ComboPracticeScreen(entry.arguments?.getLong("id") ?: 0L, back)
                }
                composable(Routes.DEFENSE) { DefenseScreen(back) }
                composable(Routes.CLINCH) { ClinchScreen(back) }
                composable(Routes.REACTION) { ReactionScreen(back) }
                composable(Routes.ROUND) { RoundScreen(back) }
                composable(Routes.PROGRAMS) { ProgramsScreen(go, back) }
                composable("program/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    ProgramRunScreen(entry.arguments?.getString("id").orEmpty(), back)
                }

                composable("opponent/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    OpponentScreen(entry.arguments?.getInt("id") ?: 1, go, back)
                }
                composable(
                    "fight/{id}/{camp}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("camp") { type = NavType.IntType })
                ) { entry ->
                    FightScreen(entry.arguments?.getInt("id") ?: 1, (entry.arguments?.getInt("camp") ?: 0) == 1, back)
                }
                composable(Routes.CAMP) { CampScreen(go, back) }

                composable("attribute/{name}", arguments = listOf(navArgument("name") { type = NavType.StringType })) { entry ->
                    AttributeScreen(entry.arguments?.getString("name").orEmpty(), go, back)
                }
                composable(Routes.ACHIEVEMENTS) { AchievementsScreen(back) }
                composable(Routes.RECORDS) { RecordsScreen(back) }
                composable(Routes.HISTORY) { HistoryScreen(back) }
                composable(Routes.CHALLENGES) { ChallengesScreen(back) }
                composable(Routes.COACH) { CoachScreen(go, back) }
                composable(Routes.SETTINGS) { SettingsScreen(go, back) }
                composable(Routes.PRIVACY) { PrivacyScreen(back) }
                composable(Routes.ABOUT) { AboutScreen(back) }

                // ---- club: member side
                composable(Routes.CLUB) { ClubScreen(go, back) }
                composable(Routes.SCHEDULE) { ScheduleScreen(go, back) }
                composable("event/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    EventScreen(entry.arguments?.getLong("id") ?: 0L, back)
                }
                composable(Routes.SURVEYS) { SurveysScreen(go, back) }
                composable(
                    "survey/{id}/{guest}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("guest") { type = NavType.IntType })
                ) { entry ->
                    SurveyScreen(entry.arguments?.getLong("id") ?: 0L, (entry.arguments?.getInt("guest") ?: 0) == 1, back)
                }
                composable(Routes.NOTICES) { NoticesScreen(back) }
                composable(Routes.REQUESTS) { RequestsScreen(back) }

                // ---- club: moderator side
                composable(Routes.MODERATOR) { ModeratorScreen(go, back) }
                composable(Routes.MOD_EVENTS) { ModEventsScreen(go, back) }
                composable("mod_event/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    ModEventScreen(entry.arguments?.getLong("id") ?: 0L, go, back)
                }
                composable("mod_event_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    ModEventEditScreen(entry.arguments?.getLong("id") ?: 0L, back)
                }
                composable(Routes.MOD_SURVEYS) { ModSurveysScreen(go, back) }
                composable(Routes.MOD_SURVEY_NEW) { ModSurveyNewScreen(back) }
                composable("mod_survey/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    ModSurveyScreen(entry.arguments?.getLong("id") ?: 0L, go, back)
                }
                composable(Routes.MOD_NOTICES) { ModNoticesScreen(back) }
                composable(Routes.MOD_REQUESTS) { ModRequestsScreen(back) }
            }
        }
    }
}
