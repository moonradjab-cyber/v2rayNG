package com.v2ray.ang.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF7C6CF5),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8A7CF7),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
)

// Happ-style dark purple palette
private val DarkColor = darkColorScheme(
    primary = Color(0xFF7C6CF5), // Purple accent
    onPrimary = Color(0xFF0B0A16), // Near-black purple
    primaryContainer = Color(0xFF332E57), // Deep purple
    onPrimaryContainer = Color(0xFFE7E5FF), // Pale lavender
    secondary = Color(0xFF8A7CF7), // Light purple
    onSecondary = Color(0xFF0B0A16),
    secondaryContainer = Color(0xFF332E57), // Purple pill
    onSecondaryContainer = Color(0xFFD3D0F5), // Lavender
    tertiary = Color(0xFF3DD68C), // Green
    onTertiary = Color(0xFF00120A),
    tertiaryContainer = Color(0xFF10362A),
    onTertiaryContainer = Color(0xFFA0F2D0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E0B1E), // Dark purple base
    onBackground = Color(0xFFECEAF6), // Near white
    surface = Color(0xFF0E0B1E),
    onSurface = Color(0xFFECEAF6),
    surfaceVariant = Color(0xFF221E3C), // Purple gray
    onSurfaceVariant = Color(0xFFA6A2C8), // Lavender gray (subtitles)
    outline = Color(0xFF3A3560),
    outlineVariant = Color(0xFF221E3C),
    inverseSurface = Color(0xFFECEAF6),
    inverseOnSurface = Color(0xFF0E0B1E),
    inversePrimary = Color(0xFF332E57),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF7C6CF5),
    surfaceContainerLowest = Color(0xFF08060F),
    surfaceContainerLow = Color(0xFF14112A),
    surfaceContainer = Color(0xFF1C1834), // Cards
    surfaceContainerHigh = Color(0xFF241F40),
    surfaceContainerHighest = Color(0xFF2C2748),
)

// Semantic Colors
val colorPing = Color(0xFF3DD68C) // Green
val colorPingRed = Color(0xFFFF6B9D) // Pink Red
val colorConfigType = Color(0xFF8A7CF7) // Light purple
val colorFabActive = Color(0xFF5A6BF0) // Blue-purple (connected)
val colorFabInactiveLight = Color(0xFF1C1834) // Dark purple (disconnected)
val colorFabInactiveDark = Color(0xFF1C1834) // Dark purple (disconnected)
val dividerColorLight = Color(0xFFE0E0E0)
val dividerColorDark = Color(0xFF2A2648) // Purple divider

// Toast Colors 70%
val toastNormalBgLight = Color(0xB3353A3E)
val toastNormalBgDark = Color(0xB34A4F54)
val toastSuccessBg = Color(0xB3388E3C)
val toastErrorBg = Color(0xB3D50000)
val toastInfoBg = Color(0xB35A4FCB)
val toastIconCircleBg = Color(0x33FFFFFF)
val toastTextColor = Color.White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force the custom dark purple scheme everywhere (Happ style).
    val colorScheme = DarkColor
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides true,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
