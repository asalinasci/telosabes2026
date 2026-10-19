package com.telosabes.telosabes2026.seguridad;

/**
 * Niveles de usuario de la aplicacion.
 *
 * El valor de cada constante es el que se guarda en la base de datos
 * (columna usuarios.rol), por eso no se deben cambiar los nombres.
 */
public enum Rol {

    /** Administrador: se crea unicamente por SQL, no existe en la aplicacion. */
    ADMIN("Administrador"),

    /** Profesor: puede ver el listado de alumnos. */
    PROFESOR("Profesor"),

    /** Alumno: rol que se asigna automaticamente al registrarse. */
    ALUMNO("Alumno");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre legible para mostrar en las pantallas. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Nombre que espera Spring Security: ROLE_ADMIN, ROLE_PROFESOR, ROLE_ALUMNO. */
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
