package com.plain.launcher.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.BackupData
import com.plain.launcher.data.model.GestureAction
import com.plain.launcher.data.model.UserSettings
import com.plain.launcher.data.repository.AppRepository
import com.plain.launcher.data.repository.WeatherRepository
import com.plain.launcher.ui.navigation.Screen
import com.plain.launcher.util.AppLauncher
import com.plain.launcher.util.AppSorter
import com.plain.launcher.util.FuzzySearch
import com.plain.launcher.util.HapticUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainViewModel(
    private val repository: AppRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val screenStack = mutableListOf<Screen>(Screen.Home)
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val settings: StateFlow<UserSettings> = repository.settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val allApps: StateFlow<List<AppItem>> = repository.allAppsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeApps: StateFlow<List<AppItem>> = combine(
        allApps,
        settings
    ) { apps, s ->
        val visible = apps.filterNot { it.isHidden }
        val favorites = visible.filter { it.isFavorite }
            .sortedBy { it.favoriteOrder }

        if (favorites.isNotEmpty()) {
            if (s.maxFavoritesCount > 0) {
                favorites.take(s.maxFavoritesCount)
            } else {
                favorites
            }
        } else {
            // If no favourites are pinned yet, show all apps
            AppSorter.sort(visible, s.drawerSortOrder)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val drawerApps: StateFlow<List<AppItem>> = combine(
        allApps,
        settings
    ) { apps, s ->
        val visible = apps.filterNot { it.isHidden }
        AppSorter.sort(visible, s.drawerSortOrder)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<AppItem>> = combine(
        drawerApps,
        _searchQuery
    ) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            FuzzySearch.filter(apps, query)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedAppForMenu = MutableStateFlow<AppItem?>(null)
    val selectedAppForMenu: StateFlow<AppItem?> = _selectedAppForMenu.asStateFlow()

    private val _renameApp = MutableStateFlow<AppItem?>(null)
    val renameApp: StateFlow<AppItem?> = _renameApp.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _homeScrollToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeScrollToTop = _homeScrollToTop.asSharedFlow()

    private val _isDeviceAdminActive = MutableStateFlow(false)
    val isDeviceAdminActive: StateFlow<Boolean> = _isDeviceAdminActive.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    private val _weatherFetchStatus = MutableStateFlow<String?>(null)
    val weatherFetchStatus: StateFlow<String?> = _weatherFetchStatus.asStateFlow()

    init {
        viewModelScope.launch {
            val initialSettings = settings.first()
            if (initialSettings.showWeather) {
                refreshWeather(force = false)
            }
        }
    }

    fun refreshWeather(force: Boolean = false) {
        if (_isWeatherLoading.value) return
        val currentSettings = settings.value
        val now = System.currentTimeMillis()
        if (!force && (now - currentSettings.weatherLastUpdated < 15 * 60 * 1000L) && currentSettings.weatherText.isNotBlank()) {
            return
        }

        viewModelScope.launch {
            _isWeatherLoading.value = true
            _weatherFetchStatus.value = "updating weather..."
            val result = weatherRepository.fetchWeather(
                city = currentSettings.weatherCity,
                temperatureUnit = currentSettings.weatherUnit
            )
            result.onSuccess { weatherStr ->
                val now = System.currentTimeMillis()
                repository.updateWeather(weatherStr, now)
                _weatherFetchStatus.value = "updated ($weatherStr)"
            }.onFailure { err ->
                _weatherFetchStatus.value = "error: ${err.message ?: "fetch failed"}"
            }
            _isWeatherLoading.value = false
        }
    }

    private val _targetSettingsCategory = MutableStateFlow<String?>(null)
    val targetSettingsCategory: StateFlow<String?> = _targetSettingsCategory.asStateFlow()

    fun openSettingsCategory(categoryName: String) {
        _targetSettingsCategory.value = categoryName
        navigateTo(Screen.Settings)
    }

    fun clearTargetSettingsCategory() {
        _targetSettingsCategory.value = null
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = screenStack.last()
            return true
        }
        return false
    }

    fun onHomePressed() {
        if (_currentScreen.value != Screen.Home) {
            screenStack.clear()
            screenStack.add(Screen.Home)
            _currentScreen.value = Screen.Home
        }
        _homeScrollToTop.tryEmit(Unit)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAppMenu(app: AppItem) {
        _selectedAppForMenu.value = app
    }

    fun closeAppMenu() {
        _selectedAppForMenu.value = null
    }

    fun openRenameDialog(app: AppItem) {
        _renameApp.value = app
        closeAppMenu()
    }

    fun closeRenameDialog() {
        _renameApp.value = null
    }

    fun saveRename(app: AppItem, newName: String?) {
        viewModelScope.launch {
            repository.setRename(app, newName)
            closeRenameDialog()
        }
    }

    fun togglePin(app: AppItem) {
        viewModelScope.launch {
            repository.togglePin(app)
            closeAppMenu()
        }
    }

    fun setHidden(app: AppItem, hide: Boolean) {
        viewModelScope.launch {
            repository.setHidden(app, hide)
            closeAppMenu()
        }
    }

    fun unhideAppByKey(key: String) {
        viewModelScope.launch {
            repository.setHiddenByKey(key, false)
        }
    }

    fun launchApp(context: Context, app: AppItem) {
        viewModelScope.launch {
            repository.recordLaunch(app)
        }
        AppLauncher.launchApp(context, app)
    }

    fun reorderFavorites(newOrder: List<String>) {
        viewModelScope.launch {
            repository.reorderFavorites(newOrder)
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.settingsStore.updateSettings(newSettings)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetAll()
            showToast("Settings reset to defaults")
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun refreshSystemStatus(context: Context) {
        _isDeviceAdminActive.value = AppLauncher.isDeviceAdminActive(context)
        _isDefaultLauncher.value = AppLauncher.isDefaultLauncher(context)
    }

    fun performGestureAction(context: Context, action: GestureAction, customPkg: String?) {
        if (action != GestureAction.NONE) {
            HapticUtil.performGestureFeedback(context, settings.value.enableHapticFeedback)
        }
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> navigateTo(Screen.Drawer)
            GestureAction.OPEN_SEARCH -> {
                navigateTo(Screen.Drawer)
            }
            GestureAction.OPEN_NOTESPACE -> navigateTo(Screen.Notespace)
            GestureAction.OPEN_SETTINGS -> navigateTo(Screen.Settings)
            GestureAction.LOCK_SCREEN -> {
                val locked = AppLauncher.lockScreen(context)
                if (!locked) {
                    showToast("Device admin required to lock screen")
                    navigateTo(Screen.Settings)
                }
            }
            GestureAction.OPEN_APP -> {
                if (!customPkg.isNullOrBlank()) {
                    val app = allApps.value.firstOrNull { it.packageName == customPkg }
                    if (app != null) {
                        launchApp(context, app)
                    } else {
                        showToast("App not found: $customPkg")
                    }
                }
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val backup = repository.exportBackup()
                val json = backup.toJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(json)
                    }
                }
                showToast("Backup saved successfully")
            } catch (e: Exception) {
                showToast("Backup failed: ${e.message}")
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val content = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            content.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }
                val backup = BackupData.fromJson(content.toString())
                repository.importBackup(backup)
                showToast("Backup restored successfully")
            } catch (e: Exception) {
                showToast("Restore failed: ${e.message}")
            }
        }
    }

    class Factory(
        private val repository: AppRepository,
        private val weatherRepository: WeatherRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, weatherRepository) as T
        }
    }
}
