# telosabes2026

Aplicación web con **Spring Boot + Thymeleaf + MariaDB + Spring Security**.

## Tecnologías

| Componente | Versión |
|---|---|
| Java | 21 (Temurin LTS) |
| Spring Boot | 4.1.1 |
| Spring Security | 7.1 |
| Spring Data JPA / Hibernate | 7.4 |
| Thymeleaf | 3.1 |
| MariaDB | 11.6 |
| Maven | 3.10 (wrapper incluido) |

## Roles de usuario

| Rol | Cómo se crea | Permisos |
|---|---|---|
| `ALUMNO` | Formulario público `/registro` | Zona común y ejemplo de notas |
| `PROFESOR` | El administrador promueve a un alumno | Todo lo del alumno + listado de alumnos |
| `ADMIN` | **Solo por SQL** (`db/crear-admin.ps1`) | Gestión de usuarios: promover alumnos a profesores |

Reglas importantes:

- El formulario de registro **no tiene campo de rol**: el servicio asigna siempre `ALUMNO`,
  aunque alguien añada `rol=ADMIN` a mano en el envío.
- El alumno **sí elige su ciclo** en el registro (`DAM`, `DAW`, `ASIR` o `SMR`). El
  desplegable se rellena con la tabla `ciclos` y el servicio comprueba que el ciclo existe.
- El administrador **no existe en la aplicación**: no hay pantalla para crearlo ni para
  editarlo, y **no aparece en ningún listado** (se filtra con `findByRolNot(ADMIN)`).
  Su contraseña se escribe directamente en la base de datos, cifrada con BCrypt.
- Las contraseñas se guardan siempre con **BCrypt** (`BCryptPasswordEncoder`), nunca en claro.

## Ciclos formativos

La tabla `ciclos` guarda los cuatro ciclos del centro. Se dan de alta solos al arrancar
(`CiclosIniciales`), y solo inserta los que faltan, así que se puede arrancar la aplicación
tantas veces como se quiera:

| Siglas | Nombre |
|---|---|
| `DAM` | Desarrollo de Aplicaciones Multiplataforma |
| `DAW` | Desarrollo de Aplicaciones Web |
| `ASIR` | Administración de Sistemas Informáticos y Redes |
| `SMR` | Sistemas Microinformáticos y Redes |

El alumno guarda el ciclo en `usuarios.ciclo_id` (clave ajena a `ciclos.id`). El ciclo se ve
en el listado del administrador y en el panel del profesor.

## Control de acceso

| Ruta | Acceso |
|---|---|
| `/`, `/login`, `/registro`, `/css/**` | Público |
| `/notas/**` | Cualquier usuario con sesión iniciada |
| `/profesor/**` | `PROFESOR` y `ADMIN` |
| `/admin/**` | Solo `ADMIN` |
| `/acceso-denegado` | Página 403 |

## Estructura del proyecto

```
telosabes2026/
├── db/
│   ├── crear_bd.sql                  # crea la base de datos y su usuario
│   ├── crear-admin.cmd               # lanzador (Windows bloquea los .ps1)
│   ├── crear-admin.ps1               # crea/cambia el administrador (hash BCrypt)
│   └── GenerarHash.java              # utilidad para generar hashes BCrypt
├── src/main/java/com/telosabes/telosabes2026/
│   ├── Telosabes2026Application.java # clase principal
│   ├── home/HomeController.java      # página de inicio "/"
│   ├── ciclo/                        # ciclos formativos (tabla "ciclos")
│   │   ├── Ciclo.java                # entidad: nombre y siglas
│   │   ├── CicloRepository.java
│   │   └── CiclosIniciales.java      # alta automática de DAM, DAW, ASIR y SMR
│   ├── nota/                         # ejemplo CRUD completo (tabla "notas")
│   │   ├── Nota.java                 # entidad, con autor (FK a usuarios)
│   │   ├── NotaForm.java             # formulario: sin campo de autor
│   │   ├── NotaRepository.java
│   │   └── NotaController.java       # rutas /notas
│   ├── seguridad/
│   │   ├── Rol.java                  # ADMIN, PROFESOR, ALUMNO
│   │   ├── SecurityConfig.java       # reglas de acceso, login y logout
│   │   ├── UsuarioPrincipal.java     # adapta Usuario a UserDetails
│   │   ├── UsuarioDetailsService.java# carga el usuario en el login
│   │   ├── UsuarioActual.java        # averigua el usuario de la sesión
│   │   ├── UsuarioActualAdvice.java  # expone "usuarioActual" a las plantillas
│   │   └── LoginController.java      # /login y /acceso-denegado
│   ├── usuario/
│   │   ├── Usuario.java              # entidad (tabla "usuarios", con ciclo_id)
│   │   ├── UsuarioRepository.java
│   │   ├── UsuarioService.java       # registro y promoción (reglas de rol)
│   │   ├── RegistroForm.java         # formulario público: nombre, correo, ciclo y claves
│   │   └── RegistroController.java   # /registro
│   ├── admin/AdminUsuariosController.java   # /admin/usuarios
│   └── profesor/ProfesorController.java     # /profesor
├── src/main/resources/
│   ├── application.properties        # configuración (incluye MariaDB)
│   ├── static/css/estilos.css
│   └── templates/
│       ├── index.html
│       ├── login.html
│       ├── registro.html
│       ├── error/403.html
│       ├── fragments/layout.html     # cabecera, navegación por rol y avisos
│       ├── admin/usuarios.html
│       ├── profesor/panel.html
│       └── notas/lista.html
├── src/test/java/.../seguridad/SeguridadTests.java   # 14 pruebas de seguridad
├── src/test/java/.../ciclo/CicloTests.java           # 6 pruebas de ciclos y del registro
├── src/test/java/.../nota/NotaTests.java             # 5 pruebas del autor de las notas
└── src/test/resources/application.properties         # H2 en memoria para tests
```

