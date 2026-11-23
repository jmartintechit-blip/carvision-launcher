package com.carvision.launcher.alerts

import android.content.Context
import com.carvision.core.model.Opening
import com.carvision.core.model.VehicleSnapshot
import com.carvision.launcher.data.SettingsRepository
import com.carvision.launcher.ui.openLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Aviso de conducción visible. */
data class DriveAlert(val title: String, val detail: String)

/**
 * Avisos de conducción (no es una función de seguridad certificada):
 * - apertura abierta con el coche en marcha (> 5 km/h; se retira por debajo de 3 km/h o al cerrar).
 * Solo actúa con datos fiables (reales o, en el modo demostración, simulados y marcados como tales).
 * Al aparecer un aviso nuevo suena un tono discreto y, si el usuario lo ha activado, se lee en voz alta.
 */
class DriveAlerts(
    private val context: Context,
    private val scope: CoroutineScope,
    vehicle: StateFlow<VehicleSnapshot>,
    private val settings: SettingsRepository,
) {
    private val state = MutableStateFlow<DriveAlert?>(null)
    val active: StateFlow<DriveAlert?> = state.asStateFlow()

    private val voice by lazy { Voice(context) }
    private var lastKey: Set<Opening> = emptySet()
    private var sound = true
    private var speak = false

    init {
        scope.launch { settings.alertSound.collect { sound = it } }
        scope.launch {
            settings.alertVoice.collect { enabled ->
                speak = enabled
                if (enabled) voice // inicializa el motor de voz con antelación
            }
        }
        scope.launch {
            combine(vehicle, settings.alertDoorMoving) { v, enabled -> v to enabled }.collect { (v, enabled) -> evaluate(v, enabled) }
        }
    }

    private fun evaluate(v: VehicleSnapshot, enabled: Boolean) {
        val speed = v.speedKmh.takeIf { it.isDisplayable }?.value
        val open = v.openOpenings
        val moving = speed != null && (speed > 5.0 || (state.value != null && speed > 3.0))
        if (!enabled || !moving || open.isEmpty()) {
            state.value = null
            lastKey = emptySet()
            return
        }
        val title = if (open.size == 1) {
            open.first().openLabel()
        } else {
            "${open.size} aperturas abiertas"
        }
        state.value = DriveAlert(title, "Con el coche en marcha")
        if (!lastKey.containsAll(open)) {
            // Aviso nuevo (o una apertura más): tono y, si procede, voz.
            if (sound) Chime.play()
            if (speak) voice.say("Atención. $title.")
        }
        lastKey = open
    }

    /** Lee un texto en voz alta si el usuario tiene activada la voz (p. ej. aviso de mantenimiento al arrancar). */
    fun announce(text: String) {
        if (speak) voice.say(text)
    }
}
