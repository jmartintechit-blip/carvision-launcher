# Prompt para una IA con acceso a archivos (Claude Code, etc.)

> **Opción avanzada (D-031).** Para quien tiene el código fuente (incluido en la compra) y quiere preparar su propio
> coche. Sin soporte individual: el camino recomendado es el catálogo o «Te lo hacemos».

Para quien tiene el **código fuente** de CarVision y una IA que puede leer archivos y ejecutar programas en su ordenador.
Abre la IA en la carpeta del proyecto, pega el bloque y cambia la primera línea.
Antes, mira si tu coche está en el **catálogo** del Área de cliente: si está, no hace falta nada de esto (descárgalo e
impórtalo, o usa el prompt para IA gratuita, que te guía). Si no está, también puedes pedirlo con «Te lo hacemos».

---

```text
MI COCHE: [marca, modelo, generación o años, carrocería, nº de puertas, color; y, si ya lo tengo, la ruta del modelo 3D,
del logotipo y el enlace/autor/licencia del modelo]

Estás en el proyecto CarVision (launcher para radios Android de coche). Objetivo: crear el Vehicle Pack de MI COCHE con
el generador del proyecto e importarlo en la app. Antes de nada lee LEEME.md, docs/VEHICLE_PACKS.md,
docs/BLENDER_PIPELINE.md y docs/CREAR_MI_COCHE.md.

REGLAS
- Usa el generador existente: blender/scripts/build_pack.py con un config.json (ejemplo: blender/samples/config_ejemplo.json).
  No escribas scripts nuevos de Blender ni modifiques el código de la app ni el del generador, salvo que yo lo pida.
- Licencia del modelo 3D: solo CC BY o CC0 para compartirlo. Descarta modelos extraídos de juegos (GTA, Forza, Assetto,
  BeamNG, mods, «ripped», «exported from»…) aunque digan CC BY. Comprueba también el perfil del autor (API pública:
  https://api.sketchfab.com/v3/models/<uid> → user.uid → https://api.sketchfab.com/v3/users/<uid>, campos biography y
  modelCount): descarta cuentas que dicen importar modelos de juegos o de otros, o que tienen cientos de coches de
  marcas distintas. Si la licencia es otra, avísame: el pack queda como uso personal.
- No descargues nada sin decirme antes qué archivo, de dónde y cuánto ocupa. Las descargas de Sketchfab requieren mi
  cuenta: si hace falta, te las paso yo.
- No me digas que algo funciona si no lo has comprobado (report.json y renders).

PASOS
1. Modelo 3D: ayúdame a elegirlo (Sketchfab con filtros Downloadable + CC Attribution/CC0; mejor con puertas separadas;
   < ~1 M triángulos; formato glTF/GLB). Logotipo: una imagen de la marca (PNG; si tiene fondo blanco, se quita solo).
2. Escribe un config.json junto al modelo (rutas relativas al config) con: input, output_dir, make, model, generation,
   body_style (hatchback | sedan | estate | suv | coupe), doors, length_m (largo real, búscalo), paint_color (#RRGGBB,
   opcional), logo, license {model, author, source, attribution, commercial}.
3. Ejecuta (Blender 4.2+; en Windows suele estar en C:/Program Files/Blender Foundation/Blender X.Y/blender.exe):
     blender -b --factory-startup --disable-autoexec --python blender/scripts/build_pack.py -- <config.json>
   Código de salida: 0 pack válido, 1 errores en el manifiesto, 2 fallo del generador.
4. Lee <output_dir>/report.json (steps, warnings, orientation, paint_material, members = piezas por grupo con su nombre
   original, hinges, angles, wheel_radius_m) y MIRA los 4 renders de <output_dir>/previews/ (cerrado y abierto).
5. Corrige con el config y repite el paso 3 hasta que esté bien:
   - frontal al revés → "flip_front": true
   - tamaño raro → "length_m"
   - piezas en el grupo equivocado o sin grupo → "assign": {"<grupo>": ["subcadena del nombre", ...]}
     (grupos: body, front_left_door, front_right_door, rear_left_door, rear_right_door, hood, rear_hatch,
     front_left_wheel, front_right_wheel, rear_left_wheel, rear_right_wheel; "body" saca piezas de una apertura)
   - capó que abre hacia delante → "hinges": {"hood": "front"}; trasera con bisagra abajo → {"rear_hatch": "rear"}
   - ángulo de apertura → "angles": {"<grupo>": grados}
   - material de la pintura mal elegido → "paint_material": "<nombre del material>"
   - acabados raros (asientos de color chillón, pinzas pintadas, cromados donde no toca): mira report.json → materials
     (qué acabado ha recibido cada material y en qué piezas) y corrige con "assign" o renombrando en el modelo; con
     "keep_materials": true se conservan los materiales originales tal cual
   Revisa también que no queden logotipos de terceros en texturas (llantas, neumáticos) si el pack se va a compartir.
6. Entrégame el <id>.zip y dime cómo importarlo: pendrive › CarVision › Garaje › «Importar .zip» (o copiarlo a
   Android/data/com.carvision.launcher/files/import y «Buscar en la carpeta»). Si el emulador o la radio están
   conectados por adb y te lo pido, puedes copiarlo con adb push a /sdcard/Download.

Empieza por el paso 1: pregúntame lo que te falte.
```
