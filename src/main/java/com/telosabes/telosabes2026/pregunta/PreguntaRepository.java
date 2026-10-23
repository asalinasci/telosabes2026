package com.telosabes.telosabes2026.pregunta;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA de las preguntas.
 *
 * La aplicación usa open-in-view=false, así que las consultas que se muestran
 * en pantalla traen el criterio junto con la pregunta (evita cargarlo de forma
 * perezosa al recorrer la plantilla).
 */
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    /** Preguntas de un criterio, en el orden en que se dieron de alta. */
    @EntityGraph(attributePaths = "criterio")
    List<Pregunta> findByCriterioIdOrderByIdAsc(Long criterioId);

    /** Preguntas de un tipo concreto (test o texto), en orden de alta. */
    List<Pregunta> findByTipoOrderByIdAsc(TipoPregunta tipo);

    /** Todas las preguntas con su criterio, para recorrerlas sin precarga. */
    @EntityGraph(attributePaths = "criterio")
    List<Pregunta> findAllByOrderByIdAsc();

    /** Cantidad de preguntas de un criterio. */
    long countByCriterioId(Long criterioId);
}
