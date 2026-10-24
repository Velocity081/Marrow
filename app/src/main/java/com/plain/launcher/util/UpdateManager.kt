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
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVer = getCurrentVersion(context)
        try {
            val url = URL(LATEST_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Marrow-Launcher")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                // No releases published yet on GitHub repository
                return@withContext UpdateCheckResult.UpToDate(currentVer)
            }

            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.UpToDate(currentVer)
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")
            val assets = json.optJSONArray("assets")

            var apkDownloadUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            val remoteVer = tagName.removePrefix("v").trim()
            val cleanCurrent = currentVer.removePrefix("v").trim()

            if (isNewer(remoteVer, cleanCurrent) && !apkDownloadUrl.isNullOrBlank()) {
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

    private fun isNewer(remote: String, current: String): Boolean {
        if (remote.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val rVal = rParts.getOrElse(i) { 0 }
            val cVal = cParts.getOrElse(i) { 0 }
            if (rVal > cVal) return true
            if (rVal < cVal) return false
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
            } else {
                break
            }
        }

        val totalBytes = connection.contentLength.toFloat()
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "marrow_update.apk")

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

        targetFile
    }

    fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) { }
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
