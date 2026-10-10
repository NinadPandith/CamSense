package com.camsense.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.camsense.ui.appContainer

/** Speaks [text] once when the screen opens (if voice guidance is on), cutting off what came before. */
@Composable
fun SpeakOnEnter(vararg parts: String) {
    val voice = appContainer().voice
    val text = parts.filter { it.isNotBlank() }.joinToString(". ")
    LaunchedEffect(text) { voice.say(text, interrupt = true) }
}
