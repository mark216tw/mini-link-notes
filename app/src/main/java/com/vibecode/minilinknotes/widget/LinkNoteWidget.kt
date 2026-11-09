package com.vibecode.minilinknotes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.vibecode.minilinknotes.MainActivity
import com.vibecode.minilinknotes.MiniLinkNotesApp
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.ImageStorage
import com.vibecode.minilinknotes.data.NoteEntity
import com.vibecode.minilinknotes.data.NoteType
import com.vibecode.minilinknotes.ui.theme.WidgetBackgroundDay
import com.vibecode.minilinknotes.ui.theme.WidgetBackgroundNight
import com.vibecode.minilinknotes.ui.theme.WidgetBodyDay
import com.vibecode.minilinknotes.ui.theme.WidgetBodyNight
import com.vibecode.minilinknotes.ui.theme.WidgetLinkDay
import com.vibecode.minilinknotes.ui.theme.WidgetLinkNight
import com.vibecode.minilinknotes.ui.theme.WidgetTitleDay
import com.vibecode.minilinknotes.ui.theme.WidgetTitleNight
import com.vibecode.minilinknotes.util.TimeUtils
import com.vibecode.minilinknotes.widget.configure.LinkNoteWidgetConfigureActivity

class LinkNoteWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as MiniLinkNotesApp).container
        val repository = container.repository
        val imageStorage = container.imageStorage
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

        provideContent {
            val preferences = currentState<Preferences>()
            val notes by repository.activeNoteEntities.collectAsState(initial = null)
            val noteId = preferences[noteIdKey]
            val displayState = when {
                notes == null -> WidgetDisplayState.Loading
                noteId == null -> WidgetDisplayState.Unconfigured
                else -> notes?.firstOrNull { it.id == noteId }
                    ?.let(WidgetDisplayState::Ready)
                    ?: WidgetDisplayState.Missing
            }

            LinkNoteWidgetContent(
                state = displayState,
                appWidgetId = appWidgetId,
                imageStorage = imageStorage,
                textColor = WidgetTextColor.fromPreference(preferences[textColorKey]),
                backgroundColor = WidgetBackgroundColor.fromPreference(preferences[backgroundColorKey]),
                fontPercent = normalizeWidgetFontPercent(preferences[fontPercentKey]),
            )
        }
    }

    companion object {
        val noteIdKey = longPreferencesKey("note_id")
        val textColorKey = stringPreferencesKey("text_color")
        val backgroundColorKey = stringPreferencesKey("background_color")
        val fontPercentKey = intPreferencesKey("font_percent")
    }
}

private const val WIDGET_IMAGE_MAX_PX = 256

private sealed interface WidgetDisplayState {
    data object Loading : WidgetDisplayState
    data object Unconfigured : WidgetDisplayState
    data object Missing : WidgetDisplayState
    data class Ready(val note: NoteEntity) : WidgetDisplayState
}

