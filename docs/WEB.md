# Web carvisiones.es (v1)

Código en `web/` (Next.js 16, sin base de datos). Creada el 2026-11-13 (D-030: lanzar ya la v1 con Focus y Golf).

## Cómo funciona

| Página | Qué hace |
|---|---|
| `/` | Portada para quien llega desde TikTok (móvil primero): la radio con la app real, qué hace, instalación en 3 pasos, catálogo, requisitos, compra y preguntas |
| `/comprar` (POST) | Comprueba la casilla de desistimiento y abre el pago de Stripe (2 €, `APP_PRICE_CENTS`) |
| `/gracias` | Comprueba con Stripe que el pago está hecho, crea el enlace del Área de cliente y lo envía por correo (si hay Resend) |
| `/area?t=…` | Área de cliente: descargas (APK y coches), texto para la IA con botón de copiar, instalación rápida, pedido «Te lo hacemos» |
| `/area` (sin enlace) | Pide el correo y, si tiene una compra pagada en Stripe, le reenvía el enlace (misma respuesta haya compra o no) |
| `/te-lo-hacemos` (POST) | Pago del servicio (2 €, `SERVICE_PRICE_CENTS`) con marca, modelo, año, puertas y color en los datos del pago |
| `/api/descargar/<id>?t=…` | Descarga protegida: solo con un enlace firmado de una compra de la app |
| `/funciones` | Todas las funciones con capturas reales y el vídeo de la animación de arranque; «Lo que viene» |
| `/coches` | Catálogo (Focus MK3, Golf VI) con fotos, coche CarVision, «Te lo hacemos» y enlace a «Hazlo tú» |
| `/instalar` | Guía paso a paso con capturas, ayuda con IA (gratis y de pago), volver a lo de antes y problemas |
| `/ayuda` | Preguntas frecuentes por temas y contacto |
| `/hazlo-tu` | Opción avanzada con IA de pago con acceso a archivos (D-031), con avisos |
| `/legal/*` | Aviso legal, privacidad y condiciones de compra |

- **Sin base de datos:** las compras están en Stripe. El enlace del Área es `datos.firma` (HMAC-SHA256 con
  `DOWNLOAD_SECRET`); solo el servidor puede fabricarlo.
- **Descargas fuera de `/public`:** `npm run sync` copia a `web/private/` la APK (`dist/CarVision.apk`), los coches
  (`dist/catalogo/*.zip`) y el texto para la IA (del bloque de `docs/prompts/PROMPT_IA_GRATUITA.md`).
- **Sin cookies** de analítica ni publicidad: no hace falta banner de cookies. El tema elegido (Auto/Claro/Oscuro) se
  guarda en el navegador (`localStorage`), no es una cookie.
- **Colores = los de la app** (`android/core/designsystem/Theme.kt`), con tema claro y oscuro; sigue al sistema.
- Capturas en `web/public/img/app` (emulador 1280×720, 2026-11-13) y vídeo de arranque en `web/public/video` (grabado
  con `adb screenrecord` y recortado con el editor de vídeo de Blender).
- El Área entrega también el código fuente (`scripts/make_source_zip.py`, sin material interno) y el texto para IA con
  acceso a archivos.
- Los pedidos «Te lo hacemos» se ven en el panel de Stripe (Pagos › el pago › Metadatos).

## Variables de entorno (solo en el panel de Vercel)

Ver `web/.env.example`. `STRIPE_SECRET_KEY`, `DOWNLOAD_SECRET` (32+ caracteres aleatorios), `RESEND_API_KEY`,
`MAIL_FROM`, `CONTACT_EMAIL`, `SITE_URL`, `APP_PRICE_CENTS`, `SERVICE_PRICE_CENTS`. Ninguna clave va en el código ni en
la app Android.

## Probar en local

```bash
cd web && npm install && npm run sync && npm run dev
```

Se abre en http://localhost:3100. Sin `STRIPE_SECRET_KEY`, el botón de pagar avisa de que el pago no está activo. Probado
el 2026-11-13: portada en escritorio y móvil (375 px), formulario de compra sin claves, Área de cliente con un enlace de
prueba, descarga protegida (200 con enlace válido; 403 sin enlace o con enlace falso) y `next build` sin errores.
**Sin probar todavía: un pago real de Stripe (modo prueba), el correo de Resend y la web publicada.**

## Para publicarla (lista de pasos, en orden)

Los pasos con 👤 los haces tú (cuentas, pagos, contraseñas); el resto lo hace Claude con tu visto bueno.

**Antes de nada**
1. 👤 **Probar la APK en la radio** (`dist/CarVision.apk`, 1.0.0). Va firmada con la clave de pruebas: sirve para
   probar, no para vender.
