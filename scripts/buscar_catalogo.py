"""Busca candidatos en Sketchfab (API pública de búsqueda, sin descargar modelos) para la lista de coches del catálogo."""
import json
import os
import re
import time
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))  # candidatos.json se guarda junto al script

# (id, nombre visible, generación/años, palabras que deben aparecer en el título, consultas)
CARS = [
    ("vw_golf_5", "Volkswagen Golf V", "2003-2008", ["golf"], ["golf mk5", "golf 5", "golf v"]),
    ("vw_golf_6", "Volkswagen Golf VI", "2008-2012", ["golf"], ["golf mk6", "golf 6", "golf vi"]),
    ("vw_golf_7", "Volkswagen Golf VII", "2012-2019", ["golf"], ["golf mk7", "golf 7", "golf vii"]),
    ("seat_ibiza_6l", "Seat Ibiza 6L", "2002-2008", ["ibiza"], ["seat ibiza 6l", "seat ibiza 2005"]),
    ("seat_ibiza_6j", "Seat Ibiza 6J", "2008-2017", ["ibiza"], ["seat ibiza 6j", "seat ibiza 2010", "seat ibiza"]),
    ("seat_leon_1", "Seat León MK1", "1999-2005", ["leon", "león"], ["seat leon mk1", "seat leon 1m"]),
    ("seat_leon_2", "Seat León MK2", "2005-2012", ["leon", "león"], ["seat leon mk2", "seat leon 1p"]),
    ("seat_leon_3", "Seat León MK3", "2012-2020", ["leon", "león"], ["seat leon mk3", "seat leon 5f", "seat leon 2014"]),
    ("renault_clio_3", "Renault Clio III", "2005-2012", ["clio"], ["renault clio 3", "renault clio iii"]),
    ("renault_clio_4", "Renault Clio IV", "2012-2019", ["clio"], ["renault clio 4", "renault clio iv", "renault clio"]),
    ("ford_focus_2", "Ford Focus MK2", "2004-2011", ["focus"], ["ford focus mk2", "ford focus 2008"]),
    ("renault_megane_2", "Renault Mégane II", "2002-2009", ["megane", "mégane"], ["renault megane 2", "renault megane ii"]),
    ("renault_megane_3", "Renault Mégane III", "2008-2016", ["megane", "mégane"], ["renault megane 3", "renault megane iii"]),
    ("peugeot_206", "Peugeot 206", "1998-2010", ["206"], ["peugeot 206"]),
    ("peugeot_207", "Peugeot 207", "2006-2014", ["207"], ["peugeot 207"]),
    ("peugeot_208", "Peugeot 208", "2012-2019", ["208"], ["peugeot 208"]),
    ("peugeot_307", "Peugeot 307", "2001-2008", ["307"], ["peugeot 307"]),
    ("peugeot_308", "Peugeot 308", "2007-2021", ["308"], ["peugeot 308"]),
    ("opel_corsa_d", "Opel Corsa D", "2006-2014", ["corsa"], ["opel corsa d", "opel corsa"]),
    ("opel_astra_h", "Opel Astra H", "2004-2010", ["astra"], ["opel astra h", "opel astra"]),
    ("opel_astra_j", "Opel Astra J", "2009-2015", ["astra"], ["opel astra j"]),
    ("citroen_c4", "Citroën C4", "2004-2018", ["c4"], ["citroen c4"]),
    ("citroen_c3", "Citroën C3", "2002-2016", ["c3"], ["citroen c3"]),
    ("citroen_xsara_picasso", "Citroën Xsara Picasso", "1999-2012", ["picasso"], ["xsara picasso", "citroen picasso"]),
    ("vw_polo_9n", "Volkswagen Polo 9N", "2001-2009", ["polo"], ["vw polo 9n", "volkswagen polo 2005"]),
    ("vw_polo_6r", "Volkswagen Polo 6R", "2009-2017", ["polo"], ["vw polo 6r", "volkswagen polo 2012"]),
    ("toyota_corolla", "Toyota Corolla / Auris", "2002-2018", ["corolla", "auris"], ["toyota corolla", "toyota auris"]),
    ("dacia_sandero", "Dacia Sandero", "2008-2020", ["sandero"], ["dacia sandero"]),
    ("dacia_duster", "Dacia Duster", "2010-2020", ["duster"], ["dacia duster"]),
    ("nissan_qashqai", "Nissan Qashqai", "2007-2021", ["qashqai"], ["nissan qashqai"]),
    ("ford_fiesta", "Ford Fiesta MK6/MK7", "2002-2017", ["fiesta"], ["ford fiesta"]),
    ("audi_a3", "Audi A3 8P/8V", "2003-2020", ["a3"], ["audi a3 8p", "audi a3 8v", "audi a3"]),
    ("bmw_serie3", "BMW Serie 3 E90/F30", "2005-2018", ["e90", "e91", "f30", "3 series", "320", "330", "325"],
     ["bmw e90", "bmw f30", "bmw 3 series"]),
    ("bmw_serie1", "BMW Serie 1 E87/F20", "2004-2019", ["e87", "e81", "f20", "1 series", "118", "120", "116"],
     ["bmw e87", "bmw f20", "bmw 1 series"]),
    ("mercedes_c", "Mercedes Clase C W204", "2007-2014", ["c-class", "c class", "w204", "c200", "c220", "c63"],
     ["mercedes w204", "mercedes c class"]),
    ("skoda_octavia", "Škoda Octavia II/III", "2004-2020", ["octavia"], ["skoda octavia"]),
    ("kia_ceed", "Kia Ceed", "2006-2018", ["ceed", "cee'd"], ["kia ceed"]),
    ("kia_sportage", "Kia Sportage", "2010-2021", ["sportage"], ["kia sportage"]),
    ("hyundai_tucson", "Hyundai Tucson / ix35", "2009-2020", ["tucson", "ix35"], ["hyundai tucson", "hyundai ix35"]),
    ("hyundai_i30", "Hyundai i30", "2007-2020", ["i30"], ["hyundai i30"]),
    ("seat_altea", "Seat Altea", "2004-2015", ["altea"], ["seat altea"]),
    ("fiat_500", "Fiat 500", "2007-2020", ["500"], ["fiat 500"]),
    ("peugeot_3008", "Peugeot 3008", "2009-2020", ["3008"], ["peugeot 3008"]),
    ("toyota_yaris", "Toyota Yaris", "2005-2020", ["yaris"], ["toyota yaris"]),
    ("vw_passat", "Volkswagen Passat B6/B7", "2005-2014", ["passat"], ["vw passat b6", "vw passat b7", "volkswagen passat"]),
    ("citroen_berlingo", "Citroën Berlingo / Peugeot Partner", "2008-2018", ["berlingo", "partner"],
     ["citroen berlingo", "peugeot partner"]),
]

