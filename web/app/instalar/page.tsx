import Link from "next/link";
import { Pie, Top } from "@/components/Chrome";
import { Shot } from "@/components/Shot";

export const metadata = {
  title: "Instalar · CarVision",
  description: "Guía paso a paso para instalar CarVision en tu radio Android con un pendrive y poner tu coche.",
};

export default function Instalar() {
  return (
    <>
      <Top actual="/instalar" />
      <main className="wrap hoja">
        <h1>Instalar CarVision</h1>
        <p className="intro">
          Unos 10 minutos con un pendrive. Si prefieres que te lo vayan diciendo, en tu Área tienes un texto para copiar en
          ChatGPT, Gemini o Copilot que te guía paso a paso.
        </p>
        <nav className="indice" aria-label="En esta página">
          <a href="#antes">Antes de empezar</a>
          <a href="#pasos">Paso a paso</a>
          <a href="#coche">Poner tu coche</a>
          <a href="#ia">Con ayuda de una IA</a>
          <a href="#quitar">Volver a lo de antes</a>
          <a href="#problemas">Si algo falla</a>
        </nav>

        <section id="antes">
          <h2>Antes de empezar</h2>
          <ul className="lista">
            <li>Mira la versión de Android de tu radio: Ajustes › Acerca de (o «Información») › Versión de Android. Tiene que ser 8 o superior.</li>
            <li>Un pendrive cualquiera (no hace falta formatearlo).</li>
            <li>El correo de tu compra, con el enlace a tu Área de cliente.</li>
          </ul>
        </section>

        <section id="pasos">
          <h2>Paso a paso</h2>
          <ol className="pasos">
            <li>
              <h3>Descarga la app y tu coche</h3>
              <p>
                Abre tu Área de cliente desde el correo (en el móvil o en el ordenador) y descarga «CarVision.apk» y el .zip
                de tu coche. Cópialos a un pendrive. El .zip no hace falta descomprimirlo.
              </p>
            </li>
            <li>
              <h3>Instala la app en la radio</h3>
              <p>
                Pincha el pendrive en la radio. Abre el explorador de archivos (suele llamarse «Archivos», «File Manager» o
                «Explorador») › el pendrive › toca «CarVision.apk» › «Instalar».
              </p>
              <p>
                Si la radio pide permitir «orígenes desconocidos», permítelo para el explorador. Algunas radios piden una
                contraseña para eso: viene en su manual, en la caja o en la web del fabricante.
              </p>
            </li>
            <li>
              <h3>Sigue el asistente</h3>
              <p>Al abrir CarVision aparece «Bienvenido a CarVision», con tres pasos que se marcan solos al hacerlos:</p>
              <ul>
                <li><b>Pantalla de inicio:</b> pulsa «Poner como inicio», elige CarVision (y «Siempre» si lo pregunta) y vuelve con el botón Atrás.</li>
                <li><b>Tu coche:</b> elige su color. Si ya tienes el pendrive con tu coche, en el paso siguiente lo instalas.</li>
                <li><b>Música:</b> da permiso para ver las notificaciones, y así se ve la canción que suena. Es opcional.</li>
              </ul>
              <Shot name="asistente" alt="Asistente de bienvenida de CarVision" />
            </li>
          </ol>
        </section>

        <section id="coche">
          <h2>Poner tu coche</h2>
          <ol className="pasos">
            <li>
              <h3>Pincha el pendrive con CarVision abierta</h3>
              <p>Arriba sale «Pendrive conectado» › «Buscar mi coche». La primera vez Android pide permiso: «Permitir».</p>
              <Shot name="pendrive_aviso" alt="Aviso Pendrive conectado" />
            </li>
            <li>
              <h3>Instálalo</h3>
              <p>Sale tu coche: «Instalar». En unos segundos lo tienes en la pantalla de inicio, con su logotipo.</p>
              <Shot name="pendrive_lista" alt="Coches del pendrive listos para instalar" />
            </li>
            <li>
              <h3>Elige el color</h3>
              <p>Toca el icono del Garaje (arriba, encima del coche) › fila «Color». Cambia al instante.</p>
              <Shot name="garaje_color" alt="Garaje con la fila de colores" />
            </li>
          </ol>
          <p className="nota">
            <b>¿No sale el aviso del pendrive?</b> Garaje › «Importar .zip» › en el selector, menú ☰ › el pendrive › el .zip. Y si
            tu radio no tiene selector de archivos: copia el .zip a la carpeta Android/data/com.carvision.launcher/files/import y
            pulsa «Buscar en la carpeta» en el Garaje.
          </p>
        </section>

        <section id="ia">
          <h2>Con ayuda de una IA</h2>
          <div className="dos-col">
            <div className="tarjeta">
              <h3>IA gratis (ChatGPT, Gemini, Copilot)</h3>
              <p style={{ marginTop: 8 }}>
                En tu Área copias un texto, lo pegas en la IA y cambias la primera línea por tu coche. Te pregunta qué ves,
                te dice el siguiente paso y, si algo no sale, le mandas una foto de la pantalla de la radio.
              </p>
            </div>
            <div className="tarjeta">
              <h3>IA de pago con acceso a archivos</h3>
              <p style={{ marginTop: 8 }}>
                Además de guiarte, puede preparar un coche que no esté en el catálogo con el generador incluido en la compra.{" "}
                <Link href="/hazlo-tu">Hazlo tú con una IA</Link>
              </p>
            </div>
          </div>
        </section>

        <section id="quitar">
          <h2>Volver a la pantalla de antes</h2>
          <p>
            En CarVision: Ajustes › «Volver al launcher original». Y si quieres quitarla del todo, desinstálala como
            cualquier app (Ajustes de la radio › Aplicaciones › CarVision › Desinstalar).
          </p>
        </section>

        <section id="problemas">
          <h2>Si algo falla</h2>
          <div className="faq">
            <details>
              <summary>La radio no me deja instalar el .apk</summary>
              <p>
                Busca en Ajustes de la radio «Seguridad» o «Fuentes desconocidas» y permítelo para el explorador. Si pide una
                contraseña de fábrica, viene en el manual de la radio o en la web del fabricante.
              </p>
            </details>
            <details>
              <summary>Al reiniciar la radio vuelve la pantalla de antes</summary>
              <p>
                Abre CarVision › Ajustes › Asistente de inicio › «Poner como inicio» y elige CarVision con «Siempre». Algunas
                radios tienen su propia opción de «launcher predeterminado» en sus ajustes de fábrica.
              </p>
            </details>
            <details>
              <summary>No se ve la música que suena</summary>
              <p>Hace falta el permiso de notificaciones: toca la tarjeta de música y actívalo para CarVision.</p>
            </details>
            <details>
              <summary>El coche se ve en plano (2D)</summary>
              <p>
                Tu radio no tiene el modo gráfico que necesita el 3D (OpenGL ES 3.0). La app sigue funcionando con una vista
                del coche desde arriba. Escríbenos con el modelo de tu radio.
              </p>
            </details>
            <details>
              <summary>Nada de esto me sirve</summary>
              <p>
                Escríbenos desde la página de <Link href="/ayuda">Ayuda</Link> con una foto de Ajustes › Acerca de de tu
                radio y de lo que ves en pantalla.
              </p>
            </details>
          </div>
        </section>
      </main>
      <Pie />
    </>
  );
}
