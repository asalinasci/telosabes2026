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
| `ALUMNO` | Formulario público `/registro` | Zona común, notas y **su perfil** (puede cambiar su ciclo) |
| `PROFESOR` | El administrador promueve a un alumno | Todo lo del alumno + listado de alumnos |
| `ADMIN` | **Solo por SQL** (`db/crear-admin.ps1`) | Gestión de usuarios: cambiar el ciclo, editar sus datos y promover alumnos a profesores |

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
en el listado del administrador, en el panel del profesor y en **su propio perfil**, donde
**el alumno puede cambiarlo él mismo** sin depender del administrador.

## Zona del administrador

En `/admin/usuarios` el administrador ve el listado de alumnos y profesores (nunca el
administrador) y puede hacer tres cosas con cada usuario:

- **Cambiar el ciclo**: un desplegable en el listado (con la opción *Sin ciclo*) que se
  guarda en el momento, sin salir de la página.
- **Editar sus datos**: nombre, correo y ciclo desde `/admin/usuarios/{id}/editar`.
  El correo se guarda en minúsculas y no puede coincidir con el de otro usuario.
- **Hacer profesor**: convierte el alumno en `PROFESOR`.

El formulario de edición **no tiene campo de rol** (`EditarUsuarioForm`): el rol solo se
cambia con *Hacer profesor*, así que no se puede enviar `rol=ADMIN` a mano. El servicio
comprueba el ciclo contra la tabla `ciclos` (no se fía del formulario) y **rechaza cualquier
cambio sobre la cuenta de administrador**, aunque se llame a la ruta directamente.

## Perfil del usuario

En `/perfil` (enlace **Mi perfil** en la cabecera) cada usuario con sesión iniciada ve sus
datos: nombre, correo, rol y ciclo. Debajo tiene un desplegable para **cambiar de ciclo**
cuando quiera, con la opción *Sin ciclo*.

- El usuario **no viaja en el formulario**: se toma siempre de la sesión (`UsuarioActual`),
  así que **nadie puede cambiar el ciclo de otro** ni aunque manipule la petición.
- El ciclo se comprueba contra la tabla `ciclos`; si no existe, no se cambia nada.
- El perfil se recarga de la base de datos (no del principal de la sesión, que se creó al
  iniciar sesión y tendría el ciclo antiguo).
- La cuenta de administrador ve el perfil, pero **sin desplegable**: sus datos no se
  gestionan desde la aplicación.

## Módulos profesionales

Cada ciclo tiene sus módulos oficiales en la tabla `modulos` (clave ajena `modulos.ciclo_id`
→ `ciclos.id`). Se dan de alta solos al arrancar (`ModulosIniciales`), después de los ciclos,
y solo inserta los que faltan:

| Ciclo | Módulos | 1º curso | 2º curso | Horas |
|---|---|---|---|---|
| `DAM` | 17 | 8 | 9 | 2000 |
| `DAW` | 16 | 8 | 8 | 2000 |
| `ASIR` | 17 | 8 | 9 | 2000 |
| `SMR` | 15 | 7 | 8 | 2000 |

Los datos son los de las tablas de la Consejería de Educación y Formación Profesional de la
Región de Murcia (curso 2025/2026, nuevo sistema de FP). De cada módulo se guarda el código
oficial del Real Decreto, el curso, el orden, las horas semanales y el reparto entre las horas
que se imparten en el centro y las de la fase de formación en la empresa. Cada ciclo tiene
2000 horas repartidas en dos cursos de 30 horas semanales.

En todos los ciclos están los módulos transversales del nuevo sistema: *Itinerario personal
para la empleabilidad I y II*, *Digitalización aplicada a los sectores productivos*,
*Sostenibilidad aplicada al sistema productivo*, *Inglés profesional* y el *Proyecto
intermodular*. El módulo optativo se guarda sin código, porque cada centro elige la optativa
del catálogo.

Se pueden consultar en el catálogo público **`/ciclos`**, enlazado desde la cabecera.

### Resultados de aprendizaje

Cada módulo tiene sus resultados de aprendizaje oficiales en la tabla `ras` (clave ajena
`ras.modulo_id` → `modulos.id`), con el orden y el texto literal del BOE. Se muestran como
desplegables dentro del catálogo `/ciclos` y se dan de alta solos al arrancar
(`RasIniciales`, después de los módulos): sube cada RA por módulo y orden, crea los que
faltan y borra los que ya no estén en el fichero. Son **373 RA en los 61 módulos con
código**; los 4 módulos optativos no tienen RA porque la optativa la elige cada centro.

