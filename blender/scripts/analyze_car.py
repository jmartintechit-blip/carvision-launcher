"""Análisis de un coche importado: caja envolvente global, piezas de carrocería y renders de diagnóstico.

Uso:
  blender -b --factory-startup --disable-autoexec <coche.blend> --python blender/scripts/analyze_car.py -- <carpeta_salida>
"""
import math
import os
import random
import sys

import bpy
from mathutils import Vector

out_dir = sys.argv[sys.argv.index("--") + 1]
os.makedirs(out_dir, exist_ok=True)

meshes = [o for o in bpy.data.objects if o.type == 'MESH']


def world_bbox(o):
    pts = [o.matrix_world @ Vector(c) for c in o.bound_box]
    mn = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    mx = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return mn, mx


boxes = {o.name: world_bbox(o) for o in meshes}
gmn = Vector((min(b[0].x for b in boxes.values()), min(b[0].y for b in boxes.values()), min(b[0].z for b in boxes.values())))
gmx = Vector((max(b[1].x for b in boxes.values()), max(b[1].y for b in boxes.values()), max(b[1].z for b in boxes.values())))
size = gmx - gmn
print("CV_GLOBAL min", tuple(round(v, 3) for v in gmn), "max", tuple(round(v, 3) for v in gmx), "size", tuple(round(v, 3) for v in size))

print("CV_BODY objetos con material body1 (caja de mundo):")
for o in meshes:
    if any(m and m.name == "body1" for m in o.data.materials):
        mn, mx = boxes[o.name]
        tris = sum(len(p.vertices) - 2 for p in o.data.polygons)
        print(f"  {o.name[:45]:45s} tris={tris:5d} min=({mn.x:+.2f},{mn.y:+.2f},{mn.z:+.2f}) max=({mx.x:+.2f},{mx.y:+.2f},{mx.z:+.2f})")

print("CV_EMBLEM objetos con material emblem1:")
for o in meshes:
    if any(m and m.name in ("emblem1", "Color_I16") for m in o.data.materials):
        mn, mx = boxes[o.name]
        print(f"  {o.name[:45]:45s} min=({mn.x:+.2f},{mn.y:+.2f},{mn.z:+.2f}) max=({mx.x:+.2f},{mx.y:+.2f},{mx.z:+.2f})")

# ---- Renders de diagnóstico: cada objeto con un color aleatorio (Workbench).
scene = bpy.context.scene
scene.render.engine = 'BLENDER_WORKBENCH'
scene.display.shading.color_type = 'RANDOM'
scene.display.shading.light = 'STUDIO'
scene.render.resolution_x = 1200
scene.render.resolution_y = 800
scene.render.film_transparent = False

center = (gmn + gmx) / 2
radius = size.length / 2
cam_data = bpy.data.cameras.new("diag_cam")
cam_data.lens = 50
cam = bpy.data.objects.new("diag_cam", cam_data)
scene.collection.objects.link(cam)
scene.camera = cam


def shoot(name, direction, ortho=False):
    d = Vector(direction).normalized()
    cam.location = center + d * radius * 3.2
    look = (center - cam.location).normalized()
    cam.rotation_euler = look.to_track_quat('-Z', 'Z' if abs(look.z) < 0.99 else 'Y').to_euler()
    cam_data.type = 'ORTHO' if ortho else 'PERSP'
    cam_data.ortho_scale = max(size.x, size.y, size.z) * 1.1
    scene.render.filepath = os.path.join(out_dir, f"{name}.png")
    bpy.ops.render.render(write_still=True)
    print("CV_RENDER", scene.render.filepath)


# El eje largo del coche se determina por la caja global.
long_axis = max(range(3), key=lambda i: size[i])
print("CV_LONG_AXIS", "XYZ"[long_axis])
axes = [Vector((1, 0, 0)), Vector((0, 1, 0)), Vector((0, 0, 1))]
up = axes[max(range(3), key=lambda i: -999 if i == long_axis else (1 if i == 2 else 0))]
side_axis = [i for i in range(3) if i != long_axis and axes[i] != up][0]
L = axes[long_axis]
S = axes[side_axis]
U = up
shoot("01_lado_A", S, ortho=True)
shoot("02_lado_B", -S, ortho=True)
shoot("03_frente_A", L * 1.0 + S * 0.8 + U * 0.5)
shoot("04_frente_B", -L * 1.0 + S * 0.8 + U * 0.5)
shoot("05_arriba", U + L * 0.001, ortho=True)
print("CV_DONE")
