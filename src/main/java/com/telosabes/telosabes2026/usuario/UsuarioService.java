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

    /**
     * Datos actuales de un usuario.
     *
     * Se usa en la pantalla de perfil para no fiarse del principal de la
     * sesion, que se creo al iniciar sesion y puede tener el ciclo antiguo.
     */
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario " + id));
    }

    /**
     * Busca un usuario que el administrador pueda modificar.
     *
     * @throws IllegalArgumentException si no existe o es el administrador
     */
    @Transactional(readOnly = true)
    public Usuario buscarEditable(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario " + id));

        if (usuario.esAdministrador()) {
            // El administrador no se toca nunca desde la aplicacion
            throw new IllegalArgumentException("La cuenta de administrador no se puede modificar");
        }
        return usuario;
    }

    /**
     * Cambia los datos de un usuario (nombre, correo y ciclo).
     *
     * @throws EmailYaRegistradoException si el correo ya lo usa otro usuario
     * @throws CicloNoValidoException     si el ciclo elegido no existe
     * @throws IllegalArgumentException   si el usuario no existe o es el administrador
     */
    @Transactional
    public Usuario actualizarDatos(Long id, EditarUsuarioForm formulario) {
        Usuario usuario = buscarEditable(id);
        String email = normalizar(formulario.getEmail());

        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new EmailYaRegistradoException(email);
            }
        });

        usuario.setNombre(formulario.getNombre().trim());
        usuario.setEmail(email);
        usuario.setCiclo(resolverCiclo(formulario.getCicloId()));

        return usuarioRepository.save(usuario);
    }

    /**
     * Cambia solo el ciclo de un usuario (desplegable del listado).
     *
     * @throws CicloNoValidoException   si el ciclo elegido no existe
     * @throws IllegalArgumentException si el usuario no existe o es el administrador
     */
    @Transactional
    public Usuario cambiarCiclo(Long id, Long cicloId) {
        Usuario usuario = buscarEditable(id);
        usuario.setCiclo(resolverCiclo(cicloId));
        return usuarioRepository.save(usuario);
    }

    /** Alumnos, para el listado de los profesores. */
    @Transactional(readOnly = true)
    public List<Usuario> listarAlumnos() {
        return usuarioRepository.findByRolOrderByNombreAsc(Rol.ALUMNO);
    }

    public long contarPorRol(Rol rol) {
        return usuarioRepository.countByRol(rol);
    }

    /**
     * El ciclo se busca siempre en la base de datos, no nos fiamos del
     * formulario. Vacio (null) significa "sin ciclo".
     */
    private Ciclo resolverCiclo(Long cicloId) {
        if (cicloId == null) {
            return null;
        }
        return cicloRepository.findById(cicloId)
                .orElseThrow(() -> new CicloNoValidoException(cicloId));
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
