import { Pie, Top } from "@/components/Chrome";
import CopyButton from "@/components/CopyButton";
import { CONTACT_EMAIL, SERVICE_PRICE_CENTS, euros } from "@/lib/config";
import { listFiles, readPrompt, sizeLabel } from "@/lib/files";
import { verifyAccess } from "@/lib/token";

export const dynamic = "force-dynamic";
export const metadata = { title: "Mi área · CarVision", robots: { index: false } };

type Props = { searchParams: Promise<Record<string, string | undefined>> };

export default async function Area({ searchParams }: Props) {
  const q = await searchParams;
  const access = verifyAccess(q.t);

  if (!access) {
    return (
      <>
        <Top />
        <main className="wrap hoja">
          <h1>Mi área</h1>
          {q.enviado ? (
            <p style={{ marginTop: 16 }} role="status">
              Si ese correo tiene una compra, en unos minutos te llega un enlace para entrar. Mira también en spam.
            </p>
          ) : (
            <p style={{ marginTop: 16 }}>
              Se entra con el enlace que te enviamos al comprar. Si no lo encuentras, escribe el correo con el que pagaste.
            </p>
          )}
          <form method="post" action="/area/recuperar" className="campos" style={{ maxWidth: 460 }}>
            <label className="campo">
              Correo de la compra
              <input type="email" name="email" required autoComplete="email" inputMode="email" />
            </label>
            <button className="btn" type="submit">Enviarme el enlace</button>
          </form>
        </main>
        <Pie />
      </>
    );
  }

  const t = encodeURIComponent(q.t!);
  const files = listFiles();
  const app = files.filter((f) => f.kind === "app");
  const cars = files.filter((f) => f.kind === "car");
  const code = files.filter((f) => f.kind === "code");
  const prompt = readPrompt();
  const promptArchivos = readPrompt("archivos");

  return (
    <>
      <Top />
      <main className="wrap hoja">
        <h1>Mi área</h1>
        <p style={{ marginTop: 12 }}>Compra de {access.e}. Guarda este enlace en favoritos: es tu acceso.</p>

        {access.k === "app" ? (
          <>
            <h2>1. Descarga la app y tu coche</h2>
            <p>Cópialos a un pendrive. El .zip del coche no hace falta descomprimirlo.</p>
            <ul className="descargas">
              {[...app, ...cars].map((f) => (
                <li key={f.id} className="descarga">
                  <div><b>{f.title}</b><span>{f.name} · {sizeLabel(f.bytes)}</span></div>
                  <a className="btn sec peq" href={`/api/descargar/${f.id}?t=${t}`} download>Descargar</a>
                </li>
              ))}
            </ul>

            <h2>2. Deja que una IA te guíe</h2>
            <p>
              Copia este texto en ChatGPT, Gemini o Copilot (gratis), cambia la primera línea por tu coche y sigue sus pasos.
              Si algo no sale como dice, mándale una foto de la pantalla.
            </p>
            <pre className="prompt">{prompt}</pre>
            <p style={{ marginTop: 12 }}><CopyButton text={prompt} /></p>

            <h2>3. Instalación rápida (si prefieres hacerlo solo)</h2>
            <ol>
              <li>En la radio, abre el explorador de archivos › el pendrive › toca «CarVision.apk» › Instalar.</li>
              <li>Abre CarVision y sigue el asistente: pantalla de inicio, tu coche y música.</li>
              <li>Con la app abierta, pincha el pendrive › «Buscar mi coche» › Instalar.</li>
            </ol>
            <p style={{ marginTop: 12 }}>Con capturas, en la <a href="/instalar">guía de instalación</a>.</p>

            <h2 id="avanzado">Avanzado: hazlo tú con una IA de pago</h2>
            <p>
              Si usas una IA que trabaja con archivos en tu ordenador (como Claude Code), descarga el código, ábrela en esa
              carpeta y pégale este texto: prepara tu coche con el generador incluido. Opción sin soporte individual;{" "}
              <a href="/hazlo-tu">lee antes cómo funciona</a>.
            </p>
            <ul className="descargas">
              {code.map((f) => (
                <li key={f.id} className="descarga">
                  <div><b>{f.title}</b><span>{f.name} · {sizeLabel(f.bytes)}</span></div>
                  <a className="btn sec peq" href={`/api/descargar/${f.id}?t=${t}`} download>Descargar</a>
                </li>
              ))}
            </ul>
            <pre className="prompt">{promptArchivos}</pre>
            <p style={{ marginTop: 12 }}><CopyButton text={promptArchivos} label="Copiar el texto para IA con archivos" /></p>
          </>
        ) : (
          <>
            <h2>Tu pedido «Te lo hacemos»</h2>
            <p>Estamos preparando tu coche. Te escribimos al correo de la compra cuando esté listo.</p>
          </>
        )}

        <h2 id="te-lo-hacemos">¿Tu coche no está? Te lo hacemos</h2>
        <p>Dinos qué coche es y lo preparamos para ti por {euros(SERVICE_PRICE_CENTS)}. Te avisamos por correo.</p>
        {q.falta === "coche" && <p className="aviso" role="alert" style={{ marginTop: 12 }}>Falta la marca, el modelo o el año.</p>}
        {q.falta === "acepto" && <p className="aviso" role="alert" style={{ marginTop: 12 }}>Marca la casilla para continuar.</p>}
        {q.pago === "no-disponible" && <p className="aviso" role="alert" style={{ marginTop: 12 }}>El pago aún no está activo.</p>}
        <form method="post" action="/te-lo-hacemos" style={{ maxWidth: 640 }}>
          <input type="hidden" name="volver" value={`/area?t=${t}#te-lo-hacemos`} />
          <div className="campos dos">
            <label className="campo">Marca<input name="marca" required maxLength={40} placeholder="Seat" /></label>
            <label className="campo">Modelo<input name="modelo" required maxLength={40} placeholder="León" /></label>
            <label className="campo">Año<input name="anio" required maxLength={9} inputMode="numeric" placeholder="2012" /></label>
            <label className="campo">
              Puertas
              <select name="puertas" defaultValue="5"><option>3</option><option>5</option><option>4 (berlina)</option></select>
            </label>
            <label className="campo">Color<input name="color" maxLength={30} placeholder="Gris" /></label>
          </div>
          <label className="check" style={{ marginTop: 16, color: "var(--texto-2)" }}>
            <input type="checkbox" name="acepto" value="si" required />
            <span>Acepto las condiciones del servicio. Si no existe un modelo 3D válido de mi coche, me devuelven el dinero.</span>
          </label>
          <p style={{ marginTop: 16 }}><button className="btn" type="submit">Pedir mi coche · {euros(SERVICE_PRICE_CENTS)}</button></p>
        </form>

        <h2>¿Dudas?</h2>
        <p>Escríbenos a <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a> con una foto de la pantalla de la radio.</p>
      </main>
      <Pie />
    </>
  );
}
