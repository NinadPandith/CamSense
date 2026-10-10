package com.camsense.scoring

import com.camsense.core.ModuleId

/**
 * Every tuning number in the app lives here: weights, thresholds, timeouts.
 * The PRD calls these "starting values"; change them here after testing and log
 * each change in the commit message.
 */
object ScanConfig {

    // ---- Risk scoring (PRD "Risk scoring logic") ----
    val WEIGHTS: Map<ModuleId, Float> = mapOf(
        ModuleId.GLINT to 0.30f,
        ModuleId.NETWORK to 0.25f,
        ModuleId.INFRARED to 0.15f,
        ModuleId.OBJECTS to 0.10f,
        ModuleId.BLE to 0.10f,
        ModuleId.MAGNET to 0.10f,
    )
    fun weightOf(id: ModuleId): Float = WEIGHTS.getValue(id)

    const val AGREEMENT_BONUS = 0.2f
    /** Below this is Low. */
    const val LOW_MAX = 0.3f
    /** Up to and including this is Medium; above is High. */
    const val MEDIUM_MAX = 0.6f

    // Minimum levels for one strong signal (decision on top of the PRD formula:
    // with weights alone, a phone streaming video on the Wi-Fi would only reach Low).
    /** Network score at or above this means a device is confirmed to stream video. */
    const val NETWORK_STREAM_SCORE = 0.85f
    /** A confirmed video stream lifts the result to at least this score (High). */
    const val NETWORK_STREAM_FLOOR = 0.65f
    /** Glint score at or above this lifts the result to at least Medium. */
    const val GLINT_STRONG_SCORE = 0.8f
    const val GLINT_STRONG_FLOOR = 0.35f

    /** A module's score at or above this counts as "fired" in reports and the Low copy. */
    const val MODULE_FIRED_SCORE = 0.5f

    // ---- Guided scan: minimum time before each step's button unlocks (App Flow) ----
    const val NETWORK_STEP_MIN_MS = 10_000L
    const val ROOM_SWEEP_MIN_MS = 20_000L
    const val IR_STEP_MIN_MS = 15_000L
    const val LENS_STEP_MIN_MS = 20_000L
    /** After going to the background, the network scan may keep running this long. */
    const val BACKGROUND_NETWORK_GRACE_MS = 30_000L

    // ---- M3 Wi-Fi scan ----
    /** RTSP, HTTP, HTTP-alt, ONVIF/HTTP, RTMP, DroidCam, MJPEG-streamer. */
    val CAMERA_PORTS = listOf(554, 80, 8080, 8000, 1935, 4747, 8081)
    /** Extra ports used only to notice that a host is alive. */
    val LIVENESS_PORTS = listOf(443, 53, 62078)
    val HTTP_PORTS = setOf(80, 8080, 8000, 4747, 8081)
    const val RTSP_PORT = 554
    const val RTMP_PORT = 1935
    const val NETWORK_PARALLELISM = 64
    const val SOCKET_TIMEOUT_MS = 300
    const val BANNER_TIMEOUT_MS = 800
    const val BANNER_MAX_CHARS = 200
    const val MDNS_LISTEN_MS = 6_000L
    const val SSDP_LISTEN_MS = 3_000L
    /** Words in a banner, mDNS or SSDP name that mean "this is a camera or stream". */
    val STREAM_KEYWORDS = listOf(
        "ip webcam", "droidcam", "rtsp/1.0", "multipart/x-mixed-replace", "mjpeg", "mjpg",
        "onvif", "hikvision", "dahua", "ipcam", "ip camera", "netcam", "webcam", "video stream",
        "live stream", "streaming", "camera", "dvr", "nvr", "xmeye", "v380", "yoosee", "tuya",
        "espcam", "esp32-cam", "wansview", "reolink", "foscam", "amcrest", "wyze", "ezviz", "imou",
    )
    /** Suspicion scores for network devices (0–1). */
    const val NET_SCORE_STREAM = 0.9f          // stream confirmed by banner or RTSP reply
    const val NET_SCORE_VIDEO_PORT = 0.6f      // RTSP/RTMP port open, no banner
    const val NET_SCORE_CAMERA_NAME = 0.6f     // camera-like mDNS/SSDP/host name
    const val NET_SCORE_HTTP_ALT = 0.3f        // 8080/8000/4747/8081 open, unknown service
    const val NET_SCORE_HTTP_ONLY = 0.1f       // only port 80
    const val NET_FINDING_MIN = 0.3f
    /** Answers an ONVIF probe for video transmitters: almost always an IP camera. */
    const val NET_SCORE_ONVIF = 0.9f
    const val ONVIF_LISTEN_MS = 3_000L

