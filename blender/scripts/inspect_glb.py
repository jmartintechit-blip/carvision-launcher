"""Informe de un .glb/.gltf importado en Blender: cajas en coordenadas de Blender (Z arriba), pivotes y materiales.

Uso: blender -b --factory-startup --disable-autoexec --python blender/scripts/inspect_glb.py -- <modelo.glb>
"""
import sys

import bpy
from mathutils import Vector

path = sys.argv[sys.argv.index("--") + 1]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=path)
bpy.context.view_layer.update()


def world_box(objs):
    pts = []
    for o in objs:
        if o.type == 'MESH':
            pts += [o.matrix_world @ Vector(c) for c in o.bound_box]
    if not pts:
        return None
    mn = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    mx = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return mn, mx


def tree(o):
    out = [o]
    for c in o.children:
        out += tree(c)
    return out


def fmt(v):
    return "(" + ", ".join(f"{x:+.3f}" for x in v) + ")"


allm = [o for o in bpy.data.objects if o.type == 'MESH']
mn, mx = world_box(allm)
print("CV_GLOBAL min", fmt(mn), "max", fmt(mx), "size", fmt(mx - mn))
for o in bpy.data.objects:
    if o.parent is None or (o.parent and o.parent.parent is None and len(o.children) > 0) or o.name.startswith(("Wheel", "BodyDoor", "BodyHood", "BodyRear")):
        if o.children or o.name.startswith("Wheel"):
            b = world_box(tree(o))
            if b:
                print(f"CV_NODE {o.name:24s} pivot={fmt(o.matrix_world.translation)} min={fmt(b[0])} max={fmt(b[1])}")
for o in bpy.data.objects:
    if o.name.startswith("Wheel") and o.type == 'MESH':
        b = world_box([o])
        print(f"CV_WHEELPART {o.name:24s} center={fmt((b[0] + b[1]) / 2)} size={fmt(b[1] - b[0])}")
print("CV_MATS", sorted({m.name for m in bpy.data.materials}))
print("CV_IMAGES", [(i.name, tuple(i.size)) for i in bpy.data.images])
