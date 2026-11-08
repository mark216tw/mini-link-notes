package com.vibecode.minilinknotes.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStyleTest {

    @Test
    fun normalize_clampsToSupportedRange() {
        assertEquals(50, normalizeWidgetFontPercent(0))
        assertEquals(50, normalizeWidgetFontPercent(-20))
        assertEquals(500, normalizeWidgetFontPercent(1000))
    }

    @Test
    fun normalize_roundsToTenPercentSteps() {
        assertEquals(100, normalizeWidgetFontPercent(104))
        assertEquals(110, normalizeWidgetFontPercent(106))
        assertEquals(500, normalizeWidgetFontPercent(495))
    }

    @Test
    fun normalize_usesDefaultWhenMissing() {
        assertEquals(DEFAULT_WIDGET_FONT_PERCENT, normalizeWidgetFontPercent(null))
    }

    @Test
    fun textColor_fallsBackToAuto() {
        assertEquals(WidgetTextColor.AUTO, WidgetTextColor.fromPreference(null))
        assertEquals(WidgetTextColor.AUTO, WidgetTextColor.fromPreference("UNKNOWN"))
        assertEquals(WidgetTextColor.BLUE, WidgetTextColor.fromPreference("BLUE"))
    }
}
