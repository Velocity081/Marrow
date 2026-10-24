package com.plain.launcher.data.model

import java.util.UUID

data class PriorityAlertRule(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val keyword: String = "",
    val glowEdges: Boolean = true,
    val flashTorch: Boolean = false,
    val glowColor: Long = 0xFFFFD600L
)
