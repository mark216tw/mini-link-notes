package com.vibecode.minilinknotes.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter

enum class NoteType {
    LINK,
    TEXT,
}

enum class TitleFetchState {
    NONE,
    SUCCESS,
    FAILED,
}

enum class DisplayMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class SortField {
    CREATED_AT,
    TITLE,
    TAG,
}

data class AppSettings(
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val autoFetchTitle: Boolean = true,
    val sortField: SortField = SortField.CREATED_AT,
    val sortAscending: Boolean = false,
)

@Entity(
    tableName = "notes",
    indices = [
        Index("deletedAt"),
        Index("createdAt"),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: NoteType,
    val title: String = "",
    val url: String? = null,
    val content: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val titleFetchState: TitleFetchState = TitleFetchState.NONE,
    val titleEdited: Boolean = false,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int,
    val sortOrder: Int,
)

@Entity(
    tableName = "note_tag",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagId: Long,
)

data class NoteWithTags(
    @Embedded val note: NoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity> = emptyList(),
)

data class TagWithNoteCount(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "noteCount") val noteCount: Int,
)

class Converters {
    @TypeConverter
    fun noteTypeToString(value: NoteType): String = value.name

    @TypeConverter
    fun stringToNoteType(value: String): NoteType = NoteType.valueOf(value)

    @TypeConverter
    fun titleFetchStateToString(value: TitleFetchState): String = value.name

    @TypeConverter
    fun stringToTitleFetchState(value: String): TitleFetchState = TitleFetchState.valueOf(value)
}
