# Generador de coches (CarVision Pack Builder) — diseño y estado

Objetivo (D-023): que cualquier comprador ponga el coche que quiera, aunque no sepa de tecnología. La parte técnica la
hace una herramienta propia y probada; la IA (gratuita o con acceso a archivos) solo guía. Resultado: un `.zip` de
Vehicle Pack (modelo 3D, nombre, logotipo en barra lateral y animación de arranque) que se importa sin recompilar.

## Estado (2026-11-12)

| Paso | Estado |
|---|---|
| 1. Paquete `blender/carvision_pack_builder/` y utilidades comunes | ✅ Hecho: `common.py` (antes `blender/scripts/cv_common.py`, que ahora solo reexporta; comprobado en Blender 5.2) |
| 2. Motor: importar, orientar, ruedas, detectar piezas, bisagras | ✅ `engine.py` |
| 3. Motor: materiales, logotipo, exportar, manifiesto, zip, renders, informe | ✅ `engine.py`, `logo.py`, `blender/scripts/build_pack.py` |
| 4. Probar el motor con el Focus original (`vehicle-packs/ford_focus_mk3_5p/source/original/Ford Focus.blend`) | ✅ Sin tocar nada a mano: 4 ruedas (radio 0,331 m, igual que el afinado), 4 puertas, capó (bisagra idéntica a la afinada), portón (panel partido en dos mitades), frontal con confianza alta, logotipo sin fondo; importado en la app con el selector de archivos y recoloreado a azul |
| 5. Probar con 2–3 modelos más | ✅ «Car Concept» de Khronos (piezas con nombre, 2 puertas, ruedas giradas 30°, bisagras corregibles con `hinges`) y un coche de una sola pieza sin nombres, girado y en centímetros (se separa en 754 piezas y todo se encuentra por geometría). El Car Kit de Kenney no se pudo descargar directamente (enlace firmado para el navegador). Modelos de prueba en `blender/samples/test_models/` (no se versionan) |
| 6. Complemento de Blender con panel de 5 pasos | ✅ `ui.py` (barra lateral › CarVision). Flujo completo probado por script en Blender 5.3 (abrir, asignar, probar aperturas, crear pack); se instala desde `dist/carvision_pack_builder.zip` (`scripts/make_addon_zip.py`). ✅ Probado también **con la interfaz real** de Blender 5.3 (clics y rueda simulados con `--enable-event-simulate`, capturas de cada paso). 🔶 Falta una persona de verdad |
| 7. Prompts (IA gratuita / IA con archivos) y guía | ✅ `docs/prompts/PROMPT_IA_GRATUITA.md`, `docs/prompts/PROMPT_IA_CON_ARCHIVOS.md`, `docs/CREAR_MI_COCHE.md`. ✅ Probado en **ChatGPT gratis real** (sin cuenta) y en una simulación completa con dos subagentes (IA + persona sin conocimientos); ver «Pruebas con personas simuladas y ChatGPT» |

## Arquitectura: un motor, dos caminos

```
blender/carvision_pack_builder/
  __init__.py   bl_info + registro del panel (paso 6)
  common.py     utilidades: materiales, unir piezas, sombra de contacto, exportar GLB   ✅
  engine.py     motor completo, sin interfaz (pasos 2–3)
  logo.py       quitar el fondo del logotipo, recortar, 1024 px (numpy, sin PIL)
  ui.py         panel «CarVision» de 5 pasos (paso 6)
blender/scripts/build_pack.py   línea de comandos: blender -b ... --python build_pack.py -- config.json
```

- **IA con archivos (Claude, etc.)**: ejecuta `build_pack.py` con un `config.json`, lee `report.json` y los renders,
  corrige el config (asignaciones, giro del frontal, bisagras) y repite. No escribe scripts nuevos.
- **IA gratuita**: guía a la persona por el panel de 5 botones de Blender; explica los avisos del informe.

### config.json (borrador)

