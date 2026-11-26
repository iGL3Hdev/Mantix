import { createFileRoute } from "@tanstack/react-router";

import { AppLayout } from "@/components/AppLayout";

// La aplicación sólo se pinta en el navegador (decisión del 2026-11-25): los datos se piden a /api
// desde el cliente. La landing sigue con SSR
export const Route = createFileRoute("/app")({
  ssr: false,
  component: AppLayout,
});
