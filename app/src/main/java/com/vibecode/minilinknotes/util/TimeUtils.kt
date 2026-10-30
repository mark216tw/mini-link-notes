package com.vibecode.minilinknotes.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

object TimeUtils {

    const val TRASH_RETENTION_DAYS = 30

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

    fun formatDateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)

    fun trashDaysLeft(deletedAt: Long, now: Long = System.currentTimeMillis()): Int {
        val expiresAt = deletedAt + TRASH_RETENTION_DAYS * DAY_MILLIS
        val days = ceil((expiresAt - now).toDouble() / DAY_MILLIS).toInt()
        return days.coerceAtLeast(0)
    }

    fun trashExpiryThreshold(now: Long = System.currentTimeMillis()): Long =
        now - TRASH_RETENTION_DAYS * DAY_MILLIS
}
