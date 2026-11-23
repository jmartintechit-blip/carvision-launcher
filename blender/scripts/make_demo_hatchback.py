"""Genera el coche de PRUEBA de CarVision: un hatchback esquemático de 5 puertas.

NO es un modelo de ningún coche real. Sirve para validar el pipeline (jerarquía de nodos,
bisagras, animaciones, materiales, exportación GLB y carga en Android) mientras se integra
el modelo definitivo. Proporciones aproximadas de un compacto de segmento C (4,36 m).

Uso:
  blender -b --factory-startup --python blender/scripts/make_demo_hatchback.py -- <salida.glb>
"""
import math
import os
import sys

import bmesh
import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cv_common as cv  # noqa: E402

out = sys.argv[sys.argv.index("--") + 1] if "--" in sys.argv else "demo_hatchback.glb"

cv.reset_scene()

# ---------------------------------------------------------------- materiales
paint = cv.material("paint", (0.05, 0.16, 0.42), metallic=0.55, roughness=0.32, coat=1.0)
glass = cv.material("glass", (0.015, 0.02, 0.03), roughness=0.04, alpha=0.82)
trim = cv.material("trim_black", (0.02, 0.02, 0.022), roughness=0.55)
chrome = cv.material("chrome", (0.9, 0.9, 0.92), metallic=1.0, roughness=0.12)
tire = cv.material("tire", (0.025, 0.025, 0.025), roughness=0.9)
rim = cv.material("rim", (0.62, 0.64, 0.67), metallic=1.0, roughness=0.28)
head = cv.material("light_front", (0.9, 0.95, 1.0), roughness=0.1,
                   emission=(0.85, 0.93, 1.0), emission_strength=3.0)
tail = cv.material("light_rear", (0.5, 0.02, 0.02), roughness=0.2,
                   emission=(1.0, 0.05, 0.04), emission_strength=2.0)

# ---------------------------------------------------------------- dimensiones
L, W = 4.36, 1.82
XB = W / 2           # semiancho de carrocería
XC = 0.74            # semiancho del habitáculo
FRONT, REAR = -L / 2, L / 2
AXLE_F, AXLE_R = -1.26, 1.39
WR = 0.32            # radio de rueda
BELT = 0.98          # línea de cintura

root = bpy.data.objects.new("car_root", None)
bpy.context.scene.collection.objects.link(root)

# ---------------------------------------------------------------- carrocería
lower = [
    (FRONT + 0.02, 0.30), (FRONT, 0.52), (FRONT + 0.06, 0.70), (FRONT + 0.25, 0.80),
    (-0.95, BELT), (1.80, BELT + 0.03), (REAR - 0.04, 0.98), (REAR, 0.62),
    (REAR - 0.03, 0.30),
]
body = cv.mesh_object("body", cv.extrude_profile_x(lower, -XB, XB), paint, parent=root)

# Pasos de rueda (resta booleana, antes del biselado).
for i, y in enumerate((AXLE_F, AXLE_R)):
    cut = cv.mesh_object(f"_arch_{i}", cv.cylinder_x(0, y, WR, WR + 0.06, XB + 0.2))
    cut.hide_render = True
    cut.hide_set(True)
    mod = body.modifiers.new(f"arch_{i}", 'BOOLEAN')
    mod.operation = 'DIFFERENCE'
    mod.object = cut
cv.add_bevel(body, 0.07, segments=4)

cabin = [(-0.95, BELT), (-0.10, 1.46), (1.40, 1.45), (2.02, 1.02), (1.80, BELT + 0.02)]
cv.mesh_object("cabin_glass", cv.extrude_profile_x(cabin, -XC, XC), glass, parent=body)
roof = cv.mesh_object("roof", cv.box(-XC + 0.02, XC - 0.02, -0.12, 1.40, 1.44, 1.49), paint, parent=body)
cv.add_bevel(roof, 0.03)

cv.mesh_object("grille", cv.box(-0.48, 0.48, FRONT - 0.005, FRONT + 0.05, 0.38, 0.58), trim, parent=body)
for side in (1, -1):
    x0, x1 = sorted((side * 0.52, side * 0.86))
    cv.mesh_object(f"headlight_{'l' if side > 0 else 'r'}",
                   cv.box(x0, x1, FRONT + 0.04, FRONT + 0.16, 0.70, 0.79), head, parent=body)
    x0, x1 = sorted((side * 0.58, side * 0.88))
    cv.mesh_object(f"taillight_{'l' if side > 0 else 'r'}",
                   cv.box(x0, x1, REAR - 0.06, REAR + 0.005, 0.80, 0.92), tail, parent=body)

