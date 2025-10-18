import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> RemovableCarousel(
    items: List<T>,
    getPainter: (T) -> androidx.compose.ui.graphics.painter.Painter?,
    onRemoveAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    cornerRadiusDp: Int = 16,
    askConfirm: Boolean = true,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })
    var pendingRemoveIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            pageSpacing = 12.dp
        ) { page ->
            val painter = getPainter(items[page])
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "image-$page",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        // Delete button overlay (top-right)
        IconButton(
            onClick = {
                val i = pagerState.currentPage
                if (askConfirm) pendingRemoveIndex = i else onRemoveAt(i)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(40.dp)
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove")
        }
    }

    // Simple confirm dialog (optional)
    if (askConfirm && pendingRemoveIndex != null) {
        val idx = pendingRemoveIndex!!
        Dialog(onDismissRequest = { pendingRemoveIndex = null }) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.Text("Remove this image?")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { pendingRemoveIndex = null }
                        ) { androidx.compose.material3.Text("Cancel") }

                        androidx.compose.material3.Button(
                            onClick = {
                                onRemoveAt(idx)
                                pendingRemoveIndex = null
                            }
                        ) { androidx.compose.material3.Text("Remove") }
                    }
                }
            }
        }
    }
}