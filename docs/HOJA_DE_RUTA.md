# Hoja de ruta hasta la web desplegada (carvisiones.es)

Creada el 2026-11-12 a partir de D-026, D-027 y D-028 (`DECISIONS.md`) y del análisis de la competencia
(`docs/ANALISIS_RD_V129.md`). Se actualiza al cerrar cada etapa.

## Etapas

| # | Etapa | Qué incluye | Depende de ti | Estado |
|---|---|---|---|---|
| 1 | **Instalación ×5 en la app** | Un solo APK para todas las radios; asistente de primer arranque; pendrive automático (detecta packs al pinchar el USB); color del coche desde la app; botón «Volver al launcher original» | Nada | ✅ Hecha en emulador (v0.10.0); falta radio real |
| 2 | **Catálogo de coches** | Descargar los modelos preseleccionados (`docs/CATALOGO_COCHES.md`), generar y revisar los packs, fichas con miniatura | Iniciar sesión en Sketchfab en el navegador integrado y aprobar las descargas | ⏳ |
| 3 | **Web carvisiones.es** | Portada pensada para TikTok (móvil primero), pago, área de cliente (descargas, catálogo, «Te lo hacemos», prompt para copiar), guía de instalación de 3 pasos, aviso legal y condiciones | Cuentas (Stripe, alojamiento, correo), DNS del dominio, precio | ⏳ |
| 4 | **Datos reales del coche (fase 4, CAN)** | Detectar el tipo de radio, leer puertas/maletero/intermitentes/velocidad sin permisos especiales, «Aprender mi coche», diagnóstico por QR | Modelo de tu radio y pruebas en el coche | ⏳ Se puede hacer en paralelo |
| 5 | **Prueba en radio real y lanzamiento** | Instalar la APK final en tu radio, probar el asistente y el catálogo, publicar la web | Tu radio | ⏳ |

## PRÓXIMO PASO (al decir «continuar»): revisión visual del generador

Pedido por el cliente el 2026-11-12, **antes del catálogo y de la web**. El primer coche, hecho a mano (Focus /
coche por defecto), está bien. El importado hecho solo con el generador automático tiene fallos, y si pasan en uno
pasarán en todos. Hay que revisarlo entero y arreglar el generador para que todos los coches salgan perfectos:

1. **Pintura mal repartida**: partes que deberían ir pintadas salen sin pintar (p. ej. la zona del parachoques de los
   antiniebla) y partes que no se pintan salen del color de la carrocería (discos de freno, intermitentes laterales
   de los retrovisores, pinzas de freno en azul).
2. **Llantas raras** comparadas con las del coche hecho a mano.
3. **Asientos en verde chillón**, como si estuvieran rotos.
4. **La puerta, al abrirse, se ve muy mal**.
5. **Buscar el resto de fallos**: revisión completa (materiales, cristales, interior, luces, ruedas, aperturas, sombras)
   comparando con el coche hecho a mano, y dejar comprobaciones en el generador para que no vuelvan a pasar.

Datos de las radios del cliente para la fase 4: `docs/RADIOS.md`.

## Propuesta técnica para la web (etapa 3)

- **Next.js en Vercel** (gratis para empezar): portada estática muy rápida y área de cliente.
- **Base de datos Neon (Postgres)**: clientes, compras, peticiones «Te lo hacemos».
- **Archivos** (APK, packs): almacenamiento con enlaces de descarga temporales, solo para clientes.
- **Pago con Stripe Checkout** (tarjeta, Apple Pay, Google Pay); sin datos de tarjeta en nuestra web.
- **Acceso sin contraseña**: tras pagar llega un correo con un enlace mágico (correo con Resend desde
  `hola@carvisiones.es`).
- Las claves secretas solo viven en el panel de Vercel (nunca en el código ni en la app Android).
- Idioma: español primero; preparada para inglés, francés, alemán, italiano y portugués.

## Lo que necesito de ti (cuando puedas; nada bloquea la etapa 1)

1. **Tu radio**: marca y modelo, chip (Ajustes › Acerca de), versión de Android, marca de la caja CAN y su app.
2. **Sketchfab**: iniciar sesión en el navegador integrado para descargar los modelos (te digo cada archivo antes).
3. **Cuentas para la web**, a tu nombre: Stripe, Vercel, Neon y Resend (te guío cuando lleguemos). Las crea y las
   inicia el titular; yo nunca escribo contraseñas ni claves.
4. **Dónde compraste carvisiones.es**, para apuntar el DNS a la web.
5. **Precios**: app (¿2 €, como la competencia, o distinto?) y «Te lo hacemos».
