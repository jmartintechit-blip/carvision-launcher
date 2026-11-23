"use client";

import { useState } from "react";

// La radio de la portada: la app real en un marco doble DIN. Se cambia lo que se ve tocando los mandos.
type Vista = { id: string; label: string; alt: string } & ({ img: string } | { video: string; poster: string });
const VISTAS: Vista[] = [
  { id: "golf", label: "Golf VI", img: "/img/app/inicio_golf.webp", alt: "CarVision en la radio con un Volkswagen Golf VI azul en 3D, la hora, la música y los datos del viaje" },
  { id: "focus", label: "Focus MK3", img: "/img/app/inicio_focus.webp", alt: "CarVision con un Ford Focus MK3 naranja en 3D y el logotipo de Ford" },
  { id: "arranque", label: "Al encender", video: "/video/arranque_ford.mp4", poster: "/video/arranque_ford.webp", alt: "Animación de arranque: el óvalo de Ford aparece, lo cruza un destello de luz y da paso a la pantalla de inicio" },
  { id: "puerta", label: "Puerta abierta", img: "/img/app/puerta.webp", alt: "El Focus con la puerta delantera izquierda abierta en 3D y el aviso «Puerta delantera izquierda abierta»" },
  { id: "noche", label: "De noche", img: "/img/app/oscuro.webp", alt: "La pantalla de inicio en tema oscuro, que se pone sola al encender las luces del coche" },
];

export default function Radio() {
  const [vista, setVista] = useState(VISTAS[0]);
  const [tocado, setTocado] = useState(false); // el efecto de encendido solo al cambiar de vista, no al cargar
  const cls = tocado ? "entra" : undefined;
  return (
    <figure style={{ margin: 0 }}>
      <div className="radio">
        <div className="pantalla">
          {"img" in vista ? (
            <img key={vista.id} className={cls} src={vista.img} alt={vista.alt} width={1280} height={720} fetchPriority="high" />
          ) : (
            <video key={vista.id} className={cls} src={vista.video} poster={vista.poster} autoPlay muted loop playsInline aria-label={vista.alt} />
          )}
        </div>
      </div>
      <div className="mandos" role="group" aria-label="Ver en la pantalla">
        {VISTAS.map((v) => (
          <button key={v.id} type="button" className="mando" aria-pressed={v.id === vista.id} onClick={() => { setTocado(true); setVista(v); }}>
            {v.label}
          </button>
        ))}
      </div>
      <figcaption className="pie-radio">Capturas reales de la app en una pantalla de 1280 × 720. Los datos del viaje están en modo demostración.</figcaption>
    </figure>
  );
}
