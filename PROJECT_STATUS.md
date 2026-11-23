# Estado del proyecto — CarVision Launcher

Última actualización: 2026-11-14

| Fase | Estado | Evidencia / notas |
|---|---|---|
| 0. Análisis y requisitos | ✅ Hecho | `docs/FASE_0_ANALISIS.md`, `docs/ANALISIS_REFERENCIAS.md` |
| 1. Arquitectura | ✅ Aprobada (D-003 a D-007) | 6 módulos Gradle; ver `DECISIONS.md` |
| 2. Interfaz + coche estático | ✅ Hecha (emulador) | Diseño OEM+, temas, ambientes, logos, arranque; probado en 1024×600 y 1280×720. Falta la radio real |
| 3. Animaciones 3D | ✅ Hecha (emulador) | Puertas, portón, capó, ruedas, giro con inercia, vista en planta, bienvenida. Falta probar con el dedo en la radio |
| 4. Comunicación CAN | ⏳ No iniciada | Solo modo demo y "sin conexión" |
| 5. Funciones principales | ✅ Hecha (emulador) | Carnet de mantenimiento y ordenador de a bordo; datos reales pendientes del CAN |
| 6. Funciones WOW | 🟡 Primera parte hecha (emulador) | Historial de viajes, aviso de apertura en marcha (banner, tono, voz), bienvenida 3D. v0.8.0 |
| 7. Packs de vehículos | ✅ Hecha (emulador) | Formato, validador, importación .zip (selector de archivos y carpeta, probadas), garaje, guía `docs/VEHICLE_PACKS.md` |
| 8. Documentación y entrega | 🟡 Casi completa | APK final optimizada (13 MB); guías de instalación, de packs y de desarrollo. Falta probarlas en una radio real |
| 9. Web y venta | 🟡 Lista para publicar | Web hecha y probada en local (`docs/WEB.md`); App 1.0.0 en `dist/` (clave de pruebas). Falta: clave de firma, Vercel, DNS, Resend, compra de prueba |
| 10. Marketing | ⏸️ Aplazado hasta tener la web publicada | Borrador preparado: `docs/marketing/TIKTOK.md`, 3 vídeos verticales y foto de perfil (se revisará con el cliente después) |

## Funcionalidades (estado real)

| Función | Estado |
|---|---|
| Compilación (`gradlew assembleDebug`) | ✅ Probado (2026-11-11) |
| Pantalla de inicio (reloj, música, datos, barra lateral) | ✅ Probado en emulador 1024×600 (Android 8.1) |
| Escalado de interfaz por altura de pantalla | 🟡 Implementado; probado solo en 1024×600 |
| Temas claro / oscuro / automático (por hora), persistentes | ✅ Probado en emulador |
| Coche 3D (Filament) con fondo transparente | ✅ Probado en emulador con OpenGL ES 3.1 |
| Respaldo 2D si la GPU no tiene OpenGL ES 3.0 | ✅ Probado en emulador con OpenGL ES 2.0 |
| Puertas y portón animados desde el manifiesto del pack | ✅ Probado (modo demo) |
| Giro táctil con inercia, vista inicial, vista en planta | 🟡 Implementado; giro táctil no probado con dedo real (solo toques por adb) |
| Modo demostración, datos marcados como simulados | 🧪 Simulado, por diseño |
| Datos reales del vehículo | ⛔ No hay ningún CAN Adapter todavía |
| Cajón de aplicaciones | 🟡 Implementado, sin probar a fondo |
| Hora y zona horaria actualizadas al cambiar | ✅ Probado (fallo encontrado y corregido) |
| Coche por defecto «CarVision» (pack `carvision_default`, Focus sin marcas) | ✅ Probado en emulador: sin emblemas (frontal, trasera, insignia ST, volante), nombre y arranque «CarVision», 1,73 MB |
| Ford Focus con marca como coche de prueba «importado» | ✅ Probado: se instala en el primer arranque, carga con su logotipo, se borra y no vuelve |
| Ford Focus MK3 5p (pack `ford_focus_mk3_5p`) | ✅ Probado en emulador: 4 puertas, portón y capó animados, guarnecidos, emblemas, 1,76 MB |
| Ruedas que giran con la velocidad | ✅ Probado (modo demo) |
| Animación de arranque con logotipo de la marca | ✅ Probado: aparece al reiniciar el emulador con CarVision como inicio |
| Tema automático por luces del coche | ✅ Probado con la señal simulada; 🔌 con datos reales necesita un CAN Adapter |
| Música en reproducción y controles | 🟡 Implementado; probados el permiso y el estado "Sin música". Sin probar con una app reproduciendo (el emulador no trae reproductor) |
| CarVision como pantalla de inicio predeterminada | ✅ Probado en emulador (elegido como "Siempre", arranca tras reiniciar) |
| Diseño OEM+ (pantalla completa, iconos propios, paleta, tipografía, dock, suelo de luz) | ✅ Probado en emulador 1280×720, tema oscuro y claro |
| Música: carátula, título, artista y fondo teñido | ✅ Probado con sesión de prueba (dos carátulas de colores distintos) |
| Garaje e importación de packs .zip | ✅ Probado: con el selector de archivos de Android y desde la carpeta de importación, sin recompilar |
| APK final (R8, una por arquitectura) | ✅ Probada en emulador x86: 13,4 MB arm64-v8a, 11,8 MB armeabi-v7a. 🔶 Las APK ARM sin probar en radio |
| Giro de las ruedas sin efecto «rueda de carreta» | ✅ 5 tests + vídeo en emulador (`docs/capturas/fase6/ruedas_giro.mp4`) |
| Ambientes de fondo (6) | ✅ Probado en emulador |
| Logotipo por coche (pack, elegido o marca en texto) | ✅ Probado: coche sin logotipo → marca en texto; logotipo elegido con fondo de cuadros → fondo quitado |
| Visor 3D sin bordes visibles | ✅ Probado en claro y oscuro con varios ambientes |
| Carnet de mantenimiento (registros, próximas, intervalos editables) | ✅ Probado en emulador: alta, edición, persistencia tras cerrar la app |
| Exportación del carnet a PDF | ✅ Probado (ejemplo en `docs/ejemplos/`) |
| Avisos de mantenimiento (chip de Inicio y aviso al arrancar) | ✅ Probado |
| Ordenador de a bordo (viaje, datos con origen, aperturas) | ✅ Probado con datos simulados; 🔌 datos reales pendientes de CAN Adapter |
| Historial de viajes | ✅ Probado en emulador: viajes guardados entre reinicios, etiqueta DEMO, borrar historial |
| Aviso de apertura abierta en marcha (banner) | ✅ Probado con datos simulados: aparece por encima de 5 km/h, se retira al parar o al desactivarlo |
| Aviso: tono y voz | 🟡 Implementado; sin oír (el emulador se lanzó sin audio). Pendiente de probar en la radio |
| Animación de bienvenida (cámara alrededor del coche) | ✅ Probado en emulador tras el logotipo de arranque |
| Tests unitarios | ✅ 32 pasan (2026-11-12) |
| 1280×720 | ✅ Probado: mismas proporciones que 1024×600, arranque en frío 1,4 s |
| Garaje: elegir coche (CarVision por defecto, Ford de prueba importado, importados por el usuario) | ✅ Probado; cambia el pack sin reiniciar |
| Una sola APK para todas las radios (17 MB) | ✅ Instalada y probada en emulador x86 (v0.10.0) |
| Asistente de primer arranque | ✅ Probado en emulador: los pasos se marcan solos al volver de los ajustes de Android |
| Pendrive automático (aviso + instalar desde USB) | ✅ Probado en emulador Android 8.1 con disco USB virtual; Android 10+ y radio real sin probar |
| Color del coche desde la app | ✅ Probado en emulador (al instante, por coche) |

