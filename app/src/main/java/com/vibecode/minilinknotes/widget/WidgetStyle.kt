package com.vibecode.minilinknotes.widget

import androidx.compose.ui.graphics.Color
import com.vibecode.minilinknotes.ui.theme.WidgetTextBlack
import com.vibecode.minilinknotes.ui.theme.WidgetTextBlue
import com.vibecode.minilinknotes.ui.theme.WidgetTextDarkGray
import com.vibecode.minilinknotes.ui.theme.WidgetTextLightGray
import com.vibecode.minilinknotes.ui.theme.WidgetTextTeal
import com.vibecode.minilinknotes.ui.theme.WidgetTextWhite
import com.vibecode.minilinknotes.ui.theme.WidgetTextYellow

enum class WidgetTextColor(val color: Color?) {
    AUTO(null),
    BLACK(WidgetTextBlack),
    WHITE(WidgetTextWhite),
    DARK_GRAY(WidgetTextDarkGray),
    LIGHT_GRAY(WidgetTextLightGray),
    BLUE(WidgetTextBlue),
    TEAL(WidgetTextTeal),
    YELLOW(WidgetTextYellow),
    ;

    companion object {
        fun fromPreference(value: String?): WidgetTextColor =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

const val DEFAULT_WIDGET_FONT_PERCENT = 100
const val MIN_WIDGET_FONT_PERCENT = 50
const val MAX_WIDGET_FONT_PERCENT = 500
const val WIDGET_FONT_PERCENT_STEP = 10

fun normalizeWidgetFontPercent(value: Int?): Int {
    val bounded = (value ?: DEFAULT_WIDGET_FONT_PERCENT)
        .coerceIn(MIN_WIDGET_FONT_PERCENT, MAX_WIDGET_FONT_PERCENT)
    return ((bounded + WIDGET_FONT_PERCENT_STEP / 2) / WIDGET_FONT_PERCENT_STEP) *
        WIDGET_FONT_PERCENT_STEP
}
