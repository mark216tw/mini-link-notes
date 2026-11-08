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
import com.vibecode.minilinknotes.widget.LinkNoteWidget
import com.vibecode.minilinknotes.widget.WidgetTextColor
import com.vibecode.minilinknotes.widget.normalizeWidgetFontPercent
import kotlinx.coroutines.launch

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
                Triple(
                    preferences[LinkNoteWidget.noteIdKey],
                    WidgetTextColor.fromPreference(preferences[LinkNoteWidget.textColorKey]),
                    normalizeWidgetFontPercent(preferences[LinkNoteWidget.fontPercentKey]),
                )
            }.getOrElse {
                Triple(null, WidgetTextColor.AUTO, normalizeWidgetFontPercent(null))
            }
            viewModel.setInitialConfiguration(
                noteId = storedConfiguration.first,
                textColor = storedConfiguration.second,
                fontPercent = storedConfiguration.third,
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
