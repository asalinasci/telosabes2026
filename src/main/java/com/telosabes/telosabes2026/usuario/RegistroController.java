package com.telosabes.telosabes2026.usuario;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Registro publico de alumnos: cualquiera puede darse de alta,
 * pero siempre con el rol ALUMNO.
 */
@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Formulario vacio para la pantalla de registro. */
    @ModelAttribute("registroForm")
    public RegistroForm formularioVacio() {
        return new RegistroForm();
    }

    @GetMapping("/registro")
    public String formulario() {
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroForm") RegistroForm formulario,
                            BindingResult resultado,
                            RedirectAttributes flash) {

        if (!formulario.clavesCoinciden()) {
            resultado.rejectValue("claveRepetida", "claveNoCoincide", "Las contraseñas no coinciden");
            return "registro";
        }

        if (resultado.hasErrors()) {
            return "registro";
        }

        try {
            usuarioService.registrarAlumno(formulario);
        } catch (EmailYaRegistradoException e) {
            resultado.rejectValue("email", "emailDuplicado", "Ese correo ya esta registrado");
            return "registro";
        }

        flash.addFlashAttribute("registrado", true);
        return "redirect:/login?registrado";
    }
}
