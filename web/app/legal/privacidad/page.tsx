import { Pie, Top } from "@/components/Chrome";
import Titular from "@/components/Titular";
import { CONTACT_EMAIL } from "@/lib/config";

export const metadata = { title: "Privacidad · CarVision" };

export default function Privacidad() {
  return (
    <>
      <Top />
      <main className="wrap hoja">
        <h1>Privacidad</h1>
        <h2>Responsable</h2>
        <p><Titular />. Contacto: {CONTACT_EMAIL}.</p>
        <h2>Qué datos usamos y para qué</h2>
        <p>
          Tu correo y los datos de pago, para venderte la app, darte acceso a las descargas y, si lo pides, preparar tu
          coche («Te lo hacemos»: marca, modelo, año, puertas y color). Base legal: el contrato de compra.
        </p>
        <p>No usamos cookies de publicidad ni de analítica. La web solo usa lo técnicamente necesario para funcionar.</p>
        <p>La app CarVision no envía tus datos a ningún sitio: todo se guarda en la radio.</p>
        <h2>Quién más los trata</h2>
        <p>
          Stripe (cobro; ve los datos de la tarjeta, nosotros no), Vercel (alojamiento de la web) y Resend (envío del correo
          con tu enlace). Pueden tratar datos fuera de la UE con las garantías del RGPD (cláusulas contractuales tipo).
        </p>
        <h2>Cuánto tiempo</h2>
        <p>Lo que exija la ley para las facturas y compras (en general, 6 años); el resto, mientras uses tu Área de cliente.</p>
        <h2>Tus derechos</h2>
        <p>
          Puedes pedir acceso, rectificación, supresión, oposición, limitación y portabilidad escribiendo a {CONTACT_EMAIL}.
          Si no te atendemos bien, puedes reclamar ante la Agencia Española de Protección de Datos (aepd.es).
        </p>
      </main>
      <Pie />
    </>
  );
}
