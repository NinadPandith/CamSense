package com.camsense.ui

import android.net.Uri

/** Navigation routes (App Flow doc), plus RECHECK and ABOUT. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions?returnTo={returnTo}"
    const val IR_CALIBRATION = "ir_calibration?fromSettings={fromSettings}"
    const val HOME = "home"
    // nested graph "scan" -> shares ScanViewModel
    const val SCAN_GRAPH = "scan?place={place}"
    const val PLACE_NAME = "scan/place"
    const val NETWORK_CHECK = "scan/network"
    const val ROOM_SWEEP = "scan/room"
    const val IR_CHECK = "scan/ir"
    const val LENS_CHECK = "scan/lens"
    const val CLOSE_CHECK = "scan/close"
    const val RESULTS = "results/{scanId}?readOnly={readOnly}"
    const val EVIDENCE = "evidence/{scanId}"
    const val GET_HELP = "get_help"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    /** "Check flagged spots" from a Medium result: magnetometer-only pass on a saved scan. */
    const val RECHECK = "recheck/{scanId}"

    fun permissions(returnTo: String) = "permissions?returnTo=$returnTo"
    fun irCalibration(fromSettings: Boolean) = "ir_calibration?fromSettings=$fromSettings"
    fun scan(place: String? = null) = "scan?place=${Uri.encode(place.orEmpty())}"
    fun results(scanId: String, readOnly: Boolean) = "results/$scanId?readOnly=$readOnly"
    fun evidence(scanId: String) = "evidence/$scanId"
    fun recheck(scanId: String) = "recheck/$scanId"

    const val RETURN_IR = "ir_calibration"
    const val RETURN_BACK = "back"
}
