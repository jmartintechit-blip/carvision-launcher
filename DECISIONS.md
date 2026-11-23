# Decisiones técnicas

Formato: ID · fecha · estado (Propuesta / Aprobada / Rechazada / Sustituida) · decisión · motivo · consecuencias.

## D-001 · 2026-11-11 · Aprobada (por el cliente)
**Prioridad visual primero.** Primera versión centrada en interfaz + Focus MK3 3D + temas. Web, Stripe y marketing se aplazan.

## D-002 · 2026-11-11 · Aprobada (por el cliente)
**Arquitectura universal en tres capas:** Device Adapters, Vehicle Packs y CAN Adapters, desacopladas. El launcher funciona sin ningún adaptador.

## D-003 · 2026-11-11 · Aprobada
**Stack Android:** Kotlin, Jetpack Compose, Gradle Kotlin DSL con version catalog, Coroutines/Flow, Room, DataStore.
Motivo: estándar actual de Android, bien documentado y fácil de adaptar por compradores con ayuda de IA.

## D-004 · 2026-11-11 · Aprobada y validada en prueba de viabilidad
**Renderizado 3D con Filament 1.75.1 + gltfio.** Ligero, PBR, GLB nativo, control de nodos por nombre.
- Versión 1.75.1: la 1.76+ exige compileSdk 37 y Kotlin 2.4; no compensa subir toda la cadena de herramientas ahora.
- Renderizador propio (`CarRenderer`), sin `ModelViewer`: control total de cámara, límites y consumo
  (solo dibuja cuando algo cambia).
- Requiere OpenGL ES 3.0. Sin él, la app muestra una vista en planta 2D (`CarSchematic`). Probado en emulador ES 2.0.

## D-005 · 2026-11-11 · Aprobada
**minSdk 26 (Android 8.0), targetSdk 34, compileSdk 34.** AGP 8.5.2, Gradle 8.7, Kotlin 2.0.21, Compose BOM 2024.10.01.

## D-006 · 2026-11-11 · Aprobada
**Inyección de dependencias manual** (contenedor de aplicación) en lugar de Hilt.
Motivo: menos magia de compilación, más fácil de entender y adaptar por compradores.
Consecuencia: si el proyecto crece mucho, se reevaluará.

## D-007 · 2026-11-11 · Aprobada e implementada
**Todo coche se carga por el mismo mecanismo** (`manifest.json` + `model.glb`, validado por `VehiclePackParser`).
Ningún nombre de nodo específico en el código. El manifiesto incluye la licencia del modelo, que se muestra en Ajustes.

## D-008 · 2026-11-11 · Aprobada (cliente)
**Modelo 3D gratuito con licencia que permita uso comercial y redistribución** (CC-BY o CC0), verificando procedencia.
Elegido e integrado: "Ford Focus ST (MK3)" de savelliy07 (Sketchfab, CC-BY 4.0), MK3 prerestyling 5 puertas.
Preparado con `blender/scripts/prepare_focus_mk3.py` (logotipos eliminados, 765 piezas → 11 nodos, materiales propios).
Color por defecto: naranja (recoloreable con un parámetro del script). La atribución se muestra en Ajustes.
Se mantiene el coche de prueba (`demo_hatchback`) como segundo pack: demuestra que el sistema admite varios coches.
*(2026-11-12, decisión del cliente: sale de la app; queda en `blender/samples/` como muestra de desarrollo. El ejemplo de
varios coches lo da ahora el Ford Focus instalado como coche de prueba importado, ver D-024.)*

## D-009 · 2026-11-11 · Aprobada (cliente)
**CarVision es un producto propio; la competencia (Reality Design · Car Launcher) solo se estudia.**
Regla de sala limpia: no se copia código, imágenes, modelos ni textos. Ver `docs/ANALISIS_REFERENCIAS.md`.
Diferenciación principal: 3D en tiempo real con un solo GLB por coche frente a cientos de imágenes pre-renderizadas.

## D-010 · 2026-11-11 · Aprobada
**Interfaz diseñada a 600 dp de alto y escalada a la pantalla real** (densidad efectiva = altura / 600), más un
ajuste de tamaño del usuario. Así 1024×600 y 1280×720 se ven con las mismas proporciones, sin posiciones fijas en píxeles.

## D-011 · 2026-11-11 · Aprobada
**Identidad visual propia**: grafito + acento cian "aurora", fuente Manrope (OFL), fondo generado sin fotografías,
plataforma con anillo de luz. Sin elementos de CarPlay.

