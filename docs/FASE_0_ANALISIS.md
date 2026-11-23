# CarVision Launcher — Fase 0: análisis y requisitos

Fecha: 2026-11-11 · Estado: **pendiente de aprobación**

Leyenda de estados usada en todo el proyecto:

| Etiqueta | Significado |
|---|---|
| ✅ Probado | Implementado y verificado con evidencia |
| 🟡 Implementado | Código escrito, sin verificar |
| 🔶 Pendiente hardware | Solo verificable en una radio real |
| 🧪 Simulado | Funciona con datos de demostración |
| ⛔ No compatible | Identificado como no viable |
| 🔌 Depende de hardware externo | Caja CAN, adaptador OBD, etc. |
| ❓ Por verificar | Suposición sin confirmar |

---

## 1. Comprensión del producto

CarVision Launcher es un **launcher Android universal** para radios aftermarket. Se vende como proyecto fuente (no APK). Se compone de tres capas desacopladas:

1. **Device Adapters** — particularidades de cada radio (opcionales; el launcher funciona sin ninguno).
2. **Vehicle Packs** — solo representación visual (modelo 3D, nodos, cámara, materiales). El Ford Focus MK3 5p es el primer pack, no una limitación.
3. **CAN Adapters** — acceso de solo lectura a datos del vehículo, normalizados. Su ausencia nunca bloquea la app.

Prioridad inmediata: primera versión visualmente fuerte (interfaz + Focus 3D + temas) sobre una arquitectura limpia. Web, Stripe y marketing quedan para después.

---

## 2. Estado actual del entorno (comprobado el 2026-11-11)

| Elemento | Resultado |
|---|---|
| Carpeta del proyecto | Vacía. No hay código, capturas ni modelos previos. No es un repositorio git. |
| Capturas de referencia | **No recibidas todavía.** |
| JDK | Temurin 17.0.20.1 (vía `JAVA_HOME`, carpeta compartida con otro proyecto) |
| Android SDK | `platforms/android-34`, `build-tools/34.0.0`, `cmdline-tools/latest`, emulador |
| Imagen de emulador | Android 8.1 (API 27) x86 |
| AVD existente | `mazda` (1024×600, 160 dpi, 2 GB RAM, **GPU desactivada**). Pertenece a otro proyecto: no se tocará; se creará uno propio. |
| Android Studio | No encontrado en la ruta estándar (no imprescindible: se puede compilar con Gradle Wrapper) |
| Blender | 5.2 instalado (`C:\Program Files\Blender Foundation\Blender 5.2`) |
| adb, git, Node, Python | Disponibles |

Consecuencia: se puede compilar, ejecutar en emulador 1024×600 y automatizar Blender desde línea de comandos sin instalar nada más (salvo las dependencias Gradle, que se descargan en la primera compilación).

---

## 3. Análisis de las capturas

**No puedo realizarlo todavía: las tres capturas no se han adjuntado.** No describo ningún elemento visual ni comportamiento que no haya observado.

Cuando las envíes, analizaré: distribución, tamaños relativos, tipografías, colores, jerarquía, reloj, barra lateral, widgets, posición/tamaño del coche, controles multimedia, interacción de puertas, menú de mantenimiento y transiciones (estas últimas solo si hay vídeo; con capturas estáticas no se pueden deducir).

Material útil adicional (opcional):
- Un vídeo corto de la interfaz de referencia abriendo una puerta → para entender la animación y la transición de cámara.
- Captura del estado "todas las puertas cerradas" en vista superior, si existe.
- Resolución exacta de las capturas (para saber si son de 1024×600, 1280×720 u otra).

---

## 4. Vehículo: Ford Focus MK3 hatchback 5 puertas

### 4.1 Variante visual — decisión necesaria

El MK3 tiene dos frontales distintos que no deben mezclarse:

| Variante | Periodo aproximado (Europa) | Por verificar con fotos |
|---|---|---|
| Pre-restyling | 2011 – 2014 | Parrilla, faros, pilotos traseros, paragolpes |
| Restyling (facelift) | 2014/2015 – 2018 | Parrilla, faros, pilotos traseros, paragolpes |

No detallo diferencias de carrocería de memoria: se verificarán con fotografías antes de modelar o elegir modelo.