    // Camera hotspots: spy cameras not joined to the room's Wi-Fi broadcast their own.
    const val HOTSPOT_SCAN_MS = 8_000L
    const val HOTSPOT_SCORE_CAMERA_NAME = 0.75f
    const val HOTSPOT_SCORE_CAMERA_VENDOR = 0.6f
    /** Whole words in a network name that point to a camera. */
    val HOTSPOT_CAMERA_WORDS = listOf(
        "ipc", "ipcam", "ipcamera", "camera", "cam", "webcam", "wificam", "hdwificam", "minicam",
        "spycam", "spy", "dvr", "nvr", "v380", "yoosee", "icsee", "xmeye", "lookcam", "camhi",
        "camhipro", "p2pcam", "carecam", "ezviz", "imou", "wyze", "reolink", "hikvision", "dahua",
    )

    // ---- M4 Bluetooth ----
    const val BLE_SCAN_MS = 20_000L
    /** Stronger than this (dBm) means within about a metre; -60 flagged earbuds and watches. */
    const val BLE_STRONG_RSSI = -50
    val BLE_CAMERA_KEYWORDS = listOf("cam", "ipc", "spy", "dvr", "lens", "video", "a9", "x5", "v380", "yi ")
    const val BLE_SCORE_CAMERA_NAME = 0.7f
    const val BLE_SCORE_CAMERA_VENDOR = 0.5f
    const val BLE_SCORE_STRONG_UNKNOWN = 0.3f
    const val BLE_FINDING_MIN = 0.3f

    // ---- M5 Magnetometer ----
    const val MAG_BASELINE_MS = 3_000L
    /** Field rise (µT above baseline) where beeping starts. */
    const val MAG_BEEP_START_UT = 5f
    /** Field rise where beeping is fastest. */
    const val MAG_BEEP_MAX_UT = 60f
    const val MAG_SLOWEST_BEEP_MS = 900L
    const val MAG_FASTEST_BEEP_MS = 80L
    /** Rise needed before a spot is reported as a finding. */
    const val MAG_FINDING_UT = 15f
    /** Rise that maps to a score of 1.0. */
    const val MAG_FULL_SCORE_UT = 50f

    // ---- M1 Lens glint (tune on prototype/glint_prototype.py first) ----
    const val GLINT_TARGET_WIDTH = 1280
    const val GLINT_TARGET_HEIGHT = 720
    /** Spot must reach this brightness (0–255)... */
    const val GLINT_MIN_LUMA = 200.0
    /** ...and stand this far above the mean of the box around it. */
    const val GLINT_MIN_CONTRAST = 70.0
    /** Box for that mean, in pixels: about twice the largest spot. */
    const val GLINT_WINDOW_PX = 31
    const val GLINT_MIN_DIAMETER_PX = 2f
    const val GLINT_MAX_DIAMETER_PX = 15f
    const val GLINT_MIN_CIRCULARITY = 0.7f
    /** Shortest side / longest side of a blob's box; lower is a streak, not a point. */
    const val GLINT_MIN_ASPECT = 0.5f
    /** Blobs smaller than this area skip the circularity test (perimeter is unreliable). */
    const val GLINT_CIRCULARITY_MIN_AREA = 12.0
    /** Strongest spots kept per frame; a busy frame never drops the lens. */
    const val GLINT_MAX_SPOTS = 25
    const val GLINT_CONFIRM_FRAMES = 5
    const val GLINT_MAX_MISSED_FRAMES = 3
    /** Max centroid jump between frames, as a fraction of the frame width. */
    const val GLINT_MATCH_DISTANCE = 0.04f
    /** A confirmed spot's diameter may not vary more than this ratio (max/min). */
    const val GLINT_MAX_SIZE_RATIO = 2.5f
    /** Mean frame brightness above this shows "too bright" (the torch alone lights close walls). */
    const val LENS_TOO_BRIGHT_LUMA = 140.0
    /** Lens check exposure: this fraction of the lowest compensation, so reflections stand out. */
    const val LENS_EXPOSURE_BIAS = -0.5f
    /** Hand every Nth lens-check frame to the object detector (on its own thread). */
    const val LENS_OBJECT_EVERY_N_FRAMES = 6
    /** Glint confidence for a confirmed track; grows with how long it was tracked. */
    const val GLINT_BASE_CONFIDENCE = 0.7f
    const val GLINT_FULL_CONFIDENCE_FRAMES = 20

