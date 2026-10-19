package com.telosabes.telosabes2026.admin;

import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Zona del administrador.
 *
 * El acceso esta restringido en SecurityConfig (/admin/** solo ROLE_ADMIN)
 * y ademas aqui no aparece nunca el usuario administrador.
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuariosController {

    private final UsuarioService usuarioService;

    public AdminUsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Listado de alumnos y profesores (el administrador no se muestra). */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarSinAdministradores());
        model.addAttribute("totalAlumnos", usuarioService.contarPorRol(Rol.ALUMNO));
        model.addAttribute("totalProfesores", usuarioService.contarPorRol(Rol.PROFESOR));
        return "admin/usuarios";
    }

    /** Convierte un alumno en profesor. */
    @PostMapping("/{id}/promover")
    public String promover(@PathVariable Long id, RedirectAttributes flash) {
        try {
            usuarioService.promoverAProfesor(id);
            flash.addFlashAttribute("mensaje", "El usuario ya es profesor");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
