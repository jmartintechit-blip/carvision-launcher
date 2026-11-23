package com.carvision.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { AUTO, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Preferencias del usuario (DataStore). Cambiar el tema no reinicia la actividad. */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val demo = booleanPreferencesKey("demo_mode")
        val uiScale = floatPreferencesKey("ui_scale")
        val pack = stringPreferencesKey("vehicle_pack")
        val bootAnimation = booleanPreferencesKey("boot_animation")
        val ambient = stringPreferencesKey("ambient")
        val alertDoor = booleanPreferencesKey("alert_door_moving")
        val alertSound = booleanPreferencesKey("alert_sound")
        val alertVoice = booleanPreferencesKey("alert_voice")
        val wizardDone = booleanPreferencesKey("wizard_done")
        fun paint(packId: String) = stringPreferencesKey("paint_$packId")
    }

    /** Asistente de primer arranque terminado o descartado. */
    val wizardDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.wizardDone] ?: false }
    suspend fun setWizardDone(v: Boolean) = context.dataStore.edit { it[Keys.wizardDone] = v }

    /** Color de la carrocería elegido para un coche (0xRRGGBB) o null = el del modelo. */
    fun paintColor(packId: String): Flow<Int?> = context.dataStore.data.map { p ->
        p[Keys.paint(packId)]?.toIntOrNull(16)
    }
    suspend fun setPaintColor(packId: String, color: Int?) = context.dataStore.edit {
        if (color == null) it.remove(Keys.paint(packId)) else it[Keys.paint(packId)] = Integer.toHexString(color and 0xFFFFFF)
    }

    /** Aviso de apertura abierta con el coche en marcha. */
    val alertDoorMoving: Flow<Boolean> = context.dataStore.data.map { it[Keys.alertDoor] ?: true }
    /** Sonido discreto al aparecer un aviso. */
    val alertSound: Flow<Boolean> = context.dataStore.data.map { it[Keys.alertSound] ?: true }
    /** Lectura por voz de los avisos importantes (desactivada por defecto). */
    val alertVoice: Flow<Boolean> = context.dataStore.data.map { it[Keys.alertVoice] ?: false }
    suspend fun setAlertDoorMoving(v: Boolean) = context.dataStore.edit { it[Keys.alertDoor] = v }
    suspend fun setAlertSound(v: Boolean) = context.dataStore.edit { it[Keys.alertSound] = v }
    suspend fun setAlertVoice(v: Boolean) = context.dataStore.edit { it[Keys.alertVoice] = v }

    /** Ambiente de fondo. */
    val ambient: Flow<com.carvision.core.designsystem.Ambient> = context.dataStore.data.map { p ->
        p[Keys.ambient]?.let { runCatching { com.carvision.core.designsystem.Ambient.valueOf(it) }.getOrNull() }
            ?: com.carvision.core.designsystem.Ambient.GRAPHITE
    }
    suspend fun setAmbient(a: com.carvision.core.designsystem.Ambient) = context.dataStore.edit { it[Keys.ambient] = a.name }

    /** Animación del logotipo de la marca al arrancar. */
    val bootAnimation: Flow<Boolean> = context.dataStore.data.map { it[Keys.bootAnimation] ?: true }
    suspend fun setBootAnimation(enabled: Boolean) = context.dataStore.edit { it[Keys.bootAnimation] = enabled }

    /** Pack de vehículo elegido. null = el predeterminado. */
    val selectedPack: Flow<String?> = context.dataStore.data.map { it[Keys.pack] }
    suspend fun setSelectedPack(id: String) = context.dataStore.edit { it[Keys.pack] = id }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.AUTO
    }
    val demoMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.demo] ?: false }
    val uiScale: Flow<Float> = context.dataStore.data.map { it[Keys.uiScale] ?: 1f }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.name }
    suspend fun setDemoMode(enabled: Boolean) = context.dataStore.edit { it[Keys.demo] = enabled }
    suspend fun setUiScale(scale: Float) = context.dataStore.edit { it[Keys.uiScale] = scale.coerceIn(0.8f, 1.4f) }
}
