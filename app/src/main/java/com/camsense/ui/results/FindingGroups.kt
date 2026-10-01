package com.camsense.ui.results

import com.camsense.data.db.UserVerdict

/**
 * Findings with the same check and reason become one row on Results ("Small glowing
 * dot… · 4 spots"), so a light passed several times doesn't fill the screen.
 * Pure Kotlin for unit tests.
 */
object FindingGroups {

    fun group(findings: List<FindingUi>): List<FindingUi> =
        findings.groupBy { it.module to it.reason }.values.map { items ->
            val best = items.maxBy { it.confidence }
            val verdicts = items.map { it.verdict }.toSet()
            best.copy(
                ids = items.flatMap { it.ids },
                count = items.sumOf { it.count },
                verdict = verdicts.singleOrNull() ?: UserVerdict.UNREVIEWED,
                snapshot = best.snapshot ?: items.firstNotNullOfOrNull { it.snapshot },
                technical = items.mapNotNull { it.technical }.distinct().joinToString("\n\n").ifEmpty { null },
            )
        }.sortedByDescending { it.confidence }
}
