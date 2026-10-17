# Bubbles Messages 💬

A modern, iMessage‑style SMS app for Android (Kotlin + Jetpack Compose).

**Features**
- Blue/grey chat bubbles, large "Messages" title, search, unread dots, dark mode
- Send & receive SMS (works as a default SMS app), notifications
- Call any **saved contact** or **unknown number** (📞 button in chat, contact list and "New Message")
- Permissions: **Messages (SMS)**, **Contacts**, **Storage** (+ Phone for calling, Notifications)
- Export a chat to `Downloads` as a `.txt`
- Adaptive app icon (blue gradient + white speech bubble)

## Build the APK with GitHub (no PC setup needed)
1. Create a new repository on github.com (e.g. `BubblesMessages`).
2. Upload **all files in this folder** (keep the `.github` folder – use "Add file → Upload files" and drag the unzipped folder contents).
3. Open the **Actions** tab → **Build APK** → wait ~4 min for the green tick
   (or press **Run workflow**).
4. Open the finished run → **Artifacts** → download **BubblesMessages-APK** → unzip → `app-debug.apk`.

## Install on your phone
1. Copy/open `app-debug.apk` on the phone and allow **Install unknown apps** when asked.
2. Open the app → tap **Allow access** and accept the permissions.
3. Tap **Set** on the blue banner to make it your **default SMS app** (needed to send/receive and see sent messages inside the app).

## Build locally (optional)
Open the folder in **Android Studio (Koala or newer)** → *Run ▶*.
Or from a terminal with JDK 17 and Gradle 8.9: `gradle assembleDebug`
→ `app/build/outputs/apk/debug/app-debug.apk`

## Notes
- Plain SMS only. MMS/picture messages and iMessage‑style delivery receipts are not included.
- For Google Play you'd need to declare SMS/Call‑log permissions use; sideloading has no such limit.
