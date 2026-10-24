package com.plain.launcher.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperUtil {

    suspend fun setSolidWallpaper(
        context: Context,
        color: Int = Color.BLACK,
        homeOnly: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context.applicationContext)
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(color)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val which = if (homeOnly) {
                    WallpaperManager.FLAG_SYSTEM
                } else {
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(bitmap, null, true, which)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }
}
