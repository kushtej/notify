# Notify

A clean, modern, and lightweight reminder application for Android built entirely with **Jetpack Compose** and **Material 3**.

Notify helps you schedule time-sensitive tasks and alerts with exact timing, recurring schedules, and instant snooze controls right from the notification shade.

---

## Features

- **Exact Alarm Scheduling**: Employs Android's `AlarmManager` with `RTC_WAKEUP` to ensure reminders trigger reliably, even when the device is idle or in Doze mode.
- **Flexible Recurrence**:
  - One-time reminders
  - Daily recurrences
  - Weekdays / Weekends
  - Custom day selection (e.g., Mon, Wed, Fri)
- **Actionable Notifications**: Heads-up alerts with a direct inline **Snooze** action accessible directly from the notification tray.
- **Snooze & History Tracking**:
  - Dedicated tabs for **Active**, **Snoozed**, and **Completed** reminders.
  - Configurable global snooze duration (e.g., 5, 10, 15, or 30 minutes).
  - Configurable completed task history retention and automated cleanup.
- **Material 3 UI & Theming**:
  - Seamless support for Light, Dark, and System default themes.
  - Clean card-based list layout with slide-out navigation drawer.
- **Lightweight & Private**: All data is stored locally on the device with zero network tracking or external dependencies.

---

## Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 components
- **Background Scheduling**: Android `AlarmManager` + `BroadcastReceiver` (`AlarmReceiver`)
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`) and Version Catalogs (`libs.versions.toml`)
- **Compatibility**: Android 8.0 (API level 26) through Android 14 (API level 34)

---

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/notify/
│   ├── MainActivity.kt           # App entry point, theming, and permission handling
│   ├── data/
│   │   └── ReminderStorage.kt    # Local persistence & cleanup logic
│   ├── model/
│   │   ├── Reminder.kt           # Reminder data model
│   │   └── Screen.kt             # Navigation destinations
│   ├── receiver/
│   │   └── AlarmReceiver.kt      # BroadcastReceiver for alarms & snooze actions
│   ├── scheduler/
│   │   └── AlarmScheduler.kt     # Exact alarm scheduling logic
│   └── ui/
│       ├── AppState.kt           # Shared UI state & event triggers
│       ├── NotifyApp.kt          # Root navigation drawer & screen routing
│       ├── screens/
│       │   ├── DetailScreen.kt   # Reminder detail view
│       │   ├── EditorScreen.kt   # Create & edit reminder interface
│       │   ├── ReminderListScreen.kt # Active / Snoozed / History list views
│       │   └── SettingsScreen.kt # Snooze duration & theme preferences
│       └── theme/                # Material 3 colors, typography, and themes
└── res/                          # Drawables, mipmaps, and app values
```

---

## Getting Started

### Prerequisites

- **Android Studio**: Iguana (2023.2.1) or newer
- **JDK**: Java 17
- **Android SDK**: Compile / Target SDK 34, Min SDK 26

### Building from Command Line

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/Notify.git
   cd Notify
   ```

2. Compile the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

3. Run unit tests:
   ```bash
   ./gradlew test
   ```

---

## Permissions

The app uses the following system permissions declared in `AndroidManifest.xml`:

- `POST_NOTIFICATIONS`: Required on Android 13 (API 33)+ to display notification banners.
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Required to trigger exact reminders at the user-specified time.
- `RECEIVE_BOOT_COMPLETED`: Facilitates rescheduling active alarms after device reboots.
