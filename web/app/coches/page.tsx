import Link from "next/link";
import { Pie, Top } from "@/components/Chrome";
import { Shot } from "@/components/Shot";
import { SERVICE_PRICE_CENTS, euros } from "@/lib/config";

export const metadata = {
  title: "Coches · CarVision",
  description: "Coches listos para CarVision (Ford Focus MK3, Volkswagen Golf VI), el coche CarVision con tu color y logotipo, y «Te lo hacemos».",
};

export default function Coches() {
  return (
    <>
      <Top actual="/coches" />
      <main className="wrap hoja">
        <h1>Coches</h1>
        <p className="intro">
          Cuatro formas de tener tu coche en la radio. La más rápida: que esté en el catálogo. Todos los coches funcionan
          igual: aperturas, ruedas, color y logotipo.
        </p>
        <nav className="indice" aria-label="En esta página">
          <a href="#catalogo">Catálogo</a>
          <a href="#por-defecto">Coche CarVision</a>
          <a href="#te-lo-hacemos">Te lo hacemos</a>
          <Link href="/hazlo-tu">Hazlo tú con una IA</Link>
        </nav>

        <section id="catalogo">
          <h2>Catálogo</h2>
          <p>Terminados a mano uno a uno: puertas que se abren bien, ruedas que giran, su logotipo y los detalles revisados. Se descargan desde tu Área de cliente (unos 2 MB cada uno).</p>
          <div className="coches">
            <article id="focus" className="coche tarjeta">
              <div className="foto"><img src="/img/coches/focus.webp" alt="Ford Focus MK3 naranja en CarVision" width={640} height={400} /></div>
              <div className="datos">
                <img src="/img/marcas/ford.png" alt="" width={56} height={56} />
                <div><b>Ford Focus MK3</b><span>5 puertas · 2011–2014 · logotipo Ford</span></div>
              </div>
            </article>
            <article id="golf" className="coche tarjeta">
              <div className="foto"><img src="/img/coches/golf.webp" alt="Volkswagen Golf VI azul en CarVision" width={640} height={400} /></div>
              <div className="datos">
                <img src="/img/marcas/volkswagen.png" alt="" width={56} height={56} />
                <div><b>Volkswagen Golf VI</b><span>5 puertas · 2008–2012 · logotipo Volkswagen</span></div>
              </div>
            </article>
          </div>
          <p style={{ marginTop: 16 }}>
            ¿Un año o versión parecidos? Si tu coche es de la misma generación (por ejemplo, un Golf VI de 3 puertas o un Focus
            familiar), puedes usar el más parecido o pedirnos el tuyo exacto.
          </p>
        </section>

        <section id="por-defecto" className="bloque">
          <div>
            <h2>El coche CarVision</h2>
            <p>
              Viene dentro de la app: un compacto de 5 puertas sin marca. Lo pintas de tu color y le pones el logotipo de tu
              marca (se ve en la barra lateral y en la animación de arranque).
            </p>
            <p>Es la opción si tu coche no está y no quieres esperar.</p>
          </div>
          <Shot name="inicio_defecto" alt="El coche CarVision pintado de amarillo en la pantalla de inicio" />
        </section>

        <section id="te-lo-hacemos">
          <h2>Te lo hacemos</h2>
          <p>Nos dices tu coche y lo preparamos a mano, con el mismo cuidado que los del catálogo. {euros(SERVICE_PRICE_CENTS)}.</p>
          <ol className="pasos tres">
            <li>
              <h3>Lo pides</h3>
              <p>Desde tu Área de cliente: marca, modelo, año, puertas y color. Pagas el servicio.</p>
            </li>
            <li>
              <h3>Lo preparamos</h3>
              <p>Buscamos un buen modelo 3D de tu coche, separamos puertas y ruedas, revisamos materiales y logotipo.</p>
            </li>
            <li>
              <h3>Te avisamos</h3>
              <p>Te llega un correo y lo descargas desde tu Área. Se instala igual que los del catálogo.</p>
            </li>
          </ol>
          <p className="nota"><b>Si no existe un modelo 3D adecuado</b> de tu coche, te lo decimos y te devolvemos el dinero.</p>
          <p className="mas"><Link href="/area#te-lo-hacemos">Pedir mi coche desde Mi área</Link></p>
        </section>

        <section>
          <h2>Hazlo tú con una IA de pago</h2>
          <p>
            Para quien usa una IA que trabaja con archivos en su ordenador (como Claude Code): la compra incluye el código
            y el generador de coches, y un texto para que la IA haga el trabajo. Es una opción avanzada.
          </p>
          <p className="mas"><Link href="/hazlo-tu">Ver cómo funciona</Link></p>
        </section>
      </main>
      <Pie />
    </>
  );
}
