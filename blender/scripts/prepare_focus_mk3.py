"""Prepara el Ford Focus ST MK3 (savelliy07, Sketchfab, CC-BY 4.0) para CarVision.

Entrada: el .blend original (importado de SketchUp: 765 piezas sueltas, ~89k triángulos, sin texturas).
Salida:  - <salida>.glb listo para el pack (car_root, body, puertas, portón, capó, ruedas)
         - <salida>_prepared.blend para revisarlo o retocarlo a mano

Qué hace (reproducible, sin pasos manuales):
 1. Hornea las transformaciones de cada pieza.
 2. Conserva los emblemas (óvalo azul y cromado). Con --no-logos los elimina.
 3. Clasifica cada pieza en un grupo por su caja envolvente (puertas, portón, capó, ruedas, carrocería).
 4. Une cada grupo en una sola malla (de 765 objetos a 11 nodos: menos llamadas de dibujo).
 5. Escala a metros (largo real del Focus MK3 5p: 4,36 m), centra, apoya en el suelo,
    frontal hacia -Y y lado izquierdo hacia +X (convenciones del pipeline).
 6. Coloca el origen de cada pieza móvil en su bisagra.
 7. Sustituye los ~70 materiales originales por ~12 materiales PBR propios.

Las regiones de clasificación están en las coordenadas ORIGINALES del modelo (frontal en +Y, izquierda en -X).
Si se usa otro modelo, hay que revisar estas cifras con blender/scripts/analyze_car.py.

Uso:
  blender -b --factory-startup --disable-autoexec "<Ford Focus.blend>" --python blender/scripts/prepare_focus_mk3.py -- <salida.glb> [color_hex] [--no-logos]
"""
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cv_common as cv  # noqa: E402

raw_args = sys.argv[sys.argv.index("--") + 1:]
# Los emblemas se conservan por defecto (cada usuario prepara su propio coche).
# --no-logos los elimina (por ejemplo, para material promocional).
KEEP_LOGOS = "--no-logos" not in raw_args
args = [a for a in raw_args if not a.startswith("--")]
OUT = os.path.abspath(args[0])
PAINT_HEX = args[1] if len(args) > 1 else "D9480F"   # naranja tipo "Tangerine" del Focus ST (recoloreable)
REAL_LENGTH_M = 4.358

def hex_to_linear(h):
    def ch(v):
        c = int(v, 16) / 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return (ch(h[0:2]), ch(h[2:4]), ch(h[4:6]))

# ------------------------------------------------------------------ 1. hornear transformaciones
meshes = [o for o in bpy.data.objects if o.type == 'MESH']
for o in meshes:
    if o.data.users > 1:
        o.data = o.data.copy()
for o in meshes:
    o.data.transform(o.matrix_world)
    o.parent = None
    o.matrix_world = Matrix.Identity(4)
for o in [o for o in bpy.data.objects if o.type != 'MESH']:
    bpy.data.objects.remove(o)