## D-013 · 2026-11-11 · Aprobada (cliente)
**Los emblemas de la marca se conservan por defecto** en los packs: cada usuario prepara su propio coche y los quiere.
El script admite `--no-logos` para quien prefiera quitarlos (p. ej. material promocional).

## D-014 · 2026-11-11 · Aprobada
**Modelos comprimidos con Draco** (`KHR_draco_mesh_compression`): Filament lo carga. Focus: 7,8 → 1,76 MB.

## D-015 · 2026-11-11 · Aprobada (cliente)
**Animación de arranque con el logotipo de la marca del pack** (`brand.logo` en el manifiesto). Se muestra una vez por
arranque del proceso del launcher (al encender la radio). Cuando exista la señal de contacto por CAN, también al
dar el contacto.

## D-016 · 2026-11-11 · Aprobada (cliente)
**Todos los datos del vehículo salen del propio coche (CAN o, en el futuro, OBD), nunca del GPS.**
Muchas radios no tienen antena GPS instalada. Velocidad, kilometraje, distancia de viaje y velocidad media se
obtienen de la caja CAN; si la caja no da el kilometraje, la distancia del viaje se calcula integrando la velocidad
del CAN. La app no pide permiso de ubicación. Sin CAN Adapter, estos datos se muestran como "No disponible".
Sustituye a la propuesta de `GpsVehicleDataProvider` de la Fase 0.

## D-017 · 2026-11-11 · Aprobada (cliente)
**Diseño OEM+ (referencia: CarPlay; ambiente Mercedes, BMW, Tesla). Nada debe delatar Android ni una radio china.**
- Pantalla completa sin barras del sistema.
- Iconos propios de línea fina (Lucide, ISC), sin Material Icons.
- Sin Toast, sin efecto de onda, sin diálogos del sistema cuando se puedan evitar.
- Paleta sobria con un único acento; cifras finas; coche sin marco sobre suelo de luz.
- Iconos de apps de terceros normalizados a squircle.
Toda pantalla nueva debe usar solo los componentes de `core/designsystem`.
- Columna lateral (0.4.1): iconos propios con texto debajo (como Audi/Porsche), halo en el activo, sin cajas.
  Referencia: los propietarios de Porsche criticaron los iconos de colores del PCM 2022 y prefieren el monocromo.
- Suelo (0.4.1): sin plataforma ni anillo; foco de luz difuminado y sombra de contacto horneada por coche.

## D-018 · 2026-11-11 · Aprobada (cliente)
**Ambientes de fondo propios** (degradado + dos luces de color + acento), dibujados con degradados en Compose:
cero coste de GPU y sin imágenes. Inspirados en la idea de la competencia, pero con diseños propios.

## D-019 · 2026-11-11 · Aprobada (cliente)
**Importación de packs .zip sin recompilar**, adelantada de la Fase 7. Los packs importados viven en la memoria privada
de la app (`files/packs/<id>`). Solo se admiten archivos de datos; nunca código. Para radios sin selector de archivos,
carpeta de importación en `Android/data/<app>/files/import`.

## D-020 · 2026-11-12 · Aprobada
**El carnet de mantenimiento se guarda en un archivo JSON propio (escritura atómica + copia de seguridad), no en Room.**
Motivo: son decenas de registros; JSON evita el procesador de anotaciones (KSP) y su ajuste de versiones, es legible
y fácil de exportar o migrar. Si el volumen crece (p. ej. historial de viajes), se reevaluará Room para esos datos.
El kilometraje para los cálculos solo sale de datos reales del coche o del usuario; nunca del modo demostración.

## D-022 · 2026-11-12 · Decisión del cliente
**Cada pack de coche incluye siempre el logotipo de su marca** (Ford con Ford, Opel con Opel, Seat con Seat…), y al
importarlo se integra solo en la barra lateral y en la animación de arranque. Aplica a los packs del catálogo y a los
que cree cada usuario. (El formato ya lo admite: `brand.logo` en `manifest.json`.)

## D-027 · 2026-11-12 · Decisión del cliente (riesgo asumido por el cliente)
**Modelos del catálogo: se eligen por calidad, aunque no los haya creado quien los sube.** El cliente indica que no
importa si un modelo de Sketchfab viene de un juego o de otro autor. Riesgo avisado: esos modelos pertenecen a sus
creadores reales (estudios de juegos, modeladores), así que venderlos en el catálogo puede acabar en reclamaciones o en
retirada de contenido; la licencia CC BY que muestra Sketchfab no la puede dar quien no es el autor. Se mantiene citar
al autor que figura en Sketchfab y la licencia mostrada. Sustituye para el catálogo la regla de «modelo hecho por quien
lo sube» de D-026; el prompt del cliente conserva la comprobación porque protege al cliente. Preselección en
`docs/CATALOGO_COCHES.md`. Cada descarga sigue necesitando la aprobación del cliente.

