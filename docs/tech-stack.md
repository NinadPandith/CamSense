# Tech Stack: CamSense

Oct 1, 2026 · @Ninad Pandith M N

This document defines the tools, libraries and project setup for building the app described in Doc.

## Decision: Kotlin native Android

Build CamSense as a native Android app in Kotlin with Jetpack Compose. Flutter is a good choice for normal apps, but this project lives on low-level camera frames, sensors and network sockets, which all run natively.

| Need | Kotlin native | Flutter |
|----|----|----|
| Process every camera frame with OpenCV | Direct, via CameraX ImageAnalysis | Needs platform channels to native code; frame copying slows it down |
| Run the AI model on live frames | Official Android runtime, GPU support | Plugins exist but lag behind and add a layer to debug |
| Magnetometer, BLE, raw sockets | Built-in Android APIs | Plugins, some unmaintained |
| iOS later | Rewrite needed | Shared code, but iOS blocks most scans anyway |
| Debugging | Errors point to your own code | Errors often surface inside plugins |

**Result:** Flutter would mean writing most of the hard parts in Kotlin anyway, plus a bridge. Kotlin removes that layer.

## Stack at a glance

Every MVP feature uses free, open-source tools and runs offline on the phone. Versions below were checked on 1 Oct 2026; pin them in the version catalog at project start and don't upgrade mid-build.

