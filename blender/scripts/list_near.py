"""Diagnóstico: lista las piezas cercanas a un punto (coordenadas originales del modelo, transformaciones horneadas).

Uso: blender -b --factory-startup --disable-autoexec <modelo.blend> --python blender/scripts/list_near.py -- x y z radio
"""
import sys

import bpy
from mathutils import Vector

x, y, z, r = (float(v) for v in sys.argv[sys.argv.index("--") + 1:][:4])
p0 = Vector((x, y, z))
rows = []
for o in bpy.data.objects:
    if o.type != 'MESH' or not o.data.vertices:
        continue
    pts = [o.matrix_world @ v.co for v in o.data.vertices]
    mn = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    mx = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    c = (mn + mx) / 2
    if (c - p0).length < r:
        e = mx - mn
        mats = ",".join(m.name for m in o.data.materials if m)
        rows.append((c - p0, o.name, c, e, mats, len(o.data.polygons)))
rows.sort(key=lambda t: t[0].length)
for d, name, c, e, mats, n in rows:
    print(f"CV_NEAR d=({d.x:+.2f},{d.y:+.2f},{d.z:+.2f}) ext=({e.x:.2f},{e.y:.2f},{e.z:.2f}) polys={n:5d} {name[:48]:48s} [{mats}]")
