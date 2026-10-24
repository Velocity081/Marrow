package com.plain.launcher.data.model

import java.util.UUID

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isReminder: Boolean = false,
    val reminderTime: Long? = null
)
