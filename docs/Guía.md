# Mantix — Guía de referencia del proyecto

Oct 18, 2026 · @Carlos

## Propósito y visión

Esta guía fija el rumbo de Mantix: qué se construye, en qué orden y cómo se decide cuándo una fase está terminada. Se consulta antes de empezar cada paso y se actualiza cuando cambia una decisión.

Mantix es una aplicación web para gestionar el mantenimiento de una instalación industrial: equipos, incidencias y órdenes de trabajo. El centro del proyecto no es el número de pantallas, sino el flujo de trabajo y sus reglas: qué transiciones de estado están permitidas, quién hace cada cambio y cómo queda registrado.

El objetivo es doble: construir una aplicación que modele un proceso real y practicar lo que aporta valor al perfil técnico (lógica de negocio, estados, trazabilidad y calidad del backend), con el stack ya conocido.

## Estado actual

La landing está terminada y compila: es el punto de partida estable y la fuente de la identidad visual de Mantix.

- Generada con Lovable a partir del prompt revisado.
- Repositorio en GitHub, clonado en VS Code.
- Eliminadas las referencias a Lovable.
- Compila con Vite y se ejecuta en local.
- Botón «Acceder» con aviso de acceso próximo, sin inicio de sesión real.

La landing define los estilos (paleta, tipografía, componentes) que heredará la aplicación interna.

## Stack técnico

Se usa el stack ya conocido; cada proyecto nuevo obliga a aprender algo técnico distinto, no otra tecnología.

| Capa      | Tecnología                               | Qué se practica                                            |
| --------- | ---------------------------------------- | ---------------------------------------------------------- |
| Frontend  | React + TypeScript + Vite + Tailwind CSS | Formularios complejos, tablas, filtros, gestión del estado |
| Backend   | Java + Spring Boot                       | Servicios, reglas de negocio, validaciones, transacciones  |
| Datos     | MySQL + JPA                              | Relaciones, consultas, restricciones, rendimiento          |
| Pruebas   | JUnit + pruebas de integración de la API | Reglas de estado cubiertas por tests                       |
| Seguridad | Spring Security (fase 2)                 | Autenticación y permisos por perfil                        |
| Entorno   | Docker                                   | Aplicación y base de datos reproducibles                   |

Iconos con Lucide React. Solo se añaden dependencias imprescindibles.

## Fases del proyecto

Tres fases; no se pasa a la siguiente sin cerrar los criterios de la anterior.

| Fase                       | Contenido                                                                                     | Se cierra cuando                                                                      |
| -------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 1. MVP                     | Equipos, incidencias, órdenes de trabajo, estados, historial y perfiles técnico y responsable | El flujo completo funciona de punta a punta y las transiciones de estado tienen tests |
| 2. Solidez técnica         | Pruebas, permisos por perfil, paginación, manejo de errores, consultas e indicadores          | La API es robusta ante datos erróneos y hay control de acceso                         |
| 3. Ampliaciones opcionales | Mantenimiento preventivo, repuestos, avisos, documentación adjunta                            | Se elige cada ampliación por separado, solo si aporta aprendizaje nuevo               |

## Hoja de ruta del MVP

Cinco pasos en orden; la especificación es el más importante y va antes de escribir backend.

1. **Cerrar la base del frontend.** Revisar `package.json`, `vite.config` y el lockfile por restos de Lovable. README con arranque. Commit como punto de partida.
   - Entregable: repositorio limpio.
2. **Especificación del MVP.** Modelo de datos, estados, reglas, API y criterios de aceptación (ver sección siguiente). Se guarda en `docs/` del repositorio `Mantix`.
   - Entregable: `docs/backend.md` revisada y cerrada.
3. **Backend del MVP.** Spring Boot + JPA: entidades y migraciones, servicios con reglas de estado, controladores. Tests de transiciones antes que los controladores.
   - Entregable: API del MVP con tests pasando.
