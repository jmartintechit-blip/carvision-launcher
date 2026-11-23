import Link from "next/link";
import { Pie, Top } from "@/components/Chrome";
import { Clip, Shot } from "@/components/Shot";

export const metadata = {
  title: "Funciones · CarVision",
  description: "Todo lo que hace CarVision en tu radio Android: coche en 3D con aperturas, logotipo al encender, color, tema oscuro, mantenimiento, viaje y más.",
};

const INDICE = [
  ["coche-3d", "Coche en 3D"], ["arranque", "Al encender"], ["color", "Color y logotipo"], ["tema", "Claro y oscuro"],
  ["inicio", "Pantalla de inicio"], ["vehiculo", "Viaje y datos"], ["mantenimiento", "Mantenimiento"],
  ["avisos", "Avisos"], ["pendrive", "Pendrive"], ["asistente", "Asistente"], ["ajustes", "Ajustes"],
  ["privacidad", "Privacidad"], ["proximamente", "Lo que viene"],
];

export default function Funciones() {
  return (
    <>
      <Top actual="/funciones" />
      <main className="wrap hoja">
        <h1>Todo lo que hace CarVision</h1>
        <p className="intro">
          Capturas reales de la app en una pantalla de radio de 1280 × 720. Donde ves «Demo», el dato es de demostración y
          la app lo marca así; nunca lo muestra como si fuera de tu coche.
        </p>
        <nav className="indice" aria-label="En esta página">
          {INDICE.map(([id, t]) => <a key={id} href={`#${id}`}>{t}</a>)}
        </nav>

        <section id="coche-3d" className="bloque">
          <div>
            <h2>Tu coche en 3D de verdad</h2>
            <p>
              No son fotos: es un modelo 3D que se dibuja en tiempo real en la radio. Lo giras con el dedo para verlo desde
              cualquier lado, y también desde arriba.
            </p>
            <ul>
              <li>Se abren las cuatro puertas, el capó y el maletero, cada uno con su bisagra.</li>
              <li>Las ruedas giran cuando el coche anda.</li>
              <li>Al abrirse una puerta, la cámara se gira para que la veas.</li>
            </ul>
            <p className="nota"><b>Demo:</b> en esta versión las aperturas y la velocidad son de demostración (lo verás marcado). Leer las de tu coche llegará en próximas versiones.</p>
          </div>
          <Shot name="puerta" alt="Ford Focus en 3D con la puerta delantera izquierda abierta y el aviso de puerta abierta" caption="Puerta delantera izquierda abierta, con su aviso." eager />
        </section>
        <div className="bloque al-reves" style={{ marginTop: 24 }}>
          <div><p>Todo abierto a la vez: puertas, capó y maletero, con el motor y el interior visibles.</p></div>
          <Shot name="aperturas_golf" alt="Golf VI con todas las puertas, el capó y el maletero abiertos" />
        </div>

        <section id="arranque" className="bloque al-reves">
          <div>
            <h2>El logotipo de tu marca al encender</h2>
            <p>
              Cada vez que arrancas, la radio se enciende con el logotipo de tu marca sobre negro: aparece, lo cruza un
              destello de luz y da paso a la pantalla de inicio. Como en los coches nuevos.
            </p>
            <p>Se puede desactivar en Ajustes › Animación de arranque.</p>
          </div>
          <Clip src="/video/arranque_ford.mp4" poster="/video/arranque_ford.webp" alt="Animación de arranque con el logotipo de Ford" caption="Grabación real de la animación (Ford Focus)." />
        </section>

        <section id="color" className="bloque">
          <div>
            <h2>Tu color y tu logotipo</h2>
            <p>
              En el Garaje eliges el coche y su color entre 12 habituales (blanco, plata, gris, negro, rojo, azules, verde,
              amarillo, naranja, marrón, beige) o el original. Cambia al instante, sin descargar nada.
            </p>
            <p>
              El logotipo de la marca lo traen los coches del catálogo. Si quieres otro, eliges una imagen y la app le quita
              el fondo sola; con «Quitar» vuelves al de antes.
            </p>
          </div>
          <Shot name="garaje_color" alt="El Garaje con los coches instalados y la fila de colores" caption="El Garaje: coches instalados, color y logotipo." />
        </section>

        <section id="tema" className="bloque al-reves">
          <div>
            <h2>Claro, oscuro o automático</h2>
            <p>
              En automático, la pantalla se pone oscura al encender las luces del coche (si la radio da esa señal) y, si no,
              por la hora. Así de noche no deslumbra.
            </p>
            <p>Para el tema oscuro hay seis ambientes de fondo: Grafito, Medianoche, Aurora, Atardecer, Burdeos y Titanio.</p>
          </div>
          <Shot name="oscuro" alt="La pantalla de inicio en tema oscuro" caption="Tema oscuro, con el coche iluminado." />
        </section>
        <div className="bloque" style={{ marginTop: 24 }}>
          <div><p>Los seis ambientes del tema oscuro.</p></div>
          <Shot name="ambientes" alt="Selector de ambientes: Grafito, Medianoche, Aurora, Atardecer, Burdeos y Titanio" />
        </div>

        <section id="inicio" className="bloque">
          <div>
            <h2>Una pantalla de inicio completa</h2>
            <ul>
              <li><b>Música:</b> lo que suena en tu app de música (Spotify y la mayoría de reproductores), con carátula y botones de anterior, pausa y siguiente.</li>
              <li><b>Navegación, teléfono y apps</b> a un toque desde la barra lateral; abre las que ya tiene tu radio.</li>
              <li><b>Hora, fecha y temperatura</b> en grande, para leerlas de un vistazo.</li>
              <li><b>Velocidad y revoluciones</b>, combustible, batería y kilómetros (en demostración en esta versión).</li>
            </ul>
          </div>
          <Shot name="inicio_focus" alt="Pantalla de inicio con el Ford Focus, la hora, la música y los datos del viaje" />
        </section>

        <section id="vehiculo" className="bloque al-reves">
          <div>
            <h2>Viaje y datos del coche</h2>
            <p>
              El ordenador de a bordo calcula tu viaje a partir de la velocidad: duración, velocidad media y máxima, y guarda
              el historial de viajes. Al lado, los datos del coche, cada uno con su origen, y un plano con el estado de cada
              apertura.
            </p>
            <p className="nota"><b>Demo:</b> sin un adaptador para tu radio los datos son simulados y la app lo dice («Datos simulados (modo demostración)»).</p>
          </div>
          <Shot name="vehiculo" alt="Pantalla Vehículo: viaje actual, datos del coche y plano de aperturas" />
        </section>

        <section id="mantenimiento" className="bloque">
          <div>
            <h2>Carnet de mantenimiento</h2>
            <ul>
              <li>Aceite y filtros, ITV, frenos, batería, distribución, neumáticos y más, cada uno con su intervalo (km y tiempo).</li>
              <li>Te avisa en la pantalla de inicio de lo que está vencido o a punto.</li>
              <li>Historial con fecha, kilómetros, taller y coste.</li>
              <li>Exporta el carnet en PDF para enseñarlo al vender el coche.</li>
            </ul>
          </div>
          <Shot name="mantenimiento" alt="Carnet de mantenimiento con próximas intervenciones e historial" />
        </section>
        <div className="bloque al-reves" style={{ marginTop: 24 }}>
          <div><p>Apuntar una intervención y el PDF del carnet.</p></div>
          <div style={{ display: "grid", gap: 14 }}>
            <Shot name="mantenimiento_editor" alt="Formulario para añadir una intervención de mantenimiento" />
            <Shot name="pdf_carnet" alt="Carnet de mantenimiento exportado en PDF" />
          </div>
        </div>

        <section id="avisos" className="bloque al-reves">
          <div>
            <h2>Avisos al conducir</h2>
            <p>
              Si una puerta, el capó o el maletero se abren con el coche en marcha (más de 5 km/h), aparece un aviso arriba y
              suena un tono suave. Opcionalmente, una voz lo dice. Se quita solo al cerrar o al parar.
            </p>
            <p className="nota"><b>Importante:</b> son una ayuda, no un sistema de seguridad: no sustituyen a los testigos de tu coche.</p>
          </div>
          <Shot name="puerta" alt="Aviso de puerta delantera izquierda abierta con el coche en marcha" />
        </section>

        <section id="pendrive" className="bloque">
          <div>
            <h2>Pinchas el pendrive y listo</h2>
            <p>
              Con CarVision abierta, al conectar un USB sale «Pendrive conectado · Buscar mi coche». Te enseña los coches que
              lleva y los instalas con un toque. Android pide permiso para leer el pendrive una sola vez.
            </p>
            <p>Si es tu pendrive de música, pulsa «No preguntar» y no vuelve a salir.</p>
          </div>
          <div style={{ display: "grid", gap: 14 }}>
            <Shot name="pendrive_aviso" alt="Aviso Pendrive conectado con el botón Buscar mi coche" />
            <Shot name="pendrive_lista" alt="Lista de coches del pendrive con el botón Instalar" />
          </div>
        </section>

        <section id="asistente" className="bloque al-reves">
          <div>
            <h2>Asistente de primer arranque</h2>
            <p>
              Al abrirla por primera vez, tres pasos: ponerla como pantalla de inicio, elegir tu coche y su color, y dejar
              ver la música. Cada paso se marca solo al hacerlo y puedes saltar cualquiera.
            </p>
          </div>
          <Shot name="asistente" alt="Asistente de bienvenida con los pasos Pantalla de inicio, Tu coche y Música" />
        </section>

        <section id="ajustes" className="bloque">
          <div>
            <h2>Ajustes a tu gusto</h2>
            <ul>
              <li>Tema automático, claro u oscuro, y ambiente de fondo.</li>
              <li>Tamaño de la interfaz: compacto, normal o grande, según tu pantalla.</li>
              <li>Animación de arranque, avisos, tono y voz.</li>
              <li>«Volver al launcher original» en un toque.</li>
              <li>Vuelve a abrir el asistente cuando quieras.</li>
            </ul>
          </div>
          <Shot name="ajustes" alt="Pantalla de Ajustes de CarVision" />
        </section>

        <section id="privacidad">
          <h2>Tu radio, tus datos</h2>
          <ul className="lista">
            <li>No pide root, ni accesibilidad, ni conectar la radio a un ordenador.</li>
            <li>No cambia el sistema de la radio y no escribe nada en el coche.</li>
            <li>No necesita internet y no envía tus datos a ningún sitio: todo se guarda en la radio.</li>
            <li>Un solo permiso opcional: ver las notificaciones, para mostrar la canción que suena.</li>
          </ul>
        </section>

        <section id="proximamente">
          <h2>Lo que viene</h2>
          <p>Lo que todavía no hace y en lo que trabajamos. Sin fechas: cuando esté probado, llegará como actualización.</p>
          <ul className="lista pendiente">
            <li>Leer los datos reales del coche (puertas, velocidad, luces…) en cada modelo de radio.</li>
            <li>Más coches en el catálogo.</li>
            <li>Más idiomas.</li>
          </ul>
          <p className="mas"><Link href="/#comprar">Comprar CarVision</Link></p>
        </section>
      </main>
      <Pie />
    </>
  );
}
