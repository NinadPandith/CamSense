package com.camsense.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.camsense.AppContainer
import com.camsense.CamSenseApp

/** The app-wide dependency container, for screens and ViewModel factories. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as CamSenseApp).container
