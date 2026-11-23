# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [Sin publicar]

### Añadido (preparación del lanzamiento v1, 2026-11-14)
- **Web de prueba publicada** (2026-11-20) en https://carvisiones.vercel.app, sin dominio propio ni pagos activos.
- **App 1.0.0** (`versionCode` 100) en `dist/CarVision.apk`, probada en el emulador. Firmada todavía con la clave de
  pruebas: la definitiva se crea con `scripts/crear_clave_firma.ps1` (la contraseña solo la conoce el titular; la clave
  vive en `%USERPROFILE%\.carvision`, fuera del proyecto).
- **Kit de TikTok** (`docs/marketing/TIKTOK.md`) con 3 vídeos verticales 1080×1920 montados con grabaciones reales de la
  app (`docs/marketing/tiktok/01_arranque.mp4`, `02_puertas.mp4`, `03_color.mp4`; «(modo demo)» donde se ven las
  puertas) y la foto de perfil `perfil.png`. Script de montaje: `docs/marketing/tiktok/montaje_vertical.py` (Blender).
- **Lista de pasos para publicar la web** en `docs/WEB.md` (Vercel por CLI, variables, DNS de Arsys, Resend, compra de
  prueba). Comprobado en la documentación de Vercel que la descarga de la APK (17,5 MB) cabe: va en stream.

### Corregido
- **App 0.11.0** (encontrado por el cliente probando el emulador):
  - El Ford Focus de prueba se instalaba una sola vez y la copia se quedaba con un modelo antiguo: tenía las
    **ruedas traseras fijas**. Ahora se actualiza al actualizar la app (si el usuario no lo ha borrado).
  - **Quitar el logotipo** elegido para un coche: botón «Quitar» en el Garaje (vuelve el del pack o la marca en texto).
  - Sin el **icono de la app de música** en la esquina de la carátula.
  - La APK ya no mete dentro los coches del catálogo (Golf…): se descargan aparte; sigue en 17,5 MB.
  - **Hueco sin coche al terminar la animación de arranque**: con la radio cargada, el motor 3D apenas podía dibujar
    mientras se animaba el logotipo y el coche aparecía hasta 1,5 s tarde. Ahora el visor avisa cuando el coche ya está
    dibujado y el logotipo espera quieto a esa señal antes de fundirse (como mucho 2,5 s). Probado grabando la pantalla
    (el caso que fallaba): el coche se ve durante todo el fundido. Sin carga, el arranque dura lo mismo que antes.
- **Generador: coches automáticos sin fallos visuales** (revisión pedida por el cliente comparando con el Focus hecho a
  mano): asientos verdes, pinzas y discos pintados, llantas negro espejo, antinieblas con tiras brillantes, puertas que
  arrastraban piezas del salpicadero y se veían huecas por dentro, trocitos de piloto flotando al abrir el portón.
  Acabados por tipo de pieza, materiales del objeto pasados a la malla, regla de contacto para las aperturas y
  guarnecido interior automático. Segunda revisión: puertas traseras que se llevaban piezas del asiento (el
  cambio que se probó en los faros delanteros se deshizo: estaban bien). Tercera revisión: el panel interior de las puertas se quedaba
  dentro del coche al abrirlas (en los tres coches), incluida la tira fina de la base de la ventanilla; pilotos
  traseros negros.
- **Coche por defecto y Ford Focus regenerados**: panel interior con la puerta, puertas traseras con la bisagra delante
  (antes en la rueda), marco de la ventanilla con la puerta. Cristales y asientos sin cambios. Detalle en
  `docs/GENERADOR_COCHES.md`.

- **Generador con modelos sacados de juegos** (prueba del catálogo con 5 coches): la rueda que viene una sola vez en el
  centro se copia a las 4 esquinas (bujes o pasos de rueda); se quitan restos del juego (tapacubos en el centro,
  placas bajo el suelo, piezas lejos del coche); el coche se apoya sobre sus ruedas y se centra entre ellas; una
  puerta de la que solo se encuentra el cristal ya no se abre como un cristal flotando; una apertura que arrastra
  media carrocería se descarta con aviso; las puertas del coche ya no se toman por ruedas.

