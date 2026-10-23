package com.telosabes.telosabes2026.pregunta;

import com.telosabes.telosabes2026.criterio.Criterio;
import com.telosabes.telosabes2026.criterio.CriterioRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD de preguntas para profesores y administrador.
 *
 * Cuelga de /profesor/**, que SecurityConfig ya restringe a los roles PROFESOR
 * y ADMIN: los alumnos no pueden entrar ni por la lista ni por el formulario.
 *
 * Cada pregunta se asocia a un criterio de evaluación oficial. Como hay miles
 * de criterios, el formulario los agrupa por módulo (con un filtro de texto) en
 * vez de mostrar una lista plana.
 */
@Controller
@RequestMapping("/profesor/preguntas")
public class PreguntaController {

    private static final String VISTA_FORMULARIO = "preguntas/formulario";

    private final PreguntaRepository preguntaRepository;
    private final CriterioRepository criterioRepository;
    private final ModuloRepository moduloRepository;

    public PreguntaController(PreguntaRepository preguntaRepository,
                              CriterioRepository criterioRepository,
                              ModuloRepository moduloRepository) {
        this.preguntaRepository = preguntaRepository;
        this.criterioRepository = criterioRepository;
        this.moduloRepository = moduloRepository;
    }

    /** Listado de todas las preguntas, de la más reciente a la más antigua. */
    @GetMapping
    public String listar(Model model) {
        List<Pregunta> preguntas = preguntaRepository.findAllByOrderByIdDesc();

        model.addAttribute("preguntas", preguntas);
        model.addAttribute("totalPreguntas", preguntas.size());
        model.addAttribute("totalTest", preguntas.stream()
                .filter(p -> p.getTipo() == TipoPregunta.TEST).count());
        model.addAttribute("totalTexto", preguntas.stream()
                .filter(p -> p.getTipo() == TipoPregunta.TEXTO).count());
        return "preguntas/lista";
    }

    /** Formulario de alta. */
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("preguntaForm", new PreguntaForm());
        prepararFormulario(model, null);
        return VISTA_FORMULARIO;
    }

    /** Guarda una pregunta nueva. */
    @PostMapping
    public String guardar(@Valid @ModelAttribute("preguntaForm") PreguntaForm formulario,
                          BindingResult resultado,
                          Model model,
                          RedirectAttributes flash) {
        Criterio criterio = resolverCriterio(formulario.getCriterioId(), resultado);

        if (resultado.hasErrors()) {
            prepararFormulario(model, null);
            return VISTA_FORMULARIO;
        }

        preguntaRepository.save(formulario.aPregunta(criterio));
        flash.addFlashAttribute("mensaje", "Pregunta creada correctamente.");
        return "redirect:/profesor/preguntas";
    }

    /** Formulario de edición de una pregunta existente. */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Pregunta pregunta = buscar(id);

        model.addAttribute("preguntaForm", PreguntaForm.desde(pregunta));
        prepararFormulario(model, id);
        return VISTA_FORMULARIO;
    }

    /** Guarda los cambios de una pregunta existente. */
    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("preguntaForm") PreguntaForm formulario,
                             BindingResult resultado,
                             Model model,
                             RedirectAttributes flash) {
        Pregunta pregunta = buscar(id);
        Criterio criterio = resolverCriterio(formulario.getCriterioId(), resultado);

        if (resultado.hasErrors()) {
            prepararFormulario(model, id);
            return VISTA_FORMULARIO;
        }

        formulario.aplicarA(pregunta, criterio);
        preguntaRepository.save(pregunta);
        flash.addFlashAttribute("mensaje", "Pregunta actualizada correctamente.");
        return "redirect:/profesor/preguntas";
    }

    /** Borra una pregunta. */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        preguntaRepository.delete(buscar(id));
        flash.addFlashAttribute("mensaje", "Pregunta eliminada correctamente.");
        return "redirect:/profesor/preguntas";
    }

    /** La pregunta pedida o un 404 si no existe. */
    private Pregunta buscar(Long id) {
        return preguntaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la pregunta " + id));
    }

    /**
     * Comprueba que el criterio enviado existe. Si el id llega vacío, la
     * validación del formulario ya lo marca como obligatorio.
     */
    private Criterio resolverCriterio(Long criterioId, BindingResult resultado) {
        if (criterioId == null) {
            return null;
        }
        Criterio criterio = criterioRepository.findById(criterioId).orElse(null);
        if (criterio == null) {
            resultado.rejectValue("criterioId", "noexiste", "Ese criterio de evaluacion no existe");
        }
        return criterio;
    }

    /**
     * Datos comunes a los formularios de alta y edición: tipos disponibles,
     * criterios agrupados por módulo e identificador de la pregunta (null si es
     * un alta).
     */
    private void prepararFormulario(Model model, Long preguntaId) {
        model.addAttribute("tipos", TipoPregunta.values());
        model.addAttribute("criteriosPorModulo", criteriosPorModulo());
        model.addAttribute("preguntaId", preguntaId);
    }

    /**
     * Criterios agrupados por módulo y en el orden oficial: ciclo, curso, orden
     * del módulo y, dentro de cada módulo, RA y criterio.
     */
    private Map<Modulo, List<Criterio>> criteriosPorModulo() {
        Map<Long, List<Criterio>> porModuloId = new HashMap<>();
        for (Criterio criterio : criterioRepository.findAllByOrderByRaIdAscOrdenAsc()) {
            porModuloId.computeIfAbsent(criterio.getRa().getModulo().getId(),
                    clave -> new ArrayList<>()).add(criterio);
        }

        Map<Modulo, List<Criterio>> porModulo = new LinkedHashMap<>();
        for (Modulo modulo : moduloRepository.findAllByOrderByCicloSiglasAscCursoAscOrdenAsc()) {
            List<Criterio> criterios = porModuloId.get(modulo.getId());
            if (criterios != null && !criterios.isEmpty()) {
                porModulo.put(modulo, criterios);
            }
        }
        return porModulo;
    }
}
