package com.plain.launcher.util

import android.app.Activity
import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.plain.launcher.R
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.receiver.PlainDeviceAdminReceiver

object AppLauncher {

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun getFadeTransitionBundle(context: Context): Bundle {
        return ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            R.anim.launcher_stay
        ).toBundle()
    }

    private fun applyFadeTransition(context: Context) {
        findActivity(context)?.let { activity ->
            if (Build.VERSION.SDK_INT >= 34) {
                activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    android.R.anim.fade_in,
                    R.anim.launcher_stay
                )
                activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.launcher_stay,
                    android.R.anim.fade_out
                )
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(
                    android.R.anim.fade_in,
                    R.anim.launcher_stay
                )
            }
        }
    }

    fun launchApp(context: Context, appItem: AppItem): Boolean {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val opts = getFadeTransitionBundle(context)
            val success = if (launcherApps != null) {
                val component = ComponentName(appItem.packageName, appItem.activityName)
                val handle = appItem.userHandle ?: android.os.Process.myUserHandle()
                launcherApps.startMainActivity(component, handle, null, opts)
                true
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage(appItem.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent, opts)
                    true
                } else false
            }
            if (success) {
                applyFadeTransition(context)
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    fun openAppInfo(context: Context, packageName: String) {
        try {
            val opts = getFadeTransitionBundle(context)
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent, opts)
            applyFadeTransition(context)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val opts = getFadeTransitionBundle(context)
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent, opts)
            applyFadeTransition(context)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openWebSearch(context: Context, query: String) {
        val trimmed = query.trim()
        val url = if (trimmed.isNotEmpty()) {
            "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
        } else {
            "https://www.google.com"
        }
        val uri = Uri.parse(url)
        val opts = getFadeTransitionBundle(context)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent, opts)
            applyFadeTransition(context)
        } catch (e: Exception) {
            try {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, trimmed)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(searchIntent, opts)
                applyFadeTransition(context)
            } catch (e2: Exception) {
                // ignore
            }
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val comp = PlainDeviceAdminReceiver.getComponentName(context)
        return dpm.isAdminActive(comp)
    }

    fun lockScreen(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        val comp = PlainDeviceAdminReceiver.getComponentName(context)
        return if (dpm.isAdminActive(comp)) {
            dpm.lockNow()
            true
        } else {
            false
        }
    }

    fun createDeviceAdminIntent(context: Context): Intent {
        val comp = PlainDeviceAdminReceiver.getComponentName(context)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enables plain to lock the screen on double tap or gesture."
            )
        }
    }

    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun createDefaultLauncherIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
