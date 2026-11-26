import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { getUsuarioId, setUsuarioId, subscribe, useUsuarioId } from "@/lib/usuario-actual";

const CLAVE = "mantix.usuarioId";

describe("usuario actual", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("no hay usuario hasta que se elige uno", () => {
    expect(getUsuarioId()).toBeNull();
  });

  it("recuerda el usuario elegido", () => {
    setUsuarioId(2);

    expect(getUsuarioId()).toBe(2);
  });

  it("lo guarda en el navegador para que sobreviva a una recarga", () => {
    setUsuarioId(2);

    expect(localStorage.getItem(CLAVE)).toBe("2");
  });

  it("lee el usuario que ya estaba guardado", () => {
    localStorage.setItem(CLAVE, "3");

    expect(getUsuarioId()).toBe(3);
  });

  it("ignora un valor guardado que no es un identificador válido", () => {
    for (const valor of ["abc", "", "0", "-4", "1.5"]) {
      localStorage.setItem(CLAVE, valor);

      expect(getUsuarioId()).toBeNull();
    }
  });

  it("se puede dejar sin usuario", () => {
    setUsuarioId(2);

    setUsuarioId(null);

    expect(getUsuarioId()).toBeNull();
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });

  it("avisa a quien está suscrito cada vez que cambia, y deja de avisar al darse de baja", () => {
    const aviso = vi.fn();
    const darDeBaja = subscribe(aviso);

    setUsuarioId(2);
    expect(aviso).toHaveBeenCalledTimes(1);

    darDeBaja();
    setUsuarioId(3);
    expect(aviso).toHaveBeenCalledTimes(1);
  });

  it("el hook devuelve el usuario y se actualiza cuando cambia", () => {
    const { result } = renderHook(() => useUsuarioId());
    expect(result.current).toBeNull();

    act(() => setUsuarioId(2));

    expect(result.current).toBe(2);
  });
});
