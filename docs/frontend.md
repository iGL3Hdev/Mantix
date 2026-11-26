# Especificaciones del frontend

## Stack

| Área           | Tecnología                                          |
| -------------- | --------------------------------------------------- |
| Framework      | React 19 + TanStack Start (SSR) + TanStack Router   |
| Build          | Vite 8, servidor con Nitro                          |
| Estilos        | Tailwind CSS 4, shadcn/ui (Radix), `tw-animate-css` |
| Datos          | TanStack Query                                      |
| Formularios    | React Hook Form + Zod                               |
| Iconos         | lucide-react                                        |
| Notificaciones | sonner                                              |
| Gráficos       | recharts                                            |
| Tests          | Vitest + Testing Library + jsdom                    |
| Calidad        | ESLint, Prettier, TypeScript estricto               |

Requisitos de entorno y comandos: ver el [README del frontend](../frontend/README.md). Los estilos están en [estilos.md](estilos.md).

## Estado actual

Landing de una sola página (`/`) con las secciones Inicio, Funcionalidades y Contacto. La acción "Acceder" muestra por ahora un aviso ("El acceso a Mantix estará disponible próximamente"). No hay backend conectado; el backend será en Java (Spring Boot).

## Estructura

El frontend vive en `frontend/` (como el backend en `backend/`). Las rutas de este documento son relativas a esa carpeta.

```
src/
  components/   UI reutilizable (Header, Footer, Logo, ActionButton, InfoCard)
    ui/         componentes shadcn/ui
  data/         contenido y textos (content.ts)
  sections/     secciones de la landing (Hero, Features, Value, Closing, DashboardMock)
  routes/       rutas por archivos (__root.tsx, index.tsx)
  modules/      módulos de dominio futuros: src/modules/<nombre>/
  hooks/        hooks compartidos (use-mobile)
  lib/          utilidades (cn, páginas de error)
  test/         configuración y tests
  router.tsx    creación del router y QueryClient
  start.ts      middlewares (errores 500, CSRF en server functions)
  server.ts     entrada del servidor SSR
```

Reglas del proyecto ([AGENTS.md](../AGENTS.md)): el contenido de la landing vive en `src/data`, las secciones en `src/sections` y la UI reutilizable en `src/components`. Esto mantiene el contenido separado de la presentación.

## Enrutado

- Rutas basadas en archivos en `src/routes`; `routeTree.gen.ts` se genera automáticamente y no se edita a mano.
- `__root.tsx` define el documento HTML (`lang="es"`), metadatos, fuentes, el `QueryClientProvider`, y las pantallas de 404 y de error.
- Cada ruta declara su `head` (título, descripción, Open Graph).
- Alias de imports: `@/` apunta a `src/`.

## Contenido e idioma

- Idioma de la interfaz: español.
- Los textos se definen en `src/data/content.ts` (navegación, hero, funcionalidades, propuesta de valor, cierre) y se tipan con `NavItem` e `InfoItem`.

## Accesibilidad

- Enlaces y botones con foco visible por anillo.
- `aria-label` en logo y navegación principal.
- Menú móvil conmutable desde la cabecera por debajo de `md`.

## Manejo de errores

- Middleware de servidor que devuelve una página 500 propia si falla una petición.
- Protección CSRF en server functions.
- Error boundary y 404 en la ruta raíz.

## Pruebas

`npm test` ejecuta Vitest (entorno jsdom, setup en `src/test/setup.ts`). Hay una prueba de enrutado en `src/test/app-routing.test.tsx`.

## Integración con el backend (pendiente)

El backend es Java con Spring Boot, en la carpeta `backend/` del mismo repositorio; su API está especificada en [backend.md](backend.md). Decisión del 2026-11-25 ([decisiones.md](decisiones.md)): la landing se mantiene con SSR y las rutas de la aplicación (`/app/...`) se pintan solo en el navegador. Las llamadas se hacen con `fetch` desde `src/lib/api-client.ts` y TanStack Query, y `/api` se reenvía al backend con un proxy de Nitro (`routeRules` en `vite.config.ts`), lo que evita CORS.
