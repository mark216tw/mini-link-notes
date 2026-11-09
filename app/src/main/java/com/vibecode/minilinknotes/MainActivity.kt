package com.vibecode.minilinknotes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecode.minilinknotes.ui.MiniLinkNotesRoot
import com.vibecode.minilinknotes.ui.theme.MiniLinkNotesTheme
import com.vibecode.minilinknotes.util.ShareParser

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppViewModel
    private var handledIntent: Intent? = null
    private var noteLaunchHandled = false
    private var pendingNoteId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            noteLaunchHandled = savedInstanceState.getBoolean(STATE_NOTE_LAUNCH_HANDLED)
        }

        val container = (application as MiniLinkNotesApp).container
        viewModel = ViewModelProvider(
            this,
            AppViewModel.Factory(
                context = this,
                repository = container.repository,
                settingsStore = container.settingsStore,
                backupManager = container.backupManager,
                reminderScheduler = container.reminderScheduler,
            ),
        )[AppViewModel::class.java]

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MiniLinkNotesTheme(state.settings) { darkTheme ->
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                MiniLinkNotesRoot(
                    viewModel = viewModel,
                    state = state,
                    imageStorage = container.imageStorage,
                    pendingNoteId = pendingNoteId,
                    onPendingNoteHandled = { pendingNoteId = null },
                    onOpenLink = { _, url -> openLink(url) },
                )
            }
        }

        handlePendingNote(intent)
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        noteLaunchHandled = false
        handlePendingNote(intent)
        handleIncomingIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_NOTE_LAUNCH_HANDLED, noteLaunchHandled)
    }

    private fun handlePendingNote(intent: Intent?) {
        if (intent == null || noteLaunchHandled) return
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0) return
        noteLaunchHandled = true
        pendingNoteId = noteId
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null || intent === handledIntent) return

        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_PROCESS_TEXT) {
            return
        }
        handledIntent = intent

        if (intent.action == Intent.ACTION_SEND && intent.isImageShare()) {
            val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: intent.clipData
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.uri
            if (stream == null) {
                viewModel.notifyEmptyShare()
            } else {
                viewModel.handleSharedImage(
                    uri = stream,
                    subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                    caption = intent.getStringExtra(Intent.EXTRA_TEXT),
                )
            }
            return
        }

        if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            val shared = ShareParser.parse(text, null, false)
            if (shared == null) {
                viewModel.notifyEmptyShare()
            } else {
                viewModel.handleSharedContent(shared)
            }
            return
        }

        val plainText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val htmlText = intent.getCharSequenceExtra(Intent.EXTRA_HTML_TEXT)?.toString()
        val text = plainText ?: htmlText
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val isHtml = intent.type == "text/html" || (plainText == null && htmlText != null)
        val shared = ShareParser.parse(text, subject, isHtml)
        if (shared == null) {
            viewModel.notifyEmptyShare()
        } else {
            viewModel.handleSharedContent(shared)
        }
    }

    private fun Intent.isImageShare(): Boolean {
        if (type?.startsWith("image/") == true) return true
        val clipType = clipData?.description?.getMimeType(0)
        return clipType?.startsWith("image/") == true
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.showMessage(R.string.open_link_failed)
        } catch (_: IllegalArgumentException) {
            viewModel.showMessage(R.string.open_link_failed)
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "com.vibecode.minilinknotes.extra.NOTE_ID"
        private const val STATE_NOTE_LAUNCH_HANDLED = "note_launch_handled"
    }
}
