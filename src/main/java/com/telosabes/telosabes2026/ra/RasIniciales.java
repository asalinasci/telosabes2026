package com.telosabes.telosabes2026.ra;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Da de alta los resultados de aprendizaje (RA) oficiales de cada módulo al
 * arrancar la aplicación.
 *
 * Los textos viven en {@code src/main/resources/ras.txt}, en formato
 * {@code GRUPO|CODIGO_MODULO|ORDEN|TEXTO}, y se generan desde el BOE con
 * {@code tools/generar_ras.py}. Cada grupo indica a qué ciclos se aplica:
 *
 * <ul>
 *   <li>{@code DAM}, {@code DAW}, {@code ASIR}, {@code SMR}: módulos propios del
 *       título de ese ciclo (los fija el Real Decreto de su título). El mismo
 *       código puede variar en comas de un título a otro, por eso cada ciclo
 *       toma la fuente de su propio Real Decreto.</li>
 *   <li>{@code TRANS}: módulos transversales de todos los ciclos (1709, 1710 y
 *       1708, del RD 659/2023).</li>
 *   <li>{@code SUPER}: transversales solo de grado superior (1665 y 0179).</li>
 *   <li>{@code MEDIO}: transversales solo de grado medio (1664 y 0156) y el
 *       Proyecto intermodular 1713 (RD 499/2024).</li>
 * </ul>
 *
 * Es idempotente: por cada módulo actualiza los RA que ya existen (buscando
 * por módulo y orden), da de alta los que falten y borra los que ya no estén
 * en el fichero. Se ejecuta después de los módulos ({@code @Order(3)}) porque
 * cada RA necesita su módulo.
 */
