package com.socialcleaner.model

import android.content.Context
import com.socialcleaner.R

data class MediaCategory(
    val name: String,
    val icon: String,
    val files: List<MediaFile>
) {
    val fileCount: Int get() = files.size
    val totalSize: Long get() = files.sumOf { it.size }

    fun getLocalizedName(context: Context): String {
        return when (name) {
            "Images" -> context.getString(R.string.cat_images)
            "Vidéos" -> context.getString(R.string.cat_videos)
            "Documents" -> context.getString(R.string.cat_documents)
            "Voice Notes" -> context.getString(R.string.cat_voice_notes)
            "Audio" -> context.getString(R.string.cat_audio)
            "Stickers" -> context.getString(R.string.cat_stickers)
            "Vidéo Notes" -> context.getString(R.string.cat_video_notes)
            "GIFs" -> context.getString(R.string.cat_gifs)
            "Autres" -> context.getString(R.string.cat_others)
            else -> name
        }
    }
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
}

data class YearGroup(
    val year: Int,
    val apps: List<AppScanResult>
) {
    val totalFiles: Int get() = apps.sumOf { it.totalFiles }
    val totalSize: Long get() = apps.sumOf { it.totalSize }
}

fun formatSize(context: Context, bytes: Long): String {
    return when {
        bytes < 1024 -> context.getString(R.string.unit_bytes, bytes)
        bytes < 1024 * 1024 -> context.getString(R.string.unit_ko, bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> context.getString(R.string.unit_mo, bytes / (1024.0 * 1024.0))
        else -> context.getString(R.string.unit_go, bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
