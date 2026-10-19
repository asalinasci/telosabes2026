package com.telosabes.telosabes2026.nota;

import com.telosabes.telosabes2026.seguridad.UsuarioActual;
import com.telosabes.telosabes2026.usuario.Usuario;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notas")
public class NotaController {

    private final NotaRepository notaRepository;
    private final UsuarioActual usuarioActual;

    public NotaController(NotaRepository notaRepository, UsuarioActual usuarioActual) {
        this.notaRepository = notaRepository;
        this.usuarioActual = usuarioActual;
    }

    /** Formulario vacio para la pantalla de notas. */
    @ModelAttribute("notaForm")
    public NotaForm formularioVacio() {
        return new NotaForm();
    }

    /** Lista todas las notas y muestra el formulario de alta. */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("notas", notaRepository.findAllByOrderByCreadoEnDesc());
        return "notas/lista";
    }

    /**
     * Guarda una nota nueva.
     *
     * El autor NO viene del formulario: se toma del usuario que tiene la
     * sesion iniciada en este momento.
     */
    @PostMapping
    public String guardar(@Valid NotaForm formulario, BindingResult resultado,
                          Authentication autenticacion,
                          RedirectAttributes flash, Model model) {
        if (resultado.hasErrors()) {
            model.addAttribute("notas", notaRepository.findAllByOrderByCreadoEnDesc());
            return "notas/lista";
        }

        Usuario autor = usuarioActual.requerido(autenticacion);
        notaRepository.save(new Nota(formulario.getTitulo(), formulario.getContenido(), autor));

        flash.addFlashAttribute("mensaje",
                "Nota guardada correctamente como \"" + autor.getNombre() + "\"");
        return "redirect:/notas";
    }

    /** Elimina una nota por su id. */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        notaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("aviso", "Nota " + id + " eliminada");
        return "redirect:/notas";
    }
}
