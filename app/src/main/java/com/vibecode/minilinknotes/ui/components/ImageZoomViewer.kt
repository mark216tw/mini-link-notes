package com.vibecode.minilinknotes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.ImageStorage
import com.vibecode.minilinknotes.ui.theme.ImageViewerContent
import com.vibecode.minilinknotes.ui.theme.ImageViewerScrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

@Composable
fun ImageZoomDialog(
    imageStorage: ImageStorage,
    fileName: String?,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        var isLoading by remember(fileName) { mutableStateOf(true) }
        var image by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
        var viewport by remember { mutableStateOf(IntSize.Zero) }
        var scale by remember(image) { mutableFloatStateOf(MIN_SCALE) }
        var offset by remember(image) { mutableStateOf(Offset.Zero) }

        LaunchedEffect(fileName) {
            isLoading = true
            image = runCatching {
                withContext(Dispatchers.IO) {
                    imageStorage.fullImageBitmap(fileName)?.asImageBitmap()
                }
            }.getOrNull()
            isLoading = false
        }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
            scale = newScale
            offset = clampOffset(offset + panChange, newScale, viewport, image)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ImageViewerScrim),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { viewport = it }
                    .pointerInput(image) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > MIN_SCALE) {
                                    scale = MIN_SCALE
                                    offset = Offset.Zero
                                } else {
                                    scale = DOUBLE_TAP_SCALE
                                    offset = clampOffset(Offset.Zero, DOUBLE_TAP_SCALE, viewport, image)
                                }
                            },
                        )
                    }
                    .transformable(
                        state = transformableState,
                        lockRotationOnZoomPan = true,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val currentImage = image
                when {
                    currentImage != null -> {
                        Image(
                            bitmap = currentImage,
                            contentDescription = contentDescription,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit,
                        )
                    }
                    isLoading -> {
                        CircularProgressIndicator(color = ImageViewerContent)
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = ImageViewerContent,
                            )
                            Text(
                                text = stringResource(R.string.image_viewer_failed),
                                color = ImageViewerContent,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.image_viewer_close),
                    tint = ImageViewerContent,
                )
            }
        }
    }
}

private fun clampOffset(
    offset: Offset,
    scale: Float,
    viewport: IntSize,
    image: ImageBitmap?,
): Offset {
    if (image == null || viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
    val fitScale = min(
        viewport.width.toFloat() / image.width,
        viewport.height.toFloat() / image.height,
    )
    val displayedWidth = image.width * fitScale * scale
    val displayedHeight = image.height * fitScale * scale
    val maxX = max(0f, (displayedWidth - viewport.width) / 2f)
    val maxY = max(0f, (displayedHeight - viewport.height) / 2f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}
