package com.telosabes.telosabes2026.admin;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la zona del administrador: cambiar el ciclo de un alumno y
 * editar sus datos. Se ejecutan con H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminUsuariosTests {

    private static final String ADMIN = "admin@ejemplo.com";

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

    @Test
    void elAdministradorCambiaElCicloDeUnAlumno() throws Exception {
        Usuario alumno = crear("Alumno Cambio De Ciclo", "cambio.ciclo@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/admin/usuarios/{id}/ciclo", alumno.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("cicloId", cicloId("SMR").toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("mensaje"));

        assertThat(usuarioRepository.findById(alumno.getId()).orElseThrow().getCiclo().getSiglas())
                .isEqualTo("SMR");
    }

    @Test
    void elAdministradorPuedeDejarAUnAlumnoSinCiclo() throws Exception {
        Usuario alumno = crear("Alumno Sin Ciclo Admin", "sin.ciclo.admin@ejemplo.com", Rol.ALUMNO, "DAW");

        mockMvc.perform(post("/admin/usuarios/{id}/ciclo", alumno.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("cicloId", ""))
                .andExpect(status().is3xxRedirection());

        assertThat(usuarioRepository.findById(alumno.getId()).orElseThrow().getCiclo()).isNull();
    }

    @Test
    void conUnCicloQueNoExisteNoSeCambiaNada() throws Exception {
        Usuario alumno = crear("Alumno Ciclo Inventado", "ciclo.inventado@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/admin/usuarios/{id}/ciclo", alumno.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("cicloId", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertThat(usuarioRepository.findById(alumno.getId()).orElseThrow().getCiclo().getSiglas())
                .isEqualTo("DAM");
    }

    @Test
    void elAdministradorEditaLosDatosDeUnUsuario() throws Exception {
        Usuario alumno = crear("Alumno Antes", "editar.antes@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/admin/usuarios/{id}/editar", alumno.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("nombre", "Alumno Despues")
                        .param("email", "Editar.Despues@Ejemplo.com")
                        .param("cicloId", cicloId("ASIR").toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("mensaje"));

        Usuario guardado = usuarioRepository.findById(alumno.getId()).orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Alumno Despues");
        // El correo se guarda en minusculas, como en el registro
        assertThat(guardado.getEmail()).isEqualTo("editar.despues@ejemplo.com");
        assertThat(guardado.getCiclo().getSiglas()).isEqualTo("ASIR");
        // El rol no se toca desde esta pantalla
        assertThat(guardado.getRol()).isEqualTo(Rol.ALUMNO);
    }

    @Test
    void laPantallaDeEdicionCargaLosDatosActuales() throws Exception {
        Usuario alumno = crear("Alumno Cargado", "editar.carga@ejemplo.com", Rol.PROFESOR, "SMR");

        mockMvc.perform(get("/admin/usuarios/{id}/editar", alumno.getId()).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar usuario")))
                .andExpect(content().string(containsString("Alumno Cargado")))
                .andExpect(content().string(containsString("editar.carga@ejemplo.com")))
                .andExpect(content().string(containsString("SMR - Sistemas Microinformáticos y Redes")));
    }

    @Test
    void noSePuedeUsarElCorreoDeOtroUsuario() throws Exception {
        Usuario primero = crear("Alumno Uno", "correo.uno@ejemplo.com", Rol.ALUMNO, "DAM");
        crear("Alumno Dos", "correo.dos@ejemplo.com", Rol.ALUMNO, "DAW");

        mockMvc.perform(post("/admin/usuarios/{id}/editar", primero.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("nombre", "Alumno Uno")
                        .param("email", "correo.dos@ejemplo.com")
                        .param("cicloId", cicloId("DAM").toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ya esta registrado")));

        assertThat(usuarioRepository.findById(primero.getId()).orElseThrow().getEmail())
                .isEqualTo("correo.uno@ejemplo.com");
    }

    @Test
    void conElNombreVacioNoSeGuarda() throws Exception {
        Usuario alumno = crear("Alumno Sin Nombre", "sin.nombre@ejemplo.com", Rol.ALUMNO, "DAM");

        mockMvc.perform(post("/admin/usuarios/{id}/editar", alumno.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("nombre", "")
                        .param("email", "sin.nombre@ejemplo.com")
                        .param("cicloId", cicloId("DAM").toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("editarUsuarioForm", "nombre"));

        assertThat(usuarioRepository.findById(alumno.getId()).orElseThrow().getNombre())
                .isEqualTo("Alumno Sin Nombre");
    }

    @Test
    void laCuentaDeAdministradorNoSePuedeEditar() throws Exception {
        Usuario administrador = crear("Administrador De Prueba", "admin.prueba@ejemplo.com", Rol.ADMIN, null);

        mockMvc.perform(get("/admin/usuarios/{id}/editar", administrador.getId())
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("error"));

        mockMvc.perform(post("/admin/usuarios/{id}/editar", administrador.getId()).with(csrf())
                        .with(user(ADMIN).roles("ADMIN"))
                        .param("nombre", "Cambiado")
                        .param("email", "admin.prueba@ejemplo.com")
                        .param("cicloId", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attributeExists("error"));

        assertThat(usuarioRepository.findById(administrador.getId()).orElseThrow().getNombre())
                .isEqualTo("Administrador De Prueba");
    }

    @Test
    void elListadoOfreceElDesplegableDeCiclosYElEnlaceDeEditar() throws Exception {
        crear("Alumno Listado", "listado.admin@ejemplo.com", Rol.ALUMNO, "DAW");

        mockMvc.perform(get("/admin/usuarios").with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sin ciclo")))
                .andExpect(content().string(containsString("Editar")))
                .andExpect(content().string(containsString("/admin/usuarios/")));
    }
}