Los textos viven en `src/main/resources/ras.txt` y se regeneran desde el BOE con
`python tools/generar_ras.py`. Cada línea es `GRUPO|CODIGO_MODULO|ORDEN|TEXTO`, y el grupo
indica de qué Real Decreto procede y a qué ciclos se aplica:

| Grupo | Fuente (BOE) | Ciclos | Módulos |
|---|---|---|---|
| `DAM` | RD 450/2010 | DAM | 0373, 0483-0492 |
| `DAW` | RD 686/2010 | DAW | 0373, 0483-0485, 0487, 0612-0616 |
| `ASIR` | RD 1629/2009 | ASIR | 0369-0379 |
| `SMR` | RD 1691/2007 | SMR | 0221-0228 |
| `TRANS` | RD 659/2023 | los cuatro | 1709, 1710, 1708 |
| `SUPER` | RD 659/2023 | DAM, DAW y ASIR | 1665, 0179 |
| `MEDIO` | RD 659/2023 y RD 499/2024 | SMR | 1664, 0156, 1713 |

El mismo código puede variar ligeramente de un título a otro (el 0373 solo cambia en las
comas de un Real Decreto a otro), por eso cada ciclo toma la fuente de su propio título y
los módulos transversales se comparten entre ciclos. La relación es `@ManyToOne` desde el
RA hacia el módulo, como en todo el proyecto: no hay ninguna `@OneToMany`.

### Criterios de evaluación

Cada resultado de aprendizaje tiene sus criterios de evaluación (las letras `a)`, `b)`,
`c)`... de la tabla oficial) en la tabla `criterios` (clave ajena `criterios.ra_id` →
`ras.id`, con `@ManyToOne` desde el criterio hacia su RA). Se dan de alta solos al arrancar
(`CriteriosIniciales`, después de los RA) y se ven dentro del desplegable de cada RA en
`/ciclos`. En total son **2912 criterios en los 373 RA** (1722 en DAM y 578 en SMR, porque
los módulos transversales se comparten entre los cuatro ciclos y cada ciclo tiene sus
propias filas).

Los textos viven en `src/main/resources/criterios.txt`, generados junto con `ras.txt` por
`python tools/generar_ras.py` (en el BOE van en la misma tabla). Cada línea es
`GRUPO|CODIGO_MODULO|ORDEN_RA|ORDEN|TEXTO`, donde `ORDEN_RA` es el RA al que pertenecen
(RA1, RA2...) y `ORDEN` la letra (1 = `a)`, 2 = `b)`, 3 = `c)`...). El criterio más largo
ronda los 6000 caracteres, por eso las columnas de texto admiten 8000: se guardan literales,
sin cortar.

### Consulta de temario (`/modulos/estudio`)

Para consultar los RA y los criterios con comodidad hay una pantalla propia,
**`/modulos/estudio`** (enlace **Módulos** de la cabecera, y accesos desde el perfil, el
panel del profesor y la portada). Está pensada para alumnos y profesores:

- Se elige el ciclo con un desplegable. El alumno empieza viendo el ciclo que estudia y
  puede cambiar a otro cuando quiera; el profesor y el administrador no tienen ciclo propio,
  así que arrancan en el primero.
- Los módulos van separados por curso, con su código, horas semanales y reparto de horas, y
  todo el temario sale desplegado: los RA de cada módulo con los criterios de evaluación de
  cada RA dentro.
- El módulo optativo se distingue porque no tiene RA oficiales.

Es una ruta para cualquier usuario con la sesión iniciada y reutiliza el mismo fragmento
Thymeleaf que el catálogo público, así que `/ciclos` y `/modulos/estudio` muestran siempre el
mismo temario.

## Control de acceso

