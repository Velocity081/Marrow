package com.plain.launcher.ui.settings

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.plain.launcher.data.model.PriorityAlertRule
import com.plain.launcher.util.PriorityAlertStore
import java.util.UUID
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.plain.launcher.data.model.AppFontWeight
import com.plain.launcher.data.model.AppOrientation
import com.plain.launcher.data.model.AppTextAlign
import com.plain.launcher.data.model.AppTextCase
import com.plain.launcher.data.model.AppVerticalAlign
import com.plain.launcher.data.model.DrawerSortOrder
import com.plain.launcher.data.model.ThemeMode
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.ui.components.HomeScreenPreview
import com.plain.launcher.ui.components.PlainFlatSlider
import com.plain.launcher.ui.components.PlainHorizontalOptionSelector
import com.plain.launcher.ui.components.PlainColorOptionSelector
import com.plain.launcher.ui.components.PlainListDivider
import com.plain.launcher.ui.components.PlainSectionHeader
import com.plain.launcher.ui.components.PlainSettingsCard
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.components.PlainTextOptionSelector
import com.plain.launcher.ui.components.PlainTextRow
import com.plain.launcher.ui.components.PlainTextToggle
import com.plain.launcher.ui.navigation.Screen
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import com.plain.launcher.R
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape
import com.plain.launcher.data.model.ClockFont
import com.plain.launcher.data.model.LauncherFont
import com.plain.launcher.service.PlainNotificationListenerService
import com.plain.launcher.ui.components.PlainScrollableHorizontalOptionSelector
import com.plain.launcher.ui.theme.GeistMonoFamily
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.ui.theme.getAppFontFamily
import com.plain.launcher.ui.theme.getClockFontFamily
import com.plain.launcher.util.AppIconManager
import com.plain.launcher.util.AppLauncher
import com.plain.launcher.util.DeviceInfoUtil
import com.plain.launcher.util.ScreenTimeUtil
import com.plain.launcher.util.StorageUtil
import com.plain.launcher.util.WallpaperUtil
import com.plain.launcher.util.WidgetDataProvider
import com.plain.launcher.viewmodel.MainViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch

enum class SettingsCategory(val title: String) {
    APPEARANCE("launcher appearance"),
    FAVORITES_DRAWER("favorites & app drawer"),
    SCREEN_TIME_LIMITS("screen time and limits"),
    NOTIFICATIONS_ALERTS("notifications and alerts"),
    DEVICE_STORAGE("device storage"),
    SYSTEM("system & display"),
    ACCESSIBILITY("accessibility"),
    BACKUP("backup & restore"),
    ABOUT("about"),
    MARROW_TOOLS("marrow tools")
}

sealed class SettingsNavLevel(val depth: Int) {
    object Overview : SettingsNavLevel(0)
    data class CategoryDetail(val category: SettingsCategory) : SettingsNavLevel(1)
    object ClockAndWidgets : SettingsNavLevel(2)
    object PriorityAlerts : SettingsNavLevel(2)
}

private data class WidgetItem(
    val id: String,
    val title: String
)

private val allAvailableWidgets = listOf(
    WidgetItem("weather", "weather"),
    WidgetItem("battery", "battery"),
    WidgetItem("notifications", "unread notifications"),
    WidgetItem("now_playing", "now playing"),
    WidgetItem("screen_time", "screen time"),
    WidgetItem("next_alarm", "next alarm")
)


