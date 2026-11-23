import Link from "next/link";
import Radio from "@/components/Radio";
import { Pie, Top } from "@/components/Chrome";
import BuyForm from "@/components/BuyForm";
import { Clip, Shot } from "@/components/Shot";
import { APP_PRICE_CENTS, SERVICE_PRICE_CENTS, euros } from "@/lib/config";

type Props = { searchParams: Promise<Record<string, string | undefined>> };

// Lo demás que lleva, sin capturas (cada una está en /funciones con su imagen).
const MAS = [
  { id: "mantenimiento", t: "Carnet de mantenimiento", p: "ITV, aceite, frenos, distribución… con avisos, historial con talleres y costes, y PDF." },
  { id: "vehiculo", t: "Viaje y datos del coche", p: "Duración, velocidad media y máxima, historial de viajes y estado de cada apertura." },
  { id: "inicio", t: "Música, navegación y teléfono", p: "Lo que suena con su carátula y tus apps de siempre, a un toque." },
  { id: "avisos", t: "Avisos al conducir", p: "Si se abre una puerta en marcha, un aviso y un tono suave (y voz si quieres)." },
  { id: "pendrive", t: "Pendrive automático", p: "Pinchas el USB y te ofrece instalar el coche que lleva." },
  { id: "asistente", t: "Asistente de 3 pasos", p: "La deja como pantalla de inicio, con tu coche y tu música, la primera vez." },
];

