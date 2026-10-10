package com.camsense.core

/**
 * Which guided scan runs. Stored by name in scans.mode, so never rename an entry.
 * - FULL: all six steps.
 * - QUICK: check-in scan of about 90 s (Wi-Fi + camera hotspots + lens check).
 * - SMALL_ROOM: bathroom or changing room (Wi-Fi, mirror check, IR, lens, close check).
 */
enum class ScanMode { FULL, QUICK, SMALL_ROOM }
