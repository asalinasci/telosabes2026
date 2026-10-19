package com.telosabes.telosabes2026.usuario;

/** Se lanza cuando el ciclo elegido en el registro no existe. */
public class CicloNoValidoException extends RuntimeException {

    public CicloNoValidoException(Long cicloId) {
        super("No existe el ciclo " + cicloId);
    }
}
