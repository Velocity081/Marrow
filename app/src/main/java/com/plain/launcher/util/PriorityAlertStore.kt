package com.plain.launcher.util

import android.content.Context
import com.plain.launcher.data.model.PriorityAlertRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object PriorityAlertStore {
    private const val PREFS_NAME = "priority_alerts_prefs"
    private const val KEY_RULES_JSON = "priority_rules_json"

    private val _rulesFlow = MutableStateFlow<List<PriorityAlertRule>>(emptyList())
    val rulesFlow: StateFlow<List<PriorityAlertRule>> = _rulesFlow.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            _rulesFlow.value = loadRules(context)
            isInitialized = true
        }
    }

    fun getRules(context: Context): List<PriorityAlertRule> {
        if (!isInitialized) {
            init(context)
        }
        return _rulesFlow.value
    }

    fun saveRule(context: Context, rule: PriorityAlertRule) {
        val current = loadRules(context).toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            current[index] = rule
        } else {
            current.add(rule)
        }
        persistRules(context, current)
        _rulesFlow.value = current
        com.plain.launcher.service.PlainNotificationListenerService.refresh()
    }

    fun deleteRule(context: Context, ruleId: String) {
        val current = loadRules(context).toMutableList()
        current.removeAll { it.id == ruleId }
        persistRules(context, current)
        _rulesFlow.value = current
        com.plain.launcher.service.PlainNotificationListenerService.refresh()
    }

    private fun loadRules(context: Context): List<PriorityAlertRule> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RULES_JSON, null) ?: return emptyList()
        val list = mutableListOf<PriorityAlertRule>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PriorityAlertRule(
                        id = obj.optString("id"),
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        keyword = obj.optString("keyword"),
                        glowEdges = obj.optBoolean("glowEdges", true),
                        flashTorch = obj.optBoolean("flashTorch", false),
                        glowColor = obj.optLong("glowColor", 0xFFFFD600L)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun persistRules(context: Context, rules: List<PriorityAlertRule>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (r in rules) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("packageName", r.packageName)
                put("appName", r.appName)
                put("keyword", r.keyword)
                put("glowEdges", r.glowEdges)
                put("flashTorch", r.flashTorch)
                put("glowColor", r.glowColor)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_RULES_JSON, arr.toString()).apply()
    }
}
