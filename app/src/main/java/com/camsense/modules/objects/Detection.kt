package com.camsense.modules.objects

import com.camsense.core.NormRect

/** One object the model found in one frame; box in 0–1 fractions of the upright frame. */
data class Detection(val label: String, val confidence: Float, val box: NormRect)
