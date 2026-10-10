# Notify 🔔

> A weekend timepass, vibecoded side project built for **one purpose only**: send me an exact notification when I need it. Zero ads or zero calendar spam.

---

### Why build this?

Look, all I wanted was a lightweight app that pops a notification on my phone at an exact time. But modern apps have lost their minds:

1. **Google Tasks / Keep**: For reasons beyond human comprehension, they insist on dumping every trivial ping directly onto Google Calendar. Now my calendar is a chaotic mess where *"book cab"* and *"grocery run"* are mixed with important Events / Birthdays etc.
2. **Play Store Reminder Apps**: Download any generic reminder tool and you're immediately hit with:
   - Unskippable Ads for free tier
   - Mandatory account creation and *"Sign in with Google"*
   - A paid subscription just to unlock recurring reminders
   - Demands for precise location, and full internet access... *for a reminder app?*

**Notify has none of that nonsense:**
-  **Zero Ads**
-  **Zero Accounts / Logins**
-  **Zero Internet Permission**
-  **Zero Calendar Clutter** (keeps your calendar clean and sacred)
-  **100% Offline & Private**

---

### How this was built

So this entire app was **vibecoded over a weekend with the almighty Claude Opus** handling the Android heavy lifting. It's clean, lightweight, does exactly what it's supposed to do, and runs smoothly on device without drama.

---

### Features

- **Exact Alarm Scheduling**: Uses Android's `AlarmManager` (`RTC_WAKEUP`) to trigger on time even when the phone is deep in Doze mode.
- **Multiple Times per Day**: Add multiple alert times (e.g., 9:00 AM, 2:00 PM, 8:00 PM) to a single reminder without duplicate entries.
- **Flexible Recurrence**:
  - **Specific Days**: Select any days of the week with instant presets (*All Days*, *Weekdays*, *Weekends*).
  - **Weekly Cycles**: Repeat every week, every other week, or any custom week interval.
  - **Monthly Rules**: By date (1st–31st or dynamic *Last Day of the Month*) or by weekday pattern (*2nd Tuesday*, *Last Friday*).
  - **Monthly Cycles**: Repeat every month, every 6th month, once a year, or custom month intervals.
- **Plain-English Schedule Preview**: Live ℹ️ explanation banner translates your schedule into clear English so you always know when it fires.
- **Actionable Notifications**: Pops a clean heads-up alert with an inline **Snooze** button right in the notification shade.
- **Auto-Cleanup**: Completed reminders prune themselves after a configurable window so you never have to hoard old pings.
- **Dark Mode Support**: Respects system theme or lets you lock it to dark mode.

---

### Building & Running the APK

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
