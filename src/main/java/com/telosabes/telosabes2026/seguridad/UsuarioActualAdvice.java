package com.telosabes.telosabes2026.seguridad;

import com.telosabes.telosabes2026.usuario.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Deja disponible en todas las plantillas la variable "usuarioActual"
 * con la entidad del usuario que tiene la sesion iniciada (o null).
 *
 * Asi las vistas no dependen del tipo de principal de Spring Security.
 */
@ControllerAdvice
public class UsuarioActualAdvice {

    private final UsuarioActual usuarioActual;

    public UsuarioActualAdvice(UsuarioActual usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioConectado(Authentication autenticacion) {
        return usuarioActual.de(autenticacion);
    }
}
