# SPDX-License-Identifier: GPL-3.0-or-later
# CarVision Pack Builder (complemento de Blender). Ver DECISIONS.md, D-025.
"""Utilidades comunes para los scripts de Blender de CarVision.

Convenciones del pipeline (ver docs/BLENDER_PIPELINE.md):
- Unidades en metros. Z arriba. Frontal del coche hacia -Y (el exportador glTF lo convierte a +Z).
- Lado izquierdo del coche = +X.
- Cada pieza móvil tiene su ORIGEN en la bisagra y rotación de objeto = identidad.
"""
import bpy
import bmesh


def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def material(name, color, metallic=0.0, roughness=0.5, coat=0.0, coat_roughness=0.03,
             emission=None, emission_strength=0.0, alpha=1.0):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")

    def set_input(names, value):
        for n in names:
            if n in bsdf.inputs:
                bsdf.inputs[n].default_value = value
                return

    set_input(["Base Color"], (*color, 1.0))
    set_input(["Metallic"], metallic)
    set_input(["Roughness"], roughness)
    if coat > 0:
        set_input(["Coat Weight", "Clearcoat"], coat)
        set_input(["Coat Roughness", "Clearcoat Roughness"], coat_roughness)
    if emission is not None:
        set_input(["Emission Color", "Emission"], (*emission, 1.0))
        set_input(["Emission Strength"], emission_strength)
    if alpha < 1.0:
        set_input(["Alpha"], alpha)
        for attr, val in (("surface_render_method", "BLENDED"), ("blend_method", "BLEND")):
            try:
                setattr(mat, attr, val)
            except (AttributeError, TypeError):
                pass
    return mat


def mesh_object(name, bm, mat=None, origin=(0.0, 0.0, 0.0), parent=None, smooth=False):
    """Crea un objeto a partir de un bmesh construido en coordenadas de MUNDO.
    Los vértices se desplazan para que el origen del objeto quede en `origin` (la bisagra)."""
    ox, oy, oz = origin
    for v in bm.verts:
        v.co.x -= ox
        v.co.y -= oy
        v.co.z -= oz
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    if smooth:
        for p in me.polygons:
            p.use_smooth = True
    obj = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(obj)
    if parent is not None:
        obj.parent = parent
        # Los padres no tienen rotación: posición local = posición de mundo - posición de mundo del padre.
        # (Se suma la cadena de .location porque matrix_world no está actualizada todavía.)
        px = py = pz = 0.0
        p = parent
        while p is not None:
            px += p.location.x
            py += p.location.y
            pz += p.location.z
            p = p.parent
        obj.location = (ox - px, oy - py, oz - pz)
    else:
        obj.location = origin
    if mat is not None:
        obj.data.materials.append(mat)
    return obj


