# Notify 🔔

> A weekend timepass, 100% vibecoded app created to do **one thing and one thing only**: ping me with a notification at the exact minute I asked for. That's literally it.

---

### Why does this exist? 

Look, all I wanted was an app that sends me a notification when I tell it to. But modern apps are exhausting:

1. **Google Tasks / Keep**: For some baffling reason, they insist on dumping every tiny reminder directly onto Google Calendar. Now my calendar is clogged with things like *"drink water"* or *"take chicken out of freezer"* alongside actual work meetings. My calendar looked disgusting.
2. **Play Store Reminder Apps**: Download any top-rated reminder app and you are instantly greeted with:
   - Fullscreen video ads with sound
   - *"Create an account or sign in with Google"*
   - *"Upgrade to Pro ($4.99/mo) to unlock repeat reminders"*
   - Requesting contacts, location, and full internet access... *for a reminder app?*

**Notify has none of that bullshit.**
- 🚫 **Zero Ads**
- 🚫 **Zero Accounts / Logins**
- 🚫 **Zero Internet Permission** (the app literally cannot talk to the internet even if it wanted to)
- 🚫 **Zero Calendar Clutter** (leaves your Google Calendar completely untouched)
- 📱 **100% Offline & Private**

---

### The Tech Stack (or lack thereof)

**Full disclaimer**: I am **not** an Android developer. I do not know Kotlin. If you ask me about clean architecture, MVVM, MVI, or repository patterns, I will smile and nod politely.

This entire app was **100% vibecoded and prompt-engineered into existence thanks to the god Opus**. I just threw prompts at it on a weekend until an APK popped out that does what I want. 

This is **not a resume project**. Please don't review my code. It works on my machine and on my phone, and that's a massive victory in my book.

---

### What it actually does

- **Set a reminder**: Give it a title, optional notes, date, and time.
- **Repeat if needed**: One-time, daily, weekdays, weekends, or specific days.
- **Snooze button**: Hits you with a notification with an inline "Snooze" button so you can procrastinate properly.
- **Clean history**: Completed reminders auto-clean themselves so the app doesn't hoard junk.
- **Dark mode**: Because blinding white screens at midnight are a crime.

---

### How to build & install the APK

If for some reason you also want a no-nonsense reminder app without ad popups:

#### Option 1: Just build the APK via command line

1. Make sure you have **Java 17** installed.
2. Run:
   ```bash
   ./gradlew assembleDebug
   ```
3. Grab the generated APK file from:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
4. Send it to your phone (via USB, Quick Share, Telegram, etc.) and install it.

#### Option 2: Android Studio

1. Open this folder in **Android Studio**.
2. Plug in your phone (or start an emulator).
3. Click the big green **▶ Run** button.
4. Done.
