# Architecture

Procrastination Tracker is a multi-module Android project that ships two apps — a phone app and a
standalone Wear OS app — from a single codebase. This document explains how the modules relate,
which patterns the code follows, and the reasoning behind decisions that are not obvious from
reading the source.

## Module graph

```
        ┌────────────────┐        ┌────────────────┐
        │      :app      │        │     :wear      │
        │    (phone)     │        │ (Galaxy Watch) │
        └───────┬────────┘        └───────┬────────┘
                │                         │
                └────────────┬────────────┘
                             ▼
                    ┌────────────────┐
                    │  :trackerdata  │  Room database, repository,
                    │ (android lib)  │  Data Layer sync codec
                    └───────┬────────┘
                            ▼
                    ┌────────────────┐
                    │     :core      │  pure Kotlin, no Android
                    └────────────────┘
```

Dependencies only ever point downwards. `:core` knows nothing about Android, `:trackerdata` knows
nothing about the UI, and neither app module depends on the other.

| Module | Type | Responsibility |
|---|---|---|
| `:core` | Pure Kotlin (JVM) | Timer mode logic: `TimerEngine`, `TimerMode`, `TimerPlan`, `Session`, day summaries, time formatting |
| `:trackerdata` | Android library | Tracker mode persistence: Room entities and DAOs, `TrackerRepository`, sync payload codec, live session state |
| `:app` | Android application | Phone UI (Jetpack Compose + Material 3), foreground services, quick settings tile, bubble, accessibility screen lock |
| `:wear` | Android application | Watch UI (Compose for Wear OS), its own foreground services and local database |

### Why `:core` has no Android dependency

The 52/17 and Pomodoro cycles have to behave identically on the phone and on the watch. Keeping the
engine in a plain Kotlin module makes that a compiler guarantee rather than a convention: the code
physically cannot reach for a `Context`, a `SharedPreferences` or a `Handler`, so there is no way
for one platform to quietly drift from the other.

It also means this logic is testable on the JVM, with no emulator and no device — which is why the
test backlog starts here (see [ROADMAP.md](ROADMAP.md)).

### Why `:trackerdata` is shared instead of duplicated

Tracker mode stores the same data on both devices and merges it in both directions. A single module
holding the schema, the DAOs and the sync codec means a schema change cannot land on one side and
be forgotten on the other — a mismatch that would surface as silently dropped sessions instead of a
build error.

## Presentation pattern: MVVM with unidirectional state

Both apps follow the same shape:

```
Compose UI  ──user intent──▶  ViewModel  ──suspend call──▶  Repository  ──▶  Room / Data Layer
     ▲                                                                             │
     └──────────────── StateFlow<UiState> ◀────────── Flow<Entity> ◀────────────────┘
```

- **View** — `@Composable` functions only. They render a `UiState` and emit user intents as
  lambdas. No screen queries the database directly.
- **ViewModel** — exposes a single immutable `UiState` data class per screen through `StateFlow`,
  built by combining repository `Flow`s with `stateIn(viewModelScope, ...)`. State survives
  configuration changes without any save/restore code.
- **Repository** — the only component aware of where data lives. Callers never see a DAO, an entity
  cursor, or a `DataClient`.

Room emits a new `Flow` value on every write, so a change made anywhere — the UI, a foreground
service, the quick settings tile, or a payload arriving from the watch — reaches the screen through
the same path. There is no manual refresh anywhere in the codebase.

### Dependency wiring

Dependencies are constructed lazily in the `Application` subclass and read by the ViewModels:

```kotlin
class ProcrastinationTrackerApp : Application() {
    val trackerDatabase: TrackerDatabase by lazy { TrackerDatabase.build(this) }
    val trackerRepository: TrackerRepository by lazy { TrackerRepository(trackerDatabase) }
}
```

This is manual, service-locator style wiring. It is honest about its trade-off: it costs nothing at
runtime and needs no annotation processor, but it couples the ViewModels to a concrete `Application`
type, which is exactly what makes them awkward to unit test. Replacing it with a real DI container
is a tracked backlog item, not an oversight.

## Background execution

Tracking has to survive the user leaving the app, so both modes keep running behind a persistent
notification.

| Component | Module | Purpose |
|---|---|---|
| `TimerForegroundService` | `:app`, `:wear` | Runs the countdown, posts the ongoing notification, fires the end-of-block alert |
| `TrackerForegroundService` | `:app`, `:wear` | Runs the active slice stopwatch, exposes Pause/Stop actions |
| `TrackerTileService` | `:app` | Quick settings tile to start, pause and resume without opening the app |
| `BubbleController` | `:app` | Floating bubble showing the running slice, on its own notification channel |
| `ScreenLockAccessibilityService` | `:app` | Performs `GLOBAL_ACTION_LOCK_SCREEN` for the "lock the screen now" action |

The bubble deliberately lives on a separate notification channel from the ongoing notification:
users perceive them as two different features, so turning the floating control off must not cost
them the persistent notification.

Leaving the app for another app does not interrupt tracking. Swiping the app away from Recents is
treated as an explicit stop: the elapsed time is saved and the session is closed.

## Device sync

Sync runs over the **Wearable Data Layer API**, which only delivers data items between apps sharing
both an `applicationId` and a signing key. Both modules therefore declare the same `applicationId`
(`com.foxlab.procrastinationtracker`) while keeping distinct Kotlin namespaces —
`…procrastinationtracker` on the phone and `…procrastinationtracker.watch` on the watch.

Two independent channels carry different kinds of information:

- **Session sync** (`TrackerSyncCodec` / `ActivitySyncSender`) — the durable record. Each device
  owns a complete local database and merges the peer's payload, so both work fully offline and
  reconcile whenever they are in range. A safety-net push runs every two minutes on top of the
  explicit push that follows each closed session, covering pushes that failed while the devices
  were apart.
- **Live presence** (`LiveSessionSync`) — the ephemeral "I am counting right now" broadcast that
  drives hand-off prompts when a session starts on one device while the other is also present.

Deleted sessions are recorded as tombstones (`DeletedSessionEntity`) rather than simply removed,
because a plain delete would be resurrected by the next merge from the peer device.

## Data model

Tracker mode is built around three concepts:

- **Layout profile** — a named set of slices. `Duo` and `Tri` ship as read-only templates; editing
  one always produces a new `Custom` profile instead of overwriting the template. Up to 10 custom
  profiles, each holding 2 to 6 slices.
- **Slice** — a tracked category (Work, Study, Gym, Procrastinating…) with a title, colour and icon.
- **Activity session** — one continuous run of one slice, carrying start and end timestamps plus the
  device that recorded it, which is what makes the per-device breakdown possible.

Timer mode keeps its own separate history (`AppDatabase` in `:app`), since a focus/break block is a
different shape of record from a free-running slice stopwatch.

## Technology choices

| Area | Choice |
|---|---|
| Language | Kotlin 2.4.10 |
| Phone UI | Jetpack Compose, Material 3 |
| Watch UI | Compose for Wear OS |
| Persistence | Room (SQLite), local only — no cloud, by design |
| Async | Coroutines and `Flow` |
| Navigation | Navigation Compose |
| Sync | Play Services Wearable (Data Layer API) |
| Build | Gradle 9.7, AGP 9.3.1, KSP, Java 17 bytecode |

Cloud backup is intentionally absent. The app stores only local timing data, and adding an account
system would mean handling personal data for a tool whose entire value is being opened and closed in
a few seconds.
