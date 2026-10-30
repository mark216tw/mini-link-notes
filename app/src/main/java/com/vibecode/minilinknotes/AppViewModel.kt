package com.vibecode.minilinknotes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecode.minilinknotes.data.AppSettings
import com.vibecode.minilinknotes.data.CreateNoteResult
import com.vibecode.minilinknotes.data.DisplayMode
import com.vibecode.minilinknotes.data.DuplicateTagNameException
import com.vibecode.minilinknotes.data.NoteDraft
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.data.NoteWithTags
import com.vibecode.minilinknotes.data.NotesRepository
import com.vibecode.minilinknotes.data.SettingsStore
import com.vibecode.minilinknotes.data.SortField
import com.vibecode.minilinknotes.data.TagEntity
import com.vibecode.minilinknotes.data.TagWithNoteCount
import com.vibecode.minilinknotes.data.backup.BackupException
import com.vibecode.minilinknotes.data.backup.BackupManager
import com.vibecode.minilinknotes.data.backup.ImportError
import com.vibecode.minilinknotes.data.backup.ImportMode
import com.vibecode.minilinknotes.util.SharedContent
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val notes: List<NoteWithTags> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val tagsWithCount: List<TagWithNoteCount> = emptyList(),
    val trash: List<NoteWithTags> = emptyList(),
    val query: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val unreadOnly: Boolean = false,
    val unreadCount: Int = 0,
    val totalCount: Int = 0,
) {
    val hasFilter: Boolean
        get() = query.isNotBlank() || selectedTagIds.isNotEmpty() || unreadOnly
}

data class SnackbarEvent(
    val message: String,
    val action: SnackbarAction? = null,
)

sealed interface SnackbarAction {
    data class EditNote(val noteId: Long) : SnackbarAction
    data class UndoDelete(val noteId: Long) : SnackbarAction
}

