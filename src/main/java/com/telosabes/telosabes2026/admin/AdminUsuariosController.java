package com.telosabes.telosabes2026.admin;

import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.CicloNoValidoException;
import com.telosabes.telosabes2026.usuario.EditarUsuarioForm;
import com.telosabes.telosabes2026.usuario.EmailYaRegistradoException;
import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final CicloRepository cicloRepository;

    public AdminUsuariosController(UsuarioService usuarioService, CicloRepository cicloRepository) {
        this.usuarioService = usuarioService;
        this.cicloRepository = cicloRepository;
    }

    /** Listado de alumnos y profesores (el administrador no se muestra). */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarSinAdministradores());
        model.addAttribute("ciclos", cicloRepository.findAllByOrderBySiglasAsc());
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

    /** Formulario para cambiar el ciclo de un usuario desde el listado. */
    @PostMapping("/{id}/ciclo")
    public String cambiarCiclo(@PathVariable Long id,
                               @RequestParam(name = "cicloId", required = false) Long cicloId,
                               RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioService.cambiarCiclo(id, cicloId);
            flash.addFlashAttribute("mensaje", usuario.getNombre() + ": ciclo actualizado");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    /** Pantalla para editar los datos de un usuario (nombre, correo y ciclo). */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("usuario", usuarioService.buscarEditable(id));
            model.addAttribute("editarUsuarioForm", formularioDe(id));
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/usuarios";
        }
        model.addAttribute("ciclos", cicloRepository.findAllByOrderBySiglasAsc());
        return "admin/editar-usuario";
    }

    /** Guarda los datos del formulario de edicion. */
    @PostMapping("/{id}/editar")
    public String guardar(@PathVariable Long id,
                          @Valid @ModelAttribute("editarUsuarioForm") EditarUsuarioForm formulario,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            return volverAlFormulario(id, model, flash);
        }

        try {
            Usuario usuario = usuarioService.actualizarDatos(id, formulario);
            flash.addFlashAttribute("mensaje", "Datos de " + usuario.getNombre() + " actualizados");
            return "redirect:/admin/usuarios";
        } catch (EmailYaRegistradoException e) {
            resultado.rejectValue("email", "email.duplicado", e.getMessage());
        } catch (CicloNoValidoException | IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/usuarios";
        }

        return volverAlFormulario(id, model, flash);
    }

    /** Vuelve a pintar el formulario de edicion con el usuario y los ciclos. */
    private String volverAlFormulario(Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("usuario", usuarioService.buscarEditable(id));
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/usuarios";
        }
        model.addAttribute("ciclos", cicloRepository.findAllByOrderBySiglasAsc());
        return "admin/editar-usuario";
    }

    /** Rellena el formulario con los datos actuales del usuario. */
    private EditarUsuarioForm formularioDe(Long id) {
        Usuario usuario = usuarioService.buscarEditable(id);

        EditarUsuarioForm formulario = new EditarUsuarioForm();
        formulario.setNombre(usuario.getNombre());
        formulario.setEmail(usuario.getEmail());
        formulario.setCicloId(usuario.getCiclo() == null ? null : usuario.getCiclo().getId());
        return formulario;
    }
}