@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val homeApps by viewModel.homeApps.collectAsState()
    val isDeviceAdminActive by viewModel.isDeviceAdminActive.collectAsState()
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()
    val isWeatherLoading by viewModel.isWeatherLoading.collectAsState()
    val weatherFetchStatus by viewModel.weatherFetchStatus.collectAsState()
    val targetCategoryName by viewModel.targetSettingsCategory.collectAsState()

    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var inClockSubPage by remember { mutableStateOf(false) }
    var inPriorityAlertsSubPage by remember { mutableStateOf(false) }
    val priorityRules by PriorityAlertStore.rulesFlow.collectAsState()
    val allApps by viewModel.allApps.collectAsState()

    var showRuleWindow by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<PriorityAlertRule?>(null) }

    var showResetDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentBgColor = PlainTheme.colors.background

    LaunchedEffect(targetCategoryName) {
        if (targetCategoryName != null) {
            val matching = SettingsCategory.values().find { it.name == targetCategoryName }
            if (matching != null) {
                inClockSubPage = false
                inPriorityAlertsSubPage = false
                selectedCategory = matching
            }
            viewModel.clearTargetSettingsCategory()
        }
    }

    // Intercept back button when inside a category sub-page, clock sub-page, or priority alerts sub-page
    BackHandler(enabled = inClockSubPage || inPriorityAlertsSubPage || selectedCategory != null) {
        if (inClockSubPage) {
            inClockSubPage = false
        } else if (inPriorityAlertsSubPage) {
            inPriorityAlertsSubPage = false
        } else {
            selectedCategory = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSystemStatus(context)
        PriorityAlertStore.init(context)
    }

    // SAF ActivityResultLaunchers for Backup and Restore
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(context, it) }
    }

    // Device Admin launcher
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatus(context)
    }

    // Role Manager launcher
    val defaultLauncherLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatus(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (inClockSubPage) {
                                    inClockSubPage = false
                                } else if (inPriorityAlertsSubPage) {
                                    inPriorityAlertsSubPage = false
                                } else if (selectedCategory != null) {
                                    selectedCategory = null
                                } else {
                                    viewModel.navigateBack()
                                }
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    AnimatedContent(
                        targetState = when {
                            inClockSubPage -> "< appearance"
                            inPriorityAlertsSubPage -> "< notifications"
                            selectedCategory != null -> "< settings"
                            else -> "< back"
                        },
                        transitionSpec = {
                            fadeIn(tween(200, easing = EaseOutCubic)) togetherWith fadeOut(tween(160, easing = EaseOutCubic))
                        },
                        label = "SettingsBackAnim"
                    ) { backText ->
                        PlainText(
                            text = backText,
                            color = PlainTheme.colors.secondaryText,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                AnimatedContent(
                    targetState = when {
                        inClockSubPage -> "clock face and widgets"
                        inPriorityAlertsSubPage -> "priority alerts"
                        selectedCategory != null -> selectedCategory!!.title
                        else -> "settings"
                    },
                    transitionSpec = {
                        fadeIn(tween(200, easing = EaseOutCubic)) togetherWith fadeOut(tween(160, easing = EaseOutCubic))
                    },
                    label = "SettingsTitleAnim"
                ) { titleText ->
                    PlainText(
                        text = titleText,
                        color = if (selectedCategory == SettingsCategory.MARROW_TOOLS && !inClockSubPage && !inPriorityAlertsSubPage) Color(0xFFFFD600) else PlainTheme.colors.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Main categories tab list or sub-page
            val currentNavLevel: SettingsNavLevel = when {
                inClockSubPage -> SettingsNavLevel.ClockAndWidgets
                inPriorityAlertsSubPage -> SettingsNavLevel.PriorityAlerts
                selectedCategory != null -> SettingsNavLevel.CategoryDetail(selectedCategory!!)
                else -> SettingsNavLevel.Overview
            }

            AnimatedContent(
                targetState = currentNavLevel,
                transitionSpec = {
                    val forward = targetState.depth >= initialState.depth
                    if (!settings.enableAnimations) {
                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                    } else if (forward) {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 320, easing = EaseOutCubic),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
                            targetOffsetX = { fullWidth -> -fullWidth / 4 }
                        )
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
                            initialOffsetX = { fullWidth -> -fullWidth / 4 }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(durationMillis = 320, easing = EaseOutCubic),
                            targetOffsetX = { fullWidth -> fullWidth }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(PlainTheme.colors.background),
                label = "SettingsNavTransition"
            ) { navLevel ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PlainTheme.colors.background)
                ) {
                    when (navLevel) {
                        is SettingsNavLevel.Overview -> {
                            // Category overview list with tabs and right chevrons
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PlainTheme.colors.background)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PlainSettingsCard {
                                        val categories = SettingsCategory.values()
                                        categories.forEachIndexed { index, category ->
                                            val isMarrowTools = category == SettingsCategory.MARROW_TOOLS
                                            PlainTextRow(
                                                title = category.title,
                                                titleColor = if (isMarrowTools) Color(0xFFFFD600) else PlainTheme.colors.onBackground,
                                                valueText = ">",
                                                titleFontSize = 18.sp,
                                                titleFontWeight = FontWeight.Bold,
                                                onClick = {
                                                    inClockSubPage = false
                                                    selectedCategory = category
                                                }
                                            )
                                            if (index < categories.size - 1) {
                                                PlainListDivider()
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        }

                        is SettingsNavLevel.ClockAndWidgets -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PlainTheme.colors.background)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                item {
                                    HomeScreenPreview(
                                        settings = settings,
                                        homeApps = homeApps
                                    )
                                    PlainListDivider()
                                }

                                item {
                                    PlainSectionHeader(title = "clock", isFirst = true)

                                    PlainSettingsCard {
                                        PlainTextToggle(
                                            title = "show clock",
                                            checked = settings.showClock,
                                            onCheckedChange = { viewModel.updateSettings(settings.copy(showClock = it)) }
                                        )

                                        if (settings.showClock) {
                                            PlainListDivider()

                                            PlainScrollableHorizontalOptionSelector(
                                                title = "clock font",
                                                options = ClockFont.values().toList(),
                                                selectedOption = settings.clockFont,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(clockFont = it)) },
                                                fontFamilyProvider = { getClockFontFamily(it) },
                                                labelProvider = { it.label }
                                            )

                                            PlainListDivider()

                                            PlainTextToggle(
                                                title = "24-hour time",
                                                checked = settings.clock24Hour,
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(clock24Hour = it)) }
                                            )

                                            PlainListDivider()

                                            PlainFlatSlider(
                                                title = "clock size",
                                                value = settings.clockSizeSp.toFloat(),
                                                onValueChange = { viewModel.updateSettings(settings.copy(clockSizeSp = it.toInt())) },
                                                valueRange = 16f..120f,
                                                valueDisplay = "${settings.clockSizeSp}sp"
                                            )
                                        }
                                    }

                                    PlainSectionHeader(title = "date")

                                    PlainSettingsCard {
                                        PlainTextToggle(
                                            title = "show date",
                                            checked = settings.showDate,
                                            onCheckedChange = { viewModel.updateSettings(settings.copy(showDate = it)) }
                                        )

                                        PlainListDivider()

                                        PlainFlatSlider(
                                            title = "date & widgets size",
                                            value = settings.dateWidgetSizeSp.toFloat(),
                                            onValueChange = { viewModel.updateSettings(settings.copy(dateWidgetSizeSp = it.toInt())) },
                                            valueRange = 10f..40f,
                                            valueDisplay = "${settings.dateWidgetSizeSp}sp"
                                        )
                                    }

                                    PlainSectionHeader(title = "widgets")

                                    val activeWidgets = settings.enabledWidgets.mapNotNull { id ->
                                        allAvailableWidgets.find { it.id == id }
                                    }
                                    val availableToAdd = allAvailableWidgets.filterNot { it.id in settings.enabledWidgets }

                                    PlainSettingsCard {
                                        activeWidgets.forEachIndexed { index, widget ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = {
                                                            val newWidgets = settings.enabledWidgets.filter { it != widget.id }
                                                            viewModel.updateSettings(
                                                                settings.copy(
                                                                    enabledWidgets = newWidgets,
                                                                    showWeather = "weather" in newWidgets,
                                                                    showBattery = "battery" in newWidgets
                                                                )
                                                            )
                                                        }
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                PlainText(
                                                    text = widget.title,
                                                    color = PlainTheme.colors.onBackground,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Normal
                                                )

                                                // Red circle with black minus
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .background(Color(0xFFE53935), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Canvas(modifier = Modifier.size(22.dp)) {
                                                        val strokeW = 2.dp.toPx()
                                                        val c = size.width / 2f
                                                        val halfL = 5.dp.toPx()
                                                        drawLine(
                                                            color = Color.Black,
                                                            start = Offset(c - halfL, c),
                                                            end = Offset(c + halfL, c),
                                                            strokeWidth = strokeW,
                                                            cap = StrokeCap.Round
                                                        )
                                                    }
                                                }
                                            }
                                            if (index < activeWidgets.size - 1 || availableToAdd.isNotEmpty()) {
                                                PlainListDivider()
                                            }
                                        }

                                        availableToAdd.forEachIndexed { index, widget ->
                                            val onBgColor = PlainTheme.colors.onBackground
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = {
                                                            val newWidgets = settings.enabledWidgets + widget.id
                                                            viewModel.updateSettings(
                                                                settings.copy(
                                                                    enabledWidgets = newWidgets,
                                                                    showWeather = "weather" in newWidgets,
                                                                    showBattery = "battery" in newWidgets
                                                                )
                                                            )
                                                            if (widget.id == "notifications" && !PlainNotificationListenerService.isPermissionGranted(context)) {
                                                                try {
                                                                    context.startActivity(
                                                                        Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                    )
                                                                } catch (_: Exception) {}
                                                            } else if (widget.id == "screen_time" && !WidgetDataProvider.isUsagePermissionGranted(context)) {
                                                                try {
                                                                    context.startActivity(
                                                                        Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                    )
                                                                } catch (_: Exception) {}
                                                            }
                                                        }
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                PlainText(
                                                    text = widget.title,
                                                    color = PlainTheme.colors.secondaryText,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Normal
                                                )

                                                // Plus sign
                                                Canvas(modifier = Modifier.size(20.dp)) {
                                                    val strokeW = 1.8.dp.toPx()
                                                    val c = size.width / 2f
                                                    val halfL = 6.dp.toPx()
                                                    drawLine(
                                                        color = onBgColor,
                                                        start = Offset(c - halfL, c),
                                                        end = Offset(c + halfL, c),
                                                        strokeWidth = strokeW,
                                                        cap = StrokeCap.Round
                                                    )
                                                    drawLine(
                                                        color = onBgColor,
                                                        start = Offset(c, c - halfL),
                                                        end = Offset(c, c + halfL),
                                                        strokeWidth = strokeW,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }
                                            if (index < availableToAdd.size - 1) {
                                                PlainListDivider()
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                        }
                    }

                    is SettingsNavLevel.PriorityAlerts -> {
                        PriorityAlertsSubPage(
                            context = context,
                            settings = settings,
                            onSettingsChange = { viewModel.updateSettings(it) },
                            rules = priorityRules,
                            allApps = allApps,
                            onAddRuleClick = {
                                ruleToEdit = null
                                showRuleWindow = true
                            },
                            onEditRuleClick = { rule ->
                                ruleToEdit = rule
                                showRuleWindow = true
                            }
                        )
                    }

                    is SettingsNavLevel.CategoryDetail -> {
                        val currentCategory = navLevel.category
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PlainTheme.colors.background)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            when (currentCategory) {
                                SettingsCategory.APPEARANCE -> {
                                    item {
                                        HomeScreenPreview(
                                            settings = settings,
                                            homeApps = homeApps
                                        )
                                    }

                                    item {
                                        PlainSectionHeader(title = "clock & widgets", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "clock face and widgets",
                                                valueText = ">",
                                                titleFontSize = 16.sp,
                                                titleFontWeight = FontWeight.Bold,
                                                onClick = { inClockSubPage = true }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        PlainSettingsCard {
                                            PlainHorizontalOptionSelector(
                                                title = "theme",
                                                options = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.AMOLED),
                                                selectedOption = when (settings.themeMode) {
                                                    ThemeMode.LIGHT -> ThemeMode.LIGHT
                                                    ThemeMode.AMOLED -> ThemeMode.AMOLED
                                                    else -> ThemeMode.DARK
                                                },
                                                onOptionSelected = { chosen ->
                                                    viewModel.updateSettings(settings.copy(themeMode = chosen))
                                                },
                                                labelProvider = { mode ->
                                                    when (mode) {
                                                        ThemeMode.LIGHT -> "light"
                                                        ThemeMode.DARK -> "dark"
                                                        ThemeMode.AMOLED -> "extra dark"
                                                        else -> mode.label
                                                    }
                                                }
                                            )
                                        }

                                        PlainSectionHeader(title = "alignment")

                                        PlainSettingsCard {
                                            PlainHorizontalOptionSelector(
                                                title = "clock alignment",
                                                options = AppTextAlign.values().toList(),
                                                selectedOption = settings.clockAlignment,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(clockAlignment = it)) },
                                                labelProvider = { it.label }
                                            )

                                            PlainListDivider()

                                            PlainHorizontalOptionSelector(
                                                title = "app alignment",
                                                options = AppTextAlign.values().toList(),
                                                selectedOption = settings.textAlign,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(textAlign = it)) },
                                                labelProvider = { it.label }
                                            )

                                            PlainListDivider()

                                            PlainHorizontalOptionSelector(
                                                title = "home vertical alignment",
                                                options = AppVerticalAlign.values().toList(),
                                                selectedOption = settings.verticalAlign,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(verticalAlign = it)) },
                                                labelProvider = { it.label }
                                            )
                                        }

                                        PlainSectionHeader(title = "app icons")

                                        PlainSettingsCard {
                                            PlainTextToggle(
                                                title = "miniature app icons",
                                                checked = settings.showAppIcons,
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(showAppIcons = it)) }
                                            )

                                            if (settings.showAppIcons) {
                                                PlainListDivider()

                                                PlainTextToggle(
                                                    title = "colored app icons",
                                                    checked = settings.coloredAppIcons,
                                                    onCheckedChange = { viewModel.updateSettings(settings.copy(coloredAppIcons = it)) }
                                                )
                                            }
                                        }

                                        PlainSectionHeader(title = "typography")

                                        PlainSettingsCard {
                                            PlainFlatSlider(
                                                title = "font size",
                                                value = settings.fontSizeSp.toFloat(),
                                                onValueChange = { viewModel.updateSettings(settings.copy(fontSizeSp = it.toInt())) },
                                                valueRange = 12f..32f,
                                                valueDisplay = "${settings.fontSizeSp}sp"
                                            )

                                            PlainListDivider()

                                            PlainTextOptionSelector(
                                                title = "font",
                                                options = LauncherFont.values().toList(),
                                                selectedOption = settings.selectedFont,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(selectedFont = it)) },
                                                fontFamilyProvider = { getAppFontFamily(it) },
                                                labelProvider = { it.label }
                                            )

                                            PlainListDivider()

                                            PlainHorizontalOptionSelector(
                                                title = "text case",
                                                options = listOf(AppTextCase.LOWERCASE, AppTextCase.CAPITALIZED),
                                                selectedOption = if (settings.textCase == AppTextCase.CAPITALIZED) AppTextCase.CAPITALIZED else AppTextCase.LOWERCASE,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(textCase = it)) },
                                                labelProvider = { if (it == AppTextCase.CAPITALIZED) "capitalised" else "lowercase" }
                                            )
                                        }

                                        PlainSectionHeader(title = "wallpaper & background")

                                        PlainSettingsCard {
                                            PlainTextToggle(
                                                title = "black system wallpaper",
                                                checked = settings.blackSystemWallpaper,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.updateSettings(settings.copy(blackSystemWallpaper = isChecked))
                                                    if (isChecked) {
                                                        scope.launch {
                                                            val success = WallpaperUtil.setSolidWallpaper(context, currentBgColor.toArgb(), homeOnly = true)
                                                            if (success) {
                                                                viewModel.showToast("home wallpaper set to black")
                                                            }
                                                        }
                                                    }
                                                }
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "set wallpaper to black now",
                                                valueText = "apply >",
                                                onClick = {
                                                    scope.launch {
                                                        val success = WallpaperUtil.setSolidWallpaper(context, currentBgColor.toArgb(), homeOnly = true)
                                                        if (success) {
                                                            viewModel.showToast("home wallpaper set to black")
                                                        } else {
                                                            viewModel.showToast("could not set wallpaper")
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                SettingsCategory.FAVORITES_DRAWER -> {
                                    item {
                                        PlainSectionHeader(title = "favorites", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "reorder favorites",
                                                valueText = "open >",
                                                onClick = { viewModel.navigateTo(Screen.ReorderFavorites) }
                                            )
                                        }

                                        PlainSectionHeader(title = "app drawer & search")

                                        PlainSettingsCard {
                                            PlainTextOptionSelector(
                                                title = "drawer sort order",
                                                options = DrawerSortOrder.values().toList(),
                                                selectedOption = settings.drawerSortOrder,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(drawerSortOrder = it)) },
                                                labelProvider = { it.label }
                                            )

                                            PlainListDivider()

                                            PlainTextToggle(
                                                title = "auto-open keyboard in search",
                                                checked = settings.autoOpenKeyboardInSearch,
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(autoOpenKeyboardInSearch = it)) }
                                            )

                                            PlainListDivider()

                                            PlainTextToggle(
                                                title = "auto-launch single search result",
                                                checked = settings.autoLaunchSingleSearchResult,
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(autoLaunchSingleSearchResult = it)) }
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "hidden apps",
                                                valueText = "open >",
                                                onClick = { viewModel.navigateTo(Screen.HiddenApps) }
                                            )
                                        }
                                    }
                                }

                                SettingsCategory.SCREEN_TIME_LIMITS -> {
                                    item {
                                        val allApps by viewModel.allApps.collectAsState()
                                        var screenTimeInfo by remember { mutableStateOf(ScreenTimeUtil.getQuickScreenTimeInfo(context)) }
                                        var isLoading by remember { mutableStateOf(true) }

                                        LaunchedEffect(allApps) {
                                            isLoading = true
                                            screenTimeInfo = ScreenTimeUtil.getScreenTimeInfo(context, allApps)
                                            isLoading = false
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val isOverLimit = screenTimeInfo.isLimitReached
                                        val barColor = if (isOverLimit) Color(0xFFFF5252) else Color(0xFF64B5F6)

                                        // 1. Thick rounded-rectangular bar with screen time and 4h limit
                                        PlainSettingsCard {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    PlainText(
                                                        text = "${screenTimeInfo.totalFormatted} today",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isOverLimit) Color(0xFFFF5252) else PlainTheme.colors.onBackground
                                                    )
                                                    PlainText(
                                                        text = if (isOverLimit) "4h limit reached" else "${screenTimeInfo.totalFormatted} / 4h max",
                                                        fontSize = 13.sp,
                                                        color = if (isOverLimit) Color(0xFFFF5252) else PlainTheme.colors.secondaryText
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(16.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(PlainTheme.colors.onBackground.copy(alpha = 0.14f))
                                                ) {
                                                    val fillFraction = if (screenTimeInfo.totalTimeMs > 0L) {
                                                        screenTimeInfo.usedFraction.coerceIn(0.02f, 1f)
                                                    } else 0f

                                                    if (fillFraction > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth(fraction = fillFraction)
                                                                .fillMaxHeight()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(barColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 2. Manage limits row
                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "app limits & digital wellbeing",
                                                valueText = ">",
                                                titleFontSize = 16.sp,
                                                titleFontWeight = FontWeight.Normal,
                                                onClick = {
                                                    ScreenTimeUtil.openDigitalWellbeingOrSettings(context)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 3. List of apps with icon, name, screen time today, and chevron
                                        PlainSettingsCard {
                                            if (!screenTimeInfo.hasPermission) {
                                                PlainTextRow(
                                                    title = "usage access required",
                                                    valueText = "grant >",
                                                    titleColor = Color(0xFFFF5252),
                                                    titleFontSize = 15.sp,
                                                    titleFontWeight = FontWeight.Normal,
                                                    onClick = {
                                                        context.startActivity(
                                                            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        )
                                                    }
                                                )
                                            } else if (isLoading && screenTimeInfo.appItems.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                                ) {
                                                    PlainText(
                                                        text = "calculating screen time...",
                                                        color = PlainTheme.colors.secondaryText,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            } else if (screenTimeInfo.appItems.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                                ) {
                                                    PlainText(
                                                        text = "no screen time recorded today",
                                                        color = PlainTheme.colors.secondaryText,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            } else {
                                                screenTimeInfo.appItems.forEachIndexed { index, item ->
                                                    val iconBitmap = remember(item.app.key) {
                                                        AppIconManager.getColoredIcon(context, item.app)
                                                            ?: AppIconManager.getIcon(context, item.app)
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null,
                                                                onClick = {
                                                                    AppLauncher.openAppInfo(context, item.app.packageName)
                                                                }
                                                            )
                                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (iconBitmap != null) {
                                                            Image(
                                                                bitmap = iconBitmap,
                                                                contentDescription = item.app.displayLabel,
                                                                modifier = Modifier.size(26.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(14.dp))
                                                        }

                                                        PlainText(
                                                            text = item.app.displayLabel,
                                                            color = PlainTheme.colors.onBackground,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        PlainText(
                                                            text = item.formattedTime,
                                                            color = PlainTheme.colors.secondaryText,
                                                            fontSize = 14.sp
                                                        )

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        PlainText(
                                                            text = ">",
                                                            color = PlainTheme.colors.secondaryText,
                                                            fontSize = 14.sp
                                                        )
                                                    }

                                                    if (index < screenTimeInfo.appItems.size - 1) {
                                                        PlainListDivider()
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                }

                                SettingsCategory.NOTIFICATIONS_ALERTS -> {
                                    item {
                                        val hasListenerPermission = PlainNotificationListenerService.isPermissionGranted(context)

                                        PlainSectionHeader(title = "priority alerts", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "priority alerts",
                                                valueText = ">",
                                                titleFontSize = 18.sp,
                                                titleFontWeight = FontWeight.Bold,
                                                onClick = { inPriorityAlertsSubPage = true }
                                            )
                                        }

                                        PlainSectionHeader(title = "notification badges")

                                        PlainSettingsCard {
                                            PlainTextToggle(
                                                title = "notification badges",
                                                checked = settings.showNotificationBadges,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.updateSettings(settings.copy(showNotificationBadges = isChecked))
                                                    if (isChecked && !hasListenerPermission) {
                                                        try {
                                                            context.startActivity(
                                                                Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                            )

                                            if (settings.showNotificationBadges) {
                                                PlainListDivider()

                                                PlainColorOptionSelector(
                                                    title = "badge color",
                                                    colors = UserSettings.BADGE_COLORS,
                                                    selectedColor = settings.notificationBadgeColor,
                                                    onColorSelected = { viewModel.updateSettings(settings.copy(notificationBadgeColor = it)) }
                                                )

                                                PlainListDivider()

                                                PlainTextToggle(
                                                    title = "recent notification text",
                                                    checked = settings.showRecentNotificationText,
                                                    onCheckedChange = { viewModel.updateSettings(settings.copy(showRecentNotificationText = it)) }
                                                )
                                            }
                                        }

                                        PlainSectionHeader(title = "home screen alerts")

                                        PlainSettingsCard {
                                            val isNotifWidgetEnabled = settings.enabledWidgets.contains("notifications")
                                            PlainTextToggle(
                                                title = "unread notifications widget",
                                                checked = isNotifWidgetEnabled,
                                                onCheckedChange = { isChecked ->
                                                    val currentList = settings.enabledWidgets.toMutableList()
                                                    if (isChecked) {
                                                        if (!currentList.contains("notifications")) {
                                                            currentList.add("notifications")
                                                        }
                                                        if (!hasListenerPermission) {
                                                            try {
                                                                context.startActivity(
                                                                    Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                )
                                                            } catch (_: Exception) {}
                                                        }
                                                    } else {
                                                        currentList.remove("notifications")
                                                    }
                                                    viewModel.updateSettings(settings.copy(enabledWidgets = currentList))
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                }

                                SettingsCategory.DEVICE_STORAGE -> {
                                    item {
                                        val allApps by viewModel.allApps.collectAsState()
                                        var storageInfo by remember { mutableStateOf(StorageUtil.getQuickStorageInfo(context)) }

                                        LaunchedEffect(allApps) {
                                            storageInfo = StorageUtil.getDeviceStorageInfo(context, allApps)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 1. Thick rounded-rectangular bar with storage available and used fraction
                                        PlainSettingsCard {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    PlainText(
                                                        text = "${storageInfo.availableFormatted} available",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PlainTheme.colors.onBackground
                                                    )
                                                    PlainText(
                                                        text = "${storageInfo.usedFormatted} / ${storageInfo.totalFormatted} used",
                                                        fontSize = 13.sp,
                                                        color = PlainTheme.colors.secondaryText
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(16.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(PlainTheme.colors.onBackground.copy(alpha = 0.14f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction = storageInfo.usedFraction.coerceIn(0.02f, 1f))
                                                            .fillMaxHeight()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(PlainTheme.colors.onBackground)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 2. Manage files single list item
                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "manage files",
                                                valueText = ">",
                                                titleFontSize = 16.sp,
                                                titleFontWeight = FontWeight.Normal,
                                                onClick = {
                                                    StorageUtil.openFilesApp(context)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 3. List of apps with icon, name, storage occupied, and chevron
                                        PlainSettingsCard {
                                            if (storageInfo.appItems.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                                ) {
                                                    PlainText(
                                                        text = "calculating app sizes...",
                                                        color = PlainTheme.colors.secondaryText,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            } else {
                                                storageInfo.appItems.forEachIndexed { index, item ->
                                                    val iconBitmap = remember(item.app.key) {
                                                        AppIconManager.getColoredIcon(context, item.app)
                                                            ?: AppIconManager.getIcon(context, item.app)
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null,
                                                                onClick = {
                                                                    AppLauncher.openAppInfo(context, item.app.packageName)
                                                                }
                                                            )
                                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (iconBitmap != null) {
                                                            Image(
                                                                bitmap = iconBitmap,
                                                                contentDescription = item.app.displayLabel,
                                                                modifier = Modifier.size(26.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(14.dp))
                                                        }

                                                        PlainText(
                                                            text = item.app.displayLabel,
                                                            color = PlainTheme.colors.onBackground,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        PlainText(
                                                            text = item.formattedSize,
                                                            color = PlainTheme.colors.secondaryText,
                                                            fontSize = 14.sp
                                                        )

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        PlainText(
                                                            text = ">",
                                                            color = PlainTheme.colors.secondaryText,
                                                            fontSize = 14.sp
                                                        )
                                                    }

                                                    if (index < storageInfo.appItems.size - 1) {
                                                        PlainListDivider()
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                }

                                SettingsCategory.SYSTEM -> {
                                    item {
                                        PlainSectionHeader(title = "system bars", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextToggle(
                                                title = "show status bar",
                                                checked = settings.showStatusBar,
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(showStatusBar = it)) }
                                            )

                                            PlainListDivider()

                                            PlainTextToggle(
                                                title = "show navigation bar",
                                                checked = settings.showNavBar,
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(showNavBar = it)) }
                                            )
                                        }

                                        PlainSectionHeader(title = "device & controls")

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "gestures",
                                                valueText = "open >",
                                                onClick = { viewModel.navigateTo(Screen.GestureSettings) }
                                            )

                                            PlainListDivider()

                                            PlainTextOptionSelector(
                                                title = "screen orientation",
                                                options = AppOrientation.values().toList(),
                                                selectedOption = settings.screenOrientation,
                                                onOptionSelected = { viewModel.updateSettings(settings.copy(screenOrientation = it)) },
                                                labelProvider = { it.label }
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "set as default launcher",
                                                valueText = if (isDefaultLauncher) "[current]" else "[set now]",
                                                onClick = {
                                                    if (!isDefaultLauncher) {
                                                        AppLauncher.createDefaultLauncherIntent(context)?.let {
                                                            defaultLauncherLauncher.launch(it)
                                                        }
                                                    }
                                                }
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "device admin (screen lock)",
                                                valueText = if (isDeviceAdminActive) "[active]" else "[grant]",
                                                onClick = {
                                                    if (!isDeviceAdminActive) {
                                                        val intent = AppLauncher.createDeviceAdminIntent(context)
                                                        deviceAdminLauncher.launch(intent)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                SettingsCategory.ACCESSIBILITY -> {
                                    item {
                                        PlainSectionHeader(title = "accessibility", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextToggle(
                                                title = "haptic feedback",
                                                checked = settings.enableHapticFeedback,
                                                subtitle = "vibrations on controls, gestures and long-press",
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(enableHapticFeedback = it)) }
                                            )

                                            PlainListDivider()

                                            PlainTextToggle(
                                                title = "animations",
                                                checked = settings.enableAnimations,
                                                subtitle = "screen transitions and control animations",
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(enableAnimations = it)) }
                                            )
                                        }
                                    }
                                }

                                SettingsCategory.BACKUP -> {
                                    item {
                                        PlainSectionHeader(title = "backup & restore", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "backup to JSON file",
                                                valueText = "[export]",
                                                onClick = { backupLauncher.launch("plain-backup.json") }
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "restore from JSON file",
                                                valueText = "[import]",
                                                onClick = { restoreLauncher.launch(arrayOf("application/json")) }
                                            )
                                        }

                                        PlainSectionHeader(title = "reset")

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "reset all settings to defaults",
                                                valueText = "[reset]",
                                                onClick = { showResetDialog = true }
                                            )
                                        }
                                    }
                                }

                                SettingsCategory.ABOUT -> {
                                    item {
                                        val deviceName = remember {
                                            val customName = try {
                                                android.provider.Settings.Global.getString(
                                                    context.contentResolver,
                                                    "device_name"
                                                )
                                            } catch (e: Exception) {
                                                null
                                            }
                                            if (!customName.isNullOrBlank()) {
                                                customName
                                            } else {
                                                val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                                                val model = android.os.Build.MODEL
                                                if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
                                            }
                                        }

                                        val androidVersion = remember {
                                            "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
                                        }

                                        val processor = remember { DeviceInfoUtil.getProcessorName() }
                                        val ram = remember { DeviceInfoUtil.getTotalRam(context) }
                                        val storage = remember { DeviceInfoUtil.getMaxStorage(context) }

                                        PlainSectionHeader(title = "about", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextRow(
                                                title = "launcher",
                                                valueText = "Marrow 1.0.0"
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "device",
                                                valueText = deviceName
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "android",
                                                valueText = androidVersion
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "processor",
                                                valueText = processor
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "ram",
                                                valueText = ram
                                            )

                                            PlainListDivider()

                                            PlainTextRow(
                                                title = "storage",
                                                valueText = storage
                                            )
                                        }
                                    }
                                }

                                SettingsCategory.MARROW_TOOLS -> {
                                    item {
                                        PlainSectionHeader(title = "marrow tools", isFirst = true)

                                        PlainSettingsCard {
                                            PlainTextToggle(
                                                title = "quick look-up",
                                                checked = settings.enableWebLookup,
                                                subtitle = "maths solver and web search in app drawer",
                                                onCheckedChange = { viewModel.updateSettings(settings.copy(enableWebLookup = it)) }
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }

        // Reset confirmation dialog
        if (showResetDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlainTheme.colors.background.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showResetDialog = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(PlainTheme.colors.surface)
                        .padding(24.dp)
                ) {
                    PlainText(
                        text = "reset to defaults?",
                        color = PlainTheme.colors.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PlainText(
                        text = "All customizations, renames, and pins will be reset to default values.",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showResetDialog = false }
                                )
                                .padding(8.dp)
                        ) {
                            PlainText(
                                text = "[cancel]",
                                color = PlainTheme.colors.secondaryText,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        showResetDialog = false
                                        viewModel.resetToDefaults()
                                    }
                                )
                                .padding(8.dp)
                        ) {
                            PlainText(
                                text = "[confirm reset]",
                                color = PlainTheme.colors.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (showRuleWindow) {
            RuleEditWindowDialog(
                context = context,
                existingRule = ruleToEdit,
                allApps = allApps,
                onDismiss = { showRuleWindow = false },
                onSave = { rule ->
                    PriorityAlertStore.saveRule(context, rule)
                    showRuleWindow = false
                },
                onDelete = { ruleId ->
                    PriorityAlertStore.deleteRule(context, ruleId)
                    showRuleWindow = false
                }
            )
        }
    }
}

@Composable
private fun PriorityAlertsSubPage(
    context: Context,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    rules: List<PriorityAlertRule>,
    allApps: List<com.plain.launcher.data.model.AppItem>,
    onAddRuleClick: () -> Unit,
    onEditRuleClick: (PriorityAlertRule) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                PlainSettingsCard {
                    PlainFlatSlider(
                        title = "corner radius",
                        value = settings.edgeGlowCornerRadiusDp.toFloat(),
                        onValueChange = {
                            val newRadius = it.toInt()
                            onSettingsChange(settings.copy(edgeGlowCornerRadiusDp = newRadius))
                            com.plain.launcher.util.PriorityAlertManager.triggerEdgeGlow(
                                context = context,
                                cornerRadiusDp = newRadius
                            )
                        },
                        valueRange = 0f..60f,
                        valueDisplay = "${settings.edgeGlowCornerRadiusDp} dp"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (rules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlainText(
                            text = "no priority alerts",
                            color = PlainTheme.colors.secondaryText,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                itemsIndexed(rules) { index, rule ->
                    val ruleApp = allApps.find { it.packageName == rule.packageName }
                    val icon = ruleApp?.let { AppIconManager.getIcon(context, it) }
                        ?: AppIconManager.getIconByPackage(context, rule.packageName)

                    PlainSettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditRuleClick(rule) }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PlainTheme.colors.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PlainText(
                                        text = rule.appName.take(1).uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                PlainText(
                                    text = rule.appName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (rule.keyword.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    PlainText(
                                        text = "\"${rule.keyword}\"",
                                        fontSize = 13.sp,
                                        color = PlainTheme.colors.secondaryText
                                    )
                                }

                                val actions = buildList {
                                    if (rule.glowEdges) add("glow edges")
                                    if (rule.flashTorch) add("flash")
                                }
                                if (actions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        actions.forEach { action ->
                                            PlainText(
                                                text = action,
                                                fontSize = 12.sp,
                                                color = if (action == "glow edges") Color(rule.glowColor) else PlainTheme.colors.secondaryText
                                            )
                                        }
                                    }
                                }
                            }

                            PlainText(
                                text = ">",
                                fontSize = 18.sp,
                                color = PlainTheme.colors.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Rectangular rounded wide plus on the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .height(56.dp)
                .clip(CircleShape)
                .background(PlainTheme.colors.surface)
                .clickable { onAddRuleClick() },
            contentAlignment = Alignment.Center
        ) {
            PlainText(
                text = "+",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = PlainTheme.colors.onBackground
            )
        }
    }
}

@Composable
private fun RuleEditWindowDialog(
    context: Context,
    existingRule: PriorityAlertRule?,
    allApps: List<com.plain.launcher.data.model.AppItem>,
    onDismiss: () -> Unit,
    onSave: (PriorityAlertRule) -> Unit,
    onDelete: (String) -> Unit
) {
    var selectedApp by remember {
        mutableStateOf(allApps.find { it.packageName == existingRule?.packageName })
    }
    var keyword by remember { mutableStateOf(existingRule?.keyword ?: "") }
    var glowEdges by remember { mutableStateOf(existingRule?.glowEdges ?: true) }
    var glowColor by remember { mutableStateOf(existingRule?.glowColor ?: 0xFFFFD600L) }
    var flashTorch by remember { mutableStateOf(existingRule?.flashTorch ?: false) }
    var showAppPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(PlainTheme.colors.background)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Select app row
                PlainSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppPicker = true }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedApp != null) {
                            val icon = AppIconManager.getIcon(context, selectedApp!!)
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            PlainText(
                                text = selectedApp!!.displayLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PlainText(
                                text = "select app",
                                fontSize = 16.sp,
                                color = PlainTheme.colors.secondaryText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        PlainText(
                            text = ">",
                            fontSize = 16.sp,
                            color = PlainTheme.colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional keyword input
                PlainSettingsCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        if (keyword.isEmpty()) {
                            PlainText(
                                text = "keyword (optional)",
                                color = PlainTheme.colors.secondaryText,
                                fontSize = 16.sp
                            )
                        }
                        BasicTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = PlainTheme.colors.onBackground,
                                fontSize = 16.sp,
                                fontFamily = GeistMonoFamily
                            ),
                            cursorBrush = SolidColor(PlainTheme.colors.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggles
                PlainSettingsCard {
                    PlainTextToggle(
                        title = "glow edges",
                        checked = glowEdges,
                        onCheckedChange = { glowEdges = it }
                    )
                    if (glowEdges) {
                        PlainListDivider()
                        PlainColorOptionSelector(
                            title = "glow color",
                            colors = UserSettings.BADGE_COLORS,
                            selectedColor = glowColor,
                            onColorSelected = {
                                glowColor = it
                                com.plain.launcher.util.PriorityAlertManager.triggerEdgeGlow(
                                    context = context,
                                    color = it
                                )
                            }
                        )
                    }
                    PlainListDivider()
                    PlainTextToggle(
                        title = "flash",
                        checked = flashTorch,
                        onCheckedChange = { flashTorch = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wide save button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selectedApp != null) PlainTheme.colors.onBackground else PlainTheme.colors.surface)
                        .clickable(enabled = selectedApp != null) {
                            val rule = PriorityAlertRule(
                                id = existingRule?.id ?: UUID.randomUUID().toString(),
                                packageName = selectedApp!!.packageName,
                                appName = selectedApp!!.displayLabel,
                                keyword = keyword.trim(),
                                glowEdges = glowEdges,
                                flashTorch = flashTorch,
                                glowColor = glowColor
                            )
                            if (rule.glowEdges) {
                                com.plain.launcher.util.PriorityAlertManager.triggerEdgeGlow(
                                    context = context,
                                    color = rule.glowColor
                                )
                            }
                            onSave(rule)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PlainText(
                        text = "save",
                        color = if (selectedApp != null) PlainTheme.colors.background else PlainTheme.colors.secondaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (existingRule != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDelete(existingRule.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PlainText(
                            text = "delete rule",
                            color = Color(0xFFFF5252),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        RuleAppPickerDialog(
            context = context,
            apps = allApps,
            onDismiss = { showAppPicker = false },
            onSelect = { app ->
                selectedApp = app
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun RuleAppPickerDialog(
    context: Context,
    apps: List<com.plain.launcher.data.model.AppItem>,
    onDismiss: () -> Unit,
    onSelect: (com.plain.launcher.data.model.AppItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps: List<com.plain.launcher.data.model.AppItem> = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) {
            apps.sortedBy { it.displayLabel.lowercase() }
        } else {
            apps.filter { it.displayLabel.contains(searchQuery.trim(), ignoreCase = true) }
                .sortedBy { it.displayLabel.lowercase() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
                .clip(RoundedCornerShape(20.dp))
                .background(PlainTheme.colors.background)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlainSettingsCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            PlainText(
                                text = "search apps...",
                                color = PlainTheme.colors.secondaryText,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = PlainTheme.colors.onBackground,
                                fontSize = 15.sp,
                                fontFamily = GeistMonoFamily
                            ),
                            cursorBrush = SolidColor(PlainTheme.colors.onBackground),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(items = filteredApps) { index: Int, app: com.plain.launcher.data.model.AppItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = AppIconManager.getIcon(context, app)
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                            PlainText(
                                text = app.displayLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        if (index < filteredApps.size - 1) {
                            PlainListDivider()
                        }
                    }
                }
            }
        }
    }
}
