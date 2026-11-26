import { getUsuarioId } from "@/lib/usuario-actual";

// Cliente común de la API: añade la base /api, convierte el cuerpo a JSON, envía el usuario
// actual en X-Usuario-Id y traduce los errores a ApiError.
const BASE = "/api";
const SIN_CONEXION = "No se ha podido conectar con el servidor.";

export type ErrorDeCampo = { campo: string; mensaje: string };

export class ApiError extends Error {
  readonly estado: number;
  readonly codigo: string;
  readonly errores: ErrorDeCampo[];

  constructor(estado: number, codigo: string, mensaje: string, errores: ErrorDeCampo[] = []) {
    super(mensaje);
    this.name = "ApiError";
    this.estado = estado;
    this.codigo = codigo;
    this.errores = errores;
  }
}

type Parametros = Record<string, string | number | boolean | null | undefined>;

type Opciones = {
  metodo?: "GET" | "POST" | "PUT";
  cuerpo?: unknown;
  params?: Parametros;
};

function aQuery(params: Parametros | undefined): string {
  const query = new URLSearchParams();
  for (const [clave, valor] of Object.entries(params ?? {})) {
    if (valor !== null && valor !== undefined && valor !== "") {
      query.set(clave, String(valor));
    }
  }
  const texto = query.toString();
  return texto === "" ? "" : `?${texto}`;
}

export async function apiFetch<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  const { metodo = "GET", cuerpo, params } = opciones;

  const cabeceras: Record<string, string> = { Accept: "application/json" };
  const usuarioId = getUsuarioId();
  if (usuarioId !== null) {
    cabeceras["X-Usuario-Id"] = String(usuarioId);
  }

  const peticion: RequestInit = { method: metodo, headers: cabeceras };
  if (cuerpo !== undefined) {
    cabeceras["Content-Type"] = "application/json";
    peticion.body = JSON.stringify(cuerpo);
  }

  let respuesta: Response;
  try {
    respuesta = await fetch(`${BASE}${ruta}${aQuery(params)}`, peticion);
  } catch {
    throw new ApiError(0, "ERROR_DE_RED", SIN_CONEXION);
  }

  if (respuesta.ok) {
    return (await respuesta.json()) as T;
  }
  throw await aApiError(respuesta);
}

type Problema = { codigo?: unknown; detail?: unknown; errores?: ErrorDeCampo[] };

// La API devuelve los errores como ProblemDetail con "codigo" (y "errores" si es de validación).
// Si la respuesta no es eso (p. ej. un 502 del proxy con una página HTML) se trata como error de red.
async function aApiError(respuesta: Response): Promise<ApiError> {
  try {
    const problema = (await respuesta.json()) as Problema;
    if (typeof problema.codigo === "string") {
      const mensaje =
        typeof problema.detail === "string" ? problema.detail : "La petición ha fallado.";
      return new ApiError(respuesta.status, problema.codigo, mensaje, problema.errores ?? []);
    }
  } catch {
    // El cuerpo no era JSON.
  }
  return new ApiError(respuesta.status, "ERROR_DE_RED", SIN_CONEXION);
}
