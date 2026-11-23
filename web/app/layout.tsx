import type { Metadata, Viewport } from "next";
import { Archivo, Manrope } from "next/font/google";
import "./globals.css";

// Archivo ancho para los titulares (recuerda a los rótulos de modelo de un portón); Manrope para el texto, como la app.
const display = Archivo({ subsets: ["latin"], axes: ["wdth"], variable: "--f-display", display: "swap" });
const text = Manrope({ subsets: ["latin"], variable: "--f-text", display: "swap" });

export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL || "http://localhost:3100"),
  title: "CarVision · Tu coche en la pantalla de tu radio",
  description:
    "Pantalla de inicio para radios Android de coche con tu coche en 3D: tu modelo, tu color y el logotipo de tu marca al arrancar. 2 €.",
  openGraph: {
    title: "CarVision · Tu coche en la pantalla de tu radio",
    description: "Tu coche en 3D en la radio Android: tu modelo, tu color y tu marca al arrancar.",
    images: ["/img/app/inicio_golf.webp"],
    locale: "es_ES",
    type: "website",
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#F5F6F8" },
    { media: "(prefers-color-scheme: dark)", color: "#0A0C10" },
  ],
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es" className={`${display.variable} ${text.variable}`} suppressHydrationWarning>
      <head>
        {/* Tema elegido antes de pintar (sin parpadeo). Sin elección: sigue al sistema. */}
        <script
          dangerouslySetInnerHTML={{
            __html: `try{var t=localStorage.getItem("cv-tema");if(t==="light"||t==="dark")document.documentElement.setAttribute("data-theme",t)}catch(e){}`,
          }}
        />
      </head>
      <body>{children}</body>
    </html>
  );
}
