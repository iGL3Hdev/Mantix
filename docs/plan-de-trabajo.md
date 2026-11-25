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
| 1. MVP             | 3. Backend del MVP           | ✅     | 2026-11-22 |
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
- [x] Definir `--destructive` (y `--destructive-foreground`) en `:root` de [frontend/src/styles.css](../frontend/src/styles.css).
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
3

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
- [x] README de `backend/` con el arranque (MySQL en Docker, variables de entorno, tests), la estructura de paquetes y el enlace a `docs/backend.md`.
- [x] Primera migración con el esquema (`V1`) y datos iniciales (`V2`), comprobadas con `EsquemaInicialTest`.
- [x] Configuración de la conexión a MySQL en `application.properties` para ejecutar la aplicación fuera de los tests: `DB_URL`, `DB_USER` y `DB_PASSWORD` con valores por defecto para desarrollo local (junto a `ddl-auto=validate`, `open-in-view=false` y zona horaria UTC). Comprobada contra un MySQL 8.4 en Docker en el puerto 3309.
- [x] Entidades y enums de estado: los 7 enums y las 6 entidades.
- [x] Repositorios: uno por entidad, cada uno con su test (`HistorialRepository` incluye la consulta del historial de una entidad en orden).
- [x] Servicios con las reglas de transición y el registro en el historial: `HistorialService`, `AutorizacionService`, `EquipoService` (alta, edición, baja, reactivación y `reevaluarAveria` para RN-10 y RN-11), `IncidenciaService` (apertura, cambio de prioridad, descarte y reapertura) y `OrdenTrabajoService` (crear, asignar, reasignar, empezar, resolver, validar, rechazar y cancelar). Los motivos y la resolución obligatorios (RN-21, RN-22 y los motivos de baja, reactivación, descarte y reapertura) se validan en los controladores con Bean Validation.
- [x] **Tests de servicio de cada transición (permitidas y prohibidas) antes de los controladores.** Método de trabajo: primero el test y un esqueleto que falla, después la implementación. 129 tests en verde. Cubiertas todas las reglas de los servicios: RN-01, RN-02, RN-04 a RN-07, RN-09 a RN-20, RN-23 y RN-24. Pendiente para los tests de integración: CA-32 (una acción rechazada no deja cambios, que necesita ejecutarse sin transacción de test).
- [x] DTOs, controladores y manejo global de errores. Hecho: `GlobalExceptionHandler` (errores de dominio a 409, 403, 404 y 401; error inesperado a 500 sin detalle; los errores propios de Spring reciben también `codigo`), con `GlobalExceptionHandlerTest`. `UsuarioController` (solo lectura, con filtros `perfil` y `activo`) y `UsuarioResponse`, con `UsuarioControllerTest`. `UbicacionController` (`GET /api/ubicaciones`) y `UbicacionResponse`, con `UbicacionControllerTest`. `EquipoController` en lectura (`GET /api/equipos` con filtros `estado`, `ubicacionId` y `q`, y `GET /api/equipos/{id}`) y `EquipoResponse`, con `EquipoControllerTest`. `POST /api/equipos` (crear, con `X-Usuario-Id`, `201` y `Location`) y `EquipoRequest`, con `EquipoControllerCrearTest`. Validación del cuerpo con Bean Validation (`@Valid`, `EquipoRequest` anotado) y `400` con `codigo` `VALIDACION` y lista `errores` (`campo`, `mensaje`) en `GlobalExceptionHandler`. `PUT /api/equipos/{id}` (editar, con `EquipoEdicionRequest` sin código) con `EquipoControllerEditarTest`. `POST /api/equipos/{id}/baja` con `ComentarioRequest` (comentario obligatorio, reutilizable) y `EquipoControllerBajaTest`, que comprueba también la entrada de historial. `POST /api/equipos/{id}/reactivar` con `EquipoControllerReactivarTest`, `GET /api/equipos/{id}/historial` con `EntradaHistorialResponse` y `EquipoControllerHistorialTest`: el controlador de equipos queda completo. `GET /api/incidencias` (filtros `estado`, `prioridad` y `equipoId`, más recientes primero, con `ordenActivaId` calculado en una sola consulta para todas las incidencias) con `IncidenciaControllerTest`, y los resúmenes compartidos `ResumenUsuario` y `ResumenEquipo`. `GET /api/incidencias/{id}` (detalle con `ordenes` en orden cronológico y `IncidenciaDetalleResponse` con `@JsonUnwrapped`). `POST /api/incidencias` (crear, `IncidenciaRequest`, `201` con `Location`) con `IncidenciaControllerCrearTest`, que comprueba desde fuera el efecto de RN-10 sobre el equipo. `POST /api/incidencias/{id}/prioridad` (`PrioridadRequest`, comentario opcional, `ordenActivaId` en la respuesta) con `IncidenciaControllerPrioridadTest`, que comprueba RN-10 y RN-11 desde fuera. `POST /api/incidencias/{id}/descartar` con `IncidenciaControllerDescartarTest` (RN-11 por los dos lados) y el límite de 500 caracteres del comentario en `ComentarioRequest`, que alinea el código con la tabla de límites de `docs/backend.md`. `POST /api/incidencias/{id}/reabrir` con `IncidenciaControllerReabrirTest` (RN-10 al reabrir, RN-17 y RN-14). `GET /api/incidencias/{id}/historial` con `IncidenciaControllerHistorialTest` (CA-34: las entradas de la incidencia y las de sus órdenes en una sola lista cronológica, con una consulta propia en `HistorialRepository`); el controlador de incidencias queda completo. `OrdenController` en lectura (`GET /api/ordenes` con filtros `estado`, `tecnicoId` e `incidenciaId`, y `GET /api/ordenes/{id}`) y `OrdenResponse`, con `OrdenControllerTest`; el equipo de la orden llega a través de su incidencia, con un `@EntityGraph` de ruta anidada (`incidencia.equipo`). `POST /api/ordenes` (`OrdenRequest`, `201` con `Location`, límite de 5000 caracteres en `descripcion`) con `OrdenControllerCrearTest`, que comprueba la cascada desde fuera: la incidencia pasa a `EN_CURSO` con su `ordenActivaId` y el historial queda encadenado. `POST /api/ordenes/{id}/asignar` y `/reasignar` (`TecnicoRequest`) con `OrdenControllerAsignacionTest` (asignar escribe el cambio de estado y el de técnico encadenado; reasignar solo el de técnico). `POST /api/ordenes/{id}/empezar` (sin cuerpo) y `/resolver` (`ResolucionRequest`, obligatoria y de hasta 5000 caracteres) con `OrdenControllerEjecucionTest`, con el permiso del técnico asignado (RN-06). `POST /api/ordenes/{id}/validar` (sin cuerpo) y `/rechazar` (`ComentarioRequest`) con `OrdenControllerCierreTest`, que comprueba la cascada de validar desde fuera (incidencia cerrada con su fecha, equipo repuesto, historial encadenado) y el ciclo resolver, rechazar y volver a resolver (CA-23). `POST /api/ordenes/{id}/cancelar` (`ComentarioRequest`) con `OrdenControllerCancelarTest`, que comprueba desde fuera la cascada de RN-18 (la incidencia vuelve a `ABIERTA` sin orden activa y admite una orden nueva). `GET /api/ordenes/{id}/historial` con `OrdenControllerHistorialTest` (solo las entradas de esa orden, sin mezclar las de otra entidad con el mismo id). Los 28 endpoints están hechos. Límites de la tabla de `docs/backend.md` aplicados en todos los DTO de entrada (también `descripcion` en `IncidenciaRequest` y `comentario` en `PrioridadRequest`). CA-32 (`AtomicidadTest`, con un fallo simulado a mitad de crear una orden) y CA-33 (`HistorialNoModificableTest`), y `CriteriosDeAceptacionTest` para las cláusulas de los criterios que ningún otro test comprobaba.
- [x] Tests de integración de la API: un `…ControllerTest` por controlador y por grupo de acciones, sobre MySQL real con Testcontainers y sin `@Transactional`, que comprueban el estado HTTP, el `codigo`, los efectos en cascada y lo que queda en la base de datos; más `AtomicidadTest` (CA-32), `HistorialNoModificableTest` (CA-33) y `CriteriosDeAceptacionTest`. 387 tests en el backend.

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
| `src/test/java/…/web/`             | `…ControllerTest`                                                                 | Tests de integración de la API (el sufijo `Test` es el que ejecuta `mvnw verify`) |

