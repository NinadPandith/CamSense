package com.camsense.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.camsense.R
import com.camsense.ui.components.PrimaryButton
import com.camsense.ui.components.ScanStepScreen
import com.camsense.ui.theme.CamSenseColors
import com.camsense.ui.theme.CamSenseType

/** Step 1: optional place name. Back returns Home and nothing is saved. */
@Composable
fun PlaceNameScreen(vm: ScanViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(vm.initialPlaceName) }
    BackHandler(onBack = onBack)
    ScanStepScreen(
        step = 1,
        title = stringResource(R.string.place_title),
        instruction = stringResource(R.string.place_instruction),
        onStop = onBack,
        bottom = { PrimaryButton(stringResource(R.string.action_next), { vm.startScan(name, onNext) }) },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(120) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.place_hint), style = CamSenseType.Body) },
            textStyle = CamSenseType.Body,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CamSenseColors.Primary,
                unfocusedBorderColor = CamSenseColors.Border,
                cursorColor = CamSenseColors.Primary,
                focusedTextColor = CamSenseColors.Text,
                unfocusedTextColor = CamSenseColors.Text,
                focusedPlaceholderColor = CamSenseColors.TextMuted,
                unfocusedPlaceholderColor = CamSenseColors.TextMuted,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