| Ruta | Acceso |
|---|---|
| `/`, `/login`, `/registro`, `/ciclos`, `/css/**` | Público |
| `/perfil/**`, `/notas/**`, `/modulos/estudio` | Cualquier usuario con sesión iniciada |
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
├── deploy/                           # publicar en un servidor Ubuntu
│   ├── instalar.sh                   # instala Java, MariaDB y Nginx, y crea el servicio
│   ├── crear-admin.sh                # crea/cambia el administrador (hash BCrypt)
│   ├── copia.sh                      # volcado y restauración de la base de datos
│   ├── telosabes2026.service         # unidad de systemd
│   ├── telosabes2026-copia.service   # unidad de copia (una sola vez)
│   ├── telosabes2026-copia.timer     # ejecuta la copia una vez al día
│   ├── configurar-ssh.sh             # acceso SSH (clave para todos, contraseña solo root)
│   ├── ssh-00-telosabes.conf         # configuración de sshd que aplica el anterior
│   └── nginx-telosabes2026.conf      # Nginx como reverse proxy
├── tools/
│   └── generar_ras.py                # regenera ras.txt a partir del BOE
├── src/main/java/com/telosabes/telosabes2026/
│   ├── Telosabes2026Application.java # clase principal
│   ├── home/HomeController.java      # página de inicio "/"
│   ├── ciclo/                        # ciclos formativos (tabla "ciclos")
│   │   ├── Ciclo.java                # entidad: nombre y siglas
│   │   ├── CicloRepository.java
│   │   └── CiclosIniciales.java      # alta automática de DAM, DAW, ASIR y SMR
│   ├── modulo/                       # módulos profesionales (tabla "modulos")
│   │   ├── Modulo.java               # entidad, con ciclo (FK a ciclos) y horas
│   │   ├── ModuloRepository.java
│   │   ├── ModulosIniciales.java     # alta automática de los módulos de cada ciclo
│   │   ├── ModuloController.java     # catálogo público /ciclos
│   │   └── EstudioController.java    # consulta de RA y criterios /modulos/estudio
│   ├── ra/                           # resultados de aprendizaje (tabla "ras")
│   │   ├── RA.java                   # entidad: orden, texto oficial y FK a modulos
│   │   ├── RaRepository.java
│   │   └── RasIniciales.java         # alta automática de los 373 RA (idempotente)
│   ├── criterio/                     # criterios de evaluación (tabla "criterios")
│   │   ├── Criterio.java             # entidad: orden (letra), texto y FK al RA
│   │   ├── CriterioRepository.java
│   │   └── CriteriosIniciales.java   # alta automática de los 2912 criterios
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
│   │   ├── UsuarioService.java       # registro, edición y ciclo (reglas de rol)
│   │   ├── RegistroForm.java         # formulario público: nombre, correo, ciclo y claves
│   │   ├── EditarUsuarioForm.java    # formulario del admin: nombre, correo y ciclo (sin rol)
│   │   ├── RegistroController.java   # /registro
│   │   └── PerfilController.java     # /perfil (cada uno cambia su propio ciclo)
│   ├── admin/AdminUsuariosController.java   # /admin/usuarios (ciclo, editar y promover)
│   └── profesor/ProfesorController.java     # /profesor
├── src/main/resources/
│   ├── application.properties        # configuración (incluye MariaDB)
│   ├── application-prod.properties   # perfil de producción (VPS + Nginx)
│   ├── ras.txt                       # RA oficiales (lo genera tools/generar_ras.py)
│   ├── criterios.txt                 # criterios de evaluación (mismo generador)
│   ├── static/css/estilos.css
│   └── templates/
│       ├── index.html
│       ├── login.html
│       ├── registro.html
│       ├── error/403.html
│       ├── fragments/layout.html     # cabecera, navegación por rol y avisos
│       ├── admin/usuarios.html
│       ├── admin/editar-usuario.html # editar nombre, correo y ciclo
│       ├── usuario/perfil.html       # "Mi perfil": datos y cambio de ciclo
│       ├── profesor/panel.html
│       ├── modulos/lista.html        # catálogo de ciclos, módulos y sus RA
│       ├── modulos/estudio.html      # temario por ciclo para alumnos y profesores
│       └── notas/lista.html
├── src/test/java/.../seguridad/SeguridadTests.java   # 14 pruebas de seguridad
├── src/test/java/.../ciclo/CicloTests.java           # 6 pruebas de ciclos y del registro
├── src/test/java/.../modulo/ModuloTests.java         # 6 pruebas de los módulos
├── src/test/java/.../ra/RaTests.java                 # 6 pruebas de los RA de cada módulo
├── src/test/java/.../criterio/CriterioTests.java      # 6 pruebas de los criterios de evaluación
├── src/test/java/.../modulo/EstudioTests.java         # 7 pruebas del temario para alumnos y profesores
├── src/test/java/.../admin/AdminUsuariosTests.java   # 9 pruebas de la zona del administrador
├── src/test/java/.../usuario/PerfilTests.java        # 7 pruebas del perfil y su ciclo
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

