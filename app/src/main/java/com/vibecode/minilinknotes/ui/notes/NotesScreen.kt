package com.vibecode.minilinknotes.ui.notes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibecode.minilinknotes.AppViewModel
import com.vibecode.minilinknotes.NotesUiState
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.data.NoteWithTags
import com.vibecode.minilinknotes.data.SortField
import com.vibecode.minilinknotes.data.TagEntity
import com.vibecode.minilinknotes.data.TagWithNoteCount
import com.vibecode.minilinknotes.ui.theme.tagColor
import com.vibecode.minilinknotes.util.UrlUtils
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    state: NotesUiState,
    viewModel: AppViewModel,
    onAddNote: () -> Unit,
    onEditNote: (Long) -> Unit,
    onOpenLink: (Long, String) -> Unit,
    onOpenTags: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var sortExpanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val shareText = { text: String ->
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.share_chooser_title)),
            )
        } catch (_: ActivityNotFoundException) {
            viewModel.showMessage(R.string.share_failed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.notes_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val subtitle = when {
                            state.hasFilter -> stringResource(R.string.notes_result_count, state.notes.size)
                            state.totalCount > 0 && state.unreadCount > 0 -> stringResource(
                                R.string.notes_total_unread_count,
                                state.totalCount,
                                state.unreadCount,
                            )

                            state.totalCount > 0 -> stringResource(R.string.notes_total_count, state.totalCount)
                            else -> null
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrash) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.trash_title),
                        )
                    }
                    Box {
                        IconButton(onClick = {
                            sortExpanded = false
                            moreExpanded = true
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                            )
                        }
                        DropdownMenu(
                            expanded = moreExpanded,
                            onDismissRequest = { moreExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_mark_all_read)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DoneAll, contentDescription = null)
                                },
                                enabled = state.unreadCount > 0,
                                onClick = {
                                    moreExpanded = false
                                    viewModel.markAllRead()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_manage_tags)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Label, contentDescription = null)
                                },
                                onClick = {
                                    moreExpanded = false
                                    onOpenTags()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_title)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, contentDescription = null)
                                },
                                onClick = {
                                    moreExpanded = false
                                    onOpenSettings()
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.action_add),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(onClick = {
                        moreExpanded = false
                        sortExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = stringResource(R.string.sort_menu_title),
                        )
                    }
                    SortMenu(
                        expanded = sortExpanded,
                        settings = state.settings,
                        onDismiss = { sortExpanded = false },
                        onSelect = { field, ascending ->
                            sortExpanded = false
                            viewModel.setSort(field, ascending)
                        },
                    )
                }
            }

            if (state.tagsWithCount.isNotEmpty() || state.unreadCount > 0 || state.unreadOnly) {
                TagFilterRow(
                    tags = state.tagsWithCount,
                    selectedTagIds = state.selectedTagIds,
                    unreadOnly = state.unreadOnly,
                    unreadCount = state.unreadCount,
                    dark = dark,
                    onClear = viewModel::clearFilters,
                    onToggleTag = viewModel::toggleTagFilter,
                    onToggleUnread = viewModel::toggleUnreadOnly,
                )
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.notes.isEmpty() -> {
                    EmptyNotes(
                        hasFilter = state.hasFilter,
                        onClearFilter = {
                            viewModel.setQuery("")
                            viewModel.clearFilters()
                        },
                        onAddNote = onAddNote,
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.notes, key = { it.note.id }) { noteWithTags ->
                            NoteCard(
                                noteWithTags = noteWithTags,
                                dark = dark,
                                onOpen = {
                                    val url = noteWithTags.note.url
                                    if (noteWithTags.note.type == NoteType.LINK && url != null) {
                                        onOpenLink(noteWithTags.note.id, url)
                                    } else {
                                        onEditNote(noteWithTags.note.id)
                                    }
                                },
                                onToggleRead = { viewModel.toggleRead(noteWithTags.note.id) },
                                onEdit = { onEditNote(noteWithTags.note.id) },
                                onOpenLink = {
                                    noteWithTags.note.url?.let { onOpenLink(noteWithTags.note.id, it) }
                                },
                                onCopy = { text ->
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(
                                                ClipData.newPlainText(
                                                    context.getString(R.string.app_name),
                                                    text,
                                                ),
                                            ),
                                        )
                                        viewModel.showMessage(R.string.copied)
                                    }
                                },
                                onShare = shareText,
                                onDelete = { viewModel.deleteNote(noteWithTags.note.id) },
                                onTagClick = viewModel::toggleTagFilter,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    settings: com.vibecode.minilinknotes.data.AppSettings,
    onDismiss: () -> Unit,
    onSelect: (SortField, Boolean) -> Unit,
) {
    val options = listOf(
        Triple(SortField.CREATED_AT, false, stringResource(R.string.sort_created_desc)),
        Triple(SortField.CREATED_AT, true, stringResource(R.string.sort_created_asc)),
        Triple(SortField.TITLE, true, stringResource(R.string.sort_title_asc)),
        Triple(SortField.TITLE, false, stringResource(R.string.sort_title_desc)),
        Triple(SortField.TAG, true, stringResource(R.string.sort_tag)),
    )
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        options.forEach { (field, ascending, label) ->
            val selected = settings.sortField == field &&
                (field == SortField.TAG || settings.sortAscending == ascending)
            DropdownMenuItem(
                text = {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                },
                trailingIcon = {
                    if (selected) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                    }
                },
                onClick = { onSelect(field, ascending) },
            )
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<TagWithNoteCount>,
    selectedTagIds: Set<Long>,
    unreadOnly: Boolean,
    unreadCount: Int,
    dark: Boolean,
    onClear: () -> Unit,
    onToggleTag: (Long) -> Unit,
    onToggleUnread: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedTagIds.isEmpty() && !unreadOnly,
                onClick = onClear,
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        item {
            FilterChip(
                selected = unreadOnly,
                onClick = onToggleUnread,
                label = {
                    Text(
                        text = if (unreadCount > 0) {
                            stringResource(R.string.filter_unread_count, unreadCount)
                        } else {
                            stringResource(R.string.filter_unread)
                        },
                    )
                },
            )
        }
        items(tags, key = { it.tag.id }) { item ->
            val tag = item.tag
            FilterChip(
                selected = tag.id in selectedTagIds,
                onClick = { onToggleTag(tag.id) },
                label = {
                    Text(
                        text = stringResource(R.string.tag_filter_count, tag.name, item.noteCount),
                        modifier = Modifier.widthIn(max = 160.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(tagColor(tag.colorIndex, dark), CircleShape),
                    )
                },
            )
        }
    }
}

@Composable
private fun EmptyNotes(
    hasFilter: Boolean,
    onClearFilter: () -> Unit,
    onAddNote: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (hasFilter) R.string.empty_search_title else R.string.empty_notes_title,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (hasFilter) R.string.empty_search_message else R.string.empty_notes_message,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        if (hasFilter) {
            TextButton(onClick = onClearFilter) {
                Text(stringResource(R.string.action_clear_filter))
            }
        } else {
            TextButton(onClick = onAddNote) {
                Text(stringResource(R.string.action_add))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCard(
    noteWithTags: NoteWithTags,
    dark: Boolean,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onEdit: () -> Unit,
    onOpenLink: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
    onTagClick: (Long) -> Unit,
) {
    val note = noteWithTags.note
    var menuExpanded by remember { mutableStateOf(false) }
    val isLink = note.type == NoteType.LINK
    val title = note.title.ifBlank {
        if (isLink) stringResource(R.string.note_untitled_link) else stringResource(R.string.note_untitled_text)
    }
    val host = note.url?.let(UrlUtils::hostOf)
    val snippet = note.content?.trim()?.replace('\n', ' ')
    val showSnippet = !snippet.isNullOrBlank() && !(isLink && snippet == note.url)
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        note.createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(onClick = onToggleRead) {
                Icon(
                    imageVector = if (note.isRead) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = stringResource(
                        if (note.isRead) R.string.action_mark_unread else R.string.action_mark_read,
                    ),
                    tint = if (note.isRead) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLink) {
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
                        fontWeight = if (note.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (isLink && host != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (showSnippet) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = snippet!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isLink) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (noteWithTags.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        noteWithTags.tags.take(4).forEach { tag ->
                            TagPill(
                                tag = tag,
                                dark = dark,
                                onClick = { onTagClick(tag.id) },
                            )
                        }
                        val remaining = noteWithTags.tags.size - 4
                        if (remaining > 0) {
                            Text(
                                text = stringResource(R.string.tag_more_count, remaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    if (isLink) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_open)) },
                            leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenLink()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isLink) R.string.action_copy_url else R.string.action_copy_content,
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            val value = if (isLink) note.url else note.content
                            if (value != null) onCopy(value)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (isLink) R.string.action_share_url else R.string.action_share_content,
                                ),
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            val value = if (isLink) note.url else note.content
                            if (value != null) onShare(value)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TagPill(tag: TagEntity, dark: Boolean, onClick: () -> Unit) {
    val color = tagColor(tag.colorIndex, dark)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = tag.name,
            modifier = Modifier.widthIn(max = 140.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

