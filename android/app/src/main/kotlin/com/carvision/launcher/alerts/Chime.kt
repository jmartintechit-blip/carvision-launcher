package com.carvision.launcher.alerts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sonido de aviso propio, sintetizado al vuelo (sin archivos de audio de terceros): dos notas suaves con caída
 * exponencial, ~0,35 s, volumen moderado. Pensado para avisar sin sobresaltar.
 */
object Chime {
    private const val RATE = 44_100

    private val pcm: ShortArray by lazy {
        val notes = listOf(880.0 to 0.16, 1174.66 to 0.22) // La5 → Re6
        val out = ArrayList<Short>()
        notes.forEach { (freq, dur) ->
            val n = (RATE * dur).toInt()
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                val attack = (i / (RATE * 0.008)).coerceAtMost(1.0)
                val env = attack * exp(-t * 9.0)
                val v = (sin(2 * PI * freq * t) * 0.6 + sin(2 * PI * freq * 2 * t) * 0.12) * env * 0.32
                out += (v * Short.MAX_VALUE).toInt().toShort()
            }
        }
        out.toShortArray()
    }

    fun play() {
        runCatching {
            val data = pcm
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * 2)
                .build()
            track.write(data, 0, data.size)
            track.setNotificationMarkerPosition(data.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) { t.release() }
                override fun onPeriodicNotification(t: AudioTrack) = Unit
            })
            track.play()
        }
    }
}
