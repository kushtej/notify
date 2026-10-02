# Notify 🔔

> A weekend timepass, vibecoded side project built for **one purpose only**: send me an exact notification when I need it. Zero fluff, zero ads, zero calendar spam.

---

### Why build this?

Look, all I wanted was a lightweight app that pops a notification on my phone at an exact time. But modern apps have lost their minds:

1. **Google Tasks / Keep**: For reasons beyond human comprehension, they insist on dumping every trivial ping directly onto Google Calendar. Now my calendar is a chaotic mess where *"book cab"*, *"grocery run"*, and *"water plants"* are competing for visual space with actual work meetings.
2. **Play Store Reminder Apps**: Download any generic reminder tool and you're immediately hit with:
   - Fullscreen video ads with unskippable audio
   - Mandatory account creation and *"Sign in with Google"*
   - A paid subscription just to unlock recurring reminders
   - Demands for contacts, precise location, and full internet access... *for a reminder app?*

**Notify has none of that nonsense:**
- 🚫 **Zero Ads**
- 🚫 **Zero Accounts / Logins**
- 🚫 **Zero Internet Permission** (no tracking, no telemetry, no calls home — it literally cannot connect to the web)
- 🚫 **Zero Calendar Clutter** (keeps your calendar clean and sacred)
- 📱 **100% Offline & Private**

---

### How this was built

So this entire app was **vibecoded over a weekend with the almighty Claude Opus** handling the Android heavy lifting. It's clean, lightweight, does exactly what it's supposed to do, and runs smoothly on device without drama.

---

### Features

- ⏰ **Exact Alarm Scheduling**: Uses Android's `AlarmManager` (`RTC_WAKEUP`) to trigger on time even when the phone is deep in Doze mode.
- 🔁 **Flexible Repeats**: One-time, daily, weekdays, weekends, or custom days of the week.
- 💤 **Actionable Notifications**: Pops a clean heads-up alert with an inline **Snooze** button right in the notification shade.
- 🧹 **Auto-Cleanup**: Completed reminders prune themselves after a configurable window so you never have to hoard old pings.
- 🌙 **Dark Mode Support**: Respects system theme or lets you lock it to dark mode.

---

### Building & Running the APK

#### Option 1: Command Line (Fastest)

1. Ensure you have **Java 17** installed.
2. Run:
   ```bash
   ./gradlew assembleDebug
   ```
3. Your APK will be waiting at:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
4. Transfer it to your phone and install.

#### Option 2: Android Studio

1. Open this directory in **Android Studio**.
2. Connect your device (or fire up an emulator).
3. Click the green **▶ Run** button.
