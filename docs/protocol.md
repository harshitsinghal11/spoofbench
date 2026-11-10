# Protocol — locked in before results exist

1. **Real motion data is split into train/test before any tuning.** Thresholds get chosen on train only. Every number on the dashboard comes from test only. This split, once committed, doesn't get revisited after seeing results.
2. **The library gets frozen before the hardest attack variants are built.** T3/T4 and any hook variant built specifically to evade the library are built against a frozen, tagged version — not iterated against it live.
3. **Every attack tier is scored separately**, never collapsed into one average.
4. **The dashboard always shows the unprotected run and the protected run side by side** for the same attack, same app, same conditions.
