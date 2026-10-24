package com.plain.launcher.ui.navigation

sealed class Screen {
    object Home : Screen()
    object Drawer : Screen()
    object Notespace : Screen()
    object Settings : Screen()
    object HiddenApps : Screen()
    object ReorderFavorites : Screen()
    object GestureSettings : Screen()
    data class SelectGestureApp(val gestureKey: String) : Screen()
}
