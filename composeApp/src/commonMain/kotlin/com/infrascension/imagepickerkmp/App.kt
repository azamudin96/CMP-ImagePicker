package com.infrascension.imagepickerkmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.infrascension.imagepickerkmp.util.FileIssue
import com.infrascension.imagepickerkmp.util.FileIssueType
import com.infrascension.imagepickerkmp.util.MAX_IMAGES
import com.infrascension.imagepickerkmp.util.asGallery
import com.infrascension.imagepickerkmp.util.generateDisplayName
import com.infrascension.imagepickerkmp.util.mergeWithConstraints
import com.infrascension.imagepickerkmp.util.validateIncomingDetailed
import io.github.ismoy.imagepickerkmp.domain.config.CameraCaptureConfig
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.config.PermissionAndConfirmationConfig
import io.github.ismoy.imagepickerkmp.domain.models.CompressionLevel
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.domain.models.PhotoResult
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.ExperimentalTime

/* ───────────────────────────────────────────────────────────────────────────── */
/*  App()                                                                       */
/* ───────────────────────────────────────────────────────────────────────────── */

@OptIn(ExperimentalTime::class)
@Composable
fun App() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // full-screen viewer state
    var viewerState by remember { mutableStateOf<ViewerState?>(null) }

    var fileIssues by remember { mutableStateOf<List<FileIssue>>(emptyList()) }
    var showIssuesSheet by remember { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
            var showCamera by remember { mutableStateOf(false) }
            var showGallery by remember { mutableStateOf(false) }

            var captured by remember { mutableStateOf<List<PhotoResult>>(emptyList()) }
            var selectedPhotos by remember { mutableStateOf<List<GalleryPhotoResult>>(emptyList()) }

            suspend fun showUndo(message: String, onUndo: () -> Unit) {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "UNDO",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) onUndo()
            }

            // Build merged items each recomposition when sources change
            val merged: List<MergedItem> = remember(captured, selectedPhotos) {
                buildList {
                    captured.forEachIndexed { i, p ->
                        val model = p.coilModel()
                        add(MergedItem(SourceKind.Captured, i, model, generateDisplayName(model)))
                    }
                    selectedPhotos.forEachIndexed { i, g ->
                        val model = g.coilModel()
                        add(MergedItem(SourceKind.Gallery, i, model, generateDisplayName(model)))
                    }
                }
            }

            var mergedCurrentIndex by remember(merged) { mutableStateOf(0) }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { showCamera = true }) { Text("Capture") }
                    Button(onClick = { showGallery = true }) { Text("Gallery") }
                    OutlinedButton(
                        onClick = {
                            captured = emptyList()
                            selectedPhotos = emptyList()
                        },
                        enabled = captured.isNotEmpty() || selectedPhotos.isNotEmpty()
                    ) { Text("Clear all") }
                }

                Spacer(Modifier.height(24.dp))

                if (merged.isNotEmpty()) {
                    Text(
                        text = merged.getOrNull(mergedCurrentIndex)?.displayName ?: "—",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    CapturedPhotoGalleyCarousel(
                        items = merged,
                        onRemoveAtMergedIndex = { mergedIndex ->
                            val item = merged[mergedIndex]
                            when (item.kind) {
                                SourceKind.Captured -> {
                                    val removed = captured[item.sourceIndex]
                                    captured = captured.toMutableList()
                                        .also { it.removeAt(item.sourceIndex) }
                                    coroutineScope.launch {
                                        showUndo("Removed captured photo") {
                                            captured = captured.toMutableList().also {
                                                it.add(
                                                    item.sourceIndex.coerceAtMost(it.size),
                                                    removed
                                                )
                                            }
                                        }
                                    }
                                }

                                SourceKind.Gallery -> {
                                    val removed = selectedPhotos[item.sourceIndex]
                                    selectedPhotos = selectedPhotos.toMutableList()
                                        .also { it.removeAt(item.sourceIndex) }
                                    coroutineScope.launch {
                                        showUndo("Removed gallery image") {
                                            selectedPhotos = selectedPhotos.toMutableList().also {
                                                it.add(
                                                    item.sourceIndex.coerceAtMost(it.size),
                                                    removed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onOpenFull = { startIndex ->
                            viewerState = ViewerState(ViewerList.Mixed, startIndex)
                        },
                        onPageChanged = { idx -> mergedCurrentIndex = idx },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            /* ── Launchers (unchanged) ───────────────────────────────────────── */

            if (showCamera) {
                ImagePickerLauncher(
                    config = ImagePickerConfig(
                        enableCrop = false,
                        onPhotoCaptured = { result ->
                            // 1) Convert to the same shape the validators expect
                            val incoming = listOf(result.asGallery())

                            // 2) Validate size/format per-file
                            val (valid, vIssues) = validateIncomingDetailed(incoming)

                            // 3) Merge into existing list, dedupe by uri, cap MAX_IMAGES
                            val (merged, mIssues) = mergeWithConstraints(
                                existing = selectedPhotos,
                                validated = valid
                            )

                            // 4) Apply state
                            val addedCount = merged.size - selectedPhotos.size
                            selectedPhotos = merged
                            showCamera = false

                            // 5) Per-file UI: show sheet if any issues, else a small snackbar
                            val allIssues = vIssues + mIssues
                            if (allIssues.isNotEmpty()) {
                                fileIssues =
                                    allIssues              // <-- your remember { mutableStateOf<List<FileIssue>>(...) }
                                showIssuesSheet =
                                    true              // <-- your remember { mutableStateOf(false) }
                            } else {
                                coroutineScope.launch {
                                    if (addedCount == 1) {
                                        snackbarHostState.showSnackbar("Added 1 photo.")
                                    } else {
                                        snackbarHostState.showSnackbar("No new photo (duplicate or limit reached).")
                                    }
                                }
                            }

                            // Optional: if you want immediate limit feedback
                            if (selectedPhotos.size >= MAX_IMAGES) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Max $MAX_IMAGES photos reached.") }
                            }
                        },
                        onError = { showCamera = false },
                        onDismiss = { showCamera = false },
                        cameraCaptureConfig = CameraCaptureConfig(
                            compressionLevel = CompressionLevel.MEDIUM,
                            permissionAndConfirmationConfig = PermissionAndConfirmationConfig(
                                skipConfirmation = true
                            )
                        )
                    )
                )
            }

            if (showGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { picked ->
                        // Step 1: format & size validation
                        val (valid, vIssues) = validateIncomingDetailed(picked)
                        // Step 2: merge with existing, dedupe + cap
                        val (merged, mIssues) = mergeWithConstraints(
                            existing = selectedPhotos,
                            validated = valid
                        )

                        selectedPhotos = merged
                        showGallery = false

                        // Collate & show issues (if any)
                        val allIssues = vIssues + mIssues
                        if (allIssues.isNotEmpty()) {
                            fileIssues = allIssues
                            showIssuesSheet = true
                        } else {
                            // nice feedback when everything OK
                            val added =
                                merged.size - selectedPhotos.size // careful: compute before you reassign if needed
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Added ${valid.size} photo${if (valid.size == 1) "" else "s"}.")
                            }
                        }
                    },
                    onError = { showGallery = false },
                    onDismiss = { showGallery = false },
                    enableCrop = false,
                    allowMultiple = true,
                    mimeTypes = listOf(MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG, MimeType.IMAGE_WEBP)
                )
            }


            /* ── Full-screen viewer overlay (Mixed) ──────────────────────────── */

            viewerState?.let { vs ->
                when (vs.list) {
                    ViewerList.Mixed -> {
                        // Rebuild merged again here to stay in sync
                        val mergedNow: List<MergedItem> = remember(captured, selectedPhotos) {
                            buildList {
                                captured.forEachIndexed { i, p ->
                                    val model = p.coilModel()
                                    add(
                                        MergedItem(
                                            SourceKind.Captured,
                                            i,
                                            model,
                                            generateDisplayName(model)
                                        )
                                    )
                                }
                                selectedPhotos.forEachIndexed { i, g ->
                                    val model = g.coilModel()
                                    add(
                                        MergedItem(
                                            SourceKind.Gallery,
                                            i,
                                            model,
                                            generateDisplayName(model)
                                        )
                                    )
                                }
                            }
                        }

                        FullscreenImageViewer2(
                            count = mergedNow.size,
                            initialIndex = vs.startIndex.coerceIn(0, max(0, mergedNow.size - 1)),
                            modelFor = { i -> mergedNow.getOrNull(i)?.model },
                            onClose = { viewerState = null },
                            onRemoveAt = { i ->
                                val item = mergedNow.getOrNull(i) ?: return@FullscreenImageViewer2
                                when (item.kind) {
                                    SourceKind.Captured -> {
                                        val removed = captured.getOrNull(item.sourceIndex)
                                            ?: return@FullscreenImageViewer2
                                        captured = captured.toMutableList()
                                            .also { it.removeAt(item.sourceIndex) }
                                        coroutineScope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = "Removed captured photo",
                                                actionLabel = "UNDO",
                                                withDismissAction = true
                                            )
                                            if (res == SnackbarResult.ActionPerformed) {
                                                captured = captured.toMutableList().also {
                                                    it.add(
                                                        item.sourceIndex.coerceAtMost(it.size),
                                                        removed
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    SourceKind.Gallery -> {
                                        val removed = selectedPhotos.getOrNull(item.sourceIndex)
                                            ?: return@FullscreenImageViewer2
                                        selectedPhotos = selectedPhotos.toMutableList()
                                            .also { it.removeAt(item.sourceIndex) }
                                        coroutineScope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = "Removed gallery image",
                                                actionLabel = "UNDO",
                                                withDismissAction = true
                                            )
                                            if (res == SnackbarResult.ActionPerformed) {
                                                selectedPhotos =
                                                    selectedPhotos.toMutableList().also {
                                                        it.add(
                                                            item.sourceIndex.coerceAtMost(it.size),
                                                            removed
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                }
                                if (captured.isEmpty() && selectedPhotos.isEmpty()) viewerState =
                                    null
                            }
                        )
                    }
                    // (Optional) keep legacy modes if you still call them somewhere:
                    ViewerList.Captured, ViewerList.Gallery -> {
                        // No-op; you can delete these if you never navigate to them anymore.
                    }
                }
            }
        }

        if (showIssuesSheet) {
            FileIssuesSheet(
                issues = fileIssues,
                onDismiss = { showIssuesSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileIssuesSheet(
    issues: List<FileIssue>,
    onDismiss: () -> Unit
) {
    if (issues.isEmpty()) return
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Some files were skipped", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            issues.forEach { issue ->
                val (emoji, color, reasonText) = when (issue.reason) {
                    FileIssueType.BadFormat -> Triple(
                        "🧩",
                        MaterialTheme.colorScheme.error,
                        "Unsupported format"
                    )

                    FileIssueType.TooBig -> Triple(
                        "📦",
                        MaterialTheme.colorScheme.error,
                        "File too large"
                    )

                    FileIssueType.Duplicate -> Triple(
                        "🔁",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "Duplicate"
                    )

                    FileIssueType.LimitReached -> Triple(
                        "🚫",
                        MaterialTheme.colorScheme.error,
                        "Limit reached"
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(issue.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            buildString {
                                append(reasonText)
                                issue.details?.let { append(" — "); append(it) }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }
                    Text(emoji)
                }
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("OK")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
