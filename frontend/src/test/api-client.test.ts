import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiError, apiFetch } from "@/lib/api-client";
import { setUsuarioId } from "@/lib/usuario-actual";

const fetchMock = vi.fn();

function responder(estado: number, cuerpo: unknown) {
  const tipo = estado >= 400 ? "application/problem+json" : "application/json";
  fetchMock.mockResolvedValueOnce(
    new Response(JSON.stringify(cuerpo), { status: estado, headers: { "content-type": tipo } }),
  );
}

function ultimaPeticion() {
  const [url, init] = fetchMock.mock.lastCall as [string, RequestInit];
  return { url, init, cabeceras: init.headers as Record<string, string> };
}

describe("apiFetch", () => {
  beforeEach(() => {
    localStorage.clear();
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("peticiones", () => {
    it("pide la ruta bajo /api y devuelve el JSON de la respuesta", async () => {
      responder(200, [{ id: 1, nombre: "Ana Ruiz" }]);

      const usuarios = await apiFetch<{ id: number; nombre: string }[]>("/usuarios");

      expect(ultimaPeticion().url).toBe("/api/usuarios");
      expect(usuarios).toEqual([{ id: 1, nombre: "Ana Ruiz" }]);
    });

    it("añade los parámetros con valor y omite los vacíos", async () => {
      responder(200, []);

      await apiFetch("/equipos", {
        params: { estado: "AVERIADO", ubicacionId: 2, q: "", activo: undefined, nulo: null },
      });

      expect(ultimaPeticion().url).toBe("/api/equipos?estado=AVERIADO&ubicacionId=2");
    });

    it("codifica los parámetros con caracteres especiales", async () => {
      responder(200, []);

      await apiFetch("/equipos", { params: { q: "bomba de agua & aceite" } });

      expect(ultimaPeticion().url).toBe("/api/equipos?q=bomba+de+agua+%26+aceite");
    });

    it("es un GET y no lleva cuerpo cuando no se indica nada más", async () => {
      responder(200, []);

      await apiFetch("/equipos");

      const { init, cabeceras } = ultimaPeticion();
      expect(init.method).toBe("GET");
      expect(init.body).toBeUndefined();
      expect(cabeceras["Accept"]).toBe("application/json");
      expect(cabeceras["Content-Type"]).toBeUndefined();
    });

    it("envía el cuerpo como JSON en un POST", async () => {
      responder(201, { id: 9 });

      await apiFetch("/equipos", {
        metodo: "POST",
        cuerpo: { codigo: "BOM-001", nombre: "Bomba" },
      });

      const { init, cabeceras } = ultimaPeticion();
      expect(init.method).toBe("POST");
      expect(init.body).toBe('{"codigo":"BOM-001","nombre":"Bomba"}');
      expect(cabeceras["Content-Type"]).toBe("application/json");
    });

    it("envía la cabecera X-Usuario-Id con el usuario actual", async () => {
      setUsuarioId(2);
      responder(200, []);

      await apiFetch("/equipos");

      expect(ultimaPeticion().cabeceras["X-Usuario-Id"]).toBe("2");
    });

    it("no envía la cabecera X-Usuario-Id si no hay usuario elegido", async () => {
      responder(200, []);

      await apiFetch("/equipos");

      expect(ultimaPeticion().cabeceras["X-Usuario-Id"]).toBeUndefined();
    });
  });

  describe("errores", () => {
    it("un error de la API se convierte en un ApiError con su estado, su código y su mensaje", async () => {
      responder(409, {
        codigo: "RN-12",
        detail: "El equipo 7 tiene incidencias sin cerrar.",
        title: "Regla de negocio incumplida",
      });

      const error = apiFetch("/equipos/7/baja", { metodo: "POST", cuerpo: {} });

      await expect(error).rejects.toBeInstanceOf(ApiError);
      await expect(error).rejects.toMatchObject({
        name: "ApiError",
        estado: 409,
        codigo: "RN-12",
        message: "El equipo 7 tiene incidencias sin cerrar.",
        errores: [],
      });
    });

    it("los errores de validación traen la lista de campos", async () => {
      responder(400, {
        codigo: "VALIDACION",
        detail: "Hay datos que no son válidos.",
        errores: [{ campo: "nombre", mensaje: "El nombre es obligatorio." }],
      });

      await expect(apiFetch("/equipos", { metodo: "POST", cuerpo: {} })).rejects.toMatchObject({
        estado: 400,
        codigo: "VALIDACION",
        errores: [{ campo: "nombre", mensaje: "El nombre es obligatorio." }],
      });
    });

    it("si el servidor responde con algo que no es JSON (por ejemplo un 502 del proxy) es un error de red", async () => {
      fetchMock.mockResolvedValueOnce(new Response("<html>Bad Gateway</html>", { status: 502 }));

      await expect(apiFetch("/equipos")).rejects.toMatchObject({
        estado: 502,
        codigo: "ERROR_DE_RED",
        message: "No se ha podido conectar con el servidor.",
      });
    });

    it("si no hay conexión con el servidor es un error de red con estado 0", async () => {
      fetchMock.mockRejectedValueOnce(new TypeError("Failed to fetch"));

      const error = apiFetch("/equipos");

      await expect(error).rejects.toBeInstanceOf(ApiError);
      await expect(error).rejects.toMatchObject({
        estado: 0,
        codigo: "ERROR_DE_RED",
        message: "No se ha podido conectar con el servidor.",
      });
    });
  });
});
