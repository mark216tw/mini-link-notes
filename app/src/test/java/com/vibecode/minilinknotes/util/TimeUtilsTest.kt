package com.vibecode.minilinknotes.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    private val day = 24L * 60L * 60L * 1000L

    @Test
    fun trashDaysLeft_fullRetention() {
        val now = 1_000_000_000L
        assertEquals(30, TimeUtils.trashDaysLeft(now, now))
    }

    @Test
    fun trashDaysLeft_oneDayLeft() {
        val now = 1_000_000_000L
        assertEquals(1, TimeUtils.trashDaysLeft(now - 29 * day, now))
    }

    @Test
    fun trashDaysLeft_expiredIsZero() {
        val now = 1_000_000_000L
        assertEquals(0, TimeUtils.trashDaysLeft(now - 31 * day, now))
    }
}
