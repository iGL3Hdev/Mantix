import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/app/ordenes/")({
  head: () => ({ meta: [{ title: "Órdenes · Mantix" }] }),
  component: Pagina,
});

function Pagina() {
  return <h1 className="font-display text-2xl font-semibold text-foreground">Órdenes</h1>;
}
