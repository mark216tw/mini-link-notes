package com.vibecode.minilinknotes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_notes_isRead`")
                db.execSQL("DROP INDEX IF EXISTS `index_notes_url`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `imageFileName` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `imageMimeType` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `imageSha256` TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notes_imageSha256` ON `notes` (`imageSha256`)",
                )
            }
        }
    }
}
