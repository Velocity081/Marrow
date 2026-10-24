package com.plain.launcher.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ContrastCalculator {

    /**
     * Converts an sRGB color component in range [0, 1] to linear luminance.
     */
    fun sRgbToLinear(value: Float): Float {
        val clamped = value.coerceIn(0f, 1f)
        return if (clamped <= 0.04045f) {
            clamped / 12.92f
        } else {
            ((clamped + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    /**
     * Converts linear luminance back to sRGB component in range [0, 1].
     */
    fun linearToSRgb(luminance: Float): Float {
        val clamped = luminance.coerceIn(0f, 1f)
        return if (clamped <= 0.04045f / 12.92f) {
            clamped * 12.92f
        } else {
            1.055f * clamped.pow(1f / 2.4f) - 0.055f
        }.coerceIn(0f, 1f)
    }

    /**
     * Calculates the WCAG 2.1 contrast ratio between two grayscale levels [0, 1].
     * Returns a value between 1.0 and 21.0.
     */
    fun calculateContrast(gray1: Float, gray2: Float): Float {
        val l1 = sRgbToLinear(gray1)
        val l2 = sRgbToLinear(gray2)
        val lMax = max(l1, l2)
        val lMin = min(l1, l2)
        return (lMax + 0.05f) / (lMin + 0.05f)
    }

    /**
     * Clamps the desired text gray level against the given background gray level
     * so that the contrast ratio is guaranteed to be at least minContrast (default 4.5:1 for WCAG AA).
     */
    fun clampTextGray(
        backgroundGray: Float,
        desiredTextGray: Float,
        minContrast: Float = 4.5f
    ): Float {
        val bgLinear = sRgbToLinear(backgroundGray)

        // Check if dark background or light background
        val isDarkBg = bgLinear <= 0.179f // Midpoint luminance for 4.5:1 contrast against black/white

        return if (isDarkBg) {
            // Text must be brighter than background
            // (lText + 0.05) / (bgLinear + 0.05) >= minContrast
            val minTextLinear = (minContrast * (bgLinear + 0.05f) - 0.05f).coerceIn(0f, 1f)
            val minTextGray = linearToSRgb(minTextLinear)
            max(desiredTextGray, minTextGray).coerceIn(0f, 1f)
        } else {
            // Text must be darker than background
            // (bgLinear + 0.05) / (lText + 0.05) >= minContrast
            val maxTextLinear = ((bgLinear + 0.05f) / minContrast - 0.05f).coerceIn(0f, 1f)
            val maxTextGray = linearToSRgb(maxTextLinear)
            min(desiredTextGray, maxTextGray).coerceIn(0f, 1f)
        }
    }
}
