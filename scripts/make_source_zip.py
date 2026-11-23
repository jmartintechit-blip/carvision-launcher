"""Crea el .zip del código fuente que se entrega con la compra (D-023, D-031).

Lleva la app Android, el generador de coches (Blender) y los coches incluidos y del catálogo, con su documentación
técnica y los textos para IA. NO lleva material interno: análisis de la competencia, notas de negocio y decisiones,
datos de las radios del cliente, la web de venta, modelos originales descargados, compilaciones ni claves de firma.

Uso: python scripts/make_source_zip.py [salida.zip]   (por defecto: dist/CarVision_codigo_fuente.zip)
"""
import fnmatch
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(ROOT, "dist", "CarVision_codigo_fuente.zip")

INCLUDE_DIRS = ["android", "blender", "scripts", "vehicle-packs/carvision_default", "vehicle-packs/ford_focus_mk3_5p",
                "vehicle-packs/vw_golf_6_5p", "docs/prompts", "docs/ejemplos", "docs/third-party"]
INCLUDE_FILES = ["docs/BLENDER_PIPELINE.md", "docs/CREAR_MI_COCHE.md", "docs/GUIA_DESARROLLADOR.md",
                 "docs/INSTALACION.md", "docs/VEHICLE_PACKS.md"]
EXCLUDE = [
    "*/build/*", "*/.gradle/*", "*/.idea/*", "*/.kotlin/*", "*/__pycache__/*", "*.pyc",
    "*/local.properties", "*/keystore.properties", "*.jks", "*.keystore", "*.apk", "*.aab",
    "*.blend1", "*/source/work/*", "*/source/original/*", "*.zip",
    "scripts/make_source_zip.py", "*/samples/test_models/*",
]

LEEME = """# CarVision · código fuente

Gracias por comprar CarVision. Aquí tienes el proyecto de la app y el generador de coches.

- `android/` — la app (Kotlin, Jetpack Compose, Filament). Se abre con Android Studio. Guía: `docs/GUIA_DESARROLLADOR.md`.
- `blender/` — el generador de coches: motor (`carvision_pack_builder/`) y línea de comandos
  (`scripts/build_pack.py -- config.json`). Guía: `docs/BLENDER_PIPELINE.md` y `docs/CREAR_MI_COCHE.md`.
- `vehicle-packs/` — el coche CarVision, el Ford Focus MK3 y el Volkswagen Golf VI. En cada `source/` está su
  `config.json` y su script de acabado (`finish_*.py`), como ejemplo de cómo se termina un coche a mano.
- `docs/prompts/PROMPT_IA_CON_ARCHIVOS.md` — el texto para una IA con acceso a archivos (p. ej. Claude Code) que
  prepara tu coche con el generador. `docs/VEHICLE_PACKS.md` explica el formato de los coches.

Licencias: la app y el generador son de CarVision (el complemento de Blender, `blender/carvision_pack_builder/`, es
GPL-3.0-or-later). Cada coche cita su modelo 3D y su autor en `manifest.json` (licencia CC BY 4.0); los logotipos de
marca pertenecen a sus dueños y solo identifican el coche. Para compilar una versión firmada necesitas tu propia clave.
"""


def excluded(rel: str) -> bool:
    r = "/" + rel.replace("\\", "/")
    return any(fnmatch.fnmatch(r, "*/" + p.lstrip("*/")) or fnmatch.fnmatch(rel, p) or fnmatch.fnmatch(r, p) for p in EXCLUDE)


def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n = 0
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("CarVision/LEEME.md", LEEME)
        for d in INCLUDE_DIRS:
            for base, dirs, files in os.walk(os.path.join(ROOT, d)):
                for fn in files:
                    full = os.path.join(base, fn)
                    rel = os.path.relpath(full, ROOT).replace("\\", "/")
                    if excluded(rel):
                        continue
                    z.write(full, "CarVision/" + rel)
                    n += 1
        for f in INCLUDE_FILES:
            z.write(os.path.join(ROOT, f), "CarVision/" + f)
            n += 1
    print(f"CV_SOURCE_ZIP {OUT} {n} archivos {os.path.getsize(OUT) / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
