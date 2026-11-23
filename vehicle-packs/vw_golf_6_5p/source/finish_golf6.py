"""Acabado a mano del VW Golf VI (Car2022, Sketchfab) para el catálogo de CarVision.

Lo llama el generador (config.json → "finish") con el coche ya escalado y orientado (frontal hacia -Y, izquierda +X,
suelo en Z=0) y las piezas aún sueltas. Cada arreglo está comprobado a ojo en el visor y en la app:

1. Materiales sin su textura del juego (verde o blanco de relleno): guarnecido del maletero y del portón, motor,
   pasos de rueda, bajos y rejilla bajo el parabrisas. Pasan al acabado del coche real (gris o negro).
2. Emblemas VW delantero y trasero: vienen en negro brillante, metidos en la malla de la parrilla y del portón. Se
   separan sus caras por posición y pasan a cromado, como en el coche real.
"""
import bmesh
from mathutils import Vector

DIAG = False

# 1. Materiales que el modelo trae sin su textura del juego (verde o blanco de relleno). Se les da el acabado que
#    tienen en el coche real; los demás materiales se quedan tal cual vienen.
FIXES = {
    # material: (color lineal, metálico, rugosidad, qué es)
    "vivace_interior": ((0.03, 0.03, 0.032), 0.0, 0.75, "guarnecido del maletero y del portón (venía verde)"),
    "vivace_engine": ((0.045, 0.045, 0.048), 0.35, 0.5, "motor (venía blanco)"),
    "vivace": ((0.02, 0.02, 0.022), 0.0, 0.6, "pasos de rueda y bajos (venían blancos)"),
    "Gar_cbs": ((0.015, 0.015, 0.017), 0.0, 0.55, "rejilla bajo el parabrisas (venía blanca)"),
    "material_43": ((0.015, 0.015, 0.017), 0.0, 0.55, "plástico bajo el paragolpes delantero (venía blanco)"),
}
for name, (col, met, rough, what) in FIXES.items():
    m = bpy.data.materials.get(name)
    if not (m and m.use_nodes):
        continue
    b = m.node_tree.nodes.get("Principled BSDF")
    b.inputs["Base Color"].default_value = (*col, 1.0)
    b.inputs["Metallic"].default_value = met
    b.inputs["Roughness"].default_value = rough
    rep.step(f"Golf: {what}")

# 2. Emblemas cromados
chrome = E.cv.material("cv_badge_chrome", color=(0.92, 0.92, 0.94), metallic=1.0, roughness=0.24)
EMBLEMS = [
    # (nombre, material que lo contiene, centro (x, y de la cara delantera, z), radio, hacia dónde mira).
    # Medidos en el visor con rayos sobre el emblema (2026-11-13).
    ("delantero", "etk800", Vector((0.0, -2.035, 0.644)), 0.068, -1.0),
    ("trasero", "etk800", Vector((0.015, 1.98, 0.855)), 0.066, 1.0),
]
for label, mat_name, c0, radius, facing in EMBLEMS:
    picked = 0
    for o in [o for o in bpy.data.objects if o.type == 'MESH']:
        me = o.data
        idx = [i for i, s in enumerate(me.materials) if s and s.name == mat_name]
        if not idx:
            continue
        bm = bmesh.new()
        bm.from_mesh(me)
        bm.transform(o.matrix_world)
        sel = []
        for f in bm.faces:
            if f.material_index not in idx:
                continue
            c = f.calc_center_median()
            # Dentro del disco del emblema (todos sus vértices), por delante de la parrilla/portón y mirando hacia
            # fuera. Así entran también los triángulos grandes que cruzan el emblema.
            inside = all(Vector((v.co.x - c0.x, v.co.z - c0.z)).length < radius + 0.004 for v in f.verts)
            ahead = (c.y - c0.y) * facing > 0
            if inside and ahead and f.normal.y * facing > 0.2:
                sel.append(f)
        if DIAG and sel:
            ys = sorted(f.calc_center_median().y for f in sel)
            print("CV_DIAG", label, o.name, len(sel), "y", round(ys[0], 4), round(ys[-1], 4))
        if sel:
            if chrome.name not in [s.name for s in me.materials if s]:
                me.materials.append(chrome)
            ci = [i for i, s in enumerate(me.materials) if s and s.name == chrome.name][0]
            sel_ids = {f.index for f in sel}
            bm.free()
            for poly in me.polygons:
                if poly.index in sel_ids:
                    poly.material_index = ci
            picked += len(sel_ids)
        else:
            bm.free()
    rep.step(f"Golf: emblema {label} cromado ({picked} caras)")
