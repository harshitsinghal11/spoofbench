# Threat model

## What SpoofBench's Defender library defends against

An attacker who controls the Android device the app is running on, and who wants the app to believe the device is somewhere it isn't. Specifically:

| Tier | Attacker capability |
|---|---|
| T0 | Picks a fake-GPS app in Developer Options |
| T1 | Same, but feeds it a real route instead of a static point |
| T2a/T2b | Uses a runtime hook (Frida) to hide the mock-location flag — T2a sloppily, T2b cleanly (all providers stay consistent) |
| T3 | Also fabricates matching accelerometer/gyroscope data |
| T4 | Takes a real recorded walk/drive and remaps it onto a different location |

## What it does NOT defend against

- Server-side checks (IP geolocation, cell-tower/Wi-Fi triangulation) — those are a backend's job, not a client library's.
- A compromised backend or a man-in-the-middle on the network — out of scope.
- Hardware-level attacks (fake GNSS RF signals aimed at the antenna) — different threat class, not what this targets.
- A sufficiently resourced attacker who can also defeat the app's own tamper/integrity checks — no client-side library can win that fight indefinitely. This library raises the cost of spoofing; it does not claim to make it impossible.

## Why this is still worth building

Most apps today check exactly one thing: the OS mock-location flag. That stops T0/T1 and nothing else. This library adds several independent signals so that defeating it requires real effort (T2+), not a 30-second Play Store download.
