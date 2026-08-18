# Testing strategy

Every automated test in this project runs on the JVM. There is no emulator anywhere in the
pipeline, and no test needs a device attached. That is a deliberate choice, and this document
explains what it buys, what it gives up, and where the line is drawn.

```bash
./gradlew :core:test :trackerdata:testDebugUnitTest :app:testDebugUnitTest
```

The whole suite finishes in well under a minute.

## Three tiers

| Tier | Module | Runs on | What it proves |
|---|---|---|---|
| Unit | `:core` | Plain JVM | The timer state machine, plan clamping, day aggregation and formatting |
| Integration | `:trackerdata` | Robolectric + real in-memory Room | That a payload from the watch merges correctly into this device's database |
| UI / screenshot | `:app` | Robolectric + Roborazzi | That real Compose screens render and respond to taps — with a PNG as evidence |

### Unit — `:core`

`TimerEngine` takes time as a parameter (`start(nowMillis)`, `tick(deltaMillis)`) instead of
reading a clock. A full four-cycle Pomodoro — 150 minutes of simulated time — therefore runs in
microseconds:

```kotlin
engine.start(nowMillis = 0)
val events = engine.runFor((4 * 25 + 3 * 5) * 60_000L)
assertEquals(Phase.LONG_BREAK, events.last().nextPhase)
```

This is the argument for keeping the tier at all. Proving the 52/17 cadence through the UI would
mean waiting 52 real minutes per block; here it costs nothing, so the cadence can be asserted for
every mode and every custom plan.

### Integration — the watch, faked at the right seam

Two-device sync is the hardest thing in this project to verify, and the least suited to
automation: it needs a phone and a paired watch, and pairing emulators to exercise the Wearable
Data Layer is notoriously unreliable.

So the tests fake the watch at the only seam the app actually owns. On a real pair of devices the
sequence is:

```
watch builds TrackerSyncPayload → Data Layer carries it → ActivitySyncListenerService
                                                       → repository.mergeSyncPayload(payload)
```

Everything before the last arrow is Google's transport. `WatchSyncTest` plays the watch's part
directly: it builds the payload the watch would send, puts it through the **real**
`TrackerSyncCodec`, and asserts what the **real** Room database does with it. Backed by an
in-memory database and the actual entities, so the SQL, the schema and the merge rules are all
genuinely exercised.

That covers the rules a regression would silently break:

- a session recorded on the watch lands here, with its device of origin intact
- which profile is *active* is never taken from the remote side — a local decision per device
- a newer rename wins; a stale one is ignored
- a session deleted here is never resurrected by the watch's next full push
- merging the same payload twice changes nothing, which is what makes the periodic full sync safe

**What is still verified by hand:** that the Data Layer actually delivers between two physical
devices. That is a transport check, done once on real hardware, not something worth an emulator
farm.

### UI — Compose screenshots without a device

Robolectric provides the Android runtime and [Roborazzi](https://github.com/takahirom/roborazzi)
rasterises Compose, so a screen can be rendered, driven and photographed on the JVM:

```kotlin
compose.setContent { HomeScreen(onOpenTimer = {}, onOpenTracker = {}) }
compose.onNodeWithText("Modo Tracker").performClick()
compose.onRoot().captureRoboImage("build/reports/screenshots/home-screen.png")
```

Tests both assert and photograph: text assertions state the intent, and the PNG in
`app/build/reports/screenshots/` is the proof. CI uploads the whole folder as an artifact on
every run, so a rendering change is visible from the run page.

The screenshots in the README are produced by these tests, which is why they never go stale.

> **Note on the Roborazzi Gradle plugin:** it is not applied here. It still reads AGP's removed
> `TestedExtension` and so cannot load on AGP 9. The plugin only wires record/verify tasks and
> sets one flag, so `app/build.gradle.kts` sets `roborazzi.test.record` on the test task instead
> and the library works unchanged.

## What the tiers deliberately leave out

Some things genuinely cannot be proven off-device, and pretending otherwise would be worse than
naming them:

- **That a foreground service survives the user leaving the app.** No JVM test can assert this;
  it is an Android runtime behaviour.
- **That notification actions, the quick settings tile and the bubble reach the service.**
- **That swiping the app from Recents saves and closes the session.**
- **That the Data Layer delivers between two physical devices.**

These are checked by hand on real hardware — see [DEVICE_TESTING.md](DEVICE_TESTING.md) for the
commands. They are a short, deliberate manual list rather than a flaky emulator suite.

## A note on the Application class

`HomeScreenTest` and friends run with a plain `android.app.Application` via
`@Config(application = ...)`, not the app's own `ProcrastinationTrackerApp`. The real one opens
the Room database in `onCreate` and starts a sync loop that never ends, which outlives the test and
then fails the *next* one when the database closes underneath it.

That is not a testing quirk — it is the cost of wiring dependencies through the `Application`
object, and it is exactly what the dependency injection item in the [roadmap](ROADMAP.md) is meant
to remove. Until then, the override keeps the tests honest and the reason documented.
