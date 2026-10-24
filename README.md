# plain

**plain** is an ultra-minimalist, strictly monochrome, highly customizable native Android launcher built with Kotlin and Jetpack Compose (minSdk 26, targetSdk 35).

It contains zero icons, zero Material vectors, zero shadows, zero borders/outlines, zero elevation, zero ripples, and zero accent colors. Typography throughout the entire app is set exclusively in **Geist Mono** (bundled Regular, Medium, and Bold weights under the SIL Open Font License).

---

## Deliverables & APK Locations

Pre-built, signed APKs ready for installation:

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK** (signed with debug keystore for one-step installation): `app/build/outputs/apk/release/app-release.apk`

---

## Building from Source

### Prerequisites
- JDK 17 or JDK 21 (e.g. JetBrains Runtime `jbr` bundled with Android Studio)
- Android SDK Platform 35 and Build-Tools 35.0.0

### Build Commands

```bash
# Build the Debug APK
./gradlew assembleDebug

# Build the Release APK (automatically signed with debug keystore)
./gradlew assembleRelease

# Run all unit tests (pure JVM logic verification)
./gradlew test

# Run Android Lint checks
./gradlew lint
```

*(On Windows PowerShell, use `.\gradlew.bat`)*

---

## Installation Guide (Physical Phone)

### Option A: Using ADB (Recommended)
Connect your phone with USB debugging enabled and run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# or for release:
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Option B: Direct Device Installation (Sideloading)
1. Transfer `app-release.apk` or `app-debug.apk` to your phone (via USB transfer, Google Drive, email, or local web server).
2. Open your phone's File Manager and tap the APK file.
3. If prompted, toggle **Allow from this source** ("Install unknown apps").
4. Tap **Install** and then **Open**.

---

## Setup & Permissions

### 1. Set as Default Launcher
- Open **plain**.
- Swipe left or tap `settings >`.
- Scroll to **// system & display** and tap **set as default launcher `[set now]`**.
- Alternatively, go to your phone's system **Settings > Apps > Default Apps > Home App** and select **plain**.

### 2. Grant Device Admin for Screen Lock
The double-tap gesture and lock screen gestures use Android's `DevicePolicyManager.lockNow()` API to instantly lock the display without requiring an accessibility service:
1. Open **plain** settings.
2. In **// system & display**, tap **device admin (screen lock) `[grant]`**.
3. Android's system Device Admin screen will appear. Tap **Activate this device admin app**.
4. The status will update to `[active]`. Double-tapping any empty space on the home screen will now immediately lock your phone.

---

## Hard Design Rules Implemented

- **No App Icons**: All applications are displayed as text names only.
- **Geist Mono Everywhere**: Home, App Drawer, Search, Settings, Dialogs, and Toasts use bundled `GeistMono-Regular`, `GeistMono-Medium`, and `GeistMono-Bold` TTFs. The entire `MaterialTheme.typography` defaults to Geist Mono.
- **Strictly Monochrome**: Palette consists exclusively of black, white, and shades of gray. No accent colors or hues. Active states use text/background swaps or gray levels.
- **No Outlines, Borders, Shadows, or Elevation**: Flat, clean surfaces with zero drop shadows.
- **No Ripples**: Ripple effects are completely disabled across all components with a custom `NoRippleIndication`.
- **Text-Only Controls**: Sliders are drawn as flat geometric bars. Toggles display `[on]` and `[off]`. Reorder actions use `[up]` and `[down]`.
- **Edge-to-Edge**: Transparent status and navigation bars with full cutout support.

---

## Feature List

### Home Screen
- Clean vertical list of app names (favorites by default; full list if none pinned yet).
- Optional Clock and Date header (12h/24h toggle, customizable date format, size, and alignment).
- Single tap launches the app.
- Long press opens flat text menu:
  - **Pin / unpin** from home favorites
  - **Hide app** (removes from home, drawer, and search)
  - **Rename app** (custom label displayed everywhere; searchable by both original and custom name)
  - **App info** (opens system details)
  - **Uninstall** (system deletion prompt)
- Gestures on empty space (configurable to None, Open Drawer, Open Search, Open Settings, Lock Screen, Open App):
  - Swipe Up
  - Swipe Down
  - Swipe Left
  - Swipe Right
  - Double Tap (defaults to Lock Screen)
- Pressing the Home key anywhere on the launcher returns to the top of the list.

### App Drawer & Search
- Full alphabetical list of installed launchable apps.
- Fast-scroll letter index (`a` .. `z`, `#`) along the right edge.
- Search field at the top with fuzzy matching (subsequence, prefix, and word boundary scoring).
- Optional toggle to auto-open keyboard on entering drawer.
- Optional toggle to auto-launch when exactly one search result remains.
- Work profiles supported via `LauncherApps` and displayed as text: `AppName (work)`.
- Live callbacks via `LauncherApps.Callback` for real-time app installs, updates, and removals.

### Customization (Settings with Live Preview)
- **Live Preview Box**: Shows real-time updates of clock, date, and app row typography as you adjust settings.
- **Monochrome Grayscale Sliders**: Separate background and text gray level sliders with live WCAG 2.1 contrast ratio calculation and automatic AA contrast clamping (guaranteeing $\ge 4.5:1$).
- **Typography**: Font size (12–32sp), font weight (Regular, Medium, Bold), letter spacing (-0.05 to 0.20em), line spacing (1.0x to 2.0x), item vertical spacing (0–32dp).
- **Alignment**: Text alignment (Left, Center, Right) and home vertical alignment (Top, Center, Bottom).
- **Text Case**: As is, lowercase, UPPERCASE, Capitalized.
- **Clock & Date Customization**: Toggle clock/date, 12h/24h format, font size slider, alignment, and date format strings.
- **Favorites Management**: Max favorites slider and dedicated drag/reorder screen.
- **Hidden Apps Screen**: Dedicated screen to view and unhide hidden apps.
- **System Display Controls**: Toggle status bar, toggle navigation bar, screen orientation lock (System, Portrait, Landscape).
- **Backup & Restore**: Export all settings, pins, custom renames, and hidden lists to a JSON file via Android Storage Access Framework (SAF), and restore on any device.
- **Reset to Defaults**: Flat confirmation dialog to restore default settings.

---

## Unit Testing

Pure logic components are covered by JUnit unit tests:
- `FuzzySearchTest`: Substring, prefix, acronym, subsequence matching, and dual original/custom name search.
- `AppSorterTest`: Alphabetical (A-Z, Z-A), launch frequency (Most Used), and timestamp (Recently Installed).
- `TextTransformsTest`: As-is, lowercase, uppercase, and capitalized word transforms.
- `ContrastCalculatorTest`: Relative luminance, WCAG 2.1 contrast ratios, and boundary clamping to $\ge 4.5:1$.
- `BackupRestoreTest`: Full JSON export/import round-trip fidelity.
- `SettingsSerializationTest`: Serialization, default values, and graceful corrupted input recovery.
