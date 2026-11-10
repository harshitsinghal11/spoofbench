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

## Phase 3 — Choose and prepare the target app — ✅ Complete

- Fixed build: Gradle was picking up an incomplete JRE bundled with a
  VSCode extension instead of a real JDK. Fixed by pinning
  `org.gradle.java.home` in `gradle.properties` to Android Studio's own
  bundled JDK (`C:\Program Files\Android\Android Studio\jbr`).
- Built and installed the OwnTracks oss-debug APK on the emulator.
- Added a ground-truth logging patch in `BackgroundService.kt`'s
  `onLocationChanged()` (tag `SBPROBE`), logging lat/lon/accuracy/
  provider/isMock for every location the app actually consumes —
  independent of whatever the Defender library reports later.
- Verified end to end: fed a location via `adb emu geo fix`, confirmed
  the app received it (visible in its own message queue) and SBPROBE
  logged matching values, including `isMock=false` and
  `provider=fused`.


  ## Phase 5 — Defender core — 🚧 In Progress

- Created `defender-core` as a standalone pure-Kotlin/JVM Gradle project
  (no Android dependencies) — reused the Gradle wrapper from the
  OwnTracks checkout to avoid re-downloading/version mismatches.
- Built first two detection signals, each tested independently:
  - `PhysicsSignal` — flags implausible speed/"teleport" between
    consecutive fixes (haversine distance / time delta).
  - `MockFlagSignal` — flags the OS-reported mock-location flag.
- Built `DefaultLocationTrustEngine` combining both into a single
  0-100 trust score + flag decision.
- Fixed one Gradle issue: `useJUnitPlatform()` alone wasn't enough on
  this Gradle version — needed an explicit
  `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`
  dependency.
- All 5 unit tests passing.

**Still to do:** more signals (regularity, cross-provider consistency),
the offline CSV-replay harness, and later the thin Android adapter
module that wires this into a real app.