# Mantix · Backend

API REST del MVP de Mantix: equipos, incidencias y órdenes de trabajo, con su historial y las reglas de estado.
Spring Boot 4, Java 21, Spring Data JPA, Flyway y MySQL 8.4.

La especificación completa (modelo de datos, reglas de negocio, endpoints y criterios de aceptación) está en
[`docs/backend.md`](../docs/backend.md).

## Requisitos

- Java 21
- Docker Desktop (para MySQL y para los tests)

No hace falta instalar Maven: se usa el wrapper del proyecto (`mvnw`).

## Base de datos para desarrollo

La aplicación necesita un MySQL 8.4. La forma más simple es un contenedor, en el puerto 3309 del equipo:

```powershell
docker run -d --name mantix-db `
  -e MYSQL_ROOT_PASSWORD=root `
  -e MYSQL_DATABASE=mantix `
  -e MYSQL_USER=mantix `
  -e MYSQL_PASSWORD=mantix `
  -p 3309:3306 `
  -v mantix-datos:/var/lib/mysql `
  mysql:8.4

```

Son credenciales de desarrollo. No hay que crear las tablas a mano: Flyway las crea al arrancar la aplicación
(`V1` el esquema, `V2` los datos iniciales: un responsable, tres técnicos y tres ubicaciones).

Para parar y volver a arrancar el contenedor: `docker stop mantix-db` y `docker start mantix-db`.
Si cambias las credenciales con un volumen ya creado, MySQL conserva las antiguas: borra el contenedor y el volumen
(`docker rm -f mantix-db` y `docker volume rm mantix-datos`) y vuelve a crearlos.

## Arranque

La conexión se configura con tres variables de entorno. Si no las defines, se usan los valores por defecto de
`application.properties`, que apuntan al puerto 3306:

| Variable      | Por defecto                          |
| ------------- | ------------------------------------ |
| `DB_URL`      | `jdbc:mysql://localhost:3306/mantix` |
| `DB_USER`     | `mantix`                             |
| `DB_PASSWORD` | `mantix`                             |

Con el contenedor del apartado anterior (puerto 3309), desde la carpeta `backend/`:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3309/mantix"
.\mvnw spring-boot:run
```

La API queda en http://localhost:8080. Para comprobarlo: http://localhost:8080/api/usuarios.

Las peticiones de escritura (`POST` y `PUT`) llevan la cabecera `X-Usuario-Id` con el id del usuario que actúa
(no hay login en el MVP). Por ejemplo, el responsable de los datos iniciales tiene el id 1.

## Tests

```powershell
.\mvnw verify
```

Los tests de integración levantan su propio MySQL con Testcontainers, así que **Docker Desktop tiene que estar en
marcha**. No usan la base de datos de desarrollo.

Para ejecutar solo una clase: `.\mvnw test -Dtest=NombreDeLaClaseTest`.

## Estructura

| Paquete                           | Contenido                                                        |
| --------------------------------- | ---------------------------------------------------------------- |
| `com.mantix.domain`               | Entidades y enums de estado                                      |
| `com.mantix.repository`           | Repositorios de Spring Data                                      |
| `com.mantix.service`              | Servicios: reglas de negocio, permisos y registro del historial  |
| `com.mantix.web`                  | Controladores REST y manejo global de errores                    |
| `com.mantix.web.dto`              | Objetos de entrada y salida de la API                            |
| `src/main/resources/db/migration` | Migraciones de Flyway                                            |

## Errores

Todos los errores se devuelven como `application/problem+json` con un campo `codigo` (por ejemplo `RN-05`,
`NO_ENCONTRADO` o `VALIDACION`). Los de validación añaden la lista `errores` con el campo afectado.