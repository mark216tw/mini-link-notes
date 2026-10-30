package com.vibecode.minilinknotes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActiveNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNote(id: Long): NoteWithTags?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteEntity(id: Long): NoteEntity?

    @Query("SELECT url FROM notes WHERE deletedAt IS NULL AND url IS NOT NULL")
    suspend fun getActiveUrls(): List<String>

    @Query("SELECT content FROM notes WHERE deletedAt IS NULL AND type = :type AND content IS NOT NULL")
    suspend fun getActiveContents(type: NoteType): List<String>

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getActiveNotesWithTags(): List<NoteWithTags>

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isRead = NOT isRead, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleRead(id: Long, updatedAt: Long)

    @Query("UPDATE notes SET isRead = 1, updatedAt = :updatedAt WHERE deletedAt IS NULL AND isRead = 0")
    suspend fun markAllRead(updatedAt: Long): Int

    @Query("UPDATE notes SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDeletedAt(id: Long, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun clearDeletedAt(id: Long, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNotePermanently(id: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeExpired(before: Long): Int

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun purgeAllTrash(): Int

    @Query("UPDATE notes SET title = :title, titleFetchState = :state, titleEdited = :titleEdited, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFetchedTitle(
        id: Long,
        title: String,
        state: TitleFetchState,
        titleEdited: Boolean,
        updatedAt: Long,
    )

    @Query("UPDATE notes SET titleFetchState = :state WHERE id = :id")
    suspend fun setTitleFetchState(id: Long, state: TitleFetchState)

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query(
        """
        SELECT t.*, COUNT(n.id) AS noteCount
        FROM tags t
        LEFT JOIN note_tag nt ON nt.tagId = t.id
        LEFT JOIN notes n ON n.id = nt.noteId AND n.deletedAt IS NULL
        GROUP BY t.id
        ORDER BY t.sortOrder ASC, t.name COLLATE NOCASE ASC
        """,
    )
    fun observeTagsWithCount(): Flow<List<TagWithNoteCount>>

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getTags(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTag(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findTagByName(name: String): TagEntity?

    @Insert
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tags")
    suspend fun maxTagSortOrder(): Int

    @Query("UPDATE tags SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setTagSortOrder(id: Long, sortOrder: Int)

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("SELECT COUNT(*) FROM note_tag nt INNER JOIN notes n ON n.id = nt.noteId WHERE nt.tagId = :tagId AND n.deletedAt IS NULL")
    suspend fun countNotesForTag(tagId: Long): Int

    @Query("DELETE FROM note_tag WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    @Query("DELETE FROM note_tag")
    suspend fun clearNoteTags()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTags(refs: List<NoteTagCrossRef>)
}
