package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.criterio.Criterio;
import com.telosabes.telosabes2026.criterio.CriterioRepository;
import com.telosabes.telosabes2026.ra.RA;
import com.telosabes.telosabes2026.ra.RaRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.seguridad.UsuarioActual;
import com.telosabes.telosabes2026.usuario.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta comoda de los resultados de aprendizaje y los criterios de
 * evaluacion de cada modulo.
 *
 * Es la pantalla pensada para alumnos y profesores: se elige el ciclo y se ven
 * sus modulos por curso, con los RA desplegables y los criterios de evaluacion
 * dentro de cada RA. El alumno empieza viendo el ciclo que estudia y puede
 * cambiar de ciclo con el desplegable.
 *
 * Necesita sesion iniciada (SecurityConfig deja el resto de rutas a cualquier
 * usuario autenticado), asi que entran alumnos, profesores y administrador.
 */
@Controller
@RequestMapping("/modulos")
public class EstudioController {

    private final CicloRepository cicloRepository;
    private final ModuloRepository moduloRepository;
    private final RaRepository raRepository;
    private final CriterioRepository criterioRepository;
    private final UsuarioActual usuarioActual;

    public EstudioController(CicloRepository cicloRepository,
                             ModuloRepository moduloRepository,
                             RaRepository raRepository,
                             CriterioRepository criterioRepository,
                             UsuarioActual usuarioActual) {
        this.cicloRepository = cicloRepository;
        this.moduloRepository = moduloRepository;
        this.raRepository = raRepository;
        this.criterioRepository = criterioRepository;
        this.usuarioActual = usuarioActual;
    }

    /** Modulos del ciclo elegido con sus RA y los criterios de cada RA. */
    @GetMapping("/estudio")
    public String estudio(@RequestParam(name = "cicloId", required = false) Long cicloId,
                          Authentication autenticacion,
                          Model model) {
        List<Ciclo> ciclos = cicloRepository.findAllByOrderBySiglasAsc();
        Usuario usuario = usuarioActual.de(autenticacion);

        Ciclo ciclo = elegirCiclo(cicloId, usuario, ciclos);

        // Modulos del ciclo, separados por curso (1º y 2º)
        Map<Integer, List<Modulo>> modulosPorCurso = new LinkedHashMap<>();
        Map<Long, List<RA>> raPorModulo = new LinkedHashMap<>();
        Map<Long, List<Criterio>> criteriosPorRa = new LinkedHashMap<>();

        if (ciclo != null) {
            for (Modulo modulo : moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo.getId())) {
                modulosPorCurso.computeIfAbsent(modulo.getCurso(), clave -> new ArrayList<>())
                        .add(modulo);

                List<RA> ras = raRepository.findByModuloIdOrderByOrdenAsc(modulo.getId());
                if (!ras.isEmpty()) {
                    raPorModulo.put(modulo.getId(), ras);
                }
                for (RA ra : ras) {
                    List<Criterio> criterios = criterioRepository.findByRaIdOrderByOrdenAsc(ra.getId());
                    if (!criterios.isEmpty()) {
                        criteriosPorRa.put(ra.getId(), criterios);
                    }
                }
            }
        }

        model.addAttribute("ciclos", ciclos);
        model.addAttribute("ciclo", ciclo);
        model.addAttribute("modulosPorCurso", modulosPorCurso);
        model.addAttribute("raPorModulo", raPorModulo);
        model.addAttribute("criteriosPorRa", criteriosPorRa);
        model.addAttribute("totalRa", raPorModulo.values().stream().mapToInt(List::size).sum());
        model.addAttribute("puedeElegirCiclo", usuario == null || usuario.getRol() != Rol.ALUMNO);
        return "modulos/estudio";
    }

    /**
     * Ciclo que se muestra: el pedido con el desplegable, o el que estudia el
     * alumno, o el primero de la lista (el administrador y los profesores no
     * tienen ciclo propio).
     */
    private Ciclo elegirCiclo(Long cicloId, Usuario usuario, List<Ciclo> ciclos) {
        if (cicloId != null) {
            for (Ciclo ciclo : ciclos) {
                if (ciclo.getId().equals(cicloId)) {
                    return ciclo;
                }
            }
        }
        if (usuario != null && usuario.getCiclo() != null) {
            for (Ciclo ciclo : ciclos) {
                if (ciclo.getId().equals(usuario.getCiclo().getId())) {
                    return ciclo;
                }
            }
        }
        return ciclos.isEmpty() ? null : ciclos.get(0);
    }
}
