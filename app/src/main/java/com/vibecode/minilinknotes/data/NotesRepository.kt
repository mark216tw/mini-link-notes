package com.vibecode.minilinknotes.data

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.room.withTransaction
import com.vibecode.minilinknotes.util.SharedContent
import com.vibecode.minilinknotes.util.TimeUtils
import com.vibecode.minilinknotes.util.UrlUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class NoteDraft(
    val type: NoteType,
    val title: String,
    val url: String?,
    val content: String?,
    val imageFileName: String? = null,
    val imageMimeType: String? = null,
    val imageSha256: String? = null,
    val tagIds: List<Long> = emptyList(),
    val titleManuallyEdited: Boolean = false,
)

data class CreateNoteResult(
    val id: Long,
    val duplicate: Boolean,
)

class DuplicateTagNameException : Exception()

class NotesRepository(
    private val database: AppDatabase,
    private val dao: NotesDao,
    private val settingsStore: SettingsStore,
    private val titleFetcher: TitleFetcher,
    private val imageStorage: ImageStorage,
) {

    val activeNotes: Flow<List<NoteWithTags>> = dao.observeActiveNotes()
    val activeNoteEntities: Flow<List<NoteEntity>> = dao.observeActiveNoteEntities()
    val trash: Flow<List<NoteWithTags>> = dao.observeTrash()
    val tagsWithCount: Flow<List<TagWithNoteCount>> = dao.observeTagsWithCount()

    suspend fun getNote(id: Long): NoteWithTags? = dao.getNote(id)

    suspend fun getActiveNoteEntity(id: Long): NoteEntity? = dao.getActiveNoteEntity(id)

    suspend fun importImage(uri: Uri): StoredImage = imageStorage.import(uri)

    fun imageFile(fileName: String?) = imageStorage.imageFile(fileName)

    suspend fun imageThumbnail(fileName: String?) = imageStorage.thumbnailFile(fileName)

    suspend fun discardImage(fileName: String?) = imageStorage.delete(fileName)

    suspend fun createNote(draft: NoteDraft): CreateNoteResult {
        val normalized = normalizeDraft(draft)
        val now = System.currentTimeMillis()

        val result = database.withTransaction {
            val duplicate = when (normalized.type) {
                NoteType.LINK -> normalized.url?.let { url ->
                    val key = UrlUtils.dedupeKey(url)
                    dao.getActiveUrls().any { UrlUtils.dedupeKey(it) == key }
                } ?: false

                NoteType.TEXT -> false
                NoteType.IMAGE -> normalized.imageSha256?.let { it in dao.getActiveImageHashes() } ?: false
            }
            val noteId = dao.insertNote(
                NoteEntity(
                    type = normalized.type,
                    title = normalized.title,
                    url = normalized.url,
                    content = normalized.content,
                    imageFileName = normalized.imageFileName,
                    imageMimeType = normalized.imageMimeType,
                    imageSha256 = normalized.imageSha256,
                    isRead = false,
                    createdAt = now,
                    updatedAt = now,
                    titleEdited = normalized.titleManuallyEdited,
                ),
            )
            replaceTags(noteId, normalized.tagIds)
            CreateNoteResult(noteId, duplicate)
        }
        return result
    }

    suspend fun createFromShared(shared: SharedContent): CreateNoteResult {
        val isLink = shared.url != null
        val content = shared.text.trim().ifBlank { null }
        val contentWithoutUrl = if (isLink && content == shared.url) null else content
        val title = when {
            isLink -> shared.subject.orEmpty()
            else -> shared.subject ?: firstLineTitle(shared.text)
        }
        return createNote(
            NoteDraft(
                type = if (isLink) NoteType.LINK else NoteType.TEXT,
                title = title,
                url = shared.url,
                content = contentWithoutUrl,
                tagIds = emptyList(),
                titleManuallyEdited = false,
            ),
        )
    }

    suspend fun updateNote(id: Long, draft: NoteDraft): Result<Unit> {
        val normalized = normalizeDraft(draft)
        val now = System.currentTimeMillis()
        var existing: NoteEntity? = null
        return try {
            database.withTransaction {
                existing = dao.getNoteEntity(id) ?: error("Note not found")
                val urlChanged = existing!!.url != normalized.url
                val titleEdited = when {
                    normalized.type == NoteType.LINK && normalized.title.isBlank() -> false
                    else -> existing!!.titleEdited || normalized.titleManuallyEdited
                }
                dao.updateNote(
                    existing!!.copy(
                        type = normalized.type,
                        title = normalized.title,
                        url = normalized.url,
                        content = normalized.content,
                        imageFileName = normalized.imageFileName,
                        imageMimeType = normalized.imageMimeType,
                        imageSha256 = normalized.imageSha256,
                        titleEdited = titleEdited,
                        titleFetchState = if (urlChanged) TitleFetchState.NONE else existing!!.titleFetchState,
                        updatedAt = now,
                    ),
                )
                replaceTags(id, normalized.tagIds)
            }
            if (existing?.imageFileName != normalized.imageFileName) {
                imageStorage.delete(existing?.imageFileName)
            }
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    suspend fun toggleRead(id: Long) {
        dao.toggleRead(id, System.currentTimeMillis())
    }

    suspend fun markAllRead(): Int = dao.markAllRead(System.currentTimeMillis())

    suspend fun purgeAllTrash(): Int {
        val imageFiles = dao.getTrashImageFileNames()
        val count = dao.purgeAllTrash()
        imageFiles.forEach { imageStorage.delete(it) }
        return count
    }

    suspend fun softDelete(id: Long) {
        val now = System.currentTimeMillis()
        dao.setDeletedAt(id, now, now)
    }

    suspend fun restore(id: Long) {
        dao.clearDeletedAt(id, System.currentTimeMillis())
    }

    suspend fun purge(id: Long) {
        val imageFileName = dao.getNoteEntity(id)?.imageFileName
        dao.deleteNotePermanently(id)
        imageStorage.delete(imageFileName)
    }

    suspend fun purgeExpired(): Int {
        val threshold = TimeUtils.trashExpiryThreshold()
        val imageFiles = dao.getExpiredTrashImageFileNames(threshold)
        val count = dao.purgeExpired(threshold)
        imageFiles.forEach { imageStorage.delete(it) }
        return count
    }

    suspend fun cleanupImageFiles() {
        imageStorage.cleanupUnreferenced(dao.getAllImageFileNames().toSet())
    }

    suspend fun fetchAndApplyTitle(id: Long, force: Boolean = false): Boolean {
        if (!force) {
            val settings = settingsStore.settings.first()
            if (!settings.autoFetchTitle) return false
        }
        val note = dao.getNoteEntity(id) ?: return false
        val url = note.url ?: return false
        if (!force && note.titleEdited) return false

        val result = titleFetcher.fetchTitle(url)
        return result.fold(
            onSuccess = { title ->
                val latest = dao.getNoteEntity(id)
                if (!force && latest?.titleEdited == true) {
                    false
                } else {
                    dao.updateFetchedTitle(
                        id = id,
                        title = title,
                        state = TitleFetchState.SUCCESS,
                        titleEdited = false,
                        updatedAt = System.currentTimeMillis(),
                    )
                    true
                }
            },
            onFailure = {
                dao.setTitleFetchState(id, TitleFetchState.FAILED)
                false
            },
        )
    }

    suspend fun previewTitle(url: String): Result<String> {
        val normalized = UrlUtils.normalize(url)
            ?: return Result.failure(IllegalArgumentException("Invalid URL"))
        return titleFetcher.fetchTitle(normalized)
    }

    suspend fun addTag(name: String, colorIndex: Int): Result<Long> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        if (dao.findTagByName(trimmed) != null) throw DuplicateTagNameException()
        val order = dao.maxTagSortOrder() + 1
        try {
            dao.insertTag(
                TagEntity(
                    name = trimmed,
                    colorIndex = colorIndex.coerceIn(0, TAG_COLOR_COUNT - 1),
                    sortOrder = order,
                ),
            )
        } catch (_: SQLiteConstraintException) {
            throw DuplicateTagNameException()
        }
    }

    suspend fun updateTag(id: Long, name: String, colorIndex: Int): Result<Unit> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val sameName = dao.findTagByName(trimmed)
        if (sameName != null && sameName.id != id) throw DuplicateTagNameException()
        val tag = dao.getTag(id) ?: error("Tag not found")
        try {
            dao.updateTag(
                tag.copy(
                    name = trimmed,
                    colorIndex = colorIndex.coerceIn(0, TAG_COLOR_COUNT - 1),
                ),
            )
        } catch (_: SQLiteConstraintException) {
            throw DuplicateTagNameException()
        }
    }

    suspend fun deleteTag(id: Long) {
        database.withTransaction {
            dao.deleteTag(id)
        }
    }

    suspend fun moveTag(tagId: Long, toIndex: Int) {
        database.withTransaction {
            val ordered = dao.getTags().toMutableList()
            val from = ordered.indexOfFirst { it.id == tagId }
            if (from == -1) return@withTransaction
            val target = toIndex.coerceIn(0, ordered.lastIndex)
            if (from == target) return@withTransaction
            val item = ordered.removeAt(from)
            ordered.add(target, item)
            ordered.forEachIndexed { index, tag ->
                if (tag.sortOrder != index) dao.setTagSortOrder(tag.id, index)
            }
        }
    }

    suspend fun countNotesForTag(tagId: Long): Int = dao.countNotesForTag(tagId)

    private suspend fun replaceTags(noteId: Long, tagIds: List<Long>) {
        dao.clearTagsForNote(noteId)
        val unique = tagIds.distinct()
        if (unique.isNotEmpty()) {
            dao.insertNoteTags(unique.map { NoteTagCrossRef(noteId, it) })
        }
    }

    private fun normalizeDraft(draft: NoteDraft): NoteDraft {
        return when (draft.type) {
            NoteType.LINK -> {
                val url = UrlUtils.normalize(draft.url)
                    ?: throw IllegalArgumentException("Invalid URL")
                val content = draft.content?.trim()?.ifBlank { null }
                val title = draft.title.trim().ifBlank { UrlUtils.hostOf(url).orEmpty() }
                draft.copy(url = url, content = content, title = title)
            }

            NoteType.TEXT -> {
                val content = draft.content?.trim() ?: ""
                require(content.isNotEmpty()) { "Content required" }
                val title = draft.title.trim().ifBlank { firstLineTitle(content) }
                draft.copy(url = null, content = content, title = title)
            }

            NoteType.IMAGE -> {
                require(!draft.imageFileName.isNullOrBlank()) { "Image required" }
                require(!draft.imageMimeType.isNullOrBlank()) { "Image MIME required" }
                require(!draft.imageSha256.isNullOrBlank()) { "Image hash required" }
                require(imageStorage.imageFile(draft.imageFileName) != null) { "Image missing" }
                draft.copy(
                    title = draft.title.trim(),
                    url = null,
                    content = draft.content?.trim()?.ifBlank { null },
                )
            }
        }
    }

    private fun firstLineTitle(text: String): String =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.take(80)
            .orEmpty()

    companion object {
        const val TAG_COLOR_COUNT = 12
    }
}

