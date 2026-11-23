import Link from "next/link";
import { Pie, Top } from "@/components/Chrome";
import { APP_PRICE_CENTS, CONTACT_EMAIL, SERVICE_PRICE_CENTS, euros } from "@/lib/config";

export const metadata = {
  title: "Ayuda · CarVision",
  description: "Preguntas frecuentes sobre CarVision: compatibilidad, instalación, coches, pagos, privacidad y contacto.",
};

export default function Ayuda() {
  return (
    <>
      <Top actual="/ayuda" />
      <main className="wrap hoja">
        <h1>Ayuda</h1>
        <p className="intro">Las dudas más habituales. Si la tuya no está, escríbenos y te contestamos.</p>
        <nav className="indice" aria-label="En esta página">
          <a href="#compatibilidad">Compatibilidad</a>
          <a href="#instalacion">Instalación</a>
          <a href="#uso">Uso</a>
          <a href="#coches">Coches</a>
          <a href="#compra">Compra</a>
          <a href="#privacidad">Privacidad</a>
          <a href="#contacto">Contacto</a>
        </nav>

        <section id="compatibilidad">
          <h2>Compatibilidad</h2>
          <div className="faq">
            <details>
              <summary>¿Funciona en mi radio?</summary>
              <p>
                En radios con Android 8 o superior que dejen instalar apps desde un pendrive. Lo ves en Ajustes › Acerca de ›
                Versión de Android. Es una app normal: no depende de la marca de la radio.
              </p>
            </details>
            <details>
              <summary>¿Y en una pantalla pequeña o muy grande?</summary>
              <p>Se adapta a pantallas de 1024 × 600 en adelante, y en Ajustes puedes elegir el tamaño de la interfaz: compacto, normal o grande.</p>
            </details>
            <details>
              <summary>¿Funciona en el móvil o en una tablet?</summary>
              <p>Está pensada para radios de coche, en horizontal. No la hemos probado como app de móvil o de tablet.</p>
            </details>
            <details>
              <summary>¿Necesita internet?</summary>
              <p>No. Solo para descargarla la primera vez desde tu Área (en el móvil o el ordenador).</p>
            </details>
          </div>
        </section>

        <section id="instalacion">
          <h2>Instalación</h2>
          <div className="faq">
            <details>
              <summary>¿Cómo se instala?</summary>
              <p>Con un pendrive en unos 10 minutos. Lo tienes paso a paso en <Link href="/instalar">Instalar</Link>, con capturas.</p>
            </details>
            <details>
              <summary>¿Necesito un ordenador o cables?</summary>
              <p>No. Descargas desde el móvil o el ordenador, lo pasas a un pendrive y lo instalas en la radio.</p>
            </details>
            <details>
              <summary>¿Y si me pierdo?</summary>
              <p>
                En tu Área tienes un texto para pegar en ChatGPT, Gemini o Copilot (gratis): te guía paso a paso y entiende
                las fotos que le mandes de la pantalla.
              </p>
            </details>
            <details>
              <summary>¿Puedo volver a la pantalla de antes?</summary>
              <p>Sí: Ajustes de CarVision › «Volver al launcher original», o desinstalarla como cualquier app.</p>
            </details>
          </div>
        </section>

        <section id="uso">
          <h2>Uso</h2>
          <div className="faq">
            <details>
              <summary>¿Lee las puertas, la velocidad y las luces de mi coche?</summary>
              <p>
                Todavía no. En esta versión esos datos están en modo demostración y la app lo marca con «Demo» en pantalla.
                Leer los datos reales depende del modelo de radio y llegará en próximas versiones.
              </p>
            </details>
            <details>
              <summary>¿Puedo cambiar el color o el logotipo?</summary>
              <p>Sí, en el Garaje: 12 colores al instante y el logotipo que quieras (con «Quitar» vuelves al de antes).</p>
            </details>
            <details>
              <summary>¿Se pone oscura de noche?</summary>
              <p>En automático, al encender las luces del coche si la radio da esa señal y, si no, por la hora. También puedes fijar claro u oscuro.</p>
            </details>
            <details>
              <summary>¿Funciona con Spotify y mi app de navegación?</summary>
              <p>Sí: abre las apps que ya tiene tu radio y muestra lo que suena en tu reproductor de música.</p>
            </details>
          </div>
        </section>

        <section id="coches">
          <h2>Coches</h2>
          <div className="faq">
            <details>
              <summary>¿Qué coches hay?</summary>
              <p>Ford Focus MK3 y Volkswagen Golf VI, terminados a mano, además del coche CarVision. Vamos añadiendo más. <Link href="/coches">Ver coches</Link></p>
            </details>
            <details>
              <summary>Mi coche no está</summary>
              <p>
                Tres opciones: pedirlo con «Te lo hacemos» ({euros(SERVICE_PRICE_CENTS)}), usar el coche CarVision con tu color y
                tu logotipo, o hacerlo tú con una IA de pago (<Link href="/hazlo-tu">opción avanzada</Link>).
              </p>
            </details>
            <details>
              <summary>¿Cuánto tarda «Te lo hacemos»?</summary>
              <p>Depende del coche: cada uno se prepara a mano. Te avisamos por correo cuando esté listo, y si no existe un modelo 3D válido, te devolvemos el dinero.</p>
            </details>
          </div>
        </section>

        <section id="compra">
          <h2>Compra</h2>
          <div className="faq">
            <details>
              <summary>¿Cuánto cuesta?</summary>
              <p>{euros(APP_PRICE_CENTS)}, pago único, sin suscripción. Incluye las actualizaciones de la versión 1.</p>
            </details>
            <details>
              <summary>¿Cómo pago?</summary>
              <p>Con tarjeta, Apple Pay o Google Pay, en Stripe. Nosotros no vemos los datos de tu tarjeta.</p>
            </details>
            <details>
              <summary>¿Cómo entro en mi Área de cliente?</summary>
              <p>Con el enlace que te llega por correo al pagar; no hay contraseña. Si lo pierdes, entra en <Link href="/area">Mi área</Link> y escribe tu correo.</p>
            </details>
            <details>
              <summary>¿Puedo devolverla?</summary>
              <p>
                Al ser un contenido digital que se entrega al momento, al comprar aceptas que la descarga empiece ya y no hay
                derecho de desistimiento. Si no te funciona, escríbenos y lo vemos contigo.
              </p>
            </details>
            <details>
              <summary>¿Qué incluye el código fuente?</summary>
              <p>
                El proyecto de la app y el generador de coches, para quien quiera adaptarla o hacer sus coches con una IA de
                pago. No hace falta para usar CarVision.
              </p>
            </details>
          </div>
        </section>

        <section id="privacidad">
          <h2>Privacidad</h2>
          <p>
            La app no envía tus datos a ningún sitio: todo se guarda en la radio. No pide root, ni accesibilidad, ni conectar
            la radio a un ordenador. En la web solo usamos tu correo para la compra. <Link href="/legal/privacidad">Política de privacidad</Link>
          </p>
        </section>

        <section id="contacto">
          <h2>Contacto</h2>
          <p>
            Escríbenos a <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>. Si es un problema con la radio, adjunta una
            foto de Ajustes › Acerca de y de lo que ves en pantalla.
          </p>
        </section>
      </main>
      <Pie />
    </>
  );
}