# ------------------------------------------------------------------ 2. logotipos
LOGO_MATS = {"Color_I16", "emblem1"}
removed = 0
if not KEEP_LOGOS:
    def _box(o):
        pts = [v.co for v in o.data.vertices]
        return (Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts))),
                Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts))))
    emblems = [o for o in meshes if o.data.vertices and any(m and m.name in LOGO_MATS for m in o.data.materials)]
    eboxes = [_box(o) for o in emblems]
    doomed = set(emblems)
    for o in meshes:
        if not o.data.vertices or o in doomed:
            continue
        # Otras insignias del modelo (p. ej. «ST» del portón) llevan "emblem" en el nombre.
        if "emblem" in o.name.lower():
            doomed.add(o)
            continue
        # Marcos y soportes del emblema: piezas pequeñas sin pintura metidas en la caja del emblema
        # o centradas en él (el marco negro del óvalo delantero es algo mayor que el emblema).
        mn, mx = _box(o)
        if any(m and m.name == "body1" for m in o.data.materials):
            continue
        oc, oe = (mn + mx) / 2, mx - mn
        for emn, emx in eboxes:
            pad = Vector((0.03, 0.03, 0.03))
            inside = all(emn[i] - pad[i] <= mn[i] and mx[i] <= emx[i] + pad[i] for i in range(3))
            ec, ee = (emn + emx) / 2, emx - emn
            centred = (oc - ec).length < 0.12 and max(oe) < max(ee) * 1.6
            if inside or centred:
                doomed.add(o)
                break
    for o in doomed:
        print("CV_LOGO_REMOVED", o.name[:40])
        meshes.remove(o)
        bpy.data.objects.remove(o)
        removed += 1
    # Tapas lisas en el color de la carrocería donde iban los emblemas exteriores (debajo queda un hueco).
    import bmesh as _bm
    ys_all = [v.co.y for o in meshes for v in o.data.vertices]
    y_lo, y_hi = min(ys_all), max(ys_all)
    paint_src = next(m for m in bpy.data.materials if m.name == "body1")
    for (emn, emx), em in zip(eboxes, emblems):
        ec = (emn + emx) / 2
        if y_lo + 1.0 < ec.y < y_hi - 1.0:
            continue                      # emblema interior (volante): no necesita tapa
        front = ec.y > (y_lo + y_hi) / 2
        # Algo por delante de la cara interior del emblema y algo mayor que él: tapa el borde del hueco.
        depth = emx.y - emn.y
        y = (emn.y + depth * 0.62) if front else (emx.y - depth * 0.3)
        rx, rz = ((emx.x - emn.x) / 2 * (1.28 if front else 1.16), (emx.z - emn.z) / 2 * (1.5 if front else 1.3))
        # Caras de marcos grandes (p. ej. el del paragolpes) que forman el aro del emblema: fuera.
        for o in meshes:
            if any(m and m.name == "body1" for m in o.data.materials) or not o.data.polygons:
                continue
            bmo = _bm.new()
            bmo.from_mesh(o.data)
            kill = [f for f in bmo.faces
                    if ((f.calc_center_median().x - ec.x) / (rx * 1.25)) ** 2 + ((f.calc_center_median().z - ec.z) / (rz * 1.4)) ** 2 <= 1.0
                    and abs(f.calc_center_median().y - ec.y) < 0.15]
            if kill and len(kill) < len(bmo.faces):
                _bm.ops.delete(bmo, geom=kill, context='FACES')
                bmo.to_mesh(o.data)
                print("CV_LOGO_RING", o.name[:30], len(kill), "caras")
            bmo.free()
        bmf = _bm.new()
        n = 40
        ring = [bmf.verts.new((ec.x + rx * math.cos(2 * math.pi * i / n), y, ec.z + rz * math.sin(2 * math.pi * i / n))) for i in range(n)]
        face = bmf.faces.new(ring if front else ring[::-1])
        me = bpy.data.meshes.new("cv_fill")
        bmf.to_mesh(me)
        bmf.free()
        fill = bpy.data.objects.new("cv_fill_" + ("front" if front else "rear"), me)
        bpy.context.scene.collection.objects.link(fill)
        me.materials.append(paint_src)
        fill["cv_group"] = "body" if front else "rear_hatch"
        meshes.append(fill)
        print("CV_LOGO_FILL", fill.name, tuple(round(v, 2) for v in ec))
print(f"CV_STEP logotipos {'conservados' if KEEP_LOGOS else 'eliminados: ' + str(removed)}")


def bbox(o):
    vs = o.data.vertices
    if not vs:
        return None
    xs = [v.co.x for v in vs]; ys = [v.co.y for v in vs]; zs = [v.co.z for v in vs]
    return Vector((min(xs), min(ys), min(zs))), Vector((max(xs), max(ys), max(zs)))


boxes = {}
for o in list(meshes):
    b = bbox(o)
    if b is None:
        bpy.data.objects.remove(o)
        meshes.remove(o)
    else:
        boxes[o.name] = b

gmn = Vector((min(b[0].x for b in boxes.values()), min(b[0].y for b in boxes.values()), min(b[0].z for b in boxes.values())))
gmx = Vector((max(b[1].x for b in boxes.values()), max(b[1].y for b in boxes.values()), max(b[1].z for b in boxes.values())))
print("CV_STEP caja global original", tuple(round(v, 2) for v in gmn), tuple(round(v, 2) for v in gmx))

# ------------------------------------------------------------------ 3. clasificación (coordenadas originales)
XL_OUT, XR_OUT = 1.05, 4.05        # zona exterior de los costados (izquierda x pequeña, derecha x grande)
Y_DOOR_SPLIT = 6.91                # separación entre puerta delantera y trasera
Y_FRONT_DOOR_END = 9.75
Y_REAR_DOOR_START = 4.40


