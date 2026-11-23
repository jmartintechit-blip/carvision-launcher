"""Valida un GLB de vehículo contra un manifest.json de CarVision (Python estándar, sin dependencias).

Comprueba:
- que existe el nodo raíz y cada nodo de pieza animada declarado en el manifiesto;
- que las piezas animadas no tienen rotación ni escala propias (el eje del manifiesto es local);
- muestra la posición de cada bisagra para revisarla.

Uso:  python blender/scripts/validate_glb.py vehicle-packs/<pack>/manifest.json
"""
import json
import os
import struct
import sys


def read_glb_json(path):
    with open(path, "rb") as f:
        magic, version, _length = struct.unpack("<4sII", f.read(12))
        if magic != b"glTF" or version != 2:
            raise SystemExit(f"{path}: no es un GLB 2.0")
        chunk_len, chunk_type = struct.unpack("<II", f.read(8))
        if chunk_type != 0x4E4F534A:
            raise SystemExit(f"{path}: el primer bloque no es JSON")
        return json.loads(f.read(chunk_len))


def main():
    manifest_path = sys.argv[1]
    manifest = json.load(open(manifest_path, encoding="utf-8"))
    glb = os.path.join(os.path.dirname(manifest_path), manifest["model"]["file"])
    gltf = read_glb_json(glb)
    nodes = gltf.get("nodes", [])
    by_name = {n.get("name"): (i, n) for i, n in enumerate(nodes)}
    errors, warnings = [], []

    root = manifest["model"].get("rootNode", "car_root")
    if root not in by_name:
        errors.append(f"Falta el nodo raíz '{root}'")

    for part in manifest.get("parts", []):
        name = part["node"]
        if name not in by_name:
            errors.append(f"Falta el nodo '{name}' (pieza {part['id']})")
            continue
        _, n = by_name[name]
        rot = n.get("rotation", [0, 0, 0, 1])
        scale = n.get("scale", [1, 1, 1])
        if any(abs(a - b) > 1e-4 for a, b in zip(rot, [0, 0, 0, 1])):
            warnings.append(f"'{name}' tiene rotación propia {rot}: el eje del manifiesto se interpreta en ese espacio rotado")
        if any(abs(s - 1) > 1e-4 for s in scale):
            warnings.append(f"'{name}' tiene escala {scale}")
        t = n.get("translation", [0, 0, 0])
        print(f"  bisagra {name:22s} ({t[0]:+.3f}, {t[1]:+.3f}, {t[2]:+.3f})  eje {part['axis']}  {part['openAngleDeg']:+.0f}°")

    tris = 0
    for mesh in gltf.get("meshes", []):
        for prim in mesh.get("primitives", []):
            if "indices" in prim:
                tris += gltf["accessors"][prim["indices"]]["count"] // 3
    size_mb = os.path.getsize(glb) / 1e6
    print(f"  nodos: {len(nodes)}  materiales: {len(gltf.get('materials', []))}  triángulos: {tris:,}  tamaño: {size_mb:.2f} MB")

    for w in warnings:
        print("AVISO:", w)
    for e in errors:
        print("ERROR:", e)
    print("RESULTADO:", "OK" if not errors else "FALLA")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
