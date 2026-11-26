import { QueryClient } from "@tanstack/react-query";
import { RouterProvider, createMemoryHistory, createRouter } from "@tanstack/react-router";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { routeTree } from "@/routeTree.gen";

async function pintarEn(ruta: string) {
  const router = createRouter({
    routeTree,
    context: { queryClient: new QueryClient() },
    history: createMemoryHistory({ initialEntries: [ruta] }),
  });
  await router.load();
  render(<RouterProvider router={router} />);
}

describe("Layout de la aplicación", () => {
  it("enlaza a las tres secciones de la aplicación", async () => {
    await pintarEn("/app/equipos");

    expect(screen.getByRole("link", { name: "Equipos" }).getAttribute("href")).toBe("/app/equipos");
    expect(screen.getByRole("link", { name: "Incidencias" }).getAttribute("href")).toBe(
      "/app/incidencias",
    );
    expect(screen.getByRole("link", { name: "Órdenes" }).getAttribute("href")).toBe("/app/ordenes");
  });

  it("muestra la página de la sección dentro del layout", async () => {
    await pintarEn("/app/incidencias");

    expect(screen.getByRole("heading", { name: "Incidencias" })).toBeTruthy();
  });

  it("marca como activa la sección en la que estás", async () => {
    await pintarEn("/app/incidencias");

    const activo = screen.getByRole("link", { name: "Incidencias" }).closest("[data-active]");
    expect(activo?.getAttribute("data-active")).toBe("true");
    const otro = screen.getByRole("link", { name: "Equipos" }).closest("[data-active]");
    expect(otro?.getAttribute("data-active")).toBe("false");
  });

  it("tiene un enlace al inicio de la aplicación con el logo", async () => {
    await pintarEn("/app/equipos");

    expect(
      screen.getByRole("link", { name: "Mantix, inicio de la aplicación" }).getAttribute("href"),
    ).toBe("/app");
  });
});
