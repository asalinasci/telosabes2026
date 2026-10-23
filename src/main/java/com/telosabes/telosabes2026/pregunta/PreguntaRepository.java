package com.telosabes.telosabes2026.pregunta;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA de las preguntas.
 *
 * La aplicación usa open-in-view=false, así que las consultas que se muestran
 * en pantalla traen el criterio junto con la pregunta (y el RA, el módulo y el
 * ciclo a los que pertenece), para no cargarlos de forma perezosa al recorrer
 * la plantilla.
 */
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    /** Preguntas de un criterio, en el orden en que se dieron de alta. */
    @EntityGraph(attributePaths = "criterio")
    List<Pregunta> findByCriterioIdOrderByIdAsc(Long criterioId);

    /** Preguntas de un tipo concreto (test o texto), en orden de alta. */
    List<Pregunta> findByTipoOrderByIdAsc(TipoPregunta tipo);

    /** Todas las preguntas con su criterio, de la más reciente a la más antigua. */
    @EntityGraph(attributePaths = {"criterio", "criterio.ra", "criterio.ra.modulo",
            "criterio.ra.modulo.ciclo", "autor"})
    List<Pregunta> findAllByOrderByIdDesc();

    /** Todas las preguntas con su criterio y su autor, sin precarga perezosa. */
    @EntityGraph(attributePaths = {"criterio", "criterio.ra", "criterio.ra.modulo",
            "criterio.ra.modulo.ciclo", "autor"})
    List<Pregunta> findAllByOrderByIdAsc();

    /** Una pregunta con toda su ruta hasta el ciclo, para el formulario de edición. */
    @Override
    @EntityGraph(attributePaths = {"criterio", "criterio.ra", "criterio.ra.modulo",
            "criterio.ra.modulo.ciclo", "autor"})
    Optional<Pregunta> findById(Long id);

    /** Cantidad de preguntas de un criterio. */
    long countByCriterioId(Long criterioId);
}
