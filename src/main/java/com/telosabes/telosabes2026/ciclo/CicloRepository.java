package com.telosabes.telosabes2026.ciclo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repositorio Spring Data JPA de los ciclos formativos. */
public interface CicloRepository extends JpaRepository<Ciclo, Long> {

    /** Todos los ciclos ordenados por sus siglas (ASIR, DAM, DAW, SMR). */
    List<Ciclo> findAllByOrderBySiglasAsc();

    Optional<Ciclo> findBySiglasIgnoreCase(String siglas);

    boolean existsBySiglasIgnoreCase(String siglas);
}
