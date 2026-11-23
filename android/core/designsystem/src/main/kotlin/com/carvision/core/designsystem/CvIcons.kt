// Archivo generado por scripts/svg_icons_to_compose.py. No editar a mano.
// Iconos: Lucide (https://lucide.dev), licencia ISC. Ver docs/third-party/Lucide-LICENSE.txt
package com.carvision.core.designsystem

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Iconos de línea de CarVision (trazo uniforme, sin aspecto Android). */
object CvIcons {
    private const val STROKE = 1.6f

    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Home: ImageVector by lazy {
        icon(
            "Home",
            "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
            "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        )
    }

    val Navigation: ImageVector by lazy {
        icon(
            "Navigation",
            "M3,11 L22,2 L13,21 L11,13 L3,11 Z",
        )
    }

    val Music: ImageVector by lazy {
        icon(
            "Music",
            "M21 16V3a1 1 0 0 0-1.28-.96l-10 2.917a1 1 0 0 0-.72.96V19",
            "M15.0,16.0 a3.0,3.0 0 1,0 6.0,0 a3.0,3.0 0 1,0 -6.0,0 Z",
            "M3.0,19.0 a3.0,3.0 0 1,0 6.0,0 a3.0,3.0 0 1,0 -6.0,0 Z",
        )
    }

    val Phone: ImageVector by lazy {
        icon(
            "Phone",
            "M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384",
        )
    }

    val Apps: ImageVector by lazy {
        icon(
            "Apps",
            "M4.0,3.0 h5.0 a1.0,1.0 0 0 1 1.0,1.0 v5.0 a1.0,1.0 0 0 1 -1.0,1.0 h-5.0 a1.0,1.0 0 0 1 -1.0,-1.0 v-5.0 a1.0,1.0 0 0 1 1.0,-1.0 Z",
            "M15.0,3.0 h5.0 a1.0,1.0 0 0 1 1.0,1.0 v5.0 a1.0,1.0 0 0 1 -1.0,1.0 h-5.0 a1.0,1.0 0 0 1 -1.0,-1.0 v-5.0 a1.0,1.0 0 0 1 1.0,-1.0 Z",
            "M15.0,14.0 h5.0 a1.0,1.0 0 0 1 1.0,1.0 v5.0 a1.0,1.0 0 0 1 -1.0,1.0 h-5.0 a1.0,1.0 0 0 1 -1.0,-1.0 v-5.0 a1.0,1.0 0 0 1 1.0,-1.0 Z",
            "M4.0,14.0 h5.0 a1.0,1.0 0 0 1 1.0,1.0 v5.0 a1.0,1.0 0 0 1 -1.0,1.0 h-5.0 a1.0,1.0 0 0 1 -1.0,-1.0 v-5.0 a1.0,1.0 0 0 1 1.0,-1.0 Z",
        )
    }

    val Car: ImageVector by lazy {
        icon(
            "Car",
            "m21 8-2 2-1.5-3.7A2 2 0 0 0 15.646 5H8.4a2 2 0 0 0-1.903 1.257L5 10 3 8",
            "M7 14h.01",
            "M17 14h.01",
            "M5.0,10.0 h14.0 a2.0,2.0 0 0 1 2.0,2.0 v4.0 a2.0,2.0 0 0 1 -2.0,2.0 h-14.0 a2.0,2.0 0 0 1 -2.0,-2.0 v-4.0 a2.0,2.0 0 0 1 2.0,-2.0 Z",
            "M5 18v2",
            "M19 18v2",
        )
    }

    val Service: ImageVector by lazy {
        icon(
            "Service",
            "M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.106-3.105c.32-.322.863-.22.983.218a6 6 0 0 1-8.259 7.057l-7.91 7.91a1 1 0 0 1-2.999-3l7.91-7.91a6 6 0 0 1 7.057-8.259c.438.12.54.662.219.984z",
        )
    }

    val Settings: ImageVector by lazy {
        icon(
            "Settings",
            "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
            "M9.0,12.0 a3.0,3.0 0 1,0 6.0,0 a3.0,3.0 0 1,0 -6.0,0 Z",
        )
    }

    val Play: ImageVector by lazy {
        icon(
            "Play",
            "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z",
        )
    }

    val Pause: ImageVector by lazy {
        icon(
            "Pause",
            "M15.0,3.0 h3.0 a1.0,1.0 0 0 1 1.0,1.0 v16.0 a1.0,1.0 0 0 1 -1.0,1.0 h-3.0 a1.0,1.0 0 0 1 -1.0,-1.0 v-16.0 a1.0,1.0 0 0 1 1.0,-1.0 Z",
            "M6.0,3.0 h3.0 a1.0,1.0 0 0 1 1.0,1.0 v16.0 a1.0,1.0 0 0 1 -1.0,1.0 h-3.0 a1.0,1.0 0 0 1 -1.0,-1.0 v-16.0 a1.0,1.0 0 0 1 1.0,-1.0 Z",
        )
    }

    val Previous: ImageVector by lazy {
        icon(
            "Previous",
            "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z",
            "M3 20V4",
        )
    }

    val Next: ImageVector by lazy {
        icon(
            "Next",
            "M21 4v16",
            "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z",
        )
    }

    val DoorOpen: ImageVector by lazy {
        icon(
            "DoorOpen",
            "M10 21H2",
            "M10 3H7a2 2 0 00-2 2v16",
            "M14 12h.01",
            "M19 21V5a2 2 0 00-1.675-1.974l-6.163-1.013A1 1 0 0010 3v18a1 1 0 001.124.992z",
            "M22 21h-3",
        )
    }

    val DoorClosed: ImageVector by lazy {
        icon(
            "DoorClosed",
            "M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16",
            "M2 21h20",
            "M9 12h.01",
        )
    }

    val Speed: ImageVector by lazy {
        icon(
            "Speed",
            "m12 14 4-4",
            "M3.34 19a10 10 0 1 1 17.32 0",
        )
    }

    val Rpm: ImageVector by lazy {
        icon(
            "Rpm",
            "M22 12h-2.48a2 2 0 0 0-1.93 1.46l-2.35 8.36a.25.25 0 0 1-.48 0L9.24 2.18a.25.25 0 0 0-.48 0l-2.35 8.36A2 2 0 0 1 4.49 12H2",
        )
    }

    val Fuel: ImageVector by lazy {
        icon(
            "Fuel",
            "M14 13h2a2 2 0 0 1 2 2v2a2 2 0 0 0 4 0v-6.998a2 2 0 0 0-.59-1.42L18 5",
            "M14 21V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v16",
            "M2 21h13",
            "M3 9h11",
        )
    }

    val Temperature: ImageVector by lazy {
        icon(
            "Temperature",
            "M14 4v10.54a4 4 0 1 1-4 0V4a2 2 0 0 1 4 0Z",
        )
    }

    val Battery: ImageVector by lazy {
        icon(
            "Battery",
            "M10 14v-4",
            "M22 14v-4",
            "M6 14v-4",
            "M4.0,6.0 h12.0 a2.0,2.0 0 0 1 2.0,2.0 v8.0 a2.0,2.0 0 0 1 -2.0,2.0 h-12.0 a2.0,2.0 0 0 1 -2.0,-2.0 v-8.0 a2.0,2.0 0 0 1 2.0,-2.0 Z",
        )
    }

    val Odometer: ImageVector by lazy {
        icon(
            "Odometer",
            "M12 13v8",
            "M12 3v3",
            "M18.172 6a2 2 0 0 1 1.414.586l2.06 2.06a1.207 1.207 0 0 1 0 1.708l-2.06 2.06a2 2 0 0 1-1.414.586H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z",
        )
    }

    val Lights: ImageVector by lazy {
        icon(
            "Lights",
            "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5",
            "M9 18h6",
            "M10 22h4",
        )
    }

    val View3d: ImageVector by lazy {
        icon(
            "View3d",
            "m15.194 13.707 3.814 1.86-1.86 3.814",
            "M16.47214 7.52786 A 5 10 0 1 0 13 21.79796",
            "M21.79796 11 A 10 5 0 1 0 19 15.57071",
        )
    }

    val TopView: ImageVector by lazy {
        icon(
            "TopView",
            "M5 21a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2",
            "M9 21h1",
            "M14 21h1",
        )
    }

    val Recenter: ImageVector by lazy {
        icon(
            "Recenter",
            "M2,12 L5,12",
            "M19,12 L22,12",
            "M12,2 L12,5",
            "M12,19 L12,22",
            "M5.0,12.0 a7.0,7.0 0 1,0 14.0,0 a7.0,7.0 0 1,0 -14.0,0 Z",
            "M9.0,12.0 a3.0,3.0 0 1,0 6.0,0 a3.0,3.0 0 1,0 -6.0,0 Z",
        )
    }

    val CanData: ImageVector by lazy {
        icon(
            "CanData",
            "M4.9 16.1C1 12.2 1 5.8 4.9 1.9",
            "M7.8 4.7a6.14 6.14 0 0 0-.8 7.5",
            "M10.0,9.0 a2.0,2.0 0 1,0 4.0,0 a2.0,2.0 0 1,0 -4.0,0 Z",
            "M16.2 4.8c2 2 2.26 5.11.8 7.47",
            "M19.1 1.9a9.96 9.96 0 0 1 0 14.1",
            "M9.5 18h5",
            "m8 22 4-11 4 11",
        )
    }

    val NoConnection: ImageVector by lazy {
        icon(
            "NoConnection",
            "m19 5 3-3",
            "m2 22 3-3",
            "M6.3 20.3a2.4 2.4 0 0 0 3.4 0L12 18l-6-6-2.3 2.3a2.4 2.4 0 0 0 0 3.4Z",
            "M7.5 13.5 10 11",
            "M10.5 16.5 13 14",
            "m12 6 6 6 2.3-2.3a2.4 2.4 0 0 0 0-3.4l-2.6-2.6a2.4 2.4 0 0 0-3.4 0Z",
        )
    }

    val Demo: ImageVector by lazy {
        icon(
            "Demo",
            "M14 2v6a2 2 0 0 0 .245.96l5.51 10.08A2 2 0 0 1 18 22H6a2 2 0 0 1-1.755-2.96l5.51-10.08A2 2 0 0 0 10 8V2",
            "M6.453 15h11.094",
            "M8.5 2h7",
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon(
            "ChevronRight",
            "m9 18 6-6-6-6",
        )
    }

    val Check: ImageVector by lazy {
        icon(
            "Check",
            "M20 6 9 17l-5-5",
        )
    }

    val Close: ImageVector by lazy {
        icon(
            "Close",
            "M18 6 6 18",
            "m6 6 12 12",
        )
    }

    val Bluetooth: ImageVector by lazy {
        icon(
            "Bluetooth",
            "m7 7 10 10-5 5V2l5 5L7 17",
        )
    }

    val Wifi: ImageVector by lazy {
        icon(
            "Wifi",
            "M12 20h.01",
            "M2 8.82a15 15 0 0 1 20 0",
            "M5 12.859a10 10 0 0 1 14 0",
            "M8.5 16.429a5 5 0 0 1 7 0",
        )
    }

    val Sun: ImageVector by lazy {
        icon(
            "Sun",
            "M8.0,12.0 a4.0,4.0 0 1,0 8.0,0 a4.0,4.0 0 1,0 -8.0,0 Z",
            "M12 2v2",
            "M12 20v2",
            "m4.93 4.93 1.41 1.41",
            "m17.66 17.66 1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "m6.34 17.66-1.41 1.41",
            "m19.07 4.93-1.41 1.41",
        )
    }

    val Moon: ImageVector by lazy {
        icon(
            "Moon",
            "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401",
        )
    }

    val SunMoon: ImageVector by lazy {
        icon(
            "SunMoon",
            "M12 2v2",
            "M14.837 16.385a6 6 0 1 1-7.223-7.222c.624-.147.97.66.715 1.248a4 4 0 0 0 5.26 5.259c.589-.255 1.396.09 1.248.715",
            "M16 12a4 4 0 0 0-4-4",
            "m19 5-1.256 1.256",
            "M20 12h2",
        )
    }

    val Map: ImageVector by lazy {
        icon(
            "Map",
            "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z",
            "M15 5.764v15",
            "M9 3.236v15",
        )
    }

    val ShieldCheck: ImageVector by lazy {
        icon(
            "ShieldCheck",
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
            "m9 12 2 2 4-4",
        )
    }

    val Bell: ImageVector by lazy {
        icon(
            "Bell",
            "M10.268 21a2 2 0 0 0 3.464 0",
            "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326",
        )
    }

    val Info: ImageVector by lazy {
        icon(
            "Info",
            "M2.0,12.0 a10.0,10.0 0 1,0 20.0,0 a10.0,10.0 0 1,0 -20.0,0 Z",
            "M12 16v-4",
            "M12 8h.01",
        )
    }

}
