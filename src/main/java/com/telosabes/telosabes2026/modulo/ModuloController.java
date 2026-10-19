package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
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
 * curso. Es informativo, asi que no hace falta tener la sesion iniciada
 * (ver SecurityConfig).
 */
@Controller
public class ModuloController {

    private final CicloRepository cicloRepository;
    private final ModuloRepository moduloRepository;

    public ModuloController(CicloRepository cicloRepository, ModuloRepository moduloRepository) {
        this.cicloRepository = cicloRepository;
        this.moduloRepository = moduloRepository;
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

        model.addAttribute("ciclos", ciclos);
        model.addAttribute("modulosPorCiclo", modulosPorCiclo);
        return "modulos/lista";
    }
}
