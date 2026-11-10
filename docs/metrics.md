# Metrics — how every number on the dashboard is computed

Locking these in before any code runs, so results can't quietly get redefined to look better later.

- **Detection Rate (per tier)** — of all attack runs at a given tier, the % the library flagged as spoofed. Reported per tier, never as one blended average (a blended number would hide exactly where detection stops working).
- **False Positive Rate** — of all genuine (non-spoofed) sessions, the % wrongly flagged. Measured both per 30-second window and per full session.
- **Time-to-Detect** — seconds between the spoofed location first reaching the app and the library raising a flag, using the device's own clock on both ends.
- **Trust Score** — the library's 0–100 output per location update. Not the headline number by itself — it's what the detection rate and false-positive rate are computed from, at a fixed cutoff decided in advance.

If the sample size for a tier is small (a handful of runs), the dashboard says so next to the number instead of presenting it as precise.
