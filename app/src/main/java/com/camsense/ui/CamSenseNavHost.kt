package com.camsense.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.camsense.ui.evidence.EvidenceScreen
import com.camsense.ui.help.GetHelpScreen
import com.camsense.ui.history.HistoryScreen
import com.camsense.ui.home.HomeScreen
import com.camsense.ui.onboarding.IrCalibrationScreen
import com.camsense.ui.onboarding.OnboardingScreen
import com.camsense.ui.onboarding.PermissionsScreen
import com.camsense.ui.results.ResultsScreen
import com.camsense.ui.scan.CloseCheckScreen
import com.camsense.ui.scan.IrCheckScreen
import com.camsense.ui.scan.LensCheckScreen
import com.camsense.ui.scan.NetworkCheckScreen
import com.camsense.ui.scan.PlaceNameScreen
import com.camsense.ui.scan.RecheckScreen
import com.camsense.ui.scan.RoomSweepScreen
import com.camsense.ui.scan.ScanViewModel
import com.camsense.ui.scan.ScanStep
import com.camsense.ui.scan.MirrorCheckScreen
import com.camsense.core.ScanMode
import kotlinx.coroutines.CoroutineScope
import com.camsense.ui.settings.AboutScreen
import com.camsense.ui.settings.SettingsScreen
import com.camsense.ui.splash.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun CamSenseNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onReady = { onboarded ->
                nav.navigate(if (onboarded) Routes.HOME else Routes.ONBOARDING) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.permissions(Routes.RETURN_IR)) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(
            Routes.PERMISSIONS,
            arguments = listOf(navArgument("returnTo") { type = NavType.StringType; defaultValue = Routes.RETURN_BACK }),
        ) { entry ->
            val returnTo = entry.arguments?.getString("returnTo") ?: Routes.RETURN_BACK
            val container = appContainer()
            val scope = rememberCoroutineScope()
            PermissionsScreen(
                firstLaunch = returnTo == Routes.RETURN_IR,
                onDone = { cameraAllowed ->
                    when {
                        returnTo != Routes.RETURN_IR -> nav.popBackStack()
                        cameraAllowed -> nav.navigate(Routes.irCalibration(false)) {
                            popUpTo(Routes.PERMISSIONS) { inclusive = true }
                        }
                        // No camera: the IR test can't run, so onboarding ends here.
                        else -> scope.launch {
                            container.settings.setIrSupported(false)
                            container.settings.setOnboardingDone(true)
                            nav.navigate(Routes.HOME) { popUpTo(Routes.PERMISSIONS) { inclusive = true } }
                        }
                    }
                },
            )
        }
        composable(
            Routes.IR_CALIBRATION,
            arguments = listOf(navArgument("fromSettings") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val fromSettings = entry.arguments?.getBoolean("fromSettings") ?: false
            IrCalibrationScreen(onDone = {
                if (fromSettings) nav.popBackStack()
                else nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartScan = { mode -> nav.navigate(Routes.scan(mode = mode)) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenScan = { id -> nav.navigate(Routes.results(id, readOnly = true)) },
            )
        }

        navigation(
            startDestination = Routes.PLACE_NAME,
            route = Routes.SCAN_GRAPH,
            arguments = listOf(
                navArgument("place") { type = NavType.StringType; defaultValue = "" },
                navArgument("mode") { type = NavType.StringType; defaultValue = ScanMode.FULL.name },
            ),
        ) {
            composable(Routes.PLACE_NAME) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                PlaceNameScreen(vm,
                    onNext = { goToStep(nav, scope, vm, ScanStep.PLACE) },
                    onBack = { nav.popBackStack(Routes.HOME, inclusive = false) })
            }
            composable(Routes.NETWORK_CHECK) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                NetworkCheckScreen(vm,
                    onContinue = { goToStep(nav, scope, vm, ScanStep.NETWORK) },
                    onAllowNearby = { nav.navigate(Routes.permissions(Routes.RETURN_BACK)) },
                    onStopped = { goHome(nav) })
            }
            composable(Routes.ROOM_SWEEP) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                RoomSweepScreen(vm,
                    onNext = { goToStep(nav, scope, vm, ScanStep.ROOM) },
                    onSkipCamera = { goToStep(nav, scope, vm, ScanStep.ROOM) },
                    onStopped = { goHome(nav) })
            }
            composable(Routes.MIRROR_CHECK) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                MirrorCheckScreen(vm, onNext = { goToStep(nav, scope, vm, ScanStep.MIRROR) }, onStopped = { goHome(nav) })
            }
            composable(Routes.IR_CHECK) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                IrCheckScreen(vm,
                    onNext = { goToStep(nav, scope, vm, ScanStep.IR) },
                    onSkipCamera = { goToStep(nav, scope, vm, ScanStep.IR) },
                    onStopped = { goHome(nav) })
            }
            composable(Routes.LENS_CHECK) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                LensCheckScreen(vm,
                    onNext = { goToStep(nav, scope, vm, ScanStep.LENS) },
                    onSkipCamera = { goToStep(nav, scope, vm, ScanStep.LENS) },
                    onStopped = { goHome(nav) })
            }
            composable(Routes.CLOSE_CHECK) { entry ->
                val vm = scanViewModel(nav, entry)
                val scope = rememberCoroutineScope()
                CloseCheckScreen(vm,
                    onResults = { goToStep(nav, scope, vm, ScanStep.CLOSE) },
                    onStopped = { goHome(nav) })
            }
        }

        composable(
            Routes.RESULTS,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType },
                navArgument("readOnly") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            val readOnly = entry.arguments?.getBoolean("readOnly") ?: false
            ResultsScreen(
                scanId = scanId,
                readOnly = readOnly,
                onHome = { goHome(nav) },
                onBack = { if (readOnly) nav.popBackStack() else goHome(nav) },
                onScanAgain = { place, mode -> nav.navigate(Routes.scan(place, mode)) { popUpTo(Routes.HOME) } },
                onCheckSpots = { nav.navigate(Routes.recheck(scanId)) },
                onEvidence = { nav.navigate(Routes.evidence(scanId)) },
                onGetHelp = { nav.navigate(Routes.GET_HELP) },
                onDeleted = { nav.popBackStack() },
            )
        }
        composable(Routes.RECHECK, arguments = listOf(navArgument("scanId") { type = NavType.StringType })) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            RecheckScreen(scanId,
                onResults = {
                    nav.navigate(Routes.results(scanId, readOnly = false)) {
                        popUpTo(Routes.RESULTS) { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() })
        }
        composable(Routes.EVIDENCE, arguments = listOf(navArgument("scanId") { type = NavType.StringType })) { entry ->
            EvidenceScreen(entry.arguments?.getString("scanId").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(Routes.GET_HELP) { GetHelpScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpenScan = { id -> nav.navigate(Routes.results(id, readOnly = true)) },
                onStartScan = { nav.navigate(Routes.scan()) { popUpTo(Routes.HOME) } },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onRecalibrate = { nav.navigate(Routes.irCalibration(fromSettings = true)) },
                onAbout = { nav.navigate(Routes.ABOUT) },
                onGetHelp = { nav.navigate(Routes.GET_HELP) },
            )
        }
        composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
    }
}

