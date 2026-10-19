package com.telosabes.telosabes2026.ciclo;

import com.telosabes.telosabes2026.seguridad.Rol;
import com.telosabes.telosabes2026.usuario.Usuario;
import com.telosabes.telosabes2026.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
 * Pruebas de los ciclos formativos y de su eleccion en el registro.
 * Se ejecutan con H2 en memoria, no necesitan MariaDB.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CicloTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Ciclo ciclo(String siglas) {
        return cicloRepository.findBySiglasIgnoreCase(siglas).orElseThrow(
                () -> new AssertionError("No esta dado de alta el ciclo " + siglas));
    }

    @Test
    void losCuatroCiclosEstanDadosDeAlta() {
        List<String> siglas = cicloRepository.findAllByOrderBySiglasAsc()
                .stream().map(Ciclo::getSiglas).toList();

        assertThat(siglas).containsExactly("ASIR", "DAM", "DAW", "SMR");

        assertThat(ciclo("DAM").getNombre()).isEqualTo("Desarrollo de Aplicaciones Multiplataforma");
        assertThat(ciclo("DAW").getNombre()).isEqualTo("Desarrollo de Aplicaciones Web");
        assertThat(ciclo("ASIR").getNombre()).isEqualTo("Administración de Sistemas Informáticos y Redes");
        assertThat(ciclo("SMR").getNombre()).isEqualTo("Sistemas Microinformáticos y Redes");
    }

    @Test
    void elFormularioDeRegistroOfreceLosCuatroCiclos() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ciclo que vas a estudiar")))
                .andExpect(content().string(containsString("ASIR - Administración de Sistemas Informáticos y Redes")))
                .andExpect(content().string(containsString("DAM - Desarrollo de Aplicaciones Multiplataforma")))
                .andExpect(content().string(containsString("DAW - Desarrollo de Aplicaciones Web")))
                .andExpect(content().string(containsString("SMR - Sistemas Microinformáticos y Redes")));
    }

    @Test
    void elRegistroGuardaElCicloElegido() throws Exception {
        Long cicloDaw = ciclo("DAW").getId();

        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Alumno De Daw")
                        .param("email", "daw@ejemplo.com")
                        .param("cicloId", cicloDaw.toString())
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        Usuario creado = usuarioRepository.findByEmailIgnoreCase("daw@ejemplo.com").orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.ALUMNO);
        assertThat(creado.getCiclo()).isNotNull();
        assertThat(creado.getCiclo().getId()).isEqualTo(cicloDaw);
        assertThat(creado.getCiclo().getSiglas()).isEqualTo("DAW");
    }

    @Test
    void sinCicloNoSeRegistraNadie() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Alumno Sin Ciclo")
                        .param("email", "sinciclo@ejemplo.com")
                        // sin cicloId: el desplegable se queda en "Elige tu ciclo..."
                        .param("cicloId", "")
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Elige el ciclo que vas a estudiar")));

        assertThat(usuarioRepository.findByEmailIgnoreCase("sinciclo@ejemplo.com")).isEmpty();
    }

    @Test
    void conUnCicloQueNoExisteNoSeRegistraNadie() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Alumno Inventado")
                        .param("email", "inventado@ejemplo.com")
                        .param("cicloId", "999999")
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ese ciclo no existe")));

        assertThat(usuarioRepository.findByEmailIgnoreCase("inventado@ejemplo.com")).isEmpty();
    }

    @Test
    void elListadoDelAdministradorMuestraElCicloDeCadaAlumno() throws Exception {
        Long cicloSmr = ciclo("SMR").getId();

        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Alumno De Smr")
                        .param("email", "smr@ejemplo.com")
                        .param("cicloId", cicloSmr.toString())
                        .param("clave", "clave1234")
                        .param("claveRepetida", "clave1234"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/usuarios").with(user("admin@ejemplo.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SMR")));
    }
}
