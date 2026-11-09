package com.vibecode.minilinknotes.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vibecode.minilinknotes.MainActivity
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.NoteEntity
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.util.UrlUtils

object ReminderNotifier {

    const val CHANNEL_ID = "note_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun notify(context: Context, note: NoteEntity) {
        val title = note.title.ifBlank {
            context.getString(
                when (note.type) {
                    NoteType.LINK -> R.string.note_untitled_link
                    NoteType.TEXT -> R.string.note_untitled_text
                    NoteType.IMAGE -> R.string.note_untitled_image
                },
            )
        }
        val text = note.content?.trim()
            ?.replace('\n', ' ')
            ?.takeIf { it.isNotBlank() }
            ?: note.url?.let(UrlUtils::hostOf)
            ?: context.getString(R.string.reminder_notification_text)

        val contentIntent = PendingIntent.getActivity(
            context,
            note.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(note.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Notification permission was revoked; the reminder is still consumed.
        }
    }
}
