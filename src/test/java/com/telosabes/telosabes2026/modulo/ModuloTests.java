package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
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
 * Pruebas de los modulos profesionales de cada ciclo.
 * Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ModuloTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    private Ciclo ciclo(String siglas) {
        return cicloRepository.findBySiglasIgnoreCase(siglas).orElseThrow(
                () -> new AssertionError("No esta dado de alta el ciclo " + siglas));
    }

    private List<Modulo> modulos(String siglas) {
        return moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo(siglas).getId());
    }

    @Test
    void losCuatroCiclosTienenSusModulosDadosDeAlta() {
        assertThat(modulos("DAM")).hasSize(17);
        assertThat(modulos("DAW")).hasSize(16);
        assertThat(modulos("ASIR")).hasSize(17);
        assertThat(modulos("SMR")).hasSize(15);
    }

    @Test
    void cadaCicloSumaDosMilHoras() {
        for (String siglas : List.of("DAM", "DAW", "ASIR", "SMR")) {
            int total = modulos(siglas).stream().mapToInt(Modulo::getHorasTotales).sum();
            assertThat(total)
                    .as("horas totales del ciclo " + siglas)
                    .isEqualTo(2000);
        }
    }

    @Test
    void losModulosTransversalesEstanEnTodosLosCiclos() {
        for (String siglas : List.of("DAM", "DAW", "ASIR", "SMR")) {
            List<String> nombres = modulos(siglas).stream().map(Modulo::getNombre).toList();

            assertThat(nombres)
                    .as("modulos transversales del ciclo " + siglas)
                    .contains(
                            "Itinerario personal para la empleabilidad I",
                            "Itinerario personal para la empleabilidad II",
                            "Digitalización aplicada a los sectores productivos",
                            "Sostenibilidad aplicada al sistema productivo",
                            "Inglés profesional");
        }
    }

    @Test
    void elModuloDeProgramacionDeDamTieneSusHoras() {
        Modulo programacion = modulos("DAM").stream()
                .filter(m -> "0485".equals(m.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DAM no tiene el modulo 0485 Programacion"));

        assertThat(programacion.getNombre()).isEqualTo("Programación");
        assertThat(programacion.getCurso()).isEqualTo(1);
        assertThat(programacion.getHorasSemanales()).isEqualTo(7);
        assertThat(programacion.getHorasCentro()).isEqualTo(195);
        assertThat(programacion.getHorasEmpresa()).isEqualTo(45);
        assertThat(programacion.getHorasTotales()).isEqualTo(240);
        assertThat(programacion.getCiclo().getSiglas()).isEqualTo("DAM");
    }

    @Test
    void losModulosDeUnCicloSalenOrdenadosPorCursoYParecenPrimero() {
        List<Modulo> dam = modulos("DAM");

        assertThat(dam.get(0).getCurso()).isEqualTo(1);
        assertThat(dam.get(0).getCodigo()).isEqualTo("0373");
        assertThat(dam.get(dam.size() - 1).getCurso()).isEqualTo(2);
        assertThat(dam.get(dam.size() - 1).getNombre())
                .isEqualTo("Proyecto intermodular de desarrollo de aplicaciones multiplataforma");
    }

    @Test
    void elCatalogoDeCiclosYModulosEsPublico() throws Exception {
        mockMvc.perform(get("/ciclos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ciclos formativos y sus modulos")))
                .andExpect(content().string(containsString("DAM")))
                .andExpect(content().string(containsString("DAW")))
                .andExpect(content().string(containsString("ASIR")))
                .andExpect(content().string(containsString("SMR")))
                .andExpect(content().string(containsString("0485")))
                .andExpect(content().string(containsString("Programación")))
                .andExpect(content().string(containsString("Proyecto intermodular")));
    }
}