Fotos que necesito de **tu** coche (o de la variante elegida): frontal, trasera, lateral, ¾ delantera, ¾ trasera, una puerta delantera abierta, una trasera abierta y el portón abierto (para colocar bisagras y ángulos máximos). Además: color exacto (nombre comercial si lo sabes), modelo de llanta y acabado (Trend, Titanium…).

### 4.2 Origen del modelo 3D — decisión crítica (licencia y marca)

El producto entrega el proyecto fuente, así que **el `.glb` y el `.blend` llegan íntegros al comprador**. Eso es redistribución del asset, y condiciona el origen del modelo:

| Opción | Calidad | Velocidad | Riesgo de licencia | Comentario |
|---|---|---|---|---|
| A. Modelo comprado en tienda (CGTrader, TurboSquid, Hum3D…) | Alta | Rápida | **Alto** | Las licencias estándar suelen prohibir redistribuir el archivo del modelo. Habría que leer la licencia concreta o negociar una ampliada. Sirve para prototipo interno, no para vender sin revisar la licencia. |
| B. Modelo encargado a un modelador 3D con cesión de derechos | Alta | Media (días/semanas, coste) | Bajo (por contrato) | Opción más limpia para un producto comercial. |
| C. Modelo hecho por nosotros en Blender (scripts + modelado manual) | Media | Media | Bajo | Puedo generar por script la estructura, jerarquía y pivotes, pero **no puedo garantizar un Focus reconocible de alta calidad solo por script**. Necesita trabajo de modelado manual con referencias. |
| D. Modelo con licencia CC-BY / CC0 | Variable | Rápida | Medio | Muchos modelos de coches gratuitos son extracciones de videojuegos con procedencia dudosa. Solo si la procedencia es verificable. |

**Marca:** usar el nombre "Ford Focus" y una réplica reconocible del diseño en un producto comercial puede requerir autorización del fabricante (marca y diseño). Recomiendo: no incluir logotipos de Ford en el modelo ni en la web, describirlo como "modelo de demostración inspirado en un hatchback compacto de 5 puertas (Ford Focus MK3)" y que un profesional lo revise antes de lanzar. Esto queda en `KNOWN_LIMITATIONS.md`.

**Recomendación:** prototipo visual rápido con un modelo de la opción A o D usado solo internamente (sin publicar el pack), mientras se resuelve B o C para el producto final. Como la arquitectura carga el coche desde un pack, sustituir el modelo no toca código.

### 4.3 Requisitos técnicos del modelo (independientes de su origen)

- Jerarquía mínima: `car_root`, `body`, `front_left_door`, `front_right_door`, `rear_left_door`, `rear_right_door`, `rear_hatch`, `front_left_wheel`, `front_right_wheel`, `rear_left_wheel`, `rear_right_wheel`.
- Origen de cada puerta en su eje de bisagra; portón con eje en la bisagra del techo.
- Presupuesto orientativo (a validar en la prueba de viabilidad): 60k–150k triángulos, texturas de 1K–2K, pocos materiales (pintura, cristal, cromo/plástico, faros, neumático, llanta).
- Cristales con material transparente simple (sin refracción costosa).
- Sombra de contacto horneada bajo el coche (textura) en lugar de sombras dinámicas.

---

## 5. Viabilidad técnica

### 5.1 Hardware objetivo

Las radios Android aftermarket varían mucho (SoC, GPU, Android 8–13+, 1–8 GB RAM). Implicaciones:

- **3D:** Filament requiere OpenGL ES 3.0 o Vulkan. Algunos SoC económicos solo ofrecen ES 2.0 ❓. Hace falta un **modo de respaldo sin 3D**: vistas pre-renderizadas desde Blender (giro por fotogramas) + esquema 2D cenital para el estado de puertas. Así la app es universal aunque la GPU no lo sea.
- **Glassmorphism:** el desenfoque real en Compose (`Modifier.blur` / `RenderEffect`) solo funciona en Android 12+. En versiones anteriores se usarán capas translúcidas con degradado, sin desenfoque. Mismo diseño, menos coste.
- **Rendimiento:** limitar el render 3D a 30 fps en hardware modesto y renderizar solo cuando hay cambios (giro, animación); en reposo, sin consumo de GPU.

