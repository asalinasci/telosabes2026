package com.telosabes.telosabes2026.seguridad;

import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }

        if (autenticacion.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getUsuario();
        }

        // Por si la autenticacion no la hemos creado nosotros (tests, remember-me...)
        return usuarioRepository.findByEmailIgnoreCase(autenticacion.getName()).orElse(null);
    }
}
