package com.telosabes.telosabes2026.usuario;

/** Se lanza cuando alguien intenta registrarse con un correo ya existente. */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("El correo " + email + " ya esta registrado");
    }
}