Al arrancar, Hibernate crea las tablas `notas`, `usuarios`, `ciclos` y `modulos`, y se dan de
alta los cuatro ciclos formativos (DAM, DAW, ASIR y SMR) con sus módulos profesionales.

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
3. Consulta **Ciclos** (`/ciclos`) para ver los módulos de cada ciclo con sus horas.
4. Inicia sesión con el administrador en `/login`.
5. En **Usuarios** (`/admin/usuarios`) verás el ciclo de cada alumno, podrás cambiarlo con el
   desplegable, pulsar **Editar** para cambiar su nombre o su correo, y *Hacer profesor*.
6. Entra con el alumno y abre **Mi perfil** (`/perfil`): ahí cambia él mismo su ciclo.
7. Cierra sesión y entra otra vez con el administrador: el alumno ahora verá la opción
   **Profesor** si lo has promovido.

## Publicar en un servidor (Ubuntu + Nginx)

En `deploy/` están los ficheros para dejar la aplicación corriendo en un servidor Ubuntu
con Nginx delante. La aplicación escucha **solo en `127.0.0.1:8080`** y Nginx es quien
recibe el tráfico de internet (perfil `prod`).

Qué monta `deploy/instalar.sh`:

| Elemento | Dónde queda |
|---|---|
| Java 21, MariaDB, Nginx y los paquetes necesarios | instalados con `apt` |
| Base de datos `telosabes2026` y su usuario | MariaDB (contraseña aleatoria) |
| Aplicación | `/opt/telosabes2026/telosabes2026.jar` |
| Credenciales de la base de datos | `/etc/telosabes2026.env` (permisos `600`, solo root) |
| Servicio systemd (arranca solo al reiniciar) | `/etc/systemd/system/telosabes2026.service` |
| Nginx (puerto 80 → 127.0.0.1:8080) | `/etc/nginx/sites-available/telosabes2026` |
| Cortafuegos (22, 80 y 443) | `ufw` activo |

### Pasos

```bash
# 1. En tu ordenador: compilar el JAR
mvnw.cmd clean package -DskipTests        # Windows
./mvnw clean package -DskipTests          # Linux/macOS

# 2. Copiar el JAR y la carpeta deploy/ al servidor
scp target/telosabes2026-0.0.1-SNAPSHOT.jar root@TU_IP:/root/despliegue/telosabes2026.jar
scp -r deploy/* root@TU_IP:/root/despliegue/

# 3. En el servidor (como root): instalar y publicar
cd /root/despliegue
chmod +x *.sh
DB_PASSWORD='una-clave-larga-y-aleatoria' ./instalar.sh /root/despliegue/telosabes2026.jar

# 4. Crear el administrador (cuando la aplicación ya ha arrancado una vez)
./crear-admin.sh correo@ejemplo.com 'ClaveSecreta1!' "Administrador"

# 5. Acceso SSH: clave para todos y contraseña solo para root
./configurar-ssh.sh
```

El script se puede ejecutar **varias veces**: instala lo que falte, actualiza el JAR y
reinicia el servicio, pero **no toca los datos**.

### Comandos útiles en el servidor

```bash
systemctl status telosabes2026      # estado del servicio
journalctl -u telosabes2026 -f      # log en vivo
systemctl restart telosabes2026     # reiniciar la aplicación
systemctl reload nginx              # recargar la configuración de Nginx
ufw status                          # estado del cortafuegos
```

### Para actualizar la aplicación más adelante

```bash
mvnw.cmd clean package -DskipTests
scp target/telosabes2026-0.0.1-SNAPSHOT.jar root@TU_IP:/root/despliegue/telosabes2026.jar
ssh root@TU_IP "cd /root/despliegue && ./instalar.sh /root/despliegue/telosabes2026.jar"
```

### Copias de seguridad de la base de datos

Los datos reales (usuarios, notas, ciclos) **no están en el repositorio**: viven en MariaDB,
dentro de la VPS. `deploy/instalar.sh` activa un temporizador que hace un volcado diario en
`/var/backups/telosabes2026/` y conserva los últimos 30 días.

```bash
/opt/telosabes2026/copia.sh volcado            # copia manual, ahora mismo
/opt/telosabes2026/copia.sh listar             # ver las copias que hay
/opt/telosabes2026/copia.sh restaurar /var/backups/telosabes2026/telosabes2026-2026-10-20_031500.sql.gz

systemctl list-timers telosabes2026-copia.timer   # cuándo es la próxima copia
```

