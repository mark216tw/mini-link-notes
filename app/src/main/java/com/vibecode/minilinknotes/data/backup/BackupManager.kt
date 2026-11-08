package com.vibecode.minilinknotes.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.vibecode.minilinknotes.data.AppDatabase
import com.vibecode.minilinknotes.data.ImageStorage
import com.vibecode.minilinknotes.data.NoteEntity
import com.vibecode.minilinknotes.data.NoteTagCrossRef
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.data.NotesDao
import com.vibecode.minilinknotes.data.NotesRepository
import com.vibecode.minilinknotes.data.StoredImage
import com.vibecode.minilinknotes.data.TagEntity
import com.vibecode.minilinknotes.util.UrlUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val contentResolver: ContentResolver,
    private val database: AppDatabase,
    private val dao: NotesDao,
    private val imageStorage: ImageStorage,
    private val cacheDir: File,
) {

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val notes = dao.getActiveNotesWithTags()
        val tags = dao.getTags()
        val images = linkedMapOf<String, Pair<File, BackupImage>>()

        val backupNotes = notes.map { noteWithTags ->
            val note = noteWithTags.note
            val backupImage = if (note.type == NoteType.IMAGE) {
                val file = imageStorage.imageFile(note.imageFileName)
                val hash = note.imageSha256
                val mime = note.imageMimeType
                if (file != null && !hash.isNullOrBlank() && !mime.isNullOrBlank()) {
                    val extension = file.extension.takeIf { it.isNotBlank() } ?: "img"
                    val path = "$IMAGE_ENTRY_DIRECTORY/$hash.$extension"
                    images.putIfAbsent(path, file to BackupImage(path, mime, hash))
                    images[path]?.second
                } else {
                    null
                }
            } else {
                null
            }
            BackupNote(
                type = note.type.name,
                title = note.title,
                url = note.url,
                content = note.content,
                isRead = note.isRead,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                tags = noteWithTags.tags.map { it.name },
                image = backupImage,
            )
        }

        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            tags = tags.map { tag ->
                BackupTag(
                    name = tag.name,
                    colorIndex = tag.colorIndex,
                    sortOrder = tag.sortOrder,
                )
            },
            notes = backupNotes,
        )
        val payload = BackupCodec.encode(file).toByteArray(Charsets.UTF_8)

        try {
            contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                ZipOutputStream(stream.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                    zip.write(payload)
                    zip.closeEntry()
                    images.forEach { (path, image) ->
                        zip.putNextEntry(ZipEntry(path))
                        image.first.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: throw IOException("Cannot open output")
        } catch (error: IOException) {
            throw BackupException(ImportError.IO, error)
        }
        notes.size
    }

    suspend fun importFrom(uri: Uri, mode: ImportMode): Int = withContext(Dispatchers.IO) {
        val archive = File.createTempFile("minilinknotes-import", ".tmp", cacheDir)
        try {
            val copied = contentResolver.openInputStream(uri)?.use { input ->
                archive.outputStream().use { output -> input.copyLimitedTo(output, MAX_ARCHIVE_BYTES) }
            } ?: throw IOException("Cannot open input")
            if (copied <= 4) throw BackupException(ImportError.INVALID)

            if (archive.hasZipMagic()) {
                importZip(archive, mode)
            } else {
                importJson(archive, mode)
            }
        } catch (error: BackupException) {
            throw error
        } catch (error: IOException) {
            throw BackupException(ImportError.IO, error)
        } catch (error: Exception) {
            throw BackupException(ImportError.INVALID, error)
        } finally {
            archive.delete()
        }
    }

    private suspend fun importJson(archive: File, mode: ImportMode): Int {
        val raw = FileInputStream(archive).use { it.readLimited(MAX_JSON_BYTES) }
        val file = BackupCodec.decode(String(raw, Charsets.UTF_8))
        val oldImages = if (mode == ImportMode.REPLACE) dao.getAllImageFileNames() else emptyList()
        val imported = database.withTransaction {
            importInternal(file, mode, emptyMap())
        }
        if (mode == ImportMode.REPLACE) {
            oldImages.forEach { imageStorage.delete(it) }
        }
        return imported.count
    }

    private suspend fun importZip(archive: File, mode: ImportMode): Int {
        ZipFile(archive).use { zip ->
            val manifestEntry = zip.getEntry(MANIFEST_ENTRY)
                ?: throw BackupException(ImportError.INVALID)
            if (manifestEntry.size > MAX_JSON_BYTES) throw BackupException(ImportError.INVALID)
            val manifest = zip.getInputStream(manifestEntry).use { it.readLimited(MAX_JSON_BYTES) }
            val file = BackupCodec.decode(String(manifest, Charsets.UTF_8))

            val imagesByNote = mutableMapOf<Int, StoredImage>()
            file.notes.forEachIndexed { index, note ->
                if (note.type != NoteType.IMAGE.name) return@forEachIndexed
                val image = note.image ?: return@forEachIndexed
                if (!image.path.isSafeEntryName()) return@forEachIndexed
                val entry = zip.getEntry(image.path) ?: return@forEachIndexed
                if (entry.size > MAX_IMAGE_BYTES) return@forEachIndexed
                val bytes = runCatching {
                    zip.getInputStream(entry).use { it.readLimited(MAX_IMAGE_BYTES.toInt()) }
                }.getOrNull() ?: return@forEachIndexed
                if (!bytes.sha256Hex().equals(image.sha256, ignoreCase = true)) return@forEachIndexed
                val stored = runCatching {
                    imageStorage.import(ByteArrayInputStream(bytes), image.mimeType)
                }.getOrNull() ?: return@forEachIndexed
                if (!stored.sha256.equals(image.sha256, ignoreCase = true)) {
                    imageStorage.delete(stored.fileName)
                    return@forEachIndexed
                }
                imagesByNote[index] = stored
            }

            val oldImages = if (mode == ImportMode.REPLACE) dao.getAllImageFileNames() else emptyList()
            val result = database.withTransaction {
                importInternal(file, mode, imagesByNote)
            }
            if (mode == ImportMode.REPLACE) {
                oldImages.forEach { imageStorage.delete(it) }
            }
            imagesByNote.values
                .filter { it.fileName !in result.usedImages }
                .forEach { imageStorage.delete(it.fileName) }
            return result.count
        }
    }

    private data class ImportResult(
        val count: Int,
        val usedImages: Set<String>,
    )

    private suspend fun importInternal(
        file: BackupFile,
        mode: ImportMode,
        imagesByNote: Map<Int, StoredImage>,
    ): ImportResult {
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
        val existingImageHashes: MutableSet<String> = if (mode == ImportMode.MERGE) {
            dao.getActiveImageHashes().mapTo(mutableSetOf()) { it.lowercase() }
        } else {
            mutableSetOf()
        }

        val usedImages = mutableSetOf<String>()
        var imported = 0
        file.notes.forEachIndexed { index, backupNote ->
            val type = runCatching { NoteType.valueOf(backupNote.type) }.getOrNull() ?: return@forEachIndexed
            val url = backupNote.url?.let(UrlUtils::normalize)
            if (type == NoteType.LINK && url == null) return@forEachIndexed
            val content = backupNote.content?.trim()?.ifBlank { null }
            if (type == NoteType.TEXT && content == null) return@forEachIndexed

            val storedImage = if (type == NoteType.IMAGE) {
                imagesByNote[index] ?: return@forEachIndexed
            } else {
                null
            }

            if (mode == ImportMode.MERGE) {
                if (url != null && !existingUrlKeys.add(UrlUtils.dedupeKey(url))) return@forEachIndexed
                if (type == NoteType.TEXT && content != null && !existingTextKeys.add(content)) {
                    return@forEachIndexed
                }
                if (storedImage != null && !existingImageHashes.add(storedImage.sha256.lowercase())) {
                    return@forEachIndexed
                }
            }

            val now = System.currentTimeMillis()
            val createdAt = backupNote.createdAt.takeIf { it > 0 } ?: now
            val noteId = dao.insertNote(
                NoteEntity(
                    type = type,
                    title = backupNote.title.trim(),
                    url = url,
                    content = content,
                    imageFileName = storedImage?.fileName,
                    imageMimeType = storedImage?.mimeType,
                    imageSha256 = storedImage?.sha256,
                    isRead = backupNote.isRead,
                    createdAt = createdAt,
                    updatedAt = backupNote.updatedAt.takeIf { it > 0 } ?: createdAt,
                    titleEdited = backupNote.title.isNotBlank(),
                ),
            )
            storedImage?.let { usedImages += it.fileName }
            val refs = backupNote.tags
                .mapNotNull { tagIdByName[it.trim().lowercase()] }
                .distinct()
                .map { NoteTagCrossRef(noteId, it) }
            if (refs.isNotEmpty()) dao.insertNoteTags(refs)
            imported++
        }
        return ImportResult(imported, usedImages)
    }

    private fun File.hasZipMagic(): Boolean {
        val magic = ByteArray(4)
        val read = FileInputStream(this).use { it.read(magic) }
        return read == 4 &&
            magic[0] == 'P'.code.toByte() &&
            magic[1] == 'K'.code.toByte() &&
            magic[2] == 3.toByte() &&
            magic[3] == 4.toByte()
    }

    private fun String.isSafeEntryName(): Boolean {
        if (isBlank() || startsWith("/") || contains('\\')) return false
        return split('/').none { it.isEmpty() || it == "." || it == ".." }
    }

    private fun InputStream.copyLimitedTo(output: java.io.OutputStream, limit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            if (total > limit) throw IOException("Backup is too large")
            output.write(buffer, 0, read)
        }
        return total
    }

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            if (total + read > limit) throw IOException("Entry is too large")
            output.write(buffer, 0, read)
            total += read
        }
        return output.toByteArray()
    }

    private fun ByteArray.sha256Hex(): String =
        MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

    companion object {
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val IMAGE_ENTRY_DIRECTORY = "images"
        private const val MAX_JSON_BYTES = 20 * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 25L * 1024L * 1024L
        private const val MAX_ARCHIVE_BYTES = 400L * 1024L * 1024L
    }
}
