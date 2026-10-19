package com.muaythai.arena

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muaythai.arena.domain.model.Rank
import com.muaythai.arena.ui.LocalFeedback
import com.muaythai.arena.ui.components.RankBadge
import com.muaythai.arena.ui.components.onRedColorScheme
import com.muaythai.arena.ui.theme.ArenaColors
import com.muaythai.arena.ui.nav.ArenaApp
import com.muaythai.arena.ui.screens.OnboardingScreen
import com.muaythai.arena.ui.theme.ArenaTheme
import com.muaythai.arena.ui.vm.AppViewModel
import com.muaythai.arena.ui.vm.ArenaViewModelFactory
import com.muaythai.arena.ui.vm.LocalViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MuayThaiApp).container

        setContent {
            val factory = remember { ArenaViewModelFactory(container) }
            val appViewModel: AppViewModel = viewModel(factory = factory)
            val ui by appViewModel.ui.collectAsStateWithLifecycle()
            val dark = ui.settings.darkMode

            // The status bar always sits on red, so its icons are always light. The navigation bar follows the theme.
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                )
            }

            ArenaTheme(dark = dark) {
                CompositionLocalProvider(
                    LocalFeedback provides container.feedback,
                    LocalViewModelFactory provides factory
                ) {
                    when {
                        !ui.ready -> Box(
                            Modifier.fillMaxSize().background(ArenaColors.Crimson),
                            contentAlignment = Alignment.Center
                        ) {
                            MaterialTheme(colorScheme = onRedColorScheme()) { RankBadge(Rank.NOVICE) }
                        }
                        !ui.onboarded -> OnboardingScreen(onFinish = appViewModel::completeOnboarding)
                        else -> ArenaApp()
                    }
                }
            }
        }
    }
}
