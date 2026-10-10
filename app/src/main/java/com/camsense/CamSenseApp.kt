package com.camsense

import android.app.Application
import android.util.Log
import com.camsense.core.voice.VoiceGuide
import com.camsense.data.EvidenceStore
import com.camsense.data.ScanRepository
import com.camsense.data.SettingsStore
import com.camsense.data.db.CamSenseDatabase
import com.camsense.modules.bluetooth.BleScanModule
import com.camsense.modules.bluetooth.OuiDatabase
import com.camsense.modules.glint.GlintModule
import com.camsense.modules.infrared.IrModule
import com.camsense.modules.magnetometer.MagnetSweepModule
import com.camsense.modules.network.NetworkScanModule
import com.camsense.modules.objects.ObjectDetector
import com.camsense.modules.objects.ObjectsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

/** Manual dependency injection: one container for the whole app (Tech Stack doc). */
class AppContainer(val app: Application) {
    /** Outlives screens; used for file writes that must finish (snapshots, cleanup). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val appVersion: String = BuildConfig.VERSION_NAME
    val database: CamSenseDatabase by lazy { CamSenseDatabase.build(app) }
    val settings: SettingsStore by lazy { SettingsStore(app) }
    val evidenceStore: EvidenceStore by lazy { EvidenceStore(app, database.evidenceDao()) }
    val repository: ScanRepository by lazy { ScanRepository(database, evidenceStore, appVersion) }
    val objectDetector: ObjectDetector by lazy { ObjectDetector(app) }
    val voice: VoiceGuide by lazy { VoiceGuide(app, settings, appScope) }
    private val oui: OuiDatabase by lazy { OuiDatabase(app) }

    /** A fresh set of the six modules for each guided scan. */
    fun newScanModules(): ScanModules {
        val objects = ObjectsModule(objectDetector)
        return ScanModules(
            network = NetworkScanModule(app, oui),
            ble = BleScanModule(app, oui),
            objects = objects,
            infrared = IrModule(settings),
            glint = GlintModule(evidenceStore, objects, appScope),
            magnet = MagnetSweepModule(app),
        )
    }
}

data class ScanModules(
    val network: NetworkScanModule,
    val ble: BleScanModule,
    val objects: ObjectsModule,
    val infrared: IrModule,
    val glint: GlintModule,
    val magnet: MagnetSweepModule,
) {
    val all get() = listOf(glint, infrared, network, ble, magnet, objects)
}

class CamSenseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (!OpenCVLoader.initLocal()) Log.e("CamSense", "OpenCV failed to load")
        container.appScope.launch {
            // An app killed mid-scan leaves the scan RUNNING; it is never shown in History.
            container.repository.abortStaleRunning()
        }
    }
}