```json
{
  "input": "ruta/al/modelo.glb",
  "output_dir": "salida/seat_leon_mk1",
  "make": "Seat", "model": "León", "generation": "MK1 (1999–2005)",
  "body_style": "hatchback", "doors": 5,
  "length_m": 4.18,
  "paint_color": "#C0C0C0",
  "logo": "ruta/logo.png",
  "flip_front": null,
  "assign": { "front_left_door": ["Door_FL"], "body": ["DoorFrame"] },
  "hinges": { "hood": "rear" },
  "angles": { "hood": -45 },
  "license": { "model": "CC-BY-4.0", "author": "…", "source": "https://…", "attribution": "…", "commercial": true },
  "max_tris": 300000,
  "previews": true
}
```

Salida: `pack/` (manifest.json, model.glb, brand/logo.png), `<id>.zip`, `report.json` (grupos, piezas, bisagras,
radio de rueda, triángulos, tamaño, avisos, confianza de la orientación), `previews/*.png`, `<id>_prepared.blend`.
El `id` sale de marca + modelo + generación (minúsculas, `_`), y debe cumplir el validador de la app.

## Diseño del motor (decidido)

1. **Importar**: .glb/.gltf (`import_scene.gltf`), .fbx (`import_scene.fbx`), .obj (`wm.obj_import`), .blend (append).
   Guardar en cada malla los nombres del objeto y de sus padres (`cv_names`) antes de deshacer la jerarquía.
   Convertir a malla (modificadores), copiar mallas compartidas, hornear transformaciones, borrar vacíos/luces/cámaras.
   **Borrar los datos de variantes glTF** (`gltf2_variant_default_materials`, `gltf2_variant_mesh_data` y las de la
   escena): si no, el exportador restaura materiales antiguos (lección del Car Concept).
   Si el modelo tiene pocas mallas (< 40), separar por partes sueltas (`mesh.separate(type='LOOSE')` en modo edición
   multiobjeto) para encontrar puertas fundidas en un mismo objeto.
2. **Orientar y escalar** (convención del pipeline: Z arriba, frontal −Y, izquierda +X, ruedas en z = 0, centrado):
   largo en Y (girar 90° si el lado largo es X); frontal por pistas de nombre (faros, capó, parabrisas… frente a pilotos,
   maletero, escape), después altura de los extremos y voladizo delantero mayor que el trasero; informar confianza;
   `flip_front` lo fuerza. Escala: `length_m` si viene; si el largo no está entre 2,5 y 6,5 m, probar 0,01 / 0,0254 /
   0,1 / 10 / 100 y quedarse con el que dé ~4,3 m.
3. **Ruedas**: candidatos cerca del suelo, redondos (ext. Y ≈ ext. Z ±25 %), 0,4–1,1 m de diámetro, estrechos en X; uno
   por esquina; buje = centro del mayor; radio = mitad de la altura. Asignar lo que está **en el eje** o es tornillería
   pequeña dentro del disco (regla del Focus); la **pinza de freno y el paso de rueda se quedan en la carrocería**.
   Ruedas delanteras giradas: medir el eje con PCA y enderezarlas (lección del Car Concept, venían a 30°).
4. **Piezas**: el *tipo* sale del nombre (puerta/door/tür/porte; capó/hood/bonnet/capot; maletero/trunk/boot/tailgate/
   hatch/heckklappe/coffre; rueda/wheel/tire/rim…, con cuidado de que «rim» no case con «trim»; pinza/caliper → carrocería)
   y la *posición* (del./tras., izq./der.) siempre de la geometría. Sin pistas de nombre:
   - pintura = material con más superficie en las piezas largas de carrocería (o por nombre: paint/body/carroc/lack);
   - **puertas**: paneles pintados en el costado (|x| > 0,6·semiancho, finos en X, 0,45–1,7 m de largo, entre los pasos
     de rueda); dos por lado = delantera y trasera, uno = solo delantera;
   - **capó**: panel pintado ancho en el 35 % delantero, por arriba; **maletero/portón**: panel pintado ancho en el 30 %
     trasero; en `hatchback` incluye la luneta, en `sedan` no;
   - después, cada pieza cuya caja quede **entera** dentro del volumen de esa apertura (piel ampliada hacia dentro hasta
     ~0,2 m, hacia arriba hasta la ventanilla) pasa al grupo (manillas, cristales, guarnecidos);
   - si la carrocería es una sola pieza, avisar: el coche funciona pero sin aperturas animadas.
   - `assign` del config (por subcadena del nombre) manda sobre todo lo anterior.