## D-031 · 2026-11-13 · Decisión del cliente
**La web vuelve a ofrecer «Hazlo tú con una IA de pago» como opción avanzada, y la compra de 2 € incluye el código
fuente.** Matiza D-030: el catálogo y «Te lo hacemos» siguen siendo el camino recomendado; la opción avanzada (IA con
acceso a archivos, p. ej. Claude Code, + el generador del código fuente) se presenta con avisos claros (resultado no
garantizado, depende del modelo 3D). El código fuente se entrega sin material interno: ni el análisis de la
competencia, ni las notas internas de negocio, ni datos de las radios del cliente.

## D-030 · 2026-11-13 · Decisión del cliente
**Catálogo pequeño y hecho a mano; el generador es solo herramienta interna; lanzar ya la versión 1.**
- Motivo: la prueba con 5 modelos de Sketchfab sacó fallos distintos en cada uno (Corsa con la carrocería «arrugada»
  de su textura original, luneta del Golf en rojo, puertas soldadas en Ibiza y Clio, León sin neumáticos). Un generador
  automático no deja perfectos coches tan distintos, y un coche con fallos en el catálogo resta más que suma.
- **Catálogo:** 7–8 coches muy comunes en estas radios, cada uno terminado a mano como el Focus (sin un solo fallo,
  logotipo de la marca en el coche, en la animación de arranque y en el inicio). **La versión 1 sale con un solo coche
  en el catálogo: el Ford Focus MK3**, repasado otra vez. Se acabaron los modelos automáticos para el catálogo.
- **Generador:** se queda para nosotros (hace la parte mecánica y luego se termina a mano) y se sigue mejorando para ir
  más rápido. **Se quita de cara al público**: ni el Área de cliente ofrece `carvision_pack_builder.zip`, ni el prompt
  guía a hacerlo en Blender. El cliente elige: coche del catálogo, coche por defecto (con su marca, nombre y color) o
  «Te lo hacemos». Los pedidos «Te lo hacemos» se hacen también a mano y pasan después al catálogo.
- **Lanzamiento:** sacar ya la v1 (web, perfil, Área de cliente, pago) y ampliar después con actualizaciones, en vez
  de pulir sin fecha. Las 33 descargas de modelos de D-027 se cancelan. Sustituye la parte C de D-026.
- **Ampliación del cliente (mismo día):** el catálogo de la v1 serán **Ford Focus MK3, VW Golf VI y Opel Corsa D**,
  los tres terminados a mano. «A mano» = con todas las herramientas (generador como base, script de acabado propio de
  cada coche, revisión en el visor y en la app), no un proceso automático para cualquier modelo. Perfil: solo TikTok.
  Dominio en Arsys; Stripe conectado.
- **Cierre del catálogo v1 (cliente, 2026-11-13): sale con Ford Focus MK3 y VW Golf VI.** El Corsa D se queda para una
  actualización, cuando haya un modelo base sin abolladuras.

## D-029 · 2026-11-13 · Decisión del cliente
**Precios: app 2 € (igual que la competencia tras su subida) y «Te lo hacemos» (coche a medida) 2 €.** El cliente tiene
el dominio carvisiones.es en uno de los conectores de alojamiento; Stripe lo dará de alta él más adelante.

## D-028 · 2026-11-12 · Propuesta (pendiente de aprobar)
**Objetivo «mejores en todo» frente a Reality Design 1.29** (análisis en `docs/ANALISIS_RD_V129.md`): instalación de 3
pasos (APK universal, asistente de primer arranque, pendrive automático, catálogo dentro de la app, sin accesibilidad
ni adb), empezar la fase 4 (CAN) con la radio real del cliente, color del coche desde la app, diagnóstico por QR,
varios idiomas.

## D-026 · 2026-11-12 · Decisión del cliente
**Cómo pone su coche el cliente: catálogo de packs listos + un solo texto para la IA + servicio «Te lo hacemos».**
Objetivo: que nadie se agobie. El cliente no ve una lista de tareas; copia un texto, lo pega en su IA y la IA le guía.
- **Catálogo:** packs ya hechos de los coches más comunes, **fuera de la app** (Área de cliente / web). Cada pack ocupa
  ~2 MB y se descarga solo el suyo: la app no crece. Lo ampliamos nosotros con el generador (D-023).
