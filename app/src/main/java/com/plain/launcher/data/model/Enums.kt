package com.plain.launcher.data.model

enum class ThemeMode(val label: String) {
    DARK("dark"),
    LIGHT("light"),
    AMOLED("extra dark"),
    SYSTEM("system")
}

enum class AppFontWeight(val label: String) {
    REGULAR("regular"),
    MEDIUM("medium"),
    BOLD("bold")
}

enum class AppTextAlign(val label: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right")
}

enum class AppVerticalAlign(val label: String) {
    TOP("top"),
    CENTER("center"),
    BOTTOM("bottom")
}

enum class AppTextCase(val label: String) {
    AS_IS("as is"),
    LOWERCASE("lowercase"),
    UPPERCASE("uppercase"),
    CAPITALIZED("capitalized")
}

enum class DrawerSortOrder(val label: String) {
    AZ("a - z"),
    ZA("z - a"),
    MOST_USED("most used"),
    RECENTLY_INSTALLED("recently installed")
}

enum class GestureAction(val label: String) {
    NONE("none"),
    OPEN_DRAWER("open drawer"),
    OPEN_SEARCH("open search"),
    OPEN_NOTESPACE("open notespace"),
    OPEN_SETTINGS("open settings"),
    LOCK_SCREEN("lock screen"),
    OPEN_APP("open app")
}

enum class AppOrientation(val label: String) {
    SYSTEM("follow system"),
    PORTRAIT("portrait"),
    LANDSCAPE("landscape")
}
