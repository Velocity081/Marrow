package com.plain.launcher

import com.plain.launcher.data.model.AppTextCase
import com.plain.launcher.util.TextTransforms
import org.junit.Assert.assertEquals
import org.junit.Test

class TextTransformsTest {

    @Test
    fun testAsIsTransform() {
        val input = "Plain Launcher 2026"
        val output = TextTransforms.applyCase(input, AppTextCase.AS_IS)
        assertEquals("Plain Launcher 2026", output)
    }

    @Test
    fun testLowercaseTransform() {
        val input = "Plain Launcher 2026"
        val output = TextTransforms.applyCase(input, AppTextCase.LOWERCASE)
        assertEquals("plain launcher 2026", output)
    }

    @Test
    fun testUppercaseTransform() {
        val input = "Plain Launcher 2026"
        val output = TextTransforms.applyCase(input, AppTextCase.UPPERCASE)
        assertEquals("PLAIN LAUNCHER 2026", output)
    }

    @Test
    fun testCapitalizedTransform() {
        val input = "plain launcher app"
        val output = TextTransforms.applyCase(input, AppTextCase.CAPITALIZED)
        assertEquals("Plain Launcher App", output)
    }
}
