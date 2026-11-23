# Limitaciones conocidas

Última actualización: 2026-11-11

## Hardware y plataforma
- **3D requiere OpenGL ES 3.0.** Sin él, la app muestra una vista en planta 2D (probado en emulador con ES 2.0).
  Qué radios reales están en ese caso: ❓ sin verificar.
- **Emulador:** las imágenes Android 8.1 arrancan por defecto con OpenGL ES 2.0. Para ver el 3D hay que lanzar el
  emulador con `-feature GLESDynamicVersion`.
- **Desenfoque real (glassmorphism) solo en Android 12+.** Se usan capas translúcidas sin desenfoque en todas las versiones.
- **Launcher predeterminado:** algunos firmwares de radio pueden relanzar su launcher original al arrancar. 🔶 Sin verificar.
- **APK final:** 13,4 MB (arm64-v8a) y 11,8 MB (armeabi-v7a). Solo se ha ejecutado la de emulador (x86); las de
  ARM, que son las de las radios, están compiladas pero sin probar en un aparato real.
- Solo probado en emulador (1024×600 y 1280×720, Android 8.1). Sin pruebas en radio real.

## Diseño
- La pantalla completa oculta las barras de Android, pero algunas radios superponen su propia barra o botones de
  fabricante por encima de cualquier app. 🔶 Verificar en cada radio.
- Los iconos de apps antiguas (no adaptativos) se muestran sobre un fondo squircle y no a sangre completa.

- Quitar el fondo de un logotipo es automático solo si el fondo es claro y uniforme (blanco o cuadros). Para logotipos
  sobre fondos oscuros o fotografías, conviene usar un PNG con transparencia.

## Datos del vehículo
- No existe todavía ningún CAN Adapter. Sin modo demostración, los datos se muestran como **No disponible**.
- OBD-II estándar no proporciona el estado individual de puertas ni del portón.

- **Ordenador de a bordo:** un viaje por arranque del launcher (o al pulsar «Nuevo viaje»). Si la radio no reinicia
  Android al quitar el contacto (suspensión), dos trayectos pueden quedar en un mismo viaje hasta tener la señal de
  contacto por CAN. El consumo medio no se calcula hasta disponer del consumo instantáneo por CAN.
- **Aviso de apertura en marcha:** el banner solo se ve dentro de CarVision; con otra app delante (p. ej. el
  navegador) suenan el tono y la voz, pero no se dibuja nada encima de esa app (no se pide el permiso de
  superposición). No es una función de seguridad certificada.
- **Tono y voz de los avisos:** sin oír todavía (el emulador se usa sin audio). La voz depende de que la radio tenga
  un motor de voz con español; si no lo tiene, no se dice nada.

## Multimedia
- Ver lo que suena requiere conceder **acceso a notificaciones** (algunas radios ocultan ese ajuste; se puede
  conceder por adb). Sin probar todavía con una app reproduciendo.
- La radio FM/AM del fabricante solo aparecerá si publica una sesión multimedia estándar (depende de cada radio).

## 3D y recursos gráficos
- **Coche por defecto «CarVision»:** es el Focus MK3 sin emblemas. La silueta sigue siendo reconocible como un Focus,
  y donde iban los emblemas quedan dos huecos ovalados lisos del color de la carrocería (apenas visibles en la app).
- **Ford Focus MK3:** los guarnecidos interiores de puerta son una aproximación generada (copia del panel exterior),
  no un modelado real. Las pinzas de freno giran con la rueda. 18 materiales sin nombre útil del original se han
  convertido en plástico negro.
- **Animación de arranque:** aparece al arrancar el proceso del launcher. Si la radio no reinicia Android al
  apagarse (suspensión), no se verá en cada encendido hasta tener la señal de contacto por CAN.
- **Giro de las ruedas:** es decorativo. Por encima de unos 23 km/h gira siempre igual (≈1,7 vueltas/s) para que
  nunca parezca ir hacia atrás; no es proporcional a la velocidad real.
- La iluminación usa el entorno `default_env` del ejemplo de Filament (Apache 2.0). Pendiente de sustituir por un
  entorno propio generado desde un HDRI CC0.

## Generador de coches
- Detecta puertas, capó y portón por nombre o por geometría; con modelos raros puede equivocarse: por eso el panel
  permite corregir y «Probar aperturas». Si la carrocería es una sola pieza soldada, no hay aperturas animadas.
- El panel de Blender está probado con su interfaz real (clics simulados) y el prompt en ChatGPT gratis y en una
  simulación con una persona sin conocimientos; falta que lo use una persona real.
- No detecta logotipos de terceros dentro de texturas (p. ej. en neumáticos): hay que revisarlo si el pack se comparte.
- Los acabados automáticos se basan en nombres (varios idiomas), posición y color. Un modelo con nombres engañosos puede
  necesitar corrección («assign» o renombrar). Las pinzas pintadas pasan a gris oscuro aunque el coche real las lleve
  de color.
- Modelos sacados de juegos: si traen la rueda una sola vez (plantilla), se copia a las esquinas; si la plantilla
  es solo la llanta (sin neumático), el coche sale sin neumáticos: ese modelo no vale para el catálogo. Si las puertas
  vienen soldadas a la carrocería (solo el cristal suelto), se quedan cerradas en vez de abrir un cristal flotando.
- Coches con aperturas poco habituales (portón-cúpula, puertas de tijera) pueden abrirse solo con su panel.

## Instalación (v0.10.0)
- El pendrive automático está probado en el emulador Android 8.1 con un disco USB virtual. En Android 10 o más usa otra
  pantalla del sistema (elegir carpeta) que no se ha probado; en radios reales, tampoco.
- Cambiar el color solo funciona en packs con material «paint» (los del generador y los Focus). Si la pintura del
  modelo es una textura, el color se mezcla con ella.
- El botón «Poner como inicio» abre la pantalla de Android de apps predeterminadas en Android 9 o menos (el cliente
  elige CarVision allí); en Android 10 o más sale la pregunta del sistema directamente (sin probar).

## Catálogo de coches (D-026)
- **v1: catálogo de Focus MK3 y Golf VI** (D-030, decidido por el cliente); el Corsa D llegará en una actualización: espera otro modelo base (el actual trae la carrocería
  abollada). El Golf tiene 233 000 triángulos (el Focus, 89 000): falta comprobar la fluidez en una radio real. Los demás coches se piden con «Te lo hacemos» o se usa
  el coche por defecto con color y logotipo propios.
- Hay **pocos modelos gratuitos válidos** de coches europeos comunes: la mayoría de los coches «CC BY» de Sketchfab los
  suben cuentas que los sacan de juegos o de otros autores. En la búsqueda de 46 coches solo quedaron unos 10–14
  candidatos razonables; faltan varios de los más vendidos (Ibiza, León, Clio, Golf, 208…). Ver `docs/CATALOGO_COCHES.md`.
- La procedencia de un modelo gratuito **no se puede verificar al 100%**, tampoco la del Focus que usa el coche por
  defecto (su autor no da indicios de importar de juegos, pero no está comprobado).
- El servicio «Te lo hacemos» y el Área de cliente aún no existen; el prompt ya los menciona.

## Legal y licencias
- Los modelos 3D con CC-BY exigen **atribución**: se muestra en Ajustes y se documentará en el producto.
- Los packs incluyen por defecto los emblemas de la marca (decisión D-013). Son marcas registradas de sus titulares;
  para material promocional público conviene usar `--no-logos`.