def center(b):
    return (b[0] + b[1]) / 2


def extent(b):
    return b[1] - b[0]


groups = {k: [] for k in ("body", "front_left_door", "front_right_door", "rear_left_door",
                          "rear_right_door", "rear_hatch", "hood")}

# Ruedas: centros a partir de las llantas (material rim1).
# Solo la llanta principal de cada rueda (la de más caras): los tapones de tornillo también usan "rim1"
# y, si se toman como referencia, el eje queda desplazado y la rueda "tiembla" al girar.
rims_all = [o for o in meshes if any(m and m.name == "rim1" for m in o.data.materials)]
rims = [o for o in rims_all if len(o.data.polygons) > 500]
wheel_centers = {}
for r in rims:
    c = center(boxes[r.name])
    side = "left" if c.x < (gmn.x + gmx.x) / 2 else "right"
    pos = "front" if c.y > (gmn.y + gmx.y) / 2 else "rear"
    wheel_centers[f"{pos}_{side}_wheel"] = c
    rim_box_x = (boxes[r.name][0].x, boxes[r.name][1].x)
    wheel_rim_x = globals().setdefault("wheel_rim_x", {})
    wheel_rim_x[f"{pos}_{side}_wheel"] = rim_box_x
for k in wheel_centers:
    groups[k] = []
print("CV_STEP ruedas:", {k: tuple(round(v, 2) for v in c) for k, c in wheel_centers.items()})

for o in meshes:
    b = boxes[o.name]
    c, e = center(b), extent(b)
    mats = {m.name for m in o.data.materials if m}
    target = "body"
    if "cv_group" in o:
        groups[o["cv_group"]].append(o)
        continue

    # Ruedas: solo lo que gira de verdad con la rueda.
    #  a) piezas centradas en el eje (neumático, llanta, disco de freno, tapacubos);
    #  b) tornillería pequeña dentro del disco de la llanta y en su cara exterior.
    # Quedan fuera (fijas en la carrocería) la pinza de freno y el paso de rueda: si giraran, parecería que la
    # rueda va suelta y que lleva algo pegado por fuera.
    for k, wc in wheel_centers.items():
        if abs(c.x - wc.x) > 0.45:
            continue
        on_axis = abs(c.y - wc.y) < 0.06 and abs(c.z - wc.z) < 0.06 and e.y < 1.8 and e.z < 1.8
        small_bolt = (((c.y - wc.y) ** 2 + (c.z - wc.z) ** 2) ** 0.5 < 0.62 and max(e.x, e.y, e.z) < 0.12)
        if on_axis or small_bolt:
            target = k
            break

    # Las pinzas de freno (BAER) van pintadas y caen en la zona de la puerta trasera: nunca son puerta.
    if target == "body" and "BAER" not in o.name and e.y < 3.2 and e.x < 1.25 and 0.55 < c.z < 3.55:
        if c.x < XL_OUT:
            side = "left"
        elif c.x > XR_OUT:
            side = "right"
        else:
            side = None
        if side:
            if Y_DOOR_SPLIT <= c.y < Y_FRONT_DOOR_END:
                target = f"front_{side}_door"
            elif Y_REAR_DOOR_START <= c.y < Y_DOOR_SPLIT:
                target = f"rear_{side}_door"

    if target == "body" and c.y < 4.15 and c.z > 1.72 and 0.75 < c.x < 4.35 and e.x < 4.3:
        target = "rear_hatch"

    if target == "body" and "body1" in mats and c.y > 10.2 and c.z > 1.85 and e.y < 3.0:
        target = "hood"

    groups[target].append(o)

for k, v in groups.items():
    print(f"CV_GROUP {k:18s} {len(v):4d} piezas")

# Panel pintado principal de cada pieza móvil (el más grande con body1): de él sale la bisagra,
# para que el retrovisor u otros salientes no la desplacen.
skin_box = {}
for k, objs in groups.items():
    painted = [o for o in objs if any(m and m.name == "body1" for m in o.data.materials)]
    if painted:
        skin_box[k] = boxes[max(painted, key=lambda o: (boxes[o.name][1] - boxes[o.name][0]).y).name]  # el panel más largo

