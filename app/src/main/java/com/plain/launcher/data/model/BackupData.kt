package com.plain.launcher.data.model

import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val version: Int = 1,
    val settings: UserSettings = UserSettings(),
    val renames: Map<String, String> = emptyMap(),
    val favorites: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
    val launchCounts: Map<String, Int> = emptyMap()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)

        val sObj = JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("selectedFont", settings.selectedFont.name)
            put("backgroundGray", settings.backgroundGray.toDouble())
            put("backgroundBrightness", settings.backgroundBrightness.toDouble())
            put("blackSystemWallpaper", settings.blackSystemWallpaper)
            put("textGray", settings.textGray.toDouble())
            put("fontSizeSp", settings.fontSizeSp)
            put("fontWeight", settings.fontWeight.name)
            put("letterSpacingEm", settings.letterSpacingEm.toDouble())
            put("lineSpacingMultiplier", settings.lineSpacingMultiplier.toDouble())
            put("itemSpacingDp", settings.itemSpacingDp)
            put("textAlign", settings.textAlign.name)
            put("verticalAlign", settings.verticalAlign.name)
            put("textCase", settings.textCase.name)
            put("showAppIcons", settings.showAppIcons)
            put("enableHapticFeedback", settings.enableHapticFeedback)
            put("enableAnimations", settings.enableAnimations)
            put("showClock", settings.showClock)
            put("showDate", settings.showDate)
            put("showWeather", settings.showWeather)
            put("showBattery", settings.showBattery)
            put("enableWebLookup", settings.enableWebLookup)
            put("weatherText", settings.weatherText)
            put("weatherCity", settings.weatherCity)
            put("weatherUnit", settings.weatherUnit)
            put("weatherLastUpdated", settings.weatherLastUpdated)
            put("clock24Hour", settings.clock24Hour)
            put("clockSizeSp", settings.clockSizeSp)
            put("dateWidgetSizeSp", settings.dateWidgetSizeSp)
            put("clockAlignment", settings.clockAlignment.name)
            put("dateFormat", settings.dateFormat)
            put("maxFavoritesCount", settings.maxFavoritesCount)
            put("drawerSortOrder", settings.drawerSortOrder.name)
            put("showStatusBar", settings.showStatusBar)
            put("showNavBar", settings.showNavBar)
            put("screenOrientation", settings.screenOrientation.name)
            put("autoOpenKeyboardInSearch", settings.autoOpenKeyboardInSearch)
            put("autoLaunchSingleSearchResult", settings.autoLaunchSingleSearchResult)
            put("gestureSwipeUp", settings.gestureSwipeUp.name)
            put("gestureSwipeDown", settings.gestureSwipeDown.name)
            put("gestureSwipeLeft", settings.gestureSwipeLeft.name)
            put("gestureSwipeRight", settings.gestureSwipeRight.name)
            put("gestureDoubleTap", settings.gestureDoubleTap.name)

            val customAppsObj = JSONObject()
            settings.gestureCustomApps.forEach { (k, v) ->
                customAppsObj.put(k, v)
            }
            put("gestureCustomApps", customAppsObj)
        }
        root.put("settings", sObj)

        val renamesObj = JSONObject()
        renames.forEach { (k, v) -> renamesObj.put(k, v) }
        root.put("renames", renamesObj)

        val favsArr = JSONArray()
        favorites.forEach { favsArr.put(it) }
        root.put("favorites", favsArr)

        val hiddenArr = JSONArray()
        hidden.forEach { hiddenArr.put(it) }
        root.put("hidden", hiddenArr)

        val countsObj = JSONObject()
        launchCounts.forEach { (k, v) -> countsObj.put(k, v) }
        root.put("launchCounts", countsObj)

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): BackupData {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)

            val settings = if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val customAppsMap = mutableMapOf<String, String>()
                if (sObj.has("gestureCustomApps")) {
                    val caObj = sObj.getJSONObject("gestureCustomApps")
                    caObj.keys().forEach { k ->
                        customAppsMap[k] = caObj.getString(k)
                    }
                }

                UserSettings(
                    themeMode = safeValueOf(sObj.optString("themeMode"), UserSettings().themeMode),
                    selectedFont = safeValueOf(sObj.optString("selectedFont"), UserSettings().selectedFont),
                    backgroundGray = sObj.optDouble("backgroundGray", UserSettings().backgroundGray.toDouble()).toFloat(),
                    backgroundBrightness = sObj.optDouble("backgroundBrightness", UserSettings().backgroundBrightness.toDouble()).toFloat(),
                    blackSystemWallpaper = sObj.optBoolean("blackSystemWallpaper", UserSettings().blackSystemWallpaper),
                    textGray = sObj.optDouble("textGray", UserSettings().textGray.toDouble()).toFloat(),
                    fontSizeSp = sObj.optInt("fontSizeSp", UserSettings().fontSizeSp),
                    fontWeight = safeValueOf(sObj.optString("fontWeight"), UserSettings().fontWeight),
                    letterSpacingEm = sObj.optDouble("letterSpacingEm", UserSettings().letterSpacingEm.toDouble()).toFloat(),
                    lineSpacingMultiplier = sObj.optDouble("lineSpacingMultiplier", UserSettings().lineSpacingMultiplier.toDouble()).toFloat(),
                    itemSpacingDp = sObj.optInt("itemSpacingDp", UserSettings().itemSpacingDp),
                    textAlign = safeValueOf(sObj.optString("textAlign"), UserSettings().textAlign),
                    verticalAlign = safeValueOf(sObj.optString("verticalAlign"), UserSettings().verticalAlign),
                    textCase = safeValueOf(sObj.optString("textCase"), UserSettings().textCase),
                    showAppIcons = sObj.optBoolean("showAppIcons", UserSettings().showAppIcons),
                    enableHapticFeedback = sObj.optBoolean("enableHapticFeedback", UserSettings().enableHapticFeedback),
                    enableAnimations = sObj.optBoolean("enableAnimations", UserSettings().enableAnimations),
                    showClock = sObj.optBoolean("showClock", UserSettings().showClock),
                    showDate = sObj.optBoolean("showDate", UserSettings().showDate),
                    showWeather = sObj.optBoolean("showWeather", UserSettings().showWeather),
                    showBattery = sObj.optBoolean("showBattery", UserSettings().showBattery),
                    enableWebLookup = sObj.optBoolean("enableWebLookup", UserSettings().enableWebLookup),
                    weatherText = sObj.optString("weatherText", UserSettings().weatherText),
                    weatherCity = sObj.optString("weatherCity", UserSettings().weatherCity),
                    weatherUnit = sObj.optString("weatherUnit", UserSettings().weatherUnit),
                    weatherLastUpdated = sObj.optLong("weatherLastUpdated", UserSettings().weatherLastUpdated),
                    clock24Hour = sObj.optBoolean("clock24Hour", UserSettings().clock24Hour),
                    clockSizeSp = sObj.optInt("clockSizeSp", UserSettings().clockSizeSp),
                    dateWidgetSizeSp = sObj.optInt("dateWidgetSizeSp", UserSettings().dateWidgetSizeSp),
                    clockAlignment = safeValueOf(sObj.optString("clockAlignment"), UserSettings().clockAlignment),
                    dateFormat = sObj.optString("dateFormat", UserSettings().dateFormat),
                    maxFavoritesCount = sObj.optInt("maxFavoritesCount", UserSettings().maxFavoritesCount),
                    drawerSortOrder = safeValueOf(sObj.optString("drawerSortOrder"), UserSettings().drawerSortOrder),
                    showStatusBar = sObj.optBoolean("showStatusBar", UserSettings().showStatusBar),
                    showNavBar = sObj.optBoolean("showNavBar", UserSettings().showNavBar),
                    screenOrientation = safeValueOf(sObj.optString("screenOrientation"), UserSettings().screenOrientation),
                    autoOpenKeyboardInSearch = sObj.optBoolean("autoOpenKeyboardInSearch", UserSettings().autoOpenKeyboardInSearch),
                    autoLaunchSingleSearchResult = sObj.optBoolean("autoLaunchSingleSearchResult", UserSettings().autoLaunchSingleSearchResult),
                    gestureSwipeUp = safeValueOf(sObj.optString("gestureSwipeUp"), UserSettings().gestureSwipeUp),
                    gestureSwipeDown = safeValueOf(sObj.optString("gestureSwipeDown"), UserSettings().gestureSwipeDown),
                    gestureSwipeLeft = safeValueOf(sObj.optString("gestureSwipeLeft"), UserSettings().gestureSwipeLeft),
                    gestureSwipeRight = safeValueOf(sObj.optString("gestureSwipeRight"), UserSettings().gestureSwipeRight),
                    gestureDoubleTap = safeValueOf(sObj.optString("gestureDoubleTap"), UserSettings().gestureDoubleTap),
                    gestureCustomApps = customAppsMap
                )
            } else {
                UserSettings()
            }

            val renames = mutableMapOf<String, String>()
            if (root.has("renames")) {
                val rObj = root.getJSONObject("renames")
                rObj.keys().forEach { k -> renames[k] = rObj.getString(k) }
            }

            val favorites = mutableListOf<String>()
            if (root.has("favorites")) {
                val fArr = root.getJSONArray("favorites")
                for (i in 0 until fArr.length()) {
                    favorites.add(fArr.getString(i))
                }
            }

            val hidden = mutableListOf<String>()
            if (root.has("hidden")) {
                val hArr = root.getJSONArray("hidden")
                for (i in 0 until hArr.length()) {
                    hidden.add(hArr.getString(i))
                }
            }

            val launchCounts = mutableMapOf<String, Int>()
            if (root.has("launchCounts")) {
                val lcObj = root.getJSONObject("launchCounts")
                lcObj.keys().forEach { k -> launchCounts[k] = lcObj.getInt(k) }
            }

            return BackupData(
                version = version,
                settings = settings,
                renames = renames,
                favorites = favorites,
                hidden = hidden,
                launchCounts = launchCounts
            )
        }

        private inline fun <reified T : Enum<T>> safeValueOf(value: String, default: T): T {
            return try {
                enumValueOf<T>(value)
            } catch (e: Exception) {
                default
            }
        }
    }
}
