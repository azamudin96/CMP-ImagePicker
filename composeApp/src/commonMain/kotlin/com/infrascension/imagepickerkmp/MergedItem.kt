package com.infrascension.imagepickerkmp

data class MergedItem(
    val kind: SourceKind,
    val sourceIndex: Int,   // index inside captured[] or selectedPhotos[]
    val model: Any?,         // Coil model (uri/path/etc.)
    val displayName: String
)