**Entregable:** API del MVP con tests pasando.
**Se cierra cuando:** `mvn verify` pasa y cada regla `RN-xx` tiene al menos un test.

---

### Paso 4. Frontend de la aplicación

**Objetivo:** pantallas de equipos, incidencias y órdenes, conectadas directamente a la API real (decisión del 2026-11-25: sin datos de prueba previos).
**Carpeta:** `frontend/` dentro del repositorio `Mantix` (se mueve desde la raíz el 2026-11-25; las rutas `src/...` de esta lista son relativas a `frontend/`).

Puede empezar en paralelo al Paso 3, en cuanto `docs/backend.md` esté cerrada.

Tareas:

- [x] Decidir si se mantiene SSR o se pasa a SPA y anotarlo en `decisiones.md`: SSR en la landing y cliente (`ssr: false`) en `/app/...` (2026-11-25).
- [ ] Layout de la aplicación con barra lateral y cabecera.
- [ ] Botón «Acceder» de la landing: que lleve a la aplicación en lugar de mostrar el aviso.
- [ ] Módulo de equipos: listado con filtros, detalle y alta/edición.
- [ ] Módulo de incidencias: listado, alta y cambio de estado.
- [ ] Módulo de órdenes: listado, detalle con historial y cambio de estado.
- [ ] Selector de usuario/perfil simulado (sin login real hasta la fase 2).
- [ ] Llamadas a la API con el proxy de Vite (`/api` hacia `localhost:8080`) y la cabecera `X-Usuario-Id`.
- [ ] Tests de los componentes con lógica (formularios y cambio de estado).

