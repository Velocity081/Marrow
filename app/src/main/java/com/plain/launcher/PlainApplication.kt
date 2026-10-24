package com.plain.launcher

import android.app.Application
import com.plain.launcher.data.local.AppCustomizationStore
import com.plain.launcher.data.local.SettingsDataStore
import com.plain.launcher.data.repository.AppRepository
import com.plain.launcher.data.repository.WeatherRepository

class PlainApplication : Application() {

    lateinit var settingsStore: SettingsDataStore
        private set

    lateinit var customizationStore: AppCustomizationStore
        private set

    lateinit var appRepository: AppRepository
        private set

    lateinit var weatherRepository: WeatherRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsDataStore(this)
        customizationStore = AppCustomizationStore(this)
        appRepository = AppRepository(this, customizationStore, settingsStore)
        weatherRepository = WeatherRepository(this)
    }
}
