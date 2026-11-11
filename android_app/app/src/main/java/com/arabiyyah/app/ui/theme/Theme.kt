package com.arabiyyah.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ThemePreferences {
    private const val PREFS_NAME = "arabiyyah_theme_prefs"
    private const val KEY_IS_DARK = "key_is_dark_mode"

    fun getInitialDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_IS_DARK)) {
            prefs.getBoolean(KEY_IS_DARK, false)
        } else {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            mode == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun saveDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply()
    }
}

val LocalThemeIsDark = compositionLocalOf { false }
val LocalThemeToggle = compositionLocalOf<() -> Unit> { {} }

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = NavyDark,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = NavyDark,
    secondaryContainer = NavyMedium,
    onSecondaryContainer = GoldLight,
    tertiary = GoldLight,
    onTertiary = NavyDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = GoldAccent.copy(alpha = 0.5f),
    outlineVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldAccent,
    onSecondary = NavyDark,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = EmeraldLight,
    onTertiary = SurfaceLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = EmeraldPrimary.copy(alpha = 0.4f),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun ArabiyyahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
