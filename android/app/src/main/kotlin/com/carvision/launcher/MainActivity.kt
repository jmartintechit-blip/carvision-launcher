package com.carvision.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.carvision.launcher.ui.Destination
import com.carvision.launcher.ui.LauncherRoot

class MainActivity : ComponentActivity() {

    /** Pantalla visible. Vive en la actividad para que "Inicio" y "Atrás" la puedan cambiar. */
    private val destination = mutableStateOf(Destination.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        // Al mostrar el teclado, Android saca las barras del sistema: cuando el teclado se cierra se vuelven a ocultar.
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val barsVisible = insets.isVisible(WindowInsetsCompat.Type.systemBars())
            if (!imeVisible && barsVisible) {
                v.removeCallbacks(rehide)
                v.postDelayed(rehide, 1200)
            }
            ViewCompat.onApplyWindowInsets(v, insets)
        }
        val container = (application as CarVisionApp).container

        // Como pantalla de inicio, "Atrás" nunca cierra el launcher: vuelve a Inicio.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                destination.value = Destination.HOME
            }
        })

        // Pendrive automático: Android avisa al montar o quitar un USB (sin permisos de almacenamiento).
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_MEDIA_MOUNTED)
            addAction(android.content.Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(android.content.Intent.ACTION_MEDIA_REMOVED)
            addAction(android.content.Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        registerReceiver(usbReceiver, filter)
        container.usb.refresh()

        setContent {
            LauncherRoot(
                container = container,
                destination = destination.value,
                onNavigate = { destination.value = it },
            )
        }
    }

    private val rehide = Runnable { hideSystemBars() }

    private val usbReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            val usb = (application as CarVisionApp).container.usb
            if (intent.action == android.content.Intent.ACTION_MEDIA_MOUNTED) usb.onMounted() else usb.refresh()
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(usbReceiver) }
        super.onDestroy()
    }

    /**
     * Pantalla completa: se ocultan la barra de estado y los botones de Android para que la radio parezca de serie.
     * Siguen accesibles deslizando desde el borde, y se vuelven a ocultar solas.
     */
    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        // El acceso a notificaciones se concede fuera de la app: se comprueba al volver.
        (application as CarVisionApp).container.media.start()
        (application as CarVisionApp).container.usb.refresh()
    }

    /** Pulsar "Inicio" estando ya en el launcher vuelve a la pantalla principal. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        destination.value = Destination.HOME
    }
}