- **Catálogo v1 hecho a mano (D-030)**: VW Golf VI terminado y probado en la app (logotipo VW en inicio, Garaje y
  animación de arranque; emblemas cromados; sin colores de relleno). El Focus automático de pruebas se quitó (queda el
  coche por defecto y el Focus pulido). Generador: `remove` (quitar piezas por nombre), piezas duplicadas fuera,
  gancho `finish` para el script de acabado de cada coche.

### Añadido
- **Web: segunda versión (revisión del cliente)**: tema claro y oscuro con los colores de la app (selector Auto/Claro/
  Oscuro), páginas Funciones, Coches, Instalar, Ayuda y «Hazlo tú con una IA», fotos de los coches del catálogo,
  vídeo real de la animación de arranque (Ford Focus), capturas nuevas de todas las funciones; en la portada,
  «Todo lo que lleva» con 3 bloques grandes y una lista limpia del resto; código fuente y texto para IA de
  pago en el Área (D-031), menú para móvil.
- **Web carvisiones.es (v1, sin publicar)** en `web/`: portada móvil primero con la app real en una radio, compra con
  Stripe (casilla de desistimiento), Área de cliente con enlace firmado (descargas protegidas, texto para la IA,
  «Te lo hacemos»), reenvío del enlace por correo y páginas legales. Detalle en `docs/WEB.md`.
- **Instalación ×5 (v0.10.0, D-028)**, probada en el emulador (Android 8.1, 1280×720):
  - **Una sola APK** para todas las radios (`dist/CarVision.apk`, 17 MB; ARM 64, ARM 32 y x86), con las librerías
    nativas comprimidas. Antes había que elegir entre dos.
  - **Asistente de primer arranque**: pantalla de inicio, tu coche (con color) y música; cada paso se marca solo al
    volver de los ajustes de Android; «Más tarde» no vuelve a molestar; se reabre en Ajustes › Sistema.
  - **Pendrive automático**: al conectar un USB aparece «Pendrive conectado · Buscar mi coche»; Android pide permiso
    una vez y la app lista los packs del USB para instalarlos con un toque. Sin permisos de almacenamiento.
    «No preguntar» para pendrives de música.
  - **Color del coche desde la app**: 12 colores habituales + «Original», al instante y por coche. El generador nombra
    la pintura «paint» y lo declara en el manifiesto (`paint.material`).
  - **«Volver al launcher original»** como opción visible en Ajustes › Sistema.
- **Análisis de Reality Design 1.29** (`docs/ANALISIS_RD_V129.md`) y propuesta para superarles (D-028).
- **Preselección del catálogo por calidad** (D-027): 38 coches con modelo elegido, sin descargar todavía.
- **Plan del catálogo de coches (D-026)**: packs ya hechos fuera de la app, prompt único y servicio «Te lo hacemos».
  Búsqueda de modelos para 46 coches comunes (`scripts/buscar_catalogo.py`, `docs/CATALOGO_COCHES.md`), sin descargar nada.

### Cambiado
- **Prompt único para IA gratuita**: primero pregunta si la app está instalada y si el coche está en el catálogo, y
  lleva por el camino que toca: instalar la app en la radio (nuevo), importar del catálogo, pedir «Te lo hacemos» o
  hacerlo con Blender. Probado en ChatGPT gratis real.
- **Regla nueva para elegir modelo**: mirar el perfil del autor en Sketchfab; se descartan las cuentas que dicen
  importar de juegos o de otros (prompts y `docs/CREAR_MI_COCHE.md`).
