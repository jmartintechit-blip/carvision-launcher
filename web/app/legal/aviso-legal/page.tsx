import { Pie, Top } from "@/components/Chrome";
import Titular from "@/components/Titular";
import { CONTACT_EMAIL } from "@/lib/config";

export const metadata = { title: "Aviso legal · CarVision" };

export default function AvisoLegal() {
  return (
    <>
      <Top />
      <main className="wrap hoja">
        <h1>Aviso legal</h1>
        <h2>Quién vende</h2>
        <p>
          <Titular conEtiquetas />. Correo: {CONTACT_EMAIL}.
        </p>
        <p>Este sitio, carvisiones.es, informa y vende la app CarVision para radios Android de coche.</p>
        <h2>Marcas de terceros</h2>
        <p>
          CarVision no está afiliada, patrocinada ni aprobada por Ford, Volkswagen ni ningún otro fabricante. Sus nombres y
          logotipos son marcas de sus dueños y aquí solo se usan para identificar el coche de cada cliente.
        </p>
        <h2>Modelos 3D</h2>
        <p>
          Los coches en 3D parten de modelos publicados con licencia Creative Commons Atribución (CC BY 4.0); cada coche cita
          a su autor dentro de la app (Ajustes › Acerca de) y nosotros los adaptamos.
        </p>
        <h2>Responsabilidad</h2>
        <p>
          CarVision es una pantalla de inicio: no cambia el sistema de la radio ni escribe nada en el coche. Los avisos de la
          app son una ayuda y no sustituyen a los testigos del coche.
        </p>
      </main>
      <Pie />
    </>
  );
}
