package com.plain.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.AppFontWeight
import com.plain.launcher.data.model.AppTextAlign
import com.plain.launcher.data.model.AppTextCase
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.ui.theme.GeistMonoFamily
import com.plain.launcher.ui.theme.getAppFontFamily
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.util.AppIconManager
import com.plain.launcher.util.HapticUtil
import com.plain.launcher.util.TextTransforms

fun AppFontWeight.toComposeWeight(): FontWeight {
    return when (this) {
        AppFontWeight.REGULAR -> FontWeight.Normal
        AppFontWeight.MEDIUM -> FontWeight.Medium
        AppFontWeight.BOLD -> FontWeight.Bold
    }
}

fun AppTextAlign.toComposeAlign(): TextAlign {
    return when (this) {
        AppTextAlign.LEFT -> TextAlign.Start
        AppTextAlign.CENTER -> TextAlign.Center
        AppTextAlign.RIGHT -> TextAlign.End
    }
}

fun AppTextAlign.toAlignment(): Alignment.Horizontal {
    return when (this) {
        AppTextAlign.LEFT -> Alignment.Start
        AppTextAlign.CENTER -> Alignment.CenterHorizontally
        AppTextAlign.RIGHT -> Alignment.End
    }
}

@Composable
fun PlainText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PlainTheme.colors.onBackground,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.em,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: FontFamily = getAppFontFamily(PlainTheme.settings.selectedFont),
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            color = color,
            textAlign = textAlign
        )
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlainAppRow(
    label: String,
    settings: UserSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    app: AppItem? = null,
    badgeCount: Int = 0,
    recentNotificationText: String? = null
) {
    val context = LocalContext.current
    val transformed = TextTransforms.applyCase(label, settings.textCase)
    val textAlign = settings.textAlign.toComposeAlign()
    val weight = settings.fontWeight.toComposeWeight()

    val iconBitmap = if (settings.showAppIcons && app != null) {
        remember(app.key, settings.coloredAppIcons) {
            if (settings.coloredAppIcons) {
                AppIconManager.getColoredIcon(context, app) ?: AppIconManager.getIcon(context, app)
            } else {
                AppIconManager.getIcon(context, app)
            }
        }
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    HapticUtil.performAppLongPress(context, settings.enableHapticFeedback)
                    onLongClick()
                }
            )
            .padding(vertical = (settings.itemSpacingDp / 2).dp, horizontal = 24.dp),
        contentAlignment = when (settings.textAlign) {
            AppTextAlign.LEFT -> Alignment.CenterStart
            AppTextAlign.CENTER -> Alignment.Center
            AppTextAlign.RIGHT -> Alignment.CenterEnd
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = when (settings.textAlign) {
                AppTextAlign.LEFT -> Arrangement.Start
                AppTextAlign.CENTER -> Arrangement.Center
                AppTextAlign.RIGHT -> Arrangement.End
            }
        ) {
            // Recent notification text to the left
            if (!recentNotificationText.isNullOrBlank()) {
                PlainText(
                    text = recentNotificationText,
                    color = PlainTheme.colors.secondaryText,
                    fontSize = (settings.fontSizeSp * 0.75f).coerceAtLeast(11f).sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Notification badge: circle same size as icon (16.dp) with the number inside
            if (badgeCount > 0) {
                val badgeColor = Color(settings.notificationBadgeColor)
                val badgeTextColor = if (badgeColor.luminance() > 0.42f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PlainText(
                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                        color = badgeTextColor,
                        fontSize = if (badgeCount > 9) 7.5.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            PlainText(
                text = transformed,
                color = PlainTheme.colors.onBackground,
                fontSize = settings.fontSizeSp.sp,
                fontWeight = weight,
                letterSpacing = settings.letterSpacingEm.em,
                lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
                textAlign = textAlign
            )
        }
    }
}

@Composable
fun PlainTextRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    titleFontSize: TextUnit = 15.sp,
    titleFontWeight: FontWeight = FontWeight.Medium,
    titleColor: Color = PlainTheme.colors.onBackground
) {
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                PlainText(
                    text = title,
                    color = titleColor,
                    fontSize = titleFontSize,
                    fontWeight = titleFontWeight
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    PlainText(
                        text = subtitle,
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!valueText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(16.dp))
                PlainText(
                    text = valueText,
                    color = PlainTheme.colors.secondaryText,
                    fontSize = 14.sp
                )
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(8.dp))
                PlainText(
                    text = ">",
                    color = PlainTheme.colors.secondaryText,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PlainSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback,
    enableAnimations: Boolean = PlainTheme.settings.enableAnimations
) {
    val context = LocalContext.current
    val trackWidth = 44.dp
    val trackHeight = 32.dp
    val thumbSize = 26.dp
    val thumbPadding = 3.dp
    val animDuration = if (enableAnimations) 160 else 0

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding,
        animationSpec = tween(durationMillis = animDuration),
        label = "switchThumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) PlainTheme.colors.onBackground else PlainTheme.colors.surface,
        animationSpec = tween(durationMillis = animDuration),
        label = "switchTrackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) PlainTheme.colors.background else PlainTheme.colors.secondaryText,
        animationSpec = tween(durationMillis = animDuration),
        label = "switchThumbColor"
    )

    val clickableModifier = if (onCheckedChange != null) {
        modifier.clickable(
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                HapticUtil.performClick(context, enableHaptic)
                onCheckedChange(!checked)
            }
        )
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(thumbSize)
                .clip(RoundedCornerShape(percent = 50))
                .background(thumbColor)
        )
    }
}

