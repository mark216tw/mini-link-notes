package com.vibecode.minilinknotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.vibecode.minilinknotes.data.AppSettings
import com.vibecode.minilinknotes.data.DisplayMode

@Composable
fun MiniLinkNotesTheme(
    settings: AppSettings,
    content: @Composable (darkTheme: Boolean) -> Unit,
) {
    val dark = when (settings.displayMode) {
        DisplayMode.LIGHT -> false
        DisplayMode.DARK -> true
        DisplayMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        settings.dynamicColor && supportsDynamic -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        dark -> darkColorScheme(
            primary = TealPrimaryDark,
            onPrimary = DarkOnPrimary,
            primaryContainer = DarkPrimaryContainer,
            onPrimaryContainer = DarkOnPrimaryContainer,
            secondary = DarkSecondary,
            background = DarkBackground,
            surface = DarkBackground,
            surfaceVariant = DarkSurfaceVariant,
        )

        else -> lightColorScheme(
            primary = TealPrimary,
            onPrimary = Color.White,
            primaryContainer = LightPrimaryContainer,
            onPrimaryContainer = LightOnPrimaryContainer,
            secondary = LightSecondary,
            background = LightBackground,
            surface = LightBackground,
            surfaceVariant = LightSurfaceVariant,
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        content(dark)
    }
}
