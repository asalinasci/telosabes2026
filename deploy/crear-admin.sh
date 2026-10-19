#!/usr/bin/env bash
#
# Crea (o actualiza) el usuario administrador de telosabes2026 en MariaDB.
#
# El administrador NO se puede crear ni modificar desde la aplicacion: su
# contrasena vive unicamente en la base de datos, siempre cifrada con BCrypt.
#
# Uso (en el servidor, como root):
#     ./crear-admin.sh correo@ejemplo.com 'ClaveSecreta1!' "Administrador"
#
# La tabla "usuarios" la crea la aplicacion la primera vez que arranca,
# asi que este script se ejecuta DESPUES del despliegue.
#
# Si ya existe un usuario con ese correo, le cambia la contrasena y se
# asegura de que tenga el rol ADMIN.

set -euo pipefail

DB_NOMBRE="${DB_NAME:-telosabes2026}"
EMAIL="${1:?Uso: ./crear-admin.sh correo@ejemplo.com 'ClaveSecreta1!' [\"Nombre Apellidos\"]}"
CLAVE="${2:?Falta la contrasena del administrador}"
NOMBRE="${3:-Administrador}"

if [ "${#CLAVE}" -lt 8 ]; then
    echo "La contrasena debe tener al menos 8 caracteres." >&2
    exit 1
fi
if [ "${#CLAVE}" -gt 72 ]; then
    echo "La contrasena no puede superar los 72 caracteres (limite de BCrypt)." >&2
    exit 1
fi

# htpasswd (paquete apache2-utils) genera el hash BCrypt con el mismo
# formato que entiende Spring Security ($2y$).
if ! command -v htpasswd >/dev/null 2>&1; then
    echo "Instalando apache2-utils (htpasswd)..."
    DEBIAN_FRONTEND=noninteractive apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq apache2-utils
fi

HASH="$(htpasswd -bnBC 12 "" "$CLAVE" | cut -d: -f2)"
EMAIL_MINUSCULAS="$(printf '%s' "$EMAIL" | tr '[:upper:]' '[:lower:]')"

mariadb --protocol=socket -uroot "$DB_NOMBRE" <<SQL
INSERT INTO usuarios (nombre, email, clave, rol, activo, fecha_registro)
VALUES ('$NOMBRE', '$EMAIL_MINUSCULAS', '$HASH', 'ADMIN', 1, NOW())
ON DUPLICATE KEY UPDATE
    clave   = '$HASH',
    rol     = 'ADMIN',
    activo  = 1;

SELECT id, nombre, email, rol FROM usuarios WHERE email = '$EMAIL_MINUSCULAS';
SQL

echo
echo "Administrador listo: $EMAIL_MINUSCULAS"