- **Coche por defecto de CarVision** (pack `carvision_default`): el compacto de 5 puertas del pack del Focus MK3, sin
  ninguna marca, con el nombre «CarVision» en la cabecera, la barra lateral y la animación de arranque. Es el que ve
  quien no ha elegido coche ni importado ninguno.
- **El Ford Focus con su marca pasa a ser un coche de prueba «importado»**: viaja dentro de la app, pero en el primer
  arranque se instala como si el usuario lo hubiera importado (etiqueta «Importado», se puede borrar y, si se borra, no
  vuelve). Sirve para ver cómo queda un coche con su logotipo en la barra lateral y en el arranque.

### Eliminado
- El coche de prueba esquemático ya no aparece en el Garaje: se mueve a `blender/samples/demo_hatchback` como muestra de
  desarrollo.
- `prepare_focus_mk3.py --no-logos` ahora quita también los marcos de los emblemas y la insignia «ST», y tapa con
  chapa del color de la carrocería los huecos donde iban los emblemas (antes quedaban óvalos oscuros).
- El nombre del coche se muestra como «Marca Modelo» o solo «Marca» si el modelo está vacío.

### Añadido
- `blender/scripts/inspect_glb.py`: informe de cualquier .glb (cajas, pivotes, materiales, texturas) antes de prepararlo.
- `cv_common`: sombra de contacto, unión de piezas y exportación reutilizables para el futuro generador de coches.
- **Generador de coches (motor + línea de comandos)**: `blender/scripts/build_pack.py -- config.json` convierte un
  modelo .glb/.gltf/.fbx/.obj/.blend en un pack listo para importar: orienta y escala el coche, encuentra ruedas
  (y endereza las giradas), puertas, capó y portón (por nombre o por geometría, también paneles partidos en dos),
  coloca bisagras, repara materiales para glTF, da acabado metalizado a la pintura (recoloreable), prepara el logotipo
  (quita el fondo), hornea la sombra, comprime, valida el manifiesto y genera .zip, renders e informe. Probado con el
  Focus original sin ajustes a mano. Diseño y estado: `docs/GENERADOR_COCHES.md`.
- **Complemento de Blender «CarVision Pack Builder»** (`dist/carvision_pack_builder.zip`, se crea con
  `scripts/make_addon_zip.py`): panel de 5 pasos en la barra lateral del visor 3D (abrir, revisar por colores, corregir
  asignando piezas y probando aperturas, marca/logotipo/licencia, crear pack).
- El complemento se distribuye con licencia GPL-3.0-or-later (D-025): cabecera SPDX en cada archivo y el texto
  oficial de la GPL v3 en `LICENSE.txt`, incluido en el .zip.
- **Pruebas de uso** del generador: con la interfaz real de Blender (clics simulados y capturas), en ChatGPT gratis real
  y con una simulación completa IA + persona sin conocimientos (que llegó a importar su León en la radio). Cambios:
  panel adaptado a la barra lateral estrecha (sin textos cortados), sin contorno de selección que tape los colores,
  avisos con nombres de botones (no claves del config) y recalculados al corregir, «Largo del coche en metros (de
  parachoques a parachoques)», aviso de que el color se ve en el pack; guía y prompt con Partes A-D, GLB, «Quick Setup»
  en español, dónde está el complemento, cómo comprobar una apertura no encontrada, opciones exactas de los desplegables,
  logotipo con cuadritos falsos, versión del modelo y pendrive en el selector de Android.
- **Guía `docs/CREAR_MI_COCHE.md`** y **prompts** para IA gratuita y para IA con acceso a archivos (`docs/prompts/`).
- El generador elige la pintura por lo que se ve desde fuera, reconoce radios de llanta sueltos sin arrastrar la pinza
  de freno, y el informe lista qué piezas (por su nombre original) van en cada grupo. Probado también con el
  «Car Concept» de Khronos y con un coche de una sola pieza sin nombres.

