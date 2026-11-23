# Catálogo de coches listos (D-026)

Packs ya hechos para que el cliente solo tenga que descargar su coche e importarlo. Van **fuera de la app** (Área de
cliente / web): cada pack ocupa unos 2 MB y cada cliente descarga solo el suyo.

## Qué modelo vale para el catálogo

El catálogo se vende, así que el modelo 3D tiene que permitir uso comercial y redistribución:

- Licencia **CC BY** (citando al autor) o **CC0**. CC BY-SA solo si el pack se publica también con CC BY-SA.
  Las licencias NC (no comercial) y ND (sin modificaciones) **no valen**.
- **Hecho por quien lo sube.** Se descartan los modelos sacados de juegos aunque digan CC BY. Además de la descripción,
  hay que mirar el **perfil del autor**: muchas cuentas lo dicen claramente («Importing BeamNG, Assetto Corsa, GTA»,
  «I did not make any of these models») o tienen cientos de coches de marcas distintas.
- Misma generación y carrocería que el coche del catálogo; mejor con puertas separadas; menos de ~1 M triángulos.
- Pendiente de consultar antes de vender: los logotipos de marca son marcas registradas.

## Búsqueda del 2026-11-12 (46 coches, sin descargar nada)

Se buscaron en la API pública de Sketchfab los coches más comunes en España (2000–2018), con filtro «Downloadable» y
licencias CC BY / CC0. Script: `scripts/buscar_catalogo.py` (solo lee la búsqueda pública; no descarga modelos). Los perfiles de autor se
revisaron aparte con la API pública (`/v3/models/<uid>` → `/v3/users/<uid>`).

**Resultado: hay muchos menos modelos válidos de lo esperado.**

- Las cuentas con más coches «CC BY» son resubidas de juegos o de terceros. Ejemplos comprobados en su perfil:
  - Merc_TV / szymonpasterczyk: 994 modelos; dice que importa de BeamNG, Assetto Corsa y GTA San Andreas.
  - Nieve5677 / niev: 2.359 modelos; dice que no hizo ninguno.
  - Otras con cientos de coches de todas las marcas: tonielpro520, zapkolenko07, tangwang920, Car2022, Tyler_Dave,
    teenlin3, DR1KING100K, supercarmodels, amogusstrikesback2…
- Quitando esas cuentas, de 46 coches solo quedan **unos 10–14 candidatos razonables**, y no todos de la generación exacta.
- Ninguna procedencia se puede verificar al 100%: solo podemos descartar lo que tiene indicios claros.

### Candidatos que parecen originales (pendientes de ver y aprobar la descarga)

