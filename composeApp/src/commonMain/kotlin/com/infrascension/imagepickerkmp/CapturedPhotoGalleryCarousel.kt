package com.infrascension.imagepickerkmp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CapturedPhotoGalleyCarousel(
    items: List<MergedItem>,
    onRemoveAtMergedIndex: (Int) -> Unit,
    onOpenFull: (Int) -> Unit,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    cornerRadiusDp: Int = 16
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState.currentPage, items.size) {
        onPageChanged(pagerState.currentPage.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }

    Box(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(cornerRadiusDp.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                pageSpacing = 12.dp
            ) { page ->
                val model = items[page].model
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(page) { detectTapGestures(onTap = { onOpenFull(page) }) }
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Photo $page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            DotsIndicator(totalDots = items.size, selectedIndex = pagerState.currentPage)
        }

        IconButton(
            onClick = { onRemoveAtMergedIndex(pagerState.currentPage) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}