#!/usr/bin/env bash
#
# Configura el acceso SSH de telosabes2026 a partir de
# deploy/ssh-00-telosabes.conf.
#
# Uso (en el servidor, como root):
#     ./configurar-ssh.sh
#
# Deja la clave SSH para todos y la contrasena solo para root. Se puede
# ejecutar varias veces: guarda una copia de la configuracion anterior, valida
# con "sshd -t" y, si algo no cuadra, la restaura.

set -euo pipefail

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORIGEN="$AQUI/ssh-00-telosabes.conf"
DESTINO="/etc/ssh/sshd_config.d/00-telosabes.conf"

titulo() { echo; echo "=== $* ==="; }

if [ "$(id -u)" -ne 0 ]; then
    echo "Hay que ejecutarlo como root." >&2
    exit 1
fi

if [ ! -f "$ORIGEN" ]; then
    echo "No encuentro $ORIGEN" >&2
    exit 1
fi

titulo "1/3 Copia de la configuracion actual"
if [ -f "$DESTINO" ]; then
    COPIA="$DESTINO.bak.$(date +%Y%m%d%H%M%S)"
    cp -p "$DESTINO" "$COPIA"
    echo "Guardada en $COPIA"
else
    echo "Todavia no habia configuracion propia."
fi

titulo "2/3 Nueva configuracion"
install -m 644 "$ORIGEN" "$DESTINO"
cat "$DESTINO"

titulo "3/3 Validacion y recarga"
if ! sshd -t; then
    echo "La configuracion NO es valida. Restauro la copia de seguridad." >&2
    if [ -n "${COPIA:-}" ]; then
        cp -p "$COPIA" "$DESTINO"
    else
        rm -f "$DESTINO"
    fi
    exit 1
fi
systemctl reload ssh 2>/dev/null || systemctl reload sshd
echo "sshd recargado."

echo
echo "Valores en vigor (sin el bloque Match):"
sshd -T | grep -Ei '^(passwordauthentication|permitrootlogin|pubkeyauthentication|kbdinteractiveauthentication)'
echo
echo "root entra con contrasena; el resto de usuarios, solo con su clave SSH."
