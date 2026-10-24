package com.plain.launcher.data.model

import android.os.UserHandle

data class AppItem(
    val packageName: String,
    val activityName: String,
    val originalLabel: String,
    val customLabel: String? = null,
    val userHandle: UserHandle? = null,
    val userSerial: Long = 0L,
    val isWorkProfile: Boolean = false,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = Int.MAX_VALUE,
    val isHidden: Boolean = false,
    val launchCount: Int = 0,
    val installTime: Long = 0L,
    val lastUsedTime: Long = 0L
) {
    val key: String
        get() = "$packageName/$activityName#$userSerial"

    val displayLabel: String
        get() {
            val base = if (!customLabel.isNullOrBlank()) customLabel else originalLabel
            return if (isWorkProfile) "$base (work)" else base
        }
}
