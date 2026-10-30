package com.vibecode.minilinknotes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.vibecode.minilinknotes.AppViewModel
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.NoteDraft
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.data.TagEntity
import com.vibecode.minilinknotes.ui.theme.tagColor
import com.vibecode.minilinknotes.util.UrlUtils
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    noteId: Long,
    tags: List<TagEntity>,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onManageTags: () -> Unit,
) {
    val isNew = noteId <= 0
    val scope = rememberCoroutineScope()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var initialized by rememberSaveable(noteId) { mutableStateOf(isNew) }
    var typeName by rememberSaveable(noteId) { mutableStateOf(NoteType.LINK.name) }
    var title by rememberSaveable(noteId) { mutableStateOf("") }
    var url by rememberSaveable(noteId) { mutableStateOf("") }
    var content by rememberSaveable(noteId) { mutableStateOf("") }
    var selectedTagIds by rememberSaveable(noteId) { mutableStateOf(setOf<Long>()) }
    var titleManuallyEdited by rememberSaveable(noteId) { mutableStateOf(false) }
    var initialType by rememberSaveable(noteId) { mutableStateOf(NoteType.LINK.name) }
    var initialTitle by rememberSaveable(noteId) { mutableStateOf("") }
    var initialUrl by rememberSaveable(noteId) { mutableStateOf("") }
    var initialContent by rememberSaveable(noteId) { mutableStateOf("") }
    var initialTagIds by rememberSaveable(noteId) { mutableStateOf(setOf<Long>()) }

    var isSaving by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var infoRes by remember { mutableStateOf<Int?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var showDiscard by remember { mutableStateOf(false) }

    val type = runCatching { NoteType.valueOf(typeName) }.getOrDefault(NoteType.LINK)

    LaunchedEffect(noteId) {
        if (initialized) return@LaunchedEffect
        val noteWithTags = viewModel.getNote(noteId)
        if (noteWithTags == null) {
            onBack()
            return@LaunchedEffect
        }
        val note = noteWithTags.note
        typeName = note.type.name
        title = note.title
        url = note.url.orEmpty()
        content = note.content.orEmpty()
        selectedTagIds = noteWithTags.tags.map { it.id }.toSet()
        titleManuallyEdited = note.titleEdited
        initialType = note.type.name
        initialTitle = title
        initialUrl = url
        initialContent = content
        initialTagIds = selectedTagIds
        initialized = true
    }

    val hasChanges = if (isNew) {
        title.isNotBlank() || url.isNotBlank() || content.isNotBlank() || selectedTagIds.isNotEmpty() ||
            typeName != NoteType.LINK.name
    } else {
        typeName != initialType || title != initialTitle || url != initialUrl ||
            content != initialContent || selectedTagIds != initialTagIds
    }

    val requestBack = {
        if (hasChanges) showDiscard = true else onBack()
    }

    BackHandler(enabled = hasChanges) { showDiscard = true }

    val validTagIds = selectedTagIds.filter { id -> tags.any { it.id == id } }.toSet()

    val submit: () -> Unit = submit@{
        val draft = NoteDraft(
            type = type,
            title = title,
            url = if (type == NoteType.LINK) url else null,
            content = content.ifBlank { null },
            tagIds = validTagIds.toList(),
            titleManuallyEdited = titleManuallyEdited,
        )
        val invalid = when {
            type == NoteType.LINK && UrlUtils.normalize(url) == null -> R.string.editor_link_required
            type == NoteType.TEXT && content.isBlank() -> R.string.editor_content_required
            else -> null
        }
        if (invalid != null) {
            errorRes = invalid
            return@submit
        }
        scope.launch {
            isSaving = true
            errorRes = null
            val result = viewModel.saveNote(noteId.takeIf { it > 0 }, draft)
            isSaving = false
            result.onSuccess { onBack() }
                .onFailure { error ->
                    errorRes = when {
                        error is IllegalArgumentException && type == NoteType.LINK ->
                            R.string.editor_link_required

                        error is IllegalArgumentException -> R.string.editor_content_required
                        else -> R.string.editor_save_failed
                    }
                }
        }
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            !isNew -> stringResource(R.string.editor_edit)
                            type == NoteType.TEXT -> stringResource(R.string.editor_new_text)
                            else -> stringResource(R.string.editor_new_link)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = submit,
                        enabled = !isSaving && initialized,
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (!initialized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.editor_type),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == NoteType.LINK,
                    onClick = {
                        typeName = NoteType.LINK.name
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.note_type_link)) },
                    leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                )
                FilterChip(
                    selected = type == NoteType.TEXT,
                    onClick = {
                        typeName = NoteType.TEXT.name
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.note_type_text)) },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleManuallyEdited = true
                    errorRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.editor_title)) },
                placeholder = { Text(stringResource(R.string.editor_title_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            if (type == NoteType.LINK) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorRes = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editor_url)) },
                    placeholder = { Text(stringResource(R.string.editor_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isFetching = true
                                infoRes = null
                                val result = viewModel.previewTitle(url)
                                isFetching = false
                                result.onSuccess { fetched ->
                                    title = fetched
                                    titleManuallyEdited = false
                                    infoRes = R.string.editor_fetch_success
                                }.onFailure {
                                    infoRes = R.string.editor_fetch_failed
                                }
                            }
                        },
                        enabled = !isFetching && url.isNotBlank(),
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(stringResource(R.string.editor_refetch))
                    }
                    if (titleManuallyEdited) {
                        Text(
                            text = stringResource(R.string.editor_title_manual),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    errorRes = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (type == NoteType.TEXT) 220.dp else 140.dp),
                label = {
                    Text(
                        stringResource(
                            if (type == NoteType.TEXT) R.string.editor_content else R.string.editor_content_optional,
                        ),
                    )
                },
                placeholder = { Text(stringResource(R.string.editor_content_hint)) },
                shape = RoundedCornerShape(12.dp),
            )

            Text(
                text = stringResource(R.string.editor_tags),
                style = MaterialTheme.typography.titleSmall,
            )
            if (tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_no_tags),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in validTagIds,
                            onClick = {
                                selectedTagIds = if (tag.id in selectedTagIds) {
                                    selectedTagIds - tag.id
                                } else {
                                    selectedTagIds + tag.id
                                }
                            },
                            label = { Text(tag.name) },
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
            TextButton(onClick = onManageTags) {
                Text(stringResource(R.string.action_manage_tags))
            }

            val messageRes = errorRes ?: infoRes
            if (messageRes != null) {
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorRes != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.editor_unsaved_title)) },
            text = { Text(stringResource(R.string.editor_unsaved_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscard = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.action_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
