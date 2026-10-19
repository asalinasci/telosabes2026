# telosabes2026

Aplicación web con **Spring Boot + Thymeleaf + MariaDB**.

## Tecnologías

| Componente | Versión |
|---|---|
| Java | 21 (Temurin LTS) |
| Spring Boot | 4.1.1 |
| Spring Data JPA / Hibernate | 7.4 |
| Thymeleaf | 3.1 |
| MariaDB | 11.6 |
| Maven | 3.10 (wrapper incluido) |

## Estructura del proyecto

```
telosabes2026/
├── db/
│   └── crear_bd.sql                  # crea la base de datos y el usuario
├── src/main/java/com/telosabes/telosabes2026/
│   ├── Telosabes2026Application.java # clase principal
│   ├── home/HomeController.java      # página de inicio "/"
│   └── nota/                         # ejemplo CRUD completo
│       ├── Nota.java                 # entidad JPA (tabla "notas")
│       ├── NotaRepository.java       # Spring Data JPA
│       └── NotaController.java       # rutas /notas
├── src/main/resources/
│   ├── application.properties        # configuración (incluye MariaDB)
│   ├── static/css/estilos.css        # estilos
│   └── templates/
│       ├── index.html
│       ├── fragments/layout.html     # cabecera y avisos reutilizables
│       └── notas/lista.html
└── src/test/resources/application.properties   # H2 en memoria para tests
```

## Puesta en marcha

### 1. Crear la base de datos

```bash
mariadb -u root -p -e "source db/crear_bd.sql"
```

Esto crea la base `telosabes2026` y el usuario `telosabes2026` / `telosabes2026`.
Si cambias credenciales, actualiza también `src/main/resources/application.properties`.

### 2. Arrancar la aplicación

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Abre el navegador en: <http://localhost:8080>

- `http://localhost:8080/` → página de inicio
- `http://localhost:8080/notas` → ejemplo CRUD (crear, listar, eliminar)

Hibernate crea la tabla `notas` automáticamente al arrancar
(`spring.jpa.hibernate.ddl-auto=update`).

## Compilar y probar

```bash
mvnw.cmd clean package      # genera el .jar en target/
mvnw.cmd test               # ejecuta los tests (usan H2, no necesitan MariaDB)
```

El `.jar` se ejecuta con:

```bash
java -jar target/telosabes2026-0.0.1-SNAPSHOT.jar
```

## Variables de entorno (opcional)

Las credenciales de base de datos se pueden sobreescribir sin tocar el código:

| Variable | Valor por defecto |
|---|---|
| `DB_USER` | `telosabes2026` |
| `DB_PASSWORD` | `telosabes2026` |

Ejemplo en PowerShell:

```powershell
$env:DB_PASSWORD="mi-clave-segura"; .\mvnw.cmd spring-boot:run
```

## Configuración de la base de datos

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/telosabes2026
spring.datasource.username=${DB_USER:telosabes}
spring.datasource.password=${DB_PASSWORD:telosabes}
spring.jpa.hibernate.ddl-auto=update
```

## Notas

- `spring.jpa.hibernate.ddl-auto=update` es cómodo en desarrollo pero **no** debe
  usarse en producción: allí se recomienda `validate` junto con Flyway o Liquibase.
- Las credenciales reales nunca deben subirse al repositorio: usa variables de
  entorno o un fichero `application-local.properties` (ya está en `.gitignore`).
