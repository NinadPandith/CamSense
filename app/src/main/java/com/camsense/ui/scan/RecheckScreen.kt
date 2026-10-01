package com.camsense.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.camsense.R
import com.camsense.ui.appContainer
import com.camsense.ui.components.CamSenseScreen
import com.camsense.ui.components.KeepScreenOn
import com.camsense.ui.components.NoticeBanner
import com.camsense.ui.components.PrimaryButton

/** "Check flagged spots" from a Medium result: close check only, then the scan is re-scored. */
@Composable
fun RecheckScreen(scanId: String, onResults: () -> Unit, onBack: () -> Unit) {
    val container = appContainer()
    val vm: RecheckViewModel = viewModel(factory = viewModelFactory { initializer { RecheckViewModel(container, scanId) } })
    KeepScreenOn()
    val live by vm.magnet.live.collectAsState()
    val spots by vm.spots.collectAsState()

    CamSenseScreen(
        title = stringResource(R.string.close_title),
        onBack = onBack,
        bottom = { PrimaryButton(stringResource(R.string.close_button), { vm.finish(onResults) }) },
    ) {
        if (!vm.magnet.isSupported()) {
            NoticeBanner(stringResource(R.string.close_no_magnetometer))
        } else {
            NoticeBanner(stringResource(R.string.close_instruction))
            CloseCheckContent(live, spots, vm::selectSpot, vm.magnet::recalibrate)
        }
    }
}
