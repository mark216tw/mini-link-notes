package com.vibecode.minilinknotes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MiniLinkNotesApp).container
        viewModel = ViewModelProvider(
            this,
            AppViewModel.Factory(
                context = this,
                repository = container.repository,
                settingsStore = container.settingsStore,
                backupManager = container.backupManager,
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
                    onOpenLink = { _, url -> openLink(url) },
                )
            }
        }

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null || intent === handledIntent) return
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_PROCESS_TEXT) {
            return
        }
        handledIntent = intent

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
}