@Composable
private fun LinkNoteWidgetContent(
    state: WidgetDisplayState,
    appWidgetId: Int,
    imageStorage: ImageStorage,
    textColor: WidgetTextColor,
    backgroundColor: WidgetBackgroundColor,
    fontPercent: Int,
) {
    val context = LocalContext.current
    val readyNote = (state as? WidgetDisplayState.Ready)?.note
    val openAction = readyNote?.let { openNoteAction(context, it.id) } ?: openAppAction(context)
    val selectedColor = textColor.color?.let { ColorProvider(day = it, night = it) }
    val customBackground = backgroundColor.color
    val autoTitleColor = if (customBackground == null) {
        ColorProvider(day = WidgetTitleDay, night = WidgetTitleNight)
    } else {
        ColorProvider(day = WidgetTitleDay, night = WidgetTitleDay)
    }
    val autoBodyColor = if (customBackground == null) {
        ColorProvider(day = WidgetBodyDay, night = WidgetBodyNight)
    } else {
        ColorProvider(day = WidgetBodyDay, night = WidgetBodyDay)
    }
    val autoLinkColor = if (customBackground == null) {
        ColorProvider(day = WidgetLinkDay, night = WidgetLinkNight)
    } else {
        ColorProvider(day = WidgetLinkDay, night = WidgetLinkDay)
    }
    val scale = fontPercent / 100f
    val titleColor = selectedColor ?: autoTitleColor
    val bodyColor = selectedColor ?: autoBodyColor
    val linkColor = selectedColor ?: autoLinkColor

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                day = customBackground ?: WidgetBackgroundDay,
                night = customBackground ?: WidgetBackgroundNight,
            )
            .appWidgetBackground()
            .cornerRadius(12.dp)
            .clickable(openAction)
            .padding(12.dp),
    ) {
        if (state !is WidgetDisplayState.Ready) {
            val message = when (state) {
                WidgetDisplayState.Loading -> R.string.widget_loading
                WidgetDisplayState.Unconfigured -> R.string.widget_unconfigured
                WidgetDisplayState.Missing -> R.string.widget_unavailable
                is WidgetDisplayState.Ready -> error("Handled above")
            }
            Text(
                text = context.getString(message),
                style = TextStyle(
                    color = bodyColor,
                    fontSize = (13 * scale).sp,
                ),
                maxLines = 3,
            )
            if (state !== WidgetDisplayState.Loading) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = context.getString(R.string.widget_select_hint),
                    modifier = GlanceModifier.clickable(configureAction(context, appWidgetId)),
                    style = TextStyle(
                        color = linkColor,
                        fontSize = (13 * scale).sp,
                        textDecoration = TextDecoration.Underline,
                    ),
                )
            }
            return@Column
        }

        val note = state.note
        val isLink = note.type == NoteType.LINK
        val isImage = note.type == NoteType.IMAGE
        val thumbnail = if (isImage) {
            remember(note.imageFileName) {
                imageStorage.thumbnailBitmap(note.imageFileName)?.let { bitmap ->
                    val scale = WIDGET_IMAGE_MAX_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
                    if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt().coerceAtLeast(1),
                            (bitmap.height * scale).toInt().coerceAtLeast(1),
                            true,
                        )
                    } else {
                        bitmap
                    }
                }
            }
        } else {
            null
        }
        val title = note.title.ifBlank {
            context.getString(
                when (note.type) {
                    NoteType.LINK -> R.string.note_untitled_link
                    NoteType.TEXT -> R.string.note_untitled_text
                    NoteType.IMAGE -> R.string.note_untitled_image
                },
            )
        }

        if (thumbnail != null) {
            Image(
                provider = ImageProvider(thumbnail),
                contentDescription = context.getString(R.string.editor_image),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .cornerRadius(8.dp),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
        }

        Text(
            text = title,
            style = TextStyle(
                color = titleColor,
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 2,
        )

        note.url?.let { url ->
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = url,
                modifier = GlanceModifier.clickable(openLinkAction(url)),
                style = TextStyle(
                    color = linkColor,
                    fontSize = (12 * scale).sp,
                    textDecoration = TextDecoration.Underline,
                ),
                maxLines = 1,
            )
        }

        val content = note.content?.trim()
        if (!content.isNullOrBlank() && !(isLink && content == note.url)) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = content,
                style = TextStyle(
                    color = bodyColor,
                    fontSize = (12 * scale).sp,
                ),
                maxLines = 8,
            )
        }

        val reminderAt = note.reminderAt
        if (note.reminderEnabled && reminderAt != null) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_notification),
                    contentDescription = context.getString(R.string.reminder_content_description),
                    modifier = GlanceModifier.size(14.dp),
                    colorFilter = ColorFilter.tint(linkColor),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = TimeUtils.formatDateTime(reminderAt),
                    style = TextStyle(
                        color = linkColor,
                        fontSize = (9 * scale).sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun openLinkAction(url: String): Action {    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return actionStartActivity(intent)
}

private fun openNoteAction(context: Context, noteId: Long): Action {
    val intent = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_NOTE_ID, noteId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return actionStartActivity(intent)
}

private fun openAppAction(context: Context): Action {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return actionStartActivity(intent)
}

private fun configureAction(context: Context, appWidgetId: Int): Action {
    val intent = Intent(context, LinkNoteWidgetConfigureActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return actionStartActivity(intent)
}
