package com.telosabes.telosabes2026.usuario;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Registro publico de alumnos: cualquiera puede darse de alta,
 * pero siempre con el rol ALUMNO y eligiendo su ciclo.
 */
@Controller
public class RegistroController {

    private final UsuarioService usuarioService;
    private final CicloRepository cicloRepository;

    public RegistroController(UsuarioService usuarioService, CicloRepository cicloRepository) {
        this.usuarioService = usuarioService;
        this.cicloRepository = cicloRepository;
    }

    /** Formulario vacio para la pantalla de registro. */
    @ModelAttribute("registroForm")
    public RegistroForm formularioVacio() {
        return new RegistroForm();
    }

    /** Ciclos disponibles para el desplegable (tambien al volver con errores). */
    @ModelAttribute("ciclos")
    public List<Ciclo> ciclosDisponibles() {
        return cicloRepository.findAllByOrderBySiglasAsc();
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
        } catch (CicloNoValidoException e) {
            resultado.rejectValue("cicloId", "cicloNoValido", "Ese ciclo no existe");
            return "registro";
        }

        flash.addFlashAttribute("registrado", true);
        return "redirect:/login?registrado";
    }
}
