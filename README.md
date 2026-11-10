# SpoofBench

A Kotlin library that drops into any Android app and detects GPS/location spoofing — plus an open benchmark that proves it actually works, by attacking a real open-source app with a tiered set of spoofing attacks, before and after the library is integrated.

## What this is

- **`defender/`** — the Kotlin library. Pure-logic core (no Android imports) + a thin Android adapter. Scores every location update 0–100 and says which signal fired.
- **`attacker/`** — the attack tooling. Five tiers, from "pick a fake-GPS app in Developer Options" to "replay a real recorded walk at a different location."
- **`bench/`** — runs every attack tier against a target app twice (unprotected, then with the library integrated) and records what got caught.
- **`dashboard/`** — a simple before/after scorecard.

## Ethics boundary

Every attack here runs against our own fork of an open-source app, built and executed locally, on our own device/emulator. Nothing here touches another person's device, another company's servers, or a published app we don't control.

## Status

🚧 Early build — see `docs/protocol.md` for what's locked in before any numbers get reported.

## License

Apache-2.0 for the code in this repo. The forked target app keeps its own upstream license — see `targets/`.
