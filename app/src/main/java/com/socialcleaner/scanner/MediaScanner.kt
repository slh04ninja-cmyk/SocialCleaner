package com.socialcleaner.scanner

import android.os.Environment
import com.socialcleaner.model.*
import java.io.File
import java.util.Calendar

data class SocialApp(
    val name: String,
    val icon: String,
    val packageName: String,
    val mediaPaths: List<String>,
    val categories: Map<String, List<String>> // category name -> file extensions
)

object AppRegistry {

    val supportedApps = listOf(
        SocialApp(
            name = "WhatsApp",
            icon = "whatsapp",
            packageName = "com.whatsapp",
            mediaPaths = listOf(
                "Android/media/com.whatsapp/WhatsApp/Media",
                "WhatsApp/Media"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp", "gif"),
                "Vidéos" to listOf("mp4", "3gp", "mkv", "avi"),
                "Documents" to listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "zip", "apk"),
                "Voice Notes" to listOf("opus", "ogg", "m4a"),
                "Audio" to listOf("mp3", "aac", "wav", "wma"),
                "Stickers" to listOf("webp", "png"),
                "Vidéo Notes" to listOf("mp4"),
                "GIFs" to listOf("gif", "mp4")
            )
        ),
        SocialApp(
            name = "Telegram",
            icon = "telegram",
            packageName = "org.telegram.messenger",
            mediaPaths = listOf(
                "Android/data/org.telegram.messenger/files/Telegram",
                "Telegram"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp", "gif"),
                "Vidéos" to listOf("mp4", "mkv", "avi"),
                "Documents" to listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "apk"),
                "Audio" to listOf("mp3", "ogg", "m4a", "opus"),
                "Voice Notes" to listOf("opus", "ogg"),
                "Stickers" to listOf("webp", "tgs"),
                "GIFs" to listOf("mp4", "gif")
            )
        ),
        SocialApp(
            name = "Facebook",
            icon = "facebook",
            packageName = "com.facebook.katana",
            mediaPaths = listOf(
                "Android/data/com.facebook.katana/files",
                "Pictures/Facebook"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp"),
                "Vidéos" to listOf("mp4", "3gp"),
                "Documents" to listOf("pdf", "doc", "docx")
            )
        ),
        SocialApp(
            name = "Instagram",
            icon = "instagram",
            packageName = "com.instagram.android",
            mediaPaths = listOf(
                "Android/data/com.instagram.android/files",
                "Pictures/Instagram",
                "DCIM/Instagram"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp"),
                "Vidéos" to listOf("mp4", "mkv")
            )
        ),
        SocialApp(
            name = "Snapchat",
            icon = "snapchat",
            packageName = "com.snapchat.android",
            mediaPaths = listOf(
                "Android/data/com.snapchat.android/files",
                "Snapchat",
                "DCIM/Snapchat"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp"),
                "Vidéos" to listOf("mp4", "mkv")
            )
        ),
        SocialApp(
            name = "TikTok",
            icon = "tiktok",
            packageName = "com.zhiliaoapp.musically",
            mediaPaths = listOf(
                "Android/data/com.zhiliaoapp.musically/files",
                "Movies/TikTok",
                "DCIM/TikTok"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp"),
                "Vidéos" to listOf("mp4", "mkv")
            )
        ),
        SocialApp(
            name = "Messenger",
            icon = "messenger",
            packageName = "com.facebook.orca",
            mediaPaths = listOf(
                "Android/data/com.facebook.orca/files",
                "Pictures/Messenger"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp", "gif"),
                "Vidéos" to listOf("mp4", "3gp"),
                "Audio" to listOf("mp3", "ogg", "m4a"),
                "Voice Notes" to listOf("opus", "ogg")
            )
        ),
        SocialApp(
            name = "Signal",
            icon = "signal",
            packageName = "org.thoughtcrime.securesms",
            mediaPaths = listOf(
                "Android/data/org.thoughtcrime.securesms/files"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp"),
                "Vidéos" to listOf("mp4"),
                "Documents" to listOf("pdf", "doc", "docx"),
                "Audio" to listOf("mp3", "ogg", "m4a")
            )
        )
    )
}

class MediaScanner {

    private val basePaths = listOf(
        Environment.getExternalStorageDirectory().absolutePath,
        "/storage/emulated/0"
    )

    fun scanApp(app: SocialApp, targetYear: Int? = null): List<AppScanResult> {
        val results = mutableListOf<AppScanResult>()
        val yearGroups = mutableMapOf<Int, MutableList<MediaFile>>()

        for (mediaPath in app.mediaPaths) {
            for (basePath in basePaths) {
                val fullPath = File(basePath, mediaPath)
                if (fullPath.exists() && fullPath.isDirectory) {
                    scanDirectory(fullPath, app.categories, yearGroups)
                }
            }
        }

        val years = if (targetYear != null) {
            listOf(targetYear)
        } else {
            yearGroups.keys.sorted()
        }

        for (year in years) {
            val files = yearGroups[year] ?: continue
            val categories = categorizeFiles(files, app.categories)

            results.add(
                AppScanResult(
                    appName = app.name,
                    appIcon = app.icon,
                    packageName = app.packageName,
                    categories = categories,
                    year = year
                )
            )
        }

        return results
    }

    private fun scanDirectory(
        dir: File,
        categoryMap: Map<String, List<String>>,
        yearGroups: MutableMap<Int, MutableList<MediaFile>>
    ) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectory(file, categoryMap, yearGroups)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                val isKnown = categoryMap.values.flatten().any { it == ext }
                if (isKnown || ext.isNotEmpty()) {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = file.lastModified()
                    }
                    val year = calendar.get(Calendar.YEAR)

                    val mediaFile = MediaFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified()
                    )

                    yearGroups.getOrPut(year) { mutableListOf() }.add(mediaFile)
                }
            }
        }
    }

    private fun categorizeFiles(
        files: List<MediaFile>,
        categoryMap: Map<String, List<String>>
    ): List<MediaCategory> {
        val categorized = mutableMapOf<String, MutableList<MediaFile>>()
        val uncategorized = mutableListOf<MediaFile>()

        for (file in files) {
            val ext = file.name.substringAfterLast('.', "").lowercase()
            var found = false

            for ((categoryName, extensions) in categoryMap) {
                if (extensions.contains(ext)) {
                    categorized.getOrPut(categoryName) { mutableListOf() }.add(file)
                    found = true
                    break
                }
            }

            if (!found) {
                uncategorized.add(file)
            }
        }

        val result = mutableListOf<MediaCategory>()

        for ((name, categoryFiles) in categorized) {
            result.add(MediaCategory(name, getCategoryIcon(name), categoryFiles))
        }

        if (uncategorized.isNotEmpty()) {
            result.add(MediaCategory("Autres", "other", uncategorized))
        }

        return result.sortedByDescending { it.totalSize }
    }

    private fun getCategoryIcon(name: String): String {
        return when (name.lowercase()) {
            "images" -> "image"
            "vidéos", "videos" -> "video"
            "documents" -> "document"
            "voice notes" -> "mic"
            "audio" -> "music"
            "stickers" -> "sticker"
            "vidéo notes" -> "video_note"
            "gifs" -> "gif"
            else -> "other"
        }
    }
}
