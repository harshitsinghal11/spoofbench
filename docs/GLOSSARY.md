# Glossary — SpoofBench

Har baar jab koi naya term/tool introduce hua, uska matlab aur project me uska role yahan hai.

---

## Project-level

**SpoofBench** — poora project ka naam. Ek open-source benchmark jo test karta hai ki Android apps GPS-spoofing (fake location) ko kitna achhe se detect kar paate hain — "Defender" library ke bina aur uske saath, side-by-side.

**Defender-core** — woh Kotlin library jo hum khud bana rahe hain. Iska kaam hai: kisi app ko incoming location updates dekh kar batana "yeh location genuine lag rahi hai ya spoofed" (0-100 trust score ke saath). Yeh pure-Kotlin/JVM module hai — Android se independent, isiliye isse CSV file par bhi test kar sakte hain, emulator chalaye bina.

**Target app** — woh real-world app jispe hum attack karte hain aur Defender test karte hain. Humne **OwnTracks** (open-source location-tracking app) ko fork kiya hai.

**Attacker / attack scripts** — woh tools/scripts jo fake location target app tak pahunchate hain, alag-alag "tiers" (difficulty levels) me — T0 se T4 tak.

**Bench / dashboard** — final piece jo sab results (unprotected run vs protected run, har attack tier ke against) ek simple dashboard me dikhayega.

---

## Core Android/location concepts

**LocationManager** — Android ka system service jo GPS/network se location updates deta hai apps ko.

**Mock location / `isMock`** — Android har `Location` object ke saath ek boolean flag bhejta hai batane ke liye "yeh location real GPS hardware se aayi ya kisi test-provider/fake-GPS app se". Yeh sabse basic signal hai jo apps check karte hain (aur jo sabse pehle bypass kiya jata hai).

**Provider** — location kahan se aayi iska naam: `"gps"` (satellite), `"network"` (cell/Wi-Fi triangulation — kam accurate), `"fused"` (dono ka mix), `"passive"`.

**Accuracy (accuracyM)** — Android ka apna estimate ki location kitni precise hai (meters me radius). Real GPS me yeh fix-to-fix thoda-thoda badalta rehta hai (jitter); fake-GPS apps aksar ek fixed value bhejte hain.

**Satellites** — kitne GPS satellites se signal lock mila, iska count. Zyada satellites = better accuracy expected.

---

## Dev environment

**AVD (Android Virtual Device) / Emulator** — Android Studio ka virtual phone jisme hum bina real device ke test karte hain.

**`adb` (Android Debug Bridge)** — command-line tool jisse hum emulator/phone ko control karte hain (app install, logs dekhna, fake location bhejna, etc.).

**`adb emu geo fix <lon> <lat>`** — emulator ko ek fixed fake GPS coordinate bhejne ka command. **Yaad rakh: longitude pehle, latitude baad me** — ulta likhne se galat jagah ki location set ho jaati hai (yeh galti humse ek baar hui thi).

**`adb root`** — emulator ko root access dena, jisse Frida jaisa tool system-level pe kaam kar paye. Sirf `google_apis` (non-Play-Store) system image pe kaam karta hai.

**Frida** — ek "dynamic instrumentation" tool — iska matlab hai: kisi running app ke andar jaa kar uske function calls ko runtime pe intercept/modify karna, bina uska source code change kiye. Hum isse baad me T2+ attacks banayenge jo app ko location-check bypass karne ke liye trick karenge.

**`frida-server`** — Frida ka Android-side binary jo emulator/phone ke andar chalta hai, taaki desktop se uske saath connect ho sake.

**Gradle** — Android/Kotlin projects banane ka build tool (jaise npm JavaScript ke liye hai). `gradlew` iska wrapper script hai jo bina Gradle alag se install kiye chalta hai.

---

## Defender-core internals

