package com.telosabes.telosabes2026.nota;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notas")
public class NotaController {

    private final NotaRepository notaRepository;

    public NotaController(NotaRepository notaRepository) {
        this.notaRepository = notaRepository;
    }

    /** Lista todas las notas y muestra el formulario de alta. */
    @GetMapping
    public String listar(Model model) {
        if (!model.containsAttribute("nota")) {
            model.addAttribute("nota", new Nota());
        }
        model.addAttribute("notas", notaRepository.findAllByOrderByCreadoEnDesc());
        return "notas/lista";
    }

    /** Guarda una nota nueva. */
    @PostMapping
    public String guardar(@Valid Nota nota, BindingResult resultado,
                          RedirectAttributes flash, Model model) {
        if (resultado.hasErrors()) {
            model.addAttribute("notas", notaRepository.findAllByOrderByCreadoEnDesc());
            return "notas/lista";
        }
        notaRepository.save(nota);
        flash.addFlashAttribute("mensaje", "Nota guardada correctamente");
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
