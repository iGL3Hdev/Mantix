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
| 1. MVP             | 1. Base del frontend         | ✅     | 2026-10-18 |
| 1. MVP             | 2. Especificación del MVP    | ✅     | 2026-10-18 |
| 1. MVP             | 3. Backend del MVP           | 🔄     | —      |
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
- [x] Hacer commit y push a `main`.

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

**Entregable:** repositorio limpio en `main` (commit `40276b1`).
**Se cierra cuando:** lint, tests y build pasan y `decisiones.md` existe.

---

### Paso 2. Especificación del MVP

**Objetivo:** cerrar las seis preguntas de la guía antes de escribir backend.

Se redacta entre los dos, un apartado por sesión, en este orden:

- [x] **Modelo de datos:** entidades, atributos, relaciones y restricciones (punto de partida: Equipo, Ubicación, Incidencia, Orden de trabajo, Usuario, Historial).
- [x] **Estados y transiciones:** diagrama o tabla de estados de incidencia y de orden, con las transiciones permitidas y quién puede hacer cada una.
- [x] **Reglas de negocio:** qué está prohibido y qué es obligatorio, cada regla con un identificador (`RN-01`, `RN-02`…) para enlazarla desde los tests.
- [x] **Trazabilidad:** qué datos guarda cada cambio (quién, cuándo, estado anterior y nuevo, comentario).
- [x] **API:** endpoints del MVP con método, ruta, cuerpo, respuesta y códigos de error.
- [x] **Criterios de aceptación:** lista verificable, una línea por comportamiento (`CA-01`…).
- [x] Revisión final y cierre de la especificación.

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
**Carpeta:** `backend/` dentro del repositorio `Mantix` (un solo repositorio, ver `decisiones.md`, 2026-10-19).

> Los nombres de entidades y endpoints de esta lista son provisionales: se confirman con `docs/backend.md`.

Tareas:

- [x] Crear el proyecto con Spring Initializr en `backend/`: Java 21, Maven, dependencias Web, Data JPA, Validation, MySQL Driver, Flyway y Testcontainers (tests con MySQL real, sin H2).
- [ ] README de `backend/` con arranque y enlace a `docs/backend.md`.
- [x] Primera migración con el esquema (`V1`) y datos iniciales (`V2`), comprobadas con `EsquemaInicialTest`.
- [ ] Configuración de la conexión a MySQL en `application.properties` para ejecutar la aplicación fuera de los tests (ya hechos: `ddl-auto=validate`, `open-in-view=false` y zona horaria UTC).
- [x] Entidades y enums de estado: los 7 enums y las 6 entidades.
- [x] Repositorios: uno por entidad, cada uno con su test (`HistorialRepository` incluye la consulta del historial de una entidad en orden).
- [ ] Servicios con las reglas de transición y el registro en el historial. Hechos: `HistorialService`, `AutorizacionService`, `EquipoService` (alta, edición, baja y reactivación) e `IncidenciaService` (apertura, cambio de prioridad, descarte y reapertura, con RN-10 y RN-11 mediante `EquipoService.reevaluarAveria`). Falta `OrdenTrabajoService`, que también cerrará incidencias (RN-18) y recalculará el equipo.
- [ ] **Tests de servicio de cada transición (permitidas y prohibidas) antes de los controladores.** Método de trabajo: primero el test y un esqueleto que falla, después la implementación. Cubiertas hasta ahora: RN-01 (parcial), RN-02, RN-04, RN-05, RN-09, RN-12, RN-13 y RN-14.
- [ ] DTOs, controladores y manejo global de errores.
- [ ] Tests de integración de la API.

Archivos (dentro de `backend/`, paquete base `com.mantix`; las rutas de `src/` son relativas a esa carpeta):

| Ubicación                          | Archivos                                                                          | Propósito                                          |
| ---------------------------------- | --------------------------------------------------------------------------------- | -------------------------------------------------- |
| `backend/`                         | `pom.xml`, `README.md`, `.gitignore`                                              | Proyecto Maven y documentación                     |
| `backend/src/main/resources/`      | `application.properties`                                                               | Conexión a MySQL, JPA y Flyway                     |
| `backend/src/main/resources/db/migration/` | `V1__esquema_inicial.sql`, `V2__datos_iniciales.sql`                      | Esquema y datos de partida (usuarios, ubicaciones) |
| `domain/`                          | `Equipo`, `Ubicacion`, `Incidencia`, `OrdenTrabajo`, `Usuario`, `Historial` | Entidades JPA                                      |
| `domain/`                          | `EstadoEquipo`, `EstadoIncidencia`, `EstadoOrden`, `Prioridad`, `Perfil`, `TipoEntidad`, `AccionHistorial` | Enums                                              |
| `repository/`                      | un `…Repository` por entidad                                                      | Acceso a datos                                     |
| `service/`                         | `EquipoService`, `IncidenciaService`, `OrdenTrabajoService`, `HistorialService`   | Reglas de negocio y transacciones                  |
| `service/`                         | `AutorizacionService`, `RelojConfig` (en `config/`)                               | Usuario que actúa (RN-04, RN-05) y reloj inyectable |
| `service/`                         | `DatosEquipo`, `CambiosEquipo` (y equivalentes para incidencias y órdenes)        | Datos de entrada de los servicios, independientes de la capa web |
| `service/`                         | `ErrorDeDominio` (madre) y `ReglaNegocioException` (409), `PermisoDenegadoException` (403), `RecursoNoEncontradoException` (404), `UsuarioNoIdentificadoException` (401) | Errores de dominio con `codigo` |
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
**Carpeta:** raíz del repositorio `Mantix`.

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

