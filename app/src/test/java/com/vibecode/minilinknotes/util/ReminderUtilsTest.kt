package com.vibecode.minilinknotes.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUtilsTest {

    private val zone = ZoneId.systemDefault()

    @Test
    fun defaultReminderAt_isOneHourLaterWithoutSeconds() {
        val now = Instant.parse("2026-11-09T10:15:30Z").toEpochMilli()
        val result = ReminderUtils.defaultReminderAt(now)
        val time = Instant.ofEpochMilli(result).atZone(zone)
        val expected = Instant.ofEpochMilli(now).atZone(zone).plusHours(1)
        assertEquals(expected.toLocalDate(), time.toLocalDate())
        assertEquals(expected.hour, time.hour)
        assertEquals(expected.minute, time.minute)
        assertEquals(0, time.second)
        assertEquals(0, time.nano)
    }

    @Test
    fun combineDate_keepsTimeOfOriginalTimestamp() {
        val base = Instant.parse("2026-11-09T10:15:00Z").toEpochMilli()
        val baseTime = Instant.ofEpochMilli(base).atZone(zone).toLocalTime()
        val nextDay = Instant.ofEpochMilli(base).atZone(zone).toLocalDate().plusDays(1)
        val result = ReminderUtils.combineDate(base, nextDay)
        val resultDateTime = Instant.ofEpochMilli(result).atZone(zone)
        assertEquals(nextDay, resultDateTime.toLocalDate())
        assertEquals(baseTime, resultDateTime.toLocalTime())
    }

    @Test
    fun combineTime_keepsDateOfOriginalTimestamp() {
        val base = Instant.parse("2026-11-09T10:15:00Z").toEpochMilli()
        val baseDate = Instant.ofEpochMilli(base).atZone(zone).toLocalDate()
        val result = ReminderUtils.combineTime(base, 23, 45)
        val resultDateTime = Instant.ofEpochMilli(result).atZone(zone)
        assertEquals(baseDate, resultDateTime.toLocalDate())
        assertEquals(23, resultDateTime.hour)
        assertEquals(45, resultDateTime.minute)
    }

    @Test
    fun isValid_requiresFutureTimestamp() {
        val now = System.currentTimeMillis()
        assertFalse(ReminderUtils.isValid(null, now))
        assertFalse(ReminderUtils.isValid(now, now))
        assertFalse(ReminderUtils.isValid(now - 1_000, now))
        assertTrue(ReminderUtils.isValid(now + 1_000, now))
    }

    @Test
    fun combineDate_withStartOfDayDate() {
        val base = Instant.parse("2026-11-09T10:15:00Z").toEpochMilli()
        val date = LocalDate.of(2030, 1, 2)
        val result = ReminderUtils.combineDate(base, date)
        assertEquals(date, Instant.ofEpochMilli(result).atZone(zone).toLocalDate())
    }
}