2. 👤 **Crear la clave de firma** (una vez en la vida de la app):
   `powershell -ExecutionPolicy Bypass -File scripts\crear_clave_firma.ps1`. Copia `%USERPROFILE%\.carvision` a un
   pendrive y a una nube privada; la contraseña, al gestor de contraseñas.
3. Claude: compilar la 1.0.0 firmada, copiarla a `dist/`, rehacer el zip del código (`scripts/make_source_zip.py`) y
   `npm run sync`. **En la radio de prueba hay que desinstalar la versión de prueba** antes de instalar la firmada
   (Android no deja actualizar entre claves distintas).
4. 👤 Datos que faltan en las páginas legales: código postal y ciudad; cómo se indica el IVA (gestoría).
5. 👤 Decidir el precio final de «Te lo hacemos» y si se mantiene la promesa de devolver el dinero.

**Publicar**
6. 👤 **Vercel:** cuenta (plan Hobby gratis vale para empezar). Claude crea el proyecto con la carpeta `web/` como raíz.
   Se publica **con la CLI desde `web/`** (`npx vercel@latest` y luego `--prod`), no desde Git: la APK y los zips de
   `web/private/` no están en Git (`.gitignore`) y sin ellos las descargas darían 503.
7. 👤 **Variables en Vercel** (Settings › Environment Variables), nunca en el código:
   - `DOWNLOAD_SECRET`: 32+ caracteres aleatorios (p. ej. `openssl rand -base64 48`). **No cambiarlo nunca después**:
     invalidaría los enlaces del Área de todos los clientes.
   - `STRIPE_SECRET_KEY`: primero la de pruebas `sk_test_…`.
   - `SITE_URL=https://carvisiones.es`, `APP_PRICE_CENTS=200`, `SERVICE_PRICE_CENTS` (según el paso 5),
     `MAIL_FROM`, `CONTACT_EMAIL`.
8. 👤 **Arsys (DNS):** al añadir carvisiones.es en Vercel (Settings › Domains) Vercel da los registros (un `A` para
   `carvisiones.es` y un `CNAME` para `www`); se ponen en el panel DNS de Arsys. Tarda de minutos a unas horas.
9. 👤 **Correo hola@carvisiones.es:** buzón en Arsys o reenvío a tu correo.
10. 👤 **Resend:** cuenta, añadir el dominio carvisiones.es, poner en Arsys los registros que da (SPF/DKIM) y
    `RESEND_API_KEY` en Vercel. Sin esto la web funciona, pero el enlace del Área solo se ve en `/gracias` (no llega
    por correo).

Estado el 2026-11-14: `next build` sin errores y prueba local en modo producción: todas las páginas 200, descargas con
enlace válido 200 (APK 1.0.0, Focus, Golf, código), sin enlace o con enlace falso 403.

Estado el 2026-11-20: **versión de prueba publicada en https://carvisiones.vercel.app** (proyecto Vercel
`carvisiones`, región París `cdg1`, publicada con la CLI desde `web/`). `web/vercel.json` fija Next.js: el proyecto
creado por la CLI no lo detectaba y todas las páginas daban 404. `DOWNLOAD_SECRET` ya está en Vercel (aleatorio,
oculto; no cambiarlo nunca). Sin `SITE_URL`, la web usa su dirección .vercel.app. carvisiones.es apunta aún a Arsys.
Comprobado en la web publicada: páginas 200, descargas sin enlace o con enlace falso 403, «Comprar» avisa de que el pago
no está activo, «recuperar enlace» responde igual haya compra o no, y los archivos de `web/private` están subidos.
**Sin probar:** una descarga con enlace real (se verá con la compra de prueba).
Volver a publicar tras cambios: `cd web && npm run sync && npx vercel@latest deploy --prod`.

**Comprobar antes de cobrar**
11. Compra de prueba con la tarjeta de pruebas de Stripe (`4242 4242 4242 4242`): pago → `/gracias` → correo → Área →
    descargar APK, Focus, Golf y código; «Te lo hacemos» → los datos del coche en Stripe; `/area` → recuperar enlace.
12. 👤 Cambiar a la clave real `sk_live_…` en Vercel y volver a publicar. Hacer una compra real de 2 € y
    devolvértela desde Stripe.
13. Después: TikTok y redes (aplazado; borrador en `docs/marketing/TIKTOK.md`).

**Límites de Vercel comprobados (2026-11-14):** las descargas salen por una función que **transmite el archivo por
partes** (stream), así que el límite de 4,5 MB de respuesta no se aplica (la APK pesa 17,5 MB); los archivos de
`web/private` (27 MB) entran en la función con holgura (máx. 250 MB).
