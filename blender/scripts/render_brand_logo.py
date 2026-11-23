"""Renderiza el emblema de la marca desde el propio modelo 3D a un PNG transparente (logo del pack).

Busca los objetos que usan los materiales del emblema, toma el que tenga la cara frontal más grande
(normalmente el del portón) y lo renderiza de frente con cámara ortográfica y fondo transparente.

Uso:
  blender -b --factory-startup --disable-autoexec <original.blend> --python blender/scripts/render_brand_logo.py -- <salida.png> [nombre_objeto]
"""
import math
import os
import sys

import bpy
from mathutils import Vector

args = sys.argv[sys.argv.index("--") + 1:]
out = os.path.abspath(args[0])
wanted = args[1] if len(args) > 1 else None
EMBLEM_MATS = {"Color_I16", "emblem1"}

scene = bpy.context.scene
cands = [o for o in bpy.data.objects if o.type == 'MESH' and any(m and m.name in EMBLEM_MATS for m in o.data.materials)]
if wanted:
    cands = [o for o in cands if wanted in o.name]
if not cands:
    raise SystemExit("No se encuentra ningún emblema")


def wbox(o):
    pts = [o.matrix_world @ Vector(c) for c in o.bound_box]
    return (Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts))),
            Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts))))


def flat_area(o):
    mn, mx = wbox(o)
    d = sorted(mx - mn)
    return d[1] * d[2] / max(d[0], 1e-4)          # emblema plano: dos lados grandes y uno fino


emblem = max(cands, key=flat_area)
mn, mx = wbox(emblem)
size = mx - mn
thin = min(range(3), key=lambda i: size[i])
print("CV_EMBLEM", emblem.name, "tamaño", tuple(round(v, 3) for v in size), "eje fino", "XYZ"[thin])

# Sombreado suave (el emblema importado tiene caras planas).
for poly in emblem.data.polygons:
    poly.use_smooth = True

# Ocultar todo lo demás.
for o in bpy.data.objects:
    if o.type == 'MESH':
        o.hide_render = o is not emblem

# Materiales de render del emblema: azul lacado y cromo.
def principled(name, color, metallic, rough, coat=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*color, 1)
    b.inputs["Metallic"].default_value = metallic
    b.inputs["Roughness"].default_value = rough
    if coat and "Coat Weight" in b.inputs:
        b.inputs["Coat Weight"].default_value = coat
    return m

blue = principled("logo_blue_r", (0.0, 0.035, 0.26), 0.2, 0.15, coat=1.0)
chrome = principled("logo_chrome_r", (0.95, 0.95, 0.97), 1.0, 0.12)
for slot in emblem.material_slots:
    if slot.material and slot.material.name == "Color_I16":
        slot.material = blue
    else:
        slot.material = chrome

# Cámara ortográfica de frente al emblema (mirando por su eje fino, desde fuera del coche).
center = (mn + mx) / 2
axis = Vector((0, 0, 0))
axis[thin] = 1.0
# El exterior es el lado más alejado del centro del coche.
all_meshes = [o for o in bpy.data.objects if o.type == 'MESH']
car_c = sum((sum(wbox(o), Vector()) / 2 for o in all_meshes), Vector()) / len(all_meshes)
if (center - car_c).dot(axis) < 0:
    axis = -axis
cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
scene.collection.objects.link(cam)
cam.data.type = 'ORTHO'
dims = [size[i] for i in range(3) if i != thin]
cam.data.ortho_scale = max(dims) * 1.08
cam.location = center + axis * 2.0
up = Vector((0, 0, 1)) if thin != 2 else Vector((0, 1, 0))
cam.rotation_euler = (center - cam.location).to_track_quat('-Z', 'Z' if thin != 2 else 'Y').to_euler()
scene.camera = cam

# Iluminación de estudio para que el cromo tenga reflejos.
world = bpy.data.worlds.new("w")
world.use_nodes = True
nt = world.node_tree
bg = nt.nodes["Background"]
grad = nt.nodes.new("ShaderNodeTexGradient")
coord = nt.nodes.new("ShaderNodeTexCoord")
ramp = nt.nodes.new("ShaderNodeValToRGB")
ramp.color_ramp.elements[0].color = (0.25, 0.26, 0.28, 1)
ramp.color_ramp.elements[1].color = (2.2, 2.2, 2.3, 1)
nt.links.new(coord.outputs["Generated"], grad.inputs["Vector"])
nt.links.new(grad.outputs["Color"], ramp.inputs["Fac"])
nt.links.new(ramp.outputs["Color"], bg.inputs["Color"])
scene.world = world
for i, (rx, rz, e) in enumerate(((50, 30, 4.0), (70, -120, 2.0))):
    l = bpy.data.objects.new(f"l{i}", bpy.data.lights.new(f"l{i}", 'SUN'))
    l.data.energy = e
    l.rotation_euler = (math.radians(rx), 0, math.radians(rz))
    scene.collection.objects.link(l)

engines = {e.identifier for e in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items}
scene.render.engine = 'CYCLES' if 'CYCLES' in engines else 'BLENDER_EEVEE'
if scene.render.engine == 'CYCLES':
    scene.cycles.samples = 96
    scene.cycles.device = 'CPU'
scene.render.film_transparent = True
aspect = dims[0] / dims[1] if dims[0] > dims[1] else dims[1] / dims[0]
scene.render.resolution_x = 1024
scene.render.resolution_y = int(1024 / aspect * 1.08)
scene.render.image_settings.file_format = 'PNG'
scene.render.image_settings.color_mode = 'RGBA'
scene.render.filepath = out
os.makedirs(os.path.dirname(out), exist_ok=True)
bpy.ops.render.render(write_still=True)
print("CV_LOGO_OK", out)