/**
 * Moves the scan on from [from]: the view model decides the next step for this scan
 * type (skipping IR on phones that can't see it, camera steps without the camera).
 * Results pops the whole scan graph, so Back on Results goes Home, never into the scan.
 */
private fun goToStep(nav: NavController, scope: CoroutineScope, vm: ScanViewModel, from: ScanStep) {
    scope.launch {
        when (val step = vm.next(from)) {
            ScanStep.RESULTS -> vm.finish { id ->
                nav.navigate(Routes.results(id, readOnly = false)) { popUpTo(Routes.SCAN_GRAPH) { inclusive = true } }
            }
            ScanStep.NETWORK -> nav.navigate(Routes.NETWORK_CHECK)
            ScanStep.ROOM -> nav.navigate(Routes.ROOM_SWEEP)
            ScanStep.MIRROR -> nav.navigate(Routes.MIRROR_CHECK)
            ScanStep.IR -> nav.navigate(Routes.IR_CHECK)
            ScanStep.LENS -> nav.navigate(Routes.LENS_CHECK)
            ScanStep.CLOSE -> nav.navigate(Routes.CLOSE_CHECK)
            ScanStep.PLACE -> Unit
        }
    }
}

private fun goHome(nav: NavController) {
    if (!nav.popBackStack(Routes.HOME, inclusive = false)) {
        nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
    }
}

/** The ScanViewModel scoped to the "scan" graph, shared by all six steps. */
@Composable
private fun scanViewModel(nav: NavController, entry: NavBackStackEntry): ScanViewModel {
    val container = appContainer()
    val parent = remember(entry) { nav.getBackStackEntry(Routes.SCAN_GRAPH) }
    return viewModel(
        viewModelStoreOwner = parent,
        factory = viewModelFactory { initializer { ScanViewModel(container, createSavedStateHandle()) } },
    )
}
