import Link from "next/link";
import ThemeToggle from "./ThemeToggle";
import { CONTACT_EMAIL } from "@/lib/config";

const LINKS = [
  { href: "/funciones", label: "Funciones" },
  { href: "/coches", label: "Coches" },
  { href: "/instalar", label: "Instalar" },
  { href: "/ayuda", label: "Ayuda" },
  { href: "/area", label: "Mi área" },
];

export function Top({ actual }: { actual?: string }) {
  return (
    <header className="cabecera">
      <div className="wrap">
        <Link href="/" className="marca">CARVISION</Link>
        <nav className="nav" aria-label="Principal">
          {LINKS.map((l) => (
            <Link key={l.href} href={l.href} aria-current={actual === l.href ? "page" : undefined}>{l.label}</Link>
          ))}
        </nav>
        <div className="tema-cabecera"><ThemeToggle /></div>
        <details className="menu-movil">
          <summary>Menú</summary>
          <nav aria-label="Principal">
            <div className="tema-menu"><ThemeToggle /></div>
            <Link href="/">Inicio</Link>
            {LINKS.map((l) => <Link key={l.href} href={l.href}>{l.label}</Link>)}
            <Link href="/#comprar">Comprar</Link>
          </nav>
        </details>
      </div>
    </header>
  );
}

export function Pie() {
  return (
    <footer className="pie">
      <div className="wrap">
        <nav aria-label="Más">
          <Link href="/funciones">Funciones</Link>
          <Link href="/coches">Coches</Link>
          <Link href="/instalar">Instalar</Link>
          <Link href="/hazlo-tu">Hazlo tú con una IA</Link>
          <Link href="/ayuda">Ayuda</Link>
          <Link href="/area">Mi área</Link>
        </nav>
        <nav aria-label="Legal">
          <Link href="/legal/aviso-legal">Aviso legal</Link>
          <Link href="/legal/privacidad">Privacidad</Link>
          <Link href="/legal/condiciones">Condiciones de compra</Link>
          <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>
        </nav>
        <p>
          CarVision no está afiliada a Ford, Volkswagen ni a ninguna otra marca. Los nombres y logotipos de marca se usan
          solo para identificar el coche de cada cliente.
        </p>
      </div>
    </footer>
  );
}
