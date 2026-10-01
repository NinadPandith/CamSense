package com.camsense.core

/** One place the user should hold the phone near in the close check. */
data class Spot(val key: String, val name: String, val modules: Set<ModuleId>)

/** Builds the close-check list from camera findings. Pure Kotlin for unit tests. */
object Spots {

    /** "object-3" → name from the object finding; "glint-2" → "Bright reflection 2"; "ir-1" → "Glowing dot 1". */
    fun nameFor(key: String, module: ModuleId, reason: String): String = when {
        key.startsWith("glint-") -> "Bright reflection ${key.removePrefix("glint-")}"
        key.startsWith("ir-") -> "Glowing dot ${key.removePrefix("ir-")}"
        module == ModuleId.OBJECTS && ':' in reason -> reason.substringBefore(':').trim()
        else -> reason
    }

    /**
     * Distinct spots from the camera modules. An object name wins over a glint's
     * generic name when both share a key; the magnetometer never adds spots.
     */
    fun from(findings: List<Pair<ModuleId, Finding>>): List<Spot> {
        val byKey = linkedMapOf<String, Spot>()
        val ordered = findings.filter { it.first != ModuleId.MAGNET && it.second.targetKey != null }
            .sortedBy { if (it.first == ModuleId.OBJECTS) 0 else 1 }
        for ((module, f) in ordered) {
            val key = f.targetKey!!
            val existing = byKey[key]
            byKey[key] = existing?.copy(modules = existing.modules + module)
                ?: Spot(key, nameFor(key, module, f.reason), setOf(module))
        }
        return byKey.values.sortedByDescending { it.modules.size }
    }
}
