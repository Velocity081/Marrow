package com.plain.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.AppTextAlign
import com.plain.launcher.data.model.AppVerticalAlign
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.ui.theme.LocalPlainSettings
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.ui.theme.getAppFontFamily
import com.plain.launcher.ui.theme.getClockFontFamily
import com.plain.launcher.util.AppIconManager
import com.plain.launcher.util.TextTransforms
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreenPreview(
    settings: UserSettings,
    homeApps: List<AppItem>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenAspect = (configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat().coerceAtLeast(1f))
        .coerceIn(0.42f, 0.56f)

    val previewHeight = 320.dp
    val previewWidth = previewHeight * screenAspect

    val verticalArrangement = when (settings.verticalAlign) {
        AppVerticalAlign.TOP -> Arrangement.Top
        AppVerticalAlign.CENTER -> Arrangement.Center
        AppVerticalAlign.BOTTOM -> Arrangement.Bottom
    }

    CompositionLocalProvider(LocalPlainSettings provides settings) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Authentic phone aspect ratio chassis
            Box(
                modifier = Modifier
                    .width(previewWidth)
                    .height(previewHeight)
                    .clip(RoundedCornerShape(26.dp))
                    .background(PlainTheme.colors.background)
                    .border(
                        BorderStroke(
                            1.8.dp,
                            PlainTheme.colors.onBackground.copy(alpha = 0.28f)
                        ),
                        RoundedCornerShape(26.dp)
                    )
            ) {
                // Top speaker / notch pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 7.dp)
                        .size(width = 24.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PlainTheme.colors.onBackground.copy(alpha = 0.25f))
                )

                // Miniature 1:1 Phone Home Screen Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (settings.showStatusBar) 15.dp else 10.dp,
                            bottom = 12.dp,
                            start = 6.dp,
                            end = 6.dp
                        ),
                    verticalArrangement = verticalArrangement
                ) {
                    PreviewClockHeader(settings = settings)

                    Spacer(modifier = Modifier.height(6.dp))

                    PreviewAppList(settings = settings, homeApps = homeApps)
                }
            }
        }
    }
}

@Composable
private fun PreviewClockHeader(
    settings: UserSettings,
    modifier: Modifier = Modifier
) {
    val hasActiveWidgets = settings.enabledWidgets.isNotEmpty()
    if (!settings.showClock && !settings.showDate && !hasActiveWidgets) return

    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(Date()) }
    var batteryText by remember { mutableStateOf("85% battery") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    if (settings.showBattery) {
        LaunchedEffect(Unit) {
            try {
                val intent = context.registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                )
                val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
                batteryText = "$pct% battery"
            } catch (_: Exception) {}
        }
    }

    val timeFormatPattern = if (settings.clock24Hour) "HH:mm" else "hh:mm a"
    val timeStr = remember(currentTime, settings.clock24Hour) {
        SimpleDateFormat(timeFormatPattern, Locale.getDefault()).format(currentTime)
    }

    val dateStr = remember(currentTime, settings.dateFormat) {
        try {
            SimpleDateFormat(settings.dateFormat, Locale.getDefault()).format(currentTime)
        } catch (_: Exception) {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(currentTime)
        }
    }

    val align = settings.clockAlignment.toAlignment()
    val textAlign = settings.clockAlignment.toComposeAlign()
    val fontFamily = getAppFontFamily(settings.selectedFont)

    val clockSize = (settings.clockSizeSp * 0.36f).coerceAtLeast(8.5f).sp
    val dateSize = (settings.dateWidgetSizeSp * 0.36f).coerceAtLeast(5.5f).sp
    val secondarySize = (settings.dateWidgetSizeSp * 0.36f * 0.9f).coerceAtLeast(5.0f).sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalAlignment = align
    ) {
        if (settings.showClock) {
            PlainText(
                text = timeStr,
                color = PlainTheme.colors.onBackground,
                fontSize = clockSize,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                fontFamily = getClockFontFamily(settings.clockFont)
            )
        }
        if (settings.showDate) {
            PlainText(
                text = dateStr,
                color = PlainTheme.colors.secondaryText,
                fontSize = dateSize,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
                fontFamily = fontFamily
            )
        }
        var hasRenderedWidget = false
        val separatorColor = PlainTheme.colors.onBackground.copy(alpha = 0.16f)

        for (widgetId in settings.enabledWidgets) {
            when (widgetId) {
                "weather" -> {
                    if (settings.weatherText.isNotBlank()) {
                        if (hasRenderedWidget) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 1.5.dp)
                                    .width(10.dp)
                                    .height(0.8.dp)
                                    .background(separatorColor)
                            )
                        }
                        PlainText(
                            text = settings.weatherText,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                            fontFamily = fontFamily
                        )
                        hasRenderedWidget = true
                    }
                }
                "battery" -> {
                    if (hasRenderedWidget) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .width(14.dp)
                                .height(1.dp)
                                .background(separatorColor)
                        )
                    }
                    PlainText(
                        text = batteryText,
                        color = PlainTheme.colors.secondaryText,
                        fontSize = secondarySize,
                        fontWeight = FontWeight.Normal,
                        textAlign = textAlign,
                        fontFamily = fontFamily
                    )
                    hasRenderedWidget = true
                }
                "notifications" -> {
                    if (hasRenderedWidget) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .width(14.dp)
                                .height(1.dp)
                                .background(separatorColor)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = when (settings.clockAlignment) {
                            AppTextAlign.LEFT -> Arrangement.Start
                            AppTextAlign.CENTER -> Arrangement.Center
                            AppTextAlign.RIGHT -> Arrangement.End
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .background(PlainTheme.colors.secondaryText, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        PlainText(
                            text = "2 unread",
                            color = PlainTheme.colors.secondaryText,
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                            fontFamily = fontFamily
                        )
                    }
                    hasRenderedWidget = true

                    // Second "priority alert" widget in yellow
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .width(14.dp)
                            .height(1.dp)
                            .background(separatorColor)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = when (settings.clockAlignment) {
                            AppTextAlign.LEFT -> Arrangement.Start
                            AppTextAlign.CENTER -> Arrangement.Center
                            AppTextAlign.RIGHT -> Arrangement.End
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFFFFD600), androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        PlainText(
                            text = "1 priority alert",
                            color = androidx.compose.ui.graphics.Color(0xFFFFD600),
                            fontSize = secondarySize,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                            fontFamily = fontFamily
                        )
                    }
                }
                "now_playing" -> {
                    if (hasRenderedWidget) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .width(14.dp)
                                .height(1.dp)
                                .background(separatorColor)
                        )
                    }
                    PlainText(
                        text = "♫ Sunflower • Post Malone",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = secondarySize,
                        fontWeight = FontWeight.Normal,
                        textAlign = textAlign,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    hasRenderedWidget = true
                }
                "screen_time" -> {
                    if (hasRenderedWidget) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .width(14.dp)
                                .height(1.dp)
                                .background(separatorColor)
                        )
                    }
                    PlainText(
                        text = "2h 15m today",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = secondarySize,
                        fontWeight = FontWeight.Normal,
                        textAlign = textAlign,
                        fontFamily = fontFamily
                    )
                    hasRenderedWidget = true
                }
                "next_alarm" -> {
                    if (hasRenderedWidget) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .width(14.dp)
                                .height(1.dp)
                                .background(separatorColor)
                        )
                    }
                    PlainText(
                        text = "alarm 07:30",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = secondarySize,
                        fontWeight = FontWeight.Normal,
                        textAlign = textAlign,
                        fontFamily = fontFamily
                    )
                    hasRenderedWidget = true
                }
            }
        }
    }
}