### Corregido
- Al reimportar el coche que ya estaba elegido (p. ej. una versión mejorada), la app seguía mostrando el anterior
  hasta reiniciarla.

## [0.9.0] — 2026-11-12 · Preparado para entregar

### Añadido
- **APK final optimizada** (`gradlew assembleRelease`): R8 y recursos reducidos, una APK por arquitectura.
  13,4 MB (arm64-v8a) y 11,8 MB (armeabi-v7a) frente a 47 MB de la de depuración. Firma con
  `android/keystore.properties` (no se versiona); sin ese archivo se firma con la clave de depuración, solo para probar.
  Probada en emulador: coche 3D, carnet, PDF, ordenador de a bordo e historial funcionan igual que en depuración.
- **Guías**: `docs/VEHICLE_PACKS.md` (formato de los coches), `docs/INSTALACION.md` (instalar en la radio) y
  `docs/GUIA_DESARROLLADOR.md` (compilar, firmar, añadir un CAN Adapter o un coche).
- Al importar un coche cuya licencia no permite uso comercial, el aviso lo indica («solo uso personal»).

### Corregido
- **Ruedas que parecían ir hacia delante y hacia atrás** (efecto «rueda de carreta»): a velocidad real los radios
  saltaban 45–90° por fotograma. Ahora el giro visible se limita (≈1,7 vueltas/s), avanza como mucho 12° por fotograma,
  acelera y frena con suavidad, y se dibuja a frecuencia completa. Lógica separada en `WheelSpin` con 5 tests.
- El logotipo en texto (coches sin logotipo) salía cortado en las tarjetas del Garaje («CARVISIOI»): ahora se ajusta
  al hueco disponible.
- Atribución del Focus corregida: decía «logotipos eliminados», pero los emblemas se conservan desde la v0.4.
- La APK incluía por error la descarga original del Focus (`ford-focus-st-mk3.zip`, 9,8 MB) que estaba en la
  carpeta del pack: ahora solo se empaquetan los tipos de archivo que usa un pack (los mismos que admite el importador).

## [0.8.0] — 2026-11-12 · Funciones WOW (primera parte)

### Añadido
- **Historial de viajes** (pantalla Vehículo → «Historial»): inicio, distancia, duración, media y máxima de cada viaje,
  con la etiqueta DEMO en los simulados y el total de km reales. Se guarda en `files/trips.json` (escritura atómica)
  con un punto de guardado cada 20 s, así que un corte de corriente al quitar el contacto no pierde el viaje.
  Al arrancar, el viaje anterior pasa al historial. Viajes de menos de 0,2 km no se guardan; máximo 300. 4 tests.
- **Aviso de apertura abierta en marcha**: si una puerta, el maletero o el capó están abiertos a más de 5 km/h
  (se retira por debajo de 3 km/h o al cerrar), la cabecera muestra el aviso con un punto ámbar que respira.
  Tono propio sintetizado en la app (sin archivos de audio de terceros) y, opcionalmente, lectura por voz con el motor
  de voz en español de la radio. No es una función de seguridad certificada y así se indica en Ajustes.
- **Animación de bienvenida**: al terminar el logotipo de arranque, la cámara rodea el coche desde el lado contrario
  y se acerca hasta la vista principal (3 s, movimiento suave). Tocar el coche la interrumpe.
- **Ajustes → Avisos**: activar o desactivar el aviso, el sonido y la voz (voz desactivada por defecto).

### Cambiado
- Modo demostración: la velocidad simulada ya no se para al abrir una puerta, para poder probar el aviso.
- Cifras siempre en formato español (coma decimal), aunque la radio esté en otro idioma.

### Corregido
- Pantalla Vehículo en 1280×720: el botón «Historial» y las notas del viaje salían cortados; las etiquetas de
  aperturas se acortan («Delantera izq.») para que quepan sin cortar palabras.

## [0.7.0] — 2026-11-12 · Ordenador de a bordo

