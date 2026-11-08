package com.vibecode.minilinknotes

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.vibecode.minilinknotes.data.AppDatabase
import com.vibecode.minilinknotes.data.ImageStorage
import com.vibecode.minilinknotes.data.NotesRepository
import com.vibecode.minilinknotes.data.SettingsStore
import com.vibecode.minilinknotes.data.TitleFetcher
import com.vibecode.minilinknotes.data.backup.BackupManager
import com.vibecode.minilinknotes.widget.LinkNoteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

class MiniLinkNotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.purgeExpiredTrash()
    }
}

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "mini-link-notes.db",
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()

    private val notesDao = database.notesDao()
    val settingsStore = SettingsStore(appContext)
    private val titleFetcher = TitleFetcher()
    val imageStorage = ImageStorage(appContext)

    val repository = NotesRepository(
        database = database,
        dao = notesDao,
        settingsStore = settingsStore,
        titleFetcher = titleFetcher,
        imageStorage = imageStorage,
    )

    val backupManager = BackupManager(
        contentResolver = appContext.contentResolver,
        database = database,
        dao = notesDao,
        imageStorage = imageStorage,
        cacheDir = appContext.cacheDir,
    )

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        applicationScope.launch {
            repository.activeNoteEntities.collect {
                runCatching { LinkNoteWidget().updateAll(appContext) }
            }
        }
    }

    fun purgeExpiredTrash() {
        applicationScope.launch {
            repository.purgeExpired()
            repository.cleanupImageFiles()
        }
    }
}
