package com.vibecode.minilinknotes.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object BackupCodec {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(raw: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), raw)
        } catch (error: SerializationException) {
            throw BackupException(ImportError.INVALID, error)
        } catch (error: IllegalArgumentException) {
            throw BackupException(ImportError.INVALID, error)
        }
        if (file.schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw BackupException(ImportError.NEWER_VERSION)
        }
        return file
    }
}