# ------------------------------------------------------------------ 3b. paneles interiores de las puertas
# El forro interior de cada lateral es UNA pieza que abarca las dos puertas, el pilar y el lateral trasero: si se deja
# en la carrocería, al abrir la puerta su panel interior se queda dentro del coche. Se recorta la parte pegada a cada
# puerta con el mismo método del generador (engine._carve_door_faces).
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from carvision_pack_builder import engine as E  # noqa: E402

UNIT = REAL_LENGTH_M / (gmx.y - gmn.y)            # metros por unidad del modelo
door_vols = {}
for k in ("front_left_door", "front_right_door", "rear_left_door", "rear_right_door"):
    sk_objs = [o for o in groups[k] if any(m and m.name == "body1" for m in o.data.materials)]
    if not sk_objs:
        continue
    skin = [E.Part(max(sk_objs, key=lambda o: (boxes[o.name][1] - boxes[o.name][0]).y))]
    lo, hi = skin[0].mn.copy(), skin[0].mx.copy()
    if "left" in k:
        hi.x += 0.3 / UNIT                          # izquierda = x pequeña: hacia dentro es +X
    else:
        lo.x -= 0.3 / UNIT
    hi.z += 0.6 / UNIT
    door_vols[k] = (lo, hi, skin)
E._carve_door_faces([E.Part(o) for o in groups["body"]], door_vols, "body1", E.Report(), unit=UNIT, left_sign=-1.0)
for o in [o for o in bpy.data.objects if o.type == 'MESH' and o.get("cv_carved_for")]:
    groups[o["cv_carved_for"]].append(o)
    meshes.append(o)
    print(f"CV_STEP panel interior recortado -> {o['cv_carved_for']}: {o.name}")

# ------------------------------------------------------------------ 4. materiales
paint = cv.material("paint", hex_to_linear(PAINT_HEX), metallic=0.45, roughness=0.3, coat=1.0, coat_roughness=0.04)
M = {
    "paint": paint,
    "glass": cv.material("glass", (0.01, 0.012, 0.015), roughness=0.03, alpha=0.78),
    "chrome": cv.material("chrome", (0.92, 0.92, 0.94), metallic=1.0, roughness=0.1),
    "rim": cv.material("rim", (0.32, 0.33, 0.35), metallic=1.0, roughness=0.3),
    "tire": cv.material("tire", (0.02, 0.02, 0.02), roughness=0.92),
    "plastic": cv.material("plastic_black", (0.018, 0.018, 0.02), roughness=0.55),
    "trim_gloss": cv.material("trim_gloss_black", (0.01, 0.01, 0.012), roughness=0.15),
    "interior": cv.material("interior", (0.03, 0.03, 0.032), roughness=0.75),
    "light_red": cv.material("light_rear", (0.45, 0.01, 0.01), roughness=0.08, emission=(1.0, 0.02, 0.01), emission_strength=0.6),
    "light_clear": cv.material("light_lens", (0.85, 0.87, 0.9), metallic=0.9, roughness=0.06),
    "steel": cv.material("brake_steel", (0.45, 0.45, 0.46), metallic=1.0, roughness=0.45),
    "white": cv.material("white_detail", (0.8, 0.8, 0.8), roughness=0.4),
    "logo_blue": cv.material("logo_blue", (0.0, 0.03, 0.22), metallic=0.3, roughness=0.12, coat=1.0),
}
for name, m in M.items():
    b = m.node_tree.nodes.get("Principled BSDF")
    m.diffuse_color = b.inputs["Base Color"].default_value  # color en vistas Workbench

