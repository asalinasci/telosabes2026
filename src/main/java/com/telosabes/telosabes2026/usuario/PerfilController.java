package com.telosabes.telosabes2026.usuario;

import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.seguridad.UsuarioActual;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Perfil del usuario conectado.
 *
 * Aqui cada alumno ve sus datos y puede cambiar el ciclo que estudia sin
 * depender del administrador. El usuario NO llega nunca desde el formulario:
 * se toma siempre de la sesion, asi nadie puede cambiar el ciclo de otro.
 */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final CicloRepository cicloRepository;
    private final UsuarioActual usuarioActual;

    public PerfilController(UsuarioService usuarioService,
                            CicloRepository cicloRepository,
                            UsuarioActual usuarioActual) {
        this.usuarioService = usuarioService;
        this.cicloRepository = cicloRepository;
        this.usuarioActual = usuarioActual;
    }

    /** Datos del usuario conectado y formulario para cambiar de ciclo. */
    @GetMapping
    public String ver(Authentication autenticacion, Model model) {
        // Lo recargamos de la base de datos: el principal de la sesion se creo
        // al iniciar sesion y todavia tendria el ciclo antiguo.
        Usuario usuario = usuarioService.buscarPorId(usuarioActual.requerido(autenticacion).getId());

        model.addAttribute("usuario", usuario);
        model.addAttribute("ciclos", cicloRepository.findAllByOrderBySiglasAsc());
        // El administrador no se toca desde la aplicacion
        model.addAttribute("puedeCambiarCiclo", !usuario.esAdministrador());
        return "usuario/perfil";
    }

    /** Cambia el ciclo del usuario conectado (nunca el de otro). */
    @PostMapping("/ciclo")
    public String cambiarCiclo(@RequestParam(name = "cicloId", required = false) Long cicloId,
                               Authentication autenticacion,
                               RedirectAttributes flash) {
        Long id = usuarioActual.requerido(autenticacion).getId();

        try {
            usuarioService.cambiarCiclo(id, cicloId);
            flash.addFlashAttribute("mensaje", "Tu ciclo se ha actualizado");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/perfil";
    }
}
