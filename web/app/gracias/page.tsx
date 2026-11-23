import Link from "next/link";
import { Pie, Top } from "@/components/Chrome";
import { CONTACT_EMAIL, SITE_URL } from "@/lib/config";
import { sendAccessEmail } from "@/lib/mail";
import { getStripe } from "@/lib/stripe";
import { signAccess } from "@/lib/token";

export const dynamic = "force-dynamic";
export const metadata = { title: "Gracias · CarVision", robots: { index: false } };

type Props = { searchParams: Promise<{ session_id?: string }> };

export default async function Gracias({ searchParams }: Props) {
  const { session_id } = await searchParams;
  const stripe = getStripe();
  let estado: "ok" | "pendiente" | "error" = "error";
  let link = "";
  let kind: "app" | "service" = "app";
  let car = "";
  let mailed = false;
  if (stripe && session_id && /^cs_[A-Za-z0-9_]+$/.test(session_id)) {
    try {
      const s = await stripe.checkout.sessions.retrieve(session_id);
      const email = s.customer_details?.email?.toLowerCase();
      kind = s.metadata?.kind === "service" ? "service" : "app";
      car = [s.metadata?.marca, s.metadata?.modelo, s.metadata?.anio && `(${s.metadata.anio})`].filter(Boolean).join(" ");
      if (s.payment_status === "paid" && email) {
        link = `${SITE_URL}/area?t=${signAccess({ e: email, s: s.id, k: kind, t: Date.now() })}`;
        mailed = await sendAccessEmail(email, link, kind).catch(() => false);
        estado = "ok";
      } else {
        estado = "pendiente";
      }
    } catch {
      estado = "error";
    }
  }
  return (
    <>
      <Top />
      <main className="wrap hoja">
        {estado === "ok" && kind === "app" && (
          <>
            <h1>Listo. Ya es tuyo.</h1>
            <p style={{ marginTop: 16 }}>
              En tu Área de cliente tienes la app, los coches y el texto para que una IA te guíe en la instalación.
            </p>
            <p>{mailed ? "También te hemos enviado el enlace por correo. Guárdalo: es tu acceso." : "Guarda esta página en favoritos: el enlace es tu acceso."}</p>
            <p style={{ marginTop: 24 }}><a className="btn" href={link}>Abrir mi Área de cliente</a></p>
          </>
        )}
        {estado === "ok" && kind === "service" && (
          <>
            <h1>Pedido recibido.</h1>
            <p style={{ marginTop: 16 }}>Vamos a preparar tu {car || "coche"}. Te escribimos al correo de la compra cuando esté listo.</p>
            <p style={{ marginTop: 24 }}><a className="btn" href={link}>Ver mi pedido</a></p>
          </>
        )}
        {estado === "pendiente" && (
          <>
            <h1>Estamos confirmando el pago.</h1>
            <p style={{ marginTop: 16 }}>Algunos métodos de pago tardan unos minutos. Recarga esta página en un momento.</p>
          </>
        )}
        {estado === "error" && (
          <>
            <h1>No encontramos esta compra.</h1>
            <p style={{ marginTop: 16 }}>
              Si has pagado, entra en <Link href="/area">Mi área</Link> con tu correo y te enviamos el enlace. Si no llega,
              escríbenos a <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>.
            </p>
          </>
        )}
      </main>
      <Pie />
    </>
  );
}
