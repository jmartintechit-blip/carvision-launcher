import Pendiente from "@/components/Pendiente";

// Datos del titular para las páginas legales. Se leen del entorno (panel de Vercel) para que no estén en el código
// publicado en GitHub. Lo que falte se marca como pendiente.
export default function Titular({ conEtiquetas = false }: { conEtiquetas?: boolean }) {
  const nombre = process.env.LEGAL_NAME;
  const nif = process.env.LEGAL_NIF;
  const domicilio = process.env.LEGAL_ADDRESS;
  return (
    <>
      {conEtiquetas ? "Titular: " : null}
      {nombre || <Pendiente>nombre del titular</Pendiente>}
      {conEtiquetas ? ". NIF: " : ", NIF "}
      {nif || <Pendiente>NIF</Pendiente>}
      {conEtiquetas ? ". Domicilio: " : ". "}
      {domicilio || <Pendiente>domicilio</Pendiente>}
    </>
  );
}
