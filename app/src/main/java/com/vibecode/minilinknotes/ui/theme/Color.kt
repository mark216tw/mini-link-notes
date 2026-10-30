package com.vibecode.minilinknotes.ui.theme

import androidx.compose.ui.graphics.Color

data class TagColor(val light: Color, val dark: Color)

val TagColors = listOf(
    TagColor(Color(0xFFB3261E), Color(0xFFF2B8B5)),
    TagColor(Color(0xFFB2570A), Color(0xFFFFB77C)),
    TagColor(Color(0xFF8A6D00), Color(0xFFFFD65C)),
    TagColor(Color(0xFF5F6B00), Color(0xFFD6E36B)),
    TagColor(Color(0xFF2E6B32), Color(0xFF9CD99F)),
    TagColor(Color(0xFF00695C), Color(0xFF6FD8C8)),
    TagColor(Color(0xFF00639B), Color(0xFF7CC7F2)),
    TagColor(Color(0xFF33489B), Color(0xFFAAB8F5)),
    TagColor(Color(0xFF6A2C91), Color(0xFFD5A6EE)),
    TagColor(Color(0xFF9C2756), Color(0xFFF5A3C1)),
    TagColor(Color(0xFF6B4A3A), Color(0xFFC9A998)),
    TagColor(Color(0xFF4A6069), Color(0xFFAFC3CB)),
)

fun tagColor(index: Int, dark: Boolean): Color {
    val color = TagColors[index.coerceIn(0, TagColors.lastIndex)]
    return if (dark) color.dark else color.light
}

val TealPrimary = Color(0xFF0F766E)
val TealPrimaryDark = Color(0xFF7ED9CE)

val DarkOnPrimary = Color(0xFF003734)
val DarkPrimaryContainer = Color(0xFF00504B)
val DarkOnPrimaryContainer = Color(0xFF9CF2E8)
val DarkSecondary = Color(0xFFB0CCC7)
val DarkBackground = Color(0xFF101413)
val DarkSurfaceVariant = Color(0xFF3F4947)

val LightPrimaryContainer = Color(0xFFB2DFD8)
val LightOnPrimaryContainer = Color(0xFF00201D)
val LightSecondary = Color(0xFF4A635F)
val LightBackground = Color(0xFFFAFDFC)
val LightSurfaceVariant = Color(0xFFDBE5E2)
