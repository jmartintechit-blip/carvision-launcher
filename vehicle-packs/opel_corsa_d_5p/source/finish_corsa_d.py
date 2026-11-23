"""Acabado a mano del Opel Corsa D 5 puertas (Merc_TV, Sketchfab) para el catálogo de CarVision.

Lo llama el generador (config.json → "finish") con el coche ya escalado y orientado (frontal hacia -Y, izquierda +X,
suelo en Z=0) y las piezas aún sueltas.

En este modelo TODOS los materiales vienen en blanco (metálico 1, rugosidad 1): el color solo está en el nombre
(blackcorsa, redcorsa, chromeplast…). Además la pintura y la mecánica traen texturas del juego (suciedad y golpes
para el efecto de daños) que hacían que la carrocería se viera «arrugada». Aquí se le da a cada material el acabado
del coche real, uno a uno, sin texturas de daños. Revisado en el visor y en la app.
"""
cv = E.cv
MAT = {}


def mat(key, **kw):
    if key not in MAT:
        MAT[key] = cv.material("cv_corsa_" + key, **kw)
    return MAT[key]


# Acabados (colores lineales). Valores de la paleta probada con el Focus hecho a mano.
def plastic():
    return mat("plastic", color=(0.018, 0.018, 0.02), roughness=0.55)


def trim_black():
    return mat("trim_black", color=(0.012, 0.012, 0.014), roughness=0.35)


def interior():
    return mat("interior", color=(0.03, 0.03, 0.032), roughness=0.75)


def interior_gray():
    return mat("interior_gray", color=(0.07, 0.07, 0.072), roughness=0.7)


def chrome():
    return mat("chrome", color=(0.92, 0.92, 0.94), metallic=1.0, roughness=0.1)


def alloy():
    return mat("alloy", color=(0.55, 0.56, 0.58), metallic=1.0, roughness=0.28)


def tire():
    return mat("tire", color=(0.02, 0.02, 0.02), roughness=0.92)


def glass():
    return mat("glass", color=(0.01, 0.012, 0.015), roughness=0.03, alpha=0.72)


def light_lens():
    return mat("light_lens", color=(0.85, 0.87, 0.9), metallic=0.9, roughness=0.06)


def light_red():
    return mat("light_red", color=(0.45, 0.01, 0.01), roughness=0.08, emission=(1.0, 0.02, 0.01), emission_strength=0.6)


def light_amber():
    return mat("light_amber", color=(0.85, 0.32, 0.02), roughness=0.1)


def mech():
    return mat("mech", color=(0.07, 0.07, 0.075), metallic=0.6, roughness=0.5)


def engine():
    return mat("engine", color=(0.045, 0.045, 0.048), metallic=0.35, roughness=0.5)


def pick(name, o):
    """Acabado para el material `name` en la pieza `o` (según dónde está)."""
    g = o.get("cv_group", "")
    pts = [o.matrix_world @ v.co for v in o.data.vertices] if o.data.vertices else []
    cy = sum(p.y for p in pts) / len(pts) if pts else 0.0
    wheel = g.endswith("_wheel")
    rear = cy > 1.0
    front = cy < -1.2
    base = name.split(".")[0]
    if base == "blackcorsa":
        return tire() if wheel else plastic()
    if base == "whitecorsa":
        return alloy() if wheel else chrome()              # llantas; reflectores de los faros
    if base == "chromeplast":
        return chrome()
    if base == "graycorsa":
        return interior_gray()
    if base in ("corsad_int", "sunburst_interior"):
        return interior()
    if base == "redcorsa":
        return light_red() if rear else interior()
    if base == "corsad_tail":
        return light_red() if rear else interior()         # en el salpicadero son testigos: interior
    if base == "corsad_brake":
        return light_red()
    if base in ("corsad_povl", "corsad_povr"):
        return light_amber()
    if base in ("corsad_povlint", "corsad_povrint"):
        return interior()
    if base in ("corsad_long", "corsad_short", "corsad_fog", "corsad_RL"):
        return light_lens()
    if base in ("glass_base",):
        return glass()
    if base == "sunburst_engine":
        return engine()
    if base == "sunburst":
        return mech()
    return None


changed = {}
for o in [o for o in bpy.data.objects if o.type == 'MESH']:
    for slot in o.material_slots:
        m = slot.material
        if m is None or m.name.startswith("cv_") or m.name == paint:
            continue
        new = pick(m.name, o)
        if new is not None:
            slot.material = new
            changed[m.name.split(".")[0]] = changed.get(m.name.split(".")[0], 0) + 1
rep.step("Corsa: acabados a mano por material: " + ", ".join(f"{k} {v}" for k, v in sorted(changed.items())))

# Pintura: sin las texturas de suciedad y golpes del juego (era lo que la hacía verse arrugada). Blanco Corsa.
pm = bpy.data.materials.get("paint") or bpy.data.materials.get(paint)
if pm and pm.use_nodes:
    nt = pm.node_tree
    for n in [n for n in nt.nodes if n.type == 'TEX_IMAGE']:
        nt.nodes.remove(n)
    b = nt.nodes.get("Principled BSDF")
    b.inputs["Base Color"].default_value = (0.80, 0.80, 0.79, 1.0)
    b.inputs["Metallic"].default_value = 0.0
    b.inputs["Roughness"].default_value = 0.32
    for k in ("Coat Weight", "Clearcoat"):
        if k in b.inputs:
            b.inputs[k].default_value = 1.0
    rep.step("Corsa: pintura sin texturas de daños, blanco con barniz")

# Normales: las de la carrocería venían deformadas (manchas y «arrugas» al reflejar la luz, sobre todo en puertas y
# paragolpes). Se borran las normales personalizadas y se suaviza por ángulo.
# PENDIENTE (2026-11-13): la geometría del modelo trae abolladuras del juego (paragolpes trasero, puertas); ni las
# normales ni un suavizado laplaciano las dejan perfectas. Hace falta otro modelo base del Corsa D.
import math
fixed = 0
for o in [o for o in bpy.data.objects if o.type == 'MESH']:
    if not any(s.material and s.material.name in ("paint", paint) for s in o.material_slots):
        continue
    me = o.data
    # En Blender 4.1+ las normales personalizadas son el atributo «custom_normal»: se borra directamente.
    if "custom_normal" in me.attributes:
        me.attributes.remove(me.attributes["custom_normal"])
    with bpy.context.temp_override(object=o, active_object=o, selected_objects=[o], selected_editable_objects=[o]):
        bpy.ops.object.shade_smooth_by_angle(angle=math.radians(35), keep_sharp_edges=False)
    if me.has_custom_normals:
        raise RuntimeError(f"{o.name}: siguen las normales personalizadas")
    fixed += 1
rep.step(f"Corsa: normales de la carrocería rehechas en {fixed} piezas (quitan las manchas de «arrugas»)")
