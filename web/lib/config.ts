// Ajustes de la web leídos del entorno (panel de Vercel). Nada secreto se escribe en el código.
// Sin SITE_URL (versión de prueba en Vercel, sin dominio) se usa la dirección .vercel.app que pone Vercel.
const vercelUrl = process.env.VERCEL_PROJECT_PRODUCTION_URL || process.env.VERCEL_URL;
export const SITE_URL = (process.env.SITE_URL || (vercelUrl ? `https://${vercelUrl}` : "http://localhost:3100")).replace(/\/$/, "");
export const APP_PRICE_CENTS = Number(process.env.APP_PRICE_CENTS || 200);
export const SERVICE_PRICE_CENTS = Number(process.env.SERVICE_PRICE_CENTS || 200);
export const CONTACT_EMAIL = process.env.CONTACT_EMAIL || "hola@carvisiones.es";

export function euros(cents: number): string {
  const v = cents / 100;
  return (Number.isInteger(v) ? v.toString() : v.toFixed(2).replace(".", ",")) + " €";
}