### 5.2 Renderizado 3D — propuesta

| Opción | Pros | Contras |
|---|---|---|
| **Filament + gltfio (propuesta)** | Ligero, PBR de calidad, GLB nativo, manipulación de nodos por nombre, animaciones glTF, mantenido por Google | API de bajo nivel; requiere ES 3.0 |
| SceneView (envoltorio de Filament para Compose) | Integración Compose cómoda | Capa extra con cambios de API frecuentes; menos control |
| three.js en WebView | Muy flexible | Más RAM, arranque lento, depende del WebView de la radio |
| Unity / Godot | Potentes | Excesivos en tamaño y RAM para un launcher |

Antes de comprometernos: **prueba de viabilidad** (spike) con Filament que verifique carga GLB, cámara orbital con límites, rotación de un nodo-puerta por nombre, consumo y fps en el emulador 1024×600 y, cuando sea posible, en tu radio.

### 5.3 Funciones sin CAN — qué es posible de verdad

| Función | Fuente | Estado previsto | Limitación |
|---|---|---|---|
| Reloj, fecha | Sistema | Viable | — |
| Temas claro/oscuro/auto | App | Viable | "Auto" por hora o por el modo noche del sistema; el sensor de luces del coche solo con CAN |
| Lanzar apps / cajón de apps | `PackageManager` con `<queries>` | Viable | Sin `QUERY_ALL_PACKAGES` |
| Ser launcher (HOME) | Intent `HOME` + `RoleManager` (Android 10+) | Viable ❓ | Algunos firmwares relanzan su launcher al arrancar: 🔶 a verificar en cada radio |
| Música actual y controles | `MediaSessionManager` + acceso a notificaciones | Viable con permiso | El usuario debe conceder "acceso a notificaciones"; algunas radios ocultan esa pantalla. Sin él: solo botón "abrir música" |
| Velocidad | ~~GPS~~ → solo datos del coche (D-016) | Descartado el GPS | Muchas radios no tienen GPS instalado |
| RPM, combustible, temp. exterior, voltaje, km, puertas | CAN / OBD | 🔌 | Sin adaptador: "No disponible" |
| Mantenimiento, viajes manuales | Room local | Viable | — |

### 5.4 CAN — enfoque y datos necesarios

Arquitectura (sin depender de ninguna marca):

```
VehicleDataProvider (interfaz)
 ├─ UnavailableVehicleDataProvider   → por defecto: todo "No disponible"
 ├─ DemoVehicleDataProvider          → datos simulados, siempre etiquetados 🧪
 ├─ (GPS descartado: D-016)
 ├─ [futuro] ObdVehicleDataProvider  → adaptador ELM327: PIDs estándar, sin puertas
 └─ [futuro] adaptadores de fabricante (solo con documentación o captura verificable)
```

Cada señal: `valor`, `unidad`, `marca temporal`, `estado` (VÁLIDO / NO_DISPONIBLE / ANTIGUO / SIMULADO), `origen`.

OBD-II estándar no ofrece el estado individual de puertas; aporta, según la ECU, velocidad, RPM, temperatura de refrigerante, nivel de combustible (no todas lo soportan) y voltaje del módulo de control.

**Información que necesito de tu radio** (no bloquea el prototipo visual):

1. Ajustes → Información: modelo, versión de Android, versión de MCU, número de compilación, CPU/SoC, RAM, resolución.
2. Ajustes de fábrica → CANBUS: protocolo/fabricante seleccionado y perfil de vehículo (foto de la pantalla).
3. Foto de la etiqueta de la caja CAN (marca, referencia, versión) y del cableado si es visible.
4. ¿La interfaz original de la radio muestra puertas abiertas, temperatura exterior o datos de viaje? (captura o foto).
5. Con depuración USB/ADB activada, la salida de:
   ```
   adb shell getprop
   adb shell pm list packages -f
   adb shell dumpsys package | findstr /i "canbus can_ mcu"
   adb shell dumpsys activity services
   ```
   Con esto podemos identificar qué servicio del fabricante distribuye los datos. **No asumiré ningún protocolo** hasta tener evidencia.

---

## 6. Arquitectura inicial propuesta (resumen; se detalla en Fase 1)

