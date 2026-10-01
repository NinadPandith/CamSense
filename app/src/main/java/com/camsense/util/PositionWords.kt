package com.camsense.util

/** "top left", "center", … for TalkBack announcements of overlays. */
object PositionWords {
    fun of(x: Float, y: Float): String {
        val v = when { y < 0.33f -> "top"; y > 0.66f -> "bottom"; else -> "" }
        val h = when { x < 0.33f -> "left"; x > 0.66f -> "right"; else -> "" }
        return when {
            v.isEmpty() && h.isEmpty() -> "center"
            v.isEmpty() -> h
            h.isEmpty() -> v
            else -> "$v $h"
        }
    }
}
