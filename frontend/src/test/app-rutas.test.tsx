import { QueryClient } from "@tanstack/react-query";
import { createMemoryHistory, createRouter } from "@tanstack/react-router";
import { describe, expect, it } from "vitest";

import { routeTree } from "@/routeTree.gen";

function crearRouter(rutaInicial = "/") {
  return createRouter({
    routeTree,
    context: { queryClient: new QueryClient() },
    history: createMemoryHistory({ initialEntries: [rutaInicial] }),
  });
}

describe("Rutas de la aplicación", () => {
  it.each(["/app/equipos", "/app/incidencias", "/app/ordenes"])(
    "%s tiene su propia página",
    (ruta) => {
      const router = crearRouter();

      expect(router.matchRoutes(ruta).at(-1)?.routeId).toBe(`${ruta}/`);
    },
  );

  it("/app lleva a la sección de equipos", async () => {
    const router = crearRouter("/app");

    await router.load();

    expect(router.state.location.pathname).toBe("/app/equipos");
  });

  it("las páginas de la aplicación se pintan solo en el navegador", () => {
    const router = crearRouter();

    expect(router.routesById["/app"].options.ssr).toBe(false);
  });
});