def extrude_profile_x(points_yz, x0, x1):
    """Prisma: perfil lateral (y, z) extruido entre x0 y x1. Devuelve un bmesh en coordenadas de mundo."""
    bm = bmesh.new()
    a = [bm.verts.new((x0, y, z)) for (y, z) in points_yz]
    b = [bm.verts.new((x1, y, z)) for (y, z) in points_yz]
    bm.faces.new(a[::-1])
    bm.faces.new(b)
    n = len(points_yz)
    for i in range(n):
        j = (i + 1) % n
        bm.faces.new((a[i], a[j], b[j], b[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return bm


def box(x0, x1, y0, y1, z0, z1):
    return extrude_profile_x([(y0, z0), (y1, z0), (y1, z1), (y0, z1)], x0, x1)


def cylinder_x(cx, cy, cz, radius, x_half, segments=40):
    """Cilindro con eje X (ruedas)."""
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, cap_tris=False, segments=segments,
                          radius1=radius, radius2=radius, depth=2 * x_half)
    # create_cone genera el eje en Z: girarlo a X.
    import mathutils
    bmesh.ops.rotate(bm, verts=bm.verts, cent=(0, 0, 0),
                     matrix=mathutils.Matrix.Rotation(1.5707963, 3, 'Y'))
    bmesh.ops.translate(bm, verts=bm.verts, vec=(cx, cy, cz))
    return bm


def add_bevel(obj, width, segments=3, limit_angle_deg=40.0):
    mod = obj.modifiers.new("Bevel", 'BEVEL')
    mod.width = width
    mod.segments = segments
    mod.limit_method = 'ANGLE'
    import math
    mod.angle_limit = math.radians(limit_angle_deg)
    try:
        mod.harden_normals = True
    except AttributeError:
        pass
    return mod


def export_glb(path):
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format='GLB',
        export_apply=True,
        export_yup=True,
        use_selection=False,
    )


def bake_contact_shadow(parts, root, tmp_png, wheel_keys=("front_left_wheel", "front_right_wheel", "rear_left_wheel", "rear_right_wheel"),
                        res=512, darkness=0.93):
    """Sombra de contacto horneada bajo el coche (plano "contact_shadow" hijo de `root`).

    Silueta cenital del coche -> desenfoque cercano + amplio -> manchas bajo cada neumático. Da "peso" sobre el suelo
    sin sombras en tiempo real (barato en cualquier GPU). La app excluye este plano del encuadre.
    `parts`: dict nombre -> objeto (coordenadas finales: Z arriba, ruedas en z = 0, coche centrado).
    """
    import os
    import numpy as np
    from mathutils import Vector

    objs = [o for o in parts.values() if o is not None]

    def obb(o):
        vs = o.data.vertices
        xs = [v.co.x + o.location.x for v in vs]
        ys = [v.co.y + o.location.y for v in vs]
        return min(xs), min(ys), max(xs), max(ys)

    boxes = [obb(o) for o in objs if len(o.data.vertices)]
    car_mn = Vector((min(b[0] for b in boxes), min(b[1] for b in boxes), 0))
    car_mx = Vector((max(b[2] for b in boxes), max(b[3] for b in boxes), 0))
    size = max(car_mx.x - car_mn.x, car_mx.y - car_mn.y) * 1.35

    scene = bpy.context.scene
    prev_engine = scene.render.engine
    scene.render.engine = 'BLENDER_WORKBENCH'
    scene.display.shading.light = 'FLAT'
    scene.display.shading.color_type = 'SINGLE'
    scene.display.shading.single_color = (0, 0, 0)
    scene.render.film_transparent = True
    scene.render.resolution_x = scene.render.resolution_y = res
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    cam = bpy.data.objects.new("sh_cam", bpy.data.cameras.new("sh_cam"))
    scene.collection.objects.link(cam)
    cam.data.type = 'ORTHO'
    cam.data.ortho_scale = size
    cam.location = (0, 0, 10)
    cam.rotation_euler = (0, 0, 0)
    scene.camera = cam
    scene.render.filepath = tmp_png
    bpy.ops.render.render(write_still=True)
    sil_img = bpy.data.images.load(tmp_png)
    sw, shh = sil_img.size
    sil = np.array(sil_img.pixels[:], dtype=np.float32).reshape(shh, sw, 4)[:, :, 3]
    res = sw

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

    shadow = np.clip(gauss(sil, res * 0.012) * 0.55 + gauss(sil, res * 0.055) * 0.45, 0, 1)
    yy, xx = np.mgrid[0:res, 0:res]
    px_per_m = res / size
    for k in wheel_keys:
        w = parts.get(k)
        if w is None:
            continue
        cx = res / 2 + w.location.x * px_per_m
        cy = res / 2 + w.location.y * px_per_m
        sx, sy = 0.09 * px_per_m, 0.22 * px_per_m
        shadow = np.maximum(shadow, 0.95 * np.exp(-(((xx - cx) / sx) ** 2 + ((yy - cy) / sy) ** 2) / 2))
    shadow = np.clip(shadow, 0, darkness)
    rgba = np.zeros((res, res, 4), dtype=np.float32)
    rgba[:, :, 3] = shadow
    sh_img = bpy.data.images.new("car_contact_shadow", res, res, alpha=True)
    sh_img.pixels = rgba.ravel().tolist()
    sh_img.pack()
    bpy.data.images.remove(sil_img)
    os.remove(tmp_png)

    mat = bpy.data.materials.new("contact_shadow")
    mat.use_nodes = True
    nt = mat.node_tree
    bsdf = nt.nodes.get("Principled BSDF")
    tex = nt.nodes.new("ShaderNodeTexImage")
    tex.image = sh_img
    bsdf.inputs["Base Color"].default_value = (0, 0, 0, 1)
    bsdf.inputs["Roughness"].default_value = 1.0
    nt.links.new(tex.outputs["Alpha"], bsdf.inputs["Alpha"])
    for attr, val in (("surface_render_method", "BLENDED"), ("blend_method", "BLEND")):
        try:
            setattr(mat, attr, val)
        except (AttributeError, TypeError):
            pass
    bm = bmesh.new()
    hs = size / 2
    vs = [bm.verts.new(p) for p in ((-hs, -hs, 0.004), (hs, -hs, 0.004), (hs, hs, 0.004), (-hs, hs, 0.004))]
    face = bm.faces.new(vs)
    uvl = bm.loops.layers.uv.new("UVMap")
    for loop, uv in zip(face.loops, ((0, 0), (1, 0), (1, 1), (0, 1))):
        loop[uvl].uv = uv
    obj = mesh_object("contact_shadow", bm, mat, parent=root)
    bpy.data.objects.remove(cam)
    scene.render.engine = prev_engine
    return obj, size


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


def join(objs, name):
    """Une varios objetos de malla en uno (menos llamadas de dibujo)."""
    objs = [o for o in objs if o is not None]
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


def export_pack_glb(root, out_path, draco=True):
    """Exporta `root` y sus hijos como GLB (Y arriba, Draco)."""
    for o in bpy.data.objects:
        o.select_set(False)

    def sel(o):
        o.select_set(True)
        for c in o.children:
            sel(c)

    sel(root)
    bpy.ops.export_scene.gltf(filepath=out_path, export_format='GLB', export_apply=True, export_yup=True, use_selection=True,
                              export_draco_mesh_compression_enable=draco, export_draco_mesh_compression_level=6,
                              export_draco_position_quantization=14, export_draco_normal_quantization=10)