    /** Surroundings brighter than this (box mean, 0–255) mean a lit screen or wall, not a lens. */
    const val GLINT_MAX_SURROUND = 120.0

    val GLINT_SPOTS = com.camsense.core.camera.SpotParams(
        minLuma = GLINT_MIN_LUMA, minContrast = GLINT_MIN_CONTRAST, windowPx = GLINT_WINDOW_PX,
        minDiameter = GLINT_MIN_DIAMETER_PX, maxDiameter = GLINT_MAX_DIAMETER_PX,
        minCircularity = GLINT_MIN_CIRCULARITY, circularityMinArea = GLINT_CIRCULARITY_MIN_AREA,
        minAspect = GLINT_MIN_ASPECT, maxSpots = GLINT_MAX_SPOTS, maxSurround = GLINT_MAX_SURROUND,
    )

    // Angle test: a lens moves with the scene; a reflection on flat glass slides across it.
    /** Width of the small copy used to measure how far the whole picture moved. */
    const val SCENE_MOTION_WIDTH = 160
    /** Total scene movement (fraction of the frame) needed before judging a spot. */
    const val PARALLAX_MIN_SCENE_MOTION = 0.04f
    /** A spot sliding more than this share of the scene's movement is a reflection. */
    const val PARALLAX_MAX_RATIO = 0.5f

    // Flash-blink test: once a glint is confirmed, the torch goes off for a moment.
    // A lens reflection vanishes with it; a lamp or LED stays lit.
    /** Frames to wait after switching the torch before measuring (camera latency). */
    const val FLASH_LATENCY_FRAMES = 3
    /** Frames measured with the torch off. */
    const val FLASH_SAMPLE_FRAMES = 4
    /**
     * Frames to let exposure settle after the torch is back on (~0.4 s). With only 4,
     * the over-bright frames after the blink made new "spots" that started new tests:
     * the torch blinked in a loop on a real phone.
     */
    const val FLASH_SETTLE_FRAMES = 12
    /** At most one flash test per this many milliseconds... */
    const val FLASH_COOLDOWN_MS = 2_000L
    /** ...and at most this many per lens check. */
    const val FLASH_MAX_TESTS = 6
    /** A spot this close (fraction of frame) to an already tested place reuses its result. */
    const val FLASH_SAME_PLACE_DISTANCE = 0.06f
    /** Torch-off brightness / torch-on brightness at or below this: a lens reflection. */
    const val FLASH_LENS_RATIO = 0.5
    /** At or above this: a light source, not a reflection (finding dropped). */
    const val FLASH_LIGHT_RATIO = 0.8
    /** Confidence of a glint that passed the flash-blink test. */
    const val FLASH_LENS_CONFIDENCE = 0.92f
    /** Pixels around a spot searched for its brightness. */
    const val FLASH_SAMPLE_RADIUS_PX = 6

    // ---- M2 Infrared ----
    const val IR_DARK_MAX_LUMA = 60.0
    const val IR_MIN_DIAMETER_PX = 2f
    const val IR_MAX_DIAMETER_PX = 25f
    const val IR_CONFIRM_FRAMES = 4
    const val IR_CALIBRATION_TIMEOUT_MS = 15_000L
    /** Times a spot must flicker on to pass the TV-remote calibration. */
    const val IR_CALIBRATION_HITS = 3
    const val IR_BASE_CONFIDENCE = 0.6f
    /** "Continue anyway" in a bright room multiplies confidence by this. */
    const val BRIGHT_ROOM_CONFIDENCE_FACTOR = 0.5f

    val IR_SPOTS = com.camsense.core.camera.SpotParams(
        minLuma = 160.0, minContrast = 80.0, windowPx = 51,
        minDiameter = IR_MIN_DIAMETER_PX, maxDiameter = IR_MAX_DIAMETER_PX,
        minCircularity = 0.5f, circularityMinArea = GLINT_CIRCULARITY_MIN_AREA,
        minAspect = 0.4f, maxSpots = 15, maxSurround = 90.0,
    )
    /** U or V this far below neutral (128) is a coloured light, not an IR glow. */
    const val IR_CHROMA_TOLERANCE = 12f
    /** Chroma is averaged over this radius (pixels) around a glow. */
    const val IR_CHROMA_RADIUS_PX = 8
    /** A newly confirmed glow this close (fraction of frame) to an earlier one is the same glow. */
    const val IR_MERGE_DISTANCE = 0.08f
    /** Keep at most this many glow findings (strongest). */
    const val IR_MAX_FINDINGS = 3
    /** The remote's LED is close to the lens, so it can look big and blurry. */
    val IR_CALIBRATION_SPOTS = com.camsense.core.camera.SpotParams(
        minLuma = 150.0, minContrast = 60.0, windowPx = 101,
        minDiameter = 2f, maxDiameter = 60f,
        minCircularity = 0.3f, circularityMinArea = GLINT_CIRCULARITY_MIN_AREA,
        minAspect = 0.3f, maxSpots = 8,
    )

