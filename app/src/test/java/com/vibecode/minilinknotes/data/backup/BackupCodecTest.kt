package com.vibecode.minilinknotes.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {

    @Test
    fun roundTrip_preservesTagsAndNotes() {
        val file = BackupFile(
            exportedAt = 1234L,
            tags = listOf(BackupTag("工作", colorIndex = 2, sortOrder = 0)),
            notes = listOf(
                BackupNote(
                    type = "LINK",
                    title = "範例",
                    url = "https://example.com",
                    content = "說明",
                    isRead = true,
                    createdAt = 100L,
                    updatedAt = 200L,
                    tags = listOf("工作"),
                ),
                BackupNote(type = "TEXT", title = "文字", content = "內容", createdAt = 300L),
            ),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(file))
        assertEquals(file, decoded)
    }

    @Test
    fun roundTrip_preservesImageMetadata() {
        val file = BackupFile(
            exportedAt = 1L,
            notes = listOf(
                BackupNote(
                    type = "IMAGE",
                    title = "照片",
                    createdAt = 2L,
                    image = BackupImage(
                        path = "images/abc.jpg",
                        mimeType = "image/jpeg",
                        sha256 = "abc",
                    ),
                ),
            ),
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(file))
        assertEquals(file, decoded)
    }

    @Test
    fun decode_acceptsLegacySchemaWithoutImages() {
        val raw = """
            {
              "schemaVersion": 1,
              "exportedAt": 1,
              "tags": [],
              "notes": [{
                "type": "TEXT",
                "title": "舊筆記",
                "content": "內容",
                "createdAt": 1
              }]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(raw)
        assertEquals(1, decoded.schemaVersion)
        assertEquals(null, decoded.notes.single().image)
    }

    @Test
    fun decode_rejectsInvalidJson() {
        assertThrows(BackupException::class.java) {
            BackupCodec.decode("{ not json")
        }
    }

    @Test
    fun decode_rejectsNewerSchemaVersion() {
        val raw = """
            {
              "schemaVersion": 99,
              "exportedAt": 1,
              "tags": [],
              "notes": []
            }
        """.trimIndent()
        val error = assertThrows(BackupException::class.java) {
            BackupCodec.decode(raw)
        }
        assertEquals(ImportError.NEWER_VERSION, error.reason)
    }
}
