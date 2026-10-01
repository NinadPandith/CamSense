package com.camsense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.ui.theme.CamSenseColors

/** Six dots at the top: done = lime, current = lime ring, next = border colour. */
@Composable
fun StepProgress(current: Int, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.step_of, current)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.semantics { contentDescription = label },
    ) {
        for (step in 1..6) {
            val dot = Modifier.size(10.dp)
            when {
                step < current -> Box(dot.background(CamSenseColors.Primary, CircleShape))
                step == current -> Box(dot.border(2.dp, CamSenseColors.Primary, CircleShape))
                else -> Box(dot.background(CamSenseColors.Border, CircleShape))
            }
        }
    }
}
