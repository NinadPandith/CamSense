package com.camsense

import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.Reasons
import com.camsense.core.Spots
import org.junit.Assert.assertEquals
import org.junit.Test

class SpotsTest {
    @Test
    fun objectNameWinsAndModulesMerge() {
        val spots = Spots.from(listOf(
            ModuleId.GLINT to Finding(Reasons.GLINT, 0.9f, "object-1"),
            ModuleId.OBJECTS to Finding(Reasons.objectHidingSpot("Tissue box"), 0.7f, "object-1"),
            ModuleId.GLINT to Finding(Reasons.GLINT, 0.8f, "glint-2"),
            ModuleId.INFRARED to Finding(Reasons.IR, 0.6f, "ir-1"),
            ModuleId.MAGNET to Finding(Reasons.MAGNET, 0.6f, "object-9"),
            ModuleId.NETWORK to Finding(Reasons.NET_STREAM, 0.9f, null),
        ))
        assertEquals(listOf("object-1", "glint-2", "ir-1"), spots.map { it.key })
        assertEquals("Tissue box", spots[0].name)
        assertEquals(setOf(ModuleId.GLINT, ModuleId.OBJECTS), spots[0].modules)
        assertEquals("Bright reflection 2", spots[1].name)
        assertEquals("Glowing dot 1", spots[2].name)
    }
}
