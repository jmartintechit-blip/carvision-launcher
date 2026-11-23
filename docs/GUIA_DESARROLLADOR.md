# Guía para desarrolladores

Para quien tiene el código fuente de CarVision y quiere compilarlo, adaptarlo a su radio o a su coche, o publicarlo.

## 1. Requisitos

| Herramienta | Versión |
|---|---|
| Android Studio | Koala o posterior (o solo línea de comandos) |
| JDK | 17 (el que trae Android Studio vale) |
| Android SDK | Plataforma 34 |
| Gradle | 8.7, incluido (`gradlew`); no hace falta instalarlo |
| Blender (opcional) | 4.x/5.x, solo para preparar coches nuevos |
| Python 3 (opcional) | Solo para los scripts de `scripts/` y `blender/scripts/` |

Versiones fijadas en `android/gradle/libs.versions.toml`: Kotlin 2.0.21, AGP 8.5.2, Compose BOM 2024.10.01 y
**Filament 1.75.1** (no subir sin probar: 1.76+ exige compileSdk 37 y Kotlin 2.4, ver `DECISIONS.md`).

## 2. Compilar

Abre la carpeta `android/` en Android Studio, o desde la terminal:

```bash
cd android
./gradlew test             # tests unitarios
./gradlew assembleDebug    # APK de depuración (una por arquitectura)
./gradlew assembleRelease  # APK final optimizada (R8), una por arquitectura
```

Las APK quedan en `android/app/build/outputs/apk/<debug|release>/app-<arquitectura>-<tipo>.apk`.
Los packs de `vehicle-packs/` se empaquetan solos al compilar (solo `manifest.json`, `.glb` e imágenes; nunca
`source/`).

## 3. Firmar la versión final

Sin configuración, `assembleRelease` firma con la clave de **depuración**: vale para probar, no para distribuir.
Para firmar con tu clave:

1. Crea la clave una vez (guárdala bien: sin ella no podrás publicar actualizaciones):
   ```bash
   keytool -genkeypair -v -keystore carvision-release.jks -alias carvision -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Crea `android/keystore.properties` (está en `.gitignore`; **no lo subas nunca a un repositorio**):
   ```properties
   storeFile=../carvision-release.jks
   storePassword=…
   keyAlias=carvision
   keyPassword=…
   ```
   `storeFile` es relativo a la carpeta `android/`.
3. `./gradlew assembleRelease`.

Cambia `applicationId` en `android/app/build.gradle.kts` si vas a publicar tu propia versión, para no chocar con
otras instalaciones de CarVision.

## 4. Estructura

```
android/
  app/                 Launcher: pantallas (Compose), navegación, ajustes, avisos, multimedia, datos guardados
  core/model/          Señales normalizadas del vehículo (Kotlin puro)
  core/designsystem/   Tema, colores, ambientes, tipografía, iconos y componentes
  core/maintenance/    Carnet de mantenimiento: modelo y cálculos (Kotlin puro)
  vehicle/data/        Fuentes de datos del vehículo (sin conexión, demostración) y ordenador de a bordo
  vehicle/packs/       Formato de Vehicle Pack: validador, cargador e importador de .zip
  render3d/            Motor 3D (Filament), cámara, animaciones y vista 2D de respaldo
vehicle-packs/         Coches incluidos (ver docs/VEHICLE_PACKS.md)
blender/scripts/       Preparación y validación de modelos (ver docs/BLENDER_PIPELINE.md)
scripts/               Empaquetar packs .zip, convertir iconos
```

Las dependencias entre módulos se crean a mano en `app/.../data/AppContainer.kt` (sin framework de inyección).

## 5. Arquitectura: tres capas independientes

| Capa | Qué es | Estado |
|---|---|---|
| **Vehicle Packs** | Cómo se ve el coche: modelo 3D, piezas que se abren, logotipo | ✅ Implementada (`vehicle/packs`) |
| **CAN Adapters** | De dónde salen los datos del coche: `VehicleDataProvider` | 🟡 Interfaz, «sin conexión» y demostración. Ningún adaptador real todavía |
| **Device Adapters** | Particularidades de cada radio (botones físicos, encendido, audio) | ⏳ Prevista, aún sin código |

El launcher funciona sin ningún adaptador: los datos salen como «No disponible».

### 5.1 Añadir una fuente de datos (CAN Adapter)

Implementa `VehicleDataProvider` (`vehicle/data/.../VehicleDataProvider.kt`) y emite `VehicleSnapshot`:

```kotlin
class MiRadioCanProvider(/* … */) : VehicleDataProvider {
    override val id = "mi_radio_can"
    override val displayName = "CAN de Mi Radio"
    private val state = MutableStateFlow(VehicleSnapshot.unavailable())
    override val snapshot: StateFlow<VehicleSnapshot> = state.asStateFlow()