OK_LIC = {"CC Attribution": "CC BY", "CC0 Public Domain": "CC0", "CC Attribution-ShareAlike": "CC BY-SA"}
RIP = re.compile(r"\b(gta|forza|ripped|rip|mods?|exported from|assetto|beamng|need for speed|nfs|gran turismo|"
                 r"test drive|ets2|euro truck|city car driving|car parking|extracted|fivem|game ready|from the game|"
                 r"in-game|ingame|roblox|drift hunters|crew 2|midtown|lods?)\b", re.I)
DOORS = re.compile(r"(open(ing)? doors?|separated|separate parts|rigged|doors? open|animated doors|opening|"
                   r"detachable|all parts)", re.I)
LOWPOLY = re.compile(r"\b(low ?poly|lowpoly|stylized|cartoon|toy|voxel|psx|ps1)\b", re.I)


def api(params):
    url = "https://api.sketchfab.com/v3/search?" + urllib.parse.urlencode(params)
    for _ in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "catalog-check"}),
                                        timeout=30) as r:
                return json.load(r)
        except Exception as e:  # noqa: BLE001
            print("  reintento", e)
            time.sleep(3)
    return {"results": []}


def main():
    out = {}
    for cid, name, years, must, queries in CARS:
        seen = {}
        for q in queries:
            for lic in ("by", "cc0"):
                d = api({"type": "models", "q": q, "downloadable": "true", "license": lic, "count": 24,
                         "sort_by": "-likeCount"})
                for m in d.get("results", []):
                    seen[m["uid"]] = m
                time.sleep(0.4)
        cands = []
        for m in seen.values():
            lic = (m.get("license") or {}).get("label") if isinstance(m.get("license"), dict) else m.get("license")
            if lic not in OK_LIC:
                continue
            title = m["name"].lower()
            if not any(w in title for w in must):
                continue
            text = " ".join([m["name"], m.get("description") or "", " ".join(t["name"] for t in m.get("tags", []))])
            rip = sorted({x.lower() for x in RIP.findall(text)} if RIP.search(text) else set())
            rip = sorted({mm.group(0).lower() for mm in RIP.finditer(text)})
            faces = m.get("faceCount") or 0
            cands.append({
                "uid": m["uid"], "name": m["name"], "author": m["user"]["username"],
                "license": OK_LIC[lic], "faces": faces, "likes": m.get("likeCount", 0),
                "url": "https://sketchfab.com/3d-models/" + m["uid"],
                "rip_words": rip, "door_hint": bool(DOORS.search(text)), "lowpoly": bool(LOWPOLY.search(text)),
                "desc": (m.get("description") or "")[:400].replace("\n", " "),
            })
        def score(c):
            s = 0
            if c["rip_words"]:
                s -= 100
            if c["lowpoly"] or c["faces"] < 20000:
                s -= 30
            if c["faces"] > 1500000:
                s -= 20
            if c["door_hint"]:
                s += 15
            s += min(c["likes"], 200) / 10
            return s
        cands.sort(key=score, reverse=True)
        out[cid] = {"name": name, "years": years, "candidates": cands}
        good = [c for c in cands if not c["rip_words"] and not c["lowpoly"] and c["faces"] >= 20000]
        print(f"{name:38s} candidatos={len(cands):3d} sin_banderas={len(good):3d}")
    json.dump(out, open(os.path.join(HERE, "candidatos.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)


main()