## Generador de coches (D-023)

✅ Hecho: motor + línea de comandos (`blender/scripts/build_pack.py`), complemento de Blender con panel de 5 pasos,
prompts para IA gratuita y para IA con archivos, y guía `docs/CREAR_MI_COCHE.md`. Probado con 3 modelos muy distintos
(Focus original, Car Concept de Khronos, coche de una sola pieza sin nombres). Detalle en `docs/GENERADOR_COCHES.md`.
✅ Probado con la interfaz real de Blender (clics simulados), en ChatGPT gratis real y con una simulación IA + persona
sin conocimientos de principio a fin (`docs/GENERADOR_COCHES.md`). 🔶 Sigue faltando una persona real.
✅ Licencia del complemento: GPL-3.0-or-later (D-025), con el texto oficial de la GPL incluido en su .zip.

## Catálogo de coches y prompt único (D-026, D-030)

✅ **Cambio de plan (D-030, 2026-11-13):** catálogo de 7–8 coches hechos a mano, sin un fallo; la **v1 sale con un solo
coche, el Ford Focus MK3**. El generador queda como herramienta interna (prueba con 5 modelos de juego en
`docs/CATALOGO_COCHES.md`: ninguno salía perfecto solo). Prompt único para IA gratuita sin Blender: catálogo, coche
por defecto con su color y logotipo, o «Te lo hacemos». ⏳ Área de cliente y servicio: con la web.

## Próximos pasos (en orden; reordenado por el cliente el 2026-11-12: primero lo que no necesita la radio)

Hecho: carnet de mantenimiento (v0.6.0), ordenador de a bordo (v0.7.0), importación de packs .zip (v0.5.0),
Fase 6 primera parte (v0.8.0).

1. ~~APK final optimizada~~ ✅ (v0.9.0).
2. ~~Cerrar la Fase 7: guía de packs y selector de archivos~~ ✅ (v0.9.0).
3. ~~Fase 8: guía de instalación y guía para desarrolladores~~ ✅ (v0.9.0).
4. Probar en la radio real del cliente y recoger datos para el CAN.
5. Fase 4: CAN Adapter de solo lectura para esa radio.
6. Fase 6 (segunda parte): oír tono y voz en la radio; más avisos con señales CAN reales.
7. Fases 9 y 10: web, venta y marketing. 🔶 **Web hecha y probada en local** (`web/`, `docs/WEB.md`): portada,
   pago con Stripe, Área de cliente con descargas protegidas, «Te lo hacemos» y páginas legales; falta publicarla.
   **Adelantado por D-030: lanzar la v1 ya** (web, perfil, Área de cliente,
   pago) con el Focus como único coche del catálogo; después, actualizaciones y más coches.
