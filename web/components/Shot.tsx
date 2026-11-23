// Captura real de la app con el marco de la pantalla. `name` = archivo en /public/img/app (sin extensión).
export function Shot({ name, alt, caption, eager }: { name: string; alt: string; caption?: string; eager?: boolean }) {
  return (
    <figure className="captura">
      <div className="marco">
        <img
          src={`/img/app/${name}_640.webp`}
          srcSet={`/img/app/${name}_640.webp 640w, /img/app/${name}.webp 1280w`}
          sizes="(min-width: 880px) 620px, 100vw"
          alt={alt}
          width={1280}
          height={720}
          loading={eager ? undefined : "lazy"}
        />
      </div>
      {caption && <figcaption>{caption}</figcaption>}
    </figure>
  );
}

// Vídeo corto de la app (sin sonido, en bucle). Se ve el póster si el navegador no reproduce vídeo.
export function Clip({ src, poster, alt, caption }: { src: string; poster: string; alt: string; caption?: string }) {
  return (
    <figure className="captura">
      <div className="marco">
        <video src={src} poster={poster} autoPlay muted loop playsInline preload="metadata" aria-label={alt} width={960} height={540} />
      </div>
      {caption && <figcaption>{caption}</figcaption>}
    </figure>
  );
}
