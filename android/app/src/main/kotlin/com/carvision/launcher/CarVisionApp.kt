package com.carvision.launcher

import android.app.Application
import com.carvision.launcher.data.AppContainer

class CarVisionApp : Application() {
    /** Contenedor de dependencias (inyección manual, DECISIONS.md D-006). */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
