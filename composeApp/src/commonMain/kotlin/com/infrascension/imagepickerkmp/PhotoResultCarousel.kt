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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.ismoy.imagepickerkmp.domain.models.PhotoResult

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoResultCarousel(
    photos: List<PhotoResult>,
    onRemoveAt: (Int) -> Unit,
    onOpenFull: (Int) -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    cornerRadiusDp: Int = 16
) {
    if (photos.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { photos.size })

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
                val item = photos[page]
                val model = remember(item) { item.coilModel() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(page) {
                            detectTapGestures(onTap = { onOpenFull(page) })
                        }
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Captured $page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            DotsIndicator(totalDots = photos.size, selectedIndex = pagerState.currentPage)
        }

        IconButton(
            onClick = { onRemoveAt(pagerState.currentPage) },
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

fun PhotoResult.coilModel(): Any? = this.uri