package com.vibecode.minilinknotes.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LinkNoteWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = LinkNoteWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                appWidgetIds.forEach { appWidgetId ->
                    runCatching {
                        updateAppWidgetState(
                            context = context,
                            glanceId = manager.getGlanceIdBy(appWidgetId),
                        ) { preferences ->
                            preferences.remove(LinkNoteWidget.noteIdKey)
                            preferences.remove(LinkNoteWidget.textColorKey)
                            preferences.remove(LinkNoteWidget.backgroundColorKey)
                            preferences.remove(LinkNoteWidget.fontPercentKey)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
