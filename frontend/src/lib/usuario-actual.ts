import { useSyncExternalStore } from "react";

// El usuario que "hace" las acciones en el MVP (no hay login real hasta la fase 2).
// Se guarda en el navegador para que sobreviva a una recarga y el cliente de la API lo envía
// en la cabecera X-Usuario-Id.
const CLAVE = "mantix.usuarioId";

const oyentes = new Set<() => void>();

export function getUsuarioId(): number | null {
  const guardado = localStorage.getItem(CLAVE);
  if (guardado === null) return null;
  const id = Number(guardado);
  return Number.isInteger(id) && id > 0 ? id : null;
}

export function setUsuarioId(id: number | null): void {
  if (id === null) {
    localStorage.removeItem(CLAVE);
  } else {
    localStorage.setItem(CLAVE, String(id));
  }
  oyentes.forEach((oyente) => oyente());
}

export function subscribe(oyente: () => void): () => void {
  oyentes.add(oyente);
  return () => {
    oyentes.delete(oyente);
  };
}

export function useUsuarioId(): number | null {
  // El tercer argumento es el valor en el servidor, donde no hay localStorage.
  return useSyncExternalStore(subscribe, getUsuarioId, () => null);
}