| Coche | Modelo en Sketchfab | Autor | Licencia | Triángulos | Nota |
|---|---|---|---|---|---|
| Renault Mégane II (fase 2) | [Renault Megane 2 Phase 2 Privilege Rouge Vif](https://sketchfab.com/3d-models/50740e1ed4ff4dfd8a246fccb7729f0f) | azul88007 | CC BY | 470 k | «Recreación de memoria»; el autor tiene 6 modelos |
| Hyundai ix35 / Tucson | [Hyundai ix35 (Tucson)](https://sketchfab.com/3d-models/e239ad27c5854f4189fe467ffb1ec303) | savelliy07 | CC BY | 165 k | Mismo autor que el Focus actual (16 modelos) |
| Mercedes Clase E W212 | [Mercedes-Benz E-Class (W212)](https://sketchfab.com/3d-models/9b70707fd2304f578175158564719c5d) | savelliy07 | CC BY | 205 k | Mismo autor que el Focus; no estaba en la lista, muy común (taxis) |
| Kia Sportage (2011–2014) | [Kia Sportage (SL 2011-2014)](https://sketchfab.com/3d-models/f15205a444fb42f7a2ebf6765a36026a) | farhad.Guli | CC BY | 82 k | |
| Kia Ceed | [KIA Ceed](https://sketchfab.com/3d-models/2874517681bc475e904f81ecf5dcb1db) | JakobOlbracht | CC BY | 269 k | Ver qué generación es |
| Hyundai i30 (2011) | [2011 Hyundai i30 FD](https://sketchfab.com/3d-models/036a13aefc3f48c5ba996aa2361b429e) | leeyunhe902 (D3NK) | CC BY | 168 k | Hecho por él (SketchUp) |
| Škoda Octavia III familiar | [Skoda Octavia RS](https://sketchfab.com/3d-models/852ea89f86e647998b11b7eb9cac6f8a) | vershinin.nvkz | CC BY | 219 k | Versión RS |
| Citroën C3 | [Citroën C3](https://sketchfab.com/3d-models/45058edec718498b8cf2b041e254ea3b) | klaxoneer | CC BY | 975 k | «Mi propio coche»; puede ser un escaneo de una pieza (sin aperturas) |
| Fiat 500 | [Fiat 500 Abarth](https://sketchfab.com/3d-models/c14dac5368f94dc886dbdd78e0cd9644) | attix84work | CC BY | 783 k | Versión Abarth |
| Opel Corsa D (5p) | [Opel Corsa 5Door 2011 Remake](https://sketchfab.com/3d-models/9517720f580e454cbf838beef310536b) | homicivan53 | CC BY | 974 k | «Remake» de origen no aclarado: revisar |

Sin candidato válido en esta búsqueda: Seat Ibiza, Seat León (las tres generaciones), Renault Clio, Ford Focus MK2,
Peugeot 206/207/208/307/308, Opel Astra, VW Golf, VW Polo, Dacia Sandero, Nissan Qashqai, Ford Fiesta, Audi A3,
BMW Serie 1 y 3, Seat Altea, Toyota Yaris, VW Passat, Citroën Berlingo… Justo varios de los más vendidos.

## Preselección por calidad (D-027, 2026-11-12)

Por decisión del cliente se eligen los modelos **por calidad**, sea o no el autor quien los creó (ver D-027). Elegidos
por generación exacta, triángulos, «me gusta» y descripción; **todavía sin verlos en 3D ni descargarlos**. La calidad
real (interior, puertas separadas, materiales) se comprueba al pasarlos por el generador.

| Coche | Modelo | Autor | Triángulos | Nota |
|---|---|---|---|---|
| Seat Ibiza 6J (2008–2017) | [Seat Ibiza MK4 2008-2017](https://sketchfab.com/3d-models/37a903d1a5c04138af2d5aa02a3b88f2) | Merc_TV | 82 k | También lo tiene la competencia |
| Seat Ibiza 6L (2002–2008) | [Seat/Cupra Ibiza MK3 2002-2008](https://sketchfab.com/3d-models/b04f24acc233404c8b1a9877904a7d74) | Merc_TV | 178 k |  |
| Seat León MK1 | [SEAT León MK1 1996-2006](https://sketchfab.com/3d-models/f6ee13885f2d461d84b9a17a2f36bb47) | Merc_TV | 132 k |  |
| Seat León MK2 | [SEAT León MK2 2005-2012](https://sketchfab.com/3d-models/9142860554c0423284930cff3231b74d) | Merc_TV | 257 k |  |
| Seat León MK3 | [SEAT LEON FR 2014](https://sketchfab.com/3d-models/15aa95f7983640d3931ead1bc8af4a78) | DR1KING100K | 1,2 M | Pesado: el generador lo reduce |
| VW Golf V | [vw golf 5 gti](https://sketchfab.com/3d-models/4389c32f54964b5ab01aa8462419b6c1) | Preview2SEBIT69 | 82 k | Versión GTI |
| VW Golf VI | [Volkswagen Golf 6. (A6)](https://sketchfab.com/3d-models/8d8a301cee5143e9a6f25519e32dde21) | Car2022 | 239 k |  |
| VW Golf VII | [Volkswagen Golf 7 mk7](https://sketchfab.com/3d-models/3c86d324d56f4027b08cbd7ba5f10bbb) | DR1KING100K | 624 k | También lo tiene la competencia (GTI) |
| VW Polo 9N | [Volkswagen Polo 3-door MK4 2001–2009](https://sketchfab.com/3d-models/6fd9f1259b8248059e32f983cf5b0cf7) | Merc_TV | 636 k | 3 puertas |
| VW Polo 6R | [Volkswagen Polo MK5 2009-2018](https://sketchfab.com/3d-models/d0463d789e7c46039e979511ec8f5e92) | Merc_TV | 102 k |  |
| Renault Clio III | [Renault Clio Sport MK3 3-Door 2005-2012](https://sketchfab.com/3d-models/5f9083517834442e9fa26ef12b4792ba) | Merc_TV | 106 k | 3 puertas, versión Sport |
| Renault Mégane II | [Renault Megane 2 Phase 2 Privilege Rouge Vif](https://sketchfab.com/3d-models/50740e1ed4ff4dfd8a246fccb7729f0f) | azul88007 | 470 k |  |
| Peugeot 206 | [Peugeot 206](https://sketchfab.com/3d-models/dc25a117378545e7a804c8594aa86938) | alieslami7878 | 492 k | El más valorado (109 «me gusta») |
| Peugeot 207 | [peugeot 207](https://sketchfab.com/3d-models/ef5d59b78b96496fab2731b138820ea2) | DR1KING100K | 324 k |  |
| Peugeot 307 | [2008 Peugeot 307](https://sketchfab.com/3d-models/eb357098ec05483986262ad8eb8ab0cc) | tonielpro520 | 392 k |  |
| Peugeot 308 (I) | [Peugeot 308 5-Door MK1 2007-2021](https://sketchfab.com/3d-models/ce299e549e594d029577d0b757f3aa87) | Merc_TV | 249 k |  |
| Opel Corsa D | [Opel Corsa 5-door D 2006-2014](https://sketchfab.com/3d-models/82d1344bfa0d481da9c5a7db5a6c6752) | Merc_TV | 206 k |  |
| Opel Astra H | [Opel Astra H 2004-2014](https://sketchfab.com/3d-models/06235fb76ab3403dbb0410c05cc3046d) | Merc_TV | 770 k |  |
| Opel Astra J | [Opel Astra J Sports Tourer 2012](https://sketchfab.com/3d-models/fb11a43e37464ad9931fdcbadf55e4dd) | Merc_TV | 200 k | Familiar |
| Citroën C3 (II) | [2015 Citroën C3](https://sketchfab.com/3d-models/0761dead5baf4775ac929ec0bf4c8708) | tonielpro520 | 404 k |  |
| Citroën C4 | [Citroen C4](https://sketchfab.com/3d-models/92bdc4235b414d34be2be249913ea625) | niev | 1,3 M | Ver generación |
| Ford Fiesta VII | [Ford Fiesta MK6 2008-2019](https://sketchfab.com/3d-models/b57e5114d605433abf6c6b9f7749b473) | Merc_TV | 111 k | Es la VII (2008); también la tiene la competencia |
| Audi A3 8P Sportback | [Audi A3 Sportback MK1 (8PA) 2003-2013](https://sketchfab.com/3d-models/a961c9d05c5941dc9f8821d07961305d) | Merc_TV | 445 k | También lo tiene la competencia |
| BMW Serie 3 E90 | [BMW 3 Series (E90) 2004-2012](https://sketchfab.com/3d-models/e14e0b434bfc4fa5ae28c10fe9379fb4) | Merc_TV | 293 k |  |
| BMW Serie 1 F20 | [BMW 1 Series MK2 (F20) 2011-2019](https://sketchfab.com/3d-models/ec91e167beb347b7a962fed023c2fd2d) | Merc_TV | 167 k |  |
| Mercedes Clase C W204 | [Mercedes-Benz C-Class MK3 (W204) 2007-2014](https://sketchfab.com/3d-models/c91f15e9518f4dd2b7574002778cfc28) | Merc_TV | 751 k |  |
| Dacia Sandero I | [Dacia Sandero MK1 2008-2014](https://sketchfab.com/3d-models/0d740619a6d747feab2977fa32a3c9da) | Merc_TV | 327 k |  |
| Dacia Duster I | [Dacia Duster MK1 2010-2017](https://sketchfab.com/3d-models/aa2e9feeda3a4aceb1405c41df5d4539) | Merc_TV | 121 k |  |
| Nissan Qashqai | [Nissan Qashqai 2010](https://sketchfab.com/3d-models/d26880a3c5464b87b7a5a67323775947) | niev | 1,4 M |  |
| Škoda Octavia II familiar | [2009 Skoda Octavia Combi](https://sketchfab.com/3d-models/9b9f212d14b34d44bebf9fbb22afa54a) | tonielpro520 | 484 k |  |
| Kia Sportage | [Kia Sportage (SL 2011-2014)](https://sketchfab.com/3d-models/f15205a444fb42f7a2ebf6765a36026a) | farhad.Guli | 82 k |  |
| Hyundai ix35 | [Hyundai ix35 (Tucson)](https://sketchfab.com/3d-models/e239ad27c5854f4189fe467ffb1ec303) | savelliy07 | 165 k | Mismo autor que el Focus |
| Hyundai i30 (II) | [2011 Hyundai i30 FD](https://sketchfab.com/3d-models/036a13aefc3f48c5ba996aa2361b429e) | leeyunhe902 | 168 k |  |
| Fiat 500 | [Fiat 500](https://sketchfab.com/3d-models/4606659ff31142e3b8080f995514452f) | topfrank2013 | 348 k | 336 «me gusta» |
| Peugeot 3008 | [Peugeot 3008](https://sketchfab.com/3d-models/4adec16209e149a99a135f5de6cfbb21) | niev | 844 k |  |
| Seat Altea | [Seat Altea](https://sketchfab.com/3d-models/0c2ccf67d29d4576acd130478d3a9c33) | niev | 1,4 M |  |
| Peugeot Partner | [Peugeot Partner Tepee](https://sketchfab.com/3d-models/cb53a34acb2546fcb3cae6850272b7e3) | niev | 882 k |  |
| Mercedes Clase E W212 | [Mercedes-Benz E-Class (W212)](https://sketchfab.com/3d-models/9b70707fd2304f578175158564719c5d) | savelliy07 | 205 k | Muy común (taxis) |

Sin modelo de la generación correcta en esta búsqueda: Renault Clio IV (la competencia sí lo tiene), Ford Focus MK2,
Renault Mégane III, Peugeot 208, Toyota Corolla/Auris, Toyota Yaris, VW Passat B6/B7, Kia Ceed I/II, Citroën
Xsara Picasso. Para esos: búsqueda más amplia (otras webs) o encargo.

## Opciones para los coches sin modelo gratuito

1. **Servicio «Te lo hacemos» bajo pedido**: solo si aparece un modelo válido para ese coche.
2. **Encargar modelos** a un modelador 3D con un contrato que nos ceda el uso en el catálogo. Cuesta dinero por coche,
   pero el modelo es nuestro y con las piezas ya separadas.
3. **Comprar modelos de tiendas** (CGTrader, TurboSquid…): hay que revisar la licencia de cada una; muchas prohíben
   redistribuir el archivo del modelo, y un pack `.zip` lo contiene.
4. **Coche por defecto** con la marca, el color y el nombre del cliente.

## Estado

| Paso | Estado |
|---|---|
| Lista de coches y búsqueda | ✅ Hecha (2026-11-12) |
| Preselección por calidad (38 coches) | ✅ Hecha, sin descargar |
| Aprobar descargas (cuenta de Sketchfab) | ✅ 5 de prueba aprobadas y descargadas (2026-11-13); las otras 33, pendientes |
| Generar y revisar los packs | 🔶 5 de prueba generados y revisados en el visor (abajo) |
| Publicarlos en el Área de cliente | ⏳ Con la web (fase 9) |

> **D-030 (2026-11-13): esta lista queda como reserva.** El catálogo será de 7–8 coches muy comunes, cada uno terminado
> a mano; la v1 sale solo con el Ford Focus MK3. Las descargas pendientes se cancelan.

## Prueba con 5 coches (2026-11-13)

Modelos de BeamNG (Merc_TV, Car2022). Problemas nuevos que traían y que ahora corrige el generador para todos:
la rueda viene **una sola vez en el centro del coche** (plantilla) → se copia a las 4 esquinas (en los bujes o, si no
hay, bajo cada paso de rueda); restos del juego (tapacubos suelto en el centro, placas bajo el suelo, piezas a metros)
→ se quitan; el coche se apoya sobre sus ruedas y se centra entre ellas.

| Coche | Ruedas | Aperturas | ¿Al catálogo? |
|---|---|---|---|
| VW Golf VI (Car2022) | ✅ 4 | ✅ 4 puertas, capó, portón | ✅ Sí |
| Opel Corsa D (Merc_TV) | ✅ 4 | ✅ 4 puertas, capó, portón | ✅ Sí (la pintura trae textura de suciedad del autor) |
| Seat Ibiza 6J SC (Merc_TV) | ✅ 4 | 🔶 solo capó: las puertas y el portón van soldados a la carrocería | 🔶 Vale, pero mejor buscar uno con puertas sueltas |
| Renault Clio III (Merc_TV) | ✅ 4 | 🔶 solo capó (igual que el Ibiza) | 🔶 Igual que el Ibiza |
| Seat León MK2 (Merc_TV) | ❌ solo llantas: el modelo no trae neumáticos | ❌ carrocería tuneada, piezas mezcladas | ❌ No: buscar otro León |

Lección: en Sketchfab los modelos de **Merc_TV** traen las puertas soldadas; preferir autores con piezas sueltas
(como Car2022) al descargar los 33 restantes. Cada pack se revisa en el visor antes de publicarlo.

## Catálogo de la v1 (D-030, 2026-11-13)

Cada coche se termina a mano: el generador hace la base (escala, ruedas, aperturas) y un **script de acabado propio**
(`vehicle-packs/<coche>/source/finish_*.py`, enganchado con `"finish"` en su `config.json`) corrige lo que ese modelo
trae mal. Se revisa pieza a pieza en el visor (fondo magenta para huecos, rayos para saber qué material se ve) y en la
app (inicio, Garaje, aperturas con la simulación, animación de arranque).

| Coche | Estado | Qué se le ha hecho |
|---|---|---|
| Ford Focus MK3 (`ford_focus_mk3_5p`) | ✅ Listo | Hecho a mano antes (`prepare_focus_mk3.py`); revisado de nuevo |
| VW Golf VI (`vw_golf_6_5p`) | ✅ Listo | Quitada la placa del juego que lo encogía un 14 %; materiales originales; verde y blancos de relleno → acabados reales (maletero, motor, bajos, rejilla); emblemas VW cromados; piezas duplicadas quitadas; logotipo VW |
| Opel Corsa D (`opel_corsa_d_5p`) | ⛔ Bloqueado | Materiales hechos a mano (venían todos en blanco), sin texturas de daños, normales rehechas, logotipo Opel. **La geometría del modelo viene abollada** (paragolpes trasero, puertas): no queda perfecto. Hace falta otro modelo base |

Logotipos: VW 2019 y Opel 2023 de Wikimedia Commons (marcados como dominio público), compuestos como insignia que se
lee sobre negro y sobre claro (`blender/scripts/render_svg_mask.py` + `compose_brand_badges.py`).
Paquetes listos: `dist/catalogo/ford_focus_mk3_5p.zip`, `dist/catalogo/vw_golf_6_5p.zip`.
