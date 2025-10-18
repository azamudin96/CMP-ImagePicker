package com.infrascension.imagepickerkmp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.engawapg.lib.zoomable.ScrollGesturePropagation
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.toggleScale
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenImageViewer2(
    count: Int,
    initialIndex: Int,
    modelFor: (Int) -> Any?,
    onClose: () -> Unit,
    onRemoveAt: ((Int) -> Unit)? = null,
) {
    if (count <= 0) { onClose(); return }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { count })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true, // Zoomable will hand off drags to pager when not zoomed
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val model = modelFor(page)
            val zoomState = rememberZoomState(maxScale = 5f)

            // ✅ Correct: reset zoom/pan when this page becomes current (no position needed)
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage == page) zoomState.reset()
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Preview $page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(
                                zoomState = zoomState,
                                // ✅ Pager gets the drag when not zoomed:
                                scrollGesturePropagation = ScrollGesturePropagation.NotZoomed,
                                // ✅ Double-tap with required position:
                                onDoubleTap = { pos -> zoomState.toggleScale(2.5f, pos) },
                                // Optional: disable one-finger zoom if you want
                                // enableOneFingerZoom = false,
                            )
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
        }

        if (onRemoveAt != null) {
            IconButton(
                onClick = {
                    val idx = pagerState.currentPage
                    onRemoveAt(idx)
                    if (count <= 1) onClose()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
            }
        }
    }
}
