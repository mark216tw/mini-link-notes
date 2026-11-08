package com.vibecode.minilinknotes.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val tags: List<BackupTag> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
)

@Serializable
data class BackupTag(
    val name: String,
    val colorIndex: Int,
    val sortOrder: Int,
)

@Serializable
data class BackupImage(
    val path: String,
    val mimeType: String,
    val sha256: String,
)

@Serializable
data class BackupNote(
    val type: String,
    val title: String = "",
    val url: String? = null,
    val content: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long = 0,
    val tags: List<String> = emptyList(),
    val image: BackupImage? = null,
)

const val CURRENT_SCHEMA_VERSION = 2

enum class ImportMode {
    MERGE,
    REPLACE,
}

enum class ImportError {
    INVALID,
    NEWER_VERSION,
    IO,
}

class BackupException(val reason: ImportError, cause: Throwable? = null) : Exception(reason.name, cause)
