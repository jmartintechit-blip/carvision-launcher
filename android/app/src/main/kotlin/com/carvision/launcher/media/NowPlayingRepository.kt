package com.carvision.launcher.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.carvision.render3d.DeviceProfile
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que está sonando ahora en cualquier app con sesión multimedia. */
data class NowPlaying(
    val title: String,
    val artist: String?,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val appLabel: String?,
    val packageName: String,
    /** Icono de la app que reproduce (Spotify, YouTube Music…). */
    val appIcon: Bitmap? = null,
)

sealed interface MediaState {
    /** El usuario no ha concedido el acceso a notificaciones. */
    data object NoPermission : MediaState
    data object Idle : MediaState
    data class Active(val nowPlaying: NowPlaying) : MediaState
}

/**
 * Observa las sesiones multimedia activas (MediaSessionManager) y expone la principal.
 * Prioridad: la que está reproduciendo; si ninguna, la más reciente.
 */
class NowPlayingRepository(private val context: Context) {
    private val state = MutableStateFlow<MediaState>(MediaState.Idle)
    val current: StateFlow<MediaState> = state.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private var controllers: List<MediaController> = emptyList()
    private var started = false
    private val appIcons = mutableMapOf<String, Bitmap?>()

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refreshSessions()
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { refreshSessions() }

    companion object {
        /** Etiqueta de la sesión multimedia de prueba (solo existe en la compilación de depuración). */
        const val DEBUG_SESSION_TAG = "CarVisionDebug"
    }

    fun hasPermission(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Pantalla de Android donde se concede el acceso a notificaciones. */
    fun permissionIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Llamar al volver a primer plano (el permiso se concede fuera de la app). */
    fun start() {
        if (!hasPermission()) {
            stopInternal()
            // En Android Go, Ajustes no permite conceder este acceso: la tarjeta abre la app de música sin más
            // (se puede conceder por adb: docs/INSTALACION.md).
            state.value = if (DeviceProfile.isAndroidGo(context)) MediaState.Idle else MediaState.NoPermission
            return
        }
        if (started) {
            refreshSessions()
            return
        }
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, main)
            started = true
            refreshSessions()
        } catch (e: SecurityException) {
            state.value = MediaState.NoPermission
        }
    }

    private fun stopInternal() {
        if (started) runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = emptyList()
        started = false
    }

    private fun refreshSessions() {
        val list = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            state.value = MediaState.NoPermission
            return
        }
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        // Las sesiones propias se ignoran, salvo la de prueba de la compilación de depuración.
        controllers = list.filter { it.packageName != context.packageName || it.tag == DEBUG_SESSION_TAG }
        controllers.forEach { it.registerCallback(controllerCallback, main) }
        publish()
    }

    private fun primary(): MediaController? =
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.metadata != null }

    private fun publish() {
        val c = primary()
        val md = c?.metadata
        if (c == null || md == null) {
            state.value = MediaState.Idle
            return
        }
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val label = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrNull()
        state.value = MediaState.Active(
            NowPlaying(
                title = title,
                artist = artist,
                art = art?.let { scaleArt(it) },
                appIcon = appIcons.getOrPut(c.packageName) {
                    runCatching { context.packageManager.getApplicationIcon(c.packageName).toBitmap(96, 96) }.getOrNull()
                },
                isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                appLabel = label,
                packageName = c.packageName,
            ),
        )
    }

    /** Las carátulas pueden ser enormes: se reducen para no gastar memoria. */
    private fun scaleArt(b: Bitmap): Bitmap {
        val max = 256
        if (b.width <= max && b.height <= max) return b
        val s = max.toFloat() / maxOf(b.width, b.height)
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true)
    }

    fun playPause() {
        val c = primary() ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() = primary()?.transportControls?.skipToNext()
    fun previous() = primary()?.transportControls?.skipToPrevious()

    /** Abre la app que está sonando. */
    fun openPlayer(): Boolean {
        val c = primary() ?: return false
        val intent = c.sessionActivity
        if (intent != null) return runCatching { intent.send(); true }.getOrDefault(false)
        val launch = context.packageManager.getLaunchIntentForPackage(c.packageName) ?: return false
        return runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }
}