5. **Bisagras y ángulos** (coordenadas finales): puertas = borde delantero, cara exterior, media altura de la piel
   (izq. −62°, der. +62° sobre Y glTF); capó = borde trasero superior (−48°; `hinges.hood: front` para capós que
   abren hacia delante); portón/maletero = borde delantero superior (+80° portón, +60° maletero). Ruedas en el buje.
6. **Materiales**: cristal con transmisión → cristal translúcido (alpha), pintura recoloreable con `paint_color`,
   texturas a 1024 px como máximo, borrar materiales sin uso. Si pasa de `max_tris`, reducir (Decimate) salvo ruedas.
7. **Logotipo** (`logo.py`): si la imagen no tiene transparencia, quitar el fondo conectado al borde (blanco o cuadros
   pintados, por similitud con los colores del borde y dilatación iterativa con numpy), recortar, máx. 1024 px, PNG.
   Sin logotipo: la app usa la marca en texto.
8. **Salida**: unir por grupo, origen en bisagra, `car_root`, sombra de contacto (`common.bake_contact_shadow`),
   exportar con Draco (`common.export_pack_glb`), manifest.json con las mismas reglas que el validador de la app,
   zip, renders cerrado/abierto (EEVEE) e informe.

## Lecciones ya aprendidas (no repetir)

- Modelos convertidos (SketchUp, FBX antiguos) traen **varias salidas de material** o sombreadores «Diffuse»: el exportador
  glTF los saca blancos. `normalize_materials()` deja una salida con un Principled BSDF y su color. El Principled que
  estaba desconectado trae restos de la conversión (Metallic 1 en todo): se usa su color, no su acabado.
- Paneles **partidos en dos mitades** (portón del Focus): se buscan pares simétricos izquierda/derecha.
- Reimportar en la app el coche que ya está elegido no lo recargaba: corregido en el Garaje (se recarga a mano).
- La **pintura no es el material con más superficie** (en el Car Concept ganaban las juntas y los limpiaparabrisas):
  se elige por **lo que se ve desde fuera**, lanzando rayos al coche desde 9 direcciones.
- Un nombre de pieza no basta para dar por encontrada una apertura (en el Car Concept solo un guarnecido se llamaba
  «Hatch»): solo cuenta si entre sus piezas hay un panel pintado; si no, se busca también por geometría.
- «Floor» en un nombre puede ser la alfombrilla: un suelo solo se quita si además es plano, ocupa la planta y está abajo.

## Revisión visual del coche automático (2026-11-12)

El cliente comparó el Focus hecho a mano con el mismo coche hecho solo con el generador y encontró fallos. Causas y
arreglos (valen para cualquier coche):