@Composable
fun PlainListDivider(
    modifier: Modifier = Modifier,
    fraction: Float = 0.88f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(1.dp)
                .background(PlainTheme.colors.onBackground.copy(alpha = 0.08f))
        )
    }
}

@Composable
fun PlainSectionHeader(
    @Suppress("UNUSED_PARAMETER") title: String = "",
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") fraction: Float = 0.88f,
    isFirst: Boolean = false
) {
    val height = if (isFirst) 20.dp else 34.dp
    Spacer(modifier = modifier.height(height))
}

@Composable
fun PlainSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = PlainTheme.colors.background.luminance() < 0.5f
    val cardBg = if (isDark) {
        PlainTheme.colors.onBackground.copy(alpha = 0.08f)
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun PlainTextToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback,
    enableAnimations: Boolean = PlainTheme.settings.enableAnimations
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticUtil.performClick(context, enableHaptic)
                    onCheckedChange(!checked)
                }
            )
            .semantics {
                role = Role.Switch
                contentDescription = "$title: ${if (checked) "enabled" else "disabled"}"
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PlainText(
                text = title,
                color = PlainTheme.colors.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                PlainText(
                    text = subtitle,
                    color = PlainTheme.colors.secondaryText,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        PlainSwitch(
            checked = checked,
            onCheckedChange = null,
            enableHaptic = enableHaptic,
            enableAnimations = enableAnimations
        )
    }
}

@Composable
fun PlainFlatSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    valueDisplay: String = String.format("%.1f", value),
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback
) {
    val context = LocalContext.current
    val lastTickVal = remember { mutableFloatStateOf(value) }
    val step = (valueRange.endInclusive - valueRange.start) / 25f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlainText(
                text = title,
                color = PlainTheme.colors.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            PlainText(
                text = valueDisplay,
                color = PlainTheme.colors.secondaryText,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val onBackgroundColor = PlainTheme.colors.onBackground
        val inactiveTrackColor = PlainTheme.colors.onBackground.copy(alpha = 0.20f)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val rangeSpan = valueRange.endInclusive - valueRange.start
            val fraction = if (rangeSpan > 0f) {
                ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
            } else 0f

            val density = LocalDensity.current

            fun updateFromPx(x: Float) {
                if (totalWidthPx > 0f) {
                    val thumbWidthPx = with(density) { 8.dp.toPx() }
                    val thumbTravel = (totalWidthPx - thumbWidthPx).coerceAtLeast(1f)
                    val newFrac = ((x - thumbWidthPx / 2f) / thumbTravel).coerceIn(0f, 1f)
                    val newVal = valueRange.start + newFrac * rangeSpan
                    if (kotlin.math.abs(newVal - lastTickVal.floatValue) >= step) {
                        HapticUtil.performTick(context, enableHaptic)
                        lastTickVal.floatValue = newVal
                    }
                    onValueChange(newVal)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .pointerInput(valueRange, totalWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            updateFromPx(down.position.x)
                            drag(down.id) { change ->
                                change.consume()
                                updateFromPx(change.position.x)
                            }
                        }
                    }
            ) {
                val totalWidth = size.width
                val centerY = size.height / 2f
                val trackHeight = 10.dp.toPx()
                val thumbWidth = 8.dp.toPx()
                val thumbHeight = 30.dp.toPx()
                val gap = 5.dp.toPx()

                val thumbTravel = (totalWidth - thumbWidth).coerceAtLeast(0f)
                val thumbLeft = (fraction * thumbTravel).coerceIn(0f, thumbTravel)
                val thumbRight = thumbLeft + thumbWidth

                // Active track (left segment)
                val activeTrackEnd = (thumbLeft - gap).coerceAtLeast(0f)
                if (activeTrackEnd > 0f) {
                    val activeRadius = (trackHeight / 2f).coerceAtMost(activeTrackEnd / 2f)
                    drawRoundRect(
                        color = onBackgroundColor,
                        topLeft = Offset(0f, centerY - trackHeight / 2f),
                        size = Size(activeTrackEnd, trackHeight),
                        cornerRadius = CornerRadius(activeRadius, activeRadius)
                    )
                }

                // Inactive track (right segment)
                val inactiveTrackStart = (thumbRight + gap).coerceAtMost(totalWidth)
                val inactiveWidth = (totalWidth - inactiveTrackStart).coerceAtLeast(0f)
                if (inactiveWidth > 0f) {
                    val inactiveRadius = (trackHeight / 2f).coerceAtMost(inactiveWidth / 2f)
                    drawRoundRect(
                        color = inactiveTrackColor,
                        topLeft = Offset(inactiveTrackStart, centerY - trackHeight / 2f),
                        size = Size(inactiveWidth, trackHeight),
                        cornerRadius = CornerRadius(inactiveRadius, inactiveRadius)
                    )
                }

                // Thumb: vertical rectangle with circular / rounded corners
                val thumbRadius = thumbWidth / 2f
                drawRoundRect(
                    color = onBackgroundColor,
                    topLeft = Offset(thumbLeft, centerY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius)
                )
            }
        }
    }
}

