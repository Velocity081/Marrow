package com.plain.launcher.util

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WidgetDataProvider {

    fun getBatteryText(context: Context): String {
        return try {
            val intent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
            if (isCharging) "$pct% charging" else "$pct% battery"
        } catch (_: Exception) {
            "85% battery"
        }
    }

    fun isUsagePermissionGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun getScreenTimeToday(context: Context): String? {
        if (!isUsagePermissionGranted(context)) {
            return "screen time (tap to allow)"
        }
        return try {
            val totalTimeMs = ScreenTimeUtil.getQuickScreenTimeTotalMs(context)
            ScreenTimeUtil.formatDuration(totalTimeMs) + " today"
        } catch (_: Exception) {
            null
        }
    }

    fun getNextAlarmText(context: Context, is24Hour: Boolean): String? {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
            val nextAlarm = alarmManager.nextAlarmClock ?: return null
            val triggerTime = nextAlarm.triggerTime
            val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
            val timeStr = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(triggerTime))
            "alarm $timeStr"
        } catch (_: Exception) {
            null
        }
    }
}
