package com.telosabes.telosabes2026.seguridad;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Paginas de login y de acceso denegado.
 */
@Controller
public class LoginController {

    /** Formulario de inicio de sesion (publico). */
    @GetMapping("/login")
    public String login(Authentication autenticacion) {
        // Si ya hay una sesion iniciada no tiene sentido volver al login
        if (autenticacion != null && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "login";
    }

    /** Se muestra cuando un usuario intenta entrar en una zona sin permiso. */
    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "error/403";
    }
}
