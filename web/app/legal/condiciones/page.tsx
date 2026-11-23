import { Pie, Top } from "@/components/Chrome";
import Pendiente from "@/components/Pendiente";
import { APP_PRICE_CENTS, CONTACT_EMAIL, SERVICE_PRICE_CENTS, euros } from "@/lib/config";

export const metadata = { title: "Condiciones de compra · CarVision" };

export default function Condiciones() {
  return (
    <>
      <Top />
      <main className="wrap hoja">
        <h1>Condiciones de compra</h1>
        <h2>Qué compras</h2>
        <p>
          CarVision ({euros(APP_PRICE_CENTS)}, pago único): la app para radios Android 8 o superior, los coches del catálogo
          disponibles, el código fuente con el generador de coches y las actualizaciones de la versión 1. Se descarga desde
          tu Área de cliente justo después de pagar. El código fuente es para tu uso: puedes modificarlo, pero no revender
          la app ni los coches del catálogo.
        </p>
        <p>
          «Te lo hacemos» ({euros(SERVICE_PRICE_CENTS)}): preparamos el coche que nos indicas para usarlo en CarVision. Si no
          existe un modelo 3D adecuado de ese coche, te devolvemos el importe. Te avisamos por correo cuando esté listo.
        </p>
        <p>Precios finales en euros. <Pendiente>Confirmar con la gestoría cómo se indica el IVA</Pendiente>.</p>
        <h2>Desistimiento</h2>
        <p>
          La app es un contenido digital que se entrega al momento. Al comprar aceptas expresamente que la descarga empiece
          ya y que por eso pierdes el derecho de desistimiento (artículo 103.m de la Ley General para la Defensa de los
          Consumidores y Usuarios).
        </p>
        <h2>Compatibilidad</h2>
        <p>
          Antes de comprar, comprueba que tu radio tiene Android 8 o superior y permite instalar apps desde un pendrive. Si
          aun así no funciona, escríbenos a {CONTACT_EMAIL} con una foto de Ajustes › Acerca de y lo vemos contigo.
        </p>
        <h2>Datos del coche</h2>
        <p>
          En esta versión, las aperturas, la velocidad y el resto de datos del coche funcionan en modo demostración y la app
          lo indica. No muestra datos simulados como si fueran reales.
        </p>
      </main>
      <Pie />
    </>
  );
}
