package com.telosabes.telosabes2026.profesor;

import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Zona del profesorado.
 * El acceso esta restringido en SecurityConfig (/profesor/** solo
 * ROLE_PROFESOR y ROLE_ADMIN).
 */
@Controller
@RequestMapping("/profesor")
public class ProfesorController {

    private final UsuarioService usuarioService;

    public ProfesorController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Panel del profesor con el listado de alumnos. */
    @GetMapping
    public String panel(Model model) {
        model.addAttribute("alumnos", usuarioService.listarAlumnos());
        model.addAttribute("totalAlumnos", usuarioService.contarPorRol(Rol.ALUMNO));
        return "profesor/panel";
    }
}
