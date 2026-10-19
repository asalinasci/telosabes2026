package com.telosabes.telosabes2026.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de registro de alumnos.
 *
 * Fijate en que NO hay campo "rol": el rol se asigna siempre en el
 * servicio (ALUMNO), asi nadie puede registrarse como profesor o
 * administrador manipulando el formulario.
 */
public class RegistroForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
    private String email;

    /** Id del ciclo elegido: se comprueba contra la tabla "ciclos". */
    @NotNull(message = "Elige el ciclo que vas a estudiar")
    private Long cicloId;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String clave;

    @NotBlank(message = "Repite la contraseña")
    private String claveRepetida;

    public boolean clavesCoinciden() {
        return clave != null && clave.equals(claveRepetida);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCicloId() {
        return cicloId;
    }

    public void setCicloId(Long cicloId) {
        this.cicloId = cicloId;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getClaveRepetida() {
        return claveRepetida;
    }

    public void setClaveRepetida(String claveRepetida) {
        this.claveRepetida = claveRepetida;
    }
}