@Composable
fun <T> PlainTextOptionSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback,
    enableAnimations: Boolean = PlainTheme.settings.enableAnimations,
    fontFamilyProvider: ((T) -> FontFamily)? = null,
    labelProvider: (T) -> String = { it.toString().lowercase() }
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        PlainText(
            text = title,
            color = PlainTheme.colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val label = labelProvider(option)

                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 6.dp else 24.dp,
                    animationSpec = tween(durationMillis = if (enableAnimations) 180 else 0),
                    label = "optionCornerRadius"
                )

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PlainTheme.colors.onBackground else PlainTheme.colors.surface,
                    animationSpec = tween(durationMillis = if (enableAnimations) 160 else 0),
                    label = "optionBg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PlainTheme.colors.background else PlainTheme.colors.secondaryText,
                    animationSpec = tween(durationMillis = if (enableAnimations) 160 else 0),
                    label = "optionText"
                )

                val optionFontFamily = fontFamilyProvider?.invoke(option) ?: getAppFontFamily(PlainTheme.settings.selectedFont)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    HapticUtil.performClick(context, enableHaptic)
                                }
                                onOptionSelected(option)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlainText(
                        text = label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = optionFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun <T> PlainHorizontalOptionSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback,
    enableAnimations: Boolean = PlainTheme.settings.enableAnimations,
    labelProvider: (T) -> String = { it.toString().lowercase() }
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        PlainText(
            text = title,
            color = PlainTheme.colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val label = labelProvider(option)

                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 6.dp else 24.dp,
                    animationSpec = tween(durationMillis = if (enableAnimations) 180 else 0),
                    label = "horizontalOptionCornerRadius"
                )

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PlainTheme.colors.onBackground else PlainTheme.colors.surface,
                    animationSpec = tween(durationMillis = if (enableAnimations) 160 else 0),
                    label = "horizontalOptionBg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PlainTheme.colors.background else PlainTheme.colors.secondaryText,
                    animationSpec = tween(durationMillis = if (enableAnimations) 160 else 0),
                    label = "horizontalOptionText"
                )

                val contentAlign = when (option) {
                    AppTextAlign.LEFT -> Alignment.CenterStart
                    AppTextAlign.CENTER -> Alignment.Center
                    AppTextAlign.RIGHT -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    HapticUtil.performClick(context, enableHaptic)
                                }
                                onOptionSelected(option)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    contentAlignment = contentAlign
                ) {
                    PlainText(
                        text = label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun <T> PlainScrollableHorizontalOptionSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback,
    enableAnimations: Boolean = PlainTheme.settings.enableAnimations,
    fontFamilyProvider: ((T) -> FontFamily)? = null,
    labelProvider: (T) -> String = { it.toString().lowercase() }
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        PlainText(
            text = title,
            color = PlainTheme.colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val label = labelProvider(option)

                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 6.dp else 24.dp,
                    animationSpec = tween(durationMillis = if (enableAnimations) 180 else 0),
                    label = "scrollableOptionCornerRadius"
                )

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PlainTheme.colors.onBackground else PlainTheme.colors.surface,
                    animationSpec = tween(durationMillis = if (enableAnimations) 160 else 0),
                    label = "scrollableOptionBg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) PlainTheme.colors.background else PlainTheme.colors.secondaryText,
                    animationSpec = tween(durationMillis = if (enableAnimations) 160 else 0),
                    label = "scrollableOptionText"
                )

                val optionFontFamily = fontFamilyProvider?.invoke(option) ?: getAppFontFamily(PlainTheme.settings.selectedFont)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    HapticUtil.performClick(context, enableHaptic)
                                }
                                onOptionSelected(option)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlainText(
                        text = label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = optionFontFamily,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PlainColorOptionSelector(
    title: String,
    colors: List<Long>,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enableHaptic: Boolean = PlainTheme.settings.enableHapticFeedback,
    enableAnimations: Boolean = PlainTheme.settings.enableAnimations
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        PlainText(
            text = title,
            color = PlainTheme.colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { colorValue ->
                val isSelected = colorValue == selectedColor

                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 6.dp else 16.dp,
                    animationSpec = tween(durationMillis = if (enableAnimations) 180 else 0),
                    label = "badgeColorCorner"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    HapticUtil.performClick(context, enableHaptic)
                                }
                                onColorSelected(colorValue)
                            }
                        )
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(Color(colorValue))
                    )
                }
            }
        }
    }
}

@Composable
fun PlainSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "search...",
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(PlainTheme.colors.surface)
            .padding(horizontal = 20.dp),
        textStyle = TextStyle(
            fontFamily = getAppFontFamily(PlainTheme.settings.selectedFont),
            fontSize = 16.sp,
            color = PlainTheme.colors.onBackground
        ),
        cursorBrush = SolidColor(PlainTheme.colors.onBackground),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        PlainText(
                            text = hint,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onQueryChange("") }
                            )
                            .padding(start = 8.dp)
                    ) {
                        PlainText(
                            text = "[x]",
                            color = PlainTheme.colors.secondaryText,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun MagnifyingGlassIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        val radius = 5.2.dp.toPx()
        val centerX = 7.dp.toPx()
        val centerY = 7.dp.toPx()

        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth)
        )

        val cos45 = 0.7071f
        val startX = centerX + radius * cos45
        val startY = centerY + radius * cos45
        val endX = 15.5.dp.toPx()
        val endY = 15.5.dp.toPx()

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
