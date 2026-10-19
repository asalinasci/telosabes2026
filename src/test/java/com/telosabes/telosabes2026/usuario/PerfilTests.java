package com.telosabes.telosabes2026.usuario;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del perfil: cada alumno ve sus datos y cambia su ciclo.
 * Se ejecutan con H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PerfilTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CicloRepository cicloRepository;

    private Long cicloId(String siglas) {
        return cicloRepository.findBySiglasIgnoreCase(siglas)
                .map(Ciclo::getId)
                .orElseThrow(() -> new AssertionError("No existe el ciclo " + siglas));
    }

    private Usuario crear(String nombre, String email, Rol rol, String siglasCiclo) {
        Ciclo ciclo = siglasCiclo == null ? null
                : cicloRepository.findBySiglasIgnoreCase(siglasCiclo).orElseThrow();
        return usuarioRepository.save(new Usuario(nombre, email, "hash-de-prueba", rol, ciclo));
    }

    private Usuario recargar(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    @Test
    void elAlumnoVeSuPerfilConSusDatos() throws Exception {
        crear("Alumno Perfil", "perfil@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(get("/perfil").with(user("perfil@ejemplo.com").roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mi perfil")))
                .andExpect(content().string(containsString("Alumno Perfil")))
                .andExpect(content().string(containsString("perfil@ejemplo.com")))
                .andExpect(content().string(containsString("DAM - Desarrollo de Aplicaciones Multiplataforma")))
                .andExpect(content().string(containsString("Cambiar de ciclo")))
                .andExpect(content().string(containsString("SMR - Sistemas Microinformáticos y Redes")));
    }

    @Test
    void elAlumnoCambiaSuCicloDesdeSuPerfil() throws Exception {
        crear("Alumno Cambia", "perfil.cambia@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/perfil/ciclo").with(csrf())
                        .with(user("perfil.cambia@ejemplo.com").roles("ALUMNO"))
                        .param("cicloId", cicloId("SMR").toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil"))
                .andExpect(flash().attributeExists("mensaje"));

        assertThat(recargar("perfil.cambia@ejemplo.com").getCiclo().getSiglas()).isEqualTo("SMR");
    }

    @Test
    void elAlumnoPuedeQuedarseSinCiclo() throws Exception {
        crear("Alumno Se Queda Sin Ciclo", "perfil.sinciclo@ejemplo.com", Rol.ALUMNO, "DAW");

        mockMvc.perform(post("/perfil/ciclo").with(csrf())
                        .with(user("perfil.sinciclo@ejemplo.com").roles("ALUMNO"))
                        .param("cicloId", ""))
                .andExpect(status().is3xxRedirection());

        assertThat(recargar("perfil.sinciclo@ejemplo.com").getCiclo()).isNull();
    }

    @Test
    void conUnCicloQueNoExisteNoSeCambiaNada() throws Exception {
        crear("Alumno Ciclo Raro", "perfil.rarociclo@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/perfil/ciclo").with(csrf())
                        .with(user("perfil.rarociclo@ejemplo.com").roles("ALUMNO"))
                        .param("cicloId", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(recargar("perfil.rarociclo@ejemplo.com").getCiclo().getSiglas()).isEqualTo("DAM");
    }

    @Test
    void cambiarElCicloSoloAfectaAlUsuarioConectado() throws Exception {
        crear("Alumno Que Cambia", "perfil.a@ejemplo.com", Rol.ALUMNO, "DAM");
        crear("Alumno Que No Cambia", "perfil.b@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/perfil/ciclo").with(csrf())
                        .with(user("perfil.a@ejemplo.com").roles("ALUMNO"))
                        .param("cicloId", cicloId("ASIR").toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(recargar("perfil.a@ejemplo.com").getCiclo().getSiglas()).isEqualTo("ASIR");
        assertThat(recargar("perfil.b@ejemplo.com").getCiclo().getSiglas()).isEqualTo("DAM");
    }

    @Test
    void sinSesionNoSePuedeEntrarAlPerfil() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void elAdministradorNoPuedeCambiarSuCicloDesdeElPerfil() throws Exception {
        crear("Administrador Perfil", "admin.perfil@ejemplo.com", Rol.ADMIN, null);

        mockMvc.perform(get("/perfil").with(user("admin.perfil@ejemplo.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Los datos del administrador no se gestionan")));

        mockMvc.perform(post("/perfil/ciclo").with(csrf())
                        .with(user("admin.perfil@ejemplo.com").roles("ADMIN"))
                        .param("cicloId", cicloId("DAM").toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(recargar("admin.perfil@ejemplo.com").getCiclo()).isNull();
    }
}
