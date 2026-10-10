package com.camsense.data.report

/** Word wrap for the PDF report; [measure] gives a string's width. Pure Kotlin for unit tests. */
object TextWrap {
    fun wrap(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(' ').filter { it.isNotEmpty() }) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (measure(candidate) <= maxWidth || line.isEmpty()) {
                    // A single word longer than the line is split by characters.
                    if (line.isEmpty() && measure(word) > maxWidth) {
                        var chunk = ""
                        for (c in word) {
                            if (measure(chunk + c) > maxWidth && chunk.isNotEmpty()) { lines += chunk; chunk = "" }
                            chunk += c
                        }
                        line = chunk
                    } else {
                        line = candidate
                    }
                } else {
                    lines += line
                    line = word
                }
            }
            lines += line
        }
        return lines
    }
}
