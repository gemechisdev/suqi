package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MediaItem
import com.example.data.model.MediaKind
import com.example.data.model.mediaTypeOfUrl
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_MEDIA = 12
private const val MAX_IMAGE_BYTES = 15L * 1024 * 1024
private const val MAX_VIDEO_BYTES = 80L * 1024 * 1024
private const val MAX_IMAGE_EDGE = 1600

/**
 * Copies a picked file into app storage so it stays available after the picker permission
 * expires. Static images are downscaled (max 1600px) and re-encoded to keep loading fast;
 * GIFs and videos are copied as-is. Throws with a user-readable message when invalid.
 */
private suspend fun importMedia(context: Context, uri: Uri): MediaItem = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)
    val type = mediaTypeOfUrl(uri.toString(), mime)
    val size = try { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
    val limit = if (type == MediaKind.VIDEO) MAX_VIDEO_BYTES else MAX_IMAGE_BYTES
    if (size > limit) error("File is too large (max ${limit / (1024 * 1024)} MB)")

    val dir = File(context.filesDir, "product_media").apply { mkdirs() }
    val stamp = System.currentTimeMillis()

    if (type == MediaKind.IMAGE) {
        val compressed = try { compressImage(context, uri, mime, dir, stamp) } catch (_: Exception) { null }
        if (compressed != null) return@withContext MediaItem(Uri.fromFile(compressed).toString(), type)
    }

    val ext = when (type) {
        MediaKind.GIF -> "gif"
        MediaKind.VIDEO -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp4"
        else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
    }
    val file = File(dir, "media_$stamp.$ext")
    resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
        ?: error("Could not read the selected file")
    MediaItem(Uri.fromFile(file).toString(), type)
}

private fun compressImage(context: Context, uri: Uri, mime: String?, dir: File, stamp: Long): File? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_IMAGE_EDGE * 2) sample *= 2
    var bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val scale = MAX_IMAGE_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
    if (scale < 1f) {
        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    val orientation = try {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees != 0f) {
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }

    val png = mime == "image/png"
    val file = File(dir, "img_$stamp.${if (png) "png" else "jpg"}")
    file.outputStream().use { out ->
        bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 85, out)
    }
    return file
}

