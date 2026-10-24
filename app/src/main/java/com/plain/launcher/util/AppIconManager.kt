package com.plain.launcher.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.plain.launcher.data.model.AppItem
import java.util.concurrent.ConcurrentHashMap

object AppIconManager {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val coloredCache = ConcurrentHashMap<String, ImageBitmap>()

    fun getIcon(context: Context, app: AppItem): ImageBitmap? {
        val key = app.key
        val cached = cache[key]
        if (cached != null) return cached

        return try {
            val drawable = loadDrawable(context, app) ?: return null
            val bitmap = drawableToMonochromeBitmap(drawable, 64, 64)
            val img = bitmap.asImageBitmap()
            cache[key] = img
            img
        } catch (e: Exception) {
            null
        }
    }

    fun getIconByPackage(context: Context, packageName: String): ImageBitmap? {
        val cached = cache[packageName]
        if (cached != null) return cached

        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToMonochromeBitmap(drawable, 64, 64)
            val img = bitmap.asImageBitmap()
            cache[packageName] = img
            img
        } catch (e: Exception) {
            null
        }
    }

    fun getColoredIcon(context: Context, app: AppItem): ImageBitmap? {
        val key = app.key
        val cached = coloredCache[key]
        if (cached != null) return cached

        return try {
            val drawable = loadDrawable(context, app) ?: return null
            val px = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(64)
            val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, px, px)
            drawable.draw(canvas)
            val img = bitmap.asImageBitmap()
            coloredCache[key] = img
            img
        } catch (e: Exception) {
            null
        }
    }

    private fun loadDrawable(context: Context, app: AppItem): Drawable? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        if (launcherApps != null && app.userHandle != null) {
            try {
                val activities = launcherApps.getActivityList(app.packageName, app.userHandle)
                val target = activities.firstOrNull { it.componentName.className == app.activityName }
                    ?: activities.firstOrNull()
                if (target != null) {
                    return target.getBadgedIcon(0)
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        return try {
            val component = ComponentName(app.packageName, app.activityName)
            context.packageManager.getActivityIcon(component)
        } catch (e: Exception) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun drawableToMonochromeBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        // Convert to monochrome grayscale to strictly preserve the monochrome aesthetic
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val grayCanvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        grayCanvas.drawBitmap(bitmap, 0f, 0f, paint)

        return grayscaleBitmap
    }
}
