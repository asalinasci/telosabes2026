package com.telosabes.telosabes2026.ra;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA de los resultados de aprendizaje.
 *
 * La aplicación usa open-in-view=false, así que las consultas que se muestran
 * en pantalla traen el módulo junto con el RA (evita cargarlo de forma
 * perezosa al recorrer la plantilla).
 */
public interface RaRepository extends JpaRepository<RA, Long> {

    /** Resultados de aprendizaje de un módulo, en el orden de la tabla oficial. */
    @EntityGraph(attributePaths = "modulo")
    List<RA> findByModuloIdOrderByOrdenAsc(Long moduloId);

    /** Todos los resultados de aprendizaje con su módulo, agrupables por módulo. */
    @EntityGraph(attributePaths = "modulo")
    List<RA> findAllByOrderByModuloIdAscOrdenAsc();

    /** Cantidad de RA de un módulo (los módulos optativos no tienen). */
    long countByModuloId(Long moduloId);
}
