package com.telosabes.telosabes2026.usuario;

import com.telosabes.telosabes2026.seguridad.Rol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA de los usuarios.
 *
 * Ojo: la aplicacion trabaja con spring.jpa.open-in-view=false, asi que
 * cuando las plantillas necesitan el ciclo del alumno hay que traerlo en
 * la misma consulta (de eso se encargan los @EntityGraph).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busqueda para el login: el email es unico (se guarda en minusculas). */
    @EntityGraph(attributePaths = "ciclo")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "ciclo")
    List<Usuario> findAllByOrderByRolAscNombreAsc();

    @EntityGraph(attributePaths = "ciclo")
    List<Usuario> findByRolOrderByNombreAsc(Rol rol);

    /** Todos los usuarios menos los de un rol concreto (se usa para ocultar al administrador). */
    @EntityGraph(attributePaths = "ciclo")
    List<Usuario> findByRolNotOrderByRolAscNombreAsc(Rol rol);

    long countByRol(Rol rol);
}
