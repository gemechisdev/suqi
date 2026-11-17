package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SuqiQrGenerator {

    fun encodeMatrix(content: String): BitMatrix {
        val payload = content.ifBlank { "https://suqi.et" }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        return QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
    }

    /**
     * Renders a crisp, high-contrast scannable QR card bitmap with exact integer module
     * boundaries and a clean white quiet zone, ready to save or share as PNG.
     */
    fun renderQrCardBitmap(
        content: String,
        title: String? = null,
        badgeText: String? = null,
        sizePx: Int = 800
    ): Bitmap {
        val matrix = encodeMatrix(content)
        val hasHeader = !badgeText.isNullOrBlank() || !title.isNullOrBlank()
        val topPadding = if (hasHeader) 128 else 48
        val bottomPadding = 96
        val sidePadding = 72
        val maxQrArea = (sizePx - sidePadding * 2).coerceAtLeast(matrix.width * 4)
        val modulePx = (maxQrArea / matrix.width).coerceAtLeast(4)
        val qrSize = modulePx * matrix.width
        val cardWidth = (qrSize + sidePadding * 2).coerceAtLeast(sizePx)
        val cardHeight = topPadding + qrSize + bottomPadding

        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val qrLeft = (cardWidth - qrSize) / 2
        val qrTop = topPadding

        val pixels = IntArray(cardWidth * cardHeight) { AndroidColor.WHITE }
        val darkColor = AndroidColor.rgb(23, 23, 23)
        val borderColor = AndroidColor.rgb(234, 234, 234)

        val bLeft = (qrLeft - 16).coerceAtLeast(0)
        val bRight = (qrLeft + qrSize + 16).coerceAtMost(cardWidth - 1)
        val bTop = (qrTop - 16).coerceAtLeast(0)
        val bBottom = (qrTop + qrSize + 16).coerceAtMost(cardHeight - 1)
        for (y in bTop..bBottom) {
            for (x in bLeft..bRight) {
                if (y - bTop < 2 || bBottom - y < 2 || x - bLeft < 2 || bRight - x < 2) {
                    pixels[y * cardWidth + x] = borderColor
                }
            }
        }

        for (r in 0 until matrix.height) {
            val startY = qrTop + r * modulePx
            for (c in 0 until matrix.width) {
                if (matrix.get(c, r)) {
                    val startX = qrLeft + c * modulePx
                    for (dy in 0 until modulePx) {
                        val rowOffset = (startY + dy) * cardWidth + startX
                        for (dx in 0 until modulePx) {
                            pixels[rowOffset + dx] = darkColor
                        }
                    }
                }
            }
        }
        bitmap.setPixels(pixels, 0, cardWidth, 0, 0, cardWidth, cardHeight)

        val canvas = AndroidCanvas(bitmap)
        if (hasHeader) {
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkColor
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val headerText = badgeText?.takeIf { it.isNotBlank() } ?: title.orEmpty()
            canvas.drawText(headerText, cardWidth / 2f, 64f, headerPaint)

            if (!badgeText.isNullOrBlank() && !title.isNullOrBlank() && badgeText != title) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.rgb(255, 101, 0)
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(title, cardWidth / 2f, 102f, subPaint)
            }
        }

        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(82, 82, 82)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val displayUrl = if (content.length > 48) content.take(45) + "..." else content
        canvas.drawText(displayUrl, cardWidth / 2f, (qrTop + qrSize + 52).toFloat(), urlPaint)

        return bitmap
    }

    suspend fun saveQrBitmapToPictures(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String = "suqi_qr"
    ): Uri? = withContext(Dispatchers.IO) {
        val safePrefix = filePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "suqi_qr" }
        val fileName = "${safePrefix}_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Suqi")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG compression failed" }
                } ?: error("Failed to open output stream")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: File(context.filesDir, "qr_exports")
            dir.mkdirs()
            val outFile = File(dir, fileName)
            outFile.outputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG compression failed" }
            }
            Uri.fromFile(outFile)
        }
    }

    suspend fun writeQrBitmapToUri(context: Context, bitmap: Bitmap, destination: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG compression failed" }
        } ?: error("Could not open destination")
    }

    suspend fun createShareQrImageUri(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String = "suqi_qr"
    ): Uri = withContext(Dispatchers.IO) {
        val safePrefix = filePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "suqi_qr" }
        val shareDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
        // Clean stale cached QR exports older than 1 hour
        val cutoff = System.currentTimeMillis() - 3_600_000L
        shareDir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }

        val outFile = File(shareDir, "${safePrefix}_${System.currentTimeMillis()}.png")
        outFile.outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG compression failed" }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }
}
