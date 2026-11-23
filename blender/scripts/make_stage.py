"""Genera el escenario de CarVision: suelo de estudio con un foco de luz muy difuminado (sin bordes ni anillos).

Es un diseño propio del proyecto (identidad visual de CarVision).
Modelado para un coche de 2,2 m de semilongitud; el renderizador lo escala al coche cargado.

Uso:
  blender -b --factory-startup --python blender/scripts/make_stage.py -- <salida.glb>
"""
import math
import os
import sys

import bmesh
import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cv_common as cv  # noqa: E402

out = sys.argv[sys.argv.index("--") + 1] if "--" in sys.argv else "stage.glb"
cv.reset_scene()

R = 3.6
ACCENT = (0.48, 0.65, 1.0)   # azul hielo (acento de la interfaz)

# Suelo: disco con degradado radial a transparente (el coche "flota" sobre una luz suave, sin bordes duros).
# El color base se ajusta en tiempo real según el tema (CarRenderer.setStageTheme).
W0 = 256
floor_img = bpy.data.images.new("floor_fade", W0, W0, alpha=True)
fpx = [0.0] * (W0 * W0 * 4)
for j in range(W0):
    for i in range(W0):
        u = (i + 0.5) / W0 * 2 - 1
        v = (j + 0.5) / W0 * 2 - 1
        d = math.sqrt(u * u + v * v)
        a = math.exp(-((d * 2.1) ** 2))        # caída gaussiana: sin borde visible
        k = (j * W0 + i) * 4
        fpx[k] = fpx[k + 1] = fpx[k + 2] = 1.0
        fpx[k + 3] = a
floor_img.pixels = fpx
floor_img.pack()

disc_mat = bpy.data.materials.new("stage_disc")
disc_mat.use_nodes = True
nt0 = disc_mat.node_tree
b0 = nt0.nodes.get("Principled BSDF")
t0 = nt0.nodes.new("ShaderNodeTexImage")
t0.image = floor_img
b0.inputs["Base Color"].default_value = (0.03, 0.035, 0.045, 1)
b0.inputs["Roughness"].default_value = 0.7
nt0.links.new(t0.outputs["Alpha"], b0.inputs["Alpha"])
for attr, val in (("surface_render_method", "BLENDED"), ("blend_method", "BLEND")):
    try:
        setattr(disc_mat, attr, val)
    except (AttributeError, TypeError):
        pass


root = bpy.data.objects.new("stage_root", None)
bpy.context.scene.collection.objects.link(root)

# Suelo plano con UV radial.
bm = bmesh.new()
segs = 96
center_v = bm.verts.new((0, 0, 0))
ring_vs = [bm.verts.new((R * math.cos(2 * math.pi * k / segs), R * math.sin(2 * math.pi * k / segs), 0)) for k in range(segs)]
faces = []
for k in range(segs):
    faces.append(bm.faces.new((center_v, ring_vs[k], ring_vs[(k + 1) % segs])))
uvl = bm.loops.layers.uv.new("UVMap")
for f in faces:
    for loop in f.loops:
        x, y = loop.vert.co.x, loop.vert.co.y
        loop[uvl].uv = (x / (2 * R) + 0.5, y / (2 * R) + 0.5)
cv.mesh_object("stage_disc", bm, disc_mat, parent=root)

# La sombra de contacto la trae cada coche (horneada desde su silueta en el pipeline del pack).

os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
cv.export_glb(out)
print("CV_EXPORT_OK", out)
