"""Renders de comprobación de un coche preparado (.blend del pipeline), con aperturas abiertas o cerradas.

Uso:
  blender -b --factory-startup --disable-autoexec <coche_prepared.blend> --python blender/scripts/render_preview.py -- <manifest.json> <carpeta_salida>

Los ángulos y ejes se leen del manifest.json (espacio glTF). Conversión a Blender:
glTF (x, y, z) = Blender (x, z, -y)  ->  eje glTF (ax, ay, az) = eje Blender (ax, -az, ay).
"""
import json
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

manifest_path, out_dir = sys.argv[sys.argv.index("--") + 1:][:2]
os.makedirs(out_dir, exist_ok=True)
manifest = json.load(open(manifest_path, encoding="utf-8"))

scene = bpy.context.scene
scene.render.engine = 'BLENDER_EEVEE' if 'BLENDER_EEVEE' in {e.identifier for e in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items} else 'BLENDER_WORKBENCH'
if scene.render.engine == 'BLENDER_WORKBENCH':
    scene.display.shading.color_type = 'MATERIAL'
scene.render.resolution_x = 1280
scene.render.resolution_y = 760
world = bpy.data.worlds.new("w")
world.use_nodes = True
world.node_tree.nodes["Background"].inputs[0].default_value = (0.35, 0.37, 0.4, 1)
world.node_tree.nodes["Background"].inputs[1].default_value = 1.0
scene.world = world

sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", 'SUN'))
sun.data.energy = 3.0
sun.rotation_euler = (math.radians(50), 0, math.radians(30))
scene.collection.objects.link(sun)

cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
cam.data.lens = 45
scene.collection.objects.link(cam)
scene.camera = cam
rest = {o.name: o.matrix_basis.copy() for o in bpy.data.objects}


def pose(open_all):
    for p in manifest["parts"]:
        o = bpy.data.objects.get(p["node"])
        if o is None:
            print("CV_MISSING", p["node"])
            continue
        ax, ay, az = p["axis"]
        axis = Vector((ax, -az, ay))
        ang = math.radians(p["openAngleDeg"]) if open_all else 0.0
        o.matrix_basis = rest[o.name] @ Matrix.Rotation(ang, 4, axis)


def shoot(name, az_deg, el_deg, dist=7.5, target=(0, 0, 0.65)):
    a, e = math.radians(az_deg), math.radians(el_deg)
    t = Vector(target)
    # Frontal en -Y, izquierda en +X. az=0 -> mirando el frontal.
    cam.location = t + Vector((math.sin(a) * math.cos(e), -math.cos(a) * math.cos(e), math.sin(e))) * dist
    cam.rotation_euler = (t - cam.location).to_track_quat('-Z', 'Y').to_euler()
    scene.render.filepath = os.path.join(out_dir, name + ".png")
    bpy.ops.render.render(write_still=True)
    print("CV_RENDER", scene.render.filepath)


pose(False)
shoot("cerrado_frente_izq", 40, 15)
shoot("cerrado_trasera_dcha", 215, 15)
pose(True)
shoot("abierto_frente_izq", 55, 22)
shoot("abierto_trasera_dcha", 215, 25)
shoot("abierto_arriba", 0, 89, dist=9)