export default async function Home({ searchParams }: Props) {
  const q = await searchParams;
  const price = euros(APP_PRICE_CENTS);
  return (
    <>
      <Top actual="/" />
      <main>
        <section className="hero">
          <div className="wrap hero-grid">
            <h1 className="hero-titulo">Tu coche, en la pantalla de tu radio.</h1>
            <div className="hero-radio"><Radio /></div>
            <div className="hero-texto">
              <p className="entradilla">
                CarVision cambia la pantalla de inicio de tu radio Android por una con tu coche en 3D: tu modelo, tu
                color y el logotipo de tu marca al encenderla. Se instala con un pendrive en minutos.
              </p>
              <div className="acciones">
                <a className="btn" href="#comprar">Comprar por {price}</a>
                <Link className="btn sec" href="/funciones">Ver todo lo que hace</Link>
              </div>
            </div>
          </div>
        </section>

        <section className="seccion" aria-labelledby="hace">
          <div className="wrap">
            <div className="cabeza">
              <h2 id="hace">Todo lo que lleva</h2>
              <p>No es un fondo de pantalla: es una pantalla de inicio completa para el coche, pensada para usarse conduciendo.</p>
            </div>
            <div className="bloque">
              <div>
                <h3>Tu coche en 3D, de verdad</h3>
                <p>
                  No son fotos: lo giras con el dedo y lo ves desde cualquier lado. Se abren las cuatro puertas, el capó y el
                  maletero, y las ruedas giran al andar.
                </p>
                <p className="nota"><b>Demo:</b> en esta versión las aperturas y la velocidad son de demostración, y la app lo marca.</p>
              </div>
              <Shot name="puerta" alt="Ford Focus en 3D con la puerta delantera izquierda abierta y su aviso" />
            </div>
            <div className="bloque al-reves">
              <div>
                <h3>Tu marca al encender</h3>
                <p>Cada vez que arrancas, la radio se enciende con el logotipo de tu marca y un destello de luz, como en los coches nuevos.</p>
              </div>
              <Clip src="/video/arranque_ford.mp4" poster="/video/arranque_ford.webp" alt="Animación de arranque con el logotipo de Ford" />
            </div>
            <div className="bloque">
              <div>
                <h3>Tu color, de día y de noche</h3>
                <p>
                  12 colores al instante y el logotipo que quieras. De noche se pone oscura sola al encender las luces (o por la
                  hora), con seis fondos a elegir.
                </p>
              </div>
              <Shot name="oscuro" alt="Pantalla de inicio en tema oscuro con el Focus" />
            </div>
            <h3 className="titulo-lista">Y además</h3>
            <ul className="mas-lista">
              {MAS.map((m) => (
                <li key={m.id}>
                  <Link href={`/funciones#${m.id}`}><b>{m.t}</b></Link>
                  <span>{m.p}</span>
                </li>
              ))}
            </ul>
            <p className="mas"><Link href="/funciones">Ver todas las funciones, con capturas</Link></p>
          </div>
        </section>

        <section className="seccion" aria-labelledby="coches-t">
          <div className="wrap">
            <div className="cabeza">
              <h2 id="coches-t">Coches listos para instalar</h2>
              <p>Cada coche está terminado a mano: puertas que se abren bien, su logotipo y sus detalles. Vamos añadiendo más.</p>
            </div>
            <div className="coches">
              <Link href="/coches#focus" className="coche tarjeta" style={{ textDecoration: "none", color: "inherit" }}>
                <div className="foto"><img src="/img/coches/focus.webp" alt="Ford Focus MK3 naranja en CarVision" width={640} height={400} loading="lazy" /></div>
                <div className="datos"><img src="/img/marcas/ford.png" alt="" width={56} height={56} /><div><b>Ford Focus MK3</b><span>5 puertas · 2011–2014</span></div></div>
              </Link>
              <Link href="/coches#golf" className="coche tarjeta" style={{ textDecoration: "none", color: "inherit" }}>
                <div className="foto"><img src="/img/coches/golf.webp" alt="Volkswagen Golf VI azul en CarVision" width={640} height={400} loading="lazy" /></div>
                <div className="datos"><img src="/img/marcas/volkswagen.png" alt="" width={56} height={56} /><div><b>Volkswagen Golf VI</b><span>5 puertas · 2008–2012</span></div></div>
              </Link>
            </div>
            <div className="opciones">
              <div className="opcion tarjeta">
                <h3>¿El tuyo no está? Te lo hacemos</h3>
                <p>Nos dices marca, modelo y año y lo preparamos a mano. <span className="precio-peq">{euros(SERVICE_PRICE_CENTS)}</span></p>
                <Link href="/coches#te-lo-hacemos">Cómo funciona</Link>
              </div>
              <div className="opcion tarjeta">
                <h3>O el coche CarVision</h3>
                <p>Viene incluido: un compacto sin marca que pintas de tu color y al que pones el logotipo de tu marca.</p>
                <Link href="/coches#por-defecto">Ver el coche CarVision</Link>
              </div>
              <div className="opcion tarjeta">
                <h3>O hazlo tú con una IA</h3>
                <p>Si tienes una IA de pago que trabaja con archivos (como Claude Code), la compra incluye el código y el generador.</p>
                <Link href="/hazlo-tu">Opción avanzada</Link>
              </div>
            </div>
          </div>
        </section>

        <section className="seccion" aria-labelledby="instalar-t">
          <div className="wrap">
            <div className="cabeza">
              <h2 id="instalar-t">Se instala con un pendrive</h2>
              <p>Sin cables al ordenador, sin root y sin tocar nada de la radio. Se quita igual de fácil.</p>
            </div>
            <ol className="pasos tres">
              <li>
                <h3>Descarga</h3>
                <p>Tras pagar, entras en tu Área de cliente (sin contraseña) y bajas la app y tu coche. Los copias a un pendrive.</p>
              </li>
              <li>
                <h3>Instala la app</h3>
                <p>En la radio abres el pendrive y tocas «CarVision.apk». Un asistente de tres pasos la deja como pantalla de inicio.</p>
              </li>
              <li>
                <h3>Pon tu coche</h3>
                <p>Con la app abierta pinchas el pendrive: sale «Buscar mi coche». Un toque y listo.</p>
              </li>
            </ol>
            <div className="dos-col">
              <div className="tarjeta">
                <h3 style={{ fontSize: "var(--t-m)", fontFamily: "var(--f-text)", fontVariationSettings: "normal" }}>Con una IA gratis te guía paso a paso</h3>
                <p style={{ marginTop: 8, color: "var(--texto-2)" }}>
                  En tu Área hay un texto para copiar en ChatGPT, Gemini o Copilot. Le dices tu coche y te lleva paso a paso; si
                  algo no sale, le mandas una foto de la pantalla.
                </p>
              </div>
              <div className="tarjeta">
                <h3 style={{ fontSize: "var(--t-m)", fontFamily: "var(--f-text)", fontVariationSettings: "normal" }}>Con una IA de pago, hasta te hace el coche</h3>
                <p style={{ marginTop: 8, color: "var(--texto-2)" }}>
                  Si usas una IA con acceso a archivos, tienes otro texto para que prepare tu coche con el generador incluido.{" "}
                  <Link href="/hazlo-tu">Ver cómo</Link>
                </p>
              </div>
            </div>
            <p className="mas"><Link href="/instalar">Guía de instalación completa, con capturas</Link></p>
          </div>
        </section>

        <section className="seccion" aria-labelledby="distinto-t">
          <div className="wrap">
            <div className="cabeza">
              <h2 id="distinto-t">Lo que la hace distinta</h2>
            </div>
            <ul className="lista">
              <li><b>3D de verdad, no fotos:</b> el coche se ve desde cualquier ángulo y se mueve con suavidad.</li>
              <li><b>Cambias el color al instante</b>, sin descargar otro coche.</li>
              <li><b>Coches ligeros:</b> unos 2 MB cada uno, y la app 17 MB.</li>
              <li><b>Sin permisos raros:</b> no pide accesibilidad, ni conectar la radio a un ordenador, ni root.</li>
              <li><b>Instalación en 3 pasos</b> con asistente y pendrive automático.</li>
              <li><b>Acceso sin contraseña:</b> pagas y te llega el enlace a tu Área.</li>
              <li><b>En español</b>, pensada para las radios Android que se montan aquí.</li>
            </ul>
          </div>
        </section>

        <section className="seccion" aria-labelledby="req-t">
          <div className="wrap">
            <div className="cabeza">
              <h2 id="req-t">Antes de comprar, mira tu radio</h2>
            </div>
            <ul className="lista">
              <li>Radio con <b>Android 8 o superior</b>. Lo ves en Ajustes › Acerca de › Versión de Android.</li>
              <li>Que deje instalar apps desde un pendrive (la mayoría de radios Android lo permiten; algunas piden una contraseña que viene en su manual).</li>
              <li>Un pendrive cualquiera.</li>
            </ul>
            <p className="mas"><Link href="/ayuda#compatibilidad">Dudas de compatibilidad</Link></p>
          </div>
        </section>

        <section className="seccion" id="comprar" aria-labelledby="comprar-t">
          <div className="wrap">
            <div className="compra">
              <div>
                <h2 id="comprar-t">CarVision para tu radio</h2>
                <p className="precio">{price}</p>
                <p className="sub">Pago único. Sin suscripción.</p>
                <ul className="incluye">
                  <li>La app para tu radio Android</li>
                  <li>Los coches del catálogo y el coche CarVision</li>
                  <li>El texto que te guía con una IA gratis</li>
                  <li>El código fuente y el generador de coches</li>
                  <li>Las actualizaciones de la versión 1</li>
                </ul>
              </div>
              <BuyForm price={price} falta={q.falta} pago={q.pago} />
            </div>
          </div>
        </section>

        <section className="seccion" aria-labelledby="faq-t">
          <div className="wrap">
            <h2 id="faq-t">Preguntas</h2>
            <div className="faq">
              <details>
                <summary>¿Lee las puertas y la velocidad de mi coche?</summary>
                <p>
                  Todavía no. En esta versión esos datos están en modo demostración y la app lo indica en pantalla con la
                  etiqueta «Demo». Leer los datos reales depende de cada radio y llegará en próximas versiones.
                </p>
              </details>
              <details>
                <summary>¿Puedo volver a la pantalla de antes?</summary>
                <p>Sí. En Ajustes de CarVision tienes «Volver al launcher original», o la desinstalas como cualquier app.</p>
              </details>
              <details>
                <summary>¿Toca algo del coche o de la radio?</summary>
                <p>No. No necesita root, no cambia el sistema de la radio y no escribe nada en el coche.</p>
              </details>
              <details>
                <summary>Mi coche no está en la lista</summary>
                <p>Pídelo con «Te lo hacemos», usa el coche CarVision con tu color y tu logotipo, o hazlo tú con una IA de pago.</p>
              </details>
            </div>
            <p className="mas"><Link href="/ayuda">Todas las preguntas</Link></p>
          </div>
        </section>
      </main>
      <Pie />
    </>
  );
}
