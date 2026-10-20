package com.telosabes.telosabes2026.ra;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
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
 * Pruebas de los resultados de aprendizaje (RA) oficiales de cada modulo.
 * Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RaTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RaRepository raRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private RasIniciales rasIniciales;

    @Test
    void todosLosModulosConCodigoTienenResultadosDeAprendizaje() {
        List<Modulo> modulos = moduloRepository.findAllByOrderByCicloSiglasAscCursoAscOrdenAsc();

        // 65 modulos: 61 con codigo oficial (todos con RA) y 4 optativos sin RA
        assertThat(modulos).hasSize(65);
        assertThat(modulos.stream().filter(m -> m.getCodigo() != null)).hasSize(61);

        for (Modulo modulo : modulos) {
            long cantidad = raRepository.countByModuloId(modulo.getId());
            if (modulo.getCodigo() == null) {
                assertThat(cantidad).as("el optativo %s no tiene RA", modulo.getNombre()).isZero();
            } else {
                assertThat(cantidad).as("modulo %s (%s)", modulo.getNombre(), modulo.getCodigo())
                        .isPositive();
            }
        }

        // 373 RA repartidos en los 4 ciclos
        assertThat(raRepository.count()).isEqualTo(373);
    }

    @Test
    void losResultadosEstanOrdenadosYSonLosOficiales() {
        List<RA> ras = raDe("DAM", "0485");

        assertThat(ras).hasSize(9);
        assertThat(ras).extracting(RA::getOrden).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(ras).extracting(RA::getCodigo)
                .containsExactly("RA1", "RA2", "RA3", "RA4", "RA5", "RA6", "RA7", "RA8", "RA9");
        assertThat(ras.get(0).getDescripcion()).isEqualTo(
                "Reconoce la estructura de un programa informático, identificando y relacionando "
                        + "los elementos propios del lenguaje de programación utilizado.");
        assertThat(ras).allSatisfy(ra -> {
            assertThat(ra.getDescripcion()).isNotBlank();
            assertThat(ra.getModulo().getCodigo()).isEqualTo("0485");
        });
    }

    @Test
    void losTransversalesSonLosMismosEnTodosLosCiclos() {
        String dam = raDe("DAM", "1709").get(0).getDescripcion();
        String smr = raDe("SMR", "1709").get(0).getDescripcion();

        assertThat(dam).isEqualTo(smr);
        assertThat(dam).startsWith("Distingue las características del sector productivo");
        assertThat(raDe("DAM", "1709")).hasSize(5);
        assertThat(raDe("SMR", "1664")).hasSize(5);
        assertThat(raDe("DAM", "1665")).hasSize(6);
    }

    @Test
    void elProyectoIntermodularDeGradoMedioTieneSusCincoResultados() {
        List<RA> ras = raDe("SMR", "1713");

        assertThat(ras).hasSize(5);
        assertThat(ras.get(0).getDescripcion())
                .startsWith("Caracteriza las empresas del sector atendiendo a su organización");
        assertThat(ras.get(4).getDescripcion())
                .isEqualTo("Transmite información con claridad, de manera ordenada y estructurada.");
    }

    @Test
    void elSembradorEsIdempotente() {
        long antes = raRepository.count();

        rasIniciales.run(null);
        rasIniciales.run(null);

        assertThat(raRepository.count()).isEqualTo(antes);
    }

    @Test
    void laPaginaDeCiclosMuestraLosResultadosDeAprendizaje() throws Exception {
        mvc.perform(get("/ciclos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resultados de aprendizaje")))
                .andExpect(content().string(containsString("Reconoce la estructura de un programa informático")))
                .andExpect(content().string(containsString("Proyecto intermodular")));
    }

    /** RA de un modulo concreto, en el orden de la tabla oficial. */
    private List<RA> raDe(String siglas, String codigo) {
        Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase(siglas).orElseThrow();
        Modulo modulo = moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo.getId())
                .stream()
                .filter(m -> codigo.equals(m.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el modulo " + codigo + " en " + siglas));
        return raRepository.findByModuloIdOrderByOrdenAsc(modulo.getId());
    }
}
