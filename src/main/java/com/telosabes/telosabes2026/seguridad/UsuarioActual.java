package com.telosabes.telosabes2026.seguridad;

import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Averigua que usuario tiene la sesion iniciada.
 *
 * Se usa desde los controladores (por ejemplo, para guardar el autor de
 * una nota) y tambien desde {@link UsuarioActualAdvice} para dejar la
 * variable "usuarioActual" disponible en todas las plantillas.
 */
@Component
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActual(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devuelve el usuario conectado, o null si la peticion es anonima.
     */
    public Usuario de(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }

        // Lo normal: el login lo hizo nuestro UsuarioDetailsService
        if (autenticacion.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getUsuario();
        }

        // Por si la autenticacion no la hemos creado nosotros (tests, remember-me...)
        return usuarioRepository.findByEmailIgnoreCase(autenticacion.getName()).orElse(null);
    }

    /**
     * Igual que {@link #de(Authentication)} pero exige que exista usuario.
     * Se usa en acciones que necesitan saber quien las hace.
     *
     * @throws IllegalStateException si no hay nadie con la sesion iniciada
     */
    public Usuario requerido(Authentication autenticacion) {
        Usuario usuario = de(autenticacion);
        if (usuario == null) {
            throw new IllegalStateException("No hay ningun usuario con la sesion iniciada");
        }
        return usuario;
    }
}
