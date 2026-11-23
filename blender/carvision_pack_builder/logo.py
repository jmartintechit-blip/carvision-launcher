# SPDX-License-Identifier: GPL-3.0-or-later
# CarVision Pack Builder (complemento de Blender). Ver DECISIONS.md, D-025.
"""Logotipo de la marca para el pack: quita el fondo si no es transparente, recorta y deja un PNG de 1024 px máx.

Funciona dentro de Blender (sin PIL): lee y escribe con bpy.data.images y procesa con numpy.
Fondo: se toma como fondo lo que se parece a los colores del borde (blanco, gris o cuadros «falsos» de transparencia)
y está conectado con el borde; así no se borra el blanco que forme parte del propio logotipo.
"""
import os

import bpy
import numpy as np


def _dominant_border_colors(rgb, k=2):
    border = np.concatenate([rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]])
    q = np.round(border * 15) / 15
    vals, counts = np.unique(q.reshape(-1, 3), axis=0, return_counts=True)
    order = np.argsort(-counts)
    return [vals[i] for i in order[:k] if counts[i] > 0.08 * len(border)] or [vals[order[0]]]


def _connected_to_border(cand, max_iter=4000):
    mask = np.zeros_like(cand)
    mask[0, :] = cand[0, :]
    mask[-1, :] = cand[-1, :]
    mask[:, 0] |= cand[:, 0]
    mask[:, -1] |= cand[:, -1]
    for _ in range(max_iter):
        grown = mask.copy()
        grown[1:, :] |= mask[:-1, :]
        grown[:-1, :] |= mask[1:, :]
        grown[:, 1:] |= mask[:, :-1]
        grown[:, :-1] |= mask[:, 1:]
        grown &= cand
        if (grown == mask).all():
            break
        mask = grown
    return mask


def process_logo(src, dst, max_side=1024, tolerance=0.12):
    """Prepara el logotipo. Devuelve un texto corto con lo que se hizo."""
    img = bpy.data.images.load(src)
    w, h = img.size
    px = np.array(img.pixels[:], dtype=np.float32).reshape(h, w, 4)
    bpy.data.images.remove(img)
    did = []

    has_alpha = (px[..., 3] < 0.98).mean() > 0.02
    if not has_alpha:
        rgb = px[..., :3]
        cand = np.zeros((h, w), dtype=bool)
        for col in _dominant_border_colors(rgb):
            cand |= np.abs(rgb - col).max(axis=2) < tolerance
        bg = _connected_to_border(cand)
        px[..., 3] = np.where(bg, 0.0, 1.0)
        did.append("fondo quitado")

    alpha = px[..., 3] > 0.05
    if not alpha.any():
        raise ValueError("la imagen se ha quedado vacía al quitar el fondo")
    ys, xs = np.where(alpha)
    pad = int(0.04 * max(np.ptp(ys), np.ptp(xs))) + 2
    y0, y1 = max(0, ys.min() - pad), min(h, ys.max() + pad + 1)
    x0, x1 = max(0, xs.min() - pad), min(w, xs.max() + pad + 1)
    px = px[y0:y1, x0:x1]
    ch, cw = px.shape[:2]
    if (ch, cw) != (h, w):
        did.append("recortado")

    out = bpy.data.images.new("cv_logo", cw, ch, alpha=True)
    out.pixels = px.ravel().tolist()
    if max(cw, ch) > max_side:
        f = max_side / max(cw, ch)
        out.scale(max(1, int(cw * f)), max(1, int(ch * f)))
        did.append(f"reducido a {out.size[0]}x{out.size[1]}")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    out.filepath_raw = dst
    out.file_format = 'PNG'
    out.save()
    bpy.data.images.remove(out)
    return ", ".join(did) or "sin cambios"
