# Mantix

Gestión del mantenimiento industrial. Frontend con React, TanStack Start, Vite y Tailwind CSS.

## Requisitos

- Node.js `^22.22.2` o `^24.15.0` (recomendado: 24 LTS)
- npm 11 o superior (incluido con Node 24)

Si usas [nvm-windows](https://github.com/coreybutler/nvm-windows):

```powershell
nvm install 24
nvm use 24
```

## Arranque

```powershell
npm install
npm run dev
```

La aplicación queda disponible en http://localhost:3000.

## Scripts

| Comando              | Descripción                                |
| -------------------- | ------------------------------------------ |
| `npm run dev`        | Servidor de desarrollo                     |
| `npm run build`      | Build de producción                        |
| `npm run build:dev`  | Build en modo desarrollo                   |
| `npm run preview`    | Previsualiza el build                      |
| `npm run lint`       | Análisis con ESLint                        |
| `npm run format`     | Formatea el código con Prettier            |
| `npm test`           | Ejecuta los tests (Vitest)                 |
| `npm run test:watch` | Tests en modo observación                  |

## Estructura

- `src/data`: contenido y textos de la landing
- `src/sections`: secciones de la página
- `src/components`: componentes de UI reutilizables
- `src/routes`: rutas (TanStack Router)
- `src/modules/<nombre>/`: módulos de funcionalidad futuros

## Solución de problemas

- **`npm install` falla con `edgesOut`:** npm demasiado antiguo. Actualiza Node y npm (`npm install -g npm@latest`).
- **`node -v` muestra una versión antigua tras instalar otra:** hay varias instalaciones en el PATH. Comprueba con `Get-Command node -All` y usa `nvm use`.
- **PowerShell 5.1 no admite `&&`:** ejecuta los comandos por separado o encadénalos con `;`.
