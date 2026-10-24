package com.plain.launcher.util

import com.plain.launcher.data.model.AppItem
import com.plain.launcher.data.model.DrawerSortOrder
import java.util.Locale

object AppSorter {

    fun sort(items: List<AppItem>, order: DrawerSortOrder): List<AppItem> {
        val locale = Locale.getDefault()
        return when (order) {
            DrawerSortOrder.AZ -> {
                items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayLabel })
            }
            DrawerSortOrder.ZA -> {
                items.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.displayLabel })
            }
            DrawerSortOrder.MOST_USED -> {
                items.sortedWith(
                    compareByDescending<AppItem> { it.launchCount }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayLabel }
                )
            }
            DrawerSortOrder.RECENTLY_INSTALLED -> {
                items.sortedWith(
                    compareByDescending<AppItem> { it.installTime }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayLabel }
                )
            }
        }
    }
}
