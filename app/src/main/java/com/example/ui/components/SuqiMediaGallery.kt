package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.data.model.MediaItem
import com.example.data.model.MediaKind
import com.example.ui.i18n.suqiStrings
import com.example.ui.theme.LocalSuqiColors
import com.example.ui.theme.LucideIcons
import com.example.ui.theme.SuqiFontFamily
import com.example.ui.theme.SuqiImageBackdrop
import com.example.ui.theme.SuqiPrimary
import com.example.ui.theme.SuqiRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────── shared helpers ─────────────────────────

/** Coil loader that animates GIFs (ImageDecoder on API 28+, GifDecoder before). */
@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

private object VideoThumbCache {
    val cache = LruCache<String, Bitmap>(40)
}

/** First frame of a video (http(s), content:// or file://), cached and loaded off the main thread. */
@Composable
fun rememberVideoThumbnail(url: String): ImageBitmap? {
    val context = LocalContext.current
    val state = produceState<ImageBitmap?>(initialValue = VideoThumbCache.cache.get(url)?.asImageBitmap(), url) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                if (url.startsWith("http")) {
                    retriever.setDataSource(url, HashMap<String, String>())
                } else {
                    retriever.setDataSource(context, Uri.parse(url))
                }
                retriever.getFrameAtTime(0)?.also { VideoThumbCache.cache.put(url, it) }?.asImageBitmap()
            } catch (_: Exception) {
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }
    }
    return state.value
}

@Composable
private fun MediaBadge(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (small) 4.dp else 8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = if (small) 4.dp else 8.dp, vertical = if (small) 1.dp else 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = SuqiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 9.sp else 11.sp,
            color = Color.White
        )
    }
}

@Composable
private fun PlayDisc(size: Dp, modifier: Modifier = Modifier) {
    val strings = suqiStrings()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = LucideIcons.Play,
            contentDescription = strings.common.playVideoDesc,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Static thumbnail for any media kind (image / GIF first frame / video frame). */
@Composable
fun SuqiMediaThumb(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalSuqiColors.current
    Box(modifier = modifier.background(colors.imageBackdrop), contentAlignment = Alignment.Center) {
        if (item.isVideo) {
            val frame = rememberVideoThumbnail(item.url)
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF222222)))
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.url).crossfade(true).size(256).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ───────────────────────── single-item preview ─────────────────────────

/**
 * Renders one media item: images, animated GIFs, or a video that shows its first frame
 * and plays with standard playback controls once tapped.
 */
@Composable
fun SuqiMediaPreview(
    item: MediaItem,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    onImageClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    when (item.type) {
        MediaKind.VIDEO -> VideoPlayer(item.url, isActive, modifier)
        else -> {
            val loader = if (item.isGif) rememberGifImageLoader() else coil.Coil.imageLoader(context)
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.url).crossfade(true).build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                imageLoader = loader,
                modifier = modifier.then(
                    if (onImageClick != null) Modifier.clickable { onImageClick() } else Modifier
                )
            )
        }
    }
}

@Composable
private fun VideoPlayer(url: String, isActive: Boolean, modifier: Modifier = Modifier) {
    var started by remember(url) { mutableStateOf(false) }
    // Leaving the page stops playback and returns to the poster frame.
    LaunchedEffect(isActive) { if (!isActive) started = false }

    Box(modifier = modifier.background(Color.Black)) {
        if (started) {
            AndroidView(
                factory = { ctx ->
                    val container = FrameLayout(ctx)
                    val videoView = VideoView(ctx)
                    val controller = MediaController(ctx)
                    controller.setAnchorView(container)
                    videoView.setMediaController(controller)
                    videoView.setVideoURI(Uri.parse(url))
                    videoView.setOnPreparedListener { mp ->
                        mp.isLooping = false
                        videoView.start()
                    }
                    container.addView(
                        videoView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER
                        )
                    )
                    container
                },
                onRelease = { container ->
                    (container.getChildAt(0) as? VideoView)?.stopPlayback()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val frame = rememberVideoThumbnail(url)
            val strings = suqiStrings()
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = strings.common.videoPreviewDesc,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            PlayDisc(
                size = 60.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { started = true }
            )
        }
    }
}

// ───────────────────────── product gallery ─────────────────────────

/**
 * Unified, swipeable gallery for product images, GIFs and videos with a thumbnail strip,
 * type badges, a counter and a fullscreen viewer.
 *
 * [jumpToUrl] scrolls to a media item (e.g. the image of the selected variant).
 */
@Composable
fun SuqiMediaGallery(
    media: List<MediaItem>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    jumpToUrl: String? = null,
    height: Dp = 320.dp
) {
    val colors = LocalSuqiColors.current
    val shape = RoundedCornerShape(SuqiRadius.Card)
    val scope = rememberCoroutineScope()

    if (media.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(if (colors.isDark) colors.secondaryBackground else SuqiImageBackdrop),
            contentAlignment = Alignment.Center
        ) {
            Icon(LucideIcons.ImagePlus, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(40.dp))
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { media.size })
    val thumbState = rememberLazyListState()
    var fullscreenStart by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(jumpToUrl, media) {
        val index = media.indexOfFirst { it.url == jumpToUrl }
        if (jumpToUrl != null && index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        thumbState.animateScrollToItem((pagerState.currentPage - 1).coerceAtLeast(0))
    }

    Box(modifier = modifier) {
        androidx.compose.foundation.layout.Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(shape)
                    .background(if (colors.isDark) colors.secondaryBackground else SuqiImageBackdrop)
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = media[page]
                    SuqiMediaPreview(
                        item = item,
                        isActive = pagerState.currentPage == page,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        onImageClick = { fullscreenStart = page }
                    )
                }

                val current = media[pagerState.currentPage.coerceIn(0, media.lastIndex)]
                if (current.isGif) MediaBadge("GIF", Modifier.align(Alignment.TopStart).padding(12.dp))
                if (current.isVideo) MediaBadge("VIDEO", Modifier.align(Alignment.TopStart).padding(12.dp))

                if (media.size > 1) {
                    MediaBadge(
                        text = "${pagerState.currentPage + 1}/${media.size}",
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    )
                }
            }

            if (media.size > 1) {
                LazyRow(
                    state = thumbState,
                    contentPadding = PaddingValues(vertical = 10.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(media) { index, item ->
                        val selected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) SuqiPrimary else colors.border,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        ) {
                            SuqiMediaThumb(item, Modifier.fillMaxSize())
                            if (item.isGif) MediaBadge("GIF", Modifier.align(Alignment.BottomEnd).padding(3.dp), small = true)
                            if (item.isVideo) PlayDisc(22.dp, Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }

    fullscreenStart?.let { start ->
        FullscreenMediaViewer(media = media, startIndex = start, onDismiss = { fullscreenStart = null })
    }
}

@Composable
private fun FullscreenMediaViewer(media: List<MediaItem>, startIndex: Int, onDismiss: () -> Unit) {
    val state = rememberPagerState(initialPage = startIndex.coerceIn(0, media.lastIndex), pageCount = { media.size })
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
                SuqiMediaPreview(
                    item = media[page],
                    isActive = state.currentPage == page,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(LucideIcons.X, contentDescription = suqiStrings().common.closeLabel, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            if (media.size > 1) {
                MediaBadge(
                    "${state.currentPage + 1}/${media.size}",
                    Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }
}
