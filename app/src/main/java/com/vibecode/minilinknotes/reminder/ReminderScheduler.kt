package com.vibecode.minilinknotes.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vibecode.minilinknotes.data.NotesDao

class ReminderScheduler(
    private val context: Context,
    private val dao: NotesDao,
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(noteId: Long, triggerAt: Long) {
        val at = maxOf(triggerAt, System.currentTimeMillis() + IMMEDIATE_DELAY_MS)
        val pendingIntent = pendingIntent(
            noteId = noteId,
            triggerAt = at,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Fall back to an inexact alarm below.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
    }

    fun cancel(noteId: Long) {
        val pendingIntent = pendingIntent(
            noteId = noteId,
            triggerAt = 0L,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    suspend fun cancelAll() {
        dao.getReminderNoteIds().forEach { cancel(it) }
    }

    suspend fun syncAll() {
        dao.getEnabledReminders().forEach { entry ->
            schedule(entry.noteId, entry.reminderAt)
        }
    }

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(noteId: Long, triggerAt: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAt)
        }
        return PendingIntent.getBroadcast(context, noteId.hashCode(), intent, flags)
    }

    private companion object {
        const val IMMEDIATE_DELAY_MS = 1_000L
    }
}