REMAP = {
    "body1": "paint",
    "glass1": "glass", "glass2": "glass", "window1": "glass", "black_glass1": "glass",
    "detail_glass_cle1": "light_clear", "mirrorMiddle1": "chrome",
    "chrome1": "chrome", "chrome1.001": "chrome", "chrome_1": "chrome", "textured_reflect1": "light_clear",
    "reflector2": "light_clear", "reverse_light1": "light_clear",
    "rim1": "rim", "inner_rim1": "plastic",
    "tire1": "tire", "rubber1": "tire", "rubber_trim1": "plastic", "weatherstrip1": "plastic",
    "tail_light1": "light_red", "detail_glass_red1": "light_red", "reflector1": "light_red", "Color_A06": "light_red",
    "leather1": "interior", "bump_leather1": "interior", "cloth1": "interior", "headliner1": "interior",
    "bump_stitching1": "interior", "bump_plastic1": "interior", "digital_gauge1": "interior",
    "gauge_emissive1": "interior", "gauge_needle1": "white",
    "frame1": "trim_gloss", "solid1": "plastic", "misc1": "plastic", "plastic1": "plastic", "plastic2": "plastic",
    "matte_colors1": "plastic", "mottled1": "plastic",
    "M_0132_LightGray": "steel", "M_0133_Gray": "steel", "M_0135_DarkGray": "plastic", "M_0136_Charcoal": "plastic",
    "_0135_DarkGray_1": "plastic", "_0135_DarkGray_2": "plastic", "_Charcoal_": "plastic", "_LightGray_": "steel",
    "metal1": "steel", "Color_C11": "white",
    "Color_I16": "logo_blue", "emblem1": "chrome",
}
unknown = set()


def remap(o):
    for slot in o.material_slots:
        old = slot.material.name if slot.material else ""
        key = REMAP.get(old)
        if key is None:
            unknown.add(old)
            key = "plastic"
        slot.material = M[key]


for o in meshes:
    remap(o)
print("CV_STEP materiales sin regla (-> plástico negro):", sorted(unknown))

# ------------------------------------------------------------------ 5. unir grupos


def join(objs, name):
    if not objs:
        return None
    target = objs[0]
    if len(objs) > 1:
        with bpy.context.temp_override(active_object=target, selected_editable_objects=objs, selected_objects=objs):
            bpy.ops.object.join()
    target.name = name
    target.data.name = name
    merge_slots(target)
    return target


def merge_slots(o):
    """Deja un solo hueco por material (menos primitivas en el GLB)."""
    me = o.data
    uniq = []
    remap_idx = {}
    for i, m in enumerate(me.materials):
        if m not in uniq:
            uniq.append(m)
        remap_idx[i] = uniq.index(m)
    idx = [remap_idx.get(p.material_index, 0) for p in me.polygons]
    me.materials.clear()
    for m in uniq:
        me.materials.append(m)
    for p, i in zip(me.polygons, idx):
        p.material_index = i


parts = {k: join(v, k) for k, v in groups.items()}

# ------------------------------------------------------------------ 5b. paneles interiores de puerta
# El modelo original no trae guarnecido interior: al abrir la puerta se vería el reverso de la chapa.
# Se crea un guarnecido copiando el panel pintado y desplazándolo hacia dentro con material de interior.
import bmesh  # noqa: E402

CARD_DEPTH_M = 0.045


def add_door_card(o, k):
    sb = skin_box.get(k)
    if sb is None:
        return 0
    s_tmp = REAL_LENGTH_M / (gmx.y - gmn.y)
    depth = CARD_DEPTH_M / s_tmp
    outward = -1.0 if "left" in k else 1.0          # coordenadas originales: izquierda = x pequeña
    me = o.data
    if M["interior"] not in list(me.materials):
        me.materials.append(M["interior"])
    card_idx = list(me.materials).index(M["interior"])
    paint_idx = {i for i, m in enumerate(me.materials) if m == M["paint"]}
    mn, mx = sb[0] - Vector((0.05, 0.05, 0.05)), sb[1] + Vector((0.05, 0.05, 0.05))
    bm = bmesh.new()
    bm.from_mesh(me)
    faces = [f for f in bm.faces if f.material_index in paint_idx
             and all(mn[i] <= f.calc_center_median()[i] <= mx[i] for i in range(3))]
    ret = bmesh.ops.duplicate(bm, geom=faces)
    nv = [g for g in ret["geom"] if isinstance(g, bmesh.types.BMVert)]
    nf = [g for g in ret["geom"] if isinstance(g, bmesh.types.BMFace)]
    bmesh.ops.translate(bm, verts=nv, vec=(-outward * depth, 0, 0))
    for f in nf:
        f.material_index = card_idx
    bmesh.ops.reverse_faces(bm, faces=nf)
    bm.to_mesh(me)
    bm.free()
    return len(nf)


for k in ("front_left_door", "front_right_door", "rear_left_door", "rear_right_door"):
    if parts.get(k):
        print(f"CV_STEP guarnecido {k}: {add_door_card(parts[k], k)} caras")

