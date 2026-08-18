# Roadmap and backlog

Current state: **both apps are functional and stable**. Timer mode and Tracker mode phase 1 are
complete, phone ↔ watch sync works in both directions, and everything runs offline on each device.

Automated tests and CI are in place — see [TESTING.md](TESTING.md) for the strategy. The
architecture work follows, now that there is a safety net to refactor against.

## Known gaps

Honest list of what the codebase is missing today.

| Gap | Impact |
|---|---|
| Some UI strings live in Compose source instead of `strings.xml` | Blocks localisation; the app is Portuguese-only today |
| `TrackerHistoryModal` (700 lines) and `TrackerBoard` (462 lines) | Large composables that mix layout with formatting and aggregation |
| `Application.onCreate` opens the database and starts a sync loop | The process touches disk just by existing, which is why tests override the Application |
| **Watch → phone sync never delivers** | Confirmed on hardware: the phone's database holds 22 sessions, all `sourceDevice=phone`, while the watch holds 6 of its own that it has marked as sent. Phone → watch works. Pre-existing, and reproduced identically on a debug build, so unrelated to R8 or Hilt |

## Phase 1 — Automated tests ✅

Done. Every tier runs on the JVM, so the whole suite is a single Gradle command with no device
and no emulator involved.

- [x] `TimerEngine` — phase transitions, long-break cadence, pause/resume, elapsed accounting
- [x] `TimerPlan.sanitized()` — boundary clamping for minutes and cycle counts
- [x] `TimerDaySummary` — productive vs. procrastinated aggregation, interrupted blocks
- [x] `TimeFormat` and `ActivityRules` — formatting edges, accent-insensitive slice naming
- [x] `TrackerSyncCodec` — payload round-trip through the real `DataMap`
- [x] **Watch sync merge tests** — the watch's payload against a real in-memory Room database:
      tombstones, last-write-wins, local-only active profile, idempotent re-merge
- [x] **Compose screenshot tests** — home screen, tracker board and summary card rendered, tapped
      and photographed on the JVM; PNGs uploaded by CI on every run
- [x] ViewModel tests — real repository over an in-memory database, no Android app required

## Phase 2 — Architecture

Incremental, with tests already in place. The goal was never to adopt a new pattern — the project is
already MVVM with unidirectional state — but to close the gaps that pattern was paying for.

- [x] **Version catalog** (`gradle/libs.versions.toml`) — single source for every dependency version
- [x] **Dependency injection** (Hilt) — constructor-injected repositories and ViewModels across both
      apps, replacing the `application as ProcrastinationTrackerApp` cast in all 11 places that used
      it. Services, the tile and the Data Layer listeners are `@AndroidEntryPoint`. Verified on a
      physical phone and watch, since the services have no automated coverage.
- [ ] **Move start-up work out of `Application.onCreate`** — it opens the database and starts an
      endless sync loop, so the process touches disk just by existing and tests must override the
      Application to stay isolated
- [ ] **Push business rules down into `:core`** — aggregation and formatting currently living in
      ViewModels and composables become pure, testable functions
- [ ] **Split the large composables** — separate layout from data shaping in the tracker board and
      the history modal
- [ ] **Extract a design-system module** — `BoardTokens` and the palette are shared informally today
- [x] **R8 on release builds** — 11.4 MB → 1.5 MB on the phone, 43.0 MB → 2.1 MB on the watch.
      The keep rules protect the three enums that are persisted by name in Room columns and in
      Data Layer payloads; renaming them would make an existing database unreadable.

## Phase 3 — Continuous integration ✅

- [x] GitHub Actions workflow: tests and both APKs on every push and pull request
- [x] Screenshots uploaded as a build artifact on every run
- [x] Build status badge in the README
- [x] Release workflow: signed APKs built and attached to the tag, with a check that both
      carry the same signature

## Phase 4 — Features

Carried over from the original plan, none of them blocking.

- [ ] **Wear OS Tile** — start and pause from the watch tile carousel, without opening the app
- [ ] **Watch face complication** showing remaining time
- [ ] **Day-by-day history chart** on the phone
- [ ] **Custom vibration patterns** at the end of a watch session
- [ ] **Tracker phase 2** — subtasks inside a slice ([spec 002](../spec/002-activity-tracker-mode.md))
- [ ] **Tracker phase 3** — running a pomodoro inside a slice, merging both modes
- [ ] **Create and edit custom profiles on the watch** (phone-only today, by design)
- [ ] **English localisation** — extract remaining hardcoded strings first

## Out of scope, deliberately

These are decided, not pending:

- **Emulators in the pipeline.** Development happens on physical devices, and an emulator-based
  instrumentation suite would add minutes of CI time and a class of failures unrelated to the
  code. The behaviours that genuinely need hardware — foreground service survival, notification
  actions, the tile, and Data Layer delivery between two devices — are verified by hand, and
  listed in [TESTING.md](TESTING.md).
- **Cloud backup and accounts** — local SQLite only; losing data on app clear is an accepted risk
- **Play Store distribution** — the app is sideloaded, so it does not carry Play compliance work
- **Automatic coaching or suggestions** based on tracked data — the app reports, it does not advise
- **Alarms inside Tracker mode** — a free-running stopwatch by design; alarms belong to Timer mode
