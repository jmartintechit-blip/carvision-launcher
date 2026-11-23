# Primera prueba en una radio real: el Ford Focus (1 GB, Android 10 Go)

Plan para instalar CarVision en la radio del Ford Focus y sacar de ahí todo lo que falle. Es la radio más exigente que
tenemos (ver `docs/RADIOS.md`): **Allwinner A133, 1 GB de RAM, Android 10 Go**, firmware NWD `K2401`.

## Qué llevar

- Un **pendrive** (FAT32) con el contenido de `dist/pendrive_radio_focus/`:
  - `CarVision.apk` — versión **1.0.1** (con el modo ligero para 1 GB).
  - `ford_focus_mk3_5p.zip` — el Focus del catálogo.
- El móvil para hacer **fotos y vídeos de la pantalla** (sirven más que cualquier descripción).
- Opcional: el portátil con un cable USB por si la radio permite depuración (para `adb`, ver el final).
- Códigos de la radio por si los pide (de `docs/RADIOS.md`): desarrollador `345543`, fábrica `1617` / `16176699`.

> La APK está firmada con la **clave de pruebas**. Cuando exista la clave definitiva habrá que desinstalar esta versión
> antes de poner la firmada (Android no deja actualizar entre firmas distintas). Los ajustes de CarVision se pierden al
> desinstalar; el resto de la radio no se toca.

## Instalar (10 minutos, con el coche arrancado o en contacto)

1. Pincha el pendrive. Abre el explorador de archivos de la radio › `CarVision.apk` › **Instalar**. Si pide permitir
   orígenes desconocidos, actívalo para el explorador (puede pedir el código de desarrollador o de fábrica).
2. Abre CarVision. Sale el asistente:
   - **Poner como inicio** › CarVision › **Siempre**. Si la radio no lo ofrece, busca en Ajustes › Aplicaciones ›
     Aplicaciones predeterminadas › Aplicación de inicio. En radios NWD puede estar en los ajustes de fábrica (`1617`).
   - **Tu coche**: con el pendrive dentro sale «Pendrive conectado» › **Buscar mi coche** › Permitir › **Instalar** el
     Focus. Color: el de su coche.
   - **Música**: en Android Go el ajuste no existe; pulsa **Saltar** (la tarjeta abrirá la app de música al tocarla).
3. Pulsa el botón **Inicio** de la radio: debe volver a CarVision.

**Volver atrás en cualquier momento:** CarVision › Ajustes › Sistema › **Volver al launcher original**, o desinstalar
CarVision desde Ajustes › Aplicaciones. No cambia nada más de la radio.

## Qué probar y apuntar

Marca cada punto como ✅ / ❌ y, si falla, foto o vídeo.

| # | Prueba | Qué mirar |
|---|---|---|
| 1 | Arranque en frío (quitar contacto, esperar 1 min, arrancar) | ¿Sale CarVision sola o vuelve el launcher de la radio? ¿Cuánto tarda en verse el coche? |
| 2 | Animación del logotipo de Ford | ¿Fluida? ¿Queda un hueco sin coche al terminar? |
| 3 | Coche en 3D | ¿Se ve bien (colores, emblemas, ruedas)? ¿O sale la vista 2D en planta? |
| 4 | Girar el coche con el dedo | ¿Responde rápido? ¿Va a tirones? |
| 5 | Datos del coche › Simulación › puertas, capó, maletero | ¿Se abren suaves? |
| 6 | Música (Bluetooth y USB) con CarVision de fondo | ¿Se corta el sonido? ¿La tarjeta abre la app de música? |
| 7 | Navegación (Google Maps u otra) y volver a Inicio | ¿CarVision tarda en volver? ¿Se reinicia (vuelve a salir el logo)? |
| 8 | Llamada por Bluetooth | ¿Se ve bien la app del teléfono y se vuelve a Inicio? |
| 9 | Cámara de marcha atrás | ¿Sale la cámara normal al meter la marcha atrás con CarVision delante? |
| 10 | Tema oscuro con las luces | ¿Cambia al encender las luces? (puede no funcionar sin CAN: anotarlo) |
| 11 | 15 minutos de uso normal | ¿Se calienta, se cuelga o se cierra alguna app? |
| 12 | Pendrive con el `.zip` | ¿Detecta el pendrive y lo instala? |

Datos útiles de la propia radio (Ajustes › Acerca de): resolución de pantalla, versión de Android y memoria libre.

## Si la radio deja usar adb (opcional)

Con depuración USB activada (Ajustes › Opciones de desarrollador, código `345543`) y el portátil conectado:

```bash
adb shell cmd notification allow_listener com.carvision.launcher/com.carvision.launcher.media.MediaListenerService
adb shell dumpsys meminfo com.carvision.launcher
adb shell getprop ro.config.low_ram
adb logcat -d > carvision_log.txt
```

La primera línea intenta activar la tarjeta de música (en Android Go Ajustes no lo deja; por adb puede que sí). Las
otras dan la memoria que usa la app, si la radio es «Go» y el registro si algo falla.

## Lo que ya está preparado para esta radio (1.0.1)

- **Modo ligero automático** en radios Android Go o con menos de ~1,6 GB: el coche se dibuja sin MSAA (solo FXAA, que
  gasta mucha menos memoria) y como mucho a ~30 fotogramas por segundo mientras se mueve; quieto no gasta nada.
- **Música en Android Go:** sin el permiso, la tarjeta ya no se queda en «Permitir» sin hacer nada: abre la app de
  música.
- Probado en el emulador con 1,5 GB y el modo ligero activo: arranque, coche, puertas y capó correctos; la app ocupa
  unos 93 MB de memoria (sin contar la memoria gráfica, que el emulador no mide).
