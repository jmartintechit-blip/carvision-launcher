import { NextResponse } from "next/server";
import { SITE_URL } from "@/lib/config";
import { sendAccessEmail } from "@/lib/mail";
import { getStripe } from "@/lib/stripe";
import { signAccess } from "@/lib/token";

// «He perdido mi enlace»: si ese correo tiene una compra pagada, se le envía de nuevo. La respuesta es siempre la
// misma, haya compra o no, para no revelar quién ha comprado.
export async function POST(req: Request) {
  const form = await req.formData();
  const email = String(form.get("email") || "").trim().toLowerCase();
  const stripe = getStripe();
  if (stripe && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    const list = await stripe.checkout.sessions.list({ customer_details: { email }, status: "complete", limit: 20 });
    const paid = list.data.filter((s) => s.payment_status === "paid");
    const app = paid.find((s) => s.metadata?.kind === "app") || paid[0];
    if (app) {
      const k = app.metadata?.kind === "service" ? "service" : "app";
      const t = signAccess({ e: email, s: app.id, k, t: Date.now() });
      await sendAccessEmail(email, `${SITE_URL}/area?t=${t}`, k);
    }
  }
  return NextResponse.redirect(`${SITE_URL}/area?enviado=1`, 303);
}
