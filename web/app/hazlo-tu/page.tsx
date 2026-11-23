import Link from "next/link";
import { Pie, Top } from "@/components/Chrome";
import { SERVICE_PRICE_CENTS, euros } from "@/lib/config";

export const metadata = {
  title: "Hazlo tú con una IA · CarVision",
  description: "Opción avanzada: prepara tu propio coche para CarVision con una IA de pago con acceso a archivos y el generador incluido.",
};

export default function HazloTu() {
  return (
    <>
      <Top />
      <main className="wrap hoja">
        <h1>Hazlo tú con una IA</h1>
        <p className="intro">
          Opción avanzada para quien usa una IA de pago que trabaja con archivos en su ordenador. Si no es tu caso, lo más
          fácil es el <Link href="/coches#catalogo">catálogo</Link>, el <Link href="/coches#por-defecto">coche CarVision</Link> o{" "}
          <Link href="/coches#te-lo-hacemos">«Te lo hacemos»</Link> ({euros(SERVICE_PRICE_CENTS)}).
        </p>

        <section>
          <h2>Qué necesitas</h2>
          <ul className="lista">
            <li>
              Una IA que pueda leer archivos y ejecutar programas en tu ordenador, como Claude Code (de pago). Las IA de chat
              de una web (ChatGPT, Gemini…) no sirven para esto: no pueden abrir Blender en tu ordenador.
            </li>
            <li>Un ordenador con Windows, Mac o Linux y Blender (gratis, de blender.org, versión 4.2 o posterior).</li>
            <li>El código fuente de CarVision: lo descargas en tu Área de cliente (incluido en la compra).</li>
            <li>Un modelo 3D de tu coche (la IA te ayuda a buscarlo; mejor con las puertas separadas).</li>
          </ul>
        </section>

        <section>
          <h2>Cómo funciona</h2>
          <ol className="pasos">
            <li>
              <h3>Descarga el código y el texto para la IA</h3>
              <p>En tu Área: «Código fuente» (un .zip) y el «Texto para una IA con acceso a archivos». Descomprime el código en una carpeta.</p>
            </li>
            <li>
              <h3>Abre la IA en esa carpeta y pega el texto</h3>
              <p>Cambia la primera línea por tu coche: marca, modelo, años, carrocería, puertas y color.</p>
            </li>
            <li>
              <h3>La IA prepara el coche</h3>
              <p>
                Te ayuda a elegir un modelo 3D, usa el generador incluido para separar puertas, capó, maletero y ruedas, ponerlo
                a su tamaño real y prepararle el color, lo revisa con imágenes y te da el .zip del coche.
              </p>
            </li>
            <li>
              <h3>Lo instalas como cualquier coche</h3>
              <p>Pendrive › «Buscar mi coche» › Instalar. <Link href="/instalar#coche">Ver cómo</Link></p>
            </li>
          </ol>
        </section>

        <section>
          <h2>Antes de lanzarte</h2>
          <ul className="lista pendiente">
            <li>El resultado depende mucho del modelo 3D: con algunos modelos las puertas no se pueden separar y el coche se ve sin aperturas.</li>
            <li>Puede llevar un rato de prueba y corrección, y el uso de la IA tiene su coste.</li>
            <li>Es una opción sin soporte individual: si quieres el coche perfecto sin complicarte, pídenos «Te lo hacemos».</li>
            <li>Usa modelos 3D con una licencia que te permita usarlos (por ejemplo, CC BY), y cita a su autor.</li>
          </ul>
          <p className="mas"><Link href="/#comprar">Comprar CarVision</Link></p>
        </section>
      </main>
      <Pie />
    </>
  );
}
