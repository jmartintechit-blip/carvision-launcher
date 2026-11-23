package com.carvision.render3d

import android.content.Context
import android.opengl.Matrix
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import com.carvision.vehicle.packs.AnimatedPart
import com.carvision.vehicle.packs.LoadedVehiclePack
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Vista del coche. */
enum class CarViewMode { PERSPECTIVE, TOP }

/**
 * Renderizador del coche con Filament.
 *
 * Diseño:
 * - Fondo transparente (TextureView no opaco) para que la interfaz Compose se vea detrás.
 * - Solo dibuja cuando algo cambia (giro, inercia, puertas, cámara). En reposo no consume GPU.
 * - No conoce ningún coche concreto: nombres de nodos, ejes y ángulos llegan del manifiesto del pack.
 *
 * Debe crearse y usarse en el hilo principal.
 */
class CarRenderer(private val context: Context) {

    companion object {
        init {
            Utils.init()
        }

        private const val STAGE_ASSET = "stage/stage.glb"
        private const val IBL_ASSET = "envs/studio_ibl.ktx"
        private const val TOP_PITCH = 88f
        private const val TOP_YAW = 180f
        /** Bienvenida: la cámara arranca girada, más alta y más lejos, y se acerca rodeando el coche. */
        private const val INTRO_YAW = 150f
        private const val INTRO_PITCH = 12f
        private const val INTRO_DOLLY = 1.32f
        private const val INTRO_SEC = 3.0f
        /** Nombre del plano de sombra de contacto dentro del GLB del coche (pipeline de Blender). */
        private const val SHADOW_NODE = "contact_shadow"
    }

    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val displayHelper = DisplayHelper(context)
    private var swapChain: SwapChain? = null

    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)
    private var carAsset: FilamentAsset? = null
    private var stageAsset: FilamentAsset? = null
    private var indirectLight: IndirectLight? = null
    private var iblTexture: com.google.android.filament.Texture? = null
    private val sun = EntityManager.get().create()

    // --- Encuadre (se calcula al cargar el coche) ---
    private var target = floatArrayOf(0f, 0.6f, 0f)
    private var radius = 2.5f
    private var aspect = 16f / 9f
    private var fovDeg = 28f
    private var zoom = 1f
    private var minPitch = 4f
    private var maxPitch = 55f
    private var homeYaw = 35f
    private var homePitch = 16f

    // --- Estado de cámara (grados) ---
    private var yaw = 35f
    private var pitch = 16f
    private var yawVelocity = 0f
    private var cameraAnim: CameraAnim? = null
    /** Animación de bienvenida: preparada (pose inicial puesta, esperando) o en curso. */
    private var introArmed = false
    private var intro: IntroAnim? = null
    /** Multiplicador de distancia (acercamiento de la bienvenida). */
    private var dolly = 1f
    private var mode = CarViewMode.PERSPECTIVE
    private var userDragging = false

    // --- Piezas animadas ---
    private class PartState(
        val part: AnimatedPart,
        val instance: Int,
        val rest: FloatArray,
        val focusYaw: Float,
        var amount: Float = 0f,
        var target: Float = 0f,
    )
    private val parts = mutableMapOf<String, PartState>()

    // --- Ruedas: giran según la velocidad recibida ---
    private class WheelState(val instance: Int, val rest: FloatArray, val axis: List<Float>, val radius: Float)
    private val wheels = mutableListOf<WheelState>()
    private var speedMs = 0f
    /** Giro visible de las ruedas, suavizado y limitado para que siempre se vea avanzar (ver [WheelSpin]). */
    private val wheelSpin = WheelSpin()

    /** false mientras la vista no se ve (otra pantalla encima): no se dibuja nada. */
    private var active = true
    /** Si es true, al abrirse una pieza la cámara gira para mostrarla. */
    var autoFocusOpenings: Boolean = true

    private var dirty = true
    private var frameScheduled = false
    private var lastFrameNanos = 0L
    private var destroyed = false

    /** Se llama una vez por coche cargado, cuando ya se ha dibujado en pantalla (3 fotogramas seguidos). */
    var onCarShown: (() -> Unit)? = null
    private var shownFrames = 0
    private var shownReported = false
    private val choreographer = Choreographer.getInstance()
    private val frameCallback = Choreographer.FrameCallback { onFrame(it) }

    /** Se llama cuando cambia el estado visible de la cámara (para la interfaz). */
    var onViewModeChanged: ((CarViewMode) -> Unit)? = null

    /**
     * false si la GPU no ofrece OpenGL ES 3.0 (nivel de funciones 0 de Filament).
     * En ese caso no se carga nada 3D y la interfaz debe usar la vista 2D de respaldo.
     */
    val isSupported: Boolean = engine.activeFeatureLevel != Engine.FeatureLevel.FEATURE_LEVEL_0

    init {
        if (isSupported) setupScene()
    }

    private fun setupScene() {
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        view.antiAliasing = View.AntiAliasing.FXAA
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
            enabled = true
            sampleCount = 4
        }
        renderer.clearOptions = renderer.clearOptions.apply {
            clear = true
            clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }
        camera.setExposure(16f, 1f / 125f, 100f)

        val ibl = KTX1Loader.createIndirectLight(engine, readAsset(IBL_ASSET))
        iblTexture = ibl.cubemap
        indirectLight = ibl.indirectLight?.apply { intensity = 32_000f }
        scene.indirectLight = indirectLight

        LightManager.Builder(LightManager.Type.SUN)
            .color(1f, 0.97f, 0.92f)
            .intensity(70_000f)
            .direction(0.35f, -1f, -0.55f)
            .sunAngularRadius(1.6f)
            .castShadows(false)
            .build(engine, sun)
        scene.addEntity(sun)

        loadStage()

        uiHelper.isOpaque = false
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
                @Suppress("DEPRECATION")
                val display = context.display ?: (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
                displayHelper.attach(renderer, display)
                requestFrame()
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                aspect = width.toFloat() / height.coerceAtLeast(1)
                updateProjection()
                requestFrame()
            }
        }
    }

    fun attachTo(textureView: TextureView) {
        if (isSupported) uiHelper.attachTo(textureView)
    }

    // ------------------------------------------------------------------ carga

    /** Carga el coche del pack. Sustituye al anterior si lo había. */
    fun loadPack(pack: LoadedVehiclePack) {
        if (!isSupported) return
        destroyCar()
        shownFrames = 0
        shownReported = false
        val glb = pack.readModel()
        val asset = assetLoader.createAsset(glb.toDirectBuffer())
            ?: throw IllegalStateException("No se pudo leer el modelo 3D de ${pack.manifest.id}")
        resourceLoader.loadResources(asset)
        findPaint(asset, glb, pack.manifest.paint.material)
        applyPaint()
        asset.releaseSourceData()
        carAsset = asset

        // Coloca el coche: centrado en X/Z, ruedas apoyadas en y = 0, frontal hacia +Z.
        // La caja envolvente excluye la sombra de contacto (es un plano mucho mayor que el coche).
        val (c, h) = carBounds(asset)
        val m = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        Matrix.rotateM(m, 0, pack.manifest.model.forwardYawDeg, 0f, 1f, 0f)
        Matrix.translateM(m, 0, -c[0], -(c[1] - h[1]), -c[2])
        val tm = engine.transformManager
        tm.setTransform(tm.getInstance(asset.root), m)
        scene.addEntities(asset.entities)

        radius = sqrt(h[0] * h[0] + h[1] * h[1] + h[2] * h[2])
        target = floatArrayOf(0f, h[1] * 0.75f, 0f)
        scaleStage(maxOf(h[0], h[2]))

        val cam = pack.manifest.camera
        fovDeg = cam.fovDeg
        zoom = cam.zoom
        minPitch = cam.minPitchDeg
        maxPitch = cam.maxPitchDeg
        homeYaw = cam.homeYawDeg
        homePitch = cam.homePitchDeg
        yaw = homeYaw
        pitch = homePitch
        mode = CarViewMode.PERSPECTIVE
        if (introArmed) setIntroStartPose()
        updateProjection()

        wheels.clear()
        pack.manifest.wheels.forEach { w ->
            val e = asset.getFirstEntityByName(w.node)
            if (e == 0) return@forEach
            val inst = tm.getInstance(e)
            wheels += WheelState(inst, FloatArray(16).also { tm.getTransform(inst, it) }, w.axis, w.radiusM)
        }

        parts.clear()
        pack.manifest.parts.forEach { part ->
            val entity = asset.getFirstEntityByName(part.node)
            if (entity == 0) return@forEach // pieza ausente: el validador del pack ya lo avisa
            val inst = tm.getInstance(entity)
            val rest = FloatArray(16).also { tm.getTransform(inst, it) }
            // Hacia dónde mirar para ver esta pieza: dirección horizontal de su bisagra respecto al centro.
            val world = FloatArray(16).also { tm.getWorldTransform(inst, it) }
            val focus = Math.toDegrees(atan2(world[12].toDouble(), world[14].toDouble())).toFloat()
            parts[part.signal] = PartState(part, inst, rest, focus)
        }
        requestFrame()
    }

    /** Centro y semiextensión del coche, sin contar los nodos que no son carrocería (sombra de contacto). */
    private fun carBounds(asset: FilamentAsset): Pair<FloatArray, FloatArray> {
        val rm = engine.renderableManager
        val tm = engine.transformManager
        val mn = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val mx = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        val world = FloatArray(16)
        val box = com.google.android.filament.Box()
        var any = false
        for (e in asset.renderableEntities) {
            if (asset.getName(e) == SHADOW_NODE || !rm.hasComponent(e)) continue
            rm.getAxisAlignedBoundingBox(rm.getInstance(e), box)
            tm.getWorldTransform(tm.getInstance(e), world)
            val bc = box.center
            val bh = box.halfExtent
            for (sx in -1..1 step 2) for (sy in -1..1 step 2) for (sz in -1..1 step 2) {
                val lx = bc[0] + sx * bh[0]; val ly = bc[1] + sy * bh[1]; val lz = bc[2] + sz * bh[2]
                for (i in 0..2) {
                    val v = world[i] * lx + world[4 + i] * ly + world[8 + i] * lz + world[12 + i]
                    if (v < mn[i]) mn[i] = v
                    if (v > mx[i]) mx[i] = v
                }
            }
            any = true
        }
        if (!any) {
            val b = asset.boundingBox
            return b.center to b.halfExtent
        }
        return floatArrayOf((mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2, (mn[2] + mx[2]) / 2) to
            floatArrayOf((mx[0] - mn[0]) / 2, (mx[1] - mn[1]) / 2, (mx[2] - mn[2]) / 2)
    }

    private fun loadStage() {
        val bytes = try { readAsset(STAGE_ASSET) } catch (e: Exception) { return }
        val asset = assetLoader.createAsset(bytes) ?: return
        resourceLoader.loadResources(asset)
        asset.releaseSourceData()
        scene.addEntities(asset.entities)
        stageAsset = asset
    }

    /** El escenario está modelado para un coche de 2,2 m de semilongitud; se escala al coche real. */
    private fun scaleStage(carHalfLength: Float) {
        val stage = stageAsset ?: return
        val s = carHalfLength / 2.5f
        val m = FloatArray(16).also { Matrix.setIdentityM(it, 0); Matrix.scaleM(it, 0, s, s, s) }
        val tm = engine.transformManager
        tm.setTransform(tm.getInstance(stage.root), m)
    }

    // ------------------------------------------------------------- control

    /** Arrastre del usuario, en píxeles. */
    fun onDrag(dxPx: Float, dyPx: Float, viewWidthPx: Int) {
        if (mode == CarViewMode.TOP) return
        userDragging = true
        cameraAnim = null
        cancelIntro()
        val degPerPx = 220f / viewWidthPx.coerceAtLeast(1)
        yaw -= dxPx * degPerPx
        pitch = (pitch + dyPx * degPerPx * 0.6f).coerceIn(minPitch, maxPitch)
        yawVelocity = 0f
        requestFrame()
    }

    /** Fin del arrastre con velocidad horizontal en px/s (inercia). */
    fun onDragEnd(velocityXPxPerSec: Float, viewWidthPx: Int) {
        userDragging = false
        if (mode == CarViewMode.TOP) return
        yawVelocity = -velocityXPxPerSec * 220f / viewWidthPx.coerceAtLeast(1)
        requestFrame()
    }

    fun resetView() = setViewMode(CarViewMode.PERSPECTIVE, force = true)

    /**
     * Prepara la bienvenida: deja la cámara en la pose inicial (girada, alta y alejada) y espera a [playIntro].
     * Se llama mientras se ve el logotipo de arranque, para que al desaparecer no haya saltos.
     */
    fun armIntro() {
        introArmed = true
        intro = null
        cameraAnim = null
        yawVelocity = 0f
        if (mode == CarViewMode.PERSPECTIVE) setIntroStartPose()
        requestFrame()
    }

    /** Lanza la bienvenida: la cámara rodea el coche y se acerca hasta la vista principal (unos 3 s). */
    fun playIntro() {
        if (!introArmed) return
        introArmed = false
        if (mode != CarViewMode.PERSPECTIVE) return
        intro = IntroAnim(yaw, pitch, dolly)
        requestFrame()
    }

    private fun setIntroStartPose() {
        yaw = homeYaw - INTRO_YAW
        pitch = (homePitch + INTRO_PITCH).coerceAtMost(maxPitch)
        dolly = INTRO_DOLLY
        dirty = true
    }

    /** El usuario toma el control: se corta la bienvenida (la distancia vuelve suavemente a la normal). */
    private fun cancelIntro() {
        introArmed = false
        intro = null
    }

    fun setViewMode(newMode: CarViewMode, force: Boolean = false) {
        if (newMode == mode && !force) return
        mode = newMode
        yawVelocity = 0f
        cancelIntro()
        if (newMode == CarViewMode.TOP) animateCameraTo(TOP_YAW, TOP_PITCH, 650)
        else animateCameraTo(homeYaw, homePitch, 650)
        onViewModeChanged?.invoke(newMode)
    }

    /**
     * Estado de las aperturas por clave de señal ("door.front_left"…).
     * Las piezas sin clave en el mapa no cambian.
     */
    fun setOpenings(open: Map<String, Boolean>) {
        var focusTarget: Float? = null
        open.forEach { (key, isOpen) ->
            val p = parts[key] ?: return@forEach
            val t = if (isOpen) 1f else 0f
            if (p.target != t) {
                if (isOpen) focusTarget = p.focusYaw
                p.target = t
            }
        }
        val f = focusTarget
        if (f != null && autoFocusOpenings && mode == CarViewMode.PERSPECTIVE && !userDragging) {
            animateCameraTo(f, homePitch + 4f, 800)
        }
        requestFrame()
    }

    /** Adapta el suelo de luz al tema: grafito en oscuro, gris suave en claro. */
    fun setStageTheme(dark: Boolean) {
        val stage = stageAsset ?: return
        val entity = stage.getFirstEntityByName("stage_disc")
        if (entity == 0) return
        val rm = engine.renderableManager
        if (!rm.hasComponent(entity)) return
        val ri = rm.getInstance(entity)
        for (i in 0 until rm.getPrimitiveCount(ri)) {
            val mi = rm.getMaterialInstanceAt(ri, i)
            if (dark) mi.setParameter("baseColorFactor", 0.055f, 0.062f, 0.075f, 0.9f)
            else mi.setParameter("baseColorFactor", 0.30f, 0.32f, 0.36f, 0.22f)
        }
        requestFrame()
    }

    /** Velocidad del vehículo en km/h (null o <1 = parado). Las ruedas giran en consecuencia. */
    fun setSpeedKmh(kmh: Double?) {
        val v = ((kmh ?: 0.0) / 3.6).toFloat()
        val newSpeed = if (v < 0.3f) 0f else v
        if (newSpeed != speedMs) {
            speedMs = newSpeed
            requestFrame()
        }
    }

    /** Pausa (false) o reanuda (true) el dibujo, p. ej. cuando otra pantalla tapa el coche. */
    fun setActive(isActive: Boolean) {
        if (active == isActive) return
        active = isActive
        if (isActive) requestFrame()
    }

    // ------------------------------------------------------------ bucle

    private fun requestFrame() {
        if (!isSupported || !active) return
        dirty = true
        if (!frameScheduled && !destroyed) {
            frameScheduled = true
            lastFrameNanos = 0L
            choreographer.postFrameCallback(frameCallback)
        }
    }

    private fun onFrame(frameTimeNanos: Long) {
        frameScheduled = false
        if (destroyed) return
        val dt = if (lastFrameNanos == 0L) 1f / 60f else ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceIn(0f, 0.1f)
        lastFrameNanos = frameTimeNanos

        if (!active) {
            lastFrameNanos = 0L
            return
        }
        var animating = updateCamera(dt)
        animating = updateParts(dt) || animating
        val wheelsMoving = updateWheels(dt)
        animating = animating || wheelsMoving
        // Hasta confirmar que el coche se ha dibujado, se sigue intentando aunque no se mueva nada.
        animating = animating || (carAsset != null && !shownReported)

        if (dirty || animating) {
            applyCamera()
            val sc = swapChain
            val ok = sc != null && uiHelper.isReadyToRender && renderer.beginFrame(sc, frameTimeNanos)
            if (ok) {
                renderer.render(view)
                renderer.endFrame()
                dirty = false
                if (carAsset != null && !shownReported && ++shownFrames >= 3) {
                    shownReported = true
                    onCarShown?.invoke()
                }
            } else if (!shownReported) {
                shownFrames = 0
            }
        }
        if (dirty || animating) {
            frameScheduled = true
            choreographer.postFrameCallback(frameCallback)
        } else {
            lastFrameNanos = 0L
        }
    }

    private fun updateCamera(dt: Float): Boolean {
        intro?.let { a ->
            a.t = (a.t + dt / INTRO_SEC).coerceAtMost(1f)
            val e = smoother(a.t)
            yaw = a.fromYaw + (homeYaw - a.fromYaw) * e
            pitch = a.fromPitch + (homePitch - a.fromPitch) * e
            dolly = a.fromDolly + (1f - a.fromDolly) * e
            if (a.t >= 1f) { intro = null; dolly = 1f }
            return true
        }
        var dollyMoving = false
        if (!introArmed && dolly != 1f) {
            dolly += (1f - dolly) * (1f - exp(-6f * dt))
            if (abs(dolly - 1f) < 0.002f) dolly = 1f
            dollyMoving = true
        }
        cameraAnim?.let { a ->
            a.t = (a.t + dt / a.durationSec).coerceAtMost(1f)
            val e = easeInOut(a.t)
            yaw = a.fromYaw + (a.toYaw - a.fromYaw) * e
            pitch = a.fromPitch + (a.toPitch - a.fromPitch) * e
            if (a.t >= 1f) cameraAnim = null
            return true
        }
        if (abs(yawVelocity) > 1f) {
            yaw += yawVelocity * dt
            yawVelocity *= exp(-3.5f * dt)
            return true
        }
        yawVelocity = 0f
        return dollyMoving
    }

    private fun updateParts(dt: Float): Boolean {
        var any = false
        val tm = engine.transformManager
        val rot = FloatArray(16)
        val out = FloatArray(16)
        parts.values.forEach { p ->
            if (p.amount == p.target) return@forEach
            val step = dt * 1000f / p.part.durationMs
            p.amount = if (p.target > p.amount) min(p.target, p.amount + step) else maxOf(p.target, p.amount - step)
            val angle = p.part.openAngleDeg * easeInOut(p.amount)
            val a = p.part.axis
            Matrix.setRotateM(rot, 0, angle, a[0], a[1], a[2])
            Matrix.multiplyMM(out, 0, p.rest, 0, rot, 0)
            tm.setTransform(p.instance, out)
            any = true
        }
        return any
    }

    private fun updateWheels(dt: Float): Boolean {
        if (wheels.isEmpty()) return false
        if (!wheelSpin.update(dt, speedMs, wheels.first().radius)) return false
        val tm = engine.transformManager
        val rot = FloatArray(16)
        val out = FloatArray(16)
        val wheelAngleDeg = wheelSpin.angleDeg
        wheels.forEach { w ->
            Matrix.setRotateM(rot, 0, wheelAngleDeg, w.axis[0], w.axis[1], w.axis[2])
            Matrix.multiplyMM(out, 0, w.rest, 0, rot, 0)
            tm.setTransform(w.instance, out)
        }
        return true
    }

    private fun applyCamera() {
        val y = Math.toRadians(yaw.toDouble())
        val p = Math.toRadians(pitch.toDouble())
        val d = distance().toDouble()
        val ex = target[0] + d * cos(p) * sin(y)
        val ey = target[1] + d * sin(p)
        val ez = target[2] + d * cos(p) * cos(y)
        // Vector "arriba" perpendicular a la mirada: válido también en la vista cenital.
        val ux = -sin(p) * sin(y)
        val uy = cos(p)
        val uz = -sin(p) * cos(y)
        camera.lookAt(ex, ey, ez, target[0].toDouble(), target[1].toDouble(), target[2].toDouble(), ux, uy, uz)
    }

    /** Distancia que encaja la esfera envolvente del coche en el campo de visión más estrecho. */
    private fun distance(): Float {
        val vHalf = Math.toRadians(fovDeg / 2.0)
        val hHalf = kotlin.math.atan(tan(vHalf) * aspect)
        val half = min(vHalf, hHalf)
        // En perspectiva el coche puede llenar más (la esfera envolvente es conservadora);
        // en planta el coche ocupa todo su largo en vertical y necesita el encuadre completo.
        val fill = if (mode == CarViewMode.TOP) 1.0f else 0.82f
        return (radius / sin(half)).toFloat() * zoom * fill * dolly
    }

    private fun updateProjection() {
        camera.setProjection(fovDeg.toDouble(), aspect.toDouble(), 0.05, 100.0, Camera.Fov.VERTICAL)
    }

    private class CameraAnim(
        val fromYaw: Float, val toYaw: Float,
        val fromPitch: Float, val toPitch: Float,
        val durationSec: Float, var t: Float = 0f,
    )

    private fun animateCameraTo(toYaw: Float, toPitch: Float, durationMs: Int) {
        // Gira por el camino más corto.
        var delta = (toYaw - yaw) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        cameraAnim = CameraAnim(yaw, yaw + delta, pitch, toPitch, durationMs / 1000f)
        requestFrame()
    }

    private fun easeInOut(t: Float) = t * t * (3f - 2f * t)

    /** Arranque y llegada muy suaves (smootherstep): movimiento de cámara de anuncio. */
    private fun smoother(t: Float) = t * t * t * (t * (t * 6f - 15f) + 10f)

    private class IntroAnim(val fromYaw: Float, val fromPitch: Float, val fromDolly: Float, var t: Float = 0f)

    // ------------------------------------------------------------ utilidades

    private fun readAsset(path: String): ByteBuffer =
        context.assets.open(path).use { it.readBytes() }.toDirectBuffer()

    private fun ByteArray.toDirectBuffer(): ByteBuffer =
        ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).put(this).also { it.flip() }

    // ---------------------------------------------------------------- color de la pintura

    private var paintInstances: List<com.google.android.filament.MaterialInstance> = emptyList()
    /** Color original de la pintura (lineal, RGBA) leído del GLB. */
    private var paintOriginal: FloatArray? = null
    /** Color elegido por el usuario (sRGB 0xRRGGBB) o null = el original del modelo. */
    private var paintColor: Int? = null

    /** true si el coche cargado tiene un material de pintura que se puede recolorear. */
    val canRecolor: Boolean get() = paintInstances.isNotEmpty()

    /** Cambia el color de la carrocería al instante. null vuelve al color original del modelo. */
    fun setPaintColor(srgb: Int?) {
        paintColor = srgb
        applyPaint()
    }

    private fun applyPaint() {
        val rgba = paintColor?.let { c ->
            fun lin(v: Int): Float { val s = v / 255f; return if (s <= 0.04045f) s / 12.92f else Math.pow(((s + 0.055f) / 1.055f).toDouble(), 2.4).toFloat() }
            floatArrayOf(lin((c shr 16) and 0xFF), lin((c shr 8) and 0xFF), lin(c and 0xFF), 1f)
        } ?: paintOriginal ?: return
        paintInstances.forEach { it.setParameter("baseColorFactor", rgba[0], rgba[1], rgba[2], rgba[3]) }
        requestFrame()
    }

    /** Busca las instancias del material de pintura y su color original (baseColorFactor del glTF). */
    private fun findPaint(asset: FilamentAsset, glb: ByteArray, materialName: String) {
        val rm = engine.renderableManager
        paintInstances = asset.renderableEntities.flatMap { e ->
            val ri = rm.getInstance(e)
            if (ri == 0) emptyList() else (0 until rm.getPrimitiveCount(ri)).map { rm.getMaterialInstanceAt(ri, it) }
        }.filter { it.name == materialName }.distinct()
        paintOriginal = runCatching {
            // GLB: cabecera de 12 bytes y primer bloque JSON (longitud en bytes 12..15).
            val len = java.nio.ByteBuffer.wrap(glb, 12, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
            val json = org.json.JSONObject(String(glb, 20, len, Charsets.UTF_8))
            val mats = json.getJSONArray("materials")
            (0 until mats.length()).map { mats.getJSONObject(it) }.first { it.optString("name") == materialName }
                .optJSONObject("pbrMetallicRoughness")?.optJSONArray("baseColorFactor")
                ?.let { a -> FloatArray(4) { i -> a.optDouble(i, 1.0).toFloat() } } ?: floatArrayOf(1f, 1f, 1f, 1f)
        }.getOrNull()
    }

    private fun destroyCar() {
        paintInstances = emptyList()
        paintOriginal = null
        carAsset?.let {
            scene.removeEntities(it.entities)
            assetLoader.destroyAsset(it)
        }
        carAsset = null
        parts.clear()
        wheels.clear()
    }

    /** Libera todos los recursos nativos. El objeto no puede reutilizarse. */
    fun destroy() {
        if (destroyed) return
        destroyed = true
        choreographer.removeFrameCallback(frameCallback)
        uiHelper.detach()
        destroyCar()
        stageAsset?.let { scene.removeEntities(it.entities); assetLoader.destroyAsset(it) }
        stageAsset = null
        resourceLoader.destroy()
        assetLoader.destroy()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        indirectLight?.let { engine.destroyIndirectLight(it) }
        iblTexture?.let { engine.destroyTexture(it) }
        engine.destroyEntity(sun)
        EntityManager.get().destroy(sun)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroy()
    }
}