**Signal** — ek chhota, independent check jo ek location update dekh kar "evidence" deta hai (0.0 = sab theek lagta hai, 1.0 = clearly suspicious hai). Abhi 5 bane hain:
- `MockFlagSignal` — OS ka `isMock` flag check karta hai.
- `PhysicsSignal` — "teleport" detect karta hai (do consecutive points ke beech implausible speed, haversine distance se calculate).
- `RegularitySignal` — location updates ka timing bahut perfect/unvarying toh suspicious (real GPS me natural jitter hota hai).
- `SignalQualitySignal` — accuracy value bahut constant rehna suspicious (real GPS me yeh fluctuate karta hai).
- `ProviderPlausibilitySignal` — provider ("gps"/"network") aur uski accuracy/satellite count aapas me contradict toh suspicious.

**Evidence** — har signal ka individual 0.0–1.0 output, uss specific check ke liye.

**Trust score** — sab signals ke evidence ko combine karke final 0–100 number ("worst signal wins" logic — jo bhi signal sabse zyada suspicious bola, woh final score ko sabse zyada neeche khinchta hai).

**`DefaultLocationTrustEngine`** — woh class jo sab signals ko chalati hai aur final trust score + evidence breakdown deti hai.

**Haversine formula** — do GPS coordinates ke beech actual ground-distance (meters me) nikalne ka standard formula, Earth ki curvature account karke.

**CSV Replay harness** — ek standalone tool (`CsvReplay.kt`) jo ek recorded `gps.csv` file ko row-by-row Defender engine se guzaarta hai aur verdicts print karta hai — isse hume emulator/phone chalaye bina hi real data test karne milta hai.

---

## Attack tiers (threat model se)

| Tier | Matlab |
|---|---|
| **T0** | Developer Options se koi off-the-shelf fake-GPS app use karna — sabse basic attack. |
| **T1** | Same, lekin ek static point ki jagah ek real route follow karte hue. |
| **T2a** | Frida hook use karke `isMock` flag ko chhupana — sloppy tareeke se (baaki signals abhi bhi pakad sakte hain). |
| **T2b** | Same, lekin "clean" tareeke se — saare providers consistent rehte hain, sirf flag hi nahi, poora pattern realistic banane ki koshish. |
| **T3** | Location ke saath-saath fake accelerometer/gyroscope (motion sensor) data bhi fabricate karna. |
| **T4** | Ek real recorded walk/drive ko utha kar kisi aur location pe "remap" kar dena (replay attack). |

---

## Methodology terms (protocol.md se)

**Train/test split** — real-motion data ka ek hissa (train) thresholds tune karne ke liye use hota hai, baaki (test) sirf final results report karne ke liye — taaki numbers "cheat" na ho sakein.

**Frozen defender** — ek baar jab hardest attacks (T3/T4) banane lage, Defender library ka ek fixed/tagged version lock kar diya jata hai — usse live iterate nahi karte, taaki results fair rahe (attacker ko pata na ho defender kya expect kar raha hai).

**Detection Rate** — kisi ek tier ke attacks me se kitne % Defender ne flag kiye.

**False Positive Rate** — genuine (non-spoofed) sessions me se kitne % galti se flag ho gaye.

**Time-to-Detect** — spoofed location app tak pahunchne se lekar Defender ke flag raise karne tak kitna time laga.



## Attack tooling (Phase 6)

**Mock-location test provider** — Android ka built-in API
(`LocationManager.addTestProvider` + `setTestProviderLocation`) jisse koi app
system ko fake GPS location de sakta hai. Yehi mechanism real attacker apps
use karte hain jab "Allow mock locations" Developer Option on hoti hai.

**`appops`** — Android ka permission-control command jisse adb se directly
kisi app ko special permission (jaise mock-location) de sakte hain, bina
Developer Options UI me manually jaake select kiye.

**Foreground service** — ek Android service jo user ko dikhne wali notification
ke saath chalti hai, taaki OS use background me bhi zinda rakhe. Humne shuru
me isse use karne ki koshish ki, lekin naye Android versions me
*location-type* foreground service ko bina app ke "visible" hue adb se start
karna allowed nahi hai — isliye wapas simple background `Service` pe aa gaye,
bas usse start karne se pehle `MainActivity` ko adb se launch kar dete hain
(isse app thodi der ke liye "visible" ho jaata hai, jo plain service start
karne ke liye kaafi hai).

**`adb shell am start-service`** — kisi Android service ko directly command-line
se start karne ka tareeka, bina app UI touch kiye — isi se humara T0 attack
script repeatable/automatable bana.