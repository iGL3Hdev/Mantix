# Mantix — Plan de trabajo

Oct 18, 2026 · @Carlos

Este documento es la hoja de ruta de [Guía.md](Guía.md) convertida en pasos que se pueden ejecutar. Para cada paso indica qué archivos hay que crear o modificar, dónde y cuándo se da por cerrado. El código lo escribe Carlos; las indicaciones de cada archivo se dan al empezar el paso.

## Cómo se usa

- Las tareas se marcan `[x]` al terminarlas.
- Un paso se cierra cuando cumple **todos** sus criterios de cierre. En ese momento se actualiza la tabla de estado y se añade una línea al registro de avance.
- El trabajo se hace directamente en `main`, con commit y push al final de cada día o al cerrar un paso.
- Si una decisión cambia, se anota en [decisiones.md](decisiones.md) con fecha y motivo, y se ajusta este plan.
- Leyenda: ⏳ pendiente · 🔄 en curso · ✅ cerrado.

## Estado general

| Fase               | Paso                         | Estado | Cierre |
| ------------------ | ---------------------------- | ------ | ------ |
| 1. MVP             | 1. Base del frontend         | 🔄     | —      |
| 1. MVP             | 2. Especificación del MVP    | ⏳     | —      |
| 1. MVP             | 3. Backend del MVP           | ⏳     | —      |
| 1. MVP             | 4. Frontend de la aplicación | ⏳     | —      |
| 1. MVP             | 5. Docker y revisión de fase | ⏳     | —      |
| 2. Solidez técnica | —                            | ⏳     | —      |
| 3. Ampliaciones    | —                            | ⏳     | —      |

---

## Fase 1 · MVP

### Paso 1. Base del frontend

**Objetivo:** dejar el repositorio `Mantix` limpio, documentado y etiquetado como punto de partida.

Tareas:

- [x] Renombrar el proyecto en `package.json`: `"name": "mantix"`.
- [x] Ejecutar `npm install` para que `package-lock.json` recoja el nombre nuevo.
- [x] Definir `--destructive` (y `--destructive-foreground`) en `:root` de [src/styles.css](../src/styles.css).
- [x] Quitar el aviso «Pendiente» de [estilos.md](estilos.md) y añadir el token a la paleta.
- [x] Crear `docs/decisiones.md` con las decisiones cerradas hasta hoy (ver abajo).
- [x] Actualizar «Integración con el backend» en [frontend.md](frontend.md): la estructura del repositorio y el framework (Spring Boot) ya están decididos; solo queda abierta la elección entre SSR y SPA.
- [x] Enlazar `docs/` desde el [README](../README.md): guía, plan de trabajo, frontend, estilos y backend.
- [x] Configurar Prettier con `"endOfLine": "auto"` (Git en Windows convierte a CRLF) y ejecutar `npm run format`.
- [x] Comprobar que `npm run lint`, `npm test` y `npm run build` terminan sin errores.
- [x] Hacer commit y push a `main` y crear la etiqueta `v0.1-landing` (`git tag v0.1-landing` + `git push origin v0.1-landing`).

Archivos:

| Acción    | Archivo                                            | Propósito                                 |
| --------- | -------------------------------------------------- | ----------------------------------------- |
| Modificar | `package.json`                                     | Nombre del proyecto                       |
| Modificar | `src/styles.css`                                   | Token `destructive` en modo claro         |
| Modificar | `.prettierrc`                                      | `endOfLine: auto` por los saltos de línea de Windows |
| Crear     | `docs/decisiones.md`                               | Registro de decisiones con fecha y motivo |
| Modificar | `docs/estilos.md`, `docs/frontend.md`, `README.md` | Documentación al día                      |

Contenido inicial de `docs/decisiones.md` (una entrada por decisión: fecha, decisión y motivo):

- Tailwind CSS en toda la aplicación.
- Base de datos MySQL.
- Dos repositorios: `Mantix` (frontend, mantiene su nombre) y `mantix-backend`.
- Perfiles técnico y responsable desde el MVP.
- La documentación, incluida la especificación del backend (`docs/backend.md`), vive en `docs/` de `Mantix`.

**Entregable:** repositorio limpio y etiqueta `v0.1-landing`.
**Se cierra cuando:** lint, tests y build pasan; la etiqueta existe en GitHub; `decisiones.md` existe.

---

### Paso 2. Especificación del MVP

**Objetivo:** cerrar las seis preguntas de la guía antes de escribir backend.

Se redacta entre los dos, un apartado por sesión, en este orden:

