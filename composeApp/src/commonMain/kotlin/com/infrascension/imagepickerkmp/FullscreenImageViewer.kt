package com.infrascension.imagepickerkmp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenImageViewer(
    count: Int,
    initialIndex: Int,
    painterFor: (Int) -> Painter?,
    onClose: () -> Unit,
    onRemoveAt: ((Int) -> Unit)? = null,
) {
    if (count <= 0) {
        // no more images, auto-close
        onClose()
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { count })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,  // swipe between images
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val painter = painterFor(page)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = "Preview $page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Close button (top-left)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // Remove button (top-right)
        if (onRemoveAt != null) {
            IconButton(
                onClick = {
                    val idx = pagerState.currentPage
                    onRemoveAt(idx)

                    // If that was the last image, auto-close on next recomposition
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