> ⚠️ Las copias se quedan en la misma VPS: si la VPS muere, se pierden con ella.
> Descárgalas de vez en cuando a tu ordenador (o llévalas a una nube):
>
> ```powershell
> scp root@TU_IP:/var/backups/telosabes2026/*.sql.gz C:\Copia-telosabes\
> ```

### Mudar la aplicación a otra VPS más potente

1. **VPS nueva**: clave SSH, `deploy/` y el JAR, y ejecutar `./instalar.sh` igual que en los
   pasos anteriores. Al no haber cambiado todavía el registro DNS, la web vieja sigue en pie
   y puedes probar la nueva por su IP sin molestar a nadie.
2. **Copiar los datos**: en la vieja `copia.sh volcado`, bajar el `.sql.gz` con `scp`, subirlo
   a la nueva y allí `copia.sh restaurar <fichero>`. Los usuarios y sus contraseñas llegan
   incluidos (viajan cifrados con BCrypt), así que nadie tiene que volver a registrarse.
   Si usas la misma `DB_PASSWORD` en ambas, no hay que tocar nada más.
3. **Bajar el TTL** del registro A (a 300) un par de horas antes.
4. **Cambiar el registro A** a la IP nueva (Hostytec → Zona DNS).
5. **Certificado**: `certbot --nginx -d midominio.com -d www.midominio.com --redirect`,
   y volver a instalar fail2ban y el acceso SSH como en esta VPS (clave para todos, y
   contraseña solo para `root` si se quiere).
6. Dejar la VPS vieja unos días por si acaso y luego cancelarla.

Para los usuarios lo único que se nota es un salto de unos minutos (propagación del DNS)
y el tener que volver a iniciar sesión.

### Notas de seguridad

- El puerto 8080 **no** se abre al exterior: solo lo usa Nginx desde dentro del servidor.
- Los datos de conexión **no** están en el código: van en `/etc/telosabes2026.env`.
- Sin dominio no hay HTTPS: mientras el sitio vaya por `http://`, las contraseñas viajan
  sin cifrar. Cuando se tenga un dominio apuntando a la IP, se puede añadir HTTPS gratis
  con Certbot (`apt install certbot python3-certbot-nginx` y `certbot --nginx`).
- Conviene entrar por **clave SSH**. En esta VPS el acceso por contraseña está permitido
  **solo para `root`**; el resto de usuarios tienen que usar la clave. Se configura en
  `/etc/ssh/sshd_config.d/00-telosabes.conf` (con un bloque `Match User root` al final),
  que se carga antes que el de cloud-init y por eso manda. Para volver a prohibirlo del
  todo, basta con quitar el bloque `Match` y poner `PermitRootLogin prohibit-password`,
  validar con `sshd -t` y recargar con `systemctl reload ssh`.

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
mvnw.cmd test               # 67 pruebas (usan H2, no necesitan MariaDB)
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

Pruebas incluidas en `ModuloTests`:

- los cuatro ciclos tienen sus módulos dados de alta (17, 16, 17 y 15)
- cada ciclo suma 2000 horas
- los módulos transversales están en todos los ciclos
- el módulo de Programación de DAM tiene sus horas y su curso
- los módulos salen ordenados por curso y el proyecto intermodular va el último
- el catálogo `/ciclos` es público y muestra los ciclos con sus módulos

Pruebas incluidas en `AdminUsuariosTests`:

- el administrador cambia el ciclo de un alumno desde el desplegable del listado
- puede dejarlo sin ciclo
- con un ciclo que no existe no se cambia nada
- el administrador edita nombre y correo (el correo se guarda en minúsculas)
- la pantalla de edición carga los datos actuales
- no se puede poner el correo de otro usuario
- con el nombre vacío no se guarda
- la cuenta de administrador no se puede editar (ni por GET ni por POST)
- el listado ofrece el desplegable de ciclos y el enlace de editar

Pruebas incluidas en `PerfilTests`:

- el alumno ve su perfil con sus datos y su ciclo
- cambia su ciclo desde su perfil
- puede quedarse sin ciclo
- con un ciclo que no existe no se cambia nada
- cambiar el ciclo solo afecta al usuario conectado
- sin sesión no se puede entrar al perfil
- el administrador no puede cambiar su ciclo desde el perfil

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
- La carpeta `documentacion/` **no se sube al repositorio** (está en `.gitignore`),
  pero se conserva en el disco local.
