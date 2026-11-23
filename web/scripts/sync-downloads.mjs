// Copia a web/private lo que se descarga desde el Área de cliente: la APK, los coches del catálogo y el texto
// para la IA. Se ejecuta antes de publicar: `npm run sync`.
import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..", "..");
const out = path.resolve(import.meta.dirname, "..", "private");
fs.mkdirSync(out, { recursive: true });

const files = [
  { id: "app", src: "dist/CarVision.apk", name: "CarVision.apk", title: "CarVision (la app)", kind: "app" },
  { id: "focus", src: "dist/catalogo/ford_focus_mk3_5p.zip", name: "ford_focus_mk3_5p.zip",
    title: "Ford Focus MK3 · 5 puertas (2011–2014)", kind: "car", make: "Ford" },
  { id: "golf", src: "dist/catalogo/vw_golf_6_5p.zip", name: "vw_golf_6_5p.zip",
    title: "Volkswagen Golf VI · 5 puertas (2008–2012)", kind: "car", make: "Volkswagen" },
  { id: "codigo", src: "dist/CarVision_codigo_fuente.zip", name: "CarVision_codigo_fuente.zip",
    title: "Código fuente y generador de coches", kind: "code" },
];
const list = [];
for (const f of files) {
  const src = path.join(root, f.src);
  if (!fs.existsSync(src)) throw new Error(`Falta ${f.src}`);
  fs.copyFileSync(src, path.join(out, f.name));
  const { src: _s, ...rest } = f;
  list.push({ ...rest, bytes: fs.statSync(src).size });
  console.log("copiado", f.name);
}
fs.writeFileSync(path.join(out, "files.json"), JSON.stringify(list, null, 2));

// Textos para la IA: el bloque ```text de cada prompt (gratis: D-026/D-030; con archivos: D-031).
for (const [src, dst] of [["PROMPT_IA_GRATUITA.md", "prompt.txt"], ["PROMPT_IA_CON_ARCHIVOS.md", "prompt_archivos.txt"]]) {
  const md = fs.readFileSync(path.join(root, "docs/prompts", src), "utf8");
  const m = md.replace(/\r\n/g, "\n").match(/```text\n([\s\S]*?)```/);
  if (!m) throw new Error(`No se encuentra el bloque del prompt en ${src}`);
  fs.writeFileSync(path.join(out, dst), m[1].trim() + "\n");
  console.log(dst, "listo");
}
