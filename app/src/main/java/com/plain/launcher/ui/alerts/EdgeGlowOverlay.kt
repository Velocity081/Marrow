package com.plain.launcher.ui.alerts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun EdgeGlowOverlay(
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFFFFD600),
    cornerRadiusDp: Int = 32,
    totalDurationMs: Long = 2300L,
    onAnimationEnd: (() -> Unit)? = null
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(glowColor, cornerRadiusDp) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = LinearEasing)
        )
        alphaAnim.animateTo(
            targetValue = 0.72f,
            animationSpec = tween(durationMillis = 320, easing = LinearEasing)
        )
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = LinearEasing)
        )
        val elapsed = 350L + 320L + 320L
        val holdTime = (totalDurationMs - elapsed - 400L).coerceAtLeast(0L)
        delay(holdTime)
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, easing = LinearEasing)
        )
        onAnimationEnd?.invoke()
    }

    val currentAlpha = alphaAnim.value

    if (currentAlpha > 0.001f) {
        Canvas(
            modifier = modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val radiusPx = cornerRadiusDp.dp.toPx()
            val strokePx = 2.dp.toPx()
            val glowPx = 8.dp.toPx()

            // 1. Draw smooth inward gradient glow layers (concentric rounded rects)
            val glowSteps = 8
            for (i in 0 until glowSteps) {
                val fraction = i.toFloat() / glowSteps
                val currentStroke = strokePx + fraction * glowPx
                val inset = currentStroke / 2f
                val layerAlpha = (currentAlpha * 0.55f * (1f - fraction) * (1f - fraction) * 0.22f).coerceIn(0f, 1f)

                val r = (radiusPx - inset).coerceAtLeast(0f)
                drawRoundRect(
                    color = glowColor.copy(alpha = layerAlpha),
                    topLeft = Offset(inset, inset),
                    size = Size(w - 2 * inset, h - 2 * inset),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = currentStroke)
                )
            }

            // 2. Draw crisp rim line on top
            val r = (radiusPx - strokePx / 2f).coerceAtLeast(0f)
            drawRoundRect(
                color = glowColor.copy(alpha = currentAlpha),
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(w - strokePx, h - strokePx),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = strokePx)
            )
        }
    }
}
