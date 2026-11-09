package com.vibecode.minilinknotes.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ReminderUtils {

    fun defaultReminderAt(now: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(now)
            .atZone(zone)
            .plusHours(1)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
    }

    fun combineDate(at: Long, date: LocalDate): Long {
        val zone = ZoneId.systemDefault()
        val time = Instant.ofEpochMilli(at).atZone(zone).toLocalTime()
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    fun combineTime(at: Long, hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    fun isValid(at: Long?, now: Long = System.currentTimeMillis()): Boolean =
        at != null && at > now
}
