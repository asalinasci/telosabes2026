package com.telosabes.telosabes2026.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el administrador puede cambiar de un usuario.
 *
 * Igual que en el registro, NO hay campo "rol": el rol solo se cambia con la
 * accion de promover a profesor, asi nadie puede enviar rol=ADMIN ni
 * ascenderse a si mismo desde el formulario.
 *
 * El ciclo puede quedarse vacio (el administrador no tiene ciclo).
 */
public class EditarUsuarioForm {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
    private String email;

    /** Id del ciclo: se comprueba contra la tabla "ciclos". Vacio = sin ciclo. */
    private Long cicloId;

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
}
