package com.carvision.launcher.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.CardShape
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvIconsCustom
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.pressable
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.PackState
import kotlinx.coroutines.launch

/** true si CarVision es ahora mismo la pantalla de inicio de la radio. */
fun isDefaultHome(ctx: Context): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val r = ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    return r?.activityInfo?.packageName == ctx.packageName
}

private enum class Step(val title: String, val icon: ImageVector) {
    HOME("Pantalla de inicio", CvIcons.Home),
    CAR("Tu coche", CvIconsCustom.Vehicle),
    MUSIC("Música", CvIcons.Music),
    DONE("¡Listo!", CvIcons.ShieldCheck),
}

/**
 * Asistente de primer arranque (D-028): pocos pasos, cada uno se marca solo cuando está hecho y ninguno es
 * obligatorio. No pide permisos especiales: solo abre los ajustes de Android que hacen falta.
 */
@Composable
fun SetupWizard(container: AppContainer, onClose: () -> Unit) {
    val c = CvTheme.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.entries[container.wizardStep.coerceIn(0, Step.entries.lastIndex)]) }
    androidx.compose.runtime.LaunchedEffect(step) { container.wizardStep = step.ordinal }
    // Los permisos se conceden fuera de la app: al volver se comprueba todo otra vez.
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val homeOk = remember(refresh) { isDefaultHome(ctx) }
    val musicOk = remember(refresh) { container.media.hasPermission() }
    val pack by container.packs.current.collectAsStateWithLifecycle()
    val manifest = (pack as? PackState.Ready)?.pack?.manifest
    val done = mapOf(Step.HOME to homeOk, Step.CAR to (manifest != null), Step.MUSIC to musicOk, Step.DONE to false)

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }
    fun requestHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); return }
            }
        }
        if (!ExternalApps.homeSettings(ctx)) container.notifier.show("Pulsa el botón Inicio de la radio y elige CarVision › Siempre")
    }
    fun finish() {
        scope.launch { container.settings.setWizardDone(true) }
        onClose()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (c.isDark) 0.6f else 0.35f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.86f)
                .clip(CardShape)
                .background(if (c.isDark) c.backgroundTop.copy(alpha = 0.98f) else c.surfaceStrong.copy(alpha = 0.99f))
                .border(1.dp, c.surfaceBorder, CardShape),
        ) {
            // Lista de pasos
            Column(
                Modifier.width(250.dp).fillMaxHeight().background(c.surface).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Bienvenido a CarVision", style = CvText.title, color = c.textPrimary)
                Text("3 pasos rápidos. Puedes saltar cualquiera.", style = CvText.label, color = c.textTertiary)
                Spacer(Modifier.height(10.dp))
                Step.entries.forEach { s ->
                    val active = s == step
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) c.selection else Color.Transparent)
                            .pressable { step = s }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val ok = done[s] == true
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(if (ok) c.ok.copy(alpha = 0.22f) else c.surfaceStrong),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if (ok) CvIcons.Check else s.icon, null, tint = if (ok) c.ok else c.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        Text(s.title, style = CvText.heading, color = if (active) c.textPrimary else c.textSecondary)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Más tarde", style = CvText.heading, color = c.textTertiary,
                    modifier = Modifier.pressable { finish() }.padding(8.dp),
                )
            }
            // Paso actual
            Column(Modifier.weight(1f).fillMaxHeight().padding(30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                fun next() { step = Step.entries[(step.ordinal + 1).coerceAtMost(Step.entries.lastIndex)] }
                when (step) {
                    Step.HOME -> {
                        StepTitle("Que CarVision sea tu pantalla de inicio", homeOk)
                        Body("Así, al pulsar el botón Inicio de la radio, verás CarVision. Tu radio, CarPlay, la cámara " +
                            "y las demás apps siguen funcionando igual. Puedes volver al launcher original cuando quieras " +
                            "en Ajustes › Sistema.")
                        Body("Si la radio pregunta qué app usar, elige CarVision y toca «Siempre».")
                        Spacer(Modifier.weight(1f))
                        Buttons(if (homeOk) "Siguiente" else "Poner como inicio", { if (homeOk) next() else requestHome() }, if (homeOk) null else "Saltar", ::next)
                    }
                    Step.CAR -> {
                        StepTitle("Tu coche", manifest != null)
                        Body(
                            if (manifest != null) "Ahora se muestra: ${manifest.name}. Elige el color de tu coche; se cambia al instante."
                            else "Cargando el coche…",
                        )
                        if (manifest != null) PaintPicker(container, manifest.id)
                        Body("¿Tienes el pack de tu coche (.zip)? Pincha el pendrive o ábrelo en el Garaje.")
                        Spacer(Modifier.weight(1f))
                        Buttons("Siguiente", ::next, "Abrir el Garaje") {
                            container.wizardReturnAfterSheet = true
                            onClose()
                            container.sheet = Sheet.GARAGE
                        }
                    }
                    Step.MUSIC -> {
                        StepTitle("Ver la música que suena (opcional)", musicOk)
                        Body("Para mostrar la carátula, el título y los botones de cualquier app de música, Android pide " +
                            "permitir a CarVision el «acceso a notificaciones». CarVision solo lee qué se está " +
                            "reproduciendo: no lee ni guarda tus mensajes.")
                        Body("En la pantalla que se abre, activa CarVision y vuelve con el botón Atrás.")
                        Spacer(Modifier.weight(1f))
                        Buttons(if (musicOk) "Siguiente" else "Permitir", {
                            if (musicOk) next() else runCatching { ctx.startActivity(container.media.permissionIntent()) }
                                .onFailure { container.notifier.show("Esta radio no tiene ese ajuste") }
                        }, if (musicOk) null else "Saltar", ::next)
                    }
                    Step.DONE -> {
                        StepTitle("¡Todo listo!", true)
                        Body("Puedes volver a abrir este asistente en Ajustes › Sistema › Asistente de inicio.")
                        Body("Para cambiar de coche o de color: icono del Garaje, arriba, sobre el coche.")
                        Spacer(Modifier.weight(1f))
                        Buttons("Empezar", ::finish, null) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTitle(text: String, ok: Boolean) {
    val c = CvTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text, style = CvText.display, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false))
        if (ok) Icon(CvIcons.Check, "Hecho", tint = c.ok, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = CvText.body, color = CvTheme.colors.textSecondary)
}

@Composable
private fun Buttons(primary: String, onPrimary: () -> Unit, secondary: String?, onSecondary: () -> Unit) {
    val c = CvTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            primary, style = CvText.heading, color = c.onPrimaryButton,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(c.primaryButton).pressable(onClick = onPrimary)
                .padding(horizontal = 28.dp, vertical = 14.dp),
        )
        if (secondary != null) Text(
            secondary, style = CvText.heading, color = c.textPrimary,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(c.surface)
                .border(1.dp, c.surfaceBorder, RoundedCornerShape(24.dp)).pressable(onClick = onSecondary)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        )
    }
}
