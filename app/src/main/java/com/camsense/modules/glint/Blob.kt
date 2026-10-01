package com.camsense.modules.glint

/**
 * One bright spot in one frame. x/y are 0–1 fractions of the upright frame;
 * [strength] is how far it stands above its surroundings (0–255).
 */
data class Blob(val x: Float, val y: Float, val diameterPx: Float, val strength: Float = 0f)