| Fallo visto | Causa | Arreglo en el motor |
|---|---|---|
| Asientos verde chillón | El modelo traía materiales «al objeto» (verde de relleno) y otros «a la malla» (pintura); el generador miraba uno y el exportador otro | Al importar, los materiales del objeto pasan a la malla (`import_model`) |
| Pinzas y discos de freno del color del coche, intermitentes del retrovisor raros | La pintura y los grises de relleno se exportaban tal cual | `classify_materials`: acabado por tipo de pieza (nombre en varios idiomas + sitio: rueda, junto al buje, habitáculo, exterior + color). La pintura junto al buje → pinza gris oscuro; en el habitáculo o en un asiento → interior |
| Llantas «negro espejo» | Metal al 100 % con color negro (relleno de SketchUp) | Paleta del coche hecho a mano: llanta aluminio, neumático, freno, cristal, cromo, plástico, interior, pilotos |
| Antiniebla con tiras brillantes | Grises metálicos de relleno | Gris liso sin tipo → plástico negro; metal negro → plástico |
| Puerta abierta rara: piezas del salpicadero que se movían con ella; por dentro, el revés de la chapa | La puerta se llevaba todo lo que cabía en su caja | Una pieza solo va con una apertura si su chapa la tapa por fuera o la toca (rayos y distancia, en varias pasadas); nunca asientos, salpicadero, chasis, escape ni ruedas. Guarnecido interior automático en cada puerta (`add_door_card`) |
| Trocito rojo flotando al abrir el portón | Fragmentos de los pilotos (en la carrocería) dentro de la caja del portón | La misma regla de contacto |
| (descubierto en la revisión) «detail» contiene «tail» y los discos salían rojos; «headliner» parecía «headlight» | Nombres ambiguos | Expresiones con límite de palabra |
| Puertas traseras que arrastraban el asiento trasero (segunda revisión) | Nombres abreviados («cloth_se01») y el rayo en diagonal (para cristales) valía también por debajo de la ventanilla | El rayo en diagonal solo cuenta por encima de la línea de la ventanilla; «_se01» se reconoce como asiento |

`report.json` incluye ahora `materials`: qué acabado ha recibido cada material del modelo y en qué piezas. Con
`"keep_materials": true` en el config se conservan los materiales originales tal cual.

| Al abrir la puerta, su panel interior se queda dentro del coche (tercera revisión; pasaba en los tres coches) | El forro interior de cada lateral es UNA pieza de casi 3 m (dos puertas, pilar y lateral trasero) con triángulos que cruzan de una puerta a otra | `_carve_door_faces`: se corta la pieza por los bordes de cada puerta y las caras pegadas por dentro a la chapa pasan a una pieza nueva que se va con la puerta. El script del coche hecho a mano usa la misma función |
| Pilotos traseros negros (tercera revisión) | La tapa del piloto se trataba como cristal tintado | Cristal rojo o de piloto en la mitad trasera → lente roja translúcida |
| Tira fina del panel que se quedaba cruzando el hueco al abrir la puerta (cuarta revisión) | El remate superior del panel (base de la ventanilla) queda por encima de la chapa: el rayo hacia fuera no la encontraba | En la franja por encima de la chapa, lo pegado al costado también va con la puerta (remate y marco de la ventanilla) |

Criterio (pedido por el cliente): se arreglan los fallos de las funciones (qué pieza va con qué apertura, bisagras,
piezas que se quedan o flotan, materiales rotos de relleno); el aspecto del modelo descargado (cristales, asientos) no se
retoca.

El coche hecho a mano (y el coche por defecto, que sale del mismo script) tenía además las puertas traseras con la
bisagra en la rueda: las pinzas de freno van pintadas y el script las metía en la puerta trasera y las tomaba como su
chapa. Corregido en `blender/scripts/prepare_focus_mk3.py` y regenerados los dos packs.

Comprobado en un visor 3D (three.js) comparando con el coche hecho a mano, y en la app (emulador): exterior, ruedas,
interior, puertas, capó y portón abiertos. Probados también el Car Concept de Khronos y el coche de una sola pieza.
En el Car Concept (prototipo con portón-cúpula) el portón se abre solo con su panel: no arrastra chasis ni asientos.

## Modelos sacados de juegos (catálogo, 2026-11-13)

Probado con Golf VI, Corsa D, Ibiza 6J, Clio III y León MK2 (BeamNG). Nuevo en `engine.py`:
- `remove_outliers`: quita piezas sueltas a más de un 20 % del tamaño del coche.
- `place_template_wheels`: si no hay ruedas en las esquinas pero sí una en el centro, la copia a las 4 esquinas.
  Primero en los bujes; si no hay, bajo cada paso de rueda (perfil inferior del costado, filtrado para que un faldón con
  pocos vértices no parezca un hueco; el paso tiene que ser ancho). La cara de la llanta mira hacia fuera.
