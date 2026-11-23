// Dato que tiene que dar el titular antes de publicar (aviso legal). Se ve marcado para que no se olvide.
export default function Pendiente({ children }: { children: React.ReactNode }) {
  return <span className="pendiente">{children}</span>;
}
