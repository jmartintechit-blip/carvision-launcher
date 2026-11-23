"""Renderiza un SVG plano (logotipo) como máscara blanca sobre fondo transparente, en alta resolución.

Sirve para convertir logotipos vectoriales (p. ej. de Wikimedia Commons) en PNG con los que luego se compone el
emblema final con Pillow (colores de marca, aros, etc.). Cada trazo del SVG queda como un objeto: con --only se
renderizan solo los trazos cuyo índice se indique (p. ej. el emblema sin el nombre de la marca).

Uso:
  blender -b --factory-startup --python blender/scripts/render_svg_mask.py -- <entrada.svg> <salida.png> <px> [--only 0,1,2] [--list]
"""
import sys

import bpy
from mathutils import Vector

args = sys.argv[sys.argv.index("--") + 1:]
src, dst, px = args[0], args[1], int(args[2])
only = None
if "--only" in args:
    only = {int(i) for i in args[args.index("--only") + 1].split(",")}

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_curve.svg(filepath=src)
curves = sorted((o for o in bpy.data.objects if o.type == 'CURVE'), key=lambda o: o.name)


def bbox(o):
    pts = [o.matrix_world @ Vector(c) for c in o.bound_box]
    return (Vector((min(p.x for p in pts), min(p.y for p in pts))), Vector((max(p.x for p in pts), max(p.y for p in pts))))


for i, o in enumerate(curves):
    lo, hi = bbox(o)
    print(f"CV_PATH {i} {o.name} x {lo.x:.4f}..{hi.x:.4f} y {lo.y:.4f}..{hi.y:.4f}")
if "--list" in args:
    sys.exit(0)

keep = [o for i, o in enumerate(curves) if only is None or i in only]
for o in curves:
    if o not in keep:
        bpy.data.objects.remove(o)

mat = bpy.data.materials.new("white")
mat.use_nodes = True
nt = mat.node_tree
nt.nodes.clear()
em = nt.nodes.new("ShaderNodeEmission")
em.inputs["Color"].default_value = (1, 1, 1, 1)
out = nt.nodes.new("ShaderNodeOutputMaterial")
nt.links.new(em.outputs[0], out.inputs[0])
for o in keep:
    o.data.materials.clear()
    o.data.materials.append(mat)

lo = Vector((min(bbox(o)[0].x for o in keep), min(bbox(o)[0].y for o in keep)))
hi = Vector((max(bbox(o)[1].x for o in keep), max(bbox(o)[1].y for o in keep)))
c = (lo + hi) / 2
size = max(hi.x - lo.x, hi.y - lo.y)

cam_data = bpy.data.cameras.new("cam")
cam_data.type = 'ORTHO'
cam_data.ortho_scale = size * 1.0
cam = bpy.data.objects.new("cam", cam_data)
cam.location = (c.x, c.y, 10)
bpy.context.scene.collection.objects.link(cam)
sc = bpy.context.scene
sc.camera = cam
sc.render.engine = 'BLENDER_EEVEE_NEXT' if 'BLENDER_EEVEE_NEXT' in [e.identifier for e in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items] else 'BLENDER_EEVEE'
sc.render.film_transparent = True
sc.view_settings.view_transform = 'Standard'
w = hi.x - lo.x
h = hi.y - lo.y
sc.render.resolution_x = px if w >= h else max(1, round(px * w / h))
sc.render.resolution_y = px if h >= w else max(1, round(px * h / w))
sc.render.filepath = dst
sc.render.image_settings.file_format = 'PNG'
sc.render.image_settings.color_mode = 'RGBA'
bpy.ops.render.render(write_still=True)
print("CV_MASK", dst, sc.render.resolution_x, sc.render.resolution_y)