@Component
@Order(3)
public class RasIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RasIniciales.class);

    /** Grupo del fichero ras.txt -> siglas de los ciclos donde se aplica. */
    private static final Map<String, List<String>> APLICAR = Map.of(
            "DAM", List.of("DAM"),
            "DAW", List.of("DAW"),
            "ASIR", List.of("ASIR"),
            "SMR", List.of("SMR"),
            "TRANS", List.of("DAM", "DAW", "ASIR", "SMR"),
            "SUPER", List.of("DAM", "DAW", "ASIR"),
            "MEDIO", List.of("SMR"));

    /** Una línea del fichero ras.txt. */
    private record Definicion(String grupo, String codigoModulo, int orden, String texto) {
    }

    private final CicloRepository cicloRepository;
    private final ModuloRepository moduloRepository;
    private final RaRepository raRepository;

    public RasIniciales(CicloRepository cicloRepository,
                        ModuloRepository moduloRepository,
                        RaRepository raRepository) {
        this.cicloRepository = cicloRepository;
        this.moduloRepository = moduloRepository;
        this.raRepository = raRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Definicion> definiciones;
        try {
            definiciones = leerFichero();
        } catch (IOException e) {
            log.error("No se pudo leer ras.txt: {}", e.getMessage());
            return;
        }

        Map<String, Map<String, Modulo>> modulosPorSigla = indexarModulos();

        // Plan: siglas -> módulo -> RA que debe tener
        Map<String, Map<Modulo, List<Definicion>>> plan = new LinkedHashMap<>();
        int sinModulo = 0;
        for (Definicion definicion : definiciones) {
            List<String> siglas = APLICAR.get(definicion.grupo());
            if (siglas == null) {
                log.warn("Grupo desconocido en ras.txt: {}", definicion.grupo());
                continue;
            }
            for (String sigla : siglas) {
                Map<String, Modulo> porCodigo = modulosPorSigla.get(sigla);
                Modulo modulo = porCodigo == null ? null : porCodigo.get(definicion.codigoModulo());
                if (modulo == null) {
                    sinModulo++;
                    continue;
                }
                plan.computeIfAbsent(sigla, clave -> new LinkedHashMap<>())
                        .computeIfAbsent(modulo, clave -> new ArrayList<>())
                        .add(definicion);
            }
        }
        if (sinModulo > 0) {
            log.warn("Hay {} RA sin módulo al que asignar (¿falta un ciclo o un módulo?)", sinModulo);
        }

        int creados = 0;
        int actualizados = 0;
        int borrados = 0;
        int modulosConRa = 0;

        for (Map<Modulo, List<Definicion>> porModulo : plan.values()) {
            for (Map.Entry<Modulo, List<Definicion>> entrada : porModulo.entrySet()) {
                Modulo modulo = entrada.getKey();

                // Última definición de cada orden (no debería haber repetidas)
                Map<Integer, Definicion> deseadas = new LinkedHashMap<>();
                for (Definicion definicion : entrada.getValue()) {
                    deseadas.put(definicion.orden(), definicion);
                }

                Map<Integer, RA> existentes = new HashMap<>();
                for (RA ra : raRepository.findByModuloIdOrderByOrdenAsc(modulo.getId())) {
                    existentes.put(ra.getOrden(), ra);
                }

                for (Definicion definicion : deseadas.values()) {
                    RA ra = existentes.get(definicion.orden());
                    if (ra == null) {
                        raRepository.save(new RA(definicion.orden(), definicion.texto(), modulo));
                        creados++;
                    } else if (!definicion.texto().equals(ra.getDescripcion())) {
                        ra.setDescripcion(definicion.texto());
                        raRepository.save(ra);
                        actualizados++;
                    }
                }

                for (RA ra : existentes.values()) {
                    if (!deseadas.containsKey(ra.getOrden())) {
                        raRepository.delete(ra);
                        borrados++;
                    }
                }

                if (!deseadas.isEmpty()) {
                    modulosConRa++;
                }
            }
        }

        if (creados > 0 || actualizados > 0 || borrados > 0) {
            log.info("Resultados de aprendizaje: {} creados, {} actualizados, {} borrados",
                    creados, actualizados, borrados);
        }
        log.info("{} resultados de aprendizaje en {} módulos de 4 ciclos",
                raRepository.count(), modulosConRa);
    }

    /** Módulos de cada ciclo con RA, indexados por su código oficial. */
    private Map<String, Map<String, Modulo>> indexarModulos() {
        Map<String, Map<String, Modulo>> porSigla = new LinkedHashMap<>();

        Set<String> todos = new LinkedHashSet<>();
        for (List<String> siglas : APLICAR.values()) {
            todos.addAll(siglas);
        }

        for (String sigla : todos) {
            Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase(sigla).orElse(null);
            if (ciclo == null) {
                log.warn("No existe el ciclo {}: no se dan de alta sus RA", sigla);
                continue;
            }
            Map<String, Modulo> porCodigo = new HashMap<>();
            for (Modulo modulo : moduloRepository.findByCicloIdOrderByCursoAscOrdenAsc(ciclo.getId())) {
                if (modulo.getCodigo() != null) {
                    porCodigo.put(modulo.getCodigo(), modulo);
                }
            }
            porSigla.put(sigla, porCodigo);
        }
        return porSigla;
    }

    /** Lee y valida las líneas de {@code ras.txt}. */
    private List<Definicion> leerFichero() throws IOException {
        List<Definicion> definiciones = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ClassPathResource("ras.txt").getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                String[] campos = linea.split("\\|", 4);
                if (campos.length != 4) {
                    log.warn("Línea {} de ras.txt con {} campos: se ignora", numero, campos.length);
                    continue;
                }
                try {
                    definiciones.add(new Definicion(campos[0].trim(), campos[1].trim(),
                            Integer.parseInt(campos[2].trim()), campos[3].trim()));
                } catch (NumberFormatException e) {
                    log.warn("Orden no numérico en la línea {} de ras.txt", numero);
                }
            }
        }

        return definiciones;
    }
}
