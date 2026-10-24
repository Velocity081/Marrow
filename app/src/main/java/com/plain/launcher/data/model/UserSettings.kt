package com.plain.launcher.data.model

enum class LauncherFont(val label: String) {
    GEIST("Geist"),
    GEIST_MONO("Geist Mono"),
    FIRA_MONO("Fira Mono")
}

enum class ClockFont(val label: String) {
    GEIST("Geist"),
    GEIST_MONO("Geist Mono"),
    FIRA_MONO("Fira Mono"),
    DOTO("Doto"),
    FRAUNCES("Fraunces"),
    UNBOUNDED("Unbounded"),
    BIG_SHOULDERS("Big Shoulders Display")
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val selectedFont: LauncherFont = LauncherFont.GEIST,
    val clockFont: ClockFont = ClockFont.GEIST_MONO,
    val backgroundGray: Float = 0.0f,
    val backgroundBrightness: Float = 1.0f,
    val blackSystemWallpaper: Boolean = true,
    val textGray: Float = 0.95f,
    val fontSizeSp: Int = 22,
    val fontWeight: AppFontWeight = AppFontWeight.REGULAR,
    val letterSpacingEm: Float = 0.0f,
    val lineSpacingMultiplier: Float = 1.7f,
    val itemSpacingDp: Int = 12,
    val textAlign: AppTextAlign = AppTextAlign.CENTER,
    val verticalAlign: AppVerticalAlign = AppVerticalAlign.CENTER,
    val textCase: AppTextCase = AppTextCase.LOWERCASE,
    val showAppIcons: Boolean = false,
    val coloredAppIcons: Boolean = false,
    val showNotificationBadges: Boolean = false,
    val notificationBadgeColor: Long = 0xFFFFD600L,
    val showRecentNotificationText: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val enableAnimations: Boolean = true,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showWeather: Boolean = true,
    val showBattery: Boolean = false,
    val enabledWidgets: List<String> = listOf("weather"),
    val enableWebLookup: Boolean = true,
    val weatherText: String = "13° sunny",
    val weatherCity: String = "Auto (GPS)",
    val weatherUnit: String = "celsius",
    val weatherLastUpdated: Long = 0L,
    val clock24Hour: Boolean = false,
    val clockSizeSp: Int = 48,
    val dateWidgetSizeSp: Int = 20,
    val clockAlignment: AppTextAlign = AppTextAlign.CENTER,
    val dateFormat: String = "EEE, MMM d",
    val maxFavoritesCount: Int = 0,
    val drawerSortOrder: DrawerSortOrder = DrawerSortOrder.AZ,
    val showStatusBar: Boolean = true,
    val showNavBar: Boolean = true,
    val screenOrientation: AppOrientation = AppOrientation.SYSTEM,
    val autoOpenKeyboardInSearch: Boolean = true,
    val autoLaunchSingleSearchResult: Boolean = false,
    val gestureSwipeUp: GestureAction = GestureAction.OPEN_SEARCH,
    val gestureSwipeDown: GestureAction = GestureAction.OPEN_NOTESPACE,
    val gestureSwipeLeft: GestureAction = GestureAction.NONE,
    val gestureSwipeRight: GestureAction = GestureAction.NONE,
    val gestureDoubleTap: GestureAction = GestureAction.NONE,
    val gestureCustomApps: Map<String, String> = emptyMap(),
    val edgeGlowCornerRadiusDp: Int = 32
) {
    companion object {
        val BADGE_COLORS: List<Long> = listOf(
            0xFFFF3B30L, // Red
            0xFFFF9500L, // Orange
            0xFFFFD600L, // Yellow
            0xFF34C759L, // Green
            0xFF00E5FFL, // Cyan
            0xFF007AFFL, // Blue
            0xFFAF52DEL  // Purple
        )
    }
}
