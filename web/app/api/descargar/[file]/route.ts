import fs from "node:fs";
import { Readable } from "node:stream";
import { filePath, listFiles } from "@/lib/files";
import { verifyAccess } from "@/lib/token";

// Descarga protegida: solo con el enlace firmado de una compra de la app.
export async function GET(req: Request, ctx: { params: Promise<{ file: string }> }) {
  const { file } = await ctx.params;
  const access = verifyAccess(new URL(req.url).searchParams.get("t"));
  if (!access || access.k !== "app") return new Response("Enlace no válido. Ábrelo desde tu Área de cliente.", { status: 403 });
  const f = listFiles().find((x) => x.id === file);
  if (!f) return new Response("Archivo no encontrado", { status: 404 });
  const p = filePath(f);
  if (!fs.existsSync(p)) return new Response("Archivo no disponible ahora mismo", { status: 503 });
  const stream = Readable.toWeb(fs.createReadStream(p)) as ReadableStream;
  return new Response(stream, {
    headers: {
      "Content-Type": f.name.endsWith(".apk") ? "application/vnd.android.package-archive" : "application/zip",
      "Content-Length": String(fs.statSync(p).size),
      "Content-Disposition": `attachment; filename="${f.name}"`,
      "Cache-Control": "private, no-store",
    },
  });
}