### Añadido
- **Pantalla Vehículo (ordenador de a bordo)** con tres bloques:
  - **Viaje actual**: distancia, duración (total y en marcha), velocidad media en marcha y máxima. Calculado a partir
    de la velocidad del coche o, si es fiable, de la diferencia del cuentakilómetros. Botón «Nuevo viaje».
    El consumo medio se muestra como «No disponible» hasta tener el consumo instantáneo del coche (no se estima).
  - **Datos del coche**: velocidad, revoluciones, combustible, temperatura exterior, batería, kilometraje y luces,
    cada uno con su origen (del coche, simulado, antiguo, introducido a mano) y su antigüedad («hace 3 s»).
  - **Aperturas**: vista en planta y estado de cada puerta, maletero y capó.
- `TripComputer` (Kotlin puro) con 8 tests: integra la velocidad, no rellena huecos sin datos, marca los viajes simulados.

## [0.6.0] — 2026-11-12 · Carnet de mantenimiento

### Añadido
- **Carnet de mantenimiento digital** (pantalla Servicio): 14 tipos de intervención (aceite, filtros, distribución,
  bomba de agua, refrigerante, líquido de frenos, frenos, neumáticos, batería, ITV, revisión y otras libres).
  Cada registro guarda fecha, kilometraje, descripción, coste, taller, observaciones y, si el taller lo indicó,
  próxima fecha y próximo kilometraje.
- **Próximas intervenciones** calculadas por km y por fecha, ordenadas por urgencia, con barra de progreso y estado
  (vencido / próximo / al día / sin datos). Intervalos **editables y desactivables**; los valores por defecto se
  presentan como orientativos.
- **Kilometraje**: del coche si el dato es real (CAN); si no, introducido a mano. Los datos simulados no se usan nunca.
- **Exportación a PDF** (A4) con selector de archivos o, si la radio no lo tiene, a `Android/data/<app>/files/export`.
- **Avisos**: chip de Inicio con el resumen ("1 vencido", "2 próximos", "Todo al día") y aviso discreto al arrancar.
- Módulo `core:maintenance` (Kotlin puro) con 7 tests; formularios propios (`CvTextField`, `CvButton`).

### Corregido
- Con el teclado abierto, los campos inferiores quedaban tapados: el editor se ajusta por encima del teclado.
- Al cerrar el teclado volvían a aparecer las barras de Android: ahora se ocultan de nuevo solas.

## [0.5.1] — 2026-11-11

### Corregido
- Al cambiar a un coche sin logotipo se quedaba el del coche anterior (p. ej. Ford con un Seat importado).

### Añadido
- **Logotipo por coche, automático**: el del usuario (si lo ha elegido), si no el del pack y, si no hay ninguno,
  la **marca en texto** (mayúsculas espaciadas, tamaño ajustado para que nunca se corte). También en la animación de arranque.
- **Garaje › Logotipo**: elegir una imagen para cualquier coche; si no tiene transparencia, **se quita el fondo
  automáticamente** (blanco o cuadros pintados) con borde suavizado, y se recorta.
- Tarjeta de música: **logotipo de la app que suena** como distintivo en la esquina de la carátula.

### Cambiado
- Tarjeta de música **sin tinte de color** (a petición: rompía la armonía con el resto).
- **Visor 3D con bordes fundidos**: el suelo de luz y la sombra se desvanecen en los cuatro lados; no se ve ningún
  rectángulo en ningún tema ni ambiente. El visor ocupa más espacio.

## [0.5.0] — 2026-11-11

### Añadido
- Tarjeta de música teñida con la carátula (retirado en 0.5.1 a petición del cliente): el fondo adoptaba el color de la portada, con transición
  suave al cambiar de canción (extracción propia, sin dependencias).
