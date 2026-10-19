package com.telosabes.telosabes2026.nota;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA: aporta el CRUD completo (save, findAll,
 * findById, delete...) sin escribir SQL ni implementacion.
 */
public interface NotaRepository extends JpaRepository<Nota, Long> {

    /**
     * Devuelve las notas mas recientes primero, trayendo tambien el autor
     * en la misma consulta (asi la plantilla puede mostrar su nombre sin
     * consultas extra).
     */
    @EntityGraph(attributePaths = "autor")
    List<Nota> findAllByOrderByCreadoEnDesc();

    /** Notas de un autor concreto (por si se quiere un "mis notas"). */
    @EntityGraph(attributePaths = "autor")
    List<Nota> findByAutorIdOrderByCreadoEnDesc(Long autorId);
}
