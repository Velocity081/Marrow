package com.plain.launcher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseName: String,
        val releaseNotes: String
    ) : UpdateCheckResult()
    data class Error(val currentVersion: String, val message: String) : UpdateCheckResult()
}

object UpdateManager {

    private const val GITHUB_OWNER = "Velocity081"
    private const val GITHUB_REPO = "Marrow"
    private const val RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
    private const val PREFS_NAME = "marrow_update_pref"
    private const val KEY_INSTALLED_TAG = "installed_tag"

    fun getInstalledVersion(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTag = prefs.getString(KEY_INSTALLED_TAG, null)
        if (!savedTag.isNullOrBlank()) {
            return savedTag
        }
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getCurrentVersion(context: Context): String {
        return getInstalledVersion(context)
    }

    fun recordInstalledVersion(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INSTALLED_TAG, tag)
            .apply()
    }

    suspend fun checkForUpdates(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVer = getInstalledVersion(context)
        try {
            val url = URL(RELEASES_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Marrow-Launcher")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.UpToDate(currentVer)
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val releases = org.json.JSONArray(responseText)
            if (releases.length() == 0) {
                return@withContext UpdateCheckResult.UpToDate(currentVer)
            }

            var latestRelease: JSONObject? = null
            var apkDownloadUrl: String? = null

            for (i in 0 until releases.length()) {
                val rel = releases.getJSONObject(i)
                if (rel.optBoolean("draft", false)) continue
                val assets = rel.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url")
                        latestRelease = rel
                        break
                    }
                }
                if (latestRelease != null && !apkDownloadUrl.isNullOrBlank()) {
                    break
                }
            }

            if (latestRelease == null || apkDownloadUrl.isNullOrBlank()) {
                return@withContext UpdateCheckResult.UpToDate(currentVer)
            }

            val tagName = latestRelease.optString("tag_name", "").trim()
            val releaseName = latestRelease.optString("name", tagName)
            val releaseNotes = latestRelease.optString("body", "")

            val remoteVer = tagName.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVer.removePrefix("v").removePrefix("V").trim()

            if (isNewer(remoteVer, cleanCurrent)) {
                UpdateCheckResult.UpdateAvailable(
                    currentVersion = currentVer,
                    latestVersion = remoteVer,
                    downloadUrl = apkDownloadUrl,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes
                )
            } else {
                UpdateCheckResult.UpToDate(currentVer)
            }
        } catch (e: Exception) {
            UpdateCheckResult.UpToDate(currentVer)
        }
    }

    private data class ParsedVersion(
        val mainParts: List<Int>,
        val isPreRelease: Boolean,
        val preReleaseNumber: Int
    )

    private fun parseVersion(v: String): ParsedVersion {
        val clean = v.trim().removePrefix("v").removePrefix("V")
        val parts = clean.split("-", limit = 2)
        val mainParts = parts[0].split(".").mapNotNull { it.toIntOrNull() }

        val preStr = if (parts.size > 1) parts[1] else ""
        val isPre = preStr.isNotBlank()
        val preNum = Regex("\\d+").find(preStr)?.value?.toIntOrNull() ?: 0

        return ParsedVersion(mainParts, isPre, preNum)
    }

    fun isNewer(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")

        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) return false

        val r = parseVersion(cleanRemote)
        val c = parseVersion(cleanCurrent)

        val maxParts = maxOf(r.mainParts.size, c.mainParts.size)
        for (i in 0 until maxParts) {
            val rVal = r.mainParts.getOrElse(i) { 0 }
            val cVal = c.mainParts.getOrElse(i) { 0 }
            if (rVal != cVal) return rVal > cVal
        }

        if (c.isPreRelease && r.isPreRelease) {
            return r.preReleaseNumber > c.preReleaseNumber
        }
        if (c.isPreRelease && !r.isPreRelease) {
            return true
        }
        if (!c.isPreRelease && r.isPreRelease) {
            return r.preReleaseNumber > 0
        }

        return false
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        var currentUrl = downloadUrl
        var connection: HttpURLConnection
        var redirectCount = 0

        while (true) {
            connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code in 300..399 && redirectCount < 5) {
                val loc = connection.getHeaderField("Location") ?: break
                connection.disconnect()
                currentUrl = loc
                redirectCount++
            } else if (code in 200..299) {
                break
            } else {
                throw IllegalStateException("HTTP $code")
            }
        }

        val totalBytes = connection.contentLength.toFloat()
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "marrow_update.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        connection.inputStream.use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var readTotal = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    readTotal += bytesRead
                    if (totalBytes > 0) {
                        onProgress((readTotal / totalBytes).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        }

        targetFile.setReadable(true, false)
        targetFile
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) { }
            }
        }
    }

    fun installApk(context: Context, apkFile: File, newVersionTag: String? = null): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                requestInstallPermission(context)
                return false
            }
        }

        if (!newVersionTag.isNullOrBlank()) {
            recordInstalledVersion(context, newVersionTag)
        }

        apkFile.setReadable(true, false)

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val resolveInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
        for (resolveInfo in resolveInfoList) {
            val pkg = resolveInfo.activityInfo.packageName
            context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
        return true
    }
}
