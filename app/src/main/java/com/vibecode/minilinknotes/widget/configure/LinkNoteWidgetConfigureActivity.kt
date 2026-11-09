package com.vibecode.minilinknotes.widget.configure

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.vibecode.minilinknotes.MiniLinkNotesApp
import com.vibecode.minilinknotes.data.AppSettings
import com.vibecode.minilinknotes.ui.theme.MiniLinkNotesTheme
import com.vibecode.minilinknotes.widget.DEFAULT_WIDGET_FONT_PERCENT
import com.vibecode.minilinknotes.widget.LinkNoteWidget
import com.vibecode.minilinknotes.widget.WidgetBackgroundColor
import com.vibecode.minilinknotes.widget.WidgetTextColor
import com.vibecode.minilinknotes.widget.normalizeWidgetFontPercent
import kotlinx.coroutines.launch

private data class StoredWidgetConfiguration(
    val noteId: Long? = null,
    val textColor: WidgetTextColor = WidgetTextColor.AUTO,
    val backgroundColor: WidgetBackgroundColor = WidgetBackgroundColor.AUTO,
    val fontPercent: Int = DEFAULT_WIDGET_FONT_PERCENT,
)

class LinkNoteWidgetConfigureActivity : ComponentActivity() {

    private val appWidgetId: Int by lazy {
        intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val glanceId = runCatching {
            GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        }.getOrNull()
        if (glanceId == null) {
            finish()
            return
        }

        enableEdgeToEdge()
        val container = (application as MiniLinkNotesApp).container
        val viewModel = ViewModelProvider(
            this,
            LinkNoteWidgetConfigureViewModel.Factory(container.repository),
        )[LinkNoteWidgetConfigureViewModel::class.java]

        lifecycleScope.launch {
            val storedConfiguration = runCatching {
                val preferences: Preferences = getAppWidgetState(
                    this@LinkNoteWidgetConfigureActivity,
                    PreferencesGlanceStateDefinition,
                    glanceId,
                )
                StoredWidgetConfiguration(
                    noteId = preferences[LinkNoteWidget.noteIdKey],
                    textColor = WidgetTextColor.fromPreference(
                        preferences[LinkNoteWidget.textColorKey],
                    ),
                    backgroundColor = WidgetBackgroundColor.fromPreference(
                        preferences[LinkNoteWidget.backgroundColorKey],
                    ),
                    fontPercent = normalizeWidgetFontPercent(
                        preferences[LinkNoteWidget.fontPercentKey],
                    ),
                )
            }.getOrElse {
                StoredWidgetConfiguration(
                    fontPercent = normalizeWidgetFontPercent(null),
                )
            }
            viewModel.setInitialConfiguration(
                noteId = storedConfiguration.noteId,
                textColor = storedConfiguration.textColor,
                backgroundColor = storedConfiguration.backgroundColor,
                fontPercent = storedConfiguration.fontPercent,
            )
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val settings by container.settingsStore.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            MiniLinkNotesTheme(settings) {
                LinkNoteWidgetConfigureScreen(
                    state = state,
                    imageStorage = container.imageStorage,
                    onQueryChange = viewModel::setQuery,
                    onSelect = viewModel::select,
                    onTextColorChange = viewModel::setTextColor,
                    onBackgroundColorChange = viewModel::setBackgroundColor,
                    onFontPercentChange = viewModel::setFontPercent,
                    onConfirm = {
                        if (state.arePreferencesLoaded) state.selectedNoteId?.let { noteId ->
                            lifecycleScope.launch {
                                updateAppWidgetState(
                                    context = this@LinkNoteWidgetConfigureActivity,
                                    glanceId = glanceId,
                                ) { preferences ->
                                    preferences[LinkNoteWidget.noteIdKey] = noteId
                                    preferences[LinkNoteWidget.textColorKey] = state.textColor.name
                                    preferences[LinkNoteWidget.backgroundColorKey] =
                                        state.backgroundColor.name
                                    preferences[LinkNoteWidget.fontPercentKey] = state.fontPercent
                                }
                                LinkNoteWidget().update(this@LinkNoteWidgetConfigureActivity, glanceId)
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                )
                                finish()
                            }
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}
