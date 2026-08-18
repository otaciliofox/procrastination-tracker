# Testing on a physical device

Notes for running the apps on real hardware. Nothing here is required to build the project — it is
the workflow used to check UI changes on a phone and a Galaxy Watch without an emulator.

Emulators are a poor fit for this project in particular: foreground services, notification actions,
the quick settings tile and the Wearable Data Layer all behave differently (or not at all) outside a
real device pair.

## Connecting

**Phone over USB** — enable **Developer options → USB debugging**, plug it in, and accept the
prompt.

**Phone or watch over Wi-Fi** — on the watch, enable developer mode by tapping the software version
five times in **Settings → About**, then turn on **ADB debugging** and **Debug over Wi-Fi**. The
screen shows an IP and port:

```bash
adb connect 192.168.0.42:5555
adb devices
```

With more than one device attached, target a specific one with `-s`:

```bash
adb -s 192.168.0.42:5555 install -r wear/build/outputs/apk/debug/wear-debug.apk
```

## Build, install, launch

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.foxlab.procrastinationtracker/.MainActivity
```

The watch module is the same cycle with `:wear:assembleDebug`.

## Screen mirroring

[scrcpy](https://github.com/Genymobile/scrcpy) mirrors the device in a desktop window and forwards
mouse and keyboard input to it — the fastest way to exercise a UI change without picking up the
phone:

```bash
scrcpy --window-title "Procrastination Tracker"
```

It works on the watch too, which is useful for checking round-screen layouts at a readable size.

## Screenshots

```bash
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png ./screen.png
```

## Simulating input

Useful for reproducing a bug in the same order every time:

```bash
adb shell input tap 540 1200      # tap at x=540 y=1200
adb shell input text "Study"      # type into the focused field
adb shell input keyevent 4        # back button
```

## Inspecting the running app

```bash
adb logcat --pid=$(adb shell pidof -s com.foxlab.procrastinationtracker)
adb shell dumpsys activity services com.foxlab.procrastinationtracker
```

The second command is the quickest way to confirm a foreground service is actually alive after the
app leaves the screen — the single most common source of "the timer stopped counting" reports.

## Verifying sync

Sync only works when both apps are signed with the same key and share an `applicationId`. Debug
builds use the shared debug keystore, so this holds automatically during development. To check that
a payload is actually moving between the devices:

```bash
adb logcat -s WearableDataLayer
```

Start a session on one device and watch the other pick it up.
