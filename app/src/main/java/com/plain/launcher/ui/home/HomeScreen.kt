package com.plain.launcher.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.AppVerticalAlign
import com.plain.launcher.data.model.GestureAction
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.ui.components.PlainAppRow
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.service.PlainNotificationListenerService
import com.plain.launcher.viewmodel.MainViewModel
import com.plain.launcher.ui.navigation.Screen
import com.plain.launcher.util.AppIconManager
import com.plain.launcher.util.HapticUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val homeApps by viewModel.homeApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val appNotifications by PlainNotificationListenerService.appNotifications.collectAsState()
    val listState = rememberLazyListState()

    var animatingSwipeApp by remember { mutableStateOf<AppItem?>(null) }
    var animatingDirection by remember { mutableStateOf(SwipeDirection.UP) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(animatingSwipeApp) {
        val app = animatingSwipeApp ?: return@LaunchedEffect
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = EaseOutCubic)
        )
        viewModel.launchApp(context, app)
        delay(120)
        animatingSwipeApp = null
        animProgress.snapTo(0f)
    }

    val handleSwipe: (GestureAction, String?, SwipeDirection) -> Unit = { action, customPkg, direction ->
        if (action == GestureAction.OPEN_APP && !customPkg.isNullOrBlank()) {
            val targetApp = allApps.firstOrNull { it.packageName == customPkg }
            if (targetApp != null) {
                HapticUtil.performGestureFeedback(context, settings.enableHapticFeedback)
                animatingDirection = direction
                animatingSwipeApp = targetApp
            } else {
                viewModel.performGestureAction(context, action, customPkg)
            }
        } else {
            viewModel.performGestureAction(context, action, customPkg)
        }
    }

    // Scroll to top when home is pressed
    LaunchedEffect(Unit) {
        viewModel.homeScrollToTop.collectLatest {
            listState.animateScrollToItem(0)
        }
    }

    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 24.dp.toPx() }
    val statusBarExclusionPx = with(density) { 60.dp.toPx() }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }

    val verticalArrangement = when (settings.verticalAlign) {
        AppVerticalAlign.TOP -> Arrangement.Top
        AppVerticalAlign.CENTER -> Arrangement.Center
        AppVerticalAlign.BOTTOM -> Arrangement.Bottom
    }

    val tapModifier = if (settings.gestureDoubleTap != GestureAction.NONE) {
        Modifier.pointerInput(settings.gestureDoubleTap) {
            detectTapGestures(
                onDoubleTap = {
                    viewModel.performGestureAction(
                        context = context,
                        action = settings.gestureDoubleTap,
                        customPkg = settings.gestureCustomApps["double_tap"]
                    )
                }
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background)
            .then(tapModifier)
            .pointerInput(settings, allApps) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        totalDragX = 0f
                        totalDragY = 0f
                        dragStartY = startOffset.y
                    },
                    onDragEnd = {
                        val dx = totalDragX
                        val dy = totalDragY
                        if (abs(dy) > abs(dx) && abs(dy) > dragThresholdPx) {
                            if (dy < 0) {
                                // Swipe Up
                                handleSwipe(
                                    settings.gestureSwipeUp,
                                    settings.gestureCustomApps["swipe_up"],
                                    SwipeDirection.UP
                                )
                            } else {
                                // Swipe Down: only trigger if gesture started below status bar zone
                                if (dragStartY >= statusBarExclusionPx) {
                                    handleSwipe(
                                        settings.gestureSwipeDown,
                                        settings.gestureCustomApps["swipe_down"],
                                        SwipeDirection.DOWN
                                    )
                                }
                            }
                        } else if (abs(dx) > abs(dy) && abs(dx) > dragThresholdPx) {
                            if (dx < 0) {
                                // Swipe Left
                                handleSwipe(
                                    settings.gestureSwipeLeft,
                                    settings.gestureCustomApps["swipe_left"],
                                    SwipeDirection.LEFT
                                )
                            } else {
                                // Swipe Right
                                handleSwipe(
                                    settings.gestureSwipeRight,
                                    settings.gestureCustomApps["swipe_right"],
                                    SwipeDirection.RIGHT
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        val dx = totalDragX
                        val dy = totalDragY
                        if (abs(dy) > abs(dx) && abs(dy) > dragThresholdPx) {
                            if (dy < 0) {
                                handleSwipe(
                                    settings.gestureSwipeUp,
                                    settings.gestureCustomApps["swipe_up"],
                                    SwipeDirection.UP
                                )
                            } else if (dragStartY >= statusBarExclusionPx) {
                                handleSwipe(
                                    settings.gestureSwipeDown,
                                    settings.gestureCustomApps["swipe_down"],
                                    SwipeDirection.DOWN
                                )
                            }
                        } else if (abs(dx) > abs(dy) && abs(dx) > dragThresholdPx) {
                            if (dx < 0) {
                                handleSwipe(
                                    settings.gestureSwipeLeft,
                                    settings.gestureCustomApps["swipe_left"],
                                    SwipeDirection.LEFT
                                )
                            } else {
                                handleSwipe(
                                    settings.gestureSwipeRight,
                                    settings.gestureCustomApps["swipe_right"],
                                    SwipeDirection.RIGHT
                                )
                            }
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (settings.showStatusBar) Modifier.statusBarsPadding() else Modifier),
            verticalArrangement = verticalArrangement
        ) {
            // Optional Clock and Date header (holding opens settings)
            ClockDateHeader(
                settings = settings,
                onLongClick = { viewModel.navigateTo(Screen.Settings) },
                onScreenTimeClick = { viewModel.openSettingsCategory("SCREEN_TIME_LIMITS") }
            )

            if (homeApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    PlainText(
                        text = "no apps found",
                        color = PlainTheme.colors.secondaryText
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(
                        items = homeApps,
                        key = { it.key }
                    ) { app ->
                        val notifData = appNotifications[app.packageName]
                        val badgeCount = if (settings.showNotificationBadges) (notifData?.count ?: 0) else 0
                        val recentText = if (settings.showRecentNotificationText && badgeCount > 0) notifData?.recentText else null

                        PlainAppRow(
                            label = app.displayLabel,
                            settings = settings,
                            app = app,
                            badgeCount = badgeCount,
                            recentNotificationText = recentText,
                            onClick = {
                                viewModel.launchApp(context, app)
                            },
                            onLongClick = {
                                viewModel.openAppMenu(app)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Quick slide-in animation for swipe-to-open app gesture
        if (animatingSwipeApp != null) {
            val app = animatingSwipeApp!!
            val iconBitmap = remember(app.key) {
                AppIconManager.getColoredIcon(context, app) ?: AppIconManager.getIcon(context, app)
            }
            val initialDistance = with(density) { 220.dp.toPx() }
            val offsetX = when (animatingDirection) {
                SwipeDirection.LEFT -> with(density) { (initialDistance * (1f - animProgress.value)).toDp() }
                SwipeDirection.RIGHT -> with(density) { (-initialDistance * (1f - animProgress.value)).toDp() }
                else -> 0.dp
            }
            val offsetY = when (animatingDirection) {
                SwipeDirection.UP -> with(density) { (initialDistance * (1f - animProgress.value)).toDp() }
                SwipeDirection.DOWN -> with(density) { (-initialDistance * (1f - animProgress.value)).toDp() }
                else -> 0.dp
            }
            val iconScale = 0.75f + 0.25f * animProgress.value
            val iconAlpha = (animProgress.value * 2.5f).coerceAtMost(1f)

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.displayLabel,
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .scale(iconScale)
                            .alpha(iconAlpha)
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
            }
        }
    }
}
