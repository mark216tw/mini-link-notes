package com.vibecode.minilinknotes.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vibecode.minilinknotes.AppViewModel
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.TagEntity
import com.vibecode.minilinknotes.data.TagWithNoteCount
import com.vibecode.minilinknotes.ui.theme.TagColors
import com.vibecode.minilinknotes.ui.theme.tagColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class DragState(
    val tagId: Long,
    val fromIndex: Int,
    val offset: Float,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagsScreen(
    tags: List<TagWithNoteCount>,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scope = rememberCoroutineScope()
    var showEditor by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<TagEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<TagEntity, Int>?>(null) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var rowHeightPx by remember { mutableIntStateOf(72) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (tags.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.tag_empty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.tag_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.tag_reorder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            tags.forEachIndexed { index, item ->
                val tag = item.tag
                TagRow(
                    item = item,
                    index = index,
                    lastIndex = tags.lastIndex,
                    dark = dark,
                    dragState = dragState,
                    onMeasured = { height -> if (height > 0) rowHeightPx = height },
                    onDragStart = { dragState = DragState(tag.id, index, 0f) },
                    onDrag = { amount ->
                        dragState = dragState?.let { it.copy(offset = it.offset + amount) }
                    },
                    onDragEnd = {
                        dragState?.let { state ->
                            val target = (state.fromIndex + (state.offset / rowHeightPx).roundToInt())
                                .coerceIn(0, tags.lastIndex)
                            if (target != state.fromIndex) {
                                viewModel.moveTag(state.tagId, target)
                            }
                        }
                        dragState = null
                    },
                    onDragCancel = { dragState = null },
                    onEdit = {
                        editingTag = tag
                        showEditor = true
                    },
                    onDelete = {
                        scope.launch {
                            val count = viewModel.noteCountForTag(tag.id)
                            pendingDelete = tag to count
                        }
                    },
                    onMoveUp = { viewModel.moveTag(tag.id, index - 1) },
                    onMoveDown = { viewModel.moveTag(tag.id, index + 1) },
                )
            }
            Spacer(Modifier.size(96.dp))
        }
    }

    if (showEditor) {
        TagEditorDialog(
            tag = editingTag,
            viewModel = viewModel,
            onDismiss = { showEditor = false },
        )
    }

    pendingDelete?.let { (tag, count) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.tag_delete_title, tag.name)) },
            text = { Text(stringResource(R.string.tag_delete_message, count)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag.id)
                        pendingDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TagRow(
    item: TagWithNoteCount,
    index: Int,
    lastIndex: Int,
    dark: Boolean,
    dragState: DragState?,
    onMeasured: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val tag = item.tag
    var menuExpanded by remember { mutableStateOf(false) }
    val color = tagColor(tag.colorIndex, dark)
    val dragging = dragState?.tagId == tag.id
    val dragLabel = stringResource(R.string.action_drag_reorder)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .graphicsLayer {
                translationY = if (dragging) dragState?.offset ?: 0f else 0f
            }
            .zIndex(if (dragging) 1f else 0f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(tag.id, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                onDragStart()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragCancel() },
                        )
                    }
                    .semantics {
                        contentDescription = dragLabel
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.tag_note_count, item.noteCount),
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
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_move_up)) },
                        enabled = index > 0,
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_move_down)) },
                        enabled = index < lastIndex,
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorDialog(
    tag: TagEntity?,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(tag?.id) { mutableStateOf(tag?.name.orEmpty()) }
    var colorIndex by rememberSaveable(tag?.id) { mutableIntStateOf(tag?.colorIndex ?: 0) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (tag == null) R.string.tag_add else R.string.tag_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorRes = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tag_name)) },
                    placeholder = { Text(stringResource(R.string.tag_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Text(
                    text = stringResource(R.string.tag_color),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TagColors.indices.forEach { index ->
                        val label = stringResource(R.string.tag_color_number, index + 1)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tagColor(index, dark))
                                .then(
                                    if (colorIndex == index) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable {
                                    colorIndex = index
                                    errorRes = null
                                }
                                .semantics {
                                    contentDescription = label
                                    selected = colorIndex == index
                                },
                        )
                    }
                }
                if (errorRes != null) {
                    Text(
                        text = stringResource(errorRes!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    if (name.isBlank()) {
                        errorRes = R.string.tag_name_required
                        return@TextButton
                    }
                    scope.launch {
                        isSaving = true
                        val result: Result<Unit> = if (tag == null) {
                            viewModel.addTag(name, colorIndex).map { }
                        } else {
                            viewModel.updateTag(tag.id, name, colorIndex)
                        }
                        isSaving = false
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            errorRes = if (viewModel.isDuplicateTagError(result.exceptionOrNull())) {
                                R.string.tag_name_duplicate
                            } else {
                                R.string.tag_name_required
                            }
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

