package com.vibecode.minilinknotes.ui.theme

import androidx.compose.ui.graphics.Color

data class TagColor(val light: Color, val dark: Color)

val TagColors = listOf(
    TagColor(Color(0xFFD32F2F), Color(0xFFFF8A80)),
    TagColor(Color(0xFFE64A19), Color(0xFFFFAB91)),
    TagColor(Color(0xFFE65100), Color(0xFFFFCC80)),
    TagColor(Color(0xFF558B2F), Color(0xFFC5E1A5)),
    TagColor(Color(0xFF2E7D32), Color(0xFF81C784)),
    TagColor(Color(0xFF00796B), Color(0xFF4DB6AC)),
    TagColor(Color(0xFF0277BD), Color(0xFF4FC3F7)),
    TagColor(Color(0xFF303F9F), Color(0xFF7986CB)),
    TagColor(Color(0xFF7B1FA2), Color(0xFFBA68C8)),
    TagColor(Color(0xFFC2185B), Color(0xFFF06292)),
    TagColor(Color(0xFF6D4C41), Color(0xFFBCAAA4)),
    TagColor(Color(0xFF455A64), Color(0xFF90A4AE)),
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

val WidgetBackgroundDay = Color(0xFFFFFFFF)
val WidgetBackgroundNight = Color(0xFF1C211F)
val WidgetTitleDay = Color(0xFF1A1C1B)
val WidgetTitleNight = Color(0xFFE2E3E1)
val WidgetBodyDay = Color(0xFF444746)
val WidgetBodyNight = Color(0xFFC4C7C5)
val WidgetLinkDay = Color(0xFF006A60)
val WidgetLinkNight = Color(0xFF4FDBC8)
val ImageViewerScrim = Color(0xF2000000)
val ImageViewerContent = Color(0xFFFFFFFF)

val WidgetTextBlack = Color(0xFF000000)
val WidgetTextWhite = Color(0xFFFFFFFF)
val WidgetTextDarkGray = Color(0xFF444444)
val WidgetTextLightGray = Color(0xFFD3D3D3)
val WidgetTextBlue = Color(0xFF1565C0)
val WidgetTextTeal = Color(0xFF008577)
val WidgetTextYellow = Color(0xFFF9A825)