@Composable
private fun PreviewAppList(
    settings: UserSettings,
    homeApps: List<AppItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewApps = remember(homeApps) {
        if (homeApps.isNotEmpty()) {
            homeApps.take(6)
        } else {
            listOf(
                AppItem(packageName = "com.android.phone", activityName = ".DialtactsActivity", originalLabel = "phone"),
                AppItem(packageName = "com.android.mms", activityName = ".ConversationList", originalLabel = "messages"),
                AppItem(packageName = "com.android.camera", activityName = ".CameraActivity", originalLabel = "camera"),
                AppItem(packageName = "com.android.chrome", activityName = ".Main", originalLabel = "browser"),
                AppItem(packageName = "com.plain.launcher", activityName = ".Settings", originalLabel = "settings")
            )
        }
    }

    val textAlign = settings.textAlign.toComposeAlign()
    val weight = settings.fontWeight.toComposeWeight()
    val fontFamily = getAppFontFamily(settings.selectedFont)
    val fontSize = (settings.fontSizeSp * 0.36f).coerceAtLeast(6.5f).sp
    val itemSpacing = (settings.itemSpacingDp * 0.36f).coerceAtLeast(1f).dp
    val lineHeight = (fontSize.value * settings.lineSpacingMultiplier).sp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        previewApps.forEach { app ->
            val transformed = TextTransforms.applyCase(app.displayLabel, settings.textCase)
            val iconBitmap = if (settings.showAppIcons) {
                remember(app.key, settings.coloredAppIcons) {
                    if (settings.coloredAppIcons) {
                        AppIconManager.getColoredIcon(context, app) ?: AppIconManager.getIcon(context, app)
                    } else {
                        AppIconManager.getIcon(context, app)
                    }
                }
            } else null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                contentAlignment = when (settings.textAlign) {
                    AppTextAlign.LEFT -> Alignment.CenterStart
                    AppTextAlign.CENTER -> Alignment.Center
                    AppTextAlign.RIGHT -> Alignment.CenterEnd
                }
            ) {
                val hasBadge = settings.showNotificationBadges && app.originalLabel.contains("message", ignoreCase = true)
                val previewRecent = if (hasBadge && settings.showRecentNotificationText) "Alex: Hey" else null

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = when (settings.textAlign) {
                        AppTextAlign.LEFT -> Arrangement.Start
                        AppTextAlign.CENTER -> Arrangement.Center
                        AppTextAlign.RIGHT -> Arrangement.End
                    }
                ) {
                    if (previewRecent != null) {
                        PlainText(
                            text = previewRecent,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = (fontSize.value * 0.75f).sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontFamily = fontFamily,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                    }
                    if (hasBadge) {
                        val badgeColor = Color(settings.notificationBadgeColor)
                        val badgeTextColor = if (badgeColor.luminance() > 0.42f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(badgeColor, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "1",
                                color = badgeTextColor,
                                fontSize = 3.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(6.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    PlainText(
                        text = transformed,
                        color = PlainTheme.colors.onBackground,
                        fontSize = fontSize,
                        fontWeight = weight,
                        letterSpacing = settings.letterSpacingEm.em,
                        lineHeight = lineHeight,
                        textAlign = textAlign,
                        fontFamily = fontFamily
                    )
                }
            }
        }
    }
}
