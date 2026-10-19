package com.telosabes.telosabes2026.usuario;

import com.telosabes.telosabes2026.seguridad.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busqueda para el login: el email es unico (se guarda en minusculas). */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findAllByOrderByRolAscNombreAsc();

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);

    /** Todos los usuarios menos los de un rol concreto (se usa para ocultar al administrador). */
    List<Usuario> findByRolNotOrderByRolAscNombreAsc(Rol rol);

    long countByRol(Rol rol);
}