@OptIn(FlowPreview::class)
class AppViewModel(
    private val appContext: Context,
    private val repository: NotesRepository,
    private val settingsStore: SettingsStore,
    private val backupManager: BackupManager,
) : ViewModel() {

    private data class Snapshot(
        val notes: List<NoteWithTags>,
        val trash: List<NoteWithTags>,
        val tags: List<TagEntity>,
        val tagsWithCount: List<TagWithNoteCount>,
        val filterQuery: String,
        val selectedTagIds: Set<Long>,
        val unreadOnly: Boolean,
    )

    private data class FilterState(
        val query: String,
        val selectedTagIds: Set<Long>,
        val unreadOnly: Boolean,
    )

    private val queryFlow = MutableStateFlow("")
    private val selectedTagIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val unreadOnlyFlow = MutableStateFlow(false)
    private val titleCollator = Collator.getInstance(Locale.TRADITIONAL_CHINESE)

    private val filterQueryFlow = queryFlow.debounce { query ->
        if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS
    }

    private val filterStateFlow = combine(
        filterQueryFlow,
        selectedTagIdsFlow,
        unreadOnlyFlow,
    ) { query, selectedTagIds, unreadOnly ->
        FilterState(query, selectedTagIds, unreadOnly)
    }

    private val _events = Channel<SnackbarEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val snapshotFlow = combine(
        repository.activeNotes,
        repository.trash,
        repository.tagsWithCount,
        filterStateFlow,
    ) { notes, trash, tagCounts, filter ->
        Snapshot(
            notes = notes,
            trash = trash,
            tags = tagCounts.map { it.tag },
            tagsWithCount = tagCounts,
            filterQuery = filter.query,
            selectedTagIds = filter.selectedTagIds,
            unreadOnly = filter.unreadOnly,
        )
    }

    private val baseUiState = combine(settingsStore.settings, snapshotFlow) { settings, snapshot ->
        buildState(settings, snapshot)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<NotesUiState> = combine(baseUiState, queryFlow) { base, query ->
        base.copy(query = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesUiState(),
    )

    init {
        viewModelScope.launch { repository.purgeExpired() }
    }

    fun setQuery(value: String) {
        queryFlow.value = value
    }

    fun toggleTagFilter(tagId: Long) {
        val current = selectedTagIdsFlow.value.toMutableSet()
        if (!current.add(tagId)) current.remove(tagId)
        selectedTagIdsFlow.value = current
    }

    fun toggleUnreadOnly() {
        unreadOnlyFlow.value = !unreadOnlyFlow.value
    }

    fun clearFilters() {
        selectedTagIdsFlow.value = emptySet()
        unreadOnlyFlow.value = false
    }

    fun setSort(field: SortField, ascending: Boolean) {
        viewModelScope.launch { settingsStore.setSort(field, ascending) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        viewModelScope.launch { settingsStore.setDisplayMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setAutoFetchTitle(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoFetchTitle(enabled) }
    }

    fun handleSharedContent(shared: SharedContent) {
        viewModelScope.launch {
            runCatching { repository.createFromShared(shared) }
                .onSuccess { result ->
                    emitShareResult(result)
                    if (shared.url != null && shared.subject.isNullOrBlank()) {
                        viewModelScope.launch { repository.fetchAndApplyTitle(result.id) }
                    }
                }
                .onFailure {
                    emitEvent(SnackbarEvent(appContext.getString(R.string.share_save_failed)))
                }
        }
    }

    fun notifyEmptyShare() {
        emitEvent(SnackbarEvent(appContext.getString(R.string.share_empty)))
    }

    fun showMessage(resId: Int) {
        emitEvent(SnackbarEvent(appContext.getString(resId)))
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.softDelete(noteId)
            emitEvent(
                SnackbarEvent(
                    message = appContext.getString(R.string.deleted_note),
                    action = SnackbarAction.UndoDelete(noteId),
                ),
            )
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            repository.restore(noteId)
            emitEvent(SnackbarEvent(appContext.getString(R.string.restored_note)))
        }
    }

    fun purgeNote(noteId: Long) {
        viewModelScope.launch {
            repository.purge(noteId)
            emitEvent(SnackbarEvent(appContext.getString(R.string.purged_note)))
        }
    }

    fun toggleRead(noteId: Long) {
        viewModelScope.launch { repository.toggleRead(noteId) }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val count = repository.markAllRead()
            if (count > 0) {
                emitEvent(
                    SnackbarEvent(appContext.getString(R.string.notes_all_read_done, count)),
                )
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val count = repository.purgeAllTrash()
            emitEvent(
                SnackbarEvent(appContext.getString(R.string.trash_purged_all, count)),
            )
        }
    }

    suspend fun getNote(noteId: Long): NoteWithTags? = repository.getNote(noteId)

    suspend fun saveNote(noteId: Long?, draft: NoteDraft): Result<Long> {
        val shouldFetchTitle = draft.type == NoteType.LINK && draft.title.isBlank()
        val result = runCatching {
            if (noteId == null || noteId <= 0) {
                repository.createNote(draft).id
            } else {
                repository.updateNote(noteId, draft).getOrThrow()
                noteId
            }
        }
        result.onSuccess { savedId ->
            emitEvent(SnackbarEvent(appContext.getString(R.string.editor_saved)))
            if (shouldFetchTitle) {
                viewModelScope.launch { repository.fetchAndApplyTitle(savedId) }
            }
        }
        return result
    }

    suspend fun previewTitle(url: String): Result<String> = repository.previewTitle(url)

    suspend fun addTag(name: String, colorIndex: Int): Result<Long> {
        val result = repository.addTag(name, colorIndex)
        result.onSuccess {
            emitEvent(SnackbarEvent(appContext.getString(R.string.tag_created)))
        }
        return result
    }

    suspend fun updateTag(tagId: Long, name: String, colorIndex: Int): Result<Unit> {
        val result = repository.updateTag(tagId, name, colorIndex)
        result.onSuccess {
            emitEvent(SnackbarEvent(appContext.getString(R.string.tag_updated)))
        }
        return result
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            repository.deleteTag(tagId)
            emitEvent(SnackbarEvent(appContext.getString(R.string.tag_deleted)))
        }
    }

    fun moveTag(tagId: Long, toIndex: Int) {
        viewModelScope.launch { repository.moveTag(tagId, toIndex) }
    }

    suspend fun noteCountForTag(tagId: Long): Int = repository.countNotesForTag(tagId)

    fun export(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportTo(uri) }
                .onSuccess { count ->
                    emitEvent(
                        SnackbarEvent(appContext.getString(R.string.settings_export_success, count)),
                    )
                }
                .onFailure {
                    emitEvent(SnackbarEvent(appContext.getString(R.string.settings_export_failed)))
                }
        }
    }

    fun import(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            runCatching { backupManager.importFrom(uri, mode) }
                .onSuccess { count ->
                    emitEvent(
                        SnackbarEvent(appContext.getString(R.string.settings_import_success, count)),
                    )
                }
                .onFailure { error ->
                    emitEvent(SnackbarEvent(importErrorMessage(error)))
                }
        }
    }

    fun isDuplicateTagError(error: Throwable?): Boolean = error is DuplicateTagNameException

    private fun importErrorMessage(error: Throwable): String {
        val backupError = error as? BackupException
        return when (backupError?.reason) {
            ImportError.INVALID -> appContext.getString(R.string.settings_import_invalid)
            ImportError.NEWER_VERSION -> appContext.getString(R.string.settings_import_newer)
            else -> appContext.getString(
                R.string.settings_import_failed,
                error.message.orEmpty(),
            )
        }
    }

    private fun emitShareResult(result: CreateNoteResult) {
        val message = appContext.getString(
            if (result.duplicate) R.string.share_saved_duplicate else R.string.share_saved,
        )
        emitEvent(SnackbarEvent(message, SnackbarAction.EditNote(result.id)))
    }

    private fun emitEvent(event: SnackbarEvent) {
        _events.trySend(event)
    }

    private fun buildState(settings: AppSettings, snapshot: Snapshot): NotesUiState {
        val needle = snapshot.filterQuery.trim()
        val existingTagIds = snapshot.tags.map { it.id }.toSet()
        val activeTagSelection = snapshot.selectedTagIds intersect existingTagIds
        val filtered = snapshot.notes.filter { noteWithTags ->
            val note = noteWithTags.note
            val matchesQuery = needle.isBlank() ||
                note.title.contains(needle, ignoreCase = true) ||
                note.url.orEmpty().contains(needle, ignoreCase = true) ||
                note.content.orEmpty().contains(needle, ignoreCase = true)
            val matchesTags = activeTagSelection.isEmpty() ||
                noteWithTags.tags.any { it.id in activeTagSelection }
            val matchesUnread = !snapshot.unreadOnly || !note.isRead
            matchesQuery && matchesTags && matchesUnread
        }

        val sorted = when (settings.sortField) {
            SortField.CREATED_AT -> filtered.sortedBy { it.note.createdAt }
            SortField.TITLE -> filtered.sortedWith { first, second ->
                titleCollator.compare(displayTitle(first), displayTitle(second))
            }

            SortField.TAG -> filtered.sortedWith(
                compareBy(
                    { it.tags.minOfOrNull { tag -> tag.sortOrder } ?: Int.MAX_VALUE },
                    { -it.note.createdAt },
                ),
            )
        }

        val ordered = if (settings.sortField == SortField.TAG || settings.sortAscending) {
            sorted
        } else {
            sorted.reversed()
        }

        return NotesUiState(
            isLoading = false,
            settings = settings,
            notes = ordered.map { noteWithTags ->
                noteWithTags.copy(
                    tags = noteWithTags.tags.sortedWith(compareBy({ it.sortOrder }, { it.name })),
                )
            },
            tags = snapshot.tags,
            tagsWithCount = snapshot.tagsWithCount,
            trash = snapshot.trash.map { noteWithTags ->
                noteWithTags.copy(
                    tags = noteWithTags.tags.sortedBy { it.sortOrder },
                )
            },
            query = snapshot.filterQuery,
            selectedTagIds = activeTagSelection,
            unreadOnly = snapshot.unreadOnly,
            unreadCount = snapshot.notes.count { !it.note.isRead },
            totalCount = snapshot.notes.size,
        )
    }

    private fun displayTitle(noteWithTags: NoteWithTags): String =
        noteWithTags.note.title.ifBlank {
            noteWithTags.note.url ?: noteWithTags.note.content.orEmpty().take(80)
        }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }

    class Factory(
        private val context: Context,
        private val repository: NotesRepository,
        private val settingsStore: SettingsStore,
        private val backupManager: BackupManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                appContext = context.applicationContext,
                repository = repository,
                settingsStore = settingsStore,
                backupManager = backupManager,
            ) as T
        }
    }
}