/**
 * Owner-side media manager: add images / GIFs / videos (device or link), preview them, choose the
 * cover image, reorder, replace and remove. The list order is the gallery order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuqiMediaEditor(
    media: SnapshotStateList<MediaItem>,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalSuqiColors.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var replaceIndex by remember { mutableStateOf<Int?>(null) }
    var showLink by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }

    val addLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            var added = 0
            for (uri in uris) {
                if (media.size >= MAX_MEDIA) {
                    onMessage("You can add up to $MAX_MEDIA media files")
                    break
                }
                try {
                    media.add(importMedia(context, uri))
                    added++
                } catch (e: Exception) {
                    onMessage(e.message ?: "Could not add this file")
                }
            }
            importing = false
            if (added > 0) onMessage("$added file(s) added")
        }
    }

    val replaceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val index = replaceIndex
        replaceIndex = null
        if (uri != null && index != null && index in media.indices) {
            scope.launch {
                importing = true
                try {
                    media[index] = importMedia(context, uri)
                    onMessage("Media replaced")
                } catch (e: Exception) {
                    onMessage(e.message ?: "Could not replace this file")
                }
                importing = false
            }
        }
    }

    fun launchAdd() {
        if (media.size >= MAX_MEDIA) {
            onMessage("You can add up to $MAX_MEDIA media files")
            return
        }
        addLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    val coverIndex = media.indexOfFirst { !it.isVideo }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Photos, GIFs & Videos",
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.primaryText
                )
                Text(
                    text = "${media.size}/$MAX_MEDIA • The cover is the first photo. Tap a file to manage it.",
                    fontFamily = SuqiFontFamily,
                    fontSize = 11.sp,
                    color = colors.secondaryText
                )
            }
            TextButton(onClick = { showLink = !showLink }) {
                Icon(LucideIcons.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuqiPrimary)
                Spacer(Modifier.width(4.dp))
                Text("Link", fontFamily = SuqiFontFamily, fontSize = 12.sp, color = SuqiPrimary)
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.primaryTint)
                        .border(1.5.dp, SuqiPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .clickable { launchAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    if (importing) {
                        CircularProgressIndicator(color = SuqiPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(LucideIcons.ImagePlus, contentDescription = "Add media", tint = SuqiPrimary, modifier = Modifier.size(26.dp))
                            Text("Add", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SuqiPrimary)
                        }
                    }
                }
            }
            itemsIndexed(media) { index, item ->
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .clickable { selectedIndex = index }
                ) {
                    SuqiMediaThumb(item, Modifier.fillMaxSize())
                    if (index == coverIndex) {
                        TileTag("Cover", SuqiPrimary, Modifier.align(Alignment.TopStart).padding(6.dp))
                    }
                    when {
                        item.isGif -> TileTag("GIF", Color.Black.copy(alpha = 0.6f), Modifier.align(Alignment.BottomStart).padding(6.dp))
                        item.isVideo -> TileTag("VIDEO", Color.Black.copy(alpha = 0.6f), Modifier.align(Alignment.BottomStart).padding(6.dp))
                    }
                }
            }
        }

        if (showLink) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuqiTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = "",
                    placeholder = "Paste image, GIF or video link (https://…)",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val url = linkText.trim()
                        when {
                            !(url.startsWith("http://") || url.startsWith("https://")) -> onMessage("Enter a valid http(s) link")
                            media.size >= MAX_MEDIA -> onMessage("You can add up to $MAX_MEDIA media files")
                            else -> {
                                media.add(MediaItem(url, mediaTypeOfUrl(url)))
                                linkText = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuqiPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Add", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    val index = selectedIndex
    if (index != null && index in media.indices) {
        val item = media[index]
        Dialog(onDismissRequest = { selectedIndex = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(SuqiRadius.Card))
                    .background(colors.cardBackground)
                    .padding(16.dp)
            ) {
                SuqiMediaPreview(
                    item = item,
                    isActive = true,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.04f))
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when {
                        index == coverIndex -> "Cover image"
                        item.isVideo -> "Video • position ${index + 1}"
                        item.isGif -> "GIF • position ${index + 1}"
                        else -> "Photo • position ${index + 1}"
                    },
                    fontFamily = SuqiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.primaryText
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionChip("Set as cover", LucideIcons.Star, enabled = !item.isVideo && index != coverIndex) {
                        val moved = media.removeAt(index)
                        media.add(0, moved)
                        selectedIndex = 0
                    }
                    ActionChip("Move left", LucideIcons.ChevronLeft, enabled = index > 0) {
                        val moved = media.removeAt(index)
                        media.add(index - 1, moved)
                        selectedIndex = index - 1
                    }
                    ActionChip("Move right", LucideIcons.ChevronRight, enabled = index < media.lastIndex) {
                        val moved = media.removeAt(index)
                        media.add(index + 1, moved)
                        selectedIndex = index + 1
                    }
                    ActionChip("Replace", LucideIcons.RefreshCw) {
                        replaceIndex = index
                        selectedIndex = null
                        replaceLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                    ActionChip("Remove", LucideIcons.Trash2, danger = true) {
                        media.removeAt(index)
                        selectedIndex = null
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { selectedIndex = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("Done", fontFamily = SuqiFontFamily, fontWeight = FontWeight.SemiBold, color = SuqiPrimary)
                }
            }
        }
    } else if (index != null) {
        selectedIndex = null
    }
}

@Composable
private fun TileTag(text: String, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text, fontFamily = SuqiFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) Color(0xFFD92D20) else SuqiPrimary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (enabled) tint.copy(alpha = 0.6f) else Color.LightGray),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else Color.Gray, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontFamily = SuqiFontFamily, fontSize = 12.sp, color = if (enabled) tint else Color.Gray)
    }
}
