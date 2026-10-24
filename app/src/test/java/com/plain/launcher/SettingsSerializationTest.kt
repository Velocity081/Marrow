package com.plain.launcher

import com.plain.launcher.data.model.AppFontWeight
import com.plain.launcher.data.model.BackupData
import com.plain.launcher.data.model.ThemeMode
import com.plain.launcher.data.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsSerializationTest {

    @Test
    fun testDefaultSettingsSerialization() {
        val defaults = UserSettings()
        val backup = BackupData(settings = defaults)
        val json = backup.toJson()
        val restored = BackupData.fromJson(json)

        assertEquals(defaults.themeMode, restored.settings.themeMode)
        assertEquals(defaults.fontSizeSp, restored.settings.fontSizeSp)
        assertEquals(defaults.fontWeight, restored.settings.fontWeight)
        assertEquals(defaults.showClock, restored.settings.showClock)
    }

    @Test
    fun testCorruptedJsonFallbackDefaults() {
        val malformedJson = """
            {
               "version": 1,
               "settings": {
                   "themeMode": "NON_EXISTENT_THEME",
                   "fontWeight": "INVALID_WEIGHT",
                   "fontSizeSp": 22
               }
            }
        """.trimIndent()

        val restored = BackupData.fromJson(malformedJson)
        assertNotNull(restored)
        // Should fallback gracefully to default enum values
        assertEquals(ThemeMode.DARK, restored.settings.themeMode)
        assertEquals(AppFontWeight.REGULAR, restored.settings.fontWeight)
        assertEquals(22, restored.settings.fontSizeSp)
    }

    @Test
    fun testEmptyJsonReturnsSafeDefaults() {
        val emptyJson = "{}"
        val restored = BackupData.fromJson(emptyJson)
        assertNotNull(restored)
        assertEquals(1, restored.version)
        assertEquals(UserSettings(), restored.settings)
        assertEquals(emptyMap<String, String>(), restored.renames)
        assertEquals(emptyList<String>(), restored.favorites)
        assertEquals(emptyList<String>(), restored.hidden)
    }
}
