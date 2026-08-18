# Roadmap and backlog

Current state: **both apps are functional and stable**. Timer mode and Tracker mode phase 1 are
complete, phone ↔ watch sync works in both directions, and everything runs offline on each device.

Automated tests and CI are in place — see [TESTING.md](TESTING.md) for the strategy. The
architecture work follows, now that there is a safety net to refactor against.

## Known gaps

Honest list of what the codebase is missing today.

| Gap | Impact |
|---|---|
| Dependency versions hardcoded across four `build.gradle.kts` files | Versions drift between modules; upgrades are a manual sweep |
| ViewModels reach the repository by casting `Application` | Makes them untestable without an Android runtime, and leaks a live database into UI tests |
| Some UI strings live in Compose source instead of `strings.xml` | Blocks localisation; the app is Portuguese-only today |
| `isMinifyEnabled = false` in release builds | No R8 shrinking or obfuscation; larger APK than necessary |
| No release signing config | Release APKs cannot be produced reproducibly |
| `TrackerHistoryModal` (700 lines) and `TrackerBoard` (462 lines) | Large composables that mix layout with formatting and aggregation |
| ViewModels have no tests | Blocked on dependency injection — see phase 2 |

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
- [ ] ViewModel tests (blocked on phase 2)

## Phase 2 — Architecture

Incremental, with tests already in place. The goal is not to adopt a new pattern — the project is
already MVVM with unidirectional state — but to close the gaps that pattern is currently paying for.

- [ ] **Version catalog** (`gradle/libs.versions.toml`) — single source for every dependency version
- [ ] **Dependency injection** (Hilt) — constructor-injected repositories and ViewModels, replacing
      the `Application` cast. Unblocks ViewModel tests, and removes the need for the
      `@Config(application = ...)` override the screenshot tests currently need.
- [ ] **Push business rules down into `:core`** — aggregation and formatting currently living in
      ViewModels and composables become pure, testable functions
- [ ] **Split the large composables** — separate layout from data shaping in the tracker board and
      the history modal
- [ ] **Extract a design-system module** — `BoardTokens` and the palette are shared informally today
- [ ] **Enable R8** for release builds, with the keep rules Room and Compose need

## Phase 3 — Continuous integration ✅

- [x] GitHub Actions workflow: tests and both APKs on every push and pull request
- [x] Screenshots uploaded as a build artifact on every run
- [x] Build status badge in the README
- [ ] Release workflow: build a signed APK and attach it to the tag automatically

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
