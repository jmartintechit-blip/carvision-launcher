"""Informe de un .blend: objetos, jerarquía, mallas, materiales, texturas y dimensiones.

Uso:
  blender -b --factory-startup --disable-autoexec <archivo.blend> --python blender/scripts/inspect_blend.py
"""
import bpy

print("=== CV_INSPECT ===")
print("escena:", bpy.context.scene.name, "| unidades:", bpy.context.scene.unit_settings.system,
      bpy.context.scene.unit_settings.scale_length)
total = 0
for o in sorted(bpy.data.objects, key=lambda o: o.name):
    info = ""
    if o.type == 'MESH':
        me = o.data
        tris = sum(len(p.vertices) - 2 for p in me.polygons)
        total += tris
        mats = ",".join(m.name for m in me.materials if m)
        info = f"tris={tris} mats=[{mats}]"
    d = o.dimensions
    print(f"{o.type:6s} {o.name!r:40s} parent={o.parent.name if o.parent else '-':20s} "
          f"loc=({o.location.x:+.2f},{o.location.y:+.2f},{o.location.z:+.2f}) "
          f"rot=({o.rotation_euler.x:+.2f},{o.rotation_euler.y:+.2f},{o.rotation_euler.z:+.2f}) "
          f"scale=({o.scale.x:.2f},{o.scale.y:.2f},{o.scale.z:.2f}) dim=({d.x:.2f},{d.y:.2f},{d.z:.2f}) "
          f"mods={[m.type for m in o.modifiers]} {info}")
print("triángulos totales:", total)
print("--- materiales")
for m in bpy.data.materials:
    col = None
    if m.use_nodes and m.node_tree:
        b = m.node_tree.nodes.get("Principled BSDF")
        if b:
            col = tuple(round(c, 2) for c in b.inputs["Base Color"].default_value)
        texs = [n.image.name for n in m.node_tree.nodes if n.type == 'TEX_IMAGE' and n.image]
    else:
        texs = []
    print(f"  {m.name!r} users={m.users} color={col} texturas={texs}")
print("--- imágenes")
for i in bpy.data.images:
    print(f"  {i.name!r} {i.size[0]}x{i.size[1]} packed={bool(i.packed_file)} path={i.filepath!r}")
print("--- textos incrustados (scripts)")
for t in bpy.data.texts:
    print(f"  {t.name!r} líneas={len(t.lines)}")
print("=== FIN ===")
