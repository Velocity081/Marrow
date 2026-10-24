package com.plain.launcher

import com.plain.launcher.util.ContrastCalculator
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastCalculatorTest {

    @Test
    fun testBlackAndWhiteContrast() {
        val contrast = ContrastCalculator.calculateContrast(0.0f, 1.0f)
        // Theoretical max is 21.0
        assertTrue("Expected contrast ~21.0, got $contrast", contrast in 20.9f..21.1f)
    }

    @Test
    fun testIdenticalGraysContrast() {
        val contrast = ContrastCalculator.calculateContrast(0.5f, 0.5f)
        assertTrue("Expected contrast 1.0, got $contrast", contrast in 0.99f..1.01f)
    }

    @Test
    fun testClampOnDarkBackgroundMaintainsMinimum45() {
        val darkBg = 0.05f
        val clampedText = ContrastCalculator.clampTextGray(
            backgroundGray = darkBg,
            desiredTextGray = 0.10f, // Would fail contrast without clamp
            minContrast = 4.5f
        )
        val actualContrast = ContrastCalculator.calculateContrast(darkBg, clampedText)
        assertTrue(
            "Expected contrast >= 4.5, got $actualContrast with textGray=$clampedText",
            actualContrast >= 4.49f
        )
    }

    @Test
    fun testClampOnLightBackgroundMaintainsMinimum45() {
        val lightBg = 0.95f
        val clampedText = ContrastCalculator.clampTextGray(
            backgroundGray = lightBg,
            desiredTextGray = 0.90f, // Would fail contrast without clamp
            minContrast = 4.5f
        )
        val actualContrast = ContrastCalculator.calculateContrast(lightBg, clampedText)
        assertTrue(
            "Expected contrast >= 4.5, got $actualContrast with textGray=$clampedText",
            actualContrast >= 4.49f
        )
    }

    @Test
    fun testClampPreservesSufficientContrast() {
        val darkBg = 0.0f
        val highContrastText = 1.0f
        val clamped = ContrastCalculator.clampTextGray(darkBg, highContrastText, 4.5f)
        assertTrue("Expected 1.0f preserved, got $clamped", clamped in 0.99f..1.0f)
    }
}
