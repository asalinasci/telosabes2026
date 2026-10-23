package com.telosabes.telosabes2026.criterio;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA de los criterios de evaluación.
 *
 * La aplicación usa open-in-view=false, así que las consultas que se muestran
 * en pantalla traen el RA junto con el criterio (evita cargarlo de forma
 * perezosa al recorrer la plantilla).
 */
public interface CriterioRepository extends JpaRepository<Criterio, Long> {

    /** Criterios de un resultado de aprendizaje, en orden alfabético oficial. */
    List<Criterio> findByRaIdOrderByOrdenAsc(Long raId);

    /** Todos los criterios con su RA, agrupables por RA. */
    @EntityGraph(attributePaths = {"ra", "ra.modulo", "ra.modulo.ciclo"})
    List<Criterio> findAllByOrderByRaIdAscOrdenAsc();

    /** Cantidad de criterios de un RA (ningún RA oficial se queda sin ellos). */
    long countByRaId(Long raId);

    /** Criterios de todos los RA junto con su RA, para recorrerlos sin precarga. */
    @EntityGraph(attributePaths = "ra")
    List<Criterio> findAllByOrderByIdAsc();
}
