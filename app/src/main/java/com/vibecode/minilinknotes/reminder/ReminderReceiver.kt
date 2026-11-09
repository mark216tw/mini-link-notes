package com.vibecode.minilinknotes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vibecode.minilinknotes.MiniLinkNotesApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as MiniLinkNotesApp).container
                val dao = container.database.notesDao()
                val note = dao.getNoteEntity(noteId) ?: return@launch
                if (note.deletedAt != null || !note.reminderEnabled) return@launch
                ReminderNotifier.notify(context, note)
                dao.setReminderEnabled(noteId, false, System.currentTimeMillis())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.vibecode.minilinknotes.action.REMINDER"
        const val EXTRA_NOTE_ID = "com.vibecode.minilinknotes.extra.REMINDER_NOTE_ID"
        const val EXTRA_TRIGGER_AT = "com.vibecode.minilinknotes.extra.REMINDER_TRIGGER_AT"
    }
}