4. **Frontend de la aplicación.** Pantallas de equipos, incidencias y órdenes. Primero con datos de prueba tipados según la especificación; después conectadas a la API.
   - Entregable: flujo completo usable desde el navegador.
5. **Docker y revisión de fase.** Levantar aplicación y base de datos con un comando. Revisar contra los criterios de aceptación.
   - Entregable: `docker compose up` funcional y checklist del MVP cumplida.

Los pasos 3 y 4 pueden solaparse: el frontend avanza con datos de prueba mientras se construye la API.

## Contenido de la especificación del MVP

La especificación (`docs/backend.md`) responde a seis preguntas; sin ellas cerradas no se empieza el backend.

| Apartado                | Pregunta que responde                                      | Ejemplos a decidir                                                  |
| ----------------------- | ---------------------------------------------------------- | ------------------------------------------------------------------- |
| Modelo de datos         | ¿Qué entidades hay y cómo se relacionan?                   | Equipo, ubicación, incidencia, orden de trabajo, usuario, historial |
| Estados y transiciones  | ¿Qué estados tiene cada entidad y qué cambios se permiten? | Abierta → asignada → en curso → resuelta → cerrada                  |
| Reglas de negocio       | ¿Qué está prohibido u obligatorio?                         | Una orden cerrada no se reabre sin motivo registrado                |
| Trazabilidad            | ¿Qué se registra en cada cambio?                           | Quién, cuándo, estado anterior y nuevo, comentario                  |
| API                     | ¿Qué endpoints necesita el MVP?                            | CRUD de equipos, alta de incidencia, cambio de estado de orden      |
| Criterios de aceptación | ¿Cuándo está terminado el MVP?                             | Lista verificable, una línea por comportamiento                     |

Los indicadores (tiempos de resolución, equipos con más incidencias) se especifican en la fase 2.

## Convenciones del repositorio

Un solo repositorio, `Mantix`, con frontend y backend.

- Raíz: la aplicación React (frontend).
- `backend/`: el proyecto Spring Boot.
- `docs/`: toda la documentación, incluida la especificación del backend (`docs/backend.md`), donde vive el contrato de la API.
- Seguimiento del avance: [plan-de-trabajo.md](plan-de-trabajo.md).
- `docker-compose.yml` para levantar frontend, backend y base de datos: va en la raíz.

Flujo de trabajo:

- Rama `main` siempre compilable; una rama por tarea.
- Commits pequeños con mensaje descriptivo.
- Cada decisión relevante se anota en `docs/decisiones.md` con fecha y motivo.
- Frontend organizado por módulos (`equipos/`, `incidencias/`, `ordenes/`) cuando lleguen; los componentes comunes en `components/`.

## Reglas de alcance

Mantix tiene que terminarse; estas reglas evitan que se convierta en un proyecto interminable.

- Nada de IA, microservicios, Kafka ni Kubernetes porque sí. Primero una aplicación bien construida con el stack actual.
- Una funcionalidad nueva entra solo si está en la fase en curso o si sustituye a otra.
- Mejor pocas pantallas con reglas sólidas que muchas pantallas sin lógica.
- Mantix no compite en prioridad con DeCA ni con la búsqueda de empleo: se desarrolla porque mejora el perfil, no como sustituto de esa búsqueda.

## Decisiones tomadas

Las decisiones previas a la especificación están cerradas; los cambios futuros se anotan en docs/decisiones.md.

- [x] Estilos: Tailwind CSS en toda la aplicación.
- [x] Base de datos: MySQL.
- [x] Estructura del repositorio: un solo repositorio (`Mantix`), con el frontend en la raíz y el backend en `backend/`.
- [x] Documentación: centralizada en `docs/` de `Mantix`; la especificación del backend es `docs/backend.md`.
- [x] Usuarios en el MVP: perfiles básicos (técnico y responsable) desde el inicio, para registrar quién hace cada cambio.
