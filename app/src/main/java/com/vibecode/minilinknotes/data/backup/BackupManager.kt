package com.vibecode.minilinknotes.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.vibecode.minilinknotes.data.AppDatabase
import com.vibecode.minilinknotes.data.NoteEntity
import com.vibecode.minilinknotes.data.NoteTagCrossRef
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.data.NotesDao
import com.vibecode.minilinknotes.data.NotesRepository
import com.vibecode.minilinknotes.data.TagEntity
import com.vibecode.minilinknotes.util.UrlUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val contentResolver: ContentResolver,
    private val database: AppDatabase,
    private val dao: NotesDao,
) {

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val notes = dao.getActiveNotesWithTags()
        val tags = dao.getTags()
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            tags = tags.map { tag ->
                BackupTag(
                    name = tag.name,
                    colorIndex = tag.colorIndex,
                    sortOrder = tag.sortOrder,
                )
            },
            notes = notes.map { noteWithTags ->
                BackupNote(
                    type = noteWithTags.note.type.name,
                    title = noteWithTags.note.title,
                    url = noteWithTags.note.url,
                    content = noteWithTags.note.content,
                    isRead = noteWithTags.note.isRead,
                    createdAt = noteWithTags.note.createdAt,
                    updatedAt = noteWithTags.note.updatedAt,
                    tags = noteWithTags.tags.map { it.name },
                )
            },
        )
        val payload = BackupCodec.encode(file).toByteArray(Charsets.UTF_8)
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(payload) }
                ?: throw IOException("Cannot open output")
        } catch (error: IOException) {
            throw BackupException(ImportError.IO, error)
        }
        notes.size
    }

    suspend fun importFrom(uri: Uri, mode: ImportMode): Int = withContext(Dispatchers.IO) {
        val raw = try {
            contentResolver.openInputStream(uri)?.use { it.readLimited(MAX_IMPORT_BYTES) }
                ?: throw IOException("Cannot open input")
        } catch (error: IOException) {
            throw BackupException(ImportError.IO, error)
        }
        val file = BackupCodec.decode(String(raw, Charsets.UTF_8))

        database.withTransaction {
            importInternal(file, mode)
        }
    }

    private suspend fun importInternal(file: BackupFile, mode: ImportMode): Int {
        if (mode == ImportMode.REPLACE) {
            dao.clearNoteTags()
            dao.clearNotes()
            dao.clearTags()
        }

        val existingTags = dao.getTags()
        val tagIdByName = existingTags.associate { it.name.lowercase() to it.id }.toMutableMap()
        var nextSortOrder = if (mode == ImportMode.REPLACE) 0 else dao.maxTagSortOrder() + 1

        val incomingTags = file.tags
            .sortedBy { it.sortOrder }
            .distinctBy { it.name.trim().lowercase() }

        for (tag in incomingTags) {
            val name = tag.name.trim()
            if (name.isEmpty()) continue
            val key = name.lowercase()
            if (tagIdByName.containsKey(key)) continue
            val id = dao.insertTag(
                TagEntity(
                    name = name,
                    colorIndex = tag.colorIndex.coerceIn(0, NotesRepository.TAG_COLOR_COUNT - 1),
                    sortOrder = nextSortOrder++,
                ),
            )
            tagIdByName[key] = id
        }

        val existingUrlKeys: MutableSet<String> = if (mode == ImportMode.MERGE) {
            dao.getActiveUrls().mapTo(mutableSetOf(), UrlUtils::dedupeKey)
        } else {
            mutableSetOf()
        }
        val existingTextKeys: MutableSet<String> = if (mode == ImportMode.MERGE) {
            dao.getActiveContents(NoteType.TEXT).toMutableSet()
        } else {
            mutableSetOf()
        }

        var imported = 0
        for (backupNote in file.notes) {
            val type = runCatching { NoteType.valueOf(backupNote.type) }.getOrNull() ?: continue
            val url = backupNote.url?.let(UrlUtils::normalize)
            if (type == NoteType.LINK && url == null) continue
            val content = backupNote.content?.trim()?.ifBlank { null }
            if (type == NoteType.TEXT && content == null) continue

            if (mode == ImportMode.MERGE) {
                if (url != null && !existingUrlKeys.add(UrlUtils.dedupeKey(url))) continue
                if (type == NoteType.TEXT && content != null && !existingTextKeys.add(content)) continue
            }

            val now = System.currentTimeMillis()
            val createdAt = backupNote.createdAt.takeIf { it > 0 } ?: now
            val noteId = dao.insertNote(
                NoteEntity(
                    type = type,
                    title = backupNote.title.trim(),
                    url = url,
                    content = content,
                    isRead = backupNote.isRead,
                    createdAt = createdAt,
                    updatedAt = backupNote.updatedAt.takeIf { it > 0 } ?: createdAt,
                    titleEdited = backupNote.title.isNotBlank(),
                ),
            )
            val refs = backupNote.tags
                .mapNotNull { tagIdByName[it.trim().lowercase()] }
                .distinct()
                .map { NoteTagCrossRef(noteId, it) }
            if (refs.isNotEmpty()) dao.insertNoteTags(refs)
            imported++
        }
        return imported
    }

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            val allowed = minOf(read, limit - total)
            if (allowed <= 0) break
            output.write(buffer, 0, allowed)
            total += allowed
            if (total >= limit) break
        }
        return output.toByteArray()
    }

    companion object {
        private const val MAX_IMPORT_BYTES = 20 * 1024 * 1024
    }
}