## Puesta en marcha

### 1. Crear la base de datos

```bash
mariadb -u root -p -e "source db/crear_bd.sql"
```

Esto crea la base `telosabes2026` y el usuario `telosabes2026` / `telosabes2026`.
Si cambias credenciales, actualiza también `src/main/resources/application.properties`.

### 2. Arrancar la aplicación

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Al arrancar, Hibernate crea las tablas `notas`, `usuarios` y `ciclos`, y se dan de alta
los cuatro ciclos formativos (DAM, DAW, ASIR y SMR).

### 3. Crear el administrador (solo la primera vez)

```powershell
.\db\crear-admin.cmd -Email "admin@telosabes2026.local" -Clave "TuClaveSecreta1!"
```

- Genera el hash BCrypt y hace el `INSERT`/`UPDATE` en `usuarios`.
- Pide la contraseña de `root` de MariaDB de forma interactiva.
- Para cambiar la contraseña del administrador, vuelve a ejecutarlo con la nueva clave.
- Se usa el lanzador `.cmd` porque Windows suele tener bloqueada la ejecución de
  scripts `.ps1`. Si prefieres el script directamente:
  `powershell -ExecutionPolicy Bypass -File db\crear-admin.ps1 -Email ... -Clave ...`
- Opcional: `-ClaveRoot "..."` para pasar la contraseña de root sin que la pregunte
  (útil para automatizar; queda visible en la línea de comandos).

Equivalente manual (útil para entender qué se guarda):

```sql
UPDATE usuarios
   SET clave = '$2a$10$rQ8k...'   -- hash BCrypt
 WHERE email = 'admin@telosabes2026.local';
```

### 4. Usar la aplicación

1. Abre <http://localhost:8080>
2. Entra en **Registrarse**, crea un alumno y **elige el ciclo** que va a estudiar.
3. Inicia sesión con el administrador en `/login`.
4. En **Usuarios** (`/admin/usuarios`) verás el ciclo de cada alumno y podrás pulsar
   *Hacer profesor* sobre ese alumno.
5. Cierra sesión y entra con el alumno: ahora verá la opción **Profesor**.

## El autor de las notas

El ejemplo CRUD de `/notas` guarda **automáticamente el usuario de la sesión**
como autor de cada nota:

- `NotaForm` solo tiene `titulo` y `contenido`: no existe un campo de autor que
  se pueda manipular desde el navegador.
- El controlador obtiene el usuario con `UsuarioActual` y lo asigna él mismo
  (`new Nota(titulo, contenido, autor)`), así que **no se puede suplantar** a otro usuario.
- En la base de datos la relación es `notas.autor_id` → `usuarios.id` (columna obligatoria).
- El listado muestra el autor de cada nota con su nombre y su rol, y el formulario
  avisa de quién va a quedar como autor antes de guardar.

## Compilar y probar

```bash
mvnw.cmd clean package      # genera el .jar en target/
mvnw.cmd test               # 26 pruebas (usan H2, no necesitan MariaDB)
```

Pruebas incluidas en `SeguridadTests`:

- las páginas de login y registro son públicas
- sin sesión, `/admin/**` redirige al login
- un alumno recibe 403 en `/admin/**` y en `/profesor/**`
- el registro crea siempre un `ALUMNO` con la clave cifrada y el ciclo elegido
- no se puede colar `rol=ADMIN` en el formulario de registro
- se rechazan correos repetidos y contraseñas que no coinciden
- login correcto e incorrecto con correo + contraseña
- el listado del administrador no incluye al administrador
- el administrador promueve a un alumno; un alumno no puede promoverse

Pruebas incluidas en `CicloTests`:

- los cuatro ciclos (DAM, DAW, ASIR, SMR) están dados de alta
- el formulario de registro ofrece los cuatro ciclos
- el registro guarda el ciclo elegido por el alumno
- sin ciclo no se registra nadie
- con un ciclo que no existe no se registra nadie
- el listado del administrador muestra el ciclo de cada alumno

Pruebas incluidas en `NotaTests`:

- la nota guarda como autor al usuario de la sesión
- el autor no se puede falsificar enviando `autor`, `autor.id` o `autor_id`
- el listado y el formulario muestran el autor
- una nota sin título no se guarda
- sin sesión iniciada no se puede crear una nota

## Variables de entorno (opcional)

| Variable | Valor por defecto |
|---|---|
| `DB_USER` | `telosabes2026` |
| `DB_PASSWORD` | `telosabes2026` |

```powershell
$env:DB_PASSWORD="mi-clave-segura"; .\mvnw.cmd spring-boot:run
```

## Configuración de la base de datos

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/telosabes2026
spring.datasource.username=${DB_USER:telosabes2026}
spring.datasource.password=${DB_PASSWORD:telosabes2026}
spring.jpa.hibernate.ddl-auto=update
```

## Notas

- `spring.jpa.hibernate.ddl-auto=update` es cómodo en desarrollo pero **no** debe
  usarse en producción: allí se recomienda `validate` junto con Flyway o Liquibase.
- Las credenciales reales nunca deben subirse al repositorio: usa variables de
  entorno o un fichero `application-local.properties` (ya está en `.gitignore`).
- El login se hace con el **correo electrónico**; los correos se guardan en minúsculas.
