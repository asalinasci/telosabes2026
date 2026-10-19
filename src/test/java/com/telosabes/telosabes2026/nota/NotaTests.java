package com.telosabes.telosabes2026.nota;

import com.telosabes.telosabes2026.seguridad.Rol;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del autor automatico de las notas.
 * Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotaTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotaRepository notaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario crearUsuario(String nombre, String email, Rol rol) {
        return usuarioRepository.save(
                new Usuario(nombre, email, passwordEncoder.encode("clave1234"), rol));
    }

    /** Busca una nota por titulo con su autor ya cargado. */
    private Nota buscarPorTitulo(String titulo) {
        return notaRepository.findAllByOrderByCreadoEnDesc().stream()
                .filter(nota -> titulo.equals(nota.getTitulo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se guardo la nota \"" + titulo + "\""));
    }

    @Test
    void laNotaGuardaComoAutorAlUsuarioDeLaSesion() throws Exception {
        Usuario profesor = crearUsuario("Profe Autor", "autor@ejemplo.com", Rol.PROFESOR);

        mockMvc.perform(post("/notas").with(csrf())
                        .with(user("autor@ejemplo.com").roles("PROFESOR"))
                        .param("titulo", "Nota del profe")
                        .param("contenido", "Contenido de prueba"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notas"));

        Nota nota = notaRepository.findAllByOrderByCreadoEnDesc().stream()
                .filter(n -> "Nota del profe".equals(n.getTitulo()))
                .findFirst().orElseThrow();

        assertThat(nota.getAutor()).isNotNull();
        assertThat(nota.getAutor().getId()).isEqualTo(profesor.getId());
        assertThat(nota.getAutor().getEmail()).isEqualTo("autor@ejemplo.com");
        assertThat(nota.getAutor().getNombre()).isEqualTo("Profe Autor");
    }

    @Test
    void elAutorNoSePuedeFalsificarDesdeElFormulario() throws Exception {
        crearUsuario("Alumno Real", "alumno.real@ejemplo.com", Rol.ALUMNO);
        Usuario victima = crearUsuario("Profesor Victima", "victima@ejemplo.com", Rol.PROFESOR);

        mockMvc.perform(post("/notas").with(csrf())
                        .with(user("alumno.real@ejemplo.com").roles("ALUMNO"))
                        .param("titulo", "Nota con autor falsificado")
                        .param("contenido", "Intento de suplantacion")
                        // intentos de colar el autor a mano: no existen en NotaForm
                        .param("autor", victima.getId().toString())
                        .param("autor.id", victima.getId().toString())
                        .param("autor_id", victima.getId().toString()))
                .andExpect(status().is3xxRedirection());

        Nota nota = notaRepository.findAllByOrderByCreadoEnDesc().stream()
                .filter(n -> "Nota con autor falsificado".equals(n.getTitulo()))
                .findFirst().orElseThrow();

        assertThat(nota.getAutor().getEmail()).isEqualTo("alumno.real@ejemplo.com");
        assertThat(nota.getAutor().getId()).isNotEqualTo(victima.getId());
    }

    @Test
    void elListadoYElFormularioMuestranElAutor() throws Exception {
        crearUsuario("Elena Profesora", "elena@ejemplo.com", Rol.PROFESOR);

        mockMvc.perform(post("/notas").with(csrf())
                        .with(user("elena@ejemplo.com").roles("PROFESOR"))
                        .param("titulo", "Nota visible")
                        .param("contenido", "Contenido visible"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/notas").with(user("elena@ejemplo.com").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nota visible")))
                .andExpect(content().string(containsString("Elena Profesora")))
                // el formulario avisa de quien sera el autor
                .andExpect(content().string(containsString("Se guardar")))
                .andExpect(content().string(containsString("Autor")));
    }

    @Test
    void unaNotaSinTituloNoSeGuarda() throws Exception {
        crearUsuario("Pepito Alumno", "pepito@ejemplo.com", Rol.ALUMNO);
        long antes = notaRepository.count();

        mockMvc.perform(post("/notas").with(csrf())
                        .with(user("pepito@ejemplo.com").roles("ALUMNO"))
                        .param("titulo", "")
                        .param("contenido", "Sin titulo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El titulo es obligatorio")));

        assertThat(notaRepository.count()).isEqualTo(antes);
    }

    @Test
    void sinSesionNoSePuedeCrearUnaNota() throws Exception {
        mockMvc.perform(post("/notas").with(csrf())
                        .param("titulo", "Nota anonima")
                        .param("contenido", "No deberia guardarse"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(notaRepository.findAll())
                .noneMatch(nota -> "Nota anonima".equals(nota.getTitulo()));
    }
}
