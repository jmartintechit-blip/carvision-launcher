package com.carvision.launcher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.carvision.core.model.VehicleSnapshot
import com.carvision.vehicle.data.DemoVehicleDataProvider
import com.carvision.vehicle.data.UnavailableVehicleDataProvider
import com.carvision.vehicle.data.VehicleDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Dependencias de la aplicación. Una única instancia, creada en CarVisionApp. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val packageName: String = context.packageName
    val settings = SettingsRepository(context)
    val packs = PackRepository(context, appScope)
    val brandLogos = BrandLogos(context)
    val maintenance = MaintenanceRepository(context, appScope)
    val media = com.carvision.launcher.media.NowPlayingRepository(context)
    val notifier = com.carvision.launcher.ui.Notifier()

    /** Hoja (panel) abierta sobre toda la interfaz. */
    /** Pendrive automático: detecta un USB y busca packs en él. */
    val usb = UsbPacks(context, appScope)

    var sheet by androidx.compose.runtime.mutableStateOf<com.carvision.launcher.ui.Sheet?>(null)

    /** true hasta que se muestre la animación de arranque (una vez por arranque del proceso, p. ej. al encender la radio). */
    var bootSplashPending = true

    /** Asistente de primer arranque visible (D-028). */
    var wizardVisible by androidx.compose.runtime.mutableStateOf(false)
    /** El asistente abrió el Garaje: al cerrarlo, se vuelve al asistente. */
    var wizardReturnAfterSheet = false
    /** El asistente ya se ofreció en este arranque del proceso. */
    var wizardOffered = false
    /** Paso del asistente en el que se estaba (para volver a él tras abrir el Garaje). */
    var wizardStep = 0

    /** true hasta que termina la animación de arranque: entonces la cámara hace la bienvenida alrededor del coche. */
    var introPending by androidx.compose.runtime.mutableStateOf(true)

    /**
     * true cuando el visor 3D ya ha dibujado el coche (o la radio no tiene 3D). La animación de arranque espera a esta
     * señal antes de fundirse, para que al desaparecer el logotipo el coche ya esté en pantalla (en radios lentas el
     * motor 3D apenas puede dibujar mientras se anima el logotipo).
     */
    var carShown by androidx.compose.runtime.mutableStateOf(false)

    /** El aviso de mantenimiento al arrancar ya se ha mostrado en este arranque. */
    var maintenanceNoticeShown = false

    val demoProvider = DemoVehicleDataProvider()
    private val unavailableProvider = UnavailableVehicleDataProvider()

    /** Proveedor activo: demo si el usuario lo activa; si no, "sin conexión" (no hay CAN Adapter todavía). */
    val activeProvider: StateFlow<VehicleDataProvider> = settings.demoMode
        .map { demo -> if (demo) demoProvider else unavailableProvider }
        .stateIn(appScope, SharingStarted.Eagerly, unavailableProvider)

    @OptIn(ExperimentalCoroutinesApi::class)
    val vehicle: StateFlow<VehicleSnapshot> = activeProvider
        .flatMapLatest { p -> p.snapshot }
        .stateIn(appScope, SharingStarted.Eagerly, VehicleSnapshot.unavailable())

    /** Ordenador de viaje: un viaje por arranque del launcher (al dar el contacto) o al pulsar "Nuevo viaje". */
    val trip = com.carvision.vehicle.data.TripComputer()
    val trips = TripRepository(context, appScope, trip)

    /** Avisos de conducción (apertura abierta en marcha). Ayuda, no función de seguridad certificada. */
    val alerts = com.carvision.launcher.alerts.DriveAlerts(context, appScope, vehicle, settings)

    init {
        demoProvider.start(appScope)
        appScope.launch { vehicle.collect { trip.update(it) } }
        // Carga (y recarga al cambiarlo en Ajustes) el pack elegido; por defecto, el Focus MK3.
        appScope.launch {
            settings.selectedPack.distinctUntilChanged().collect { id ->
                packs.load(id ?: DEFAULT_PACK)
            }
        }
    }

    companion object {
        const val DEFAULT_PACK = "carvision_default"
    }
}
