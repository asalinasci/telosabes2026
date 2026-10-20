package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.ra.RA;
import com.telosabes.telosabes2026.ra.RaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo publico de ciclos y modulos.
 *
 * Muestra cada ciclo formativo con sus modulos profesionales repartidos por
 * curso, con los resultados de aprendizaje oficiales de cada modulo. Es
 * informativo, asi que no hace falta tener la sesion iniciada (ver
 * SecurityConfig).
 */
@Controller
public class ModuloController {

    private final CicloRepository cicloRepository;
    private final ModuloRepository moduloRepository;
    private final RaRepository raRepository;

    public ModuloController(CicloRepository cicloRepository, ModuloRepository moduloRepository,
                            RaRepository raRepository) {
        this.cicloRepository = cicloRepository;
        this.moduloRepository = moduloRepository;
        this.raRepository = raRepository;
    }

    /** Listado de ciclos con sus modulos, agrupados por ciclo. */
    @GetMapping("/ciclos")
    public String listado(Model model) {
        List<Ciclo> ciclos = cicloRepository.findAllByOrderBySiglasAsc();

        Map<Long, List<Modulo>> modulosPorCiclo = new LinkedHashMap<>();
        for (Modulo modulo : moduloRepository.findAllByOrderByCicloSiglasAscCursoAscOrdenAsc()) {
            modulosPorCiclo.computeIfAbsent(modulo.getCiclo().getId(), clave -> new ArrayList<>())
                    .add(modulo);
        }

        // Resultados de aprendizaje agrupados por modulo (orden de la tabla oficial)
        Map<Long, List<RA>> raPorModulo = new LinkedHashMap<>();
        for (RA ra : raRepository.findAllByOrderByModuloIdAscOrdenAsc()) {
            raPorModulo.computeIfAbsent(ra.getModulo().getId(), clave -> new ArrayList<>())
                    .add(ra);
        }

        model.addAttribute("ciclos", ciclos);
        model.addAttribute("modulosPorCiclo", modulosPorCiclo);
        model.addAttribute("raPorModulo", raPorModulo);
        return "modulos/lista";
    }
}
