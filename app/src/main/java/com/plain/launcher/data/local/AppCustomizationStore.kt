package com.plain.launcher.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.customizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_customizations")

data class AppCustomizations(
    val renames: Map<String, String> = emptyMap(),
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val launchCounts: Map<String, Int> = emptyMap()
)

class AppCustomizationStore(private val context: Context) {

    private object Keys {
        val RENAMES = stringPreferencesKey("renames_json")
        val FAVORITES = stringPreferencesKey("favorites_json")
        val HIDDEN = stringPreferencesKey("hidden_json")
        val LAUNCH_COUNTS = stringPreferencesKey("launch_counts_json")
    }

    val customizationsFlow: Flow<AppCustomizations> = context.customizationDataStore.data.map { prefs ->
        val renamesMap = mutableMapOf<String, String>()
        prefs[Keys.RENAMES]?.let { str ->
            try {
                val json = JSONObject(str)
                json.keys().forEach { k -> renamesMap[k] = json.getString(k) }
            } catch (e: Exception) {
                // ignore
            }
        }

        val favsList = mutableListOf<String>()
        prefs[Keys.FAVORITES]?.let { str ->
            try {
                val arr = JSONArray(str)
                for (i in 0 until arr.length()) favsList.add(arr.getString(i))
            } catch (e: Exception) {
                // ignore
            }
        }

        val hiddenSet = mutableSetOf<String>()
        prefs[Keys.HIDDEN]?.let { str ->
            try {
                val arr = JSONArray(str)
                for (i in 0 until arr.length()) hiddenSet.add(arr.getString(i))
            } catch (e: Exception) {
                // ignore
            }
        }

        val countsMap = mutableMapOf<String, Int>()
        prefs[Keys.LAUNCH_COUNTS]?.let { str ->
            try {
                val json = JSONObject(str)
                json.keys().forEach { k -> countsMap[k] = json.getInt(k) }
            } catch (e: Exception) {
                // ignore
            }
        }

        AppCustomizations(
            renames = renamesMap,
            favorites = favsList,
            hidden = hiddenSet,
            launchCounts = countsMap
        )
    }

    suspend fun setRename(key: String, newName: String?) {
        context.customizationDataStore.edit { prefs ->
            val renamesMap = mutableMapOf<String, String>()
            prefs[Keys.RENAMES]?.let { str ->
                try {
                    val json = JSONObject(str)
                    json.keys().forEach { k -> renamesMap[k] = json.getString(k) }
                } catch (e: Exception) { }
            }

            if (newName.isNullOrBlank()) {
                renamesMap.remove(key)
            } else {
                renamesMap[key] = newName.trim()
            }

            val json = JSONObject()
            renamesMap.forEach { (k, v) -> json.put(k, v) }
            prefs[Keys.RENAMES] = json.toString()
        }
    }

    suspend fun togglePin(key: String) {
        context.customizationDataStore.edit { prefs ->
            val favsList = mutableListOf<String>()
            prefs[Keys.FAVORITES]?.let { str ->
                try {
                    val arr = JSONArray(str)
                    for (i in 0 until arr.length()) favsList.add(arr.getString(i))
                } catch (e: Exception) { }
            }

            if (favsList.contains(key)) {
                favsList.remove(key)
            } else {
                favsList.add(key)
            }

            val arr = JSONArray()
            favsList.forEach { arr.put(it) }
            prefs[Keys.FAVORITES] = arr.toString()
        }
    }

    suspend fun setFavoritesOrder(newFavorites: List<String>) {
        context.customizationDataStore.edit { prefs ->
            val arr = JSONArray()
            newFavorites.forEach { arr.put(it) }
            prefs[Keys.FAVORITES] = arr.toString()
        }
    }

    suspend fun setHidden(key: String, hide: Boolean) {
        context.customizationDataStore.edit { prefs ->
            val hiddenSet = mutableSetOf<String>()
            prefs[Keys.HIDDEN]?.let { str ->
                try {
                    val arr = JSONArray(str)
                    for (i in 0 until arr.length()) hiddenSet.add(arr.getString(i))
                } catch (e: Exception) { }
            }

            if (hide) {
                hiddenSet.add(key)
            } else {
                hiddenSet.remove(key)
            }

            val arr = JSONArray()
            hiddenSet.forEach { arr.put(it) }
            prefs[Keys.HIDDEN] = arr.toString()
        }
    }

    suspend fun incrementLaunchCount(key: String) {
        context.customizationDataStore.edit { prefs ->
            val countsMap = mutableMapOf<String, Int>()
            prefs[Keys.LAUNCH_COUNTS]?.let { str ->
                try {
                    val json = JSONObject(str)
                    json.keys().forEach { k -> countsMap[k] = json.getInt(k) }
                } catch (e: Exception) { }
            }

            val current = countsMap[key] ?: 0
            countsMap[key] = current + 1

            val json = JSONObject()
            countsMap.forEach { (k, v) -> json.put(k, v) }
            prefs[Keys.LAUNCH_COUNTS] = json.toString()
        }
    }

    suspend fun importCustomizations(
        renames: Map<String, String>,
        favorites: List<String>,
        hidden: List<String>,
        launchCounts: Map<String, Int>
    ) {
        context.customizationDataStore.edit { prefs ->
            val rObj = JSONObject()
            renames.forEach { (k, v) -> rObj.put(k, v) }
            prefs[Keys.RENAMES] = rObj.toString()

            val fArr = JSONArray()
            favorites.forEach { fArr.put(it) }
            prefs[Keys.FAVORITES] = fArr.toString()

            val hArr = JSONArray()
            hidden.forEach { hArr.put(it) }
            prefs[Keys.HIDDEN] = hArr.toString()

            val cObj = JSONObject()
            launchCounts.forEach { (k, v) -> cObj.put(k, v) }
            prefs[Keys.LAUNCH_COUNTS] = cObj.toString()
        }
    }

    suspend fun resetAll() {
        context.customizationDataStore.edit { it.clear() }
    }
}