- **Un solo prompt para IA gratuita** que pregunta primero «¿está tu coche en el catálogo?» y lleva por un camino:
  A) está → instalar la app en la radio e importar el pack (10–15 min, sin 3D);
  B) no está → pedirlo con el servicio «Te lo hacemos» (precio orientativo +1 €, por decidir);
  C) hacerlo él gratis con Blender y el complemento (30–60 min). Hay otro prompt para IA con acceso a archivos.
- **Servicio «Te lo hacemos»:** formulario (marca, modelo, año, puertas, color) → lo hacemos con el generador → se le
  entrega listo y queda en el catálogo para los siguientes. Pendiente de decidir: precio final, plazo y qué se hace si no
  existe un modelo válido (propuesta: devolver el dinero o dejarle el coche por defecto con su marca y su color).
- Pendiente de consultar antes de vender el catálogo: los logotipos de marca son marcas registradas.
- Orden: 1) catálogo inicial (~30 coches; cada descarga de modelo, con aprobación del cliente); 2) prompt único;
  3) procedimiento del servicio; 4) web/Área de cliente (fase 9); 5) más adelante, descargar el coche desde la app.

## D-025 · 2026-11-12 · Decisión del cliente
**El complemento de Blender (CarVision Pack Builder) se distribuye con licencia GPL-3.0-or-later.** Es la licencia habitual
de los complementos de Blender, porque usan su API (GPL). Afecta solo a `blender/carvision_pack_builder/`: la app
CarVision y el resto del proyecto no cambian de licencia. Cada archivo del complemento lleva la cabecera SPDX y su
`blender_manifest.toml` lo declara. El texto oficial de la GPL v3 (gnu.org) va en
`blender/carvision_pack_builder/LICENSE.txt` y dentro del .zip del complemento.

## D-024 · 2026-11-12 · Decisión del cliente
**Coche por defecto: el Focus MK3 del pack actual, sin ninguna marca, presentado como «CarVision».**
Se buscaron alternativas: el «Car Concept» de Khronos (CC BY) se descartó por ser demasiado deportivo (se quiere un coche
de calle normal); los turismos gratuitos más populares de Sketchfab (VW Polo, Skoda Superb, Nissan Micra…) se
descartaron porque son modelos extraídos de mods de GTA/Forza con una licencia CC BY que el que los sube no puede dar.
El Focus tiene 5 puertas (se ven todas las funciones de aperturas) y ya estaba pulido en la app.
El Ford Focus con su marca se conserva como **coche de prueba instalado como importado** en el primer arranque
(`PackRepository.TEST_PACKS`), para enseñar cómo queda un coche con su logotipo; se puede borrar como cualquier importado.

## D-023 · 2026-11-12 · Decisión del cliente (propuesta de implementación pendiente de aprobar)
**Se venden la app y también el código fuente.** Objetivo: que cualquier comprador pueda poner el coche que quiera,
aunque no sepa de tecnología, empezando por un prompt inicial para una IA gratuita y siguiendo sus instrucciones.
Como una IA de chat no puede preparar por sí sola un modelo 3D con puertas animadas, la propuesta es: la parte técnica
la hace una herramienta propia y probada (generador de packs), la IA solo guía paso a paso, y la app y el validador
impiden instalar un pack roto. Personalizar el coche nunca exige recompilar.

## D-021 · 2026-11-12 · Aprobada (plan de Fase 6)
**Avisos de conducción como ayuda, no como seguridad.** El aviso de apertura abierta en marcha se activa por encima
de 5 km/h y se retira por debajo de 3 km/h (histéresis para que no parpadee). Solo usa datos válidos o simulados
(nunca antiguos). El tono se sintetiza en la app (dos notas suaves, sin archivos de terceros); la voz usa el motor
de voz que traiga la radio y está **desactivada por defecto**. Todo se puede desactivar en Ajustes → Avisos.
El historial de viajes se guarda en JSON propio, igual que el carnet (D-020); con 300 viajes como máximo, Room no aporta.

## D-012 · 2026-11-11 · Aprobada
**Tema automático: por las luces del coche si hay señal (petición del cliente) y, si no, por hora**
(oscuro de 20:00 a 08:00), porque muchas radios no cambian el modo noche del sistema. Implementado; la señal real
llegará con el CAN Adapter de cada radio.
