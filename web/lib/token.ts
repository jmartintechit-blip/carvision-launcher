import crypto from "node:crypto";

// Enlace del Área de cliente: datos de la compra + firma HMAC. Sin base de datos: la compra está en Stripe y el
// enlace solo lo puede fabricar quien conoce DOWNLOAD_SECRET (el servidor).
export type Access = {
  e: string; // correo del comprador
  s: string; // sesión de pago de Stripe
  k: "app" | "service"; // qué compró
  t: number; // cuándo se emitió (ms)
};

function secret(): string {
  const s = process.env.DOWNLOAD_SECRET;
  if (s && s.length >= 32) return s;
  if (process.env.NODE_ENV !== "production") return "solo-para-pruebas-en-local-no-usar-en-produccion-000";
  throw new Error("DOWNLOAD_SECRET no está configurado (mínimo 32 caracteres)");
}

export function signAccess(a: Access): string {
  const body = Buffer.from(JSON.stringify(a)).toString("base64url");
  const mac = crypto.createHmac("sha256", secret()).update(body).digest("base64url");
  return `${body}.${mac}`;
}

export function verifyAccess(token: string | null | undefined): Access | null {
  if (!token) return null;
  const [body, mac] = token.split(".");
  if (!body || !mac) return null;
  const expected = crypto.createHmac("sha256", secret()).update(body).digest("base64url");
  const a = Buffer.from(mac);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return null;
  try {
    const v = JSON.parse(Buffer.from(body, "base64url").toString("utf8")) as Access;
    return typeof v.e === "string" && (v.k === "app" || v.k === "service") ? v : null;
  } catch {
    return null;
  }
}
