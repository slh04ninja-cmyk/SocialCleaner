package com.socialcleaner.model

data class MediaCategory(
    val name: String,
    val icon: String,
    val files: List<MediaFile>
) {
    val fileCount: Int get() = files.size
    val totalSize: Long get() = files.sumOf { it.size }
    val totalSizeFormatted: String get() = formatSize(totalSize)
}

data class MediaFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long
)

data class AppScanResult(
    val appName: String,
    val appIcon: String,
    val packageName: String,
    val categories: List<MediaCategory>,
    val year: Int
) {
    val totalFiles: Int get() = categories.sumOf { it.fileCount }
    val totalSize: Long get() = categories.sumOf { it.totalSize }
    val totalSizeFormatted: String get() = formatSize(totalSize)
}

data class YearGroup(
    val year: Int,
    val apps: List<AppScanResult>
) {
    val totalFiles: Int get() = apps.sumOf { it.totalFiles }
    val totalSize: Long get() = apps.sumOf { it.totalSize }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes o"
        bytes < 1024 * 1024 -> "${bytes / 1024} Ko"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f Mo", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f Go", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
