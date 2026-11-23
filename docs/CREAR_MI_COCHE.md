# Poner mi coche en CarVision

> **Opción avanzada (D-031).** Para quien tiene el código fuente (incluido en la compra) y quiere preparar su propio
> coche. Sin soporte individual: el camino recomendado es el catálogo o «Te lo hacemos».

Guía para cambiar el coche de la app por el tuyo (marca, modelo, color, logotipo en la barra lateral y en la animación
de arranque). No hace falta saber programar ni tocar el código. Tiempo: entre 20 minutos y una hora.

> **Antes de nada:** mira si tu coche está en el **catálogo** de tu Área de cliente. Si está, solo tienes que
> descargarlo e importarlo (apartado D), sin nada de lo demás. Si no está, puedes pedir que te lo hagamos.
>
> **Consejo:** abre una IA gratuita (ChatGPT, Gemini, Copilot…), pega el texto de
> [`prompts/PROMPT_IA_GRATUITA.md`](prompts/PROMPT_IA_GRATUITA.md), escribe tu coche en la primera línea y te irá
> guiando paso a paso con esta misma guía. Si algo no sale, mándale una captura.

Lo que vas a hacer:

- **A.** Conseguir un modelo 3D gratuito de tu coche (con una licencia que lo permita).
- **B.** Instalar Blender (gratis) y el complemento de CarVision.
- **C.** Abrir el modelo en el panel de CarVision (pasos 1 a 5), revisar, corregir y crear el pack (un `.zip`).
- **D.** Importarlo en la app de la radio.

No se garantiza que exista un buen modelo de cualquier coche: de los más vendidos suele haber; de los raros, a veces
no. Si no encuentras el tuyo, la app sigue funcionando con el coche por defecto «CarVision».

---

## A. Conseguir el modelo 3D

Dónde buscar: **Sketchfab** (sketchfab.com). Hace falta una cuenta gratuita para descargar.

1. Busca la marca y el modelo, por ejemplo `seat leon mk1`.
2. Activa los filtros **Downloadable** (descargable) y, en **Licenses**, **CC Attribution** y **CC0**.
3. Abre los que te gusten y comprueba, en la página de cada uno:
   - **Licencia:** *CC Attribution* (hay que citar al autor) o *CC0* (dominio público). Otras licencias: solo uso personal.
   - **Que no sea robado de un juego:** si la descripción dice *GTA*, *Forza*, *Assetto*, *BeamNG*, *mod*, *ripped*,
     *exported from*… o da créditos a un estudio de videojuegos, descártalo: quien lo sube no tiene derecho a regalarlo.
     Mira también el **perfil del autor** (clic en su nombre): si dice que importa modelos de juegos o de otros, o tiene
     cientos de coches de marcas distintas, descártalo.
   - **Que tenga las puertas separadas** (ideal): en las imágenes se ven abiertas, o la descripción dice *opening doors*,
     *separated parts*, *rigged*. Si no, el coche se verá igual, pero sin abrir puertas.
   - **Tamaño razonable:** menos de ~1 millón de triángulos (lo pone en la página). Si tiene más, el generador lo reduce.
   - **La misma versión que tu coche:** si es otra (deportiva con alerón, otro año, 3 puertas…), se verá así en la app.
4. **Download** (descargar) › formato **GLB (.glb)**: es un solo archivo. Si viene en `.zip`, descomprímelo (clic derecho ›
   «Extraer todo»); el `.glb` suele estar dentro de la carpeta `source`.
5. Apunta el **nombre del autor** y el **enlace** de la página (suelen venir en `license.txt`): los pedirá el panel.

## B. Instalar Blender y el complemento