# ------------------------------------------------------------------ 6. escala, orientación, bisagras
length = gmx.y - gmn.y
s = REAL_LENGTH_M / length
c0 = Vector(((gmn.x + gmx.x) / 2, (gmn.y + gmx.y) / 2, gmn.z))
G = Matrix.Rotation(math.pi, 4, 'Z') @ Matrix.Scale(s, 4) @ Matrix.Translation(-c0)
print(f"CV_STEP escala {s:.4f} (largo original {length:.3f})")

for o in parts.values():
    if o:
        o.data.transform(G)

root = bpy.data.objects.new("car_root", None)
bpy.context.scene.collection.objects.link(root)


def set_origin(o, p):
    o.data.transform(Matrix.Translation(-p))
    o.location = p


def transformed_box(b):
    """Caja original -> caja en coordenadas finales (G es afín: basta transformar las 8 esquinas)."""
    pts = [G @ Vector((x, y, z)) for x in (b[0].x, b[1].x) for y in (b[0].y, b[1].y) for z in (b[0].z, b[1].z)]
    return (Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts))),
            Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts))))


hinges = {}
for k, o in parts.items():
    if o is None:
        continue
    # Puertas: bisagra desde el panel pintado. Portón y capó: desde la pieza completa (su panel viene partido en dos).
    mn, mx = transformed_box(skin_box[k]) if k.endswith("_door") and k in skin_box else bbox(o)
    if k.endswith("_door"):
        # Bisagra: borde delantero (frontal en -Y), cara exterior, altura media.
        x_out = mx.x if "left" in k else mn.x
        p = Vector((x_out, mn.y, (mn.z + mx.z) / 2))
    elif k == "rear_hatch":
        p = Vector(((mn.x + mx.x) / 2, mn.y, mx.z))          # borde superior, unido al techo
    elif k == "hood":
        p = Vector(((mn.x + mx.x) / 2, mx.y, mx.z))          # borde trasero, junto al parabrisas
    elif k.endswith("_wheel"):
        p = G @ wheel_centers[k]                              # eje real: centro de la llanta
    else:
        p = Vector((0, 0, 0))
    set_origin(o, p)
    o.parent = root
    hinges[k] = p
    tris = sum(len(f.vertices) - 2 for f in o.data.polygons)
    print(f"CV_PART {k:18s} origen=({p.x:+.3f},{p.y:+.3f},{p.z:+.3f}) tris={tris}")

# ------------------------------------------------------------------ 6b. sombra de contacto horneada
# Silueta cenital del coche -> desenfoques (cercano + amplio) + manchas bajo cada neumático.
# Da el "peso" del coche sobre el suelo sin sombras en tiempo real (barato en cualquier GPU).
import numpy as np  # noqa: E402

SH_RES = 512
car_mn = Vector((min(bbox(o)[0].x + o.location.x for o in parts.values() if o), min(bbox(o)[0].y + o.location.y for o in parts.values() if o), 0))
car_mx = Vector((max(bbox(o)[1].x + o.location.x for o in parts.values() if o), max(bbox(o)[1].y + o.location.y for o in parts.values() if o), 0))
SH_SIZE = max(car_mx.x - car_mn.x, car_mx.y - car_mn.y) * 1.35
scene = bpy.context.scene
scene.render.engine = 'BLENDER_WORKBENCH'
scene.display.shading.light = 'FLAT'
scene.display.shading.color_type = 'SINGLE'
scene.display.shading.single_color = (0, 0, 0)
scene.render.film_transparent = True
scene.render.resolution_x = scene.render.resolution_y = SH_RES
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = 'PNG'
scene.render.image_settings.color_mode = 'RGBA'
cam = bpy.data.objects.new("sh_cam", bpy.data.cameras.new("sh_cam"))
scene.collection.objects.link(cam)
cam.data.type = 'ORTHO'
cam.data.ortho_scale = SH_SIZE
cam.location = (0, 0, 10)
cam.rotation_euler = (0, 0, 0)
scene.camera = cam
tmp_png = os.path.splitext(OUT)[0] + "_silhouette.png"
scene.render.filepath = tmp_png
bpy.ops.render.render(write_still=True)
sil_img = bpy.data.images.load(tmp_png)
sw, shh = sil_img.size
sil = np.array(sil_img.pixels[:], dtype=np.float32).reshape(shh, sw, 4)[:, :, 3]
SH_RES = sw