- [ ] **Modelo de datos:** entidades, atributos, relaciones y restricciones (punto de partida: Equipo, Ubicación, Incidencia, Orden de trabajo, Usuario, Historial).
- [ ] **Estados y transiciones:** diagrama o tabla de estados de incidencia y de orden, con las transiciones permitidas y quién puede hacer cada una.
- [ ] **Reglas de negocio:** qué está prohibido y qué es obligatorio, cada regla con un identificador (`RN-01`, `RN-02`…) para enlazarla desde los tests.
- [ ] **Trazabilidad:** qué datos guarda cada cambio (quién, cuándo, estado anterior y nuevo, comentario).
- [ ] **API:** endpoints del MVP con método, ruta, cuerpo, respuesta y códigos de error.
- [ ] **Criterios de aceptación:** lista verificable, una línea por comportamiento (`CA-01`…).
- [ ] Revisión final y cierre de la especificación.

Archivos:

| Acción    | Archivo              | Propósito                                   |
| --------- | -------------------- | ------------------------------------------- |
| Crear     | `docs/backend.md`    | Especificación del MVP y contrato de la API |
| Modificar | `docs/decisiones.md` | Decisiones que salgan de la especificación  |

**Entregable:** `docs/backend.md` revisada y cerrada.
**Se cierra cuando:** los seis apartados están completos y cada regla y criterio tiene identificador.

---

### Paso 3. Backend del MVP

**Objetivo:** API del MVP en Spring Boot con las reglas de estado cubiertas por tests.
**Repositorio:** `mantix-backend` (nuevo).

> Los nombres de entidades y endpoints de esta lista son provisionales: se confirman con `docs/backend.md`.

Tareas:

- [ ] Crear el proyecto con Spring Initializr: Java 21, Maven, dependencias Web, Data JPA, Validation, MySQL Driver y Flyway (más Testcontainers o H2 para tests, se decide en el paso).
- [ ] Crear el repositorio `mantix-backend` en GitHub. README con arranque y enlace a `docs/backend.md` de `Mantix`.
- [ ] Configuración y primera migración con el esquema.
- [ ] Entidades y enums de estado.
- [ ] Repositorios.
- [ ] Servicios con las reglas de transición y el registro en el historial.
- [ ] **Tests de servicio de cada transición (permitidas y prohibidas) antes de los controladores.**
- [ ] DTOs, controladores y manejo global de errores.
- [ ] Tests de integración de la API.

Archivos (paquete base `com.mantix`):

| Ubicación                          | Archivos                                                                          | Propósito                                          |
| ---------------------------------- | --------------------------------------------------------------------------------- | -------------------------------------------------- |
| raíz                               | `pom.xml`, `README.md`, `.gitignore`                                              | Proyecto Maven y documentación                     |
| `src/main/resources/`              | `application.yml`                                                                 | Conexión a MySQL, JPA y Flyway                     |
| `src/main/resources/db/migration/` | `V1__esquema_inicial.sql`, `V2__datos_iniciales.sql`                              | Esquema y datos de partida (usuarios, ubicaciones) |
| `domain/`                          | `Equipo`, `Ubicacion`, `Incidencia`, `OrdenTrabajo`, `Usuario`, `HistorialEstado` | Entidades JPA                                      |
| `domain/`                          | `EstadoIncidencia`, `EstadoOrden`, `Perfil`                                       | Enums                                              |
| `repository/`                      | un `…Repository` por entidad                                                      | Acceso a datos                                     |
| `service/`                         | `EquipoService`, `IncidenciaService`, `OrdenTrabajoService`, `HistorialService`   | Reglas de negocio y transacciones                  |
| `service/`                         | `TransicionNoPermitidaException`, `RecursoNoEncontradoException`                  | Errores de dominio                                 |
| `web/`                             | un `…Controller` por recurso                                                      | Endpoints REST                                     |
| `web/dto/`                         | `…Request` / `…Response`                                                          | Contrato de entrada y salida                       |
| `web/`                             | `GlobalExceptionHandler`                                                          | Errores como respuestas JSON coherentes            |
| `src/test/java/…/service/`         | `…ServiceTest`                                                                    | Tests de transiciones y reglas (`RN-xx`)           |
| `src/test/java/…/web/`             | `…ControllerIT`                                                                   | Tests de integración de la API                     |

**Entregable:** API del MVP con tests pasando.
**Se cierra cuando:** `mvn verify` pasa y cada regla `RN-xx` tiene al menos un test.

---

### Paso 4. Frontend de la aplicación

**Objetivo:** pantallas de equipos, incidencias y órdenes. Primero con datos de prueba y después conectadas a la API.
**Repositorio:** `Mantix`.

Puede empezar en paralelo al Paso 3, en cuanto `docs/backend.md` esté cerrada.

Tareas:

- [ ] Decidir si se mantiene SSR o se pasa a SPA y anotarlo en `decisiones.md`.
- [ ] Layout de la aplicación con barra lateral y cabecera.
- [ ] Botón «Acceder» de la landing: que lleve a la aplicación en lugar de mostrar el aviso.
- [ ] Módulo de equipos: listado con filtros, detalle y alta/edición.
- [ ] Módulo de incidencias: listado, alta y cambio de estado.
- [ ] Módulo de órdenes: listado, detalle con historial y cambio de estado.
- [ ] Selector de usuario/perfil simulado (sin login real hasta la fase 2).
- [ ] Sustituir los datos de prueba por llamadas a la API, con proxy de Vite.
- [ ] Tests de los componentes con lógica (formularios y cambio de estado).

