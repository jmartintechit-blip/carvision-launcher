import { NextResponse } from "next/server";
import { APP_PRICE_CENTS, SITE_URL } from "@/lib/config";
import { getStripe } from "@/lib/stripe";

// Formulario de compra de la app → página de pago de Stripe.
export async function POST(req: Request) {
  const form = await req.formData();
  if (form.get("acepto") !== "si") {
    return NextResponse.redirect(`${SITE_URL}/?falta=acepto#comprar`, 303);
  }
  const stripe = getStripe();
  if (!stripe) return NextResponse.redirect(`${SITE_URL}/?pago=no-disponible#comprar`, 303);
  const session = await stripe.checkout.sessions.create({
    mode: "payment",
    locale: "es",
    customer_creation: "always",
    line_items: [
      {
        quantity: 1,
        price_data: {
          currency: "eur",
          unit_amount: APP_PRICE_CENTS,
          product_data: {
            name: "CarVision · pantalla de inicio para radio Android",
            description: "App + catálogo de coches + guía paso a paso. Descarga inmediata.",
          },
        },
      },
    ],
    metadata: { kind: "app", consent: "descarga-inmediata-sin-desistimiento" },
    success_url: `${SITE_URL}/gracias?session_id={CHECKOUT_SESSION_ID}`,
    cancel_url: `${SITE_URL}/#comprar`,
  });
  return NextResponse.redirect(session.url!, 303);
}
