package com.plain.launcher.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plain.launcher.data.model.AppTextAlign
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.service.PlainNotificationListenerService
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.components.toAlignment
import com.plain.launcher.ui.components.toComposeAlign
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.ui.theme.getClockFontFamily
import com.plain.launcher.util.HapticUtil
import com.plain.launcher.util.NotificationUtil
import com.plain.launcher.util.WidgetDataProvider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockDateHeader(
    settings: UserSettings,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onScreenTimeClick: (() -> Unit)? = null
) {
    val hasActiveWidgets = settings.enabledWidgets.isNotEmpty()
    if (!settings.showClock && !settings.showDate && !hasActiveWidgets) return

    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    var batteryText by remember { mutableStateOf("") }
    if (settings.enabledWidgets.contains("battery")) {
        LaunchedEffect(Unit) {
            while (true) {
                batteryText = WidgetDataProvider.getBatteryText(context)
                delay(15000)
            }
        }
    }

    val notificationCount by PlainNotificationListenerService.notificationCount.collectAsState()
    val priorityNotificationCount by PlainNotificationListenerService.priorityNotificationCount.collectAsState()
    val nowPlayingText by PlainNotificationListenerService.nowPlayingText.collectAsState()

    var screenTimeText by remember { mutableStateOf<String?>(null) }
    if (settings.enabledWidgets.contains("screen_time")) {
        LaunchedEffect(Unit) {
            while (true) {
                screenTimeText = WidgetDataProvider.getScreenTimeToday(context)
                delay(30000)
            }
        }
    }

    var nextAlarmText by remember { mutableStateOf<String?>(null) }
    if (settings.enabledWidgets.contains("next_alarm")) {
        LaunchedEffect(currentTime, settings.clock24Hour) {
            nextAlarmText = WidgetDataProvider.getNextAlarmText(context, settings.clock24Hour)
        }
    }

    val timeFormatPattern = if (settings.clock24Hour) "HH:mm" else "hh:mm a"
    val timeStr = remember(currentTime, settings.clock24Hour) {
        SimpleDateFormat(timeFormatPattern, Locale.getDefault()).format(currentTime)
    }

    val dateStr = remember(currentTime, settings.dateFormat) {
        try {
            val pattern = settings.dateFormat.ifBlank { "EEE, MMM d" }
            SimpleDateFormat(pattern, Locale.getDefault()).format(currentTime)
        } catch (_: Exception) {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(currentTime)
        }
    }

    val align = settings.clockAlignment.toAlignment()
    val textAlign = settings.clockAlignment.toComposeAlign()

    val headerModifier = if (onLongClick != null) {
        modifier.pointerInput(settings.enableHapticFeedback) {
            detectTapGestures(
                onLongPress = {
                    HapticUtil.performClockLongPress(context, settings.enableHapticFeedback)
                    onLongClick()
                }
            )
        }
    } else {
        modifier
    }

    val secondarySize = (settings.dateWidgetSizeSp * 0.9f).coerceAtLeast(10f).sp

    Column(
        modifier = headerModifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = align
    ) {
        if (settings.showClock) {
            PlainText(
                text = timeStr,
                color = PlainTheme.colors.onBackground,
                fontSize = settings.clockSizeSp.sp,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                fontFamily = getClockFontFamily(settings.clockFont)
            )
        }
        if (settings.showDate) {
            PlainText(
                text = dateStr,
                color = PlainTheme.colors.secondaryText,
                fontSize = settings.dateWidgetSizeSp.sp,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign
            )
        }

        // Render enabled widgets with subtle line separators between them
        var hasRenderedWidget = false
        val separatorColor = PlainTheme.colors.onBackground.copy(alpha = 0.16f)

        for (widgetId in settings.enabledWidgets) {
            when (widgetId) {
                "weather" -> {
                    if (settings.weatherText.isNotBlank()) {
                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }
                        PlainText(
                            text = settings.weatherText,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign
                        )
                        hasRenderedWidget = true
                    }
                }
                "battery" -> {
                    if (batteryText.isNotBlank()) {
                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }
                        PlainText(
                            text = batteryText,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign
                        )
                        hasRenderedWidget = true
                    }
                }
                "notifications" -> {
                    val notifGranted = PlainNotificationListenerService.isPermissionGranted(context)
                    if (notifGranted && notificationCount <= 0) {
                        // Simply hide when there are no unread notifications
                    } else {
                        val notifText = if (notifGranted) {
                            "$notificationCount unread"
                        } else {
                            "notifications (tap to enable)"
                        }
                        val showDot = notifGranted && notificationCount > 0

                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = when (settings.clockAlignment) {
                                AppTextAlign.LEFT -> androidx.compose.foundation.layout.Arrangement.Start
                                AppTextAlign.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
                                AppTextAlign.RIGHT -> androidx.compose.foundation.layout.Arrangement.End
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (!PlainNotificationListenerService.isPermissionGranted(context)) {
                                            context.startActivity(
                                                Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } else {
                                            NotificationUtil.expandNotificationShade(context)
                                        }
                                    }
                                )
                            }
                        ) {
                            if (showDot) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(PlainTheme.colors.secondaryText, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            PlainText(
                                text = notifText,
                                color = PlainTheme.colors.secondaryText,
                                fontSize = secondarySize,
                                fontWeight = FontWeight.Normal,
                                textAlign = textAlign
                            )
                        }
                        hasRenderedWidget = true
                    }

                    if (notifGranted && priorityNotificationCount > 0) {
                        val priorityAlertText = if (priorityNotificationCount == 1) {
                            "1 priority alert"
                        } else {
                            "$priorityNotificationCount priority alerts"
                        }
                        val yellowAlertColor = Color(0xFFFFD600)

                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = when (settings.clockAlignment) {
                                AppTextAlign.LEFT -> androidx.compose.foundation.layout.Arrangement.Start
                                AppTextAlign.CENTER -> androidx.compose.foundation.layout.Arrangement.Center
                                AppTextAlign.RIGHT -> androidx.compose.foundation.layout.Arrangement.End
                            },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        NotificationUtil.expandNotificationShade(context)
                                    }
                                )
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(yellowAlertColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            PlainText(
                                text = priorityAlertText,
                                color = yellowAlertColor,
                                fontSize = secondarySize,
                                fontWeight = FontWeight.Normal,
                                textAlign = textAlign
                            )
                        }
                        hasRenderedWidget = true
                    }
                }
                "now_playing" -> {
                    // Hidden if nothing is playing
                    if (!nowPlayingText.isNullOrBlank()) {
                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }
                        PlainText(
                            text = nowPlayingText!!,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        hasRenderedWidget = true
                    }
                }
                "screen_time" -> {
                    val st = screenTimeText ?: if (!WidgetDataProvider.isUsagePermissionGranted(context)) {
                        "screen time (tap to allow)"
                    } else null

                    if (!st.isNullOrBlank()) {
                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }
                        PlainText(
                            text = st,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (!WidgetDataProvider.isUsagePermissionGranted(context)) {
                                            context.startActivity(
                                                Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } else {
                                            onScreenTimeClick?.invoke()
                                        }
                                    }
                                )
                            }
                        )
                        hasRenderedWidget = true
                    }
                }
                "next_alarm" -> {
                    if (!nextAlarmText.isNullOrBlank()) {
                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(separatorColor)
                            )
                        }
                        PlainText(
                            text = nextAlarmText!!,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign
                        )
                        hasRenderedWidget = true
                    }
                }
            }
        }
    }
}
