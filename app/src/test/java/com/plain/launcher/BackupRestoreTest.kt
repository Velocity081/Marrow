package com.plain.launcher

import com.plain.launcher.data.model.AppFontWeight
import com.plain.launcher.data.model.AppOrientation
import com.plain.launcher.data.model.AppTextAlign
import com.plain.launcher.data.model.AppTextCase
import com.plain.launcher.data.model.AppVerticalAlign
import com.plain.launcher.data.model.BackupData
import com.plain.launcher.data.model.DrawerSortOrder
import com.plain.launcher.data.model.GestureAction
import com.plain.launcher.data.model.ThemeMode
import com.plain.launcher.data.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRestoreTest {

    @Test
    fun testBackupJsonRoundTrip() {
        val originalSettings = UserSettings(
            themeMode = ThemeMode.AMOLED,
            backgroundGray = 0.0f,
            blackSystemWallpaper = true,
            textGray = 0.88f,
            fontSizeSp = 20,
            fontWeight = AppFontWeight.BOLD,
            letterSpacingEm = 0.05f,
            lineSpacingMultiplier = 1.4f,
            itemSpacingDp = 16,
            textAlign = AppTextAlign.CENTER,
            verticalAlign = AppVerticalAlign.CENTER,
            textCase = AppTextCase.UPPERCASE,
            showClock = true,
            showDate = false,
            clock24Hour = true,
            clockSizeSp = 36,
            dateWidgetSizeSp = 18,
            clockAlignment = AppTextAlign.CENTER,
            dateFormat = "yyyy-MM-dd",
            maxFavoritesCount = 12,
            drawerSortOrder = DrawerSortOrder.MOST_USED,
            showStatusBar = false,
            showNavBar = true,
            screenOrientation = AppOrientation.PORTRAIT,
            autoOpenKeyboardInSearch = false,
            autoLaunchSingleSearchResult = true,
            gestureSwipeUp = GestureAction.OPEN_SEARCH,
            gestureSwipeDown = GestureAction.OPEN_SETTINGS,
            gestureSwipeLeft = GestureAction.LOCK_SCREEN,
            gestureSwipeRight = GestureAction.OPEN_APP,
            gestureDoubleTap = GestureAction.LOCK_SCREEN,
            gestureCustomApps = mapOf("swipe_right" to "com.example.custom")
        )

        val originalBackup = BackupData(
            version = 1,
            settings = originalSettings,
            renames = mapOf("com.pkg1/Act#0" to "My Browser", "com.pkg2/Act#0" to "Terminal"),
            favorites = listOf("com.pkg1/Act#0", "com.pkg2/Act#0"),
            hidden = listOf("com.bloatware/Act#0"),
            launchCounts = mapOf("com.pkg1/Act#0" to 42, "com.pkg2/Act#0" to 99)
        )

        val jsonString = originalBackup.toJson()
        val restoredBackup = BackupData.fromJson(jsonString)

        assertEquals(originalBackup.version, restoredBackup.version)
        assertEquals(originalBackup.renames, restoredBackup.renames)
        assertEquals(originalBackup.favorites, restoredBackup.favorites)
        assertEquals(originalBackup.hidden, restoredBackup.hidden)
        assertEquals(originalBackup.launchCounts, restoredBackup.launchCounts)

        // Compare settings fields
        val s1 = originalBackup.settings
        val s2 = restoredBackup.settings
        assertEquals(s1.themeMode, s2.themeMode)
        assertEquals(s1.backgroundGray, s2.backgroundGray, 0.001f)
        assertEquals(s1.blackSystemWallpaper, s2.blackSystemWallpaper)
        assertEquals(s1.textGray, s2.textGray, 0.001f)
        assertEquals(s1.fontSizeSp, s2.fontSizeSp)
        assertEquals(s1.fontWeight, s2.fontWeight)
        assertEquals(s1.letterSpacingEm, s2.letterSpacingEm, 0.001f)
        assertEquals(s1.lineSpacingMultiplier, s2.lineSpacingMultiplier, 0.001f)
        assertEquals(s1.itemSpacingDp, s2.itemSpacingDp)
        assertEquals(s1.textAlign, s2.textAlign)
        assertEquals(s1.verticalAlign, s2.verticalAlign)
        assertEquals(s1.textCase, s2.textCase)
        assertEquals(s1.showClock, s2.showClock)
        assertEquals(s1.showDate, s2.showDate)
        assertEquals(s1.clock24Hour, s2.clock24Hour)
        assertEquals(s1.clockSizeSp, s2.clockSizeSp)
        assertEquals(s1.dateWidgetSizeSp, s2.dateWidgetSizeSp)
        assertEquals(s1.clockAlignment, s2.clockAlignment)
        assertEquals(s1.dateFormat, s2.dateFormat)
        assertEquals(s1.maxFavoritesCount, s2.maxFavoritesCount)
        assertEquals(s1.drawerSortOrder, s2.drawerSortOrder)
        assertEquals(s1.showStatusBar, s2.showStatusBar)
        assertEquals(s1.showNavBar, s2.showNavBar)
        assertEquals(s1.screenOrientation, s2.screenOrientation)
        assertEquals(s1.autoOpenKeyboardInSearch, s2.autoOpenKeyboardInSearch)
        assertEquals(s1.autoLaunchSingleSearchResult, s2.autoLaunchSingleSearchResult)
        assertEquals(s1.gestureSwipeUp, s2.gestureSwipeUp)
        assertEquals(s1.gestureSwipeDown, s2.gestureSwipeDown)
        assertEquals(s1.gestureSwipeLeft, s2.gestureSwipeLeft)
        assertEquals(s1.gestureSwipeRight, s2.gestureSwipeRight)
        assertEquals(s1.gestureDoubleTap, s2.gestureDoubleTap)
        assertEquals(s1.gestureCustomApps, s2.gestureCustomApps)
    }
}
