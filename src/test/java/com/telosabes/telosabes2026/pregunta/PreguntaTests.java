package com.telosabes.telosabes2026.pregunta;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.criterio.Criterio;
import com.telosabes.telosabes2026.criterio.CriterioRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
import com.telosabes.telosabes2026.ra.RA;
import com.telosabes.telosabes2026.ra.RaRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las preguntas que evalúan cada criterio de evaluación.
 * Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@Transactional
class PreguntaTests {

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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Validator validator;

    @Test
    void unaPreguntaDeTestGuardaSusCuatroRespuestasYLaCorrecta() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);

        Pregunta pregunta = new Pregunta(
                "¿Qué estructura permite repetir un bloque de instrucciones?",
                criterio, unAutor(),
                "Una condición", "Un bucle", "Una función", "Un comentario",
                2);

        Pregunta guardada = preguntaRepository.saveAndFlush(pregunta);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getTipo()).isEqualTo(TipoPregunta.TEST);
        assertThat(guardada.getEnunciado())
                .isEqualTo("¿Qué estructura permite repetir un bloque de instrucciones?");
        assertThat(guardada.getRespuestas())
                .containsExactly("Una condición", "Un bucle", "Una función", "Un comentario");
        assertThat(guardada.getCorrecta()).isEqualTo(2);
        assertThat(guardada.getRespuestaCorrecta()).isEqualTo("Un bucle");
        assertThat(guardada.getLetraCorrecta()).isEqualTo("b)");
        assertThat(guardada.getCriterio().getId()).isEqualTo(criterio.getId());
    }

    @Test
    void unaPreguntaDeTextoNoTieneRespuestasNiCorrecta() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);

        Pregunta pregunta = new Pregunta(
                "Explica con tus palabras qué es un bucle y pon un ejemplo.", criterio, unAutor());

        Pregunta guardada = preguntaRepository.saveAndFlush(pregunta);

        assertThat(guardada.getTipo()).isEqualTo(TipoPregunta.TEXTO);
        assertThat(guardada.getRespuestas()).isEmpty();
        assertThat(guardada.getCorrecta()).isNull();
        assertThat(guardada.getRespuestaCorrecta()).isNull();
        assertThat(guardada.getLetraCorrecta()).isNull();
    }

    @Test
    void losContadoresDeAciertosYErroresEmpiezanEnCero() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);

        Pregunta guardada = preguntaRepository.saveAndFlush(new Pregunta(
                "¿Cuál es la respuesta correcta?", criterio, unAutor(),
                "a", "b", "c", "d", 4));

        assertThat(guardada.getAciertos()).isZero();
        assertThat(guardada.getErrores()).isZero();
    }

    @Test
    void laPreguntaSeCargaJuntoConSuCriterio() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);
        preguntaRepository.saveAndFlush(new Pregunta("Pregunta de prueba", criterio, unAutor(),
                "a", "b", "c", "d", 1));

        List<Pregunta> preguntas = preguntaRepository.findAllByOrderByIdAsc();

        assertThat(preguntas).isNotEmpty();
        for (Pregunta pregunta : preguntas) {
            assertThat(pregunta.getCriterio()).isNotNull();
            assertThat(pregunta.getCriterio().getDescripcion()).isNotBlank();
        }
    }

    @Test
    void seCuentanLasPreguntasDeCadaCriterio() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);
        long antes = preguntaRepository.countByCriterioId(criterio.getId());

        preguntaRepository.save(new Pregunta("Primera", criterio, unAutor(), "a", "b", "c", "d", 1));
        preguntaRepository.save(new Pregunta("Segunda", criterio, unAutor(), "a", "b", "c", "d", 2));
        preguntaRepository.flush();

        assertThat(preguntaRepository.countByCriterioId(criterio.getId()))
                .isEqualTo(antes + 2);
    }

    @Test
    void unaPreguntaDeTestSinLasCuatroRespuestasNoEsValida() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);

        Pregunta incompleta = new Pregunta(
                "¿Cuál es la respuesta correcta?", criterio, unAutor(),
                "a", "b", null, null, 1);

        assertThat(validator.validate(incompleta))
                .extracting(violacion -> violacion.getMessage())
                .contains("Una pregunta de test necesita las cuatro respuestas y marcar la correcta");

        Pregunta sinCorrecta = new Pregunta(
                "¿Cuál es la respuesta correcta?", criterio, unAutor(),
                "a", "b", "c", "d", null);
        assertThat(validator.validate(sinCorrecta)).isNotEmpty();
    }

    @Test
    void unaPreguntaDeTestCompletaEsValida() {
        Criterio criterio = primerCriterioDe("DAM", "0485", 1);

        Pregunta completa = new Pregunta(
                "¿Cuál es la respuesta correcta?", criterio, unAutor(),
                "a", "b", "c", "d", 3);

        assertThat(validator.validate(completa)).isEmpty();
    }

    /** Profesor de prueba, autor de las preguntas que se guardan en los tests. */
    private Usuario unAutor() {
        return usuarioRepository.save(new Usuario(
                "Profesor de prueba",
                "profe-" + UUID.randomUUID() + "@ejemplo.com",
                "hash-de-prueba",
                Rol.PROFESOR));
    }

    /** Primer criterio de un RA concreto, para colgar las preguntas de prueba. */
    private Criterio primerCriterioDe(String siglas, String codigoModulo, int ordenRa) {
        Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase(siglas).orElseThrow();
        Modulo modulo = moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo.getId())
                .stream()
                .filter(m -> codigoModulo.equals(m.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el modulo " + codigoModulo));
        RA ra = raRepository.findByModuloIdOrderByOrdenAsc(modulo.getId())
                .stream()
                .filter(candidato -> candidato.getOrden() == ordenRa)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe el RA" + ordenRa));
        return criterioRepository.findByRaIdOrderByOrdenAsc(ra.getId()).get(0);
    }
}
