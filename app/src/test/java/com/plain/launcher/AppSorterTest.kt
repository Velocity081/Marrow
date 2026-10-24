package com.plain.launcher

import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.DrawerSortOrder
import com.plain.launcher.util.AppSorter
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSorterTest {

    private fun createApp(
        pkg: String,
        label: String,
        launchCount: Int = 0,
        installTime: Long = 0L
    ): AppItem {
        return AppItem(
            packageName = pkg,
            activityName = "${pkg}.MainActivity",
            originalLabel = label,
            launchCount = launchCount,
            installTime = installTime
        )
    }

    @Test
    fun testSortAZ() {
        val apps = listOf(
            createApp("com.z", "Zebra"),
            createApp("com.a", "Apple"),
            createApp("com.m", "Mango")
        )
        val sorted = AppSorter.sort(apps, DrawerSortOrder.AZ)
        assertEquals("Apple", sorted[0].displayLabel)
        assertEquals("Mango", sorted[1].displayLabel)
        assertEquals("Zebra", sorted[2].displayLabel)
    }

    @Test
    fun testSortZA() {
        val apps = listOf(
            createApp("com.z", "Zebra"),
            createApp("com.a", "Apple"),
            createApp("com.m", "Mango")
        )
        val sorted = AppSorter.sort(apps, DrawerSortOrder.ZA)
        assertEquals("Zebra", sorted[0].displayLabel)
        assertEquals("Mango", sorted[1].displayLabel)
        assertEquals("Apple", sorted[2].displayLabel)
    }

    @Test
    fun testSortMostUsed() {
        val apps = listOf(
            createApp("com.a", "AppA", launchCount = 5),
            createApp("com.b", "AppB", launchCount = 42),
            createApp("com.c", "AppC", launchCount = 10)
        )
        val sorted = AppSorter.sort(apps, DrawerSortOrder.MOST_USED)
        assertEquals("AppB", sorted[0].displayLabel)
        assertEquals("AppC", sorted[1].displayLabel)
        assertEquals("AppA", sorted[2].displayLabel)
    }

    @Test
    fun testSortRecentlyInstalled() {
        val apps = listOf(
            createApp("com.a", "OldApp", installTime = 1000L),
            createApp("com.b", "NewestApp", installTime = 5000L),
            createApp("com.c", "MiddleApp", installTime = 3000L)
        )
        val sorted = AppSorter.sort(apps, DrawerSortOrder.RECENTLY_INSTALLED)
        assertEquals("NewestApp", sorted[0].displayLabel)
        assertEquals("MiddleApp", sorted[1].displayLabel)
        assertEquals("OldApp", sorted[2].displayLabel)
    }
}