- `settle_on_wheels`: quita la rueda de plantilla sobrante y lo que queda entero bajo el suelo, apoya el coche en sus
  ruedas y lo centra entre ellas.
- `find_wheel_hubs`: solo piezas del tamaño de una rueda (no puertas) y en las esquinas; el suelo se mide con ellas.
- Aperturas: se descartan las que solo tienen cristal (chapa soldada) y las que abarcan más de medio coche.
- Regresión: el Focus da las mismas 4 ruedas y 6 aperturas que antes.

## Pruebas con personas simuladas y ChatGPT (2026-11-12)

- **Interfaz real de Blender**: el panel se dibuja y funciona; se corrigieron textos cortados (barra lateral estrecha),
  el contorno de selección que tapaba los colores y el estado tras crear el pack («1 piezas»). Lecciones para la guía:
  la ventana «Quick Setup» de la primera vez (elegir Español), la pestaña CarVision no es la que se abre por defecto,
  el panel es largo (rueda del ratón).
- **ChatGPT gratis real**: buscó en Sketchfab, propuso candidatos reales del León MK1 y **descartó solo** uno «importado de
  Assetto Corsa» (regla de juegos); fue paso a paso, pidió GLB y avisó de lo que no podía confirmar (procedencia).
  **Corrección (2026-11-12):** el candidato que dio por bueno («SEAT León MK1 1996-2006», de Merc_TV) es de una cuenta
  que dice en su perfil que importa modelos de BeamNG, Assetto Corsa y GTA. La regla ahora obliga a mirar el perfil del
  autor (ver `docs/CATALOGO_COCHES.md`).
- **Prompt único en ChatGPT gratis real (2026-11-12)**: hizo las dos preguntas iniciales, saltó la instalación porque ya
  estaba hecha, ofreció «Te lo hacemos», Blender o el coche por defecto y esperó a que se eligiera. Al buscar el León MK1
  **descartó el de Merc_TV por lo que dice su perfil** y otro por ser la versión Cupra R, dijo que no encontraba uno
  fiable y recordó «Te lo hacemos».
- **Simulación (IA + persona)**: hallazgos corregidos en panel, guía y prompt:
  - avisos del motor con claves del config («flip_front», «length_m») que en el panel no significan nada → el motor tiene
    ahora redacción para el panel (`Report(ui=True)`);
  - dónde está `carvision_pack_builder.zip` (área de cliente, enlace en el correo de compra);
  - «Largo real» ambiguo → «Largo del coche en metros (de parachoques a parachoques)», con punto;
  - glTF o GLB → GLB (un solo archivo), el .glb suele estar en la carpeta `source` del .zip;
  - apertura no encontrada → antes de rendirse, hacer clic sobre ella: si se ilumina sola, asignarla.

## Licencia del complemento

GPL-3.0-or-later, confirmada por el cliente (D-025). Afecta solo a `blender/carvision_pack_builder/`, no a la app.
El texto oficial de la GPL v3 (gnu.org) está en `blender/carvision_pack_builder/LICENSE.txt` y se incluye en el .zip.

## Ejemplo de config

`blender/samples/config_ejemplo.json` (las rutas relativas son relativas al propio config).

- Los modelos gratuitos más populares de coches reales en Sketchfab suelen estar **extraídos de juegos** (mods de GTA,
  Forza) con una licencia CC BY que el que los sube no puede dar. Comprobar siempre la descripción (D-024).
- Algunos modelos traen logotipos dentro de **texturas** (Car Concept: emisión de llantas/pinzas y flanco del neumático),
  no solo como piezas. Revisar texturas y renders de cerca.
- Quitar emblemas puede dejar **huecos**: taparlos con chapa del color de la carrocería (`prepare_focus_mk3.py --no-logos`).
