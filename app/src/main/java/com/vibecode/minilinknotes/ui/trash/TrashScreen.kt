package com.vibecode.minilinknotes.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibecode.minilinknotes.AppViewModel
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.data.NoteWithTags
import com.vibecode.minilinknotes.util.TimeUtils
import com.vibecode.minilinknotes.util.UrlUtils

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    notes: List<NoteWithTags>,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenLink: (Long, String) -> Unit,
) {
    var pendingPurge by remember { mutableStateOf<NoteWithTags?>(null) }
    var pendingEmptyAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { pendingEmptyAll = true },
                        enabled = notes.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.trash_purge_all),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.trash_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.trash_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notes, key = { it.note.id }) { item ->
                TrashCard(
                    item = item,
                    onRestore = { viewModel.restoreNote(item.note.id) },
                    onPurge = { pendingPurge = item },
                    onOpenLink = {
                        item.note.url?.let { onOpenLink(item.note.id, it) }
                    },
                )
            }
        }
    }

    pendingPurge?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingPurge = null },
            title = { Text(stringResource(R.string.trash_purge_title)) },
            text = { Text(stringResource(R.string.trash_purge_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.purgeNote(item.note.id)
                        pendingPurge = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurge = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (pendingEmptyAll) {
        AlertDialog(
            onDismissRequest = { pendingEmptyAll = false },
            title = { Text(stringResource(R.string.trash_purge_all_title)) },
            text = { Text(stringResource(R.string.trash_purge_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        pendingEmptyAll = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEmptyAll = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TrashCard(
    item: NoteWithTags,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
    onOpenLink: () -> Unit,
) {
    val note = item.note
    val deletedAt = note.deletedAt ?: return
    val daysLeft = TimeUtils.trashDaysLeft(deletedAt)
    val title = note.title.ifBlank {
        if (note.type == NoteType.LINK) {
            stringResource(R.string.note_untitled_link)
        } else {
            stringResource(R.string.note_untitled_text)
        }
    }
    val host = note.url?.let(UrlUtils::hostOf)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.type == NoteType.LINK) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (host != null) {
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!note.content.isNullOrBlank()) {
                Text(
                    text = note.content.replace('\n', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.trash_deleted_at, TimeUtils.formatDateTime(deletedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.trash_days_left, daysLeft),
                style = MaterialTheme.typography.labelSmall,
                color = if (daysLeft <= 3) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (note.type == NoteType.LINK) {
                    TextButton(onClick = onOpenLink) {
                        Text(stringResource(R.string.action_open))
                    }
                }
                TextButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_restore))
                }
                IconButton(onClick = onPurge) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = stringResource(R.string.action_confirm_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
