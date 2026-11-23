import Stripe from "stripe";

let client: Stripe | null = null;

/** Cliente de Stripe, o null si la web aún no tiene la clave (entonces el botón de pago lo explica). */
export function getStripe(): Stripe | null {
  const key = process.env.STRIPE_SECRET_KEY;
  if (!key) return null;
  client ??= new Stripe(key);
  return client;
}