| Layer | Choice | Version / package |
|----|----|----|
| IDE | Android Studio (latest stable) | New-project template defaults for Kotlin, Gradle and Android Gradle Plugin |
| Language | Kotlin with coroutines and Flow | Bundled with Android Studio |
| UI | Jetpack Compose + Material 3 | Compose BOM from the new-project template |
| Architecture | MVVM: ViewModel + StateFlow, manual dependency injection | androidx.lifecycle |
| Camera | CameraX (Preview + ImageAnalysis + camera-compose) | 1.6.2 stable ([release notes](https://developer.android.com/jetpack/androidx/releases/camera)) |
| Image processing | OpenCV official Android AAR | org.opencv:opencv from Maven Central ([announcement](https://opencv.org/enhanced-opencv-for-android-support-arm-performance-gains/)) |
| On-device AI runtime | LiteRT (the new name for TensorFlow Lite) | com.google.ai.edge.litert:litert 2.1.x ([migration guide](https://ai.google.dev/edge/litert/migration)) |
| AI model | Ultralytics YOLO26-nano, fine-tuned, exported to LiteRT | yolo26n ([YOLO26](https://www.ultralytics.com/yolo/yolo26)) |
| Networking | Kotlin sockets + coroutines, NsdManager (mDNS), UDP multicast (SSDP) | Android SDK, no extra library |
| Bluetooth | BluetoothLeScanner | Android SDK |
| Sensors | SensorManager (magnetic field) | Android SDK |
| Local database | Room | androidx.room |
| Settings | DataStore (Preferences) | androidx.datastore |
| Build config | Gradle Kotlin DSL + version catalog (libs.versions.toml) | Template default |
| Version control | Git + GitHub (private repo) | n/a |

**Update to the PRD:** the PRD names YOLOv8-nano. Use YOLO26-nano instead. It is Ultralytics' current model, has no NMS step (simpler Android code) and is faster on CPU. If its export gives trouble, fall back to YOLO11-nano with the same pipeline.

**minSdk:** 29 (Android 10), as set in the PRD. **targetSdk:** the latest SDK Android Studio offers.

## Libraries and APIs per module

Each module maps to a small set of APIs; only M1, M2 and M6 need third-party libraries.

| Module | Main APIs and libraries | Key techniques |
|----|----|----|
| M1 Lens glint | CameraX ImageAnalysis, CameraControl.enableTorch(true), OpenCV imgproc | Grayscale, threshold near 250, findContours, filter by area (2–15 px) and circularity, track centroids across 5+ frames |
| M2 Infrared | CameraX with front lens selector, OpenCV imgproc | Convert to HSV, find bright purple/white blobs in low-light frames; calibration with a TV remote |
| M3 Wi-Fi scan | WifiManager / ConnectivityManager (own IP and subnet), java.net.Socket with timeouts, NsdManager, DatagramSocket for SSDP | Parallel coroutine ping/port sweep of the /24 subnet; check ports 554, 80, 8080, 8000, 1935; read banners; match MAC OUI where visible |
| M4 Bluetooth | BluetoothLeScanner, ScanCallback | 20 s scan; flag strong RSSI (above -60 dBm) and suspicious names |
| M5 Magnetometer | SensorManager, Sensor.TYPE_MAGNETIC_FIELD | Baseline average, magnitude of the field vector, beep rate from delta; ToneGenerator + Vibrator for feedback |
| M6 AI objects | LiteRT Interpreter (or CompiledModel API), optional litert-gpu | Resize frame to model input (640 or 320), run YOLO26n, map boxes back to preview |
| Scoring engine | Plain Kotlin | Weighted sum + agreement bonus from the PRD |
| Evidence mode | CameraX ImageCapture, Room, kotlinx.serialization | Save JPEG + JSON report in app-private storage |
| Scan history | Room | One table for scans, one for findings |
| Vendor lookup data | IEEE OUI list (CSV), bundled in assets | Trimmed to camera and IoT chip makers to keep size small |

**Dependency rule:** add a library only when a module needs it, and note why it was added.

## Android permissions and platform limits

The app needs camera, nearby-devices and network permissions. The biggest recent change: [Android 17 blocks local network access](https://developer.android.com/about/versions/17/behavior-changes-17) for apps targeting API 37 unless they hold the new ACCESS_LOCAL_NETWORK runtime permission, so the Wi-Fi scan fails silently without it.

| Permission | Type | Used by | Notes |
|----|----|----|----|
| CAMERA | Runtime | M1, M2, M6, evidence | Ask at the start of the guided scan |
| ACCESS_LOCAL_NETWORK | Runtime (Nearby devices group) | M3 | Required when targeting API 37+; without it, LAN sockets fail ([guide](https://developer.android.com/privacy-and-security/local-network-permission)) |
| BLUETOOTH_SCAN | Runtime (Android 12+) | M4 | Declare with usesPermissionFlags="neverForLocation" |
| ACCESS_FINE_LOCATION | Runtime | M4 on Android 10–11 only | Declare with maxSdkVersion="30" |
| INTERNET | Normal | M3 | Needed for any socket, even on the LAN |
| ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE | Normal | M3 | Read own IP and subnet |
| CHANGE_WIFI_MULTICAST_STATE | Normal | M3 | MulticastLock for mDNS and SSDP discovery |
| VIBRATE | Normal | M5 | Feedback during the sweep |

The magnetometer needs no permission. Ask for Bluetooth and local network together, since both sit in the Nearby devices group and users see one prompt.

**Platform limits that change the PRD**

- **No MAC addresses of other devices.** Since Android 10, apps cannot read the ARP table, so the MAC vendor lookup in PRD FR-3.2 mostly won't work on-device. Identify devices from mDNS/SSDP names, UPnP description files and HTTP banners instead; keep the OUI list only for BLE devices that expose a public address.

- **Foreground only.** Camera access stops when the app is in the background, so every scan runs with the app open.

- **Front-camera IR varies by phone.** Keep the calibration step from the PRD.

- **The emulator can't test this app.** Camera, torch, magnetometer and BLE need a real phone connected over USB or wireless debugging.

## Project folder structure

One Git repo holds the Android app, the Python prototypes and the model-training code, with one folder per detection module so each module can be built and tested on its own.

camsense/\
├── README.md\
├── gradle/libs.versions.toml \# every dependency version, pinned\
├── app/\
│ ├── build.gradle.kts\
│ └── src/\
│ ├── main/\
│ │ ├── AndroidManifest.xml\
│ │ ├── assets/\
│ │ │ ├── models/hiding_objects_yolo26n.tflite\
│ │ │ ├── models/labels.txt\
│ │ │ └── oui/iot_vendors.csv\
│ │ └── java/com/camsense/\
│ │ ├── MainActivity.kt\
│ │ ├── CamSenseApp.kt \# AppContainer (manual dependency injection)\
│ │ ├── core/\
│ │ │ ├── DetectionModule.kt \# shared interface\
│ │ │ ├── ModuleResult.kt\
│ │ │ └── permissions/\
│ │ ├── modules/\
│ │ │ ├── glint/ \# GlintAnalyzer, BlobTracker\
│ │ │ ├── infrared/ \# IrAnalyzer, IrCalibration\
│ │ │ ├── network/ \# SubnetScanner, PortProber, MdnsDiscovery, SsdpDiscovery\
│ │ │ ├── bluetooth/ \# BleScanner\
│ │ │ ├── magnetometer/ \# MagnetSweep\
│ │ │ └── objects/ \# ObjectDetector, YoloPostProcessor\
│ │ ├── scoring/ \# RiskEngine\
│ │ ├── data/ \# Room database, EvidenceStore, ScanRepository\
│ │ └── ui/\
│ │ ├── scan/ \# one screen per guided-scan step\
│ │ ├── results/\
│ │ ├── history/\
│ │ ├── components/ \# overlays, buttons, beep meter\
│ │ └── theme/\
│ ├── test/ \# unit tests (JVM)\
│ └── androidTest/ \# device tests\
├── ml/\
│ ├── data.yaml\
│ ├── train.py\
│ ├── export.py\
│ ├── requirements.txt\
│ ├── notebooks/train_colab.ipynb\
│ └── dataset/ \# not committed; lives in Roboflow / Drive\
└── prototype/\
├── glint_prototype.py\
├── ir_prototype.py\
└── requirements.txt

## How the code fits together

Every module implements one shared interface and publishes its result as a Flow; the RiskEngine combines those flows and the UI just displays the latest state.

**The shared interface.** Write this first and give it to the AI in every module session, so all six modules plug in the same way.

interface DetectionModule {\
val id: ModuleId // GLINT, INFRARED, NETWORK, BLE, MAGNET, OBJECTS\
val weight: Float // from the PRD scoring table\
fun isSupported(): Boolean // e.g. IR calibration failed -\> false\
val results: StateFlow\<ModuleResult\>\
suspend fun start()\
fun stop()\
}\
\
data class ModuleResult(\
val moduleId: ModuleId,\
val status: Status, // IDLE, RUNNING, DONE, SKIPPED, FAILED\
val score: Float, // 0.0 to 1.0\
val findings: List\<Finding\>\
)\
\
data class Finding(\
val reason: String, // plain words shown to the user\
val confidence: Float,\
val targetKey: String?, // same object seen by several modules, e.g. "object-3"\
val screenBox: RectF? = null, // for camera modules\
val deviceAddress: String? = null // for network and BLE modules\
)

The targetKey is what makes the agreement bonus work: when M1, M5 and M6 all tag the same object, the RiskEngine adds the bonus.

**Where each part runs**

| Part | Thread / dispatcher | Pattern |
|----|----|----|
| Camera analyzers (M1, M2, M6) | CameraX analysis executor (one background thread) | ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST; only one camera module runs per scan step |
| Network scan (M3) | Dispatchers.IO, parallelism capped at 64 | One coroutine per IP/port with a 300 ms timeout |
| Bluetooth (M4) | Callback thread | ScanCallback wrapped in callbackFlow |
| Magnetometer (M5) | Sensor thread | SensorEventListener wrapped in callbackFlow, SENSOR_DELAY_GAME |
| RiskEngine | Dispatchers.Default | combine() over all module result flows |
| UI | Main thread | ViewModel exposes StateFlow; Compose uses collectAsStateWithLifecycle |

**Frame pipeline for camera modules**

1.  CameraX delivers an ImageProxy (YUV) to the analyzer.

2.  Convert the Y plane to an OpenCV Mat (grayscale is enough for glint and IR).

3.  Run the module's detection and update its tracker.

4.  Publish findings with screen coordinates mapped to the preview.

5.  Close the ImageProxy, or CameraX stops sending frames.

## AI model training stack

Train in Google Colab with the Ultralytics Python package, then export one .tflite file that drops into app/src/main/assets/models/.

| Step | Tool |
|----|----|
| Collect and label images | Phone camera + Roboflow (free tier), YOLO format export |
| Starter data | Roboflow Universe, Open Images (filter to the 8 classes) |
| Training environment | Google Colab (free GPU), Python 3.11+ |
| Training library | ultralytics (pip), starting from yolo26n.pt |
| Experiment notes | A simple results table in the repo README (mAP, epochs, image size) |
| Export | model.export(format="litert") for float, or with quantize=8 for a smaller INT8 model ([export guide](https://docs.ultralytics.com/integrations/litert)) |
| Quick check before the app | Load the .tflite in Python with ultralytics and run it on test photos |

**Export command**

from ultralytics import YOLO\
model = YOLO("runs/detect/train/weights/best.pt")\
model.export(format="litert", imgsz=640, quantize=8, data="data.yaml")

**Tips**

- Start at imgsz=640. If the phone runs below 15 fps, retrain or export at 320.

- Keep labels.txt in the same class order as data.yaml, or the app will show wrong names.

- Ultralytics code is AGPL-3.0 licensed. That's fine for a student project; check the licence before any commercial release.

## Development environment setup

1.  **Install tools:** Android Studio (latest stable), Git and Python 3.11+.

2.  **Prepare the test phone:** enable Developer options, turn on USB debugging (or wireless debugging), and connect it to Android Studio.

3.  **Prepare the "spy camera" phone:** install IP Webcam or DroidCam on the old phone and connect both phones to the same hotspot.

4.  **Set up Python:** in prototype/ and ml/, create a virtual environment and install the packages in requirements.txt.

**Minimum laptop:** 8 GB RAM works; 16 GB makes Android Studio much smoother. Model training runs in Colab, so no GPU is needed.

## Coding conventions

- Kotlin official style; Android Studio's formatter on save.

- One class per file; file name matches the class.

- Each module lives only in its own folder under modules/ and talks to the rest of the app only through DetectionModule.

- All tuning numbers (thresholds, weights, timeouts) go in one ScanConfig.kt file, never scattered in code.

- Every Finding.reason is plain English a non-technical user understands.

- No network calls to the internet; only the local subnet.

- Log with a tag per module (e.g. "CamSense/Glint").

## Testing tools

Unit-test the pure logic on the laptop and test everything sensor-related on real phones, following the test table in the PRD.

| What | Tool | Examples |
|----|----|----|
| Pure logic | JUnit 4 + kotlinx-coroutines-test | RiskEngine weights and agreement bonus, BlobTracker, YoloPostProcessor |
| Glint and IR thresholds | Python prototypes on saved photos and videos | Record dark-room clips once, replay them after every threshold change |
| UI flow | Compose UI tests (androidTest) | Guided scan moves through all six steps; skipped modules show on results |
| Sensors and camera | Manual tests on 3+ real phones | The PRD test table: distances, clean rooms, IR remote |
| Performance | Android Studio Profiler + an FPS counter overlay | 15 fps target on a mid-range phone |
| Network scan check | Laptop browser on the hidden phone's stream URL | Confirms the "spy camera" is really streaming before a test run |

## Sources

Pages checked on 1 Oct 2026:

- [CameraX release notes](https://developer.android.com/jetpack/androidx/releases/camera) — 1.6.2 stable

- [LiteRT migration guide](https://ai.google.dev/edge/litert/migration) — TensorFlow Lite renamed, Maven package com.google.ai.edge.litert

- [LiteRT releases on GitHub](https://github.com/google-ai-edge/LiteRT/releases) — 2.1.x line

- [OpenCV on Maven Central](https://opencv.org/enhanced-opencv-for-android-support-arm-performance-gains/) — official Android AAR

- [Ultralytics YOLO26](https://www.ultralytics.com/yolo/yolo26) and [LiteRT export guide](https://docs.ultralytics.com/integrations/litert)

- [Android 17 behaviour changes](https://developer.android.com/about/versions/17/behavior-changes-17) and [local network permission guide](https://developer.android.com/privacy-and-security/local-network-permission)
