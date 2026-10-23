package com.telosabes.telosabes2026.pregunta;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.criterio.Criterio;
import com.telosabes.telosabes2026.criterio.CriterioRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
import com.telosabes.telosabes2026.ra.RA;
import com.telosabes.telosabes2026.ra.RaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas del CRUD de preguntas.
 *
 * La pantalla cuelga de /profesor/**, así que se comprueba que el alumno queda
 * fuera y que profesores y administrador pueden listar, crear, editar y borrar.
 * Cada prueba va en su propia transacción y se deshace al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PreguntaCrudTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private CriterioRepository criterioRepository;

    @Autowired
    private RaRepository raRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Test
    void elAlumnoNoPuedeEntrarEnElCrud() throws Exception {
        mockMvc.perform(get("/profesor/preguntas")
                        .with(user("alumno@ejemplo.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/profesor/preguntas/nueva")
                        .with(user("alumno@ejemplo.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/profesor/preguntas")
                        .with(csrf())
                        .with(user("alumno@ejemplo.com").roles("ALUMNO"))
                        .param("tipo", "TEXTO")
                        .param("criterioId", criterio().getId().toString())
                        .param("enunciado", "Intento de un alumno"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elProfesorYElAdministradorVenElListado() throws Exception {
        Pregunta guardada = preguntaRepository.saveAndFlush(new Pregunta(
                "¿Que estructura permite repetir instrucciones?", criterio(),
                "Una condicion", "Un bucle", "Una funcion", "Un comentario", 2));

        mockMvc.perform(get("/profesor/preguntas")
                        .with(user("profe@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("preguntas/lista"))
                .andExpect(model().attributeExists("preguntas"))
                .andExpect(content().string(containsString("¿Que estructura permite repetir instrucciones?")))
                .andExpect(content().string(containsString("b)")));

        mockMvc.perform(get("/profesor/preguntas")
                        .with(user("admin@ejemplo.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "¿Que estructura permite repetir instrucciones?")));

        assertThat(preguntaRepository.findById(guardada.getId())).isPresent();
    }

    @Test
    void elProfesorCreaUnaPreguntaDeTest() throws Exception {
        Criterio criterio = criterio();

        mockMvc.perform(post("/profesor/preguntas")
                        .with(csrf())
                        .with(user("profe@ejemplo.com").roles("PROFESOR"))
                        .param("tipo", "TEST")
                        .param("criterioId", criterio.getId().toString())
                        .param("enunciado", "¿Que es una variable?")
                        .param("respuesta1", "Un espacio en memoria con nombre")
                        .param("respuesta2", "Un bucle")
                        .param("respuesta3", "Una funcion")
                        .param("respuesta4", "Un fichero")
                        .param("correcta", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profesor/preguntas"))
                .andExpect(flash().attribute("mensaje", "Pregunta creada correctamente."));

        Pregunta creada = buscarPorEnunciado("¿Que es una variable?");
        assertThat(creada.getTipo()).isEqualTo(TipoPregunta.TEST);
        assertThat(creada.getCriterio().getId()).isEqualTo(criterio.getId());
        assertThat(creada.getRespuestaCorrecta()).isEqualTo("Un espacio en memoria con nombre");
        assertThat(creada.getAciertos()).isZero();
        assertThat(creada.getErrores()).isZero();
    }

    @Test
    void elProfesorCreaUnaPreguntaDeTextoYSeIgnoranLasRespuestas() throws Exception {
        mockMvc.perform(post("/profesor/preguntas")
                        .with(csrf())
                        .with(user("profe@ejemplo.com").roles("PROFESOR"))
                        .param("tipo", "TEXTO")
                        .param("criterioId", criterio().getId().toString())
                        .param("enunciado", "Explica que es una variable.")
                        .param("respuesta1", "Esto no deberia guardarse")
                        .param("respuesta2", "Ni esto")
                        .param("respuesta3", "Ni esto")
                        .param("respuesta4", "Ni esto")
                        .param("correcta", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profesor/preguntas"));

        Pregunta creada = buscarPorEnunciado("Explica que es una variable.");
        assertThat(creada.getTipo()).isEqualTo(TipoPregunta.TEXTO);
        assertThat(creada.getRespuestas()).isEmpty();
        assertThat(creada.getCorrecta()).isNull();
    }

    @Test
    void unaPreguntaDeTestIncompletaNoSeGuarda() throws Exception {
        long antes = preguntaRepository.count();

        mockMvc.perform(post("/profesor/preguntas")
                        .with(csrf())
                        .with(user("profe@ejemplo.com").roles("PROFESOR"))
                        .param("tipo", "TEST")
                        .param("criterioId", criterio().getId().toString())
                        .param("enunciado", "¿Pregunta incompleta?")
                        .param("respuesta1", "Solo una respuesta"))
                .andExpect(status().isOk())
                .andExpect(view().name("preguntas/formulario"))
                .andExpect(model().attributeHasFieldErrors("preguntaForm", "testBienFormada"))
                .andExpect(content().string(containsString(
                        "Una pregunta de test necesita las cuatro respuestas")));

        assertThat(preguntaRepository.count()).isEqualTo(antes);
    }

    @Test
    void sinCriterioNoSeGuardaLaPregunta() throws Exception {
        long antes = preguntaRepository.count();

        mockMvc.perform(post("/profesor/preguntas")
                        .with(csrf())
                        .with(user("profe@ejemplo.com").roles("PROFESOR"))
                        .param("tipo", "TEXTO")
                        .param("enunciado", "Pregunta sin criterio"))
                .andExpect(status().isOk())
                .andExpect(view().name("preguntas/formulario"))
                .andExpect(model().attributeHasFieldErrors("preguntaForm", "criterioId"));

        assertThat(preguntaRepository.count()).isEqualTo(antes);
    }

    @Test
    void elProfesorEditaUnaPregunta() throws Exception {
        Pregunta pregunta = preguntaRepository.saveAndFlush(new Pregunta(
                "Enunciado original", criterio(), "a", "b", "c", "d", 1));

        mockMvc.perform(get("/profesor/preguntas/{id}/editar", pregunta.getId())
                        .with(user("profe@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("preguntas/formulario"))
                .andExpect(model().attribute("preguntaId", pregunta.getId()))
                .andExpect(content().string(containsString("Enunciado original")));

        mockMvc.perform(post("/profesor/preguntas/{id}", pregunta.getId())
                        .with(csrf())
                        .with(user("profe@ejemplo.com").roles("PROFESOR"))
                        .param("tipo", "TEXTO")
                        .param("criterioId", criterio().getId().toString())
                        .param("enunciado", "Enunciado corregido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profesor/preguntas"))
                .andExpect(flash().attribute("mensaje", "Pregunta actualizada correctamente."));

        Pregunta actualizada = preguntaRepository.findById(pregunta.getId()).orElseThrow();
        assertThat(actualizada.getEnunciado()).isEqualTo("Enunciado corregido");
        assertThat(actualizada.getTipo()).isEqualTo(TipoPregunta.TEXTO);
        assertThat(actualizada.getRespuestas()).isEmpty();
        assertThat(actualizada.getCorrecta()).isNull();
    }

    @Test
    void elProfesorEliminaUnaPregunta() throws Exception {
        Pregunta pregunta = preguntaRepository.saveAndFlush(new Pregunta(
                "Pregunta que se va a borrar", criterio(), "a", "b", "c", "d", 4));

        mockMvc.perform(post("/profesor/preguntas/{id}/eliminar", pregunta.getId())
                        .with(csrf())
                        .with(user("profe@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profesor/preguntas"))
                .andExpect(flash().attribute("mensaje", "Pregunta eliminada correctamente."));

        assertThat(preguntaRepository.findById(pregunta.getId())).isEmpty();
    }

    @Test
    void editarUnaPreguntaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/profesor/preguntas/{id}/editar", 999_999L)
                        .with(user("profe@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void elFormularioDeAltaCargaLosCriteriosAgrupadosPorModulo() throws Exception {
        mockMvc.perform(get("/profesor/preguntas/nueva")
                        .with(user("profe@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("preguntas/formulario"))
                .andExpect(model().attributeExists("criteriosPorModulo"))
                .andExpect(content().string(containsString("0485")));
    }

    /** Primer criterio del RA1 de Programacion (DAM), para no depender de ids fijos. */
    private Criterio criterio() {
        Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase("DAM").orElseThrow();
        Modulo modulo = moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo.getId())
                .stream()
                .filter(m -> "0485".equals(m.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el modulo 0485"));
        RA ra = raRepository.findByModuloIdOrderByOrdenAsc(modulo.getId())
                .stream()
                .filter(candidato -> candidato.getOrden() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el RA1"));
        return criterioRepository.findByRaIdOrderByOrdenAsc(ra.getId()).get(0);
    }

    private Pregunta buscarPorEnunciado(String enunciado) {
        return preguntaRepository.findAllByOrderByIdAsc()
                .stream()
                .filter(p -> enunciado.equals(p.getEnunciado()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se guardo la pregunta: " + enunciado));
    }
}
