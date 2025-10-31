# Task TV Launcher

A simple, functional **Home Launcher** for **Android TV** that displays all installed apps and launches them on selection.

> **No root, no system app, no device owner required.**

---

## Features

- Displays **app icon + app name** in a grid
- Launches apps with **one click**
- **Stays as default launcher** after first selection
- **Auto-opens on boot**
- **Home button returns to launcher**
- **Back button does nothing** (prevents exit)
- Works on **Android 5.0+ (API 21)**

## Optional: Disable Google TV Launcher (if u have ADB this will good way )

> **Warning: Recommended for production / company deployment**  
> If you have **ADB access**, disable the system launcher to **prevent any interference**.

### with ADB Step : Disable Google TV Launcher
```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

## Screenshots

<img width="1081" height="646" alt="image" src="https://github.com/user-attachments/assets/293bb9ab-23d7-4298-8d65-b88415c5e58a" />


## Build Instructions

1. Open in **Android Studio**
2. Set `minSdkVersion 21`, `targetSdkVersion 35`
3. Build APK:  
   `Build > Build Bundle(s) / APK(s) > Build APK`

---

## Testing Instructions (Android TV Emulator)

### Step 1: Launch Emulator
1. Open **Android Studio**
2. Go to **Device Manager**
3. Launch **Android TV (1080p) API 34**

### Step 2: Install APK
```bash
adb install -r app-debug.apk
