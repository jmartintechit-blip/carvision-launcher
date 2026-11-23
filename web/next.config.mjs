/** @type {import('next').NextConfig} */
const nextConfig = {
  // Las descargas (APK y coches) viven fuera de /public: solo se sirven con un enlace firmado.
  outputFileTracingIncludes: {
    "/api/descargar/[file]": ["./private/**"],
    "/area": ["./private/prompt.txt", "./private/prompt_archivos.txt", "./private/files.json"],
  },
  poweredByHeader: false,
};
export default nextConfig;