    // ---- Shared camera checks ----
    /** No analyzed frame for this long: bind the camera again. */
    const val CAMERA_STALL_MS = 2_500L
    /** Mean absolute difference between small consecutive frames that means "too fast". */
    const val MOTION_TOO_FAST = 18.0
    const val MOTION_FRAME_SIZE = 64

    // ---- Room coverage meter and room map ----
    /** Compass sectors around the user (12 × 30°), each in 3 bands: low, middle, high. */
    const val COVERAGE_SECTORS = 12
    /** Orientation readings (~50 a second) needed in a cell before it counts: a flick past doesn't. */
    const val COVERAGE_SAMPLES = 10
    /** Below this up/down angle is the "low" band (floor, under furniture); above HIGH is the ceiling band. */
    const val COVERAGE_LOW_PITCH = -25f
    const val COVERAGE_HIGH_PITCH = 25f
    /** Below this share covered, the camera steps show what's still missing. */
    const val COVERAGE_GOOD = 0.6f
    /** Camera field of view in portrait (degrees), to place a spot in the frame on the room map. */
    const val CAMERA_HFOV_DEG = 50f
    const val CAMERA_VFOV_DEG = 65f

    // ---- Hot–cold finder (close check) ----
    const val HOTCOLD_FAST_ALPHA = 0.4f
    const val HOTCOLD_SLOW_ALPHA = 0.05f
    /** Fast minus slow average (µT) that counts as getting warmer or colder. */
    const val HOTCOLD_TREND_UT = 1.5f
    /** Field fallen this share below the recent peak: "you passed the strongest point". */
    const val HOTCOLD_PASSED_DROP = 0.35f
    const val HOTCOLD_PEAK_WINDOW_MS = 3_000L
    const val HOTCOLD_MIN_PEAK_UT = 10f

    // ---- Bathroom / changing-room mirror check ----
    /** "No gap" in the fingertip test: a hiding-spot finding at this confidence. */
    const val MIRROR_NO_GAP_CONFIDENCE = 0.6f

    // ---- Voice guidance ----
    /** The same sentence isn't spoken again within this time. */
    const val VOICE_REPEAT_MS = 6_000L

    // ---- M6 AI objects ----
    const val MODEL_ASSET = "models/hiding_objects_yolo26n.tflite"
    const val LABELS_ASSET = "models/labels.txt"
    const val OBJECT_MIN_CONFIDENCE = 0.40f
    const val OBJECT_NMS_IOU = 0.5f
    const val OBJECT_MAX_DETECTIONS = 20
    /** Tracker: IoU needed to call two boxes the same object. */
    const val OBJECT_TRACK_IOU = 0.3f
    const val OBJECT_TRACK_MAX_MISSED = 15
    /** Frames an object must be seen before it gets a target_key and a finding. */
    const val OBJECT_CONFIRM_FRAMES = 3
    /** A glint inside an object box grown by this fraction is linked to that object. */
    const val GLINT_OBJECT_MARGIN = 0.15f

    /**
     * Classes shown as "check this", mapped to the plain name used in reasons.
     * The custom model's 8 classes plus the stock COCO classes the plan uses
     * until the custom model is trained. Labels not listed here are ignored.
     */
    val HIDING_OBJECTS: Map<String, String> = mapOf(
        // Custom 8-class model (PRD FR-6.2)
        "smoke_detector" to "Smoke detector",
        "usb_charger" to "USB charger",
        "wall_clock" to "Wall clock",
        "photo_frame" to "Photo frame",
        "tissue_box" to "Tissue box",
        "air_purifier" to "Air purifier",
        "alarm_clock" to "Alarm clock",
        "wall_hole" to "Hole in the wall",
        // Stock COCO model (Implementation Plan, Day 4)
        "clock" to "Clock",
        "book" to "Book",
        "tv" to "TV",
        "cell phone" to "Phone",
        "vase" to "Vase",
        "potted plant" to "Plant pot",
    )
}
