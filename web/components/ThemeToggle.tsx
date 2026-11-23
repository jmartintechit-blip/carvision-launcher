"use client";

import { useEffect, useState } from "react";

// Igual que Ajustes › Tema de la app: Auto (sigue al móvil u ordenador), Claro u Oscuro. Se recuerda en este navegador.
type Tema = "auto" | "light" | "dark";
const OPCIONES: { id: Tema; label: string }[] = [
  { id: "auto", label: "Auto" },
  { id: "light", label: "Claro" },
  { id: "dark", label: "Oscuro" },
];

function aplicar(t: Tema) {
  const el = document.documentElement;
  if (t === "auto") el.removeAttribute("data-theme");
  else el.setAttribute("data-theme", t);
}

export default function ThemeToggle() {
  const [tema, setTema] = useState<Tema>("auto");
  useEffect(() => {
    try {
      const t = localStorage.getItem("cv-tema");
      if (t === "light" || t === "dark") setTema(t);
    } catch {
      /* sin almacenamiento: se queda en Auto */
    }
  }, []);
  return (
    <div className="tema" role="group" aria-label="Tema">
      {OPCIONES.map((o) => (
        <button
          key={o.id}
          type="button"
          aria-pressed={tema === o.id}
          onClick={() => {
            setTema(o.id);
            aplicar(o.id);
            try {
              if (o.id === "auto") localStorage.removeItem("cv-tema");
              else localStorage.setItem("cv-tema", o.id);
            } catch {
              /* nada */
            }
          }}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
