package com.telosabes.telosabes2026.usuario;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de los usuarios.
 *
 * Es el unico sitio donde se decide el rol de un usuario:
 *  - el registro publico crea SIEMPRE un ALUMNO
 *  - solo el administrador puede promover un ALUMNO a PROFESOR
 *  - nunca se crea un ADMIN desde la aplicacion
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CicloRepository cicloRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          CicloRepository cicloRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cicloRepository = cicloRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registro publico de un alumno, con el ciclo que va a estudiar.
     *
     * @throws EmailYaRegistradoException si el correo ya esta en uso
     * @throws CicloNoValidoException     si el ciclo elegido no existe
     */
    @Transactional
    public Usuario registrarAlumno(RegistroForm formulario) {
        String email = normalizar(formulario.getEmail());

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailYaRegistradoException(email);
        }

        // El ciclo se busca en la base de datos: no nos fiamos del formulario
        Ciclo ciclo = cicloRepository.findById(formulario.getCicloId())
                .orElseThrow(() -> new CicloNoValidoException(formulario.getCicloId()));

        Usuario alumno = new Usuario(
                formulario.getNombre().trim(),
                email,
                passwordEncoder.encode(formulario.getClave()),
                Rol.ALUMNO,
                ciclo);

        return usuarioRepository.save(alumno);
    }

    /**
     * Convierte un alumno en profesor.
     *
     * @return el usuario actualizado
     * @throws IllegalArgumentException si el usuario no existe o no es un alumno
     */
    @Transactional
    public Usuario promoverAProfesor(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario " + id));

        if (usuario.esAdministrador()) {
            // El administrador no se toca nunca desde la aplicacion
            throw new IllegalArgumentException("La cuenta de administrador no se puede modificar");
        }
        if (usuario.getRol() == Rol.PROFESOR) {
            throw new IllegalStateException("El usuario ya es profesor");
        }

        usuario.setRol(Rol.PROFESOR);
        return usuarioRepository.save(usuario);
    }

    /** Listado para el administrador: todos menos el administrador. */
    @Transactional(readOnly = true)
    public List<Usuario> listarSinAdministradores() {
        return usuarioRepository.findByRolNotOrderByRolAscNombreAsc(Rol.ADMIN);
    }

    /** Alumnos, para el listado de los profesores. */
    @Transactional(readOnly = true)
    public List<Usuario> listarAlumnos() {
        return usuarioRepository.findByRolOrderByNombreAsc(Rol.ALUMNO);
    }

    public long contarPorRol(Rol rol) {
        return usuarioRepository.countByRol(rol);
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