- **Garaje** (botón en la cabecera del coche): elegir coche e **importar packs .zip sin recompilar**, con selector de
  archivos o desde la carpeta `Android/data/com.carvision.launcher/files/import` (para radios sin selector).
  Los packs importados se pueden eliminar.
- **Importador seguro de packs** (`PackZipImporter`): solo archivos de datos, protección contra rutas maliciosas y
  archivos enormes, validación del manifiesto, del modelo y del logotipo antes de instalar. 4 tests nuevos.
- **Ambientes** (botón en la cabecera): 6 fondos propios con degradado y luces de color — Grafito, Medianoche, Aurora,
  Atardecer, Burdeos y Titanio —, cada uno con variante clara/oscura y su color de acento.
- `scripts/make_pack_zip.py`: empaqueta un pack en .zip listo para importar.
- Herramienta de depuración (solo en la compilación debug) para simular música con carátula en el emulador.

### Cambiado
- Iconos de **Apps** (cuatro módulos redondeados) y **Ajustes** (engranaje de perfil suave) con el mismo trazo que el resto.

## [0.4.1] — 2026-11-11 · Pulido OEM+

### Corregido
- **Ruedas delanteras que temblaban y llevaban "algo negro" por fuera**: el eje se tomaba de un tapón de tornillo
  (también usa el material de la llanta) y entraban en la rueda la moldura negra del paso de rueda y la pinza de freno.
  Ahora el eje sale de la llanta principal y solo giran las piezas centradas en él y la tornillería.
- **Logotipo pixelado** en la columna lateral y en el arranque: reducción progresiva de alta calidad al tamaño exacto
  en píxeles (`CrispImage`).

### Cambiado
- **Suelo de estudio**: foco de luz con caída gaussiana, sin borde ni anillo, y **sombra de contacto horneada** desde la
  silueta del coche (más oscura bajo los neumáticos). El coche ya no "flota" ni parece estar sobre una plataforma.
- **Columna lateral estilo marcas premium**: integrada en el fondo con una línea fina, logotipo de la marca arriba
  (vuelve a Inicio), iconos propios monocromos (`CvIconsCustom`) con su texto debajo y halo de luz en el activo.
- El encuadre del coche ignora el plano de sombra.

## [0.4.0] — 2026-11-11 · Diseño OEM+

### Cambiado
- **Pantalla completa**: se ocultan la barra de estado y los botones de Android (accesibles deslizando desde el borde).
- **Iconos propios de línea fina** (basados en Lucide, ISC) en toda la app; eliminados los iconos Material de Android
  (y su dependencia, que además reduce el APK). Controles multimedia con glifos rellenos propios.
- **Nueva paleta**: negro profundo, superficies grafito mínimas, un único acento azul hielo; tema claro equivalente.
- **Tipografía**: cifras grandes y finas (reloj, velocidad), títulos de peso medio, etiquetas pequeñas espaciadas.
- **Inicio tipo CarPlay de serie**: columna lateral con el logotipo de la marca y accesos; coche sin marco con suelo de
  luz; reloj grande; tarjeta de música tipo CarPlay; velocidad como cifra protagonista con datos secundarios.
- **Suelo de luz** difuminado con anillo fino, que se adapta al tema, en lugar del disco sólido.
- Controles de simulación movidos a un panel que se abre desde "Datos del coche".
- **Cajón de apps** con iconos squircle uniformes (capas de los iconos adaptativos sin la máscara del sistema).
- **Ajustes** en lista agrupada (Pantalla, Vehículo, Sistema, Información).
- **Avisos propios** en lugar de los Toast de Android.
- Pulsaciones sin el efecto de onda de Android: el elemento se atenúa y encoge levemente.

## [0.3.0] — 2026-11-11

### Añadido
- **Animación de arranque** con el logotipo de la marca (halo, destello y línea de luz, ≈2,8 s) al iniciar el launcher,
  p. ej. al encender la radio. El logotipo sale del pack (`brand.logo`); el de Ford se renderiza desde el emblema del
  propio modelo 3D (`blender/scripts/render_brand_logo.py`). Se puede desactivar en Ajustes.
