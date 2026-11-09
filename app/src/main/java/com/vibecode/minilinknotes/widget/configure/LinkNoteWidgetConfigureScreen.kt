package com.vibecode.minilinknotes.widget.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.AppSettings
import com.vibecode.minilinknotes.data.ImageStorage
import com.vibecode.minilinknotes.data.NoteEntity
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.ui.components.NoteImage
import com.vibecode.minilinknotes.ui.theme.MiniLinkNotesTheme
import com.vibecode.minilinknotes.ui.theme.WidgetBackgroundDay
import com.vibecode.minilinknotes.ui.theme.WidgetBackgroundNight
import com.vibecode.minilinknotes.ui.theme.WidgetBodyDay
import com.vibecode.minilinknotes.ui.theme.WidgetBodyNight
import com.vibecode.minilinknotes.ui.theme.WidgetLinkDay
import com.vibecode.minilinknotes.ui.theme.WidgetLinkNight
import com.vibecode.minilinknotes.ui.theme.WidgetTitleDay
import com.vibecode.minilinknotes.ui.theme.WidgetTitleNight
import com.vibecode.minilinknotes.widget.MAX_WIDGET_FONT_PERCENT
import com.vibecode.minilinknotes.widget.MIN_WIDGET_FONT_PERCENT
import com.vibecode.minilinknotes.widget.WIDGET_FONT_PERCENT_STEP
import com.vibecode.minilinknotes.widget.WidgetBackgroundColor
import com.vibecode.minilinknotes.widget.WidgetTextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkNoteWidgetConfigureScreen(
    state: WidgetConfigureUiState,
    imageStorage: ImageStorage,
    onQueryChange: (String) -> Unit,
    onSelect: (Long) -> Unit,
    onTextColorChange: (WidgetTextColor) -> Unit,
    onBackgroundColorChange: (WidgetBackgroundColor) -> Unit,
    onFontPercentChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var showStyleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_configure_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                tonalElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = state.arePreferencesLoaded && state.selectedNoteId != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = { showStyleDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.widget_style_title))
                        }
                    }
                }
                when {
                    state.isLoading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.notes.isEmpty() -> item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(
                                    if (state.query.isBlank()) {
                                        R.string.widget_configure_empty
                                    } else {
                                        R.string.empty_search_title
                                    },
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (state.query.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.widget_configure_empty_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    else -> {
                        items(state.notes, key = { it.id }) { note ->
                            WidgetNoteOption(
                                note = note,
                                imageStorage = imageStorage,
                                selected = note.id == state.selectedNoteId,
                                onClick = { onSelect(note.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            title = { Text(stringResource(R.string.widget_style_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                ) {
                    WidgetStyleControls(
                        state = state,
                        imageStorage = imageStorage,
                        onTextColorChange = onTextColorChange,
                        onBackgroundColorChange = onBackgroundColorChange,
                        onFontPercentChange = onFontPercentChange,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleDialog = false }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
        )
    }
}

@Composable
private fun WidgetStyleControls(
    state: WidgetConfigureUiState,
    imageStorage: ImageStorage,
    onTextColorChange: (WidgetTextColor) -> Unit,
    onBackgroundColorChange: (WidgetBackgroundColor) -> Unit,
    onFontPercentChange: (Int) -> Unit,
) {
    Text(
        text = stringResource(R.string.widget_font_size, state.fontPercent),
        style = MaterialTheme.typography.labelLarge,
    )
    Slider(
        value = state.fontPercent.toFloat(),
        onValueChange = { onFontPercentChange(it.toInt()) },
        enabled = state.arePreferencesLoaded,
        valueRange = MIN_WIDGET_FONT_PERCENT.toFloat()..MAX_WIDGET_FONT_PERCENT.toFloat(),
        steps = (MAX_WIDGET_FONT_PERCENT - MIN_WIDGET_FONT_PERCENT) / WIDGET_FONT_PERCENT_STEP - 1,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.widget_text_color),
        style = MaterialTheme.typography.labelLarge,
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WidgetTextColor.entries.forEach { textColor ->
            WidgetColorSwatch(
                selected = state.textColor == textColor,
                color = textColor.color,
                autoColors = listOf(WidgetTitleDay, WidgetTitleNight),
                label = stringResource(textColor.labelResource()),
                enabled = state.arePreferencesLoaded,
                onClick = { onTextColorChange(textColor) },
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.widget_background_color),
        style = MaterialTheme.typography.labelLarge,
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WidgetBackgroundColor.entries.forEach { backgroundColor ->
            WidgetColorSwatch(
                selected = state.backgroundColor == backgroundColor,
                color = backgroundColor.color,
                autoColors = listOf(WidgetBackgroundDay, WidgetBackgroundNight),
                label = stringResource(backgroundColor.labelResource()),
                enabled = state.arePreferencesLoaded,
                onClick = { onBackgroundColorChange(backgroundColor) },
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.widget_preview),
        style = MaterialTheme.typography.labelLarge,
    )
    Spacer(modifier = Modifier.height(4.dp))
    WidgetPreview(
        note = state.selectedNote,
        imageStorage = imageStorage,
        textColor = state.textColor,
        backgroundColor = state.backgroundColor,
        fontPercent = state.fontPercent,
    )
}

@Composable
private fun WidgetColorSwatch(
    selected: Boolean,
    color: Color?,
    autoColors: List<Color>,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val checkColor = if (color == null) {
        if (autoColors.first().luminance() > 0.5f) Color.Black else Color.White
    } else {
        if (color.luminance() > 0.5f) Color.Black else Color.White
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                enabled = enabled,
                role = Role.RadioButton,
                onClickLabel = label,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                this.selected = selected
                role = Role.RadioButton
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (color != null) {
                        Modifier.background(color)
                    } else {
                        Modifier.background(Brush.linearGradient(autoColors))
                    },
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun WidgetPreview(
    note: NoteEntity?,
    imageStorage: ImageStorage,
    textColor: WidgetTextColor,
    backgroundColor: WidgetBackgroundColor,
    fontPercent: Int,
) {
    val dark = isSystemInDarkTheme()
    val selectedColor = textColor.color
    val customBackground = backgroundColor.color
    val useDarkText = customBackground != null
    val titleColor = selectedColor
        ?: if (dark && !useDarkText) WidgetTitleNight else WidgetTitleDay
    val bodyColor = selectedColor
        ?: if (dark && !useDarkText) WidgetBodyNight else WidgetBodyDay
    val linkColor = selectedColor
        ?: if (dark && !useDarkText) WidgetLinkNight else WidgetLinkDay
    val previewBackground = customBackground
        ?: if (dark) WidgetBackgroundNight else WidgetBackgroundDay
    val scale = fontPercent / 100f
    val isLink = note?.type == NoteType.LINK
    val title = note?.title?.ifBlank {
        stringResource(
            when (note.type) {
                NoteType.LINK -> R.string.note_untitled_link
                NoteType.TEXT -> R.string.note_untitled_text
                NoteType.IMAGE -> R.string.note_untitled_image
            },
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(previewBackground)
                .padding(12.dp),
        ) {
            if (note == null) {
                Text(
                    text = stringResource(R.string.widget_unconfigured),
                    color = bodyColor,
                    fontSize = (13 * scale).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                return@Column
            }
            if (note.type == NoteType.IMAGE) {
                NoteImage(
                    imageStorage = imageStorage,
                    fileName = note.imageFileName,
                    contentDescription = stringResource(R.string.note_type_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = title.orEmpty(),
                color = titleColor,
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            note.url?.let { url ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = url,
                    color = linkColor,
                    fontSize = (12 * scale).sp,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val content = note.content?.trim()
            if (!content.isNullOrBlank() && !(isLink && content == note.url)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = content,
                    color = bodyColor,
                    fontSize = (12 * scale).sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun WidgetBackgroundColor.labelResource(): Int = when (this) {
    WidgetBackgroundColor.AUTO -> R.string.widget_background_auto
    WidgetBackgroundColor.LIGHT_YELLOW -> R.string.widget_background_light_yellow
    WidgetBackgroundColor.LIGHT_GREEN -> R.string.widget_background_light_green
    WidgetBackgroundColor.LIGHT_BLUE -> R.string.widget_background_light_blue
    WidgetBackgroundColor.LIGHT_PINK -> R.string.widget_background_light_pink
    WidgetBackgroundColor.LIGHT_PURPLE -> R.string.widget_background_light_purple
    WidgetBackgroundColor.LIGHT_ORANGE -> R.string.widget_background_light_orange
    WidgetBackgroundColor.LIGHT_GRAY -> R.string.widget_background_light_gray
}

private fun WidgetTextColor.labelResource(): Int = when (this) {
    WidgetTextColor.AUTO -> R.string.widget_color_auto
    WidgetTextColor.BLACK -> R.string.widget_color_black
    WidgetTextColor.WHITE -> R.string.widget_color_white
    WidgetTextColor.DARK_GRAY -> R.string.widget_color_dark_gray
    WidgetTextColor.LIGHT_GRAY -> R.string.widget_color_light_gray
    WidgetTextColor.BLUE -> R.string.widget_color_blue
    WidgetTextColor.TEAL -> R.string.widget_color_teal
    WidgetTextColor.YELLOW -> R.string.widget_color_yellow
}

@Composable
private fun WidgetNoteOption(
    note: NoteEntity,
    imageStorage: ImageStorage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val isLink = note.type == NoteType.LINK
    val title = note.title.ifBlank {
        stringResource(
            when (note.type) {
                NoteType.LINK -> R.string.note_untitled_link
                NoteType.TEXT -> R.string.note_untitled_text
                NoteType.IMAGE -> R.string.note_untitled_image
            },
        )
    }
    val snippet = note.content?.trim()?.replace('\n', ' ')

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (note.type == NoteType.IMAGE) {
                    NoteImage(
                        imageStorage = imageStorage,
                        fileName = note.imageFileName,
                        contentDescription = stringResource(R.string.note_type_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                note.url?.let { url ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!snippet.isNullOrBlank() && !(isLink && snippet == note.url)) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkNoteWidgetConfigureScreenPreview() {
    MiniLinkNotesTheme(AppSettings()) {
        LinkNoteWidgetConfigureScreen(
            state = WidgetConfigureUiState(
                isLoading = false,
                notes = listOf(
                    NoteEntity(
                        id = 1,
                        type = NoteType.LINK,
                        title = "範例連結",
                        url = "https://example.com",
                        content = "這是分享時留下的內容。",
                        createdAt = 0,
                        updatedAt = 0,
                    ),
                    NoteEntity(
                        id = 2,
                        type = NoteType.TEXT,
                        title = "範例文字筆記",
                        content = "只有文字的筆記。",
                        createdAt = 0,
                        updatedAt = 0,
                    ),
                ),
                selectedNoteId = 1,
                selectedNote = NoteEntity(
                    id = 1,
                    type = NoteType.LINK,
                    title = "範例連結",
                    url = "https://example.com",
                    content = "這是分享時留下的內容。",
                    createdAt = 0,
                    updatedAt = 0,
                ),
                arePreferencesLoaded = true,
            ),
            imageStorage = ImageStorage(LocalContext.current),
            onQueryChange = {},
            onSelect = {},
            onTextColorChange = {},
            onBackgroundColorChange = {},
            onFontPercentChange = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}
