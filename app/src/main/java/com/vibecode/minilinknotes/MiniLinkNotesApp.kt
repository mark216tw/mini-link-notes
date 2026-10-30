package com.vibecode.minilinknotes

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.vibecode.minilinknotes.data.AppDatabase
import com.vibecode.minilinknotes.data.NotesRepository
import com.vibecode.minilinknotes.data.SettingsStore
import com.vibecode.minilinknotes.data.TitleFetcher
import com.vibecode.minilinknotes.data.backup.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    private val notesDao = database.notesDao()
    val settingsStore = SettingsStore(appContext)
    private val titleFetcher = TitleFetcher()

    val repository = NotesRepository(
        database = database,
        dao = notesDao,
        settingsStore = settingsStore,
        titleFetcher = titleFetcher,
    )

    val backupManager = BackupManager(
        contentResolver = appContext.contentResolver,
        database = database,
        dao = notesDao,
    )

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun purgeExpiredTrash() {
        applicationScope.launch {
            repository.purgeExpired()
        }
    }
}
