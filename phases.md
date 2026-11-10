# SpoofBench — Phased Build Guide

Companion to `SpoofBench_Project_Brief_v2.docx`. This file is the execution plan: what to do, in what order, with what you need, and how you know each phase is actually done — not just "demo-able."

**Scope note:** this guide covers environment setup, architecture, data protocol, milestones, and exit criteria for every phase. It does **not** include attack/hooking implementation code — for the technical "how do I hook this specific API" questions inside Phases 5–8, use the public Frida and Android developer documentation directly as you reach them; that's normal, well-documented tooling for testing your own app, and it's better learned from Frida's own docs than pasted in bulk here.

---

## 0. Before you start

| Item | Status needed |
|---|---|
| GitHub account | Required — repo, releases, issue tracking |
| Google account | Not required — use `google_apis` emulator images, no Play Store sign-in needed |
| A hackathon picked | Needed early, so its submission deadline is known and the phase order below can be paced against it |
| Zenodo account (optional) | For a citable DOI on the released dataset — adds credibility, not required |
| A spare Android phone (optional) | For data collection and a realism check — no root needed for this part |
| Hardware | Fedora laptop, i3-7020U, 8 GB RAM confirmed workable — run the emulator **or** Android Studio, not both at once |

Everything in this project is free. No paid accounts, no API keys, no cloud spend.

---

## 1. Target architecture (what you're building toward)

```
spoofbench/
├── README.md              ← ethics boundary + limitations stated up front
├── LICENSE                ← Apache-2.0 for your own code
├── docs/                  ← threat model, metrics, protocol (write these FIRST — Phase 1)
├── datasets/               ← recorded sessions + train/test split (Phase 4)
├── attacker/               ← Python + Frida attack tooling (Phases 5-8)
├── defender/
│   ├── core/               ← PURE KOTLIN, no Android imports — the scoring logic
│   ├── android/             ← thin adapter: wires core to LocationManager/SensorManager
│   └── replay-tests/        ← JUnit tests that feed recorded CSVs through core
├── bench/                  ← orchestrator + SQLite + metrics (detection rate, FP rate, TTD, AUC, CI)
├── dashboard/               ← static HTML/JS scorecard
└── targets/                 ← fork patch + build script (not a copy of the forked app)
```

**The one architectural decision that matters most:** the Defender's scoring logic (`defender/core`) must be plain Kotlin with zero Android framework dependencies. Everything else can be worked around if you run short on time; this cannot, because it's what lets you later replay real recorded sensor data through the exact same scoring code at scale, instead of only testing live on the emulator one run at a time. Set this module boundary up in Phase 5 and don't cross it.

---

## 2. Phase-by-phase plan

### Phase 1 — Prior art, threat model, protocol

**Goal:** know what already exists, and write down your rules before you have any results to be tempted by.

- Spend one focused day searching GitHub and Google Scholar for existing Android location-spoofing benchmarks or detection libraries. Write one page: what exists, what SpoofBench adds. (The project-brief revision already found one adjacent tool — PriviSense, a Frida-based sensor-spoofing research demo with no GPS component and no open benchmark — cite it, narrow your claim around it if needed.)
- Write `docs/threat-model.md`: what SpoofBench defends against (client-side, on-device spoofing) and what it explicitly doesn't (server-side verification, network/cell triangulation, hardware attestation).
- Write `docs/metrics.md`, freezing the definitions in Section 5 of the brief: detection rate with confidence interval, false-positive rate (per-window and per-session), time-to-detect (monotonic clock, median/IQR), trust-score AUC.
- Write `docs/protocol.md`: the train/test split rule, and the commitment to freeze the Defender before building the harder hold-out attack variants. Tag this commit.
- Pick your target hackathon and note its submission deadline, so you know how much of this sequence you can complete before it.
- Create the GitHub repo with the skeleton above, `LICENSE`, and a `README` stub stating the ethics boundary (your own fork, your own device, local only).

**Exit gate:** someone outside the project could read these four docs and know exactly how every number on the eventual scorecard will be computed, before any code exists.

---

### Phase 2 — Development environment

**Goal:** a working rooted emulator and the Frida toolchain attached to a throwaway test app — proven end to end before you touch the real target.

- Confirm KVM virtualization is available (`/dev/kvm` exists) — without it the emulator is unusably slow on this hardware.
- Install the Android command-line SDK tools (not the full Android Studio IDE, to save RAM) plus JDK 17, Python 3.11+, and Frida's command-line tools.
- Create an AVD using a `google_apis` (not `google_apis_playstore`) system image — this is what lets `adb root` work without extra setup.
- Run the emulator headless with memory capped (around 2 GB) so it coexists with everything else on an 8 GB machine.
- Install Frida's on-device component, matching the exact version of your desktop Frida tools — version mismatches are the single biggest time sink people hit with this toolchain, so pin the versions you land on in `docs/env.md` immediately.
- **Smoke test before moving on:** build a trivial throwaway Android app that just logs its location, confirm you can feed it a location through the emulator's location controls, and confirm Frida can attach to the running process and read/modify a value in it. This proves the whole chain works on a disposable target before you touch the real fork.
- Budget real learning time on: Frida's attach/intercept basics, and `LocationManager` / `SensorManager` / `AppOpsManager` fundamentals. Keep a running `docs/learning-log.md` — it doubles as evidence of depth for judges later.

