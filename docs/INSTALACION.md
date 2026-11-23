# Instalar CarVision en la radio

Guía para instalar CarVision en una radio Android de coche (las llamadas «pantallas Android» o «radios chinas»).
Tiempo: unos 10 minutos. No hace falta root ni tocar el firmware de la radio.

> **Estado:** probado en emulador (Android 8.1, pantallas 1024×600 y 1280×720). Todavía **no se ha probado en una
> radio real**: si algo de esta guía no coincide con tu radio, anótalo para mejorarla.

## 1. Requisitos

| Requisito | Detalle |
|---|---|
| Android | 8.0 o superior |
| Pantalla | Pensado para 1024×600 y 1280×720 (horizontal). Otras resoluciones se escalan solas. |
| Gráficos | OpenGL ES 3.0 para el coche en 3D. Si la radio no lo tiene, se muestra una vista en planta 2D. |
| Permisos | Ninguno al instalar. Solo uno opcional después: acceso a notificaciones, para ver la música (§5). |
| Datos del coche | Opcional. Sin adaptador CAN, CarVision funciona igual y muestra «No disponible» (o datos de demostración, siempre marcados como tales). |

## 2. Elegir la APK

Hay **un solo archivo**, `CarVision.apk` (≈17 MB), que vale para todas las radios (64 y 32 bits). No hay que elegir.

## 3. Instalar

1. Copia la APK a un **pendrive USB** (o descárgala en la radio).
2. En la radio, abre el **explorador de archivos** (suele llamarse «Archivos», «File Manager» o «Explorador»).
3. Abre la APK. Si la radio lo pide, permite **instalar apps de origen desconocido** para el explorador
   (el nombre exacto del ajuste cambia según la radio).
4. Pulsa **Instalar** y, al terminar, **Abrir**.

Algunas radios piden una contraseña para entrar en los ajustes avanzados (a menudo aparece en el manual o en la
caja). CarVision no la necesita; solo puede hacer falta para permitir orígenes desconocidos.

## 4. Poner CarVision como pantalla de inicio

Al abrir CarVision por primera vez sale el **asistente** («Bienvenido a CarVision», 3 pasos: pantalla de inicio, tu
coche y su color, música). Cada paso se marca solo cuando está hecho y todos se pueden saltar.

1. En el asistente, **Poner como inicio** › elige **CarVision** (y **Siempre**, si lo pregunta) › vuelve con Atrás.
2. Sin asistente: pulsa el botón **Inicio** (Home) de la radio › **CarVision** › **Siempre**.
3. El asistente se puede volver a abrir en **Ajustes › Sistema › Asistente de inicio**.

**Tu coche desde un pendrive:** con CarVision abierto, pincha el pendrive con el `.zip` de tu coche. Arriba sale
«Pendrive conectado» › **Buscar mi coche** › (la primera vez) **Permitir** › **Instalar**.

Para volver al launcher original: el mismo ajuste de Android (o **Ajustes › Sistema › Volver al launcher original** en
CarVision) y elige el launcher de la radio.

> Algunos firmwares vuelven a poner su launcher al arrancar. Si te pasa, anótalo: es una limitación conocida pendiente
> de comprobar en radios reales (ver `KNOWN_LIMITATIONS.md`).

## 5. Ver la música que suena (opcional)

Para que la tarjeta de música muestre la carátula, el título y el artista:

1. **Ajustes › Sistema › Música en reproducción** abre el ajuste de Android **Acceso a notificaciones**.
2. Activa **CarVision**.

Si la radio oculta ese ajuste, se puede activar desde un ordenador con `adb` (depuración USB activada en la radio):

```bash
adb shell cmd notification allow_listener com.carvision.launcher/com.carvision.launcher.media.MediaListenerService
```

CarVision solo lee qué se está reproduciendo; no lee ni guarda el contenido de las notificaciones.

## 6. Primeros ajustes recomendados

| Dónde | Qué |
|---|---|
| Garaje (icono del coche, arriba) | Elegir el coche o importar uno nuevo (.zip). Ver `docs/VEHICLE_PACKS.md`. |
| Ambiente (icono de la paleta) | Fondo de pantalla. |
| Ajustes › Pantalla | Tema (automático por luces del coche o por hora), tamaño de la interfaz, animación de arranque. |
| Ajustes › Avisos | Aviso de puerta abierta en marcha, sonido y voz. |
| Ajustes › Vehículo › Datos del coche | «Demostración» para ver la interfaz con datos simulados (marcados como DEMO). |
| Servicio | Carnet de mantenimiento: introducir el kilometraje y las últimas revisiones. |

## 7. Actualizar o desinstalar

- **Actualizar:** instala la APK nueva encima. Se conservan ajustes, carnet de mantenimiento, viajes y coches importados.
- **Desinstalar:** primero vuelve al launcher original (§4) y luego desinstala CarVision desde los ajustes de Android.
  Al desinstalar se borran sus datos: exporta antes el carnet a PDF si lo quieres conservar.

## 8. Problemas frecuentes

| Problema | Solución |
|---|---|
| «App no instalada» | Usa la APK `armeabi-v7a`. Si ya tenías otra versión firmada con otra clave, desinstálala primero. |
| No se ve el coche 3D, solo una vista en planta | La radio no tiene OpenGL ES 3.0. Todo lo demás funciona igual. |
| La música sale como «Sin reproducción» | Falta el acceso a notificaciones (§5), o la app de música no publica lo que suena. |
| Al pulsar Inicio vuelve el launcher original | Repite §4 eligiendo «Siempre». |
| Los datos salen «No disponible» | Normal sin adaptador CAN. CarVision nunca inventa datos: solo muestra los del coche o los de la demostración, marcados. |
