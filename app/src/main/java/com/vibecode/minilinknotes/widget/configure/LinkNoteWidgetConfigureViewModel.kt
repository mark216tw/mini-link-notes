package com.vibecode.minilinknotes.widget.configure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecode.minilinknotes.data.NoteEntity
import com.vibecode.minilinknotes.data.NotesRepository
import com.vibecode.minilinknotes.widget.DEFAULT_WIDGET_FONT_PERCENT
import com.vibecode.minilinknotes.widget.WidgetTextColor
import com.vibecode.minilinknotes.widget.normalizeWidgetFontPercent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class WidgetConfigureUiState(
    val isLoading: Boolean = true,
    val notes: List<NoteEntity> = emptyList(),
    val selectedNote: NoteEntity? = null,
    val selectedNoteId: Long? = null,
    val query: String = "",
    val textColor: WidgetTextColor = WidgetTextColor.AUTO,
    val fontPercent: Int = DEFAULT_WIDGET_FONT_PERCENT,
    val arePreferencesLoaded: Boolean = false,
)

private data class WidgetConfiguration(
    val selectedNoteId: Long? = null,
    val textColor: WidgetTextColor = WidgetTextColor.AUTO,
    val fontPercent: Int = DEFAULT_WIDGET_FONT_PERCENT,
    val isLoaded: Boolean = false,
)

class LinkNoteWidgetConfigureViewModel(
    repository: NotesRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val configurationFlow = MutableStateFlow(WidgetConfiguration())

    val uiState: StateFlow<WidgetConfigureUiState> = combine(
        repository.activeNoteEntities,
        queryFlow,
        configurationFlow,
    ) { notes, query, configuration ->
        val keyword = query.trim()
        val filtered = if (keyword.isEmpty()) {
            notes
        } else {
            notes.filter { note ->
                note.title.contains(keyword, ignoreCase = true) ||
                    note.url?.contains(keyword, ignoreCase = true) == true ||
                    note.content?.contains(keyword, ignoreCase = true) == true
            }
        }
        WidgetConfigureUiState(
            isLoading = false,
            notes = filtered,
            selectedNote = notes.firstOrNull { it.id == configuration.selectedNoteId },
            selectedNoteId = configuration.selectedNoteId,
            query = query,
            textColor = configuration.textColor,
            fontPercent = configuration.fontPercent,
            arePreferencesLoaded = configuration.isLoaded,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WidgetConfigureUiState(),
        )
    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun select(noteId: Long) {
        configurationFlow.value = configurationFlow.value.copy(selectedNoteId = noteId)
    }

    fun setTextColor(textColor: WidgetTextColor) {
        configurationFlow.value = configurationFlow.value.copy(textColor = textColor)
    }

    fun setFontPercent(fontPercent: Int) {
        configurationFlow.value = configurationFlow.value.copy(
            fontPercent = normalizeWidgetFontPercent(fontPercent),
        )
    }

    fun setInitialConfiguration(
        noteId: Long?,
        textColor: WidgetTextColor,
        fontPercent: Int,
    ) {
        if (configurationFlow.value.isLoaded) return
        configurationFlow.value = WidgetConfiguration(
            selectedNoteId = noteId,
            textColor = textColor,
            fontPercent = normalizeWidgetFontPercent(fontPercent),
            isLoaded = true,
        )
    }

    class Factory(
        private val repository: NotesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LinkNoteWidgetConfigureViewModel(repository) as T
        }
    }
}
