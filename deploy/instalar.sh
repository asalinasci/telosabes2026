#!/usr/bin/env bash
#
# Publica telosabes2026 en un servidor Ubuntu con Nginx.
#
# Uso (en el servidor, como root):
#     DB_PASSWORD='la-clave-de-la-base-de-datos' ./instalar.sh /tmp/telosabes2026.jar
#
# Se puede ejecutar varias veces: instala lo que falte y actualiza la
# aplicacion, pero NO borra la base de datos.

set -euo pipefail

APP_NOMBRE="telosabes2026"
APP_USUARIO="telosabes2026"
APP_DIR="/opt/telosabes2026"
JAR_DESTINO="$APP_DIR/$APP_NOMBRE.jar"
ENV_FILE="/etc/telosabes2026.env"
SERVICIO="telosabes2026"
DB_NOMBRE="telosabes2026"
DB_USUARIO="telosabes2026"
PUERTO_APP="8080"

JAR_ORIGEN="${1:?Uso: ./instalar.sh /ruta/al/telosabes2026.jar}"
DB_PASSWORD="${DB_PASSWORD:?Falta la variable DB_PASSWORD}"
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

titulo() { echo; echo "=== $* ==="; }

if [ "$(id -u)" -ne 0 ]; then
    echo "Ejecuta este script como root." >&2
    exit 1
fi

export DEBIAN_FRONTEND=noninteractive

# ------------------------------------------------------------------
titulo "1/7 Paquetes del sistema"
# ------------------------------------------------------------------
apt-get update -qq

if ! command -v java >/dev/null 2>&1; then
    for paquete in openjdk-21-jre-headless openjdk-25-jre-headless default-jre-headless; do
        if apt-get install -y -qq "$paquete" >/dev/null 2>&1; then
            echo "Java instalado con el paquete: $paquete"
            break
        fi
    done
fi

if ! command -v java >/dev/null 2>&1; then
    echo "No he podido instalar Java." >&2
    exit 1
fi
java -version 2>&1 | sed -n 1p

apt-get install -y -qq mariadb-server nginx curl ufw apache2-utils >/dev/null

systemctl enable --now mariadb
systemctl enable --now nginx
echo "MariaDB: $(mariadb --version)"

# ------------------------------------------------------------------
titulo "2/7 Base de datos y su usuario"
# ------------------------------------------------------------------
# En Ubuntu, root de MariaDB entra por el socket (sin contrasena).
mariadb --protocol=socket -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`$DB_NOMBRE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USUARIO'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USUARIO'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USUARIO'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USUARIO'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NOMBRE\`.* TO '$DB_USUARIO'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NOMBRE\`.* TO '$DB_USUARIO'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
echo "Base de datos '$DB_NOMBRE' y usuario '$DB_USUARIO' listos."

# ------------------------------------------------------------------
titulo "3/7 Usuario y carpeta de la aplicacion"
# ------------------------------------------------------------------
if ! id "$APP_USUARIO" >/dev/null 2>&1; then
    useradd --system --home-dir "$APP_DIR" --shell /usr/sbin/nologin "$APP_USUARIO"
    echo "Usuario del sistema creado: $APP_USUARIO"
fi
mkdir -p "$APP_DIR"

# ------------------------------------------------------------------
titulo "4/7 Ficheros de la aplicacion"
# ------------------------------------------------------------------
install -o "$APP_USUARIO" -g "$APP_USUARIO" -m 0644 "$JAR_ORIGEN" "$JAR_DESTINO"
echo "Aplicacion copiada en $JAR_DESTINO"

# DB_USER y DB_PASSWORD fuera del codigo: solo las lee root
printf 'DB_USER=%s\nDB_PASSWORD=%s\n' "$DB_USUARIO" "$DB_PASSWORD" > "$ENV_FILE"
chown root:root "$ENV_FILE"
chmod 600 "$ENV_FILE"
echo "Credenciales guardadas en $ENV_FILE (permisos 600)"

# ------------------------------------------------------------------
titulo "5/7 Servicio systemd"
# ------------------------------------------------------------------
install -o root -g root -m 0644 "$AQUI/telosabes2026.service" "/etc/systemd/system/$SERVICIO.service"
systemctl daemon-reload
systemctl enable "$SERVICIO" >/dev/null 2>&1
systemctl restart "$SERVICIO"
echo "Servicio $SERVICIO activado y arrancado."

# ------------------------------------------------------------------
titulo "6/7 Nginx como reverse proxy"
# ------------------------------------------------------------------
install -o root -g root -m 0644 "$AQUI/nginx-telosabes2026.conf" "/etc/nginx/sites-available/$APP_NOMBRE"
ln -sfn "/etc/nginx/sites-available/$APP_NOMBRE" "/etc/nginx/sites-enabled/$APP_NOMBRE"
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
echo "Nginx recargado (puerto 80 -> 127.0.0.1:$PUERTO_APP)."

# ------------------------------------------------------------------
titulo "7/7 Cortafuegos"
# ------------------------------------------------------------------
ufw allow OpenSSH >/dev/null
ufw allow 80/tcp >/dev/null
ufw allow 443/tcp >/dev/null
ufw --force enable >/dev/null
ufw status | sed -n '1,15p'

# ------------------------------------------------------------------
titulo "Comprobacion final"
# ------------------------------------------------------------------
for intento in $(seq 1 45); do
    if curl -sf -o /dev/null "http://127.0.0.1:$PUERTO_APP/login"; then
        echo "La aplicacion responde en 127.0.0.1:$PUERTO_APP"
        break
    fi
    if [ "$intento" -eq 45 ]; then
        echo "La aplicacion NO responde. Ultimas lineas del log:" >&2
        journalctl -u "$SERVICIO" -n 40 --no-pager >&2
        exit 1
    fi
    sleep 2
done

if curl -sf -o /dev/null -H "Host: localhost" "http://127.0.0.1/"; then
    echo "Nginx responde en el puerto 80"
fi

echo
echo "Aplicacion publicada."
echo "  Anade el administrador con: $AQUI/crear-admin.sh correo@ejemplo.com 'ClaveSecreta1!'"
echo "  Estado del servicio:        systemctl status $SERVICIO"
echo "  Log en vivo:                journalctl -u $SERVICIO -f"
