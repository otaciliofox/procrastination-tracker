<div align="center">

# Procrastination Tracker

**Track where your time actually goes — on your phone and on your wrist.**

A two-mode time tracker for Android and Wear OS: the classic 52/17 and Pomodoro focus timer, plus a
free-form category stopwatch for people whose day does not fit a fixed cycle.

[![CI](https://github.com/otaciliofox/procrastination-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/otaciliofox/procrastination-tracker/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Wear OS](https://img.shields.io/badge/Wear%20OS-3%2B-1A73E8?logo=wearos&logoColor=white)](https://wearos.google.com)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Room](https://img.shields.io/badge/Room-SQLite-FF6F00?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Gradle](https://img.shields.io/badge/Gradle-9.7-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[Português (pt-BR)](README.pt-BR.md) · [Architecture](docs/ARCHITECTURE.md) · [Testing](docs/TESTING.md) · [Roadmap](docs/ROADMAP.md)

<table>
<tr>
<td align="center"><img src="docs/screenshots/tracker-board.png" width="300" alt="Tracker board"></td>
<td align="center"><img src="docs/screenshots/home-screen.png" width="300" alt="Home screen"></td>
</tr>
<tr>
<td align="center"><b>Tracker mode</b><br>each band's height is its share of the day</td>
<td align="center"><b>Home</b><br>pick how you want to measure today</td>
</tr>
</table>

<sub>These screenshots are rendered by the Compose screenshot tests, so they cannot drift from the app.</sub>

</div>

---

## Why this exists

The original **Procrastination Timer** (`com.tomuozawa.procrastinationtimer`) was pulled from the
Play Store — it had not been updated since 2019 and no longer met the target API requirements. It
was a simple, effective idea: measure focused time against procrastinated time and let the contrast
speak for itself.

This project rebuilds it for modern Android, adds a native Galaxy Watch app, and extends it with a
second mode for days that do not divide neatly into "focus" and "break".

## The two modes

### ⏱️ Timer mode — the original, rebuilt

Interval timing with a persistent notification, full session history, and a productive vs.
procrastinated comparison for today and for all time.

| Mode | Focus | Short break | Long break |
|---|---|---|---|
| **52/17** | 52 min | 17 min | — |
| **Pomodoro** | 25 min | 5 min | 30 min every 4 cycles |
| **45/15** | 45 min | 15 min | — |
| **Custom** | 1–180 min | 1–180 min | configurable, every 2–12 cycles |

### 🍕 Tracker mode — one stopwatch per category

A "pizza" of 2 to 6 named slices — Work, Study, Gym, Hobby, Procrastinating, whatever fits. Tap a
slice to start counting, tap again to pause, switch freely all day. No alarms, no forced rhythm.

- **Layout profiles** — `Duo` and `Tri` ship ready to use and are never overwritten; editing one
  creates a new custom profile. Up to 10 custom profiles, 2 to 6 slices each.
- **Runs in the background** with an ongoing notification carrying Pause and Stop actions.
  Switching to another app does not interrupt tracking; swiping the app away saves and stops.
- **Quick settings tile** and a **floating bubble** to control the running slice without opening
  the app.
- **Today and week summaries** per slice, broken down by which device recorded the time.
- **Lock the screen now** — an optional accessibility action for when the phone itself is the
  distraction.

## Phone ↔ watch sync

Both apps keep their own complete local database and work fully offline. When the devices are near
each other they reconcile in **both directions** over the Wearable Data Layer API — start a session
on the watch, finish it on the phone, and the history stays consistent either way. A live presence
channel powers hand-off prompts when both devices are counting at once.

Deletions travel as tombstones, so a session deleted on one device stays deleted instead of coming
back on the next merge.

## Architecture at a glance

Four modules, dependencies pointing one way only:

```
:app (phone) ─┐
              ├─▶ :trackerdata (Room + sync) ─▶ :core (pure Kotlin)
:wear (watch) ┘
```

`:core` holds the timer logic with **no Android dependency at all**, so phone and watch cannot drift
apart — and so it can be unit tested on the JVM without an emulator. Both apps follow MVVM with
unidirectional state: Compose renders an immutable `UiState`, ViewModels expose it as `StateFlow`,
and repositories own all data access.

The full reasoning is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Tech stack

**Kotlin** · **Jetpack Compose** + **Material 3** · **Compose for Wear OS** · **Room** ·
**Coroutines & Flow** · **Navigation Compose** · **Foreground Services** · **Quick Settings Tile** ·
**Wearable Data Layer API** · **Gradle 9.7** with **AGP 9.3.1** and **KSP**

## Tests

Every automated test runs on the JVM — no emulator, no attached device — so the whole suite is one
command and fits in a pipeline:

```bash
./gradlew :core:test :trackerdata:testDebugUnitTest :app:testDebugUnitTest
```

| Tier | What it covers |
|---|---|
| **Unit** (`:core`) | The timer state machine. Time is a parameter, so a four-cycle Pomodoro runs in microseconds instead of 150 minutes |
| **Integration** (`:trackerdata`) | The watch's sync payload merged into a real in-memory Room database: tombstones, last-write-wins, idempotent re-merge |
| **UI** (`:app`) | Real Compose screens rendered, tapped and photographed — the PNGs above come from these tests |

What genuinely needs hardware — foreground service survival, notification actions, the tile, and
Data Layer delivery between two devices — is verified by hand rather than by an emulator suite.
The reasoning is in [docs/TESTING.md](docs/TESTING.md).

## Getting started

**Requirements:** Android Studio Koala (2024.1) or newer, JDK 17+, and a device running Android 8.0
(API 26) or newer. The watch app needs Wear OS 3+ (Galaxy Watch 4 and later).

```bash
git clone https://github.com/otaciliofox/procrastination-tracker.git
cd procrastination-tracker
./gradlew assembleDebug
```

This produces both APKs:

- `app/build/outputs/apk/debug/app-debug.apk` — phone
- `wear/build/outputs/apk/debug/wear-debug.apk` — watch

### Installing

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

For the watch, enable **Settings → About → tap the software version 5 times**, then **Settings →
Developer options → ADB debugging** and **Debug over Wi-Fi**. The watch shows an IP and port:

```bash
adb connect 192.168.0.42:5555
adb -s 192.168.0.42:5555 install wear/build/outputs/apk/debug/wear-debug.apk
```

Both apps are standalone — the watch does not need the phone app running.

> **Note:** both apps must be signed with the same key for sync to work. Debug builds share the
> debug keystore, so this is automatic during development.

## Project structure

```
procrastination-tracker/
├── core/         → pure Kotlin timer logic, no Android dependency
├── trackerdata/  → Room database, repository and sync codec shared by both apps
├── app/          → phone app (Jetpack Compose + Material 3)
├── wear/         → Galaxy Watch app (Compose for Wear OS, standalone)
├── docs/         → architecture, roadmap and development guides
└── spec/         → feature specifications
```

## Roadmap

Automated tests lead the backlog, followed by dependency injection, a version catalog and CI.
Feature work — Wear OS tile, watch complication, history charts — comes after. See
[docs/ROADMAP.md](docs/ROADMAP.md) for the full list, including an honest account of what the
codebase is still missing.

## License

[MIT](LICENSE) © Otacílio Neto
