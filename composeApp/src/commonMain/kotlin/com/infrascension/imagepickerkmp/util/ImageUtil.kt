package com.infrascension.imagepickerkmp.util


import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.PhotoResult
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val MAX_IMAGES = 10
const val MAX_BYTES = 5 * 1024 * 1024 // 5MB
val ALLOWED_EXTS = setOf("jpg", "jpeg", "png", "webp")

 enum class FileIssueType { TooBig, BadFormat, Duplicate, LimitReached }

 data class FileIssue(
    val uriString: String,
    val displayName: String,
    val reason: FileIssueType,
    val details: String? = null
)


@ExperimentalTime
fun generateDisplayName(model: Any?): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    fun two(n: Int) = n.toString().padStart(2, '0')

    val timestamp = buildString {
        append(now.year)
        append(two(now.monthNumber))
        append(two(now.dayOfMonth))
        append("_")
        append(two(now.hour))
        append(two(now.minute))
        append(two(now.second))
    }

    val ext = inferExtension(model)
    return "${timestamp}_original.$ext"
}

// NEW: Try to infer an image extension from URI/path; default to "jpg"
// Safe for both Android & iOS — pure Kotlin
fun inferExtension(model: Any?): String {
    val path = when (model) {
        is String -> model
        else -> model?.toString() ?: ""
    }

    val base = path.substringBefore('#').substringBefore('?')
    val segment = base.substringAfterLast('/').substringAfterLast('\\')
    val ext = segment.substringAfterLast('.', missingDelimiterValue = "").lowercase()

    return when (ext) {
        "jpg", "jpeg" -> "jpg"
        "png" -> "png"
        "webp" -> "webp"
        "heic", "heif" -> "jpg"
        else -> "jpg"
    }
}

 fun extFromNameOrUri(nameOrUri: String?): String {
    if (nameOrUri.isNullOrBlank()) return ""
    val base = nameOrUri.substringBefore('#').substringBefore('?')
    val seg  = base.substringAfterLast('/').substringAfterLast('\\')
    return seg.substringAfterLast('.', "").lowercase()
}

 fun bestName(name: String?, uri: String): String {
    val base = (name ?: uri).substringBefore('#').substringBefore('?')
    val seg  = base.substringAfterLast('/').substringAfterLast('\\')
    return if (seg.isNotBlank()) seg else "Unknown"
}

/** Validate format/size first; return accepted + issues (TooBig, BadFormat). */
 fun validateIncomingDetailed(
    incoming: List<GalleryPhotoResult>
): Pair<List<GalleryPhotoResult>, List<FileIssue>> {
    val ok = mutableListOf<GalleryPhotoResult>()
    val issues = mutableListOf<FileIssue>()

    incoming.forEach { r ->
        val uriStr = r.uri.toString()
        val name = bestName(r.fileName, uriStr)
        val ext = extFromNameOrUri(r.fileName ?: uriStr)
        val size = r.fileSize ?: -1L

        val badFmt = ext !in ALLOWED_EXTS
        val tooBig = size >= 0 && size > MAX_BYTES

        when {
            badFmt -> issues += FileIssue(uriStr, name, FileIssueType.BadFormat, "Allowed: jpg, jpeg, png, webp")
            tooBig -> issues += FileIssue(uriStr, name, FileIssueType.TooBig, "Max size: 5 MB")
            else   -> ok += r
        }
    }
    return ok to issues
}

/** Merge into existing, dedupe by URI, cap to MAX_IMAGES; return accepted + issues (Duplicate, LimitReached). */
 fun mergeWithConstraints(
    existing: List<GalleryPhotoResult>,
    validated: List<GalleryPhotoResult>
): Pair<List<GalleryPhotoResult>, List<FileIssue>> {
    val issues = mutableListOf<FileIssue>()
    val keys = existing.map { it.uri.toString() }.toMutableSet()
    val accepted = existing.toMutableList()
    var slotsLeft = (MAX_IMAGES - existing.size).coerceAtLeast(0)

    // first flag duplicates
    validated.forEach { r ->
        val uriStr = r.uri.toString()
        val name = bestName(r.fileName, uriStr)
        if (uriStr in keys) {
            issues += FileIssue(uriStr, name, FileIssueType.Duplicate, "Already added")
        }
    }

    // then add non-dupes until limit
    validated.forEach { r ->
        val uriStr = r.uri.toString()
        val name = bestName(r.fileName, uriStr)
        if (uriStr !in keys) {
            if (slotsLeft > 0) {
                keys += uriStr
                accepted += r
                slotsLeft--
            } else {
                issues += FileIssue(uriStr, name, FileIssueType.LimitReached, "Maximum $MAX_IMAGES photos")
            }
        }
    }

    return accepted to issues
}

fun PhotoResult.asGallery(): GalleryPhotoResult = GalleryPhotoResult(
    uri = this.uri,
    width = this.width,
    height = this.height,
    fileName = this.fileName,
    fileSize = this.fileSize
)