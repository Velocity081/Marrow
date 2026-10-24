package com.plain.launcher.data.repository

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.plain.launcher.data.local.AppCustomizationStore
import com.plain.launcher.data.local.AppCustomizations
import com.plain.launcher.data.local.SettingsDataStore
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.BackupData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    val customizationStore: AppCustomizationStore,
    val settingsStore: SettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager

    private val rawAppsFlow = MutableStateFlow<List<RawAppInfo>>(emptyList())

    private data class RawAppInfo(
        val packageName: String,
        val activityName: String,
        val originalLabel: String,
        val userHandle: UserHandle,
        val userSerial: Long,
        val isWorkProfile: Boolean,
        val installTime: Long
    ) {
        val key: String get() = "$packageName/$activityName#$userSerial"
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            reloadApps()
        }

        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            reloadApps()
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            reloadApps()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            reloadApps()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            reloadApps()
        }
    }

    init {
        try {
            launcherApps?.registerCallback(launcherAppsCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            // ignore
        }
        reloadApps()
    }

    fun reloadApps() {
        scope.launch {
            val list = queryInstalledApps()
            rawAppsFlow.value = list
        }
    }

    private suspend fun queryInstalledApps(): List<RawAppInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RawAppInfo>()
        if (launcherApps == null || userManager == null) return@withContext result

        val myUserHandle = Process.myUserHandle()
        val profiles = try {
            userManager.userProfiles
        } catch (e: Exception) {
            listOf(myUserHandle)
        }

        for (profile in profiles) {
            val isWorkProfile = (profile != myUserHandle)
            val userSerial = try {
                userManager.getSerialNumberForUser(profile)
            } catch (e: Exception) {
                0L
            }

            val activityList = try {
                launcherApps.getActivityList(null, profile)
            } catch (e: Exception) {
                emptyList<LauncherActivityInfo>()
            }

            for (activityInfo in activityList) {
                val pkgName = activityInfo.applicationInfo.packageName
                // Don't show plain itself in the launcher list
                if (pkgName == context.packageName) continue

                val installTime = try {
                    context.packageManager.getPackageInfo(pkgName, 0).firstInstallTime
                } catch (e: Exception) {
                    0L
                }

                result.add(
                    RawAppInfo(
                        packageName = pkgName,
                        activityName = activityInfo.name,
                        originalLabel = activityInfo.label.toString(),
                        userHandle = profile,
                        userSerial = userSerial,
                        isWorkProfile = isWorkProfile,
                        installTime = installTime
                    )
                )
            }
        }

        result
    }

    val allAppsFlow: Flow<List<AppItem>> = combine(
        rawAppsFlow,
        customizationStore.customizationsFlow
    ) { rawList, customizations ->
        rawList.map { raw ->
            val key = raw.key
            val isFav = customizations.favorites.contains(key)
            val favOrder = if (isFav) customizations.favorites.indexOf(key) else Int.MAX_VALUE

            AppItem(
                packageName = raw.packageName,
                activityName = raw.activityName,
                originalLabel = raw.originalLabel,
                customLabel = customizations.renames[key],
                userHandle = raw.userHandle,
                userSerial = raw.userSerial,
                isWorkProfile = raw.isWorkProfile,
                isFavorite = isFav,
                favoriteOrder = favOrder,
                isHidden = customizations.hidden.contains(key),
                launchCount = customizations.launchCounts[key] ?: 0,
                installTime = raw.installTime
            )
        }
    }

    suspend fun togglePin(appItem: AppItem) {
        customizationStore.togglePin(appItem.key)
    }

    suspend fun setRename(appItem: AppItem, newName: String?) {
        customizationStore.setRename(appItem.key, newName)
    }

    suspend fun setHidden(appItem: AppItem, hide: Boolean) {
        customizationStore.setHidden(appItem.key, hide)
    }

    suspend fun setHiddenByKey(key: String, hide: Boolean) {
        customizationStore.setHidden(key, hide)
    }

    suspend fun reorderFavorites(newOrder: List<String>) {
        customizationStore.setFavoritesOrder(newOrder)
    }

    suspend fun recordLaunch(appItem: AppItem) {
        customizationStore.incrementLaunchCount(appItem.key)
    }

    suspend fun exportBackup(): BackupData {
        val currentSettings = settingsStore.settingsFlow.first()
        val currentCustomizations = customizationStore.customizationsFlow.first()

        return BackupData(
            version = 1,
            settings = currentSettings,
            renames = currentCustomizations.renames,
            favorites = currentCustomizations.favorites,
            hidden = currentCustomizations.hidden.toList(),
            launchCounts = currentCustomizations.launchCounts
        )
    }

    suspend fun importBackup(backup: BackupData) {
        settingsStore.updateSettings(backup.settings)
        customizationStore.importCustomizations(
            renames = backup.renames,
            favorites = backup.favorites,
            hidden = backup.hidden,
            launchCounts = backup.launchCounts
        )
    }

    suspend fun updateWeather(weatherText: String, lastUpdated: Long) {
        settingsStore.updateWeather(weatherText, lastUpdated)
    }

    suspend fun resetAll() {
        settingsStore.resetToDefaults()
        customizationStore.resetAll()
    }
}
