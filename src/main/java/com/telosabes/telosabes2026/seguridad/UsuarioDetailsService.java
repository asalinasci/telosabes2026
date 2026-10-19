package com.telosabes.telosabes2026.seguridad;

import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga el usuario desde MariaDB cuando alguien intenta iniciar sesion.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(UsuarioPrincipal::new)
                // Mensaje generico: no revelamos si el correo existe o no
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
    }
}
