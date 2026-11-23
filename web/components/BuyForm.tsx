import Link from "next/link";

// Formulario de compra de la app (casilla de desistimiento + pago en Stripe). Se usa en la portada.
export default function BuyForm({ price, falta, pago }: { price: string; falta?: string; pago?: string }) {
  return (
    <form className="formu" method="post" action="/comprar">
      {falta === "acepto" && <p className="aviso" role="alert">Marca la casilla para continuar: la descarga es inmediata.</p>}
      {pago === "no-disponible" && <p className="aviso" role="alert">El pago aún no está activo. Vuelve en unas horas.</p>}
      <label className="check">
        <input type="checkbox" name="acepto" value="si" required />
        <span>
          Acepto las <Link href="/legal/condiciones">condiciones de compra</Link> y quiero la descarga ya. Sé que, al ser un
          contenido digital que se entrega al momento, pierdo el derecho de desistimiento.
        </span>
      </label>
      <button className="btn" type="submit">Pagar {price}</button>
      <p className="sub" style={{ fontSize: "var(--t-s)" }}>
        Pagas en Stripe con tarjeta, Apple Pay o Google Pay. Nosotros no vemos los datos de tu tarjeta. Te llega un correo
        con el enlace a tu Área de cliente.
      </p>
    </form>
  );
}
