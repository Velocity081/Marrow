package com.plain.launcher.util

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.text.format.Formatter
import com.plain.launcher.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppStorageItem(
    val app: AppItem,
    val sizeBytes: Long,
    val formattedSize: String
)

data class DeviceStorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val totalFormatted: String,
    val availableFormatted: String,
    val usedFormatted: String,
    val usedFraction: Float,
    val appItems: List<AppStorageItem>
)

object StorageUtil {

    fun getQuickStorageInfo(context: Context): DeviceStorageInfo {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBytes = stat.blockCountLong * blockSize
            val availableBytes = stat.availableBlocksLong * blockSize
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
            val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

            DeviceStorageInfo(
                totalBytes = totalBytes,
                availableBytes = availableBytes,
                usedBytes = usedBytes,
                totalFormatted = Formatter.formatFileSize(context, totalBytes),
                availableFormatted = Formatter.formatFileSize(context, availableBytes),
                usedFormatted = Formatter.formatFileSize(context, usedBytes),
                usedFraction = fraction,
                appItems = emptyList()
            )
        } catch (_: Exception) {
            DeviceStorageInfo(0L, 0L, 0L, "0 B", "0 B", "0 B", 0f, emptyList())
        }
    }

    suspend fun getDeviceStorageInfo(context: Context, apps: List<AppItem>): DeviceStorageInfo = withContext(Dispatchers.IO) {
        val quick = getQuickStorageInfo(context)
        val pm = context.packageManager
        val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else null
        val hasUsage = hasUsageStatsPermission(context)

        val uniqueApps = apps.distinctBy { it.packageName }
        val appItems = uniqueApps.mapNotNull { app ->
            val size = getAppStorageSize(pm, storageStatsManager, hasUsage, app)
            AppStorageItem(
                app = app,
                sizeBytes = size,
                formattedSize = Formatter.formatFileSize(context, size)
            )
        }.sortedByDescending { it.sizeBytes }

        quick.copy(appItems = appItems)
    }

    private fun getAppStorageSize(
        pm: PackageManager,
        storageStatsManager: StorageStatsManager?,
        hasUsage: Boolean,
        app: AppItem
    ): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storageStatsManager != null && hasUsage) {
            try {
                val user = app.userHandle ?: android.os.Process.myUserHandle()
                val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, app.packageName, user)
                return stats.appBytes + stats.dataBytes + stats.cacheBytes
            } catch (_: Exception) {}
        }

        return try {
            val appInfo = pm.getApplicationInfo(app.packageName, 0)
            var size = 0L
            appInfo.sourceDir?.let { size += File(it).length() }
            appInfo.splitSourceDirs?.forEach { size += File(it).length() }
            size
        } catch (_: Exception) {
            0L
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openFilesApp(context: Context) {
        val intents = listOf(
            // Google Files
            Intent(Intent.ACTION_MAIN).setPackage("com.google.android.apps.nbu.files"),
            // AOSP DocumentsUI / Files
            Intent(Intent.ACTION_MAIN).setClassName("com.android.documentsui", "com.android.documentsui.files.FilesActivity"),
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "resource/folder")
            },
            // Storage manager action
            Intent("android.os.storage.action.MANAGE_STORAGE"),
            Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            // Samsung My Files
            Intent(Intent.ACTION_MAIN).setPackage("com.sec.android.app.myfiles"),
            // Fallback generic document action
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }

        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
        }
    }
}
