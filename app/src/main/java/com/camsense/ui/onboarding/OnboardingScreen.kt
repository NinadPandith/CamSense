package com.camsense.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.camsense.R
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.TextAction
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType
import kotlinx.coroutines.launch

private data class Card(val icon: ImageVector, val title: Int, val body: Int)

private val cards = listOf(
    Card(Icons.Rounded.CenterFocusStrong, R.string.onboarding_1_title, R.string.onboarding_1_body),
    Card(Icons.Rounded.Checklist, R.string.onboarding_2_title, R.string.onboarding_2_body),
    Card(Icons.Rounded.VisibilityOff, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

/** Three cards: what it does, how it works, its limits. "Skip" on every card. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { cards.size }
    val scope = rememberCoroutineScope()
    CamSenseScreen(
        title = null,
        scroll = false,
        bottom = {
            PrimaryButton(stringResource(R.string.action_next), onClick = {
                if (pager.currentPage < cards.lastIndex) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                else onDone()
            })
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextAction(stringResource(R.string.action_skip), onDone)
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val card = cards[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Icon(card.icon, contentDescription = null, tint = CamSenseColors.Primary, modifier = Modifier.size(48.dp))
                Text(stringResource(card.title), style = CamSenseType.Title, color = CamSenseColors.Text,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(card.body), style = CamSenseType.Body, color = CamSenseColors.TextMuted)
            }
        }
        PageDots(pager.currentPage, cards.size)
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        repeat(count) { i ->
            Box(Modifier.size(8.dp).background(if (i == current) CamSenseColors.Primary else CamSenseColors.Border, CircleShape))
        }
    }
}