- [x] Decidir dónde va `docker-compose.yml`: en la raíz del repositorio (decidido con el repositorio único).
- [ ] `Dockerfile` del backend (build Maven en varias etapas).
- [ ] `Dockerfile` del frontend.
- [ ] `docker-compose.yml` con MySQL (volumen y healthcheck), backend y frontend.
- [ ] `.env.example` con las variables necesarias.
- [ ] Repasar cada criterio `CA-xx` de `docs/backend.md` y marcarlo.
- [ ] Etiquetar `v1.0-mvp`.

Archivos:

| Acción    | Archivo                              | Propósito                          |
| --------- | ------------------------------------ | ---------------------------------- |
| Crear     | `backend/Dockerfile`                 | Imagen del backend                 |
| Crear     | `Dockerfile`, `.dockerignore`        | Imagen del frontend                |
| Crear     | `docker-compose.yml`                 | Orquestación de los tres servicios |
| Crear     | `.env.example`                       | Variables de entorno de ejemplo    |
| Modificar | `README.md` y `backend/README.md`    | Arranque con Docker                |

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
| 2026-10-18 | 1    | Proyecto renombrado a `mantix`, token `destructive`, `decisiones.md`, docs y README al día, Prettier con `endOfLine: auto`; lint (0 errores), tests y build pasan | `40276b1` |
| 2026-10-18 | 2    | Creada `docs/backend.md` con el modelo de datos: 6 entidades, órdenes 1:N por incidencia, estado del equipo e historial polimórfico | — |
| 2026-10-18 | 2    | Estados y transiciones de orden (validación del responsable) e incidencia (cambios automáticos según su orden) | — |
| 2026-10-18 | 2    | Reglas de negocio RN-01 a RN-24: perfiles, estado automático del equipo por incidencias críticas, baja reversible, cambio de prioridad | — |
| 2026-10-18 | 2    | Trazabilidad: tabla `historial` genérica (acción, valor anterior y nuevo), fila principal y consecuencias con `origen_id`, fechas en UTC | — |
| 2026-10-18 | 2    | API: 28 endpoints (un `POST` por acción), cabecera `X-Usuario-Id`, errores `ProblemDetail` con `codigo` RN-xx | — |
| 2026-10-18 | 2    | Criterios de aceptación CA-01 a CA-37 (cada `RN-xx` cubierto); repaso de coherencia hecho, falta la revisión de Carlos | — |
| 2026-10-18 | 2    | Carlos revisa y aprueba `docs/backend.md`. Paso cerrado | commit del día |
| 2026-10-19 | 3    | Decisión: repositorio único, backend en `backend/`. Creado el proyecto Spring Boot con Initializr (Java 21, Maven 3.10) | — |
| 2026-10-19 | 3    | Paquete `com.mantix`, MySQL 8.4 en Testcontainers, migraciones `V1` (esquema) y `V2` (usuarios y ubicaciones); 4 tests en verde | commit del día |
| 2026-10-20 | 3    | Enums de dominio, entidades `Usuario`, `Ubicacion`, `Equipo` e `Incidencia` con sus repositorios; `ddl-auto=validate` y UTC; 9 tests en verde | `Backend: entidades Ubicacion y Equipo…` y siguiente |
| 2026-10-22 | 3    | Entidades `OrdenTrabajo` e `Historial` (inmutable) con sus repositorios; 13 tests en verde. Entidades y repositorios cerrados | — |
| 2026-10-22 | 3    | `HistorialService` (obligatorio dentro de una transacción, RN-02), reloj inyectable, errores de dominio y `AutorizacionService` (RN-04, RN-05); 21 tests en verde | — |
| 2026-10-23 | 3    | `EquipoService` hecho con tests primero: alta (RN-05, RN-09), edición (RN-14), baja (RN-12) y reactivación (RN-13), con historial; 36 tests en verde | commit del día |
| 2026-10-27 | 3    | `IncidenciaService.crear` con tests primero: apertura (CA-10), usuario inactivo (RN-04), equipo de baja (RN-14) y equipo averiado por crítica (RN-10, CA-11, con `origen_id` al alta); `reevaluarAveria` compartido en `EquipoService`; 44 tests en verde | commit del día |
| 2026-10-27 | 3    | `IncidenciaService` completo, con tests primero: cambio de prioridad (RN-15, RN-05, CA-13, CA-14), descarte (RN-16, RN-11, CA-15) y reapertura (RN-17, RN-10, CA-16); 67 tests en verde. Decisión: reabrir una incidencia de un equipo de baja se rechaza con RN-14 | commit del día |
| 2026-10-18 | 1    | Se quita la etiqueta `v0.1-landing` de los criterios de cierre. Paso cerrado | `40276b1` |
