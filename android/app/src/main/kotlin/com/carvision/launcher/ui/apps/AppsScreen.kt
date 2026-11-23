package com.carvision.launcher.ui.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carvision.core.designsystem.AppIconFrame
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.SquircleShape
import com.carvision.core.designsystem.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class LaunchableApp(val label: String, val packageName: String, val activity: String, val icon: ImageBitmap, val fullBleed: Boolean)

/**
 * Cajón de aplicaciones. Todos los iconos se presentan con la misma forma squircle y tamaño, como en los
 * sistemas de serie, aunque cada app traiga su propio estilo de icono.
 */
@Composable
fun AppsScreen() {
    val ctx = LocalContext.current
    val c = CvTheme.colors
    val apps by produceState<List<LaunchableApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadApps(ctx) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Aplicaciones", style = CvText.display, color = c.textPrimary, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
        val list = apps
        if (list == null) {
            Text("Cargando…", style = CvText.body, color = c.textSecondary, modifier = Modifier.padding(start = 8.dp))
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(118.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(list, key = { it.packageName + it.activity }) { app ->
                    Column(
                        Modifier.fillMaxWidth().pressable { launch(ctx, app) }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppIconFrame(
                            72.dp,
                            Modifier
                                .shadow(10.dp, SquircleShape, clip = false)
                                .background(if (app.fullBleed) c.surface else c.surfaceStrong, SquircleShape),
                        ) {
                            Image(
                                app.icon, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = if (app.fullBleed) Modifier.fillMaxSize() else Modifier.size(52.dp),
                            )
                        }
                        Text(
                            app.label, style = CvText.label, color = c.textPrimary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Icono a sangre completa: en los iconos adaptativos (Android 8+) se dibujan las capas de fondo y primer plano
 * sin la máscara del sistema, para recortarlas después con la forma squircle propia.
 */
private fun renderIcon(d: Drawable, px: Int): Pair<Bitmap, Boolean> {
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    if (d is AdaptiveIconDrawable) {
        // Las capas miden 108 unidades y la zona visible son las 72 centrales: se amplían un 50 % y se centran.
        val inset = px / 4
        listOfNotNull(d.background, d.foreground).forEach { layer ->
            layer.setBounds(-inset, -inset, px + inset, px + inset)
            layer.draw(canvas)
        }
        return bmp to true
    }
    d.setBounds(0, 0, px, px)
    d.draw(canvas)
    return bmp to false
}

private fun loadApps(ctx: Context): List<LaunchableApp> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    val infos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    val iconPx = (72 * ctx.resources.displayMetrics.density).toInt().coerceIn(96, 216)
    return infos
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map {
            val (bmp, full) = renderIcon(it.loadIcon(pm), iconPx)
            LaunchableApp(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                activity = it.activityInfo.name,
                icon = bmp.asImageBitmap(),
                fullBleed = full,
            )
        }
        .sortedBy { it.label.lowercase() }
}

private fun launch(ctx: Context, app: LaunchableApp) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setClassName(app.packageName, app.activity)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { ctx.startActivity(intent) }
}
