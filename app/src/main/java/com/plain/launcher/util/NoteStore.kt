package com.plain.launcher.util

import android.content.Context
import com.plain.launcher.data.model.NoteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object NoteStore {
    private const val PREFS_NAME = "notespace_prefs"
    private const val KEY_NOTES_JSON = "notes_json"

    private val _notesFlow = MutableStateFlow<List<NoteItem>>(emptyList())
    val notesFlow: StateFlow<List<NoteItem>> = _notesFlow.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            _notesFlow.value = loadNotes(context)
            isInitialized = true
        }
    }

    fun getNotes(context: Context): List<NoteItem> {
        if (!isInitialized) {
            init(context)
        }
        return _notesFlow.value
    }

    fun saveNote(context: Context, note: NoteItem) {
        val current = loadNotes(context).toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            current[index] = note
        } else {
            current.add(0, note)
        }
        persistNotes(context, current)
        _notesFlow.value = current

        if (note.isReminder && note.reminderTime != null) {
            ReminderScheduler.scheduleReminder(context, note)
        } else {
            ReminderScheduler.cancelReminder(context, note.id)
        }
    }

    fun deleteNote(context: Context, noteId: String) {
        val current = loadNotes(context).toMutableList()
        current.removeAll { it.id == noteId }
        persistNotes(context, current)
        _notesFlow.value = current

        ReminderScheduler.cancelReminder(context, noteId)
    }

    fun reorderNotes(context: Context, newOrder: List<NoteItem>) {
        persistNotes(context, newOrder)
        _notesFlow.value = newOrder
    }

    private fun loadNotes(context: Context): List<NoteItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_NOTES_JSON, null) ?: return emptyList()
        val list = mutableListOf<NoteItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NoteItem(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        text = obj.optString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isReminder = obj.optBoolean("isReminder", false),
                        reminderTime = if (obj.has("reminderTime") && !obj.isNull("reminderTime")) obj.optLong("reminderTime") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun persistNotes(context: Context, notes: List<NoteItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (n in notes) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("text", n.text)
                put("timestamp", n.timestamp)
                put("isReminder", n.isReminder)
                if (n.reminderTime != null) {
                    put("reminderTime", n.reminderTime)
                }
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_NOTES_JSON, arr.toString()).apply()
    }
}
