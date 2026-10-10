# CamSense

[![CI](https://github.com/NinadPandith/CamSense/actions/workflows/ci.yml/badge.svg)](https://github.com/NinadPandith/CamSense/actions/workflows/ci.yml)
![Platform](https://img.shields.io/badge/platform-Android%2010%2B-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF)
![License](https://img.shields.io/badge/license-MIT-blue)

**Find hidden cameras with nothing but your phone.** CamSense is an Android app that
checks a hotel room, rental or changing room for hidden cameras using only the phone's
own sensors: camera, flashlight, Wi-Fi, Bluetooth and magnetometer. Six detection
modules run in a guided five-minute scan and combine into one **Low / Medium / High**
result, with a plain-English reason for every alert.

Everything runs on the device. No account, no cloud, nothing uploaded.

## How it works

| Check | How | Weight |
|---|---|---|
| M1 Lens glint | Torch + rear camera, local-contrast spot tracking, flash-blink test | 0.30 |
| M3 Wi-Fi | /24 port sweep (554, 80, 8080, 8000, 1935, 4747, 8081), banners, mDNS, SSDP, ONVIF, camera hotspots nearby | 0.25 |
| M2 Infrared | Front camera in the dark, after a TV-remote calibration | 0.15 |
| M6 AI objects | YOLO26n (LiteRT) marks likely hiding spots | 0.10 |
| M4 Bluetooth | 20 s BLE scan, camera-like names, very close unknown devices | 0.10 |
| M5 Magnetometer | Beeps faster near electronics; confirmation only | 0.10 |


**Risk score** = Σ weight × module score, plus a 0.2 bonus when two modules flag the
same object, capped at 1. On top of that, a confirmed video stream on the Wi-Fi means at
least High, and a strong lens glint at least Medium (`scoring/RiskEngine.kt`).

### Cutting false alarms

Reflections are the hard part of lens detection. A spot only counts as a lens if it:

1. stands out from its surroundings (local contrast, not a fixed brightness cut-off),
2. is not on a lit surface such as a screen or a lamp,
3. **vanishes when the flashlight blinks off** for 0.3 s (a lamp or LED stays lit), and
4. **moves with the scene** when the phone moves (a reflection on glass slides against it,
   measured with phase correlation).

Infrared glows must be white or purple, so coloured standby LEDs are ignored.

## Features

- **Three scan types:** full scan, a one-minute quick check-in, and a bathroom or
  changing-room mode with a two-way-mirror fingertip test.
- **Room coverage meter:** the rotation-vector sensor tracks where the camera pointed;
  a ring shows how much of the room is checked and which way to turn.
- **Room map:** each flagged spot is placed around where you stood ("Behind, to the left").
- **Evidence and PDF report:** annotated photos, devices found and SHA-256 hashes, shared
  through the Android share sheet. High results offer "Send to someone I trust".
- **Hot–cold finder:** the magnetometer step says warmer or colder as you sweep a suspect object.
- **Spoken guidance:** offline text-to-speech, so you can keep your eyes on the room.
- **Learns your devices:** marking a Wi-Fi or Bluetooth device as fine stops later scans flagging it.
- **Since last time:** a repeat scan of the same place lists devices that are new.
- **Accessible:** large touch targets, screen-reader descriptions, every check explains why it was skipped.

## Tech stack

| Area | Choice |
|---|---|
| Language and UI | Kotlin, Jetpack Compose, Material 3, MVVM |
| Camera | CameraX 1.6 (ImageAnalysis), OpenCV 4 |
| On-device AI | YOLO26n exported to LiteRT (TensorFlow Lite) |
| Storage | Room (with migrations), DataStore |
| Networking | Raw sockets, mDNS, SSDP, ONVIF WS-Discovery (local subnet only) |
| Reports | `android.graphics.pdf.PdfDocument`, FileProvider |
| Prototyping and ML | Python, OpenCV, Ultralytics, pytest |
| CI | GitHub Actions: unit tests, debug APK, prototype tests |

## Project structure

```
app/src/main/java/com/camsense/
  core/        DetectionModule interface, results, sensors, permissions, camera helpers
  modules/     glint, infrared, network, bluetooth, magnetometer, objects
  scoring/     ScanConfig (every tuning number), RiskEngine
  data/        Room database, repository, evidence files, PDF report, settings
  ui/          navigation, screens, components, theme
app/src/test/  JVM unit tests for trackers, scoring, post-processing and feature logic
prototype/     Python glint and IR prototypes (same algorithm and numbers as the app)
ml/            YOLO training notebook and LiteRT export
docs/          requirements, app flow, schema, content guidelines, design notes
```

## Build and run

**Requirements:** Android Studio (latest stable) and a real phone with Android 10 or
newer. The emulator has no torch, magnetometer or Bluetooth, so it can't test this app.

1. Clone the repository and open it in Android Studio. Let Gradle sync.
2. Turn on USB debugging on the phone and connect it.
3. Run the `app` configuration.

**Windows shortcut:** with Android Studio installed and the phone connected, double-click
`run.bat`. It finds Java and the SDK, builds, installs and opens the app, and shows its log.

### Demo setup

Install IP Webcam or DroidCam on an old phone, start its server and join both phones to
the same Wi-Fi. If the network check finds nothing, the hotspot may block device-to-device
traffic; use a home router or a hotspot with client isolation off.

## Tests

```bash
./gradlew testDebugUnitTest                # JVM unit tests
cd prototype && pip install -r requirements.txt && pytest
```

Glint thresholds are tuned on recorded clips with `prototype/glint_prototype.py --report clips/*.mp4`
and copied into `scoring/ScanConfig.kt`.

## AI model

`app/src/main/assets/models/` ships the stock YOLO26n model (COCO, float32, input
[1, 3, 640, 640]). The app only shows classes that commonly hide cameras (clock, book, TV,
phone, vase, plant pot). A custom model can be trained with `ml/train.py` or
`ml/notebooks/train_colab.ipynb` and exported with `ml/export.py`; it drops in with no code changes.

## Privacy

Camera frames are processed in memory and never stored, except lens-glint snapshots, which
stay in app-private storage. Evidence is saved only when you tap "Save evidence". App data
is excluded from backups. The Wi-Fi check only scans the network the phone is on and only
reads what a service sends back to a plain request; it never logs in.

## Limitations

No phone can catch everything. A wired camera without night vision that records to a
memory card gives off no Wi-Fi, Bluetooth or infrared signal. CamSense raises the chance of
finding a camera; a Low result means no clear signs, not a guarantee.

## Documentation

[Product requirements](docs/prd.md) · [App flow](docs/app-flow.md) ·
[Tech stack](docs/tech-stack.md) · [Database schema](docs/backend-schema.md) ·
[Content guidelines](docs/content-guidelines.md) · [Design notes](docs/design-notes.md)

## License

MIT, see [LICENSE](LICENSE). The bundled YOLO model is from Ultralytics and is AGPL-3.0;
check its licence before any commercial use. Fonts (Space Grotesk, Inter, JetBrains Mono)
are under the SIL Open Font License, see `licenses/`.
