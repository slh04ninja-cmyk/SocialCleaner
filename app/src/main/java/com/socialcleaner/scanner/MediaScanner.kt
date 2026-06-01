package com.socialcleaner.scanner

import android.os.Environment
import com.socialcleaner.model.*
import java.io.File
import java.util.Calendar
import java.util.regex.Pattern

data class SocialApp(
    val name: String,
    val icon: String,
    val packageName: String,
    val mediaPaths: List<String>,
    val categories: Map<String, List<String>> // category name -> file extensions
)

object AppRegistry {

    // Catégories supplémentaires appliquées à toutes les apps
    val globalCategories = mapOf(
        "Fichiers temporaires" to listOf("tmp", "temp", "cache", "log", "bak", "old", "orig", "partial"),
        "Bases de données" to listOf("db", "sqlite", "sqlite3", "db-wal", "db-shm", "db-journal")
    )

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
        ),
        SocialApp(
            name = "X (Twitter)",
            icon = "twitter",
            packageName = "com.twitter.android",
            mediaPaths = listOf(
                "Android/data/com.twitter.android/files",
                "Pictures/Twitter",
                "DCIM/Twitter"
            ),
            categories = mapOf(
                "Images" to listOf("jpg", "jpeg", "png", "webp", "gif"),
                "Vidéos" to listOf("mp4", "3gp"),
                "Documents" to listOf("pdf", "doc", "docx", "zip")
            )
        )
    )
}

class MediaScanner {

    private val basePaths = listOf(
        Environment.getExternalStorageDirectory().absolutePath
    ).distinct()

    // Patterns pour extraire la date du nom de fichier
    // WhatsApp: IMG-20240115-WA0001.jpg, VID-20240115-WA0001.mp4
    // Telegram: 2024-01-15 12.30.45.jpg
    // Generic: 20240115_123045.jpg, Screenshot_20240115-123045.png
    private val datePatterns = listOf(
        Pattern.compile("(20\\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])"),  // 20240115
        Pattern.compile("(20\\d{2})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])"),  // 2024-01-15
        Pattern.compile("(20\\d{2})_(0[1-9]|1[0-2])_(0[1-9]|[12]\\d|3[01])"),  // 2024_01_15
    )

    fun scanApp(app: SocialApp, targetYear: Int? = null): List<AppScanResult> {
        val results = mutableListOf<AppScanResult>()
        val yearGroups = mutableMapOf<Int, MutableList<MediaFile>>()

        // Merge app categories with global categories
        val mergedCategories = app.categories.toMutableMap()
        for ((name, exts) in AppRegistry.globalCategories) {
            mergedCategories.getOrPut(name) { mutableListOf() }.addAll(exts)
        }

        val seenPaths = mutableSetOf<String>()
        for (mediaPath in app.mediaPaths) {
            for (basePath in basePaths) {
                val fullPath = File(basePath, mediaPath)
                if (fullPath.exists() && fullPath.isDirectory) {
                    scanDirectory(fullPath, mergedCategories, yearGroups, seenPaths)
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
            val categories = categorizeFiles(files, mergedCategories)

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

    /**
     * Estimation rapide du nombre de fichiers (sans les lire).
     * Utilisé pour décider si on active le foreground service.
     */
    fun estimateFileCount(): Int {
        var count = 0
        for (app in AppRegistry.supportedApps) {
            for (mediaPath in app.mediaPaths) {
                for (basePath in basePaths) {
                    val dir = File(basePath, mediaPath)
                    if (dir.exists() && dir.isDirectory) {
                        count += countFilesQuick(dir)
                    }
                }
            }
        }
        return count
    }

    private fun countFilesQuick(dir: File): Int {
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += countFilesQuick(file)
            } else {
                count++
            }
        }
        return count
    }

    /**
     * Extrait l'année d'un fichier en priorisant le nom du fichier,
     * puis le chemin (dossier parent), puis lastModified().
     */
    private fun extractYear(file: File): Int {
        val fileName = file.nameWithoutExtension

        // 1. Essayer d'extraire la date du nom de fichier
        for (pattern in datePatterns) {
            val matcher = pattern.matcher(fileName)
            if (matcher.find()) {
                try {
                    val year = matcher.group(1)!!.toInt()
                    if (year in 2018..2030) {
                        return year
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Essayer d'extraire du chemin (ex: /2024/ ou /Sent/2024/)
        val path = file.absolutePath
        val pathYearPattern = Pattern.compile("[/\\\\](20\\d{2})[/\\\\]")
        val pathMatcher = pathYearPattern.matcher(path)
        if (pathMatcher.find()) {
            try {
                val year = pathMatcher.group(1)!!.toInt()
                if (year in 2018..2030) {
                    return year
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: utiliser lastModified()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = file.lastModified()
        }
        return calendar.get(Calendar.YEAR)
    }

    private fun scanDirectory(
        dir: File,
        categoryMap: Map<String, List<String>>,
        yearGroups: MutableMap<Int, MutableList<MediaFile>>,
        seenPaths: MutableSet<String>
    ) {
        // Symlink protection: skip if not a real directory
        if (!dir.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)) return

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Skip symlinks by checking canonical path
                if (file.canonicalPath == file.absolutePath) {
                    scanDirectory(file, categoryMap, yearGroups, seenPaths)
                }
            } else if (file.isFile && seenPaths.add(file.absolutePath)) {
                val ext = file.extension.lowercase()
                val isKnown = categoryMap.values.flatten().any { it == ext }
                if (isKnown || ext.isNotEmpty()) {
                    val year = extractYear(file)

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
            "fichiers temporaires" -> "temp"
            "bases de données" -> "database"
            else -> "other"
        }
    }
}
