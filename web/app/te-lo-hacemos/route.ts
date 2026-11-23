import { NextResponse } from "next/server";
import { SERVICE_PRICE_CENTS, SITE_URL } from "@/lib/config";
import { getStripe } from "@/lib/stripe";

const FIELDS = ["marca", "modelo", "anio", "puertas", "color"] as const;

// «Te lo hacemos»: el cliente nos dice su coche y paga; los datos van con el pago (metadata de Stripe).
export async function POST(req: Request) {
  const form = await req.formData();
  const back = (form.get("volver") as string) || "/#te-lo-hacemos";
  const car: Record<string, string> = {};
  for (const f of FIELDS) car[f] = String(form.get(f) || "").trim().slice(0, 80);
  if (!car.marca || !car.modelo || !car.anio) return NextResponse.redirect(`${SITE_URL}${back}?falta=coche`, 303);
  if (form.get("acepto") !== "si") return NextResponse.redirect(`${SITE_URL}${back}?falta=acepto`, 303);
  const stripe = getStripe();
  if (!stripe) return NextResponse.redirect(`${SITE_URL}${back}?pago=no-disponible`, 303);
  const label = `${car.marca} ${car.modelo} (${car.anio})`;
  const session = await stripe.checkout.sessions.create({
    mode: "payment",
    locale: "es",
    customer_creation: "always",
    line_items: [
      {
        quantity: 1,
        price_data: {
          currency: "eur",
          unit_amount: SERVICE_PRICE_CENTS,
          product_data: { name: `Te lo hacemos: ${label}`, description: "Preparamos tu coche en 3D para CarVision." },
        },
      },
    ],
    metadata: { kind: "service", ...car },
    payment_intent_data: { description: `Te lo hacemos: ${label}, ${car.puertas} puertas, ${car.color}` },
    success_url: `${SITE_URL}/gracias?session_id={CHECKOUT_SESSION_ID}`,
    cancel_url: `${SITE_URL}${back}`,
  });
  return NextResponse.redirect(session.url!, 303);
}
