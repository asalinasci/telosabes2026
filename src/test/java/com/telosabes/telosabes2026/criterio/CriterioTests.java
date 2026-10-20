package com.telosabes.telosabes2026.criterio;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
import com.telosabes.telosabes2026.ra.RA;
import com.telosabes.telosabes2026.ra.RaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de los criterios de evaluación oficiales de cada resultado de
 * aprendizaje. Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CriterioTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CriterioRepository criterioRepository;

    @Autowired
    private RaRepository raRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private CriteriosIniciales criteriosIniciales;

    @Test
    void todosLosResultadosDeAprendizajeTienenSusCriterios() {
        List<RA> ras = raRepository.findAllByOrderByIdAsc();

        // 373 RA en los cuatro ciclos y ningún RA oficial se queda sin criterios
        assertThat(ras).hasSize(373);

        long sinCriterios = 0;
        for (RA ra : ras) {
            if (criterioRepository.countByRaId(ra.getId()) == 0) {
                sinCriterios++;
            }
        }
        assertThat(sinCriterios).isZero();

        // 1722 en DAM y 578 en SMR: el sembrador los reparte por ciclo
        assertThat(criterioRepository.count()).isEqualTo(2912);
    }

    @Test
    void losCriteriosLlevanLasLetrasDeLaTablaOficial() {
        List<Criterio> criterios = criteriosDe("DAM", "0485", 1);

        // El RA1 de Programación tiene nueve criterios: de la a) a la i)
        assertThat(criterios).hasSize(9);
        assertThat(criterios).extracting(Criterio::getOrden)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(criterios).extracting(Criterio::getLetra)
                .containsExactly("a)", "b)", "c)", "d)", "e)", "f)", "g)", "h)", "i)");
        assertThat(criterios.get(0).getDescripcion())
                .isEqualTo("Se han identificado los bloques que componen la estructura de un "
                        + "programa informático.");
        assertThat(criterios.get(8).getDescripcion())
                .isEqualTo("Se han introducido comentarios en el código.");
    }

    @Test
    void cadaResultadoTieneAlMenosTresCriteriosYConLetrasContiguas() {
        for (Criterio criterio : criterioRepository.findAllByOrderByIdAsc()) {
            assertThat(criterio.getRa()).isNotNull();
        }

        List<RA> ras = raRepository.findAllByOrderByIdAsc();
        for (RA ra : ras) {
            List<Criterio> criterios = criterioRepository.findByRaIdOrderByOrdenAsc(ra.getId());
            assertThat(criterios)
                    .as("%s del módulo %s", ra.getCodigo(), ra.getModulo().getNombre())
                    .hasSizeGreaterThanOrEqualTo(3);
            assertThat(criterios).extracting(Criterio::getOrden)
                    .as("letras de %s del módulo %s", ra.getCodigo(), ra.getModulo().getNombre())
                    .containsExactlyElementsOf(java.util.stream.IntStream
                            .rangeClosed(1, criterios.size()).boxed().toList());
        }
    }

    @Test
    void losCriteriosDelProyectoIntermodularDeGradoMedioSonLosOficiales() {
        List<Criterio> criterios = criteriosDe("SMR", "1713", 5);

        assertThat(criterios).hasSize(3);
        assertThat(criterios.get(0).getLetra()).isEqualTo("a)");
        assertThat(criterios.get(0).getDescripcion())
                .isEqualTo("Se ha mantenido una actitud ordenada y metódica en la transmisión de "
                        + "la información.");
        assertThat(criterios.get(2).getDescripcion())
                .isEqualTo("Se han conocido los términos técnicos en otras lenguas que sean "
                        + "estándares del sector.");
    }

    @Test
    void elSembradorDeCriteriosEsIdempotente() {
        long antes = criterioRepository.count();

        criteriosIniciales.run(null);
        criteriosIniciales.run(null);

        assertThat(criterioRepository.count()).isEqualTo(antes);
    }

    @Test
    void laPaginaDeCiclosMuestraLosCriteriosDeEvaluacion() throws Exception {
        mvc.perform(get("/ciclos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Se han identificado los bloques que "
                        + "componen la estructura de un programa informático")))
                .andExpect(content().string(containsString("Se han introducido comentarios en el código")))
                .andExpect(content().string(containsString("class=\"letra-criterio\"")))
                .andExpect(content().string(containsString(">i)<")));
    }

    /** Criterios de un RA concreto, en el orden alfabético oficial. */
    private List<Criterio> criteriosDe(String siglas, String codigoModulo, int ordenRa) {
        Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase(siglas).orElseThrow();
        Modulo modulo = moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo.getId())
                .stream()
                .filter(m -> codigoModulo.equals(m.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el modulo " + codigoModulo));
        RA ra = raRepository.findByModuloIdOrderByIdAsc(modulo.getId())
                .stream()
                .filter(candidato -> candidato.getOrden() == ordenRa)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el RA" + ordenRa));
        return criterioRepository.findByRaIdOrderByOrdenAsc(ra.getId());
    }
}
