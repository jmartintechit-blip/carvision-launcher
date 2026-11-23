"""Generador de coches de CarVision desde la línea de comandos (para una IA con acceso a archivos, o para scripts).

Uso:
  blender -b --factory-startup --disable-autoexec --python blender/scripts/build_pack.py -- <config.json>

El config describe el modelo de entrada, la marca, el color, el logotipo, la licencia y, si hace falta, correcciones
(asignar piezas, girar el frontal, bisagras). Formato y ejemplo: docs/GENERADOR_COCHES.md.

Salida en output_dir: pack/ (manifest.json, model.glb, brand/logo.png), <id>.zip para importar en la app,
report.json (qué se ha detectado y avisos), previews/*.png (renders cerrado/abierto) y <id>_prepared.blend.
Código de salida 0 si el pack es válido, 1 si hay errores en el manifiesto, 2 si el generador falla.
"""
import json
import os
import sys
import traceback

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from carvision_pack_builder import engine  # noqa: E402

args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
if not args:
    print("Uso: blender -b --factory-startup --disable-autoexec --python build_pack.py -- <config.json>")
    sys.exit(2)

cfg_path = os.path.abspath(args[0])
with open(cfg_path, encoding="utf-8") as f:
    cfg = json.load(f)
# Las rutas relativas del config son relativas al propio config
base = os.path.dirname(cfg_path)
for key in ("input", "output_dir", "logo"):
    if cfg.get(key) and not os.path.isabs(cfg[key]):
        cfg[key] = os.path.normpath(os.path.join(base, cfg[key]))

try:
    report = engine.build(cfg)
except Exception:  # noqa: BLE001
    traceback.print_exc()
    print("CV_FAILED")
    sys.exit(2)
print("CV_REPORT", json.dumps({k: report[k] for k in ("id", "zip", "ok", "warnings") if k in report}, ensure_ascii=False))
sys.exit(0 if report.get("ok") else 1)
