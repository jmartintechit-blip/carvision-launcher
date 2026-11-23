"""Empaqueta un Vehicle Pack en un .zip listo para importar en CarVision (Garaje › Importar .zip).

Incluye solo lo necesario (manifest.json, el modelo, el logotipo y archivos de datos), nunca la carpeta source/.
Opcionalmente cambia el id y el nombre (útil para crear variantes, p. ej. otro color).

Uso:  python scripts/make_pack_zip.py vehicle-packs/ford_focus_mk3_5p salida.zip [--id nuevo_id] [--name "Nombre"]
"""
import argparse
import json
import os
import zipfile

ALLOWED = {".json", ".glb", ".png", ".webp", ".jpg", ".jpeg", ".md", ".txt"}

ap = argparse.ArgumentParser()
ap.add_argument("pack_dir")
ap.add_argument("out_zip")
ap.add_argument("--id")
ap.add_argument("--name")
a = ap.parse_args()

manifest = json.load(open(os.path.join(a.pack_dir, "manifest.json"), encoding="utf-8"))
if a.id:
    manifest["id"] = a.id
if a.name:
    manifest["name"] = a.name

with zipfile.ZipFile(a.out_zip, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2))
    for root, dirs, files in os.walk(a.pack_dir):
        dirs[:] = [d for d in dirs if d != "source" and not d.startswith(".")]
        for f in files:
            if f == "manifest.json" and root == a.pack_dir:
                continue
            if os.path.splitext(f)[1].lower() not in ALLOWED:
                continue
            full = os.path.join(root, f)
            z.write(full, os.path.relpath(full, a.pack_dir).replace(os.sep, "/"))
print("CV_PACK_ZIP", a.out_zip, manifest["id"], f"{os.path.getsize(a.out_zip) / 1e6:.2f} MB")
