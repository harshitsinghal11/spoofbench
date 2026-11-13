# Build Log

## Phase 1 — Prior art, threat model, protocol — ✅ Complete

- Set up repo skeleton: README, LICENSE (Apache-2.0), docs/threat-model.md,
  docs/metrics.md, docs/protocol.md, .gitignore.
- Locked in the ethics boundary (own fork, own device, local only) and the
  metric definitions before writing any code, so results can't quietly get
  redefined later.

## Phase 2 — Development environment — ✅ Complete

**Setup:**
- Windows 11, ASUS TUF A15, Ryzen 7 7435HS, 16 GB RAM.
- Installed Android Studio, Android SDK (API 34), Python 3.11 venv, Frida 17.25.2.
- Created AVD `Pixel_6`, system image `google_apis` x86_64 (not Play Store —
  required for `adb root` to work).

**Issues hit and fixed:**
1. New Android Studio project failed AAR metadata check — AndroidX libraries
   required compileSdk 37, project was on 36. Fixed by bumping `compileSdk`
   to 37 in `app/build.gradle.kts` and letting Studio install the platform.
2. `adb root` + background `frida-server` chained with `&` in the same
   Windows terminal line never actually ran — Windows `&` just sequences
   commands, it doesn't background them like Linux. Fixed by running the
   emulator in its own terminal and adb commands in a separate one.
3. First Frida-server download was the wrong asset
   (`frida-barebone-agent-...-winnt-x86_64` — a Windows binary). Needed
   `frida-server-17.25.2-android-x86_64.xz` instead (Android + server, not
   Windows + agent).
4. Smoke-test app (`smoketest`) crashed on launch:
   `IllegalStateException: You need to use a Theme.AppCompat theme (or
   descendant) with this activity.` — `MainActivity` extended
   `AppCompatActivity` but the project's default theme was Material3, not
   AppCompat. Fixed by switching to `ComponentActivity`, which has no theme
   requirement.
5. Switching to `ComponentActivity` broke the old `onRequestPermissionsResult`
   override (wrong signature for that base class). Replaced it with the
   modern `registerForActivityResult(ActivityResultContracts.RequestPermission())`
   API, which works cleanly with `ComponentActivity`.

**Verification:**
- `frida-ps -U` lists the full emulator process table — Frida-server
  confirmed reachable from the desktop Frida tools.
- `adb emu geo fix 77.2090 28.6139` + smoke-test app logcat (`-s LP`) shows:
  `lat=28.6139 lon=77.209 isMock=false provider=gps` — confirming the
  location pipeline (emulator → LocationManager → app) works end to end.
- Altering a value via a live Frida hook is deferred to Phase 6, where the
  first real attack script is written against the actual forked target app.

## Phase 3 — Choose and prepare the target app — ✅ Complete

- Target chosen: **OwnTracks**, `oss` build flavor (no Google Play Services
  dependency — matches our Play-Services-free emulator image).
- Fixed a Windows-specific Git issue: a resource file (`values-en/strings.xml`)
  is a symlink in the upstream repo; Windows Git doesn't create real symlinks
  by default, so it checked out as a plain text file containing the link
  target path instead of XML, breaking the resource build. Fixed by enabling
  Windows Developer Mode + `git config --global core.symlinks true` and
  re-cloning.
- Fixed a second build failure: Gradle was picking up an incomplete JRE
  bundled with a VSCode extension instead of a real JDK (`jlink` executable
  missing). Fixed by pinning `org.gradle.java.home` in `gradle.properties`
  to Android Studio's own bundled JDK (`C:\Program Files\Android\Android
  Studio\jbr`).
- Built and installed the OwnTracks `oss`-debug APK on the emulator.
- Added a ground-truth logging patch in `BackgroundService.kt`'s private
  `onLocationChanged()` (tag `SBPROBE`), logging lat/lon/accuracy/provider/
  isMock for every location the app actually consumes — independent of
  whatever the Defender library reports later.
- Verified end to end: fed a location via `adb emu geo fix`, confirmed the
  app received it (visible in its own outgoing message queue) and `SBPROBE`
  logged matching values, including `isMock=false` and `provider=fused`.

## Phase 4 — Real-motion dataset — 🚧 In Progress

- Decided on 10 real-motion recording sessions, 20 minutes each, across
  3 physical devices (own phone + 2 friends'), using the "Sensor Logger"
  app (GPS + accelerometer + gyroscope, exported as a zip per session).
- Session conditions per device: casual indoor walking with phone in
  pocket, casual indoor walking while actively using the phone (scrolling),
  and travelling (vehicle) with phone in pocket — chosen for a mix of
  motion-sensor noise profiles and GPS signal quality, including a known
  limitation that indoor sessions will have noisier/sparser GPS than
  outdoor use.
- **Status:** data collection is actively running across all 3 devices;
  zips not yet received/processed.
- **Still to do:** once zips arrive, convert each session's export into the
  project's schema (`gps.csv` + `imu.csv` + `meta.json`), commit a
  train/test split by session, and feed sample data through the Phase 5
  replay harness to validate end to end.

## Phase 5 — Defender core — 🚧 In Progress

- Created `defender-core` as a standalone pure-Kotlin/JVM Gradle project
  (zero Android dependencies) — reused the Gradle wrapper from the
  OwnTracks checkout to avoid re-downloading/version mismatches.
- Fixed one Gradle issue: `useJUnitPlatform()` alone wasn't enough on this
  Gradle version — needed an explicit
  `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`
  dependency for tests to run at all.
- Built three detection signals so far, each independently unit-tested:
  1. **`MockFlagSignal`** — flags the OS-reported mock-location flag.
  2. **`PhysicsSignal`** — flags implausible speed/"teleport" between
     consecutive fixes (haversine distance / time delta).
  3. **`RegularitySignal`** — flags location updates arriving at
     suspiciously perfect, unvarying intervals (real GNSS fixes have
     natural timing jitter; many synthetic feeds don't).
- Combined all three into `DefaultLocationTrustEngine`, producing a single
  0–100 trust score plus a per-signal evidence breakdown. 7/7 unit tests
  passing.
- Built `CsvReplay` — a standalone runner (`./gradlew run --args="<file>"`)
  that reads a `gps.csv` file and feeds every row through the engine,
  printing a verdict + evidence per row and a flagged-rate summary. This
  is the tool that will process the real friend-collected data once it's
  converted into the project schema — no emulator/phone needed to test
  against recorded data.
- Verified against a hand-written sample CSV: a teleport row correctly
  flagged via `physics`, a mock-provider row correctly flagged via
  `mockFlag`, 4 genuine rows correctly passed through untouched.

**Signals still planned** (deliberately deferred until their matching
attack tier is being built, per the brief's signal list):
- Cross-provider consistency, satellite/signal-quality consistency — to
  be added before Phase 6's attack scripts.
- GPS-vs-sensor motion correlation — paired with Phase 7 (T3).
- Hook-integrity/instrumentation detection — paired with Phase 6's T2b.

**Still to do:** 2 more GPS-only signals, then move into Phase 6.