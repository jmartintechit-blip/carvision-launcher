# Vehicle Packs — guía del formato

Un **Vehicle Pack** es la representación visual de un coche para CarVision: un modelo 3D, un archivo de
configuración y, si se quiere, el logotipo de la marca. **No contiene código** ni nada de CAN: las puertas y el resto
de piezas se enlazan con señales normalizadas (por ejemplo `door.front_left`) que llegan de cualquier fuente de datos.

Esta guía describe el formato **versión 1** (`formatVersion: 1`), el que entiende la app actual.

---

## 1. Qué contiene un pack

```
mi_coche/
├── manifest.json        obligatorio — configuración (ver §3)
├── model.glb            obligatorio — modelo 3D en glTF binario (ver §4)
└── brand/
    └── logo.png         opcional — logotipo de la marca (PNG o WebP con transparencia)
```

Tipos de archivo admitidos: `json`, `glb`, `png`, `webp`, `jpg`, `jpeg`, `md`, `txt`. Cualquier otro tipo (código,
ejecutables, archivos de Blender…) hace que el .zip se rechace. Se pueden añadir un `LEEME.md` o un `LICENCIA.txt`.

## 2. Cómo se instala

| Forma | Cuándo |
|---|---|
| **Garaje › Importar .zip** (selector de archivos de Android) | Lo normal: el .zip está en la memoria de la radio o en un USB |
| Carpeta `Android/data/com.carvision.launcher/files/import/` | Radios sin selector de archivos: se copia el .zip ahí y se pulsa «Buscar en la carpeta» |
| Incluido en la app (`vehicle-packs/` del repositorio) | Packs que se distribuyen con CarVision; se empaquetan solos al compilar |
| Coche de prueba (`PackRepository.TEST_PACKS`) | Un pack incluido que se instala como importado en el primer arranque (hoy, el Ford Focus con su marca): se puede borrar |

El .zip puede tener el pack en la raíz o dentro de una única carpeta. Para crearlo sin errores:

```bash
python scripts/make_pack_zip.py vehicle-packs/mi_coche mi_coche.zip
```

(`--id` y `--name` permiten crear variantes, por ejemplo otro color, sin tocar el original.)

**Importación segura.** Antes de instalar nada, la app comprueba que:
- no haya rutas peligrosas (`..`, rutas absolutas) ni tipos de archivo no permitidos;
- no se superen 80 MB en total, 60 MB por archivo ni 400 archivos;
- el `manifest.json` sea válido (§3) y existan el modelo y el logotipo que indica.

Si algo falla, no se instala nada y se muestra el motivo. Un pack con el mismo `id` que otro importado lo sustituye.

## 3. `manifest.json`

Ejemplo completo: [`vehicle-packs/ford_focus_mk3_5p/manifest.json`](../vehicle-packs/ford_focus_mk3_5p/manifest.json).

```json
{
  "formatVersion": 1,
  "id": "seat_ibiza_6j_5p",
  "name": "Seat Ibiza 6J 5 puertas",
  "version": "1.0.0",
  "vehicle": { "make": "Seat", "model": "Ibiza", "generation": "6J (2008–2017)", "bodyStyle": "hatchback", "doors": 5 },
  "model": { "file": "model.glb", "rootNode": "car_root", "forwardYawDeg": 0 },
  "parts": [
    { "id": "front_left_door", "node": "front_left_door", "signal": "door.front_left", "axis": [0, 1, 0], "openAngleDeg": -62, "durationMs": 800 }
  ],
  "wheels": [ { "node": "front_left_wheel", "radiusM": 0.31 } ],
  "camera": { "homeYawDeg": 40, "homePitchDeg": 14, "minPitchDeg": 3, "maxPitchDeg": 55, "fovDeg": 28, "zoom": 1.0 },
  "brand": { "name": "Seat", "logo": "brand/logo.png" },
  "license": {
    "model": "CC-BY-4.0",
    "author": "Autor del modelo",
    "source": "https://…",
    "attribution": "\"Nombre del modelo\" de Autor, CC BY 4.0. Modificado: …",
    "commercialRedistribution": true
  }
}
```

### Campos

| Campo | Obligatorio | Descripción y reglas |
|---|---|---|
| `formatVersion` | sí | Debe ser `1`. |
| `id` | sí | Identificador único: minúsculas, números, `_ . -`; de 3 a 64 caracteres; empieza por letra o número. |
| `name` | sí | Nombre que se ve en el Garaje. |
| `version` | sí | Versión del pack (texto libre, se recomienda `1.0.0`). |
| `vehicle.make` / `model` | sí | Marca y modelo; el nombre que se ve es «Marca Modelo» (o solo la marca si `model` es `""`, como en el coche por defecto «CarVision»). La marca se usa como texto si no hay logotipo. |
| `vehicle.generation` | no | Generación o años. |
| `vehicle.bodyStyle` / `doors` | sí | Carrocería (`hatchback`, `sedan`, `estate`, `suv`…) y número de puertas. |
| `model.file` | sí | Ruta relativa al `.glb` dentro del pack. |
| `model.rootNode` | no | Nodo raíz del coche (por defecto `car_root`). |
| `model.forwardYawDeg` | no | Giro a sumar si el frontal no mira a +Z (por defecto 0). |
| `parts[]` | no | Piezas que se abren (§3.1). |
| `wheels[]` | no | Ruedas que giran al circular (§3.2). |
| `camera` | no | Vista inicial (§3.3). |
| `brand.name` / `brand.logo` | no | Marca y ruta a su logotipo (`.png` o `.webp`). Sin logotipo, la barra lateral muestra la marca en texto. |
| `license` | sí | Licencia del modelo 3D (§5). |

### 3.1 Piezas que se abren (`parts`)

| Campo | Regla |
|---|---|
| `id` | Único dentro del pack. |
| `node` | Nombre del nodo en el GLB. **Su origen debe estar en la bisagra.** |
| `signal` | Una de: `door.front_left`, `door.front_right`, `door.rear_left`, `door.rear_right`, `tailgate`, `hood`. Una señal desconocida no impide instalar, pero la pieza no se moverá. |
| `axis` | Eje de giro `[x, y, z]` en el espacio local del nodo; no puede ser `[0,0,0]`. Puertas: `[0,1,0]`; capó y portón: `[1,0,0]`. |
| `openAngleDeg` | Ángulo de apertura completa, entre -150 y 150. El signo decide el sentido (con frontal +Z: puertas izquierdas negativo, derechas positivo; portón positivo; capó negativo). |
| `durationMs` | Duración de la animación, 100–5000 ms (por defecto 700). |

### 3.2 Ruedas (`wheels`)

| Campo | Regla |
|---|---|
| `node` | Nodo de la rueda. **Su origen debe estar en el centro del buje.** Solo deben colgar de él la llanta, el neumático y los tornillos (no la pinza de freno ni el paso de rueda). |
| `radiusM` | Radio exterior del neumático en metros, 0,15–0,6. |
| `axis` | Eje de giro local (por defecto `[1,0,0]`: con frontal +Z, avanzar = giro positivo sobre X). |

El giro que se ve es decorativo: a partir de unos 23 km/h se mantiene constante para que las ruedas nunca parezcan ir
hacia atrás (ver `KNOWN_LIMITATIONS.md`).

### 3.3 Cámara (`camera`)

| Campo | Por defecto | Uso |
|---|---|---|
| `homeYawDeg` | 35 | Ángulo horizontal de la vista inicial (0 = de frente). |
| `homePitchDeg` | 16 | Altura de la vista inicial en grados. |
| `minPitchDeg` / `maxPitchDeg` | 4 / 55 | Límites al girar con el dedo (`min` ≤ `max`). |
| `fovDeg` | 28 | Campo de visión, 10–90. |
| `zoom` | 1 | Mayor que 1 aleja la cámara, menor que 1 la acerca (el encuadre es automático). |

## 4. El modelo 3D (`model.glb`)

| Requisito | Detalle |
|---|---|
| Formato | glTF 2.0 binario (`.glb`). Se admite compresión **Draco** (recomendada). |
| Unidades y ejes | Metros, tamaño real. Arriba +Y, frontal **+Z**, lado izquierdo **+X** (en Blender: Z arriba, frontal -Y). |
| Suelo | Ruedas apoyadas en y = 0 y coche centrado. |
| Jerarquía | `car_root` → `body` + una pieza por apertura + una por rueda, con los nombres que indica el manifiesto. |
| Piezas móviles | Origen en la bisagra (aperturas) o en el buje (ruedas); rotación y escala aplicadas (identidad). |
| Sombra | Opcional: un plano llamado `contact_shadow` con la sombra horneada. La app lo excluye del encuadre. |
| Tamaño | Orientativo: menos de 300.000 triángulos y menos de 10 MB. Radios modestas agradecen 2–3 MB. |
| Materiales | PBR (metal/rugosidad), pocos. Sin texturas enormes: 2048 px como máximo. |

El pipeline de Blender que lo prepara automáticamente para el Focus MK3 está en
[`docs/BLENDER_PIPELINE.md`](BLENDER_PIPELINE.md). `blender/scripts/validate_glb.py` comprueba nodos y bisagras
contra el manifiesto antes de empaquetar.

## 5. Licencias

Cada pack declara la licencia **del modelo 3D**, que no es la de CarVision:

- `commercialRedistribution: true` solo si la licencia permite redistribuir el archivo en un producto comercial
  (p. ej. CC BY 4.0, CC0). Si es `false`, la app lo indica al importarlo («licencia del modelo: solo uso personal»).
- `attribution` debe ser el texto que exige la licencia, **indicando los cambios** hechos al modelo.
- Los **logotipos de marca** son marcas registradas de sus dueños. Se incluyen para que cada usuario personalice su
  propio coche; quien distribuya packs con logotipos es responsable de tener derecho a hacerlo. El pipeline permite
  quitarlos (`--no-logos`).

## 6. Comprobación rápida antes de publicar un pack

1. `python blender/scripts/validate_glb.py vehicle-packs/mi_coche/manifest.json` sin errores.
2. `python scripts/make_pack_zip.py vehicle-packs/mi_coche mi_coche.zip`.
3. Importarlo en la app (Garaje › Importar .zip) y comprobar en modo demostración:
   cada puerta, el maletero y el capó se abren hacia fuera; las ruedas giran sin bambolearse;
   la vista inicial encuadra el coche entero; el logotipo se ve nítido en la barra lateral.
