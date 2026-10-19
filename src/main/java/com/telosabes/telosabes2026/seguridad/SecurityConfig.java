package com.telosabes.telosabes2026.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de Spring Security.
 *
 * Resumen de permisos:
 *   - Publico .............. /login, /registro, /ciclos, /css/**, pagina de inicio
 *   - ADMIN                 /admin/**
 *   - PROFESOR y ADMIN      /profesor/**
 *   - Cualquier usuario autenticado: el resto (/ y /notas)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Las contraseñas se guardan con BCrypt (hash con salt).
     * Nunca se guarda ni se compara la contraseña en claro.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(autorizacion -> autorizacion
                    // Paginas y recursos publicos
                    .requestMatchers("/", "/login", "/registro", "/ciclos", "/acceso-denegado", "/error").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/webjars/**").permitAll()
                    // Solo el administrador
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    // Profesores (y el administrador)
                    .requestMatchers("/profesor/**").hasAnyRole("PROFESOR", "ADMIN")
                    // Cualquier usuario con sesion iniciada
                    .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("clave")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(salir -> salir
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?salir")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(manejo -> manejo
                        .accessDeniedPage("/acceso-denegado"));

        return http.build();
    }
}
