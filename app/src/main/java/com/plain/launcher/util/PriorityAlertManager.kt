package com.plain.launcher.util

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import com.plain.launcher.ui.alerts.EdgeGlowOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object PriorityAlertManager {
    var isMainActivityResumed: Boolean = false

    private val _isEdgeGlowActive = MutableStateFlow(false)
    val isEdgeGlowActive = _isEdgeGlowActive.asStateFlow()

    private val _activeGlowColor = MutableStateFlow(0xFFFFD600L)
    val activeGlowColor = _activeGlowColor.asStateFlow()

    private val _activeCornerRadiusDp = MutableStateFlow(32)
    val activeCornerRadiusDp = _activeCornerRadiusDp.asStateFlow()

    private var activeOverlayView: EdgeGlowOverlayView? = null

    fun triggerTorch(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    try {
                        cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } catch (_: Exception) {
                        false
                    }
                } ?: cameraManager.cameraIdList.firstOrNull() ?: return@launch

                for (i in 0 until 3) {
                    cameraManager.setTorchMode(cameraId, true)
                    delay(140)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(120)
                }
            } catch (_: Exception) {}
        }
    }

    fun stopEdgeGlow() {
        _isEdgeGlowActive.value = false
    }

    fun triggerEdgeGlow(context: Context, color: Long = 0xFFFFD600L, cornerRadiusDp: Int? = null) {
        val appContext = context.applicationContext

        _activeGlowColor.value = color
        cornerRadiusDp?.let { _activeCornerRadiusDp.value = it }

        // 1. In Launcher: trigger Compose overlay
        _isEdgeGlowActive.value = true
        CoroutineScope(Dispatchers.Main).launch {
            delay(2400)
            _isEdgeGlowActive.value = false
        }

        // 2. Outside Launcher: show floating touch-through WindowManager overlay
        if (!isMainActivityResumed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(appContext)) {
            Handler(Looper.getMainLooper()).post {
                try {
                    val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                    activeOverlayView?.let {
                        try { windowManager.removeView(it) } catch (_: Exception) {}
                        activeOverlayView = null
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        } else {
                            @Suppress("DEPRECATION")
                            WindowManager.LayoutParams.TYPE_PHONE
                        },
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    }

                    val view = EdgeGlowOverlayView(
                        context = appContext,
                        glowColorLong = color,
                        cornerRadiusDp = cornerRadiusDp ?: _activeCornerRadiusDp.value
                    ) {
                        try {
                            windowManager.removeView(activeOverlayView)
                        } catch (_: Exception) {}
                        activeOverlayView = null
                    }
                    activeOverlayView = view
                    windowManager.addView(view, params)
                } catch (_: Exception) {}
            }
        }
    }
}
