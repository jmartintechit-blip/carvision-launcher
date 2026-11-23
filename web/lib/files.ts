import fs from "node:fs";
import path from "node:path";

export type DownloadFile = { id: string; name: string; title: string; kind: "app" | "car" | "code"; make?: string; bytes: number };

const dir = path.join(process.cwd(), "private");

export function listFiles(): DownloadFile[] {
  try {
    return JSON.parse(fs.readFileSync(path.join(dir, "files.json"), "utf8"));
  } catch {
    return [];
  }
}

export function filePath(f: DownloadFile): string {
  return path.join(dir, f.name);
}

export function readPrompt(which: "gratis" | "archivos" = "gratis"): string {
  try {
    return fs.readFileSync(path.join(dir, which === "gratis" ? "prompt.txt" : "prompt_archivos.txt"), "utf8");
  } catch {
    return "";
  }
}

export function sizeLabel(bytes: number): string {
  return bytes > 1e6 ? `${(bytes / 1e6).toFixed(1).replace(".", ",")} MB` : `${Math.round(bytes / 1e3)} KB`;
}