def box_blur(a, r):
    if r < 1:
        return a
    k = 2 * r + 1
    for axis in (0, 1):
        pad = [(0, 0), (0, 0)]
        pad[axis] = (r + 1, r)
        c = np.cumsum(np.pad(a, pad, mode="edge"), axis=axis)
        a = (np.take(c, range(k, c.shape[axis]), axis=axis) - np.take(c, range(0, c.shape[axis] - k), axis=axis)) / k
    return a


def gauss(a, r):
    for _ in range(3):
        a = box_blur(a, max(1, int(r / 1.7)))
    return a


near = gauss(sil, SH_RES * 0.012)
far = gauss(sil, SH_RES * 0.055)
shadow = np.clip(near * 0.55 + far * 0.45, 0, 1)
# Manchas oscuras en el contacto de cada neumático.
yy, xx = np.mgrid[0:SH_RES, 0:SH_RES]
px_per_m = SH_RES / SH_SIZE
for k in ("front_left_wheel", "front_right_wheel", "rear_left_wheel", "rear_right_wheel"):
    if parts.get(k) is None:
        continue
    w = parts[k].location
    cx = SH_RES / 2 + w.x * px_per_m
    cy = SH_RES / 2 + w.y * px_per_m
    sig_x, sig_y = 0.09 * px_per_m, 0.22 * px_per_m
    shadow = np.maximum(shadow, 0.95 * np.exp(-(((xx - cx) / sig_x) ** 2 + ((yy - cy) / sig_y) ** 2) / 2))
shadow = np.clip(shadow, 0, 0.93)
rgba = np.zeros((SH_RES, SH_RES, 4), dtype=np.float32)
rgba[:, :, 3] = shadow
sh_img = bpy.data.images.new("car_contact_shadow", SH_RES, SH_RES, alpha=True)
sh_img.pixels = rgba.ravel().tolist()
sh_img.pack()
bpy.data.images.remove(sil_img)
os.remove(tmp_png)

sh_mat = bpy.data.materials.new("contact_shadow")
sh_mat.use_nodes = True
snt = sh_mat.node_tree
sb = snt.nodes.get("Principled BSDF")
stx = snt.nodes.new("ShaderNodeTexImage")
stx.image = sh_img
sb.inputs["Base Color"].default_value = (0, 0, 0, 1)
sb.inputs["Roughness"].default_value = 1.0
snt.links.new(stx.outputs["Alpha"], sb.inputs["Alpha"])
for attr, val in (("surface_render_method", "BLENDED"), ("blend_method", "BLEND")):
    try:
        setattr(sh_mat, attr, val)
    except (AttributeError, TypeError):
        pass
bm = bmesh.new()
hs = SH_SIZE / 2
vs = [bm.verts.new(p) for p in ((-hs, -hs, 0.004), (hs, -hs, 0.004), (hs, hs, 0.004), (-hs, hs, 0.004))]
face = bm.faces.new(vs)
uvl = bm.loops.layers.uv.new("UVMap")
for loop, uv in zip(face.loops, ((0, 0), (1, 0), (1, 1), (0, 1))):
    loop[uvl].uv = uv
sh_obj = cv.mesh_object("contact_shadow", bm, sh_mat, parent=root)
bpy.data.objects.remove(cam)
print("CV_STEP sombra de contacto horneada", f"{SH_SIZE:.2f} m")

# ------------------------------------------------------------------ 7. guardar y exportar
for o in bpy.data.objects:
    o.select_set(False)


def select_tree(o):
    o.select_set(True)
    for ch in o.children:
        select_tree(ch)


select_tree(root)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
DRACO = "--no-draco" not in raw_args   # compresión de mallas (Filament la admite); --no-draco para depurar
bpy.ops.export_scene.gltf(filepath=OUT, export_format='GLB', export_apply=True, export_yup=True, use_selection=True,
                          export_draco_mesh_compression_enable=DRACO, export_draco_mesh_compression_level=6,
                          export_draco_position_quantization=14, export_draco_normal_quantization=10)
blend_out = os.path.splitext(OUT)[0] + "_prepared.blend"
bpy.ops.wm.save_as_mainfile(filepath=blend_out)
print("CV_EXPORT_OK", OUT)
print("CV_BLEND", blend_out)