# ---------------------------------------------------------------- puertas
def door(name, side, y_hinge, y_end, window):
    """Puerta con origen en la bisagra (borde delantero). `window` = perfil (y, z) del cristal."""
    xs = side * (XB + 0.004)
    x0, x1 = sorted((xs, xs + side * 0.03))
    panel = cv.extrude_profile_x([(y_hinge, 0.36), (y_end, 0.36), (y_end, BELT + 0.01), (y_hinge, BELT + 0.01)], x0, x1)
    obj = cv.mesh_object(name, panel, paint, origin=(xs, y_hinge, 0.6), parent=root)
    cv.add_bevel(obj, 0.012)
    gx0, gx1 = sorted((side * (XC + 0.012), side * (XC + 0.022)))
    cv.mesh_object(f"{name}_glass", cv.extrude_profile_x(window, gx0, gx1), glass,
                   origin=(xs, y_hinge, 0.6), parent=obj)
    hx0, hx1 = sorted((side * (XB + 0.034), side * (XB + 0.05)))
    cv.mesh_object(f"{name}_handle", cv.box(hx0, hx1, y_end - 0.32, y_end - 0.18, 0.86, 0.89), chrome,
                   origin=(xs, y_hinge, 0.6), parent=obj)
    return obj

front_win = [(-0.72, 0.99), (-0.24, 1.37), (0.17, 1.37), (0.17, 0.99)]
rear_win = [(0.23, 0.99), (0.23, 1.37), (1.12, 1.35), (1.32, 0.99)]
for side, s in ((1, "left"), (-1, "right")):
    fd = door(f"front_{s}_door", side, -0.95, 0.20, front_win)
    door(f"rear_{s}_door", side, 0.20, 1.24, rear_win)
    # Retrovisor, unido a la puerta delantera.
    xm0, xm1 = sorted((side * (XB + 0.03), side * (XB + 0.17)))
    cv.mesh_object(f"mirror_{s}", cv.box(xm0, xm1, -0.84, -0.66, 1.00, 1.11), paint,
                   origin=(side * (XB + 0.004), -0.95, 0.6), parent=fd)

# ---------------------------------------------------------------- portón
hinge = (0.0, 1.40, 1.45)
hatch_profile = [(1.40, 1.45), (2.03, 1.02), (REAR + 0.01, 0.62), (REAR - 0.03, 0.62), (1.98, 1.02), (1.38, 1.43)]
hatch = cv.mesh_object("rear_hatch", cv.extrude_profile_x(hatch_profile, -XC + 0.02, XC - 0.02), paint,
                       origin=hinge, parent=root)
cv.add_bevel(hatch, 0.015)
hatch_glass = [(1.43, 1.452), (1.96, 1.07), (1.99, 1.07), (1.46, 1.462)]
cv.mesh_object("rear_hatch_glass", cv.extrude_profile_x(hatch_glass, -XC + 0.08, XC - 0.08), glass,
               origin=hinge, parent=hatch)

# ---------------------------------------------------------------- ruedas
def wheel(name, x_side, y):
    cx = x_side * (XB - 0.13)
    w = cv.mesh_object(name, cv.cylinder_x(cx, y, WR, WR, 0.11, segments=48), tire,
                       origin=(cx, y, WR), parent=root, smooth=True)
    cv.add_bevel(w, 0.04, segments=4)
    face = x_side * 0.112
    rx0, rx1 = sorted((cx + face, cx + face + x_side * 0.008))
    cv.mesh_object(f"{name}_rim", cv.cylinder_x((rx0 + rx1) / 2, y, WR, 0.225, (rx1 - rx0) / 2, segments=48),
                   rim, origin=(cx, y, WR), parent=w, smooth=True)
    for k in range(5):
        a = k * 2 * math.pi / 5
        bm = cv.box(min(rx0, rx1) - 0.004 * x_side, max(rx0, rx1) + 0.004 * x_side, -0.025, 0.025, 0.0, 0.2)
        import mathutils
        bmesh.ops.rotate(bm, verts=bm.verts, cent=(0, 0, 0), matrix=mathutils.Matrix.Rotation(a, 3, 'X'))
        bmesh.ops.translate(bm, verts=bm.verts, vec=(0, y, WR))
        cv.mesh_object(f"{name}_spoke{k}", bm, chrome, origin=(cx, y, WR), parent=w)
    return w

wheel("front_left_wheel", 1, AXLE_F)
wheel("front_right_wheel", -1, AXLE_F)
wheel("rear_left_wheel", 1, AXLE_R)
wheel("rear_right_wheel", -1, AXLE_R)

# Los cortadores booleanos no se exportan.
for o in list(bpy.data.objects):
    if o.name.startswith("_arch_"):
        o.hide_viewport = False
os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)

# Exportar solo la jerarquía del coche (excluye los cortadores).
for o in bpy.data.objects:
    o.select_set(False)
def select_tree(o):
    o.select_set(True)
    for c in o.children:
        select_tree(c)
for o in bpy.data.objects:
    o.hide_set(False)
select_tree(root)
bpy.ops.export_scene.gltf(filepath=out, export_format='GLB', export_apply=True, export_yup=True,
                          use_selection=True)
print("CV_EXPORT_OK", out)
