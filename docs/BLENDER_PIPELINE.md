# Pipeline de Blender → Vehicle Pack

Requisitos: Blender 4.x/5.x (probado con 5.2) y Python 3 (solo para el validador).

## Convenciones

| Elemento | Convención |
|---|---|
| Unidades | Metros, largo real del coche |
| Ejes (Blender) | Z arriba, frontal hacia **-Y**, lado izquierdo hacia **+X** (en glTF: frontal +Z, izquierda +X) |
| Suelo | Ruedas apoyadas en z = 0, coche centrado en X/Y |
| Jerarquía | `car_root` → `body`, `front_left_door`, `front_right_door`, `rear_left_door`, `rear_right_door`, `rear_hatch`, `hood`, `front_left_wheel`, `front_right_wheel`, `rear_left_wheel`, `rear_right_wheel` |
| Piezas móviles | Origen en la bisagra, rotación y escala del objeto = identidad |
| Materiales | Pocos (≈12) y PBR; el de la carrocería se llama `paint` |

## Scripts (`blender/scripts/`)

| Script | Para qué |
|---|---|
| `inspect_blend.py` | Informe de un .blend: objetos, materiales, texturas, triángulos |
| `analyze_car.py` | Caja global, piezas pintadas, logotipos y renders de diagnóstico por piezas |
| `prepare_focus_mk3.py` | Prepara el Focus MK3: hornea, clasifica piezas, une, escala, bisagras, guarnecidos, materiales, exporta con Draco (emblemas incluidos) |
| `render_preview.py` | Renders de comprobación con aperturas cerradas y abiertas (ángulos del manifest.json) |
| `extract_oval_logo.py` | Recorta un logotipo ovalado de una imagen sin transparencia real (fondo blanco o cuadros pintados) |
| `render_brand_logo.py` | Alternativa: renderiza el emblema 3D del modelo (menos calidad; solo si no hay logotipo oficial) |
| `list_near.py` | Diagnóstico: piezas cercanas a un punto (útil para revisar qué entra en cada rueda) |
| `validate_glb.py` | Comprueba nodos y bisagras del GLB contra el manifest.json |
| `inspect_glb.py` | Informe de un .glb de terceros (cajas, pivotes, materiales, texturas) antes de prepararlo |
| `make_demo_hatchback.py` | Coche de prueba esquemático (muestra de desarrollo en `blender/samples/`, no se incluye en la app) |
| `make_stage.py` | Plataforma con anillo de luz de la interfaz |

Los .blend de terceros se abren siempre con `--disable-autoexec` (no se ejecuta código incrustado).

## Ejemplo: Ford Focus MK3

```bash
blender -b --factory-startup --disable-autoexec "vehicle-packs/ford_focus_mk3_5p/source/original/Ford Focus.blend" \
  --python blender/scripts/prepare_focus_mk3.py -- vehicle-packs/ford_focus_mk3_5p/source/work/model.glb D9480F
blender -b --factory-startup --disable-autoexec vehicle-packs/ford_focus_mk3_5p/source/work/model_prepared.blend \
  --python blender/scripts/render_preview.py -- vehicle-packs/ford_focus_mk3_5p/manifest.json <carpeta_renders>
cp vehicle-packs/ford_focus_mk3_5p/source/work/model.glb vehicle-packs/ford_focus_mk3_5p/model.glb
python blender/scripts/validate_glb.py vehicle-packs/ford_focus_mk3_5p/manifest.json
```

`prepare_focus_mk3.py` usa también `engine._carve_door_faces` del generador para separar el panel interior de cada
puerta del forro lateral (pieza única en este modelo).

Argumentos de `prepare_focus_mk3.py`: color de pintura en hexadecimal; `--no-logos` quita emblemas, sus marcos e
insignias (p. ej. «ST») y tapa los huecos con chapa del color de la carrocería; `--no-draco` exporta sin comprimir.

El **coche por defecto** (`carvision_default`) es el mismo Focus preparado con `--no-logos`:

```bash
blender -b --factory-startup --disable-autoexec "vehicle-packs/ford_focus_mk3_5p/source/original/Ford Focus.blend" \
  --python blender/scripts/prepare_focus_mk3.py -- vehicle-packs/carvision_default/source/work/model.glb --no-logos
cp vehicle-packs/carvision_default/source/work/model.glb vehicle-packs/carvision_default/model.glb
python blender/scripts/validate_glb.py vehicle-packs/carvision_default/manifest.json
```

### Logotipo de la marca (animación de arranque)

Debe ser el **logotipo oficial en plano y en alta resolución** (≥ 700 px de ancho), en PNG o WebP con transparencia,
en `brand/logo.png`, y declarado en el manifiesto (`"brand": {"name": "Ford", "logo": "brand/logo.png"}`).

- Si ya tiene transparencia real: copiarlo tal cual.
- Si el fondo es blanco o lleva los "cuadros de transparencia" pintados (muy habitual en webs de logotipos)
  y el logotipo es ovalado (Ford, Hyundai, Toyota…):

```bash
python blender/scripts/extract_oval_logo.py vehicle-packs/ford_focus_mk3_5p/source/brand/ford_logo_original.png \
  vehicle-packs/ford_focus_mk3_5p/brand/logo.png
```

- Para logotipos con otra forma, recortarlo en un editor (GIMP, Photoshop, remove.bg) y exportar PNG con transparencia.
- `render_brand_logo.py` solo como último recurso: renderiza el emblema 3D del modelo y se nota de menor calidad.

## Empaquetar e instalar un coche

```bash
python scripts/make_pack_zip.py vehicle-packs/ford_focus_mk3_5p ford_focus.zip
```

En la radio: botón **Garaje** (cabecera del coche) › **Importar .zip**, o copiar el .zip a
`Android/data/com.carvision.launcher/files/import/` y pulsar **Buscar en la carpeta**. No hace falta recompilar.

## Adaptar otro coche

1. Comprobar la licencia del modelo (CC-BY, CC0 o licencia comercial con redistribución).
2. `inspect_blend.py` y `analyze_car.py` para ver cómo viene dividido.
3. Copiar `prepare_focus_mk3.py` y ajustar las regiones de clasificación (zona de cada puerta, portón y capó)
   y la tabla de materiales. **El script no crea un coche desde cero**: necesita un modelo con las puertas como piezas
   separadas, o separarlas antes a mano en Blender.
4. `render_preview.py` hasta que las aperturas giren bien.
5. Crear `manifest.json` (ejes, ángulos, cámara, licencia) y validar con `validate_glb.py`.
