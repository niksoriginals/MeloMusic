package com.niksoriginals.melo.updater

import android.content.Context
import com.niksoriginals.melo.BuildConfig
import com.niksoriginals.melo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object UpdateManager {
    // The niksoriginals repository hosts releases for multiple apps (melo, chat, nofy)
    // Melo releases are strictly tagged with "melo"
    const val TARGET_TAG = "melo"
    private const val RELEASE_TAG_API_URL = "https://api.github.com/repos/niksoriginals/niksoriginals/releases/tags/$TARGET_TAG"
    private const val ALL_RELEASES_API_URL = "https://api.github.com/repos/niksoriginals/niksoriginals/releases?per_page=100"
    private const val CHANGELOG_URL = "https://github.com/niksoriginals/niksoriginals/releases/download/$TARGET_TAG/changelog.json"

    fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        fun extractVersionNumbers(raw: String): List<Int> {
            val semverMatch = Regex("""\b\d+(\.\d+)+\b""").find(raw)?.value
            val clean = semverMatch ?: raw.filter { it.isDigit() || it == '.' }
            return clean.split(".").mapNotNull { it.toIntOrNull() }
        }

        val latestParts = extractVersionNumbers(latestVersion)
        val currentParts = extractVersionNumbers(currentVersion)

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latest = latestParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            when {
                latest > current -> return true
                latest < current -> return false
            }
        }

        val latestClean = latestVersion.removePrefix("b").removePrefix("v").trim()
        val currentClean = currentVersion.removePrefix("b").removePrefix("v").trim()
        if (latestClean == currentClean) {
            val latestIsBeta = latestVersion.startsWith("b")
            val currentIsBeta = currentVersion.startsWith("b")
            if (currentIsBeta && !latestIsBeta) return true
        }

        return false
    }

    private fun formatDate(dateStr: String): String = try {
        val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
        java.time.OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(displayFormatter)
    } catch (_: Exception) {
        try {
            val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
            LocalDateTime.parse(dateStr, isoFormatter).format(displayFormatter)
        } catch (_: Exception) {
            dateStr
        }
    }

    private fun fetchChangelogText(changelogUrlStr: String, fallbackBody: String, context: Context): String {
        return try {
            val conn = (URL(changelogUrlStr).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Melo-Updater")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val description = json.optString("description")
                val sections = json.optJSONArray("changelog")

                val builder = StringBuilder()
                if (description.isNotBlank()) {
                    builder.append(description).append("\n\n")
                }
                if (sections != null) {
                    for (i in 0 until sections.length()) {
                        val section = sections.getJSONObject(i)
                        val title = section.optString("title")
                        val items = section.optJSONArray("items")
                        if (title.isNotBlank()) {
                            builder.append("Version ").append(title).append(":\n")
                        }
                        if (items != null) {
                            for (j in 0 until items.length()) {
                                builder.append("• ").append(items.getString(j)).append("\n")
                            }
                        }
                        builder.append("\n")
                    }
                }
                val formatted = builder.toString().trim()
                if (formatted.isNotBlank()) formatted else fallbackBody
            } else {
                fallbackBody.ifBlank { context.getString(R.string.no_changelog_available) }
            }
        } catch (_: Exception) {
            fallbackBody.ifBlank { context.getString(R.string.no_changelog_available) }
        }
    }

    suspend fun checkForUpdates(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            var releaseObj: JSONObject? = null

            // 1. Try direct tag endpoint for "melo"
            try {
                val conn = (URL(RELEASE_TAG_API_URL).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Melo-Updater")
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 12000
                    readTimeout = 15000
                }
                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    releaseObj = JSONObject(json)
                }
            } catch (_: Exception) { }

            // 2. Fallback: filter for tag "melo" among all repository releases (skipping "chat", "nofy", etc.)
            if (releaseObj == null) {
                val conn = (URL(ALL_RELEASES_API_URL).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Melo-Updater")
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 12000
                    readTimeout = 15000
                }
                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(json)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        if (item.optString("tag_name") == TARGET_TAG) {
                            releaseObj = item
                            break
                        }
                    }
                }
            }

            if (releaseObj == null) {
                return@withContext Result.failure(Exception("Melo release (tag '$TARGET_TAG') not found"))
            }

            val tagName = releaseObj.optString("tag_name", TARGET_TAG)
            val rawName = releaseObj.optString("name").takeIf { it.isNotBlank() } ?: tagName
            val releaseVersion = Regex("""\b\d+(\.\d+)+\b""").find(rawName)?.value
                ?: rawName.removePrefix("v").removePrefix("b").trim()
            val currentVersion = BuildConfig.VERSION_NAME
            val publishedAt = releaseObj.optString("published_at")
            val releaseDate = formatDate(publishedAt)
            val rawBody = releaseObj.optString("body")

            val assets = releaseObj.optJSONArray("assets") ?: JSONArray()
            var apkUrl = ""
            var apkSizeMb = ""
            var fileName = "app-universal-foss-release.apk"
            var changelogJsonUrl = CHANGELOG_URL

            // Look for changelog.json in assets if uploaded
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.equals("changelog.json", ignoreCase = true)) {
                    changelogJsonUrl = asset.optString("browser_download_url", CHANGELOG_URL)
                    break
                }
            }

            // Find best APK asset: prefer app-universal-foss-release.apk, Melo-*-Universal.apk, or any release .apk
            var bestAsset: JSONObject? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.equals("app-universal-foss-release.apk", ignoreCase = true) ||
                    (assetName.contains("universal", ignoreCase = true) && assetName.endsWith(".apk", ignoreCase = true))
                ) {
                    bestAsset = asset
                    break
                }
                if (assetName.endsWith(".apk", ignoreCase = true) && !assetName.lowercase().contains("debug") && bestAsset == null) {
                    bestAsset = asset
                }
            }
            // Fallback to first .apk asset
            if (bestAsset == null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                        bestAsset = asset
                        break
                    }
                }
            }

            bestAsset?.let { asset ->
                apkUrl = asset.optString("browser_download_url")
                val sizeBytes = asset.optLong("size", 0L)
                apkSizeMb = String.format("%.1f", sizeBytes / (1024.0 * 1024.0))
                fileName = asset.optString("name", fileName)
            }

            if (apkUrl.isBlank()) {
                return@withContext Result.failure(Exception("No APK asset found in Melo release"))
            }

            val changelog = fetchChangelogText(changelogJsonUrl, rawBody, context)
            val isAvailable = isNewerVersion(releaseVersion, currentVersion)

            Result.success(
                UpdateInfo(
                    tagName = tagName,
                    versionName = releaseVersion,
                    isAvailable = isAvailable,
                    apkUrl = apkUrl,
                    apkSizeMb = apkSizeMb,
                    publishedDate = releaseDate,
                    changelog = changelog,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