Archivos:

| Acción    | Archivo                                            | Propósito                                  |
| --------- | -------------------------------------------------- | ------------------------------------------ |
| Crear     | `src/modules/<modulo>/types.ts`                    | Tipos según `docs/backend.md`              |
| Crear     | `src/modules/<modulo>/mock-data.ts`                | Datos de prueba tipados                    |
| Crear     | `src/modules/<modulo>/api.ts`                      | Llamadas a la API del módulo               |
| Crear     | `src/modules/<modulo>/hooks.ts`                    | Hooks de TanStack Query                    |
| Crear     | `src/modules/<modulo>/components/`                 | Tabla, formulario y detalle del módulo     |
| Crear     | `src/lib/api-client.ts`                            | `fetch` común: URL base, JSON y errores    |
| Crear     | `src/components/AppLayout.tsx`                     | Layout con `src/components/ui/sidebar.tsx` |
| Crear     | `src/components/EstadoBadge.tsx`                   | Etiqueta de estado reutilizable            |
| Crear     | `src/routes/app.tsx`                               | Ruta padre con el layout                   |
| Crear     | `src/routes/app/…`                                 | Rutas de equipos, incidencias y órdenes    |
| Modificar | `src/data/content.ts`, `src/components/Header.tsx` | Acción «Acceder»                           |
| Modificar | `vite.config.ts`                                   | Proxy `/api` hacia el backend              |
| Modificar | `docs/frontend.md`                                 | Estructura y rutas nuevas                  |

`<modulo>` = `equipos`, `incidencias`, `ordenes`.

**Entregable:** flujo completo usable desde el navegador.
**Se cierra cuando:** se puede dar de alta un equipo, abrir una incidencia, generar una orden y llevarla hasta cerrada, y el historial lo muestra.

---

### Paso 5. Docker y revisión de fase

**Objetivo:** levantar todo con un comando y comprobar el MVP contra sus criterios.

Tareas:

- [ ] Decidir dónde va `docker-compose.yml` y anotarlo en `decisiones.md`.
- [ ] `Dockerfile` del backend (build Maven en varias etapas).
- [ ] `Dockerfile` del frontend.
- [ ] `docker-compose.yml` con MySQL (volumen y healthcheck), backend y frontend.
- [ ] `.env.example` con las variables necesarias.
- [ ] Repasar cada criterio `CA-xx` de `docs/backend.md` y marcarlo.
- [ ] Etiquetar `v1.0-mvp` en los dos repositorios.

Archivos:

| Acción    | Archivo                              | Propósito                          |
| --------- | ------------------------------------ | ---------------------------------- |
| Crear     | `mantix-backend/Dockerfile`          | Imagen del backend                 |
| Crear     | `Mantix/Dockerfile`, `.dockerignore` | Imagen del frontend                |
| Crear     | `docker-compose.yml`                 | Orquestación de los tres servicios |
| Crear     | `.env.example`                       | Variables de entorno de ejemplo    |
| Modificar | `README.md` de ambos                 | Arranque con Docker                |

**Entregable:** `docker compose up` funcional y checklist del MVP cumplida.
**Se cierra cuando:** desde un clon limpio, `docker compose up` deja la aplicación usable y todos los `CA-xx` están marcados.

---

## Fase 2 · Solidez técnica

Se detalla al cerrar la fase 1.

- [ ] Spring Security: autenticación y permisos por perfil (técnico, responsable).
- [ ] Login real en el frontend y rutas protegidas.
- [ ] Paginación y ordenación en listados (API y tablas).
- [ ] Validación y respuestas de error homogéneas ante datos erróneos.
- [ ] Indicadores: tiempos de resolución y equipos con más incidencias (se especifican en `docs/backend.md`).
- [ ] Ampliar la cobertura de tests.

**Se cierra cuando:** la API es robusta ante datos erróneos y hay control de acceso.

## Fase 3 · Ampliaciones opcionales

Cada una se elige por separado y solo si aporta algo nuevo que aprender.

- [ ] Mantenimiento preventivo.
- [ ] Repuestos.
- [ ] Avisos.
- [ ] Documentación adjunta.

---

## Registro de avance

| Fecha      | Paso | Qué se hizo                                                                                           | Commit / etiqueta |
| ---------- | ---- | ----------------------------------------------------------------------------------------------------- | ----------------- |
| 2026-10-18 | —    | Plan de trabajo creado y guía actualizada (repositorio `Mantix`, especificación en `docs/backend.md`) | —                 |
| 2026-10-18 | 1    | Proyecto renombrado a `mantix`, token `destructive`, `decisiones.md`, docs y README al día, Prettier con `endOfLine: auto`; lint (0 errores), tests y build pasan | pendiente de commit |
