package com.vibecode.minilinknotes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredImage(
    val fileName: String,
    val mimeType: String,
    val sha256: String,
)

class ImageStorage(context: Context) {
    private val appContext = context.applicationContext
    private val imageDir = File(appContext.filesDir, IMAGE_DIRECTORY).apply { mkdirs() }
    private val thumbnailDir = File(appContext.cacheDir, THUMBNAIL_DIRECTORY).apply { mkdirs() }

    suspend fun import(uri: Uri): StoredImage = withContext(Dispatchers.IO) {
        val mimeType = appContext.contentResolver.getType(uri)
            ?.takeIf { it.startsWith("image/") }
            ?: "image/jpeg"
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image")
        input.use { store(it, mimeType) }
    }

    suspend fun import(input: InputStream, mimeType: String): StoredImage =
        withContext(Dispatchers.IO) { store(input, mimeType) }

    fun imageFile(fileName: String?): File? {
        if (fileName.isNullOrBlank() || fileName != File(fileName).name) return null
        return File(imageDir, fileName).takeIf(File::isFile)
    }

    suspend fun thumbnailFile(fileName: String?): File? = withContext(Dispatchers.IO) {
        thumbnailFileSync(fileName)
    }

    fun thumbnailBitmap(fileName: String?): Bitmap? = runCatching {
        val file = thumbnailFileSync(fileName) ?: return@runCatching null
        BitmapFactory.decodeFile(file.absolutePath)
    }.getOrNull()

    suspend fun fullImageBitmap(
        fileName: String?,
        maxPx: Int = FULL_IMAGE_MAX_PX,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val source = imageFile(fileName) ?: return@withContext null
        decodeScaled(source, maxPx)
    }

    private fun thumbnailFileSync(fileName: String?): File? {
        val source = imageFile(fileName) ?: return null
        val target = File(thumbnailDir, "${source.name}.webp")
        if (target.isFile && target.lastModified() >= source.lastModified()) return target

        val thumbnail = decodeScaled(source, THUMBNAIL_MAX_PX) ?: return null
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { output ->
            @Suppress("DEPRECATION")
            thumbnail.compress(Bitmap.CompressFormat.WEBP, 82, output)
        }
        thumbnail.recycle()
        return target
    }

    private fun decodeScaled(source: File, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxPx || bounds.outHeight / sampleSize > maxPx) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return null
        val rotation = runCatching { ExifInterface(source).rotationDegrees }.getOrDefault(0)
        val rotated = if (rotation == 0) {
            decoded
        } else {
            Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { postRotate(rotation.toFloat()) },
                true,
            ).also { if (it !== decoded) decoded.recycle() }
        }
        val scale = minOf(1f, maxPx.toFloat() / maxOf(rotated.width, rotated.height))
        return if (scale < 1f) {
            rotated.scale(
                (rotated.width * scale).toInt().coerceAtLeast(1),
                (rotated.height * scale).toInt().coerceAtLeast(1),
                filter = true,
            ).also { if (it !== rotated) rotated.recycle() }
        } else {
            rotated
        }
    }

    suspend fun delete(fileName: String?) = withContext(Dispatchers.IO) {
        if (fileName.isNullOrBlank() || fileName != File(fileName).name) return@withContext
        File(imageDir, fileName).delete()
        File(thumbnailDir, "$fileName.webp").delete()
    }

    suspend fun cleanupUnreferenced(referencedFileNames: Set<String>) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - STAGING_GRACE_MS
        imageDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name !in referencedFileNames && file.lastModified() < cutoff) {
                file.delete()
                File(thumbnailDir, "${file.name}.webp").delete()
            }
        }
        thumbnailDir.listFiles()?.forEach { thumbnail ->
            val originalName = thumbnail.name.removeSuffix(".webp")
            if (originalName !in referencedFileNames) thumbnail.delete()
        }
    }

    private fun store(input: InputStream, mimeType: String): StoredImage {
        require(mimeType.startsWith("image/")) { "Unsupported image type" }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?.takeIf { it.matches(EXTENSION_PATTERN) }
            ?: "img"
        val fileName = "${UUID.randomUUID()}.$extension"
        val target = File(imageDir, fileName)
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        try {
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) throw IOException("Image is too large")
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
            if (total == 0L) throw IOException("Empty image")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Invalid image")
            return StoredImage(fileName, mimeType, digest.digest().toHex())
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val IMAGE_DIRECTORY = "images"
        private const val THUMBNAIL_DIRECTORY = "image_thumbnails"
        private const val MAX_IMAGE_BYTES = 25L * 1024L * 1024L
        private const val THUMBNAIL_MAX_PX = 512
        private const val FULL_IMAGE_MAX_PX = 2048
        private const val STAGING_GRACE_MS = 60L * 60L * 1000L
        private val EXTENSION_PATTERN = Regex("[a-zA-Z0-9]{1,8}")
    }
}
