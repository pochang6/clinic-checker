package com.clinicchecker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clinicchecker.app.ui.*
import com.clinicchecker.app.ui.Typography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClinicCheckerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClinicCheckerApp()
                }
            }
        }
    }
}

@Composable
fun ClinicCheckerApp() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            val uiState by mainViewModel.uiState.collectAsState()
            
            Column(modifier = Modifier.fillMaxSize()) {
                MainScreen(
                    uiState = uiState,
                    onStartMonitoring = {
                        mainViewModel.startMonitoring()
                        mainViewModel.updateNextRefreshTime()
                    },
                    onStopMonitoring = { mainViewModel.stopMonitoring() },
                    onRefresh = { mainViewModel.refreshData() },
                    onSettingsClick = { navController.navigate("settings") },
                    onClearError = { mainViewModel.clearError() }
                )
                
                // Show ad banner if ads are not removed
                if (!uiState.adsRemoved) {
                    AdBanner()
                }
            }
        }
        
        composable("settings") {
            val uiState by settingsViewModel.uiState.collectAsState()
            
            SettingsScreen(
                uiState = uiState,
                onClinicIdChange = { settingsViewModel.updateClinicId(it) },
                onPasswordChange = { settingsViewModel.updatePassword(it) },
                onPollingIntervalChange = { settingsViewModel.updatePollingInterval(it) },
                onNotificationOffsetChange = { settingsViewModel.updateNotificationOffset(it) },
                onEnableVoiceChange = { settingsViewModel.updateEnableVoice(it) },
                onEnableVibrationChange = { settingsViewModel.updateEnableVibration(it) },
                onEnableSystemNotificationChange = { settingsViewModel.updateEnableSystemNotification(it) },
                onNotificationPolicyChange = { settingsViewModel.updateNotificationPolicy(it) },
                onDeveloperModeChange = { settingsViewModel.updateDeveloperMode(it) },
                onManualReservationNumberChange = { settingsViewModel.updateManualReservationNumber(it) },
                onMockHasReservationChange = { settingsViewModel.updateMockHasReservation(it) },
                onAdsRemovedChange = { settingsViewModel.updateAdsRemoved(it) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun ClinicCheckerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = dynamicColorScheme(),
        typography = Typography,
        content = content
    )
}

@Composable
fun dynamicColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        errorContainer = md_theme_light_errorContainer,
        onError = md_theme_light_onError,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inverseSurface = md_theme_light_inverseSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
        outlineVariant = md_theme_light_outlineVariant,
        scrim = md_theme_light_scrim,
    )
}

// Color definitions
private val md_theme_light_primary = Color(0xFF006C4C)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFF89F8C7)
private val md_theme_light_onPrimaryContainer = Color(0xFF002114)
private val md_theme_light_secondary = Color(0xFF4C6358)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFCEE9DA)
private val md_theme_light_onSecondaryContainer = Color(0xFF092017)
private val md_theme_light_tertiary = Color(0xFF3D6373)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFC1E8FB)
private val md_theme_light_onTertiaryContainer = Color(0xFF001F29)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_background = Color(0xFFFBFDF9)
private val md_theme_light_onBackground = Color(0xFF191C1A)
private val md_theme_light_surface = Color(0xFFFBFDF9)
private val md_theme_light_onSurface = Color(0xFF191C1A)
private val md_theme_light_surfaceVariant = Color(0xFFDBE5DE)
private val md_theme_light_onSurfaceVariant = Color(0xFF3F4943)
private val md_theme_light_outline = Color(0xFF6F7973)
private val md_theme_light_inverseOnSurface = Color(0xFFEFF1ED)
private val md_theme_light_inverseSurface = Color(0xFF2E312F)
private val md_theme_light_inversePrimary = Color(0xFF6CDBAC)
private val md_theme_light_surfaceTint = Color(0xFF006C4C)
private val md_theme_light_outlineVariant = Color(0xFFBFC9C2)
private val md_theme_light_scrim = Color(0xFF000000) 