package com.plain.launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Message
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.plain.launcher.util.PriorityAlertManager
import com.plain.launcher.ui.alerts.EdgeGlowOverlay
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.IntOffset
import com.plain.launcher.data.model.GestureAction
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.AppOrientation
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.drawer.AppDrawerScreen
import com.plain.launcher.ui.notespace.NotespaceScreen
import com.plain.launcher.ui.home.AppActionMenu
import com.plain.launcher.ui.home.HomeScreen
import com.plain.launcher.ui.home.RenameAppDialog
import com.plain.launcher.ui.navigation.Screen
import com.plain.launcher.ui.settings.GestureSettingsScreen
import com.plain.launcher.ui.settings.HiddenAppsScreen
import com.plain.launcher.ui.settings.ReorderFavoritesScreen
import com.plain.launcher.ui.settings.SelectGestureAppScreen
import com.plain.launcher.ui.settings.SettingsScreen
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.util.WallpaperUtil
import com.plain.launcher.viewmodel.MainViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as PlainApplication
        MainViewModel.Factory(app.appRepository, app.weatherRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        enableEdgeToEdge()
        handleGestureNavContract(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val selectedAppForMenu by viewModel.selectedAppForMenu.collectAsState()
            val renameApp by viewModel.renameApp.collectAsState()
            val toastMessage by viewModel.toastMessage.collectAsState()

            // Update system bars visibility
            LaunchedEffect(settings.showStatusBar, settings.showNavBar) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (settings.showStatusBar) {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                }

                if (settings.showNavBar) {
                    windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }

            // Update screen orientation lock
            LaunchedEffect(settings.screenOrientation) {
                requestedOrientation = when (settings.screenOrientation) {
                    AppOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    AppOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    AppOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }

            // Dismiss toast after duration
            LaunchedEffect(toastMessage) {
                if (toastMessage != null) {
                    delay(3000)
                    viewModel.clearToast()
                }
            }

            // Back button handling - on home screen, never finish launcher activity
            BackHandler(enabled = true) {
                if (currentScreen != Screen.Home) {
                    viewModel.navigateBack()
                } else {
                    viewModel.onHomePressed()
                }
            }

            PlainTheme(settings = settings) {
                val currentBg = PlainTheme.colors.background
                LaunchedEffect(currentBg) {
                    window.setBackgroundDrawable(ColorDrawable(currentBg.toArgb()))
                    val isLightBg = currentBg.luminance() > 0.5f
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    windowInsetsController.isAppearanceLightStatusBars = isLightBg
                    windowInsetsController.isAppearanceLightNavigationBars = isLightBg
                }

                LaunchedEffect(settings.blackSystemWallpaper, currentBg) {
                    if (settings.blackSystemWallpaper) {
                        WallpaperUtil.setSolidWallpaper(this@MainActivity, currentBg.toArgb(), homeOnly = true)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PlainTheme.colors.background)
                ) {
                    // Screen content with directional animated transitions for search
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (!settings.enableAnimations) {
                                EnterTransition.None.togetherWith(ExitTransition.None)
                            } else if (targetState is Screen.Drawer || targetState is Screen.Notespace) {
                                val springSpec = spring<IntOffset>(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                                val fadeSpec = tween<Float>(durationMillis = 220, easing = LinearOutSlowInEasing)

                                val slideIn = when {
                                    (targetState is Screen.Notespace && settings.gestureSwipeDown == GestureAction.OPEN_NOTESPACE) ||
                                    (targetState is Screen.Drawer && (settings.gestureSwipeDown == GestureAction.OPEN_SEARCH || settings.gestureSwipeDown == GestureAction.OPEN_DRAWER)) ->
                                        slideInVertically(animationSpec = springSpec, initialOffsetY = { -it })
                                    (targetState is Screen.Notespace && settings.gestureSwipeLeft == GestureAction.OPEN_NOTESPACE) ||
                                    (targetState is Screen.Drawer && (settings.gestureSwipeLeft == GestureAction.OPEN_SEARCH || settings.gestureSwipeLeft == GestureAction.OPEN_DRAWER)) ->
                                        slideInHorizontally(animationSpec = springSpec, initialOffsetX = { it })
                                    (targetState is Screen.Notespace && settings.gestureSwipeRight == GestureAction.OPEN_NOTESPACE) ||
                                    (targetState is Screen.Drawer && (settings.gestureSwipeRight == GestureAction.OPEN_SEARCH || settings.gestureSwipeRight == GestureAction.OPEN_DRAWER)) ->
                                        slideInHorizontally(animationSpec = springSpec, initialOffsetX = { -it })
                                    else ->
                                        slideInVertically(animationSpec = springSpec, initialOffsetY = { it })
                                }

                                (slideIn + fadeIn(animationSpec = fadeSpec))
                                    .togetherWith(fadeOut(animationSpec = tween(150)))
                            } else if (initialState is Screen.Drawer || initialState is Screen.Notespace) {
                                val slideOut = when {
                                    (initialState is Screen.Notespace && settings.gestureSwipeDown == GestureAction.OPEN_NOTESPACE) ||
                                    (initialState is Screen.Drawer && (settings.gestureSwipeDown == GestureAction.OPEN_SEARCH || settings.gestureSwipeDown == GestureAction.OPEN_DRAWER)) ->
                                        slideOutVertically(animationSpec = tween(200), targetOffsetY = { -it })
                                    (initialState is Screen.Notespace && settings.gestureSwipeLeft == GestureAction.OPEN_NOTESPACE) ||
                                    (initialState is Screen.Drawer && (settings.gestureSwipeLeft == GestureAction.OPEN_SEARCH || settings.gestureSwipeLeft == GestureAction.OPEN_DRAWER)) ->
                                        slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { it })
                                    (initialState is Screen.Notespace && settings.gestureSwipeRight == GestureAction.OPEN_NOTESPACE) ||
                                    (initialState is Screen.Drawer && (settings.gestureSwipeRight == GestureAction.OPEN_SEARCH || settings.gestureSwipeRight == GestureAction.OPEN_DRAWER)) ->
                                        slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { -it })
                                    else ->
                                        slideOutVertically(animationSpec = tween(200), targetOffsetY = { it })
                                }
                                fadeIn(animationSpec = tween(150))
                                    .togetherWith(slideOut + fadeOut(animationSpec = tween(180)))
                            } else {
                                fadeIn(animationSpec = tween(180))
                                    .togetherWith(fadeOut(animationSpec = tween(180)))
                            }
                        },
                        label = "screenTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            is Screen.Home -> HomeScreen(viewModel = viewModel)
                            is Screen.Drawer -> AppDrawerScreen(viewModel = viewModel)
                            is Screen.Notespace -> NotespaceScreen(viewModel = viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                            is Screen.HiddenApps -> HiddenAppsScreen(viewModel = viewModel)
                            is Screen.ReorderFavorites -> ReorderFavoritesScreen(viewModel = viewModel)
                            is Screen.GestureSettings -> GestureSettingsScreen(viewModel = viewModel)
                            is Screen.SelectGestureApp -> SelectGestureAppScreen(
                                gestureKey = screen.gestureKey,
                                viewModel = viewModel
                            )
                        }
                    }

                    // App Action Menu Overlay
                    var menuAppToDisplay by remember { mutableStateOf<AppItem?>(null) }
                    if (selectedAppForMenu != null) {
                        menuAppToDisplay = selectedAppForMenu
                    }

                    AnimatedVisibility(
                        visible = selectedAppForMenu != null,
                        enter = fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(150))
                    ) {
                        menuAppToDisplay?.let { app ->
                            AppActionMenu(
                                app = app,
                                context = this@MainActivity,
                                onPinToggle = { viewModel.togglePin(app) },
                                onRename = { viewModel.openRenameDialog(app) },
                                onDismiss = { viewModel.closeAppMenu() }
                            )
                        }
                    }

                    // Rename App Dialog Overlay
                    renameApp?.let { app ->
                        RenameAppDialog(
                            app = app,
                            onSave = { newName -> viewModel.saveRename(app, newName) },
                            onDismiss = { viewModel.closeRenameDialog() }
                        )
                    }

                    // Toast message banner
                    AnimatedVisibility(
                        visible = toastMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                    ) {
                        toastMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PlainTheme.colors.onBackground)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { viewModel.clearToast() }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                PlainText(
                                    text = msg,
                                    color = PlainTheme.colors.background,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Edge Glow Overlay for Priority Alerts
                    val isEdgeGlowActive by PriorityAlertManager.isEdgeGlowActive.collectAsState()
                    val activeGlowColor by PriorityAlertManager.activeGlowColor.collectAsState()
                    if (isEdgeGlowActive) {
                        EdgeGlowOverlay(
                            glowColor = Color(activeGlowColor),
                            cornerRadiusDp = settings.edgeGlowCornerRadiusDp
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PriorityAlertManager.isMainActivityResumed = true
    }

    override fun onPause() {
        super.onPause()
        PriorityAlertManager.isMainActivityResumed = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGestureNavContract(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomePressed()
        }
    }

    private fun handleGestureNavContract(intent: Intent?) {
        if (intent == null) return
        val contractBundle = intent.getBundleExtra("gesture_nav_contract_v1") ?: return
        intent.removeExtra("gesture_nav_contract_v1")

        val callbackMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contractBundle.getParcelable("android.intent.extra.REMOTE_CALLBACK", Message::class.java)
        } else {
            @Suppress("DEPRECATION")
            contractBundle.getParcelable("android.intent.extra.REMOTE_CALLBACK") as? Message
        } ?: return

        val replyTo = callbackMsg.replyTo ?: return

        try {
            val reply = Message.obtain().apply {
                copyFrom(callbackMsg)
                data = Bundle().apply {
                    val dm = resources.displayMetrics
                    val centerX = dm.widthPixels / 2f
                    val centerY = dm.heightPixels / 2f
                    val targetRect = RectF(centerX - 120f, centerY - 120f, centerX + 120f, centerY + 120f)
                    putParcelable("gesture_nav_contract_icon_position", targetRect)
                }
            }
            replyTo.send(reply)
        } catch (e: Exception) {
            // ignore
        }
    }
}