1. Descarga **Blender** (versión 4.2 o posterior) de [blender.org](https://www.blender.org/download/) e instálalo.
   La primera vez que lo abras sale la ventana **Quick Setup**: en **Language** elige **Español** y pulsa **Continue**.
   Así los menús saldrán en español, como en esta guía.
2. Abre Blender › **Editar › Preferencias › Complementos** › flecha **▾** (arriba a la derecha) › **Instalar desde disco…**
   (si Blender está en inglés: **Edit › Preferences › Add-ons › ▾ › Install from Disk…**)
3. Elige el archivo **`carvision_pack_builder.zip`** y actívalo (casilla marcada). Lo descargas del **Área de cliente**
   (enlace en el correo de compra de CarVision); no hay que descomprimirlo.
4. Cierra las preferencias. Pon el ratón sobre el visor 3D (la zona grande del centro) y pulsa la tecla **N**: a la
   derecha aparece una barra con pestañas verticales (Elemento, Herramienta, Vista…). Haz clic en la pestaña **CarVision**.
   El panel es largo: baja con la rueda del ratón para ver los pasos 4 y 5.

## C. El panel de CarVision (pasos 1 a 5)

### Paso 1 · Abrir modelo
- Pulsa el icono de carpeta y elige el archivo del modelo (`.glb`, `.gltf`, `.fbx`, `.obj` o `.blend`).
- Si conoces el **largo** de tu coche, de parachoques a parachoques (está en internet, p. ej. 4,18 m), escríbelo en
  **Largo del coche en metros** (con punto: 4.18): así el tamaño será exacto.
- Pulsa **Abrir y detectar piezas** y espera (de unos segundos a un par de minutos).

### Paso 2 · Revisar piezas
El coche aparece coloreado por grupos:

| Color | Grupo |
|---|---|
| Gris | Carrocería (lo que no se mueve) |
| Rojo / azul | Puerta delantera izquierda / derecha |
| Naranja / turquesa | Puerta trasera izquierda / derecha |
| Verde | Capó |
| Morado | Portón o maletero |
| Negro | Ruedas |

La lista dice cuántas piezas tiene cada grupo (✓ encontrado, ✗ no encontrado) y muestra los avisos.
Gira la vista con el botón central del ratón (o el gizmo de la esquina) y comprueba:
- ¿El **frontal** está donde debe? Si el coche ha quedado al revés, pulsa **«Frontal al revés: girar 180°»**.
- ¿Cada puerta está entera de su color (también la manilla, el cristal y el retrovisor si va en la puerta)?
- ¿Las **pinzas de freno** están en gris (no deben girar con la rueda)?

### Paso 3 · Corregir
- Para mover piezas a otro grupo: **selecciónalas en el visor** (clic; con **Mayús+clic** añades más; con la tecla **B**
  seleccionas con un recuadro), elige el grupo en la lista y pulsa **Asignar**.
  Elegir «Carrocería» quita una pieza de una puerta.
- **Ver grupo** marca en el visor todas las piezas del grupo elegido, para revisarlas.
- **Bisagra del capó / del portón:** lo normal es la primera opción; cámbiala si tu coche abre al revés.
- **Probar aperturas** abre puertas, capó y portón tal como se verán en la app. Vuelve a pulsarlo para cerrarlas.

### Paso 4 · Marca y logotipo
- **Marca**, **Modelo** y, si quieres, **Generación / años** (así se verá el nombre en la app).
- **Carrocería** y número de **puertas**.
- **Cambiar el color**: si el modelo no es del color de tu coche, márcalo y elige tu color (en el visor el coche
  sigue pintado por grupos: el color se ve en el pack y en sus imágenes de comprobación).
- **Logotipo:** una imagen del logotipo de la marca. Búscala en Google Imágenes («seat logo png», por ejemplo) y guarda
  una grande y nítida, mejor con fondo transparente o blanco (el fondo blanco se quita solo; el de cuadritos gris y blanco
  de muchas imágenes «transparentes» falsas, también). Saldrá en la barra lateral
  y en la animación de arranque. Sin logotipo, se muestra la marca en letras.
- **Licencia, autor y enlace** del modelo 3D (los del paso 1).

### Paso 5 · Crear pack
- (Opcional) elige la **carpeta de salida**; si no, se crea junto al modelo, en una carpeta `…_carvision`.
- Pulsa **Crear pack**. Al terminar verás «¡Listo!» y el nombre del `.zip`. **Abrir la carpeta** te lleva a él.
- En la carpeta `previews` hay 4 imágenes del coche cerrado y abierto: míralas antes de pasarlo a la radio. El logotipo
  no sale en ellas: se verá en la app (barra lateral y arranque). Está en `pack/brand/logo.png` por si quieres revisarlo.

> Después de crear el pack, el coche queda montado en Blender. Si quieres cambiar algo, vuelve al paso 1 (Abrir).

## D. Importarlo en la radio

1. Copia el `.zip` a un **pendrive** y conéctalo a la radio.
2. En CarVision: icono del **Garaje** (arriba, sobre el coche) › **Importar .zip** › se abre el selector de archivos:
   toca el menú **☰** (arriba a la izquierda), elige el pendrive (suele llamarse «USB») y toca el `.zip`.
3. Listo: tu coche, con su nombre y su logotipo, aparece en Inicio y en la animación de arranque.

Si la radio no tiene selector de archivos: copia el `.zip` a la carpeta `Android/data/com.carvision.launcher/files/import`
y pulsa **Buscar en la carpeta** en el Garaje. Si importas otra vez el mismo coche (por ejemplo, corregido), sustituye
al anterior.

## Avisos que puede dar el panel y qué hacer

| Aviso | Qué hacer |
|---|---|
| «No está claro dónde está el frontal» | Mira el coche: si el frontal está detrás, pulsa «Frontal al revés: girar 180°». |
| «El modelo venía con un tamaño raro…» | Escribe el largo del coche en el paso 1 y vuelve a pulsar «Abrir y detectar piezas». |
| «Solo se han encontrado N ruedas» | Selecciona las piezas de la rueda que falta y asígnalas a su grupo de rueda. |
| «No se han encontrado: Puerta…, Capó…» | Haz clic sobre esa parte del coche: si se ilumina solo ella, es una pieza suelta → elige su grupo y «Asignar» (Mayús+clic para añadir sus otras piezas). Si se ilumina toda la carrocería, no se puede abrir: el coche funciona igual. |
| «Falta la licencia» / «no se puede redistribuir» | El pack queda como **solo uso personal**. Para compartirlo, usa un modelo CC BY o CC0. |
| «No se pudo preparar el logotipo» | Prueba con otra imagen (PNG). Sin logotipo se muestra la marca en letras. |
| «No se ha encontrado el material de la pintura» | El coche sale con su color original; «Cambiar el color» no tendrá efecto. |
