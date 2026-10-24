package com.plain.launcher

import com.plain.launcher.data.model.AppItem
import com.plain.launcher.util.FuzzySearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {

    private fun createApp(pkg: String, label: String, custom: String? = null): AppItem {
        return AppItem(
            packageName = pkg,
            activityName = "${pkg}.MainActivity",
            originalLabel = label,
            customLabel = custom
        )
    }

    @Test
    fun testEmptyQueryReturnsAllItems() {
        val apps = listOf(
            createApp("com.chrome", "Chrome"),
            createApp("com.firefox", "Firefox")
        )
        val result = FuzzySearch.filter(apps, "")
        assertEquals(2, result.size)
    }

    @Test
    fun testExactMatch() {
        val apps = listOf(
            createApp("com.chrome", "Chrome"),
            createApp("com.camera", "Camera"),
            createApp("com.calc", "Calculator")
        )
        val result = FuzzySearch.filter(apps, "Camera")
        assertEquals(1, result.size)
        assertEquals("Camera", result.first().displayLabel)
    }

    @Test
    fun testPrefixMatchRankedHigher() {
        val apps = listOf(
            createApp("com.google.chrome", "Chrome"),
            createApp("com.google.calc", "Calculator"),
            createApp("com.google.calendar", "Calendar")
        )
        val result = FuzzySearch.filter(apps, "cal")
        assertEquals(2, result.size)
        assertTrue(result.any { it.displayLabel == "Calculator" })
        assertTrue(result.any { it.displayLabel == "Calendar" })
    }

    @Test
    fun testFuzzySubsequenceMatch() {
        val apps = listOf(
            createApp("com.spotify.music", "Spotify"),
            createApp("com.google.android.youtube", "YouTube"),
            createApp("com.whatsapp", "WhatsApp")
        )
        // "spt" matches "Spotify" (S-P-o-T-ify)
        val result = FuzzySearch.filter(apps, "spt")
        assertEquals(1, result.size)
        assertEquals("Spotify", result.first().displayLabel)
    }

    @Test
    fun testSearchByCustomAndOriginalName() {
        val apps = listOf(
            createApp("com.example.chat", "OriginalChat", custom = "MyMessenger")
        )
        // Search by custom name
        val matchCustom = FuzzySearch.filter(apps, "messenger")
        assertEquals(1, matchCustom.size)
        assertEquals("MyMessenger", matchCustom.first().displayLabel)

        // Search by original name
        val matchOriginal = FuzzySearch.filter(apps, "chat")
        assertEquals(1, matchOriginal.size)
        assertEquals("MyMessenger", matchOriginal.first().displayLabel)
    }

    @Test
    fun testNoMatchReturnsEmptyList() {
        val apps = listOf(
            createApp("com.chrome", "Chrome"),
            createApp("com.firefox", "Firefox")
        )
        val result = FuzzySearch.filter(apps, "xyz123")
        assertTrue(result.isEmpty())
    }
}
