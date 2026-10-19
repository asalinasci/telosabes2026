package com.telosabes.telosabes2026.modulo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA de los módulos profesionales.
 *
 * Los listados siempre piden los módulos ordenados por ciclo y curso, y traen
 * el ciclo junto con el módulo (la aplicación usa open-in-view=false, así que
 * la plantilla no puede cargarlo de forma perezosa).
 */
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    /** Módulos de un ciclo, en el orden de la tabla oficial. */
    @EntityGraph(attributePaths = "ciclo")
    List<Modulo> findByCicloIdOrderByCursoAscOrdenAsc(Long cicloId);

    /** Módulos de un ciclo (buscando por siglas), en el orden de la tabla oficial. */
    @EntityGraph(attributePaths = "ciclo")
    List<Modulo> findByCicloSiglasIgnoreCaseOrderByCursoAscOrdenAsc(String siglas);

    /** Todos los módulos, agrupables por ciclo. */
    @EntityGraph(attributePaths = "ciclo")
    List<Modulo> findAllByOrderByCicloSiglasAscCursoAscOrdenAsc();

    /** Evita duplicados al dar de alta los módulos iniciales. */
    boolean existsByCicloIdAndNombre(Long cicloId, String nombre);

    long countByCicloId(Long cicloId);
}
