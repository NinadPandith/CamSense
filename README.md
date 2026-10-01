# CamSense

Android app that checks a room for hidden cameras using only the phone's own
sensors. Six checks run in a guided five-minute scan and combine into one
Low / Medium / High result, with the reason for every alert. Everything runs on
the phone; nothing is uploaded.

| Check | How | Weight |
|---|---|---|
| M1 Lens glint | Torch + rear camera, OpenCV blob tracking | 0.30 |
| M3 Wi-Fi | /24 port sweep (554, 80, 8080, 8000, 1935, 4747, 8081), banners, mDNS, SSDP | 0.25 |
| M2 Infrared | Front camera in the dark, after a TV-remote calibration | 0.15 |
| M6 AI objects | YOLO26n (LiteRT) marks likely hiding spots | 0.10 |
| M4 Bluetooth | 20 s BLE scan, camera-like names, very close unknown devices | 0.10 |
| M5 Magnetometer | Beeps faster near electronics; confirmation only | 0.10 |

Risk = Σ weight × score + 0.2 when two modules flag the same object, capped at 1.
On top of that, a confirmed video stream on the Wi-Fi means at least High, and a
strong lens glint at least Medium (see `scoring/RiskEngine.kt`).

Docs: [PRD](docs/prd.md) · [App flow](docs/app-flow.md) · [Tech stack](docs/tech-stack.md) ·
[Content guidelines](docs/content-guidelines.md) · [Backend schema](docs/backend-schema.md) ·
[Implementation plan](docs/implementation-plan.md)

## Build and run

**Quickest (Windows):** install [Android Studio](https://developer.android.com/studio) and open it
once so it downloads the SDK. Download this branch as a ZIP, unzip it, connect your phone with
USB debugging on, and double-click `run.bat`. It finds Java and the SDK, builds, installs and opens
the app, then shows the app's log (saved to `app-log.txt`; build output goes to `build-log.txt`).

**In Android Studio:**

1. Open the folder in Android Studio (latest stable) and let Gradle sync.
   Versions are pinned in `gradle/libs.versions.toml`. The Google-hosted ones (AGP,
   AndroidX, LiteRT) couldn't be re-checked from the machine this was written on; if
   sync says a version doesn't exist, take the one Android Studio suggests.
2. Connect a real phone (Android 10+) with USB or wireless debugging. The emulator
   has no torch, magnetometer or BLE, so it can't test this app.
3. Run the `app` configuration.

### AI model

`app/src/main/assets/models/` ships the stock YOLO26n model (COCO, float32, 10 MB,
input [1, 3, 640, 640]) exported with `ml/export.py yolo26n.pt --stock`. The app shows
only the classes in `ScanConfig.HIDING_OBJECTS` (clock, book, TV, phone, vase, plant pot).

On Day 6–7, train the 8-class model with `ml/train.py` or `ml/notebooks/train_colab.ipynb`
and export it with `python export.py runs/detect/train/weights/best.pt` (INT8, calibrated on
your dataset). It replaces the same two files; no code changes are needed. If the model is
missing or fails to load, the room sweep is skipped and Results says so.

Ultralytics models are AGPL-3.0: fine for a student project, check before any commercial release.

### Demo setup

Install IP Webcam on an old phone, start its server, and join both phones to the same
hotspot. If the network check finds nothing, the hotspot may block device-to-device
traffic: use a home router or a laptop hotspot with client isolation off.

## Tests

```bash
./gradlew testDebugUnitTest          # RiskEngine, trackers, YOLO post-processing, classifiers
cd prototype && pip install -r requirements.txt && pytest
```

Tune glint thresholds on recorded clips with `prototype/glint_prototype.py --report clips/*.mp4`,
then copy the values into `scoring/ScanConfig.kt`. The prototype uses the same algorithm
and default numbers as the app.

## Project layout

```
app/src/main/java/com/camsense/
  core/        DetectionModule interface, results, permissions, camera helpers
  modules/     glint, infrared, network, bluetooth, magnetometer, objects
  scoring/     ScanConfig (every tuning number), RiskEngine
  data/        Room database, repository, evidence files, settings
  ui/          navigation, screens, components, Charcoal lime theme
prototype/     Python glint and IR prototypes
ml/            YOLO training and LiteRT export
tools/offline-check/  compile check for machines without Google Maven
```

## Privacy

Camera frames are processed in memory and never stored, except glint snapshots,
which stay in app-private storage. Evidence (photos, `report.json` and their SHA-256
hashes) is saved only when you tap "Save evidence". Backups and device transfer are
turned off for app data. The Wi-Fi check only scans the network the phone is on,
and only reads what a service sends back to a plain request; it never logs in.

Fonts: Space Grotesk, Inter and JetBrains Mono, SIL Open Font License (see `licenses/`).
