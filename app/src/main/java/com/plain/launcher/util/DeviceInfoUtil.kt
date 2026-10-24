package com.plain.launcher.util

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.text.format.Formatter
import java.io.File
import kotlin.math.ceil

object DeviceInfoUtil {

    fun getProcessorName(): String {
        // 1. Android 12+ (API 31+) SOC info
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            val socManufacturer = Build.SOC_MANUFACTURER
            val validModel = !socModel.isNullOrBlank() && !socModel.equals("unknown", ignoreCase = true)
            val validMan = !socManufacturer.isNullOrBlank() && !socManufacturer.equals("unknown", ignoreCase = true)

            if (validModel) {
                return if (validMan && !socModel.contains(socManufacturer, ignoreCase = true)) {
                    "${socManufacturer.replaceFirstChar { it.uppercase() }} $socModel"
                } else {
                    socModel
                }
            }
        }

        // 2. /proc/cpuinfo Hardware or model name
        try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists() && cpuInfo.canRead()) {
                val lines = cpuInfo.readLines()
                val hardwareLine = lines.firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
                    ?: lines.firstOrNull { it.startsWith("model name", ignoreCase = true) }
                    ?: lines.firstOrNull { it.startsWith("Processor", ignoreCase = true) }
                val value = hardwareLine?.substringAfter(":")?.trim()
                if (!value.isNullOrBlank() && !value.equals("unknown", ignoreCase = true)) {
                    return value
                }
            }
        } catch (_: Exception) {}

        // 3. Build.HARDWARE / Build.BOARD
        val hardware = Build.HARDWARE
        if (!hardware.isNullOrBlank() && !hardware.equals("unknown", ignoreCase = true)) {
            return hardware
        }

        val board = Build.BOARD
        if (!board.isNullOrBlank() && !board.equals("unknown", ignoreCase = true)) {
            return board
        }

        // 4. Fallback: cores + ABI
        val cores = Runtime.getRuntime().availableProcessors()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "CPU"
        return "$cores-Core $abi"
    }

    fun getTotalRam(context: Context): String {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val totalBytes = memInfo.totalMem
            if (totalBytes <= 0) return "Unknown"

            val gb = ceil(totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).toInt()
            if (gb >= 1) {
                "$gb GB"
            } else {
                val mb = ceil(totalBytes.toDouble() / (1024.0 * 1024.0)).toInt()
                "$mb MB"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun getMaxStorage(context: Context): String {
        return try {
            var totalBytes: Long = 0L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val storageStats = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                    totalBytes = storageStats?.getTotalBytes(StorageManager.UUID_DEFAULT) ?: 0L
                } catch (_: Exception) {}
            }

            if (totalBytes <= 0L) {
                try {
                    val stat = StatFs(Environment.getDataDirectory().path)
                    totalBytes = stat.blockCountLong * stat.blockSizeLong
                } catch (_: Exception) {}
            }

            if (totalBytes <= 0L) return "Unknown"

            // Map to standard retail storage tiers (in decimal GB / flash standard)
            val decimalGb = totalBytes / 1_000_000_000.0
            val standardTiers = listOf(16, 32, 64, 128, 256, 512, 1024)
            for (tier in standardTiers) {
                if (decimalGb >= tier * 0.72 && decimalGb <= tier * 1.08) {
                    return if (tier >= 1024) "${tier / 1024} TB" else "$tier GB"
                }
            }

            Formatter.formatFileSize(context, totalBytes)
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