**Tecnologías:** Kotlin · Jetpack Compose · Gradle Kotlin DSL + version catalog · Coroutines/Flow · Room · DataStore · Filament (tras el spike) · inyección de dependencias manual (contenedor sencillo, más fácil de adaptar por compradores que Hilt).

**Repositorio:**

```
CarVision/
├─ android/                 Proyecto Android Studio
│  ├─ app/                  Shell del launcher, navegación, pantallas
│  ├─ core/model/           Señales, entidades de dominio (Kotlin puro)
│  ├─ core/designsystem/    Tema, colores, tipografía, componentes glass
│  ├─ vehicle/data/         VehicleDataProvider + Demo/Unavailable/GPS
│  ├─ vehicle/packs/        Manifiesto, validación, carga e importación de packs
│  └─ render3d/             Envoltorio Filament + respaldo 2D/sprites
├─ vehicle-packs/ford_focus_mk3_5p/   manifest.json, model.glb, previews/
├─ assets3d/                Archivos .blend fuente
├─ blender/                 Scripts de validación/exportación
├─ docs/                    Documentación
└─ (más adelante) web/, backend/, marketing/
```

Módulos justificados: separar `core/model` y `vehicle/data` permite probar la lógica sin Android; `render3d` aísla la dependencia nativa pesada; `vehicle/packs` evita que el código central conozca el Focus. Las pantallas (inicio, mantenimiento, ajustes…) empiezan dentro de `app` y se separan solo si crecen.

**Pack desde el primer día:** el Focus se cargará leyendo su `manifest.json` (nombres de nodos, ejes y ángulos de puertas, cámara) mediante el mismo cargador que usarán los packs importados. Ningún nombre de nodo del Focus estará escrito en el código.

**minSdk propuesto:** 26 (Android 8.0). Cubre la mayoría de radios actuales y coincide con el emulador disponible (8.1). Bajarlo a 24 es posible con coste moderado.

---

## 7. Plan para el primer prototipo visual

Orden pensado para tener algo espectacular cuanto antes sin hipotecar la arquitectura:

1. **Spike 3D (≈ primera sesión):** proyecto Android mínimo + Filament + un GLB de prueba con jerarquía correcta generado por script en Blender (coche esquemático, solo para validar pivotes y nombres). Medir arranque, fps y RAM en el emulador 1024×600 con GPU del host.
2. **Esqueleto del launcher:** módulos, tema claro/oscuro/auto con DataStore, intent HOME, barra lateral, reloj, zona multimedia y widgets en estado "No disponible" / "Demo".
3. **Diseño visual definitivo** tras analizar tus capturas: propuesta de layout adaptable (1024×600 y 1280×720 como referencias, sin posiciones fijas en píxeles).
4. **Integración del Focus** en cuanto esté decidido el origen del modelo: limpieza en Blender, jerarquía, pivotes, materiales, exportación GLB y carga mediante el pack.
5. **Capturas y vídeo** del emulador en ambas resoluciones como evidencia.

Las animaciones de puertas se implementan en Fase 3, aunque el spike ya comprobará que un nodo puede girar sobre su bisagra.

---

## 8. Decisiones que necesitan tu aprobación

1. Enviar las **3 capturas** de referencia (bloqueante para el diseño definitivo, no para el spike).
2. **Variante del Focus** (pre-restyling o restyling) + fotos de referencia.
3. **Origen del modelo 3D** (A/B/C/D, sección 4.2).
4. Aprobar **Filament** como candidato para el spike (sección 5.2).
5. Aprobar **minSdk 26**, Compose, DI manual y la estructura de módulos (sección 6).
6. Nombre de paquete propuesto: `com.carvision.launcher` (cambiable; afecta a la firma del APK).

## 9. Suposiciones documentadas (no bloquean)

- La pantalla objetivo es horizontal; el modo vertical se tratará como compatible básico.
- El idioma inicial de la interfaz es español de España; se prepara para traducciones (`strings.xml`).
- No se usarán permisos de sistema ni root. Permiso opcional: acceso a notificaciones (música). Sin permiso de ubicación (D-016), ambos solicitados al usuario con explicación.
- Se creará un AVD propio `carvision_1024x600` (y otro 1280×720) con GPU del host, sin modificar el AVD `mazda`.
