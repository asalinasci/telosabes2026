#!/usr/bin/env bash
#
# Copias de seguridad de la base de datos de telosabes2026.
#
#   ./copia.sh volcado                        volcado ahora (a /var/backups/telosabes2026)
#   ./copia.sh listar                         ver las copias que hay
#   ./copia.sh restaurar <fichero.sql.gz>     restaura una copia (pide confirmacion)
#
# El temporizador telosabes2026-copia.timer ejecuta "volcado" una vez al dia
# y conserva los ultimos 30 dias.

set -euo pipefail

DB="telosabes2026"
CARPETA="/var/backups/telosabes2026"
RETENCION_DIAS=30

if [ "$(id -u)" -ne 0 ]; then
    echo "Este script tiene que ejecutarse como root." >&2
    exit 1
fi

# En MariaDB actual los programas se llaman mariadb-dump / mariadb, pero
# tambien existen como mysqldump / mysql. Nos quedamos con el que haya.
DUMP="mariadb-dump"
CLIENTE="mariadb"
command -v "$DUMP" >/dev/null 2>&1 || DUMP="mysqldump"
command -v "$CLIENTE" >/dev/null 2>&1 || CLIENTE="mysql"

mkdir -p "$CARPETA"
chmod 700 "$CARPETA"

volcado() {
    local fichero="$CARPETA/telosabes2026-$(date +%Y-%m-%d_%H%M%S).sql.gz"

    echo "Volcando la base de datos '$DB'..."
    "$DUMP" --protocol=socket -uroot --single-transaction --quick \
        --triggers --routines --events "$DB" | gzip > "$fichero"
    chmod 600 "$fichero"

    echo "Copia creada: $fichero ($(du -h "$fichero" | cut -f1))"

    # Borra las copias de mas de N dias
    find "$CARPETA" -name 'telosabes2026-*.sql.gz' -mtime "+$RETENCION_DIAS" -delete

    echo "Copias disponibles (se conservan las ultimas $RETENCION_DIAS dias):"
    ls -lh "$CARPETA" | sed -n '2,12p'
}

listar() {
    ls -lh "$CARPETA"
}

restaurar() {
    local fichero="${1:?Uso: ./copia.sh restaurar /var/backups/telosabes2026/telosabes2026-2026-10-20_030000.sql.gz}"

    if [ ! -f "$fichero" ]; then
        echo "No existe el fichero: $fichero" >&2
        exit 1
    fi

    echo "ATENCION: esto SUSTITUYE los datos actuales de la base de datos"
    echo "por los guardados en:"
    echo "    $fichero"
    printf "Escribe SI para continuar: "
    read -r respuesta

    if [ "$respuesta" != "SI" ]; then
        echo "Cancelado. No se ha tocado nada."
        exit 1
    fi

    echo "Restaurando..."
    gunzip -c "$fichero" | "$CLIENTE" --protocol=socket -uroot "$DB"
    echo "Restauracion completada."
}

case "${1:-}" in
    volcado)   volcado ;;
    listar)    listar ;;
    restaurar) restaurar "${2:-}" ;;
    *)
        sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
        exit 2
        ;;
esac