Archivos:

| Acción    | Archivo                                            | Propósito                                  |
| --------- | -------------------------------------------------- | ------------------------------------------ |
| Crear     | `src/modules/<modulo>/types.ts`                    | Tipos según `docs/backend.md`              |
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
| Crear     | `frontend/Dockerfile`, `frontend/.dockerignore` | Imagen del frontend           |
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
| 2026-10-28 | 3    | `OrdenTrabajoService.crear` con tests primero: orden `PENDIENTE` o `ASIGNADA` (CA-18), incidencia a `EN_CURSO` con `origen_id` al alta de la orden (RN-18), RN-19, RN-20 (CA-19), RN-07 y RN-04 (CA-20); 77 tests en verde | commit del día |
| 2026-10-31 | 3    | `OrdenTrabajoService.asignar` y `reasignar` con tests primero: `PENDIENTE` → `ASIGNADA` con fila `CAMBIO_TECNICO` enlazada por `origen_id` (CA-20), reasignación solo en `ASIGNADA` y a otro técnico (RN-24, CA-27), órdenes finales inmutables (RN-23, CA-26), RN-01; 91 tests en verde | commit del día |
| 2026-10-31 | 3    | `OrdenTrabajoService.empezar` y `resolver` con tests primero: solo el técnico asignado (RN-06, CA-21), `fechaInicio` y `fechaFin`, resolución copiada al comentario del historial (CA-22); resolver no cierra la incidencia (RN-18); 103 tests en verde | commit del día |
| 2026-10-31 | 3    | `OrdenTrabajoService.validar` y `rechazar` con tests primero: validar cierra la orden y la incidencia (`fechaCierre`, `origen_id` a la validación, CA-24) y recalcula el equipo (RN-11); rechazar devuelve la orden a `EN_CURSO` y el historial conserva las dos resoluciones (CA-23); RN-05, RN-01, RN-23; 118 tests en verde | commit del día |
| 2026-10-31 | 3    | `OrdenTrabajoService.cancelar` con tests primero: desde `PENDIENTE`, `ASIGNADA` o `EN_CURSO`, la incidencia vuelve a `ABIERTA` con `origen_id` a la cancelación y se puede crear una orden nueva (CA-25); una `RESUELTA` no se cancela (RN-01); órdenes finales inmutables (RN-23, CA-26). **Servicios terminados**; 129 tests en verde | commit del día |
| 2026-10-31 | 3    | Conexión a MySQL configurable por variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`). La aplicación arranca fuera de los tests contra MySQL 8.4 en Docker (puerto 3309), valida las migraciones y Hibernate valida el esquema | commit del día |
| 2026-11-03 | 3    | `GlobalExceptionHandler` con tests primero (`@WebMvcTest` y un controlador de prueba): 409, 403, 404, 401, 500 sin detalle y `codigo` también en los errores que resuelve Spring (ruta inexistente, método no permitido); 136 tests en verde | commit del día |
| 2026-11-10 | 3    | Primer controlador real: `UsuarioController` (`GET /api/usuarios` con filtros y `GET /api/usuarios/{id}`) y primer DTO (`UsuarioResponse`), con tests primero sobre MySQL real (`@SpringBootTest` + `MockMvc`); parámetros inválidos a 400 con `VALIDACION`; 144 tests en verde | commit del día |
| 2026-11-11 | 3    | `UbicacionController` (`GET /api/ubicaciones`, ordenadas por nombre) y `UbicacionResponse`, con 2 tests nuevos; 146 tests en verde | commit del día |
| 2026-11-11 | 3    | `EquipoController` en lectura (`GET /api/equipos` con filtros opcionales `estado`, `ubicacionId` y `q` en una sola consulta, y `GET /api/equipos/{id}`) y `EquipoResponse` con la ubicación resumida. Se reproduce el `LazyInitializationException` de `open-in-view=false` (test sin `@Transactional`) y se resuelve con `@EntityGraph` en el repositorio; 155 tests en verde | commit del día |
| 2026-11-15 | 3    | `POST /api/equipos`: cabecera `X-Usuario-Id` opcional (el 401 lo lanza el servicio), `EquipoRequest` como DTO de entrada, `201` con `Location`; se comprueban 401, 403 (RN-05), 409 (RN-09) y 404 a través de la capa web. Lección: un test que espera 404 puede pasar por casualidad, hay que comprobar también el `detail`. 163 tests en verde | commit del día |
| 2026-11-15 | 3    | Validación del cuerpo en `POST /api/equipos`: anotaciones en `EquipoRequest` (`@NotBlank`, `@NotNull`, `@Size` con los límites de las columnas y mensajes en español), `@Valid` en el controlador y `handleMethodArgumentNotValid` en `GlobalExceptionHandler` (400 `VALIDACION` con `errores`); 12 tests en `EquipoControllerCrearTest`; 167 tests en verde | commit del día |
| 2026-11-15 | 3    | `PUT /api/equipos/{id}`: `EquipoEdicionRequest` (sin `codigo` ni `fechaAlta`, lo que no se edita no está en el DTO) y `EquipoControllerEditarTest` (200, persistencia comprobada con un GET posterior, 401, 403, 404 de equipo y de ubicación con `detail`, 409 RN-14, 400 con `errores`); 175 tests en verde | commit del día |
| 2026-11-15 | 3    | `POST /api/equipos/{id}/baja`: `ComentarioRequest` (`@NotBlank`, el cuerpo común de las acciones con motivo) y `EquipoControllerBajaTest` (200, persistencia y entrada de historial con su motivo, 401, 403, 404, 409 RN-12 y RN-01, 400 con comentario vacío o en blanco); 184 tests en verde | commit del día |
| 2026-11-15 | 3    | `POST /api/equipos/{id}/reactivar` (reutiliza `ComentarioRequest`) y `EquipoControllerReactivarTest` (200, persistencia y entrada de historial con su motivo, 401, 403, 404, 409 RN-01, 400 sin comentario); escritura de equipos completa; 191 tests en verde | commit del día |
| 2026-11-15 | 3    | `GET /api/equipos/{id}/historial`: `EntradaHistorialResponse` (usuario resumido, `origenId` sin cargar la relación) y `@EntityGraph(usuario)` en `HistorialRepository`; `EquipoControllerHistorialTest` (orden cronológico, diez campos, entrada consecuencia con su origen, solo el equipo pedido, lista vacía, 404). Controlador de equipos completo; 197 tests en verde | commit del día |
| 2026-11-17 | 3    | `GET /api/incidencias`: filtros opcionales en una sola consulta con `@EntityGraph(equipo, reportadaPor)`, orden por apertura descendente y `ordenActivaId` (orden sin cerrar, RN-20) obtenido con una consulta para todas las incidencias (evita el N+1); DTO `IncidenciaResponse` y resúmenes compartidos `ResumenUsuario`/`ResumenEquipo` (se refactoriza `EntradaHistorialResponse`); 9 tests en `IncidenciaControllerTest`; 206 tests en verde | commit del día |
| 2026-11-18 | 3    | `GET /api/incidencias/{id}`: detalle con `ordenes` (`ResumenOrden`, técnico nulo si la orden está pendiente) en orden cronológico, `ordenActivaId` sacado de esa misma lista y `IncidenciaDetalleResponse` con `@JsonUnwrapped` para que los campos de la incidencia y `ordenes` salgan al mismo nivel; 4 tests nuevos en `IncidenciaControllerTest`; 210 tests en verde | commit del día |
| 2026-11-18 | 3    | `POST /api/incidencias`: `IncidenciaRequest` (equipo, título, descripción y prioridad obligatorios; prioridad inexistente a 400), `201` con `Location` y `ordenActivaId` nulo; `IncidenciaControllerCrearTest` (11 tests: 401, 403 RN-04, 404, 409 RN-14, validación, y RN-10 comprobado desde fuera con el equipo `AVERIADO` y su entrada de historial apuntando al alta de la incidencia); 221 tests en verde | commit del día |
| 2026-11-18 | 3    | `POST /api/incidencias/{id}/prioridad`: `PrioridadRequest` (prioridad obligatoria, comentario opcional), respuesta con `ordenActivaId` calculado con el mismo método auxiliar que usarán descartar y reabrir; `IncidenciaControllerPrioridadTest` (13 tests: cambio con historial y comentario, misma prioridad sin historial, subir a crítica avería el equipo y bajar la única crítica lo repone, 401, 403 RN-05, 404, 409 RN-15, 400); 234 tests en verde | commit del día |
| 2026-11-18 | 3    | `POST /api/incidencias/{id}/descartar` (reutiliza `ComentarioRequest` y `ordenActivaDe`) con `IncidenciaControllerDescartarTest` (11 tests: descarte con historial y motivo, RN-11 por los dos lados —repone el equipo si era la única crítica, lo deja averiado si queda otra—, 401, 403, 404, 409 RN-16, 400). Se añade `@Size(max = 500)` a `ComentarioRequest` según los límites de `docs/backend.md` (afecta también a baja y reactivar); 245 tests en verde | commit del día |
| 2026-11-18 | 3    | `POST /api/incidencias/{id}/reabrir` (mismo molde que descartar) con `IncidenciaControllerReabrirTest` (12 tests: reapertura con `fechaCierre` vacía e historial con su motivo, RN-10 —reabrir una crítica avería el equipo con su entrada apuntando a la reapertura—, 401, 403, 404, 409 RN-17 también para descartadas y RN-14 con el equipo de baja, 400); 257 tests en verde | commit del día |
| 2026-11-18 | 3    | `GET /api/incidencias/{id}/historial` (CA-34): `HistorialRepository.deLaIncidenciaYSusOrdenes`, una consulta con subconsulta que une las entradas de la incidencia y las de sus órdenes filtrando por tipo y por id (el historial es polimórfico) y ordenadas por fecha; `IncidenciaControllerHistorialTest` (7 tests: mezcla cronológica, diez campos, origen de la entrada consecuencia, solo las órdenes propias, sin órdenes, vacío, 404). Controlador de incidencias completo; 264 tests en verde | commit del día |
| 2026-11-18 | 3    | `GET /api/ordenes` y `GET /api/ordenes/{id}`: `OrdenResponse` (11 campos, equipo obtenido de la incidencia, técnico nulo si está pendiente), `OrdenTrabajoRepository.buscar` con filtros opcionales y `@EntityGraph` con ruta anidada `incidencia.equipo`; `OrdenControllerTest` (11 tests: orden, campos y resúmenes, pendiente sin técnico, resuelta con resolución y fechas, tres filtros, 400, 404); 275 tests en verde | commit del día |
| 2026-11-18 | 3    | Decisión sobre la especificación de órdenes: asignar, reasignar, empezar y validar no admiten `comentario` (los servicios no lo reciben y el historial de esas acciones se escribe sin él); solo resolver, rechazar y cancelar llevan texto, obligatorio. Se corrige la tabla de `docs/backend.md`; el comentario opcional puede añadirse en la fase 2 | commit del día |
| 2026-11-21 | 3    | `POST /api/ordenes`: `OrdenRequest` (incidencia y descripción obligatorias, técnico opcional, `@Size(max = 5000)` en la descripción), `201` con `Location`; `OrdenControllerCrearTest` (14 tests: con y sin técnico, cascada de RN-18 comprobada desde fuera con la incidencia en `EN_CURSO` y su `ordenActivaId`, historial encadenado, 401, 403 RN-05/RN-07/RN-04, 404 de incidencia y de técnico, 409 RN-19 y RN-20, 400); 289 tests en verde | commit del día |
| 2026-11-21 | 3    | `POST /api/ordenes/{id}/asignar` y `/reasignar`: `TecnicoRequest` (técnico obligatorio) y `OrdenControllerAsignacionTest` (21 tests: asignar con estado y técnico encadenados en el historial, reasignar solo con el cambio de técnico con su id anterior y nuevo, 401, 403 RN-05/RN-07/RN-04, 404 de orden y de técnico, 409 RN-01, RN-23 y RN-24 también por mismo técnico, 400); 310 tests en verde | commit del día |
| 2026-11-21 | 3    | `POST /api/ordenes/{id}/empezar` y `/resolver`: `empezar` sin `@RequestBody` (acepta petición sin cuerpo o con `{}`), `ResolucionRequest` (RN-21, 5000 caracteres); `OrdenControllerEjecucionTest` (21 tests: fechas de inicio y fin, resolución copiada al comentario del historial, la incidencia no cambia al resolver, 401, 404, 403 RN-06 para otro técnico y para el responsable, RN-04, 409 RN-01 y RN-23, 400); 331 tests en verde | commit del día |
| 2026-11-22 | 3    | `POST /api/ordenes/{id}/validar` (sin cuerpo) y `/rechazar` (`ComentarioRequest`); `OrdenControllerCierreTest` (20 tests: validar cierra la incidencia con su fecha y encadena el historial, repone el equipo si era la única crítica y no si queda otra, rechazar vacía la fecha de fin y no toca la incidencia, el historial conserva las dos resoluciones tras un rechazo —CA-23—, 401, 403 RN-05, 404, 409 RN-01 y RN-23, 400); 351 tests en verde | commit del día |
| 2026-11-22 | 3    | `POST /api/ordenes/{id}/cancelar` (`ComentarioRequest`); `OrdenControllerCancelarTest` (16 tests: desde pendiente, asignada y en curso, cascada de RN-18 con la incidencia otra vez abierta y sin orden activa, ciclo cancelar y crear una orden nueva, el equipo no cambia, historial con motivo y encadenado, 401, 403 RN-05, 404, 409 RN-01 para la resuelta y RN-23 para cerrada y ya cancelada, 400); 367 tests en verde | commit del día |
| 2026-11-22 | 3    | `GET /api/ordenes/{id}/historial`: mismo molde que el de equipos con `TipoEntidad.ORDEN`; `OrdenControllerHistorialTest` (6 tests, incluido el de que no se cuelen entradas de otra entidad con el mismo id). Con él, los 28 endpoints de la API están hechos; 373 tests en verde | commit del día |
| 2026-11-22 | 3    | Cierre de la API: límites que faltaban (`descripcion` de la incidencia, `comentario` de la prioridad); `HistorialNoModificableTest` (CA-33, 405 en PUT, DELETE, POST y PATCH sobre los tres historiales); `AtomicidadTest` (CA-32: un fallo simulado en la cuarta petición de hora al crear una orden deja todas las tablas como estaban; las acciones rechazadas no dejan cambios); `CriteriosDeAceptacionTest` (6 cláusulas sin cubrir: CA-02, 05, 10, 14, 18 y 27). Los criterios de `docs/backend.md` se marcan en el Paso 5; 387 tests en verde | commit del día |
| 2026-11-22 | 3    | **Paso 3 cerrado.** `UsuariosYUbicacionesSoloLecturaTest` (RN-08: POST, PUT, PATCH y DELETE devuelven 405) completa la cobertura: cada regla `RN-xx` tiene al menos un test, de servicio o de integración de la API. README de `backend/` con el arranque. `docs/backend.md` ajustado a cómo se citan las reglas en los tests (por el `codigo` del error o por su efecto, no con `@DisplayName`) y a los comentarios de las órdenes (solo resolver, rechazar y cancelar llevan texto). 28 endpoints y 390 tests en verde | commit del día |
| 2026-11-25 | 4    | Inicio del Paso 4. Decisiones: SSR en la landing y cliente (`ssr: false`) en `/app`; sin datos de prueba previos, directos a la API real. El frontend se mueve de la raíz a `frontend/` (78 archivos renombrados con `git mv`), con su `.gitignore` y su README; la raíz tiene ahora un README de entrada y un `.gitignore` propios y `AGENTS.md` apunta a `frontend/src/...`. Comprobado desde `frontend/`: tests (1), lint (0 errores), TypeScript y arranque de la landing | commit del día |
| 2026-10-18 | 1    | Se quita la etiqueta `v0.1-landing` de los criterios de cierre. Paso cerrado | `40276b1` |
