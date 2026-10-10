# Design notes

Decisions made while building and testing CamSense on real phones, on top of the
[PRD](prd.md) and [tech stack](tech-stack.md). Every tuning number mentioned here lives in
`scoring/ScanConfig.kt`.

## Architecture

- Kotlin, Jetpack Compose, MVVM. Manual dependency injection through `AppContainer`
  (`CamSenseApp.kt`); no DI framework.
- Each detection module lives in `modules/<name>/` and implements
  `core/DetectionModule.kt` (usually through `core/BaseModule.kt`), so modules can be
  built, tested and skipped independently.
- Everything runs on the phone. The only network traffic is to the local subnet.
- minSdk 29 (Android 10).

## Decisions

- Risk floors: a confirmed video stream on the Wi-Fi (network score >= 0.85) lifts the
  result to at least High; a strong lens glint (>= 0.8) to at least Medium. With the PRD
  weights alone, a streaming phone would only reach Low. See RiskEngine.kt.
- Agreement linking: the object detector also runs on every 6th lens-check frame; a
  glint inside an object's box takes that object's target_key ("object-N").
- Wi-Fi MAC vendor lookup is not possible on Android 10+; devices are identified by
  ports, banners, mDNS and SSDP names. The OUI list is used for Bluetooth only.
- Ports checked: 554, 80, 8080, 8000, 1935, plus 4747 (DroidCam) and 8081.
- Glint snapshots are saved as files/evidence/<scanId>/<findingId>.jpg during the scan;
  they become evidence items (with SHA-256) only when the user taps "Save evidence".
- "Check flagged spots" on a Medium result opens recheck/{scanId}: a magnetometer-only
  pass that replaces the MAGNET run and re-scores the saved scan.
- Glint spots use local contrast (pixel minus the mean of a box around it), not a fixed
  brightness cut-off; the strongest 25 per frame are kept. The lens check lowers exposure.
- Flash-blink test: each confirmed glint gets the torch switched off for ~0.3 s
  (FlashCheck). Vanishes = lens (confidence 0.92); stays lit = lamp/LED (dropped).
  FlashScheduler limits it: one test per 2 s, 6 per lens check, never twice on the same
  place (places follow the scene as the phone pans), 12 settle frames after each blink.
  Found on a real phone: with 4 settle frames the torch blinked in a loop.
- Lens check hands frames to the AI model on its own thread (ObjectsModule.submit).
- CameraPreview only unbinds its own use cases. Screens overlap during navigation, and
  unbindAll() from the screen being left froze the next screen's camera. A watchdog
  binds again if no frame arrives for 2.5 s.
- Wi-Fi check also: ONVIF WS-Discovery (camera answers = 0.9), and nearby Wi-Fi networks
  with camera-like names/makers (works without joining; NEARBY_WIFI_DEVICES on 13+,
  fine location before). Hotspots are saved in network_devices with ip = BSSID.
- Nearby unknown Bluetooth devices are one grouped finding; "very close" is > -50 dBm.
- Glints on a lit surface (box mean > 120) are skipped. Angle test (ParallaxCheck +
  SceneMotion phase correlation): a spot that slides against the scene's motion is a
  reflection on glass and is dropped, even after passing the flash test. Found on a real
  phone: a tablet screen's flash reflection passed the flash test.
- IR glows must be white/purple (U and V not below 128 - 12); coloured standby LEDs are
  ignored. Repeat sightings within 0.08 of an earlier glow merge; max 3 IR findings.
- Results groups findings with the same check and reason into one row ("4 spots").
- Debug builds show a line of camera stats (fps, brightness, spots) on camera steps,
  and log it once a second (tags CamSense/Glint, CamSense/Objects, CamSense/Infrared).
- Database version 2 (MIGRATION_1_2 in CamSenseDatabase.kt): scans.mode/coverage/
  start_yaw, findings.yaw/pitch/device_address. New columns are nullable and come last.
- Scan modes (core/ScanMode.kt, ui/scan/ScanStep.kt): FULL; QUICK = place, Wi-Fi/BT,
  lens (IR, objects, magnet skipped as NOT_IN_THIS_SCAN); SMALL_ROOM = mirror step
  instead of the room sweep. Mirror "no gap" is an OBJECTS finding (0.6).
- Coverage: OrientationSource (rotation vector) feeds CoverageGrid while a camera step
  is open; 12 yaw sectors x 3 pitch bands, middle band counts double. Findings get a
  yaw/pitch at save time; the Results room map is relative to the scan's start_yaw.
- Hot-cold (HotCold.kt): fast/slow average trend of the magnet delta. A phone
  magnetometer can't give direction, so it only says warmer/colder/passed the peak.
- Voice: TextToSpeech, English only (user's choice), off by default, Settings switch.
- PDF report (data/report/PdfReportWriter.kt) is written when evidence is saved and is
  a REPORT_PDF evidence item; shared via FileProvider (${applicationId}.files).
- Safe devices: "Checked, it's fine" stores device signatures in DataStore (no other
  database, as the user asked); later scans drop those findings before scoring.
  Light spots are never learned. "Since last time" compares with the last completed
  scan of the same place name.
- The grouped "unknown Bluetooth devices very close" finding stores every address,
  comma-separated, in device_address. Marking it safe remembers all of them. A later
  scan removes only the remembered ones and rewrites the count. Devices that rotate
  random addresses (most phones and earbuds) can still come back as new.

## Field testing

Tested on a OnePlus 15 with a tablet and an old phone acting as hidden cameras. Several
of the decisions above (flash-test settle frames, the angle test, the IR colour filter,
the camera rebind watchdog) came directly from failures seen in those tests.
