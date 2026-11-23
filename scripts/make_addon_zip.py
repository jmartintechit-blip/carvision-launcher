"""Empaqueta el complemento de Blender «CarVision Pack Builder» en un .zip instalable.

Instalar en Blender (4.2 o posterior): Editar › Preferencias › Complementos › flecha ▾ › Instalar desde disco… › el .zip.
Después: visor 3D › tecla N › pestaña «CarVision».

Uso:  python scripts/make_addon_zip.py [salida.zip]   (por defecto: dist/carvision_pack_builder.zip)
"""
import os
import sys
import zipfile

root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
src = os.path.join(root, "blender", "carvision_pack_builder")
out = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(root, "dist", "carvision_pack_builder.zip")
os.makedirs(os.path.dirname(out), exist_ok=True)

with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for dirpath, dirnames, files in os.walk(src):
        dirnames[:] = [d for d in dirnames if d != "__pycache__"]
        for fn in files:
            if fn.endswith((".py", ".toml")) or fn == "LICENSE.txt":
                full = os.path.join(dirpath, fn)
                z.write(full, os.path.join("carvision_pack_builder", os.path.relpath(full, src)))
print("CV_ADDON_ZIP", out, f"{os.path.getsize(out) / 1024:.0f} KB")
