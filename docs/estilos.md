# Estilos del frontend

Sistema de diseño definido en [src/styles.css](../src/styles.css). Tailwind CSS 4 con variables CSS y componentes shadcn/ui (estilo `new-york`, base `slate`, iconos `lucide`).

## Principios

- Todos los colores usan formato **oklch**.
- Los colores se consumen siempre mediante tokens semánticos (`bg-primary`, `text-muted-foreground`…), nunca con valores fijos en los componentes.
- Soporte de modo oscuro mediante la clase `.dark` (variante `dark` de Tailwind).
- Para añadir un color semántico: declarar la variable en `:root` y en `.dark`, y registrarla en `@theme inline` como `--color-<nombre>: var(--<nombre>)`.

## Paleta (modo claro)

| Token                  | Valor                      | Uso                                    |
| ---------------------- | -------------------------- | -------------------------------------- |
| `background`           | `oklch(0.985 0.003 230)`   | Fondo de página                        |
| `foreground`           | `oklch(0.27 0.012 250)`    | Texto principal                        |
| `card` / `popover`     | `oklch(1 0 0)`             | Superficies elevadas                   |
| `primary`              | `oklch(0.33 0.07 235)`     | Azul petróleo oscuro: acciones, marca  |
| `primary-foreground`   | `oklch(0.98 0.005 230)`    | Texto sobre primario                   |
| `secondary` / `muted`  | `oklch(0.955 0.008 230)`   | Fondos suaves                          |
| `muted-foreground`     | `oklch(0.47 0.015 250)`    | Texto secundario                       |
| `accent`               | `oklch(0.72 0.12 180)`     | Turquesa: acentos y destacados         |
| `accent-strong`        | `oklch(0.5 0.1 180)`       | Turquesa oscuro                        |
| `warning`              | `oklch(0.78 0.14 75)`      | Avisos                                 |
| `destructive`          | definido solo en modo oscuro | Acciones destructivas                |
| `border` / `input`     | `oklch(0.9 0.01 230)`      | Bordes y campos                        |
| `ring`                 | `oklch(0.5 0.1 180)`       | Anillo de foco                         |
| `chart-1` … `chart-5`  | ver `styles.css`           | Series de gráficos                     |
| `sidebar-*`            | ver `styles.css`           | Barra lateral (shadcn)                 |

El modo oscuro redefine todos los tokens sobre una base azul-pizarra (`oklch(0.129 0.042 264.695)` de fondo) con bordes translúcidos (`oklch(1 0 0 / 10%)`).

> Pendiente: `--destructive` no está definido en `:root`, solo en `.dark`. Conviene añadirlo en modo claro antes de usarlo.

## Tipografía

Se cargan desde Google Fonts en [src/routes/__root.tsx](../src/routes/__root.tsx).

| Familia         | Utilidad       | Pesos         | Uso                       |
| --------------- | -------------- | ------------- | ------------------------- |
| IBM Plex Sans   | `font-sans`    | 400, 500, 600 | Texto general             |
| Space Grotesk   | `font-display` | 500, 600, 700 | Títulos                   |
| IBM Plex Mono   | `font-mono`    | 400, 500      | Datos, códigos, etiquetas |

## Radios y sombras

- `--radius` base: `0.5rem`. Escala: `rounded-sm` (−4px), `md` (−2px), `lg` (base), `xl` (+4px) hasta `4xl` (+16px).
- Sombra `shadow-panel`: sombra amplia teñida con el color primario, para paneles destacados.

## Utilidades propias

- `bg-grid`: fondo con cuadrícula de 48 px usando el color `border`.
- `scroll-behavior: smooth` global en `html`.

## Convenciones de uso

- Contenedor de página: `mx-auto max-w-6xl px-5`.
- Foco visible en elementos interactivos: `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring`.
- Cabecera fija: `sticky top-0 z-40 border-b border-border bg-background/95 backdrop-blur`.
- Navegación de escritorio visible desde `md`; menú móvil por debajo.
- Combinar clases con `cn()` de [src/lib/utils.ts](../src/lib/utils.ts).