**Exit gate:** emulator boots headless, `adb root` succeeds, Frida-server is confirmed running and reachable (`frida-ps -U` lists device processes), and the throwaway test app correctly logs real location/sensor data end to end. (Actually *altering* a value via a Frida hook is proven separately, in Phase 6, where the first real attack script is built — doing it here would mean writing hook code before there's a real target to test it against.)

---

### Phase 3 — Choose and prepare the target app

**Goal:** one real open-source app, building cleanly, with a known place to plug the Defender in.

- Clone candidates in this order and timebox each to one afternoon, stopping at the first clean build: **OwnTracks → OpenTracks → Traccar Client → TrackMe** (see the brief for the corrected license table — TrackMe is GPLv3, not permissive like the others).
- For each candidate you actually open, read the code (don't assume from the README) and record: which location API it calls, minimum/target SDK, whether it has a Play-Services-free build flavor, and whether it already does any mock-location checking.
- Build it, install it on the emulator, grant location permissions.
- Add a small patch at the app's single location-ingest point that logs what the app actually consumed, with a timestamp — this becomes your ground truth for "was the app fooled," independent of whatever the Defender later reports.
- Keep the upstream `LICENSE`. Your repo should contain a pinned-commit build script plus your patch, not a copy of the app.

**Exit gate:** running your build script against a clean checkout reproduces a working APK, and you can see your ground-truth log lines when you manually change the emulator's location.

---

### Phase 4 — Real-motion dataset

**Goal:** the data your false-positive measurement and hardest attack tier depend on, collected early enough to actually tune against. (This is deliberately sequenced right after the target app is working, and well before the Defender is tuned — collecting it late, as an afterthought, would leave nothing to tune against.)

- Record 15–20 real motion sessions of your own (walking, and ideally cycling/driving), 10–20 minutes each, varying phone position (hand, pocket, mount) across more than one day — position variety is what makes the false-positive number realistic rather than optimistic.
- Log GPS at around 1 Hz and accelerometer/gyroscope at a high enough rate to see real motion detail, all sharing a common device clock reference.
- Define and commit a fixed CSV schema (`datasets/schema.md`) so every later script reads data the same way.
- **Split sessions into train and test now, and commit that split before any tuning happens.** This is the single most important discipline in the whole project — thresholds get chosen on train only, every reported number comes from test only, and the split is never revisited after you've seen results.
- Before publishing anything, trim or offset the start/end of routes that reveal where you live — the benchmark only needs relative motion, not your actual home address.

**Exit gate:** at least 15 sessions load cleanly with no timestamp gaps, and `datasets/split.json` is committed.

---

### Phase 5 — Defender core v0 + offline replay harness

**Goal:** the pure-Kotlin scoring engine exists and can be fed either live Android events or recorded CSVs through the same code path.

- Build the `defender/core` module with no Android imports: an interface that takes a location/sensor event stream and returns a trust score plus which signals fired.
- Implement the signal set described in the brief (mock flag, cross-provider consistency, GPS-vs-sensor physics, fix-timing regularity, satellite/signal-quality consistency) as independent, inspectable components — each should be explainable on its own to a judge, not one black-box score.
- Build `defender/android` as a thin adapter wiring real `LocationManager`/`SensorManager` callbacks into the core's event types.
- Build the offline replay harness: JUnit tests that read a recorded CSV session and feed it through the exact same core, window by window.
- **Also build the low-tier attacks now (T0: pick a mock-location app via Developer Options; T1: replay a real GPX route through it)** — these use only standard, documented Android developer-options functionality, no hooking tools, and give you your first end-to-end signal.
- Run your first baseline pass: T0/T1 against the unprotected fork, logged through your ground-truth patch from Phase 3.

**Exit gate:** the same scoring code produces a verdict both live on the emulator and when replaying a recorded CSV, and T0/T1 baseline numbers exist.

---

### Phase 6 — Hook-based attacks, split by sophistication

**Goal:** the T2 family, deliberately split into naive/full/stealth variants rather than one "hook bypass" tier — this split is what keeps your later detection numbers from being accidentally self-flattering (see the brief's Section 3 note on why a sloppy hook makes a defender look artificially strong).

- Work through standard Frida tutorials and Android developer documentation for intercepting method calls on your own locally-running test app — this is well-trodden, publicly documented ground for app developers testing their own code, and is the right place to learn the specific technique rather than from a condensed summary.
- Build T2a (hides only the mock flag), T2b (hides the flag and keeps cross-provider data consistent), and — if time allows — T2c (also hides common instrumentation artifacts) against your own forked app, running locally.
- As each variant lands, run it against the Phase 5 Defender and record what it catches and what it misses, tier by tier.

**Exit gate:** T2a and T2b both run end-to-end against the fork and produce distinguishable detection results (if T2a and T2b score identically, that's a sign the defender's cross-provider signal isn't actually doing anything — investigate before moving on).

---

### Phase 7 — Sensor-consistent attacks + completing the Defender's signal set

**Goal:** T3, and the point where the Defender gets frozen.

- Build the kinematics piece: converting a planned route into plausible accelerometer/gyroscope values (the brief flags this as the one genuinely new skill on this project — budget real time, and use the documented fallback below if it runs long).
- **Fallback if this runs long:** a simplified constant-velocity motion model instead of full physics realism — the brief explicitly sanctions this, use it rather than letting T3 stall everything downstream of it.
- Finish any remaining Defender signals from Phase 5's plan.
- **Freeze point:** once the Defender's signal set and thresholds are where you want them for the headline results, tag the commit (e.g. `defender-frozen-v1`). Everything after this point in Phases 8–9 that builds *harder* attacks is built against this frozen version, not iterated against it — this is what makes the later numbers trustworthy rather than circular.

**Exit gate:** T3 (or its fallback) runs end-to-end, the Defender's full signal set is implemented, and the freeze tag exists.

---

### Phase 8 — Replay-and-warp attack + hold-out variants

**Goal:** T4, plus the harder attack variants that test the frozen Defender honestly.

- Build the T4 pipeline: take a recorded real session and remap it onto a different location while preserving its natural motion noise.
- Build any hold-out attack variants (e.g. T2c if skipped earlier, or a harder T3) against the **frozen** Defender from Phase 7 — don't go back and adjust the Defender to catch these; if it can't, that's a reportable finding, not a bug to quietly fix.
- Expect — and plan to report honestly — that T4 is difficult or impossible to catch with motion-only signals. The brief treats this as the expected, defensible finding, not a failure of the project.

**Exit gate:** at least one real session has been successfully warped and run through the full pipeline, and the hold-out attacks have been scored against the frozen Defender without any further Defender changes.

---

### Phase 9 — Benchmark harness, integration, baseline comparisons

**Goal:** everything becomes one automated, repeatable pipeline instead of manual one-off runs.

- Build the orchestrator that can run "attack tier X against app build Y" end to end and log results to SQLite.
- Implement the metrics module: detection rate with Wilson confidence intervals, false-positive rate per-window and per-session with CIs, time-to-detect median/IQR, and trust-score ROC/AUC per tier.
- Integrate the Defender into the fork's actual source (wherever it reads location) and rebuild the APK — this is "Round 2, protected."
- Implement at least one baseline detector for comparison (a mock-flag-only check, and ideally a simple re-implementation of the kind of signal set described in other public client-side integrity tools) — scoring something other than your own Defender is what supports "works against any app" rather than "works against the one app we tuned it on."

**Exit gate:** running the orchestrator produces a full results table — every implemented tier, baseline vs. protected, with confidence intervals — without manual steps in between.

---

### Phase 10 — Round 2 execution + dashboard

**Goal:** the actual before/after comparison, and the thing judges look at.

- Run the full Round 1 (baseline) and Round 2 (protected) passes on the **test split only**.
- Build the static dashboard reading the results JSON/SQLite export: per-tier detection rate (with CI), false-positive rate, time-to-detect, baseline-vs-SpoofBench-Defender comparison, side by side.
- Sanity-check every number against the metric definitions from Phase 1 before trusting the dashboard — this is the moment a silent off-by-one in a CSV timestamp column would otherwise slip through.

**Exit gate:** the dashboard shows a complete, real before/after scorecard generated from an actual pipeline run, not placeholder numbers.

---

### Phase 11 — Write-up, README, demo rehearsal

**Goal:** package it the way a reviewer or judge will actually encounter it.

- Finish the README: ethics boundary, the methodology (split hooks, frozen-defender hold-out attacks, train/test separation), and the honest limitation that T4 remains an open problem.
- Write the final report/submission material, leaning on `docs/` rather than reconstructing the methodology from memory.
- Rehearse the 2–3 minute demo sequence: unprotected fork getting fooled → protected fork catching it → scorecard, **including the tier where it honestly struggles**. A judge who asks "so what doesn't this catch?" and gets a confident, rehearsed answer is a stronger outcome than a demo that pretends everything works.
- Do a final pass checking every claimed number traces back to a committed dataset split and a frozen-Defender run — this is your defense if a judge pushes on "did you tune this on your own test data?"

**Exit gate:** you can explain, from memory, exactly why each number on the scorecard is trustworthy — which is the actual point of the project.

---

## 3. If you need to cut scope: cut list, in order

1. LSPosed/Xposed — stay on Frida only.
2. T2c (stealth hook variant).
3. Additional hold-out attack variants — keep at least one, drop the rest.
4. Any public dataset supplement — your own recorded sessions are sufficient.
5. A live FastAPI backend — export a static `results.json` and keep the dashboard static.
6. Full T3 physics — use the constant-velocity fallback explicitly sanctioned in the brief.

**Never cut:** confidence intervals on every reported number, the train/test split discipline, the pure-Kotlin replay-able core, and the honest-limitations section of the README. These are what separate this project from "a demo that worked once on stage" — which is the actual differentiator you're building toward.
