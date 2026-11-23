package com.carvision.render3d

import android.app.ActivityManager
import android.content.Context

/**
 * Perfil de la radio. Muchas radios baratas llevan 1 GB de RAM y Android Go (p. ej. Allwinner A133 con Android 10 Go):
 * ahí se usa el «modo ligero» (sin MSAA en el 3D y a unos 30 fotogramas por segundo mientras algo se mueve) para dejar
 * memoria y GPU al resto de la radio (música, navegación, Bluetooth).
 */
object DeviceProfile {
    /** Por debajo de esta memoria total se considera una radio de poca memoria (1 GB reales suelen dar ~0,9-1,4 GB). */
    private const val LOW_MEMORY_BYTES = 1_600L * 1024 * 1024

    @Volatile private var cached: Boolean? = null

    /** Android Go (dispositivo de poca RAM) o menos de ~1,6 GB de memoria total. */
    fun isLowMemory(context: Context): Boolean = cached ?: run {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        (am.isLowRamDevice || info.totalMem in 1 until LOW_MEMORY_BYTES).also { cached = it }
    }

    /** Android Go: Ajustes no deja conceder el acceso a notificaciones (el que usa la tarjeta de música). */
    fun isAndroidGo(context: Context): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
}
