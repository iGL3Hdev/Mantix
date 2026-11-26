import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/app/equipos/")({
  head: () => ({ meta: [{ title: "Equipos · Mantix" }] }),
  component: Pagina,
});

function Pagina() {
  return <h1 className="font-display text-2xl font-semibold text-foreground">Equipos</h1>;
}
