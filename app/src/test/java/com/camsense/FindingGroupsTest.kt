package com.camsense

import com.camsense.core.ModuleId
import com.camsense.data.db.UserVerdict
import com.camsense.ui.results.FindingGroups
import com.camsense.ui.results.FindingUi
import org.junit.Assert.assertEquals
import org.junit.Test

class FindingGroupsTest {

    private fun f(id: String, module: ModuleId, reason: String, conf: Float, verdict: UserVerdict = UserVerdict.UNREVIEWED) =
        FindingUi(id, module, reason, conf, verdict, snapshot = null, technical = null)

    @Test
    fun identicalFindingsBecomeOneRow() {
        val rows = FindingGroups.group(
            List(40) { f("ir$it", ModuleId.INFRARED, "Glow", 0.6f + it / 1000f) } +
                f("g1", ModuleId.GLINT, "Lens", 0.92f),
        )
        assertEquals(2, rows.size)
        assertEquals("g1", rows[0].id) // strongest first
        assertEquals(40, rows[1].count)
        assertEquals(40, rows[1].ids.size)
        assertEquals(0.639f, rows[1].confidence, 1e-6f)
    }

    @Test
    fun groupVerdictOnlyWhenAllAgree() {
        val mixed = FindingGroups.group(listOf(
            f("a", ModuleId.BLE, "x", 0.3f, UserVerdict.CHECKED_OK),
            f("b", ModuleId.BLE, "x", 0.3f),
        ))
        assertEquals(UserVerdict.UNREVIEWED, mixed.single().verdict)
        val same = FindingGroups.group(listOf(
            f("a", ModuleId.BLE, "x", 0.3f, UserVerdict.CHECKED_OK),
            f("b", ModuleId.BLE, "x", 0.3f, UserVerdict.CHECKED_OK),
        ))
        assertEquals(UserVerdict.CHECKED_OK, same.single().verdict)
    }

    @Test
    fun differentReasonsStaySeparate() {
        val rows = FindingGroups.group(listOf(f("a", ModuleId.NETWORK, "stream", 0.9f), f("b", ModuleId.NETWORK, "port", 0.6f)))
        assertEquals(2, rows.size)
    }
}
