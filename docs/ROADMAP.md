# Roadmap and backlog

Current state: **both apps are functional and stable**. Timer mode and Tracker mode phase 1 are
complete, phone ↔ watch sync works in both directions, and everything runs offline on each device.

What follows is ordered by what unlocks the most: tests come before the architecture refactor,
because tests are what make the refactor safe.

## Known gaps

Honest list of what the codebase is missing today.

| Gap | Impact |
|---|---|
| No automated tests anywhere — zero `test`/`androidTest` source sets | Every regression is found by hand, on a device |
| Dependency versions hardcoded across four `build.gradle.kts` files | Versions drift between modules; upgrades are a manual sweep |
| ViewModels reach the repository by casting `Application` | Makes them effectively untestable without an Android runtime |
| Some UI strings live in Compose source instead of `strings.xml` | Blocks localisation; the app is Portuguese-only today |
| `isMinifyEnabled = false` in release builds | No R8 shrinking or obfuscation; larger APK than necessary |
| No release signing config | Release APKs cannot be produced reproducibly |
| `TrackerHistoryModal` (700 lines) and `TrackerBoard` (462 lines) | Large composables that mix layout with formatting and aggregation |

## Phase 1 — Automated tests

The priority. `:core` is 414 lines of pure Kotlin with no Android dependency, so its tests run on
the JVM in seconds — the best return per line of test code in the project.

- [ ] `TimerEngine` — phase transitions, long-break cadence, pause/resume, elapsed accounting
- [ ] `TimerPlan.sanitized()` — boundary clamping for minutes and cycle counts
- [ ] `TimerDaySummary` — productive vs. procrastinated aggregation
- [ ] `TimeFormat` and `ActivityRules` — formatting edges and slice count limits
- [ ] `TrackerSyncCodec` — payload round-trip, so a schema change cannot silently break sync
- [ ] Room DAO tests for `:trackerdata` — tombstone handling and two-way merge behaviour
- [ ] ViewModel tests (depends on Phase 2 DI)
- [ ] Compose UI tests for the two critical flows: start/pause/switch slice, and finish a timer block

## Phase 2 — Architecture

Incremental, with tests already in place. The goal is not to adopt a new pattern — the project is
already MVVM with unidirectional state — but to close the gaps that pattern is currently paying for.

- [ ] **Version catalog** (`gradle/libs.versions.toml`) — single source for every dependency version
- [ ] **Dependency injection** (Hilt) — constructor-injected repositories and ViewModels, replacing
      the `Application` cast. This is what unblocks ViewModel tests.
- [ ] **Push business rules down into `:core`** — aggregation and formatting currently living in
      ViewModels and composables become pure, testable functions
- [ ] **Split the large composables** — separate layout from data shaping in the tracker board and
      the history modal
- [ ] **Extract a design-system module** — `BoardTokens` and the palette are shared informally today
- [ ] **Enable R8** for release builds, with the keep rules Room and Compose need

## Phase 3 — Continuous integration

- [ ] GitHub Actions workflow: assemble both apps and run tests on every push and pull request
- [ ] Build status badge in the README, wired to that workflow
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

- **Cloud backup and accounts** — local SQLite only; losing data on app clear is an accepted risk
- **Play Store distribution** — the app is sideloaded, so it does not carry Play compliance work
- **Automatic coaching or suggestions** based on tracked data — the app reports, it does not advise
- **Alarms inside Tracker mode** — a free-running stopwatch by design; alarms belong to Timer mode
