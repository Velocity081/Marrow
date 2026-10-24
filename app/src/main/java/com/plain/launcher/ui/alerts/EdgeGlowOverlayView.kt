package com.plain.launcher.ui.alerts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

class EdgeGlowOverlayView(
    context: Context,
    private val glowColorLong: Long = 0xFFFFD600L,
    private val cornerRadiusDp: Int = 32,
    private val onFinished: () -> Unit
) : View(context) {

    private val strokePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics
    )
    private val glowPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
    )
    private val radiusPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp.toFloat(), resources.displayMetrics
    )

    private var currentAlpha = 0f

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val rectF = RectF()

    init {
        startGlowAnimation()
    }

    private fun startGlowAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f, 0.72f, 1f, 1f, 0f).apply {
            duration = 2300L
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                currentAlpha = va.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onFinished()
                }
            })
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentAlpha <= 0.001f) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val baseRed = (glowColorLong shr 16 and 0xFF).toInt()
        val baseGreen = (glowColorLong shr 8 and 0xFF).toInt()
        val baseBlue = (glowColorLong and 0xFF).toInt()

        // 1. Draw soft inward gradient glow layers (concentric rounded rects)
        val glowSteps = 8
        for (i in 0 until glowSteps) {
            val fraction = i.toFloat() / glowSteps
            val currentStroke = strokePx + fraction * glowPx
            val inset = currentStroke / 2f
            val layerAlpha = (currentAlpha * 0.55f * (1f - fraction) * (1f - fraction) * 0.22f).coerceIn(0f, 1f)
            val alphaInt = (layerAlpha * 255).toInt().coerceIn(0, 255)

            strokePaint.color = Color.argb(alphaInt, baseRed, baseGreen, baseBlue)
            strokePaint.strokeWidth = currentStroke

            val r = (radiusPx - inset).coerceAtLeast(0f)
            rectF.set(inset, inset, w - inset, h - inset)
            canvas.drawRoundRect(rectF, r, r, strokePaint)
        }

        // 2. Draw crisp rim line on top
        val lineAlphaInt = (currentAlpha * 255).toInt().coerceIn(0, 255)
        strokePaint.color = Color.argb(lineAlphaInt, baseRed, baseGreen, baseBlue)
        strokePaint.strokeWidth = strokePx

        val r = (radiusPx - strokePx / 2f).coerceAtLeast(0f)
        rectF.set(strokePx / 2f, strokePx / 2f, w - strokePx / 2f, h - strokePx / 2f)
        canvas.drawRoundRect(rectF, r, r, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }
}
