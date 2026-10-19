package com.telosabes.telosabes2026.seguridad;

import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del sistema de seguridad (roles, registro y promocion).
 * Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Id del ciclo DAM, que siempre esta dado de alta al arrancar. */
    private String cicloDam() {
        return cicloRepository.findBySiglasIgnoreCase("DAM").orElseThrow().getId().toString();
    }

    private Usuario crearUsuario(String nombre, String email, Rol rol, String claveEnClaro) {
        return usuarioRepository.save(
                new Usuario(nombre, email, passwordEncoder.encode(claveEnClaro), rol));
    }

    // ------------------------------------------------------------------
    //  Paginas publicas
    // ------------------------------------------------------------------

    @Test
    void laPaginaDeLoginEsPublica() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesion")));
    }

    @Test
    void elFormularioDeRegistroEsPublico() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registro de alumnos")));
    }

    // ------------------------------------------------------------------
    //  Control de acceso por roles
    // ------------------------------------------------------------------

    @Test
    void sinSesionNoSePuedeEntrarEnLaZonaDeAdministracion() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void unAlumnoNoPuedeEntrarEnLaZonaDeAdministracion() throws Exception {
        mockMvc.perform(get("/admin/usuarios").with(user("alumno@ejemplo.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAlumnoNoPuedeEntrarEnLaZonaDeProfesor() throws Exception {
        mockMvc.perform(get("/profesor").with(user("alumno@ejemplo.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unProfesorSiPuedeEntrarEnLaZonaDeProfesor() throws Exception {
        mockMvc.perform(get("/profesor").with(user("profe@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Panel del profesor")));
    }

    // ------------------------------------------------------------------
    //  Registro publico
    // ------------------------------------------------------------------

    @Test
    void elRegistroCreaUnAlumnoConLaClaveCifrada() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Alumno Nuevo")
                        .param("email", "Nuevo@Ejemplo.com")
                        .param("cicloId", cicloDam())
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        Usuario creado = usuarioRepository.findByEmailIgnoreCase("nuevo@ejemplo.com").orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.ALUMNO);
        assertThat(creado.getEmail()).isEqualTo("nuevo@ejemplo.com");
        // El ciclo elegido se guarda en la ficha del alumno
        assertThat(creado.getCiclo().getSiglas()).isEqualTo("DAM");
        // La contraseña nunca se guarda en claro
        assertThat(creado.getClave()).isNotEqualTo("clave1234").startsWith("$2");
    }

    @Test
    void elRegistroNoPermiteElegirElRolDesdeElFormulario() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Listillo")
                        .param("email", "listillo@ejemplo.com")
                        .param("cicloId", cicloDam())
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234")
                        // intento de manipulacion: no existe en RegistroForm
                        .param("rol", "ADMIN"))
                .andExpect(status().is3xxRedirection());

        Usuario creado = usuarioRepository.findByEmailIgnoreCase("listillo@ejemplo.com").orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.ALUMNO);
    }

    @Test
    void elRegistroRechazaCorreosRepetidosYClavesDistintas() throws Exception {
        crearUsuario("Ya Existe", "repetido@ejemplo.com", Rol.ALUMNO, "clave1234");

        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Otro")
                        .param("email", "repetido@ejemplo.com")
                        .param("cicloId", cicloDam())
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ya esta registrado")));

        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Otro Mas")
                        .param("email", "otro@ejemplo.com")
                        .param("cicloId", cicloDam())
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no coinciden")));

        assertThat(usuarioRepository.findByEmailIgnoreCase("otro@ejemplo.com")).isEmpty();
    }

    // ------------------------------------------------------------------
    //  Login
    // ------------------------------------------------------------------

    @Test
    void sePuedeIniciarSesionConElCorreoYLaClave() throws Exception {
        crearUsuario("Usuario Login", "login@ejemplo.com", Rol.ALUMNO, "clave1234");

        mockMvc.perform(formLogin("/login")
                        .userParameter("email")
                        .passwordParam("clave")
                        .user("login@ejemplo.com")
                        .password("clave1234"))
                .andExpect(authenticated().withUsername("login@ejemplo.com"));

        mockMvc.perform(formLogin("/login")
                        .userParameter("email")
                        .passwordParam("clave")
                        .user("login@ejemplo.com")
                        .password("claveIncorrecta"))
                .andExpect(unauthenticated());
    }

    // ------------------------------------------------------------------
    //  Zona del administrador
    // ------------------------------------------------------------------

    @Test
    void elListadoDelAdministradorNoMuestraAlAdministrador() throws Exception {
        crearUsuario("Alumno Visible", "visible@ejemplo.com", Rol.ALUMNO, "clave1234");
        crearUsuario("Admin Oculto", "oculto@ejemplo.com", Rol.ADMIN, "clave1234");

        mockMvc.perform(get("/admin/usuarios").with(user("admin@ejemplo.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("visible@ejemplo.com")))
                .andExpect(content().string(not(containsString("oculto@ejemplo.com"))));
    }

    @Test
    void elAdministradorConvierteUnAlumnoEnProfesor() throws Exception {
        Usuario alumno = crearUsuario("Alumno Promovido", "promover@ejemplo.com", Rol.ALUMNO, "clave1234");

        mockMvc.perform(post("/admin/usuarios/" + alumno.getId() + "/promover")
                        .with(csrf())
                        .with(user("admin@ejemplo.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        assertThat(usuarioRepository.findById(alumno.getId()).orElseThrow().getRol())
                .isEqualTo(Rol.PROFESOR);
    }

    @Test
    void unAlumnoNoPuedePromoverseASiMismo() throws Exception {
        Usuario alumno = crearUsuario("Alumno Listillo", "listillo2@ejemplo.com", Rol.ALUMNO, "clave1234");

        mockMvc.perform(post("/admin/usuarios/" + alumno.getId() + "/promover")
                        .with(csrf())
                        .with(user("listillo2@ejemplo.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.findById(alumno.getId()).orElseThrow().getRol())
                .isEqualTo(Rol.ALUMNO);
    }

    @Test
    void elAdministradorNoSePuedePromoverNiModificar() throws Exception {
        Usuario admin = crearUsuario("Admin Oculto", "admin2@ejemplo.com", Rol.ADMIN, "clave1234");

        mockMvc.perform(post("/admin/usuarios/" + admin.getId() + "/promover")
                        .with(csrf())
                        .with(user("admin2@ejemplo.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios"));

        assertThat(usuarioRepository.findById(admin.getId()).orElseThrow().getRol())
                .isEqualTo(Rol.ADMIN);
    }
}