    override fun start(scope: CoroutineScope) { /* leer la caja CAN sin bloquear; publicar en state */ }
    override fun stop() { /* cerrar la conexión */ }
}
```

Reglas obligatorias (son las que hacen fiable el producto):

- **Solo lectura.** Nunca se envían comandos ni escrituras al coche.
- Cada valor real es `VehicleSignal(value, unit, timestampMs, SignalStatus.VALID, SignalSource.CAN)`.
  Lo que no se lee se deja `VehicleSignal.unavailable()`. **Nunca** se marca como `VALID` un dato inventado o estimado.
- `start` no bloquea: si la conexión tarda o falla, se sigue emitiendo «no disponible» y `ConnectionState` lo explica.
- Las aperturas usan las claves de `Opening` (`door.front_left`, `tailgate`, `hood`…), las mismas de los packs.
- No hay datos de GPS a propósito: velocidad y kilometraje salen siempre del coche.

Después, añádelo a la elección de `activeProvider` en `AppContainer.kt` y a Ajustes › Vehículo › Datos del coche.

### 5.2 Añadir un coche

Ver `docs/VEHICLE_PACKS.md` (formato) y `docs/BLENDER_PIPELINE.md` (preparación del modelo). Un pack nuevo en
`vehicle-packs/` aparece solo en el Garaje al recompilar; o se importa como .zip sin recompilar.

### 5.3 Diseño

Toda pantalla nueva usa solo los componentes de `core/designsystem` (`GlassCard`, `CvButton`, `Segment`, `CvText`,
`CvIcons`…), para mantener el aspecto OEM+ y que los temas claro/oscuro y los ambientes funcionen solos.
La interfaz está en español (`UiLocale` fija el formato de las cifras).

## 6. Pruebas

- `./gradlew test`: tests unitarios de señales, demostración, packs, carnet, ordenador de a bordo, historial y giro de
  ruedas.
- Emulador recomendado: tableta 1024×600 o 1280×720, Android 8.1 (API 27), lanzado con OpenGL ES 3 para ver el 3D:
  ```bash
  emulator -avd <nombre> -gpu host -feature GLESDynamicVersion
  ```
  Sin `GLESDynamicVersion`, Android 8.1 arranca con OpenGL ES 2 y la app muestra la vista 2D de respaldo.
- La compilación de depuración incluye un receptor para simular música (`src/debug/`), que no existe en la versión final.

## 7. Licencias de terceros

| Componente | Licencia | Dónde |
|---|---|---|
| Filament (motor 3D) e iluminación de su ejemplo `default_env` | Apache 2.0 | dependencias Gradle; `render3d/src/main/assets/envs/studio_ibl.ktx` |
| Manrope (tipografía) | SIL OFL 1.1 | `docs/third-party/Manrope-OFL.txt` |
| Lucide (iconos convertidos) | ISC | `docs/third-party/Lucide-LICENSE.txt` |
| Complemento de Blender «CarVision Pack Builder» (código propio) | GPL-3.0-or-later (D-025) | `blender/carvision_pack_builder/LICENSE.txt` |
| Ford Focus ST (MK3), modelo 3D | CC BY 4.0, savelliy07 | atribución en su `manifest.json` y en Ajustes › Información |
| Logotipos de marcas | Marcas registradas de sus dueños | ver `docs/VEHICLE_PACKS.md` §5 |

Antes de distribuir, revisa `KNOWN_LIMITATIONS.md` (lo que todavía no está probado) y `DECISIONS.md` (por qué el
proyecto es como es).
