package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import com.telosabes.telosabes2026.usuario.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la consulta de RA y criterios por modulo (/modulos/estudio),
 * la pantalla comoda para alumnos y profesores.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EstudioTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void sinSesionNoSePuedeEntrarEnElTemario() throws Exception {
        mvc.perform(get("/modulos/estudio"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unAlumnoVeElTemarioDeSuCiclo() throws Exception {
        Ciclo dam = cicloRepository.findBySiglasIgnoreCase("DAM").orElseThrow();

        Usuario alumno = new Usuario("Alumno de prueba", "alumno.estudio@telosabes.local",
                passwordEncoder.encode("clave-de-prueba"), Rol.ALUMNO, dam);
        usuarioRepository.save(alumno);

        // Al entrar sin elegir ciclo, sale el ciclo que estudia el alumno
        mvc.perform(get("/modulos/estudio").with(user(alumno.getEmail()).roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resultados de aprendizaje y criterios")))
                .andExpect(content().string(containsString("0485")))
                .andExpect(content().string(containsString("Reconoce la estructura de un programa informático")))
                .andExpect(content().string(containsString("Se han identificado los bloques que componen")))
                .andExpect(content().string(containsString("class=\"letra-criterio\"")))
                // los modulos propios de otro ciclo no salen
                .andExpect(content().string(not(containsString("Montaje y mantenimiento de equipos"))))
                .andExpect(content().string(not(containsString("0221"))));
    }

    @Test
    void elAlumnoPuedeCambiarDeCicloConElDesplegable() throws Exception {
        Ciclo smr = cicloRepository.findBySiglasIgnoreCase("SMR").orElseThrow();

        mvc.perform(get("/modulos/estudio")
                        .param("cicloId", String.valueOf(smr.getId()))
                        .with(user("alumno.estudio@telosabes.local").roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0221")))
                .andExpect(content().string(not(containsString("Programación multimedia y dispositivos móviles"))));
    }

    @Test
    void unCicloQueNoExisteMuestraElCicloDelAlumno() throws Exception {
        mvc.perform(get("/modulos/estudio")
                        .param("cicloId", "999999")
                        .with(user("alumno.estudio@telosabes.local").roles("ALUMNO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0485")));
    }

    @Test
    void unProfesorVeElTemarioDeCualquierCiclo() throws Exception {
        Ciclo asir = cicloRepository.findBySiglasIgnoreCase("ASIR").orElseThrow();

        Usuario profesor = new Usuario("Profesor de prueba", "profesor.estudio@telosabes.local",
                passwordEncoder.encode("clave-de-prueba"), Rol.PROFESOR);
        usuarioRepository.save(profesor);

        mvc.perform(get("/modulos/estudio")
                        .param("cicloId", String.valueOf(asir.getId()))
                        .with(user(profesor.getEmail()).roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0369")))
                .andExpect(content().string(containsString("class=\"letra-criterio\"")));
    }

    @Test
    void elModuloOptativoNoTieneTemario() throws Exception {
        // El 4º optativo de SMR no tiene resultados de aprendizaje oficiales
        mvc.perform(get("/modulos/estudio")
                        .param("cicloId", String.valueOf(cicloRepository
                                .findBySiglasIgnoreCase("SMR").orElseThrow().getId()))
                        .with(user("profesor.estudio@telosabes.local").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("es el modulo")))
                .andExpect(content().string(containsString("optativo")));
    }

    @Test
    void laCabeceraEnlazaConElTemario() throws Exception {
        mvc.perform(get("/").with(user("profesor.estudio@telosabes.local").roles("PROFESOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/modulos/estudio")));
    }
}
