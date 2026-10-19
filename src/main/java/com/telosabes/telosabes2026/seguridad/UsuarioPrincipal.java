package com.telosabes.telosabes2026.seguridad;

import com.telosabes.telosabes2026.usuario.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapta nuestra entidad Usuario al modelo de Spring Security.
 * Asi la entidad no tiene que implementar UserDetails.
 */
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    /** Entidad original, por si el controlador necesita mas datos (id, nombre...). */
    public Usuario getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return usuario.getRol();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(usuario.getRol().getAuthority()));
    }

    @Override
    public String getPassword() {
        return usuario.getClave();
    }

    /** El login se hace con el correo electronico. */
    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}
