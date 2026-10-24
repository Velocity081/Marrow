package com.plain.launcher.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.plain.launcher.data.model.AppFontWeight
import com.plain.launcher.data.model.AppOrientation
import com.plain.launcher.data.model.AppTextAlign
import com.plain.launcher.data.model.AppTextCase
import com.plain.launcher.data.model.AppVerticalAlign
import com.plain.launcher.data.model.DrawerSortOrder
import com.plain.launcher.data.model.GestureAction
import com.plain.launcher.data.model.ThemeMode
import com.plain.launcher.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_FONT = stringPreferencesKey("selected_font")
        val CLOCK_FONT = stringPreferencesKey("clock_font")
        val BG_GRAY = floatPreferencesKey("bg_gray")
        val BG_BRIGHTNESS = floatPreferencesKey("bg_brightness")
        val BLACK_SYSTEM_WALLPAPER = booleanPreferencesKey("black_system_wallpaper")
        val TEXT_GRAY = floatPreferencesKey("text_gray")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val FONT_WEIGHT = stringPreferencesKey("font_weight")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val ITEM_SPACING_DP = intPreferencesKey("item_spacing_dp")
        val TEXT_ALIGN = stringPreferencesKey("text_align")
        val VERTICAL_ALIGN = stringPreferencesKey("vertical_align")
        val TEXT_CASE = stringPreferencesKey("text_case")
        val SHOW_APP_ICONS = booleanPreferencesKey("show_app_icons")
        val COLORED_APP_ICONS = booleanPreferencesKey("colored_app_icons")
        val SHOW_NOTIFICATION_BADGES = booleanPreferencesKey("show_notification_badges")
        val NOTIFICATION_BADGE_COLOR = longPreferencesKey("notification_badge_color")
        val SHOW_RECENT_NOTIFICATION_TEXT = booleanPreferencesKey("show_recent_notification_text")
        val ENABLE_HAPTIC = booleanPreferencesKey("enable_haptic")
        val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        val ENABLED_WIDGETS = stringPreferencesKey("enabled_widgets")
        val ENABLE_WEB_LOOKUP = booleanPreferencesKey("enable_web_lookup")
        val WEATHER_TEXT = stringPreferencesKey("weather_text")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_UNIT = stringPreferencesKey("weather_unit")
        val WEATHER_LAST_UPDATED = longPreferencesKey("weather_last_updated")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val CLOCK_SIZE_SP = intPreferencesKey("clock_size_sp")
        val DATE_WIDGET_SIZE_SP = intPreferencesKey("date_widget_size_sp")
        val CLOCK_ALIGNMENT = stringPreferencesKey("clock_alignment")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val MAX_FAVORITES = intPreferencesKey("max_favorites")
        val DRAWER_SORT = stringPreferencesKey("drawer_sort")
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        val SHOW_NAV_BAR = booleanPreferencesKey("show_nav_bar")
        val SCREEN_ORIENTATION = stringPreferencesKey("screen_orientation")
        val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        val AUTO_LAUNCH_SINGLE = booleanPreferencesKey("auto_launch_single")
        val GESTURE_SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
        val GESTURE_SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
        val GESTURE_SWIPE_LEFT = stringPreferencesKey("gesture_swipe_left")
        val GESTURE_SWIPE_RIGHT = stringPreferencesKey("gesture_swipe_right")
        val GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        val GESTURE_CUSTOM_APPS = stringPreferencesKey("gesture_custom_apps")
        val EDGE_GLOW_CORNER_RADIUS_DP = intPreferencesKey("edge_glow_corner_radius_dp")
        val HAS_INITIALIZED_NOTESPACE_GESTURE = booleanPreferencesKey("has_initialized_notespace_gesture")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        val defaults = UserSettings()

        val customAppsStr = prefs[Keys.GESTURE_CUSTOM_APPS] ?: "{}"
        val customAppsMap = mutableMapOf<String, String>()
        try {
            val json = JSONObject(customAppsStr)
            json.keys().forEach { k -> customAppsMap[k] = json.getString(k) }
        } catch (e: Exception) {
            // ignore
        }

        UserSettings(
            themeMode = safeValueOf(prefs[Keys.THEME_MODE], defaults.themeMode),
            selectedFont = safeValueOf(prefs[Keys.SELECTED_FONT], defaults.selectedFont),
            clockFont = safeValueOf(prefs[Keys.CLOCK_FONT], defaults.clockFont),
            backgroundGray = prefs[Keys.BG_GRAY] ?: defaults.backgroundGray,
            backgroundBrightness = prefs[Keys.BG_BRIGHTNESS] ?: defaults.backgroundBrightness,
            blackSystemWallpaper = prefs[Keys.BLACK_SYSTEM_WALLPAPER] ?: defaults.blackSystemWallpaper,
            textGray = prefs[Keys.TEXT_GRAY] ?: defaults.textGray,
            fontSizeSp = prefs[Keys.FONT_SIZE_SP] ?: defaults.fontSizeSp,
            fontWeight = safeValueOf(prefs[Keys.FONT_WEIGHT], defaults.fontWeight),
            letterSpacingEm = prefs[Keys.LETTER_SPACING] ?: defaults.letterSpacingEm,
            lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: defaults.lineSpacingMultiplier,
            itemSpacingDp = prefs[Keys.ITEM_SPACING_DP] ?: defaults.itemSpacingDp,
            textAlign = safeValueOf(prefs[Keys.TEXT_ALIGN], defaults.textAlign),
            verticalAlign = safeValueOf(prefs[Keys.VERTICAL_ALIGN], defaults.verticalAlign),
            textCase = safeValueOf(prefs[Keys.TEXT_CASE], defaults.textCase),
            showAppIcons = prefs[Keys.SHOW_APP_ICONS] ?: defaults.showAppIcons,
            coloredAppIcons = prefs[Keys.COLORED_APP_ICONS] ?: defaults.coloredAppIcons,
            showNotificationBadges = prefs[Keys.SHOW_NOTIFICATION_BADGES] ?: defaults.showNotificationBadges,
            notificationBadgeColor = prefs[Keys.NOTIFICATION_BADGE_COLOR] ?: defaults.notificationBadgeColor,
            showRecentNotificationText = prefs[Keys.SHOW_RECENT_NOTIFICATION_TEXT] ?: defaults.showRecentNotificationText,
            enableHapticFeedback = prefs[Keys.ENABLE_HAPTIC] ?: defaults.enableHapticFeedback,
            enableAnimations = prefs[Keys.ENABLE_ANIMATIONS] ?: defaults.enableAnimations,
            showClock = prefs[Keys.SHOW_CLOCK] ?: defaults.showClock,
            showDate = prefs[Keys.SHOW_DATE] ?: defaults.showDate,
            showWeather = prefs[Keys.SHOW_WEATHER] ?: defaults.showWeather,
            showBattery = prefs[Keys.SHOW_BATTERY] ?: defaults.showBattery,
            enabledWidgets = prefs[Keys.ENABLED_WIDGETS]?.let { str ->
                if (str.isBlank()) emptyList() else str.split(",")
            } ?: run {
                val list = mutableListOf("weather")
                if (prefs[Keys.SHOW_BATTERY] == true) list.add("battery")
                list
            },
            enableWebLookup = prefs[Keys.ENABLE_WEB_LOOKUP] ?: defaults.enableWebLookup,
            weatherText = prefs[Keys.WEATHER_TEXT] ?: defaults.weatherText,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: defaults.weatherCity,
            weatherUnit = prefs[Keys.WEATHER_UNIT] ?: defaults.weatherUnit,
            weatherLastUpdated = prefs[Keys.WEATHER_LAST_UPDATED] ?: defaults.weatherLastUpdated,
            clock24Hour = prefs[Keys.CLOCK_24H] ?: defaults.clock24Hour,
            clockSizeSp = prefs[Keys.CLOCK_SIZE_SP] ?: defaults.clockSizeSp,
            dateWidgetSizeSp = prefs[Keys.DATE_WIDGET_SIZE_SP] ?: defaults.dateWidgetSizeSp,
            clockAlignment = safeValueOf(prefs[Keys.CLOCK_ALIGNMENT], defaults.clockAlignment),
            dateFormat = prefs[Keys.DATE_FORMAT] ?: defaults.dateFormat,
            maxFavoritesCount = defaults.maxFavoritesCount,
            drawerSortOrder = safeValueOf(prefs[Keys.DRAWER_SORT], defaults.drawerSortOrder),
            showStatusBar = prefs[Keys.SHOW_STATUS_BAR] ?: defaults.showStatusBar,
            showNavBar = prefs[Keys.SHOW_NAV_BAR] ?: defaults.showNavBar,
            screenOrientation = safeValueOf(prefs[Keys.SCREEN_ORIENTATION], defaults.screenOrientation),
            autoOpenKeyboardInSearch = prefs[Keys.AUTO_OPEN_KEYBOARD] ?: defaults.autoOpenKeyboardInSearch,
            autoLaunchSingleSearchResult = prefs[Keys.AUTO_LAUNCH_SINGLE] ?: defaults.autoLaunchSingleSearchResult,
            gestureSwipeUp = safeValueOf(prefs[Keys.GESTURE_SWIPE_UP], defaults.gestureSwipeUp),
            gestureSwipeDown = if ((prefs[Keys.HAS_INITIALIZED_NOTESPACE_GESTURE] ?: false).not() && (prefs[Keys.GESTURE_SWIPE_DOWN] == null || prefs[Keys.GESTURE_SWIPE_DOWN] == "NONE")) {
                GestureAction.OPEN_NOTESPACE
            } else {
                safeValueOf(prefs[Keys.GESTURE_SWIPE_DOWN], defaults.gestureSwipeDown)
            },
            gestureSwipeLeft = safeValueOf(prefs[Keys.GESTURE_SWIPE_LEFT], defaults.gestureSwipeLeft),
            gestureSwipeRight = safeValueOf(prefs[Keys.GESTURE_SWIPE_RIGHT], defaults.gestureSwipeRight),
            gestureDoubleTap = safeValueOf(prefs[Keys.GESTURE_DOUBLE_TAP], defaults.gestureDoubleTap),
            gestureCustomApps = customAppsMap,
            edgeGlowCornerRadiusDp = prefs[Keys.EDGE_GLOW_CORNER_RADIUS_DP] ?: defaults.edgeGlowCornerRadiusDp
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.SELECTED_FONT] = settings.selectedFont.name
            prefs[Keys.CLOCK_FONT] = settings.clockFont.name
            prefs[Keys.BG_GRAY] = settings.backgroundGray
            prefs[Keys.BG_BRIGHTNESS] = settings.backgroundBrightness
            prefs[Keys.BLACK_SYSTEM_WALLPAPER] = settings.blackSystemWallpaper
            prefs[Keys.TEXT_GRAY] = settings.textGray
            prefs[Keys.FONT_SIZE_SP] = settings.fontSizeSp
            prefs[Keys.FONT_WEIGHT] = settings.fontWeight.name
            prefs[Keys.LETTER_SPACING] = settings.letterSpacingEm
            prefs[Keys.LINE_SPACING] = settings.lineSpacingMultiplier
            prefs[Keys.ITEM_SPACING_DP] = settings.itemSpacingDp
            prefs[Keys.TEXT_ALIGN] = settings.textAlign.name
            prefs[Keys.VERTICAL_ALIGN] = settings.verticalAlign.name
            prefs[Keys.TEXT_CASE] = settings.textCase.name
            prefs[Keys.SHOW_APP_ICONS] = settings.showAppIcons
            prefs[Keys.COLORED_APP_ICONS] = settings.coloredAppIcons
            prefs[Keys.SHOW_NOTIFICATION_BADGES] = settings.showNotificationBadges
            prefs[Keys.NOTIFICATION_BADGE_COLOR] = settings.notificationBadgeColor
            prefs[Keys.SHOW_RECENT_NOTIFICATION_TEXT] = settings.showRecentNotificationText
            prefs[Keys.ENABLE_HAPTIC] = settings.enableHapticFeedback
            prefs[Keys.ENABLE_ANIMATIONS] = settings.enableAnimations
            prefs[Keys.SHOW_CLOCK] = settings.showClock
            prefs[Keys.SHOW_DATE] = settings.showDate
            prefs[Keys.SHOW_WEATHER] = settings.enabledWidgets.contains("weather")
            prefs[Keys.SHOW_BATTERY] = settings.enabledWidgets.contains("battery")
            prefs[Keys.ENABLED_WIDGETS] = settings.enabledWidgets.joinToString(",")
            prefs[Keys.ENABLE_WEB_LOOKUP] = settings.enableWebLookup
            prefs[Keys.WEATHER_TEXT] = settings.weatherText
            prefs[Keys.WEATHER_CITY] = settings.weatherCity
            prefs[Keys.WEATHER_UNIT] = settings.weatherUnit
            prefs[Keys.WEATHER_LAST_UPDATED] = settings.weatherLastUpdated
            prefs[Keys.CLOCK_24H] = settings.clock24Hour
            prefs[Keys.CLOCK_SIZE_SP] = settings.clockSizeSp
            prefs[Keys.DATE_WIDGET_SIZE_SP] = settings.dateWidgetSizeSp
            prefs[Keys.CLOCK_ALIGNMENT] = settings.clockAlignment.name
            prefs[Keys.DATE_FORMAT] = settings.dateFormat
            prefs[Keys.MAX_FAVORITES] = settings.maxFavoritesCount
            prefs[Keys.DRAWER_SORT] = settings.drawerSortOrder.name
            prefs[Keys.SHOW_STATUS_BAR] = settings.showStatusBar
            prefs[Keys.SHOW_NAV_BAR] = settings.showNavBar
            prefs[Keys.SCREEN_ORIENTATION] = settings.screenOrientation.name
            prefs[Keys.AUTO_OPEN_KEYBOARD] = settings.autoOpenKeyboardInSearch
            prefs[Keys.AUTO_LAUNCH_SINGLE] = settings.autoLaunchSingleSearchResult
            prefs[Keys.GESTURE_SWIPE_UP] = settings.gestureSwipeUp.name
            prefs[Keys.GESTURE_SWIPE_DOWN] = settings.gestureSwipeDown.name
            prefs[Keys.GESTURE_SWIPE_LEFT] = settings.gestureSwipeLeft.name
            prefs[Keys.GESTURE_SWIPE_RIGHT] = settings.gestureSwipeRight.name
            prefs[Keys.GESTURE_DOUBLE_TAP] = settings.gestureDoubleTap.name

            val customAppsObj = JSONObject()
            settings.gestureCustomApps.forEach { (k, v) -> customAppsObj.put(k, v) }
            prefs[Keys.GESTURE_CUSTOM_APPS] = customAppsObj.toString()
            prefs[Keys.EDGE_GLOW_CORNER_RADIUS_DP] = settings.edgeGlowCornerRadiusDp
            prefs[Keys.HAS_INITIALIZED_NOTESPACE_GESTURE] = true
        }
    }

    suspend fun updateWeather(weatherText: String, lastUpdated: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.WEATHER_TEXT] = weatherText
            prefs[Keys.WEATHER_LAST_UPDATED] = lastUpdated
        }
    }

    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> safeValueOf(value: String?, default: T): T {
        if (value == null) return default
        return try {
            enumValueOf<T>(value)
        } catch (e: Exception) {
            default
        }
    }
}