- **Tema automático según las luces del coche**: si hay señal de luces, luces encendidas = tema oscuro; si no, por hora.
  Señal `lightsOn` en el modelo de datos y botón "Luces" en el modo demo.
- **Música en reproducción**: título, artista, carátula, app y controles anterior/reproducir/siguiente de cualquier app
  con sesión multimedia. Requiere conceder acceso a notificaciones (la tarjeta lleva al ajuste).
- **Ruedas que giran** según la velocidad (definidas en `wheels` del manifiesto).
- Guarnecidos interiores de las puertas del Focus.

### Cambiado
- La animación de arranque usa el **logotipo oficial en plano** (alta calidad) en lugar del emblema renderizado del
  modelo. Recortado con `blender/scripts/extract_oval_logo.py` (elipse exacta y borde suavizado).
- Los emblemas de la marca se **conservan por defecto**; `--no-logos` los quita.
- Modelo del Focus comprimido con Draco: de 7,8 MB a 1,76 MB.
- El coche 3D ya no se recarga al cambiar de pantalla; deja de dibujar mientras está tapado.
- Con solo las ruedas en movimiento se dibuja a media frecuencia para ahorrar GPU.

## [0.2.0] — 2026-11-11

### Añadido
- Pack **Ford Focus MK3 5 puertas** (`vehicle-packs/ford_focus_mk3_5p`), basado en "Ford Focus ST (MK3)" de
  savelliy07 (CC BY 4.0): 4 puertas, portón y capó animados; pack predeterminado.
- Pipeline de Blender para preparar coches importados: `inspect_blend.py`, `analyze_car.py`,
  `prepare_focus_mk3.py`, `render_preview.py`. Guía en `docs/BLENDER_PIPELINE.md`.
- Selector de vehículo en Ajustes; el pack elegido se guarda.
- Botón de simulación del capó.

### Cambiado
- Plataforma más oscura y mate, anillo de luz cian visible, coche más grande en pantalla.
- Fila de simulación en una sola línea con desplazamiento.

## [0.1.0-spike] — 2026-11-11

### Añadido
- Proyecto Android (`android/`): Kotlin, Compose, 6 módulos (`app`, `core:model`, `core:designsystem`,
  `vehicle:data`, `vehicle:packs`, `render3d`).
- Pantalla de inicio: barra lateral, reloj y fecha, tarjeta de música, datos del vehículo con etiqueta de origen,
  tarjeta del coche con estado de aperturas.
- Coche 3D con Filament: fondo transparente, giro táctil con inercia, vista inicial, vista en planta, cámara que gira
  sola para enseñar la puerta que se abre. Solo dibuja cuando hay cambios.
- Animación de puertas y portón definida en el `manifest.json` del pack.
- Vista en planta 2D de respaldo para GPUs sin OpenGL ES 3.0.
- Formato de Vehicle Pack v1 y validador (rutas seguras, ángulos, ejes, licencia).
- Modo demostración con datos marcados como simulados y controles para abrir/cerrar aperturas.
- Temas claro, oscuro y automático; tamaño de interfaz; persistencia con DataStore.
- Cajón de aplicaciones, accesos a navegación, música y teléfono.
- Scripts de Blender: coche de prueba, plataforma con anillo de luz, validador de GLB.
- 9 tests unitarios.

### Corregido
- La app se cerraba en GPUs con OpenGL ES 2.0 (formato de textura de la iluminación no soportado).
- El reloj no se actualizaba al cambiar la hora o la zona horaria del sistema.

## [Fase 0] — 2026-11-11
- Análisis de requisitos, viabilidad y arquitectura (`docs/FASE_0_ANALISIS.md`).
- Análisis de capturas y de la competencia (`docs/ANALISIS_REFERENCIAS.md`).
