package com.vibecode.minilinknotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vibecode.minilinknotes.AppViewModel
import com.vibecode.minilinknotes.NotesUiState
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.SnackbarAction
import com.vibecode.minilinknotes.ui.about.AboutScreen
import com.vibecode.minilinknotes.ui.editor.EditorScreen
import com.vibecode.minilinknotes.ui.notes.NotesScreen
import com.vibecode.minilinknotes.ui.settings.SettingsScreen
import com.vibecode.minilinknotes.ui.tags.TagsScreen
import com.vibecode.minilinknotes.ui.trash.TrashScreen

private object Routes {
    const val NOTES = "notes"
    const val EDITOR = "editor?noteId={noteId}"
    const val TAGS = "tags"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun editor(noteId: Long? = null): String =
        if (noteId == null || noteId <= 0) "editor" else "editor?noteId=$noteId"
}

@Composable
fun MiniLinkNotesRoot(
    viewModel: AppViewModel,
    state: NotesUiState,
    onOpenLink: (Long, String) -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val actionLabel = when (event.action) {
                is SnackbarAction.EditNote -> context.getString(R.string.action_edit)
                is SnackbarAction.UndoDelete -> context.getString(R.string.action_undo)
                null -> null
            }
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = actionLabel,
                withDismissAction = event.action == null,
                duration = if (event.action == null) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (val action = event.action) {
                    is SnackbarAction.EditNote -> navController.navigate(Routes.editor(action.noteId)) {
                        launchSingleTop = true
                    }

                    is SnackbarAction.UndoDelete -> viewModel.restoreNote(action.noteId)
                    null -> Unit
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.NOTES,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.NOTES) {
                NotesScreen(
                    state = state,
                    viewModel = viewModel,
                    onAddNote = { navController.navigate(Routes.editor()) },
                    onEditNote = { noteId -> navController.navigate(Routes.editor(noteId)) },
                    onOpenLink = onOpenLink,
                    onOpenTags = { navController.navigate(Routes.TAGS) },
                    onOpenTrash = { navController.navigate(Routes.TRASH) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                EditorScreen(
                    noteId = entry.arguments?.getLong("noteId") ?: -1L,
                    tags = state.tags,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onManageTags = { navController.navigate(Routes.TAGS) },
                )
            }

            composable(Routes.TAGS) {
                TagsScreen(
                    tags = state.tagsWithCount,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.TRASH) {
                TrashScreen(
                    notes = state.trash,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenLink = onOpenLink,
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = state.settings,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }

            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 88.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionColor = MaterialTheme.colorScheme.primary,
                actionContentColor = MaterialTheme.colorScheme.primary,
                dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
