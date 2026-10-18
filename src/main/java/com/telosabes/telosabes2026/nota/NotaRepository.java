package com.telosabes.telosabes2026.nota;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA: aporta el CRUD completo (save, findAll,
 * findById, delete...) sin escribir SQL ni implementacion.
 */
public interface NotaRepository extends JpaRepository<Nota, Long> {

    /** Devuelve las notas mas recientes primero. */
    List<Nota> findAllByOrderByCreadoEnDesc();
}
