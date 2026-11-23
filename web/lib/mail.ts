import { CONTACT_EMAIL, SITE_URL } from "./config";

// Correo con el enlace del Área de cliente (Resend). Sin RESEND_API_KEY no se envía nada y la web lo dice.
export async function sendAccessEmail(to: string, link: string, what: "app" | "service"): Promise<boolean> {
  const key = process.env.RESEND_API_KEY;
  if (!key) return false;
  const subject = what === "app" ? "Tu CarVision: descargas e instrucciones" : "Hemos recibido tu pedido «Te lo hacemos»";
  const intro =
    what === "app"
      ? "Gracias por comprar CarVision. Desde tu Área de cliente descargas la app y tu coche, y copias el texto que te guía paso a paso."
      : "Hemos recibido los datos de tu coche. Te escribimos a este correo cuando esté listo; lo descargarás desde tu Área de cliente.";
  const html = `<div style="font-family:Arial,sans-serif;font-size:16px;line-height:1.5;color:#161A2E">
<p>${intro}</p>
<p><a href="${link}" style="display:inline-block;background:#4B47D6;color:#fff;padding:12px 20px;border-radius:10px;text-decoration:none">Abrir mi Área de cliente</a></p>
<p style="font-size:14px;color:#55607a">Guarda este correo: el enlace es tu acceso. Si lo pierdes, pídelo de nuevo en ${SITE_URL}/area.<br>
¿Dudas? Escríbenos a ${CONTACT_EMAIL}.</p></div>`;
  const r = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${key}`, "Content-Type": "application/json" },
    body: JSON.stringify({ from: process.env.MAIL_FROM || `CarVision <${CONTACT_EMAIL}>`, to: [to], subject, html }),
  });
  return r.ok;
}
