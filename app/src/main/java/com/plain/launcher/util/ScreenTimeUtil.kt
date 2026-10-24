package com.plain.launcher.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.plain.launcher.data.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class AppScreenTimeItem(
    val app: AppItem,
    val totalTimeMs: Long,
    val formattedTime: String
)

data class ScreenTimeInfo(
    val totalTimeMs: Long,
    val totalFormatted: String,
    val limitMs: Long = 4L * 60 * 60 * 1000L,
    val usedFraction: Float,
    val isLimitReached: Boolean,
    val hasPermission: Boolean,
    val appItems: List<AppScreenTimeItem>
)

object ScreenTimeUtil {

    const val FOUR_HOURS_MS = 4L * 60 * 60 * 1000L

    fun getStartOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun formatDuration(timeMs: Long): String {
        if (timeMs <= 0L) return "0m"
        val hours = timeMs / (1000 * 60 * 60)
        val minutes = (timeMs / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun getQuickScreenTimeInfo(context: Context): ScreenTimeInfo {
        val hasPermission = WidgetDataProvider.isUsagePermissionGranted(context)
        return ScreenTimeInfo(
            totalTimeMs = 0L,
            totalFormatted = "0m",
            limitMs = FOUR_HOURS_MS,
            usedFraction = 0f,
            isLimitReached = false,
            hasPermission = hasPermission,
            appItems = emptyList()
        )
    }

    fun getQuickScreenTimeTotalMs(context: Context): Long {
        if (!WidgetDataProvider.isUsagePermissionGranted(context)) return 0L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L
        val startTime = getStartOfToday()
        val endTime = System.currentTimeMillis()
        val excluded = getExcludedPackages(context)
        val usageMap = getAppUsage(usageStatsManager, startTime, endTime, excluded)
        return usageMap.values.sum()
    }

    suspend fun getScreenTimeInfo(
        context: Context,
        apps: List<AppItem>
    ): ScreenTimeInfo = withContext(Dispatchers.IO) {
        val hasPermission = WidgetDataProvider.isUsagePermissionGranted(context)
        if (!hasPermission) {
            return@withContext ScreenTimeInfo(
                totalTimeMs = 0L,
                totalFormatted = "0m",
                limitMs = FOUR_HOURS_MS,
                usedFraction = 0f,
                isLimitReached = false,
                hasPermission = false,
                appItems = emptyList()
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext ScreenTimeInfo(
                totalTimeMs = 0L,
                totalFormatted = "0m",
                limitMs = FOUR_HOURS_MS,
                usedFraction = 0f,
                isLimitReached = false,
                hasPermission = true,
                appItems = emptyList()
            )

        val startTime = getStartOfToday()
        val endTime = System.currentTimeMillis()
        val excludedPackages = getExcludedPackages(context)

        val usageByPackage = getAppUsage(usageStatsManager, startTime, endTime, excludedPackages)

        val uniqueApps = apps.distinctBy { it.packageName }
        val appItems = uniqueApps.mapNotNull { app ->
            val timeMs = usageByPackage[app.packageName] ?: 0L
            if (timeMs > 0L) {
                AppScreenTimeItem(
                    app = app,
                    totalTimeMs = timeMs,
                    formattedTime = formatDuration(timeMs)
                )
            } else null
        }.sortedByDescending { it.totalTimeMs }

        val totalMs = appItems.sumOf { it.totalTimeMs }
        val fraction = (totalMs.toFloat() / FOUR_HOURS_MS.toFloat()).coerceIn(0f, 1f)
        val isLimitReached = totalMs >= FOUR_HOURS_MS

        ScreenTimeInfo(
            totalTimeMs = totalMs,
            totalFormatted = formatDuration(totalMs),
            limitMs = FOUR_HOURS_MS,
            usedFraction = fraction,
            isLimitReached = isLimitReached,
            hasPermission = true,
            appItems = appItems
        )
    }

    fun getExcludedPackages(context: Context): Set<String> {
        val excluded = mutableSetOf<String>()
        excluded.add(context.packageName) // Marrow itself
        excluded.add("com.android.systemui")

        // Exclude system launchers (home apps)
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfos = context.packageManager.queryIntentActivities(homeIntent, 0)
            for (info in resolveInfos) {
                info.activityInfo?.packageName?.let { excluded.add(it) }
            }
        } catch (_: Exception) {}

        return excluded
    }

    fun getAppUsage(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        excludedPackages: Set<String>
    ): Map<String, Long> {
        val appTimes = mutableMapOf<String, Long>()

        // 1. High precision event replay from UsageEvents
        try {
            // Buffer 1 hour before midnight to catch any app open across midnight boundary
            val bufferStartTime = (startTime - 60 * 60 * 1000L).coerceAtLeast(0L)
            val events = usageStatsManager.queryEvents(bufferStartTime, endTime)
            if (events != null && events.hasNextEvent()) {
                var currentPackage: String? = null
                var currentStartTime = 0L
                var isInteractive = true

                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val time = event.timeStamp
                    val pkg = event.packageName

                    when (event.eventType) {
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                        UsageEvents.Event.KEYGUARD_SHOWN -> {
                            if (isInteractive) {
                                currentPackage?.let { p ->
                                    if (p !in excludedPackages) {
                                        val sessionStart = maxOf(currentStartTime, startTime)
                                        val sessionEnd = minOf(time, endTime)
                                        if (sessionEnd > sessionStart) {
                                            val duration = sessionEnd - sessionStart
                                            appTimes[p] = (appTimes[p] ?: 0L) + duration
                                        }
                                    }
                                }
                                currentPackage = null
                                isInteractive = false
                            }
                        }
                        UsageEvents.Event.SCREEN_INTERACTIVE,
                        UsageEvents.Event.KEYGUARD_HIDDEN -> {
                            isInteractive = true
                        }
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            if (isInteractive) {
                                if (currentPackage != null && currentPackage != pkg) {
                                    if (currentPackage !in excludedPackages) {
                                        val sessionStart = maxOf(currentStartTime, startTime)
                                        val sessionEnd = minOf(time, endTime)
                                        if (sessionEnd > sessionStart) {
                                            val duration = sessionEnd - sessionStart
                                            appTimes[currentPackage!!] = (appTimes[currentPackage!!] ?: 0L) + duration
                                        }
                                    }
                                }
                                currentPackage = pkg
                                currentStartTime = time
                            }
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED,
                        UsageEvents.Event.ACTIVITY_STOPPED -> {
                            if (isInteractive && currentPackage == pkg) {
                                if (pkg !in excludedPackages) {
                                    val sessionStart = maxOf(currentStartTime, startTime)
                                    val sessionEnd = minOf(time, endTime)
                                    if (sessionEnd > sessionStart) {
                                        val duration = sessionEnd - sessionStart
                                        appTimes[pkg] = (appTimes[pkg] ?: 0L) + duration
                                    }
                                }
                                currentPackage = null
                            }
                        }
                    }
                }

                // If an app is still in foreground right now
                if (isInteractive && currentPackage != null && currentPackage !in excludedPackages) {
                    val sessionStart = maxOf(currentStartTime, startTime)
                    val sessionEnd = endTime
                    if (sessionEnd > sessionStart) {
                        val duration = sessionEnd - sessionStart
                        appTimes[currentPackage!!] = (appTimes[currentPackage!!] ?: 0L) + duration
                    }
                }

                if (appTimes.isNotEmpty()) {
                    return appTimes
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback to queryAndAggregateUsageStats if queryEvents didn't yield items
        try {
            val aggregated = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            if (!aggregated.isNullOrEmpty()) {
                for ((pkg, stats) in aggregated) {
                    if (pkg !in excludedPackages && stats.totalTimeInForeground > 0L) {
                        appTimes[pkg] = stats.totalTimeInForeground
                    }
                }
            }
        } catch (_: Exception) {}

        return appTimes
    }

    fun openDigitalWellbeingOrSettings(context: Context) {
        val intents = listOf(
            Intent("android.settings.DIGITAL_WELLBEING_SETTINGS"),
            Intent("com.google.android.apps.wellbeing.action.APP_USAGE"),
            Intent(Settings.ACTION_APP_USAGE_SETTINGS),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
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
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }
}
