package com.carvision.launcher.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import com.carvision.launcher.media.NowPlayingRepository

/**
 * SOLO EN DEPURACIÓN. Publica una sesión multimedia de prueba con carátula generada, para probar la tarjeta de
 * música en emuladores sin reproductor. No existe en la compilación release.
 *
 * adb shell am broadcast -n com.carvision.launcher/.debug.DebugMediaReceiver --es title "Canción" --es artist "Artista" --ef hue 210
 * adb shell am broadcast -n com.carvision.launcher/.debug.DebugMediaReceiver --ez stop true
 */
class DebugMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("stop", false)) {
            session?.run { isActive = false; release() }
            session = null
            return
        }
        val s = session ?: MediaSession(context.applicationContext, NowPlayingRepository.DEBUG_SESSION_TAG).also { session = it }
        val hue = intent.getFloatExtra("hue", 210f)
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, intent.getStringExtra("title") ?: "Canción de prueba")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, intent.getStringExtra("artist") ?: "Artista")
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art(hue))
                .build(),
        )
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .build(),
        )
        s.isActive = true
    }

    /** Carátula sintética: degradado diagonal de dos tonos con un círculo, a partir de un matiz. */
    private fun art(hue: Float): Bitmap {
        val b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val a = Color.HSVToColor(floatArrayOf(hue, 0.75f, 0.85f))
        val d = Color.HSVToColor(floatArrayOf((hue + 40f) % 360f, 0.8f, 0.35f))
        c.drawRect(0f, 0f, 300f, 300f, Paint().apply { shader = LinearGradient(0f, 0f, 300f, 300f, a, d, Shader.TileMode.CLAMP) })
        c.drawCircle(190f, 120f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 255, 255, 255) })
        return b
    }

    private companion object {
        var session: MediaSession? = null
    }
}
