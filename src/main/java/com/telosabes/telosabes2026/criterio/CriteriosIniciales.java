package com.telosabes.telosabes2026.criterio;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import com.telosabes.telosabes2026.modulo.Modulo;
import com.telosabes.telosabes2026.modulo.ModuloRepository;
import com.telosabes.telosabes2026.ra.RA;
import com.telosabes.telosabes2026.ra.RaRepository;
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
 * Da de alta los criterios de evaluación oficiales de cada resultado de
 * aprendizaje al arrancar la aplicación.
 *
 * Los textos viven en {@code src/main/resources/criterios.txt}, en formato
 * {@code GRUPO|CODIGO_MODULO|ORDEN_RA|ORDEN|TEXTO}, y se generan desde el BOE
 * con {@code tools/generar_ras.py} (el mismo script que los RA, porque en el
 * Boletín van juntos). El orden de las letras de la tabla oficial es 1 = «a)»,
 * 2 = «b)», 3 = «c)»... y los grupos (a qué ciclos se aplica cada línea) son
 * los mismos que en {@code ras.txt}.
 *
 * Es idempotente: por cada RA actualiza los criterios que ya existen (buscando
 * por RA y orden), da de alta los que falten y borra los que ya no estén en el
 * fichero. Se ejecuta después de los RA ({@code @Order(4)}) porque cada
 * criterio necesita su resultado de aprendizaje.
 */
@Component
@Order(4)
public class CriteriosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriteriosIniciales.class);

    /** Grupo del fichero criterios.txt -> siglas de los ciclos donde se aplica. */
    private static final Map<String, List<String>> APLICAR = Map.of(
            "DAM", List.of("DAM"),
            "DAW", List.of("DAW"),
            "ASIR", List.of("ASIR"),
            "SMR", List.of("SMR"),
            "TRANS", List.of("DAM", "DAW", "ASIR", "SMR"),
            "SUPER", List.of("DAM", "DAW", "ASIR"),
            "MEDIO", List.of("SMR"));

    /** Una línea del fichero criterios.txt. */
    private record Definicion(String grupo, String codigoModulo, int ordenRa, int orden,
                              String texto) {
    }

    private final CicloRepository cicloRepository;
    private final ModuloRepository moduloRepository;
    private final RaRepository raRepository;
    private final CriterioRepository criterioRepository;

    public CriteriosIniciales(CicloRepository cicloRepository,
                              ModuloRepository moduloRepository,
                              RaRepository raRepository,
                              CriterioRepository criterioRepository) {
        this.cicloRepository = cicloRepository;
        this.moduloRepository = moduloRepository;
        this.raRepository = raRepository;
        this.criterioRepository = criterioRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Definicion> definiciones;
        try {
            definiciones = leerFichero();
        } catch (IOException e) {
            log.error("No se pudo leer criterios.txt: {}", e.getMessage());
            return;
        }

        Map<String, Map<String, Modulo>> modulosPorSigla = indexarModulos();

        // Plan: siglas -> módulo -> orden del RA -> criterios que debe tener
        Map<String, Map<Modulo, Map<Integer, List<Definicion>>>> plan = new LinkedHashMap<>();
        int sinRa = 0;
        for (Definicion definicion : definiciones) {
            List<String> siglas = APLICAR.get(definicion.grupo());
            if (siglas == null) {
                log.warn("Grupo desconocido en criterios.txt: {}", definicion.grupo());
                continue;
            }
            for (String sigla : siglas) {
                Map<String, Modulo> porCodigo = modulosPorSigla.get(sigla);
                Modulo modulo = porCodigo == null ? null : porCodigo.get(definicion.codigoModulo());
                if (modulo == null) {
                    sinRa++;
                    continue;
                }
                plan.computeIfAbsent(sigla, clave -> new LinkedHashMap<>())
                        .computeIfAbsent(modulo, clave -> new LinkedHashMap<>())
                        .computeIfAbsent(definicion.ordenRa(), clave -> new ArrayList<>())
                        .add(definicion);
            }
        }
        if (sinRa > 0) {
            log.warn("Hay {} criterios sin módulo al que asignar (¿falta un ciclo o un módulo?)",
                    sinRa);
        }

        int creados = 0;
        int actualizados = 0;
        int borrados = 0;
        int rasConCriterios = 0;

        for (Map<Modulo, Map<Integer, List<Definicion>>> porModulo : plan.values()) {
            for (Map.Entry<Modulo, Map<Integer, List<Definicion>>> entrada : porModulo.entrySet()) {
                Modulo modulo = entrada.getKey();

                Map<Integer, RA> ras = new HashMap<>();
                for (RA ra : raRepository.findByModuloIdOrderByOrdenAsc(modulo.getId())) {
                    ras.put(ra.getOrden(), ra);
                }

                for (Map.Entry<Integer, List<Definicion>> porRa : entrada.getValue().entrySet()) {
                    RA ra = ras.get(porRa.getKey());
                    if (ra == null) {
                        log.warn("El módulo {} del ciclo no tiene el RA {}: se saltan sus criterios",
                                modulo.getNombre(), porRa.getKey());
                        continue;
                    }

                    // Última definición de cada orden (no debería haber repetidas)
                    Map<Integer, Definicion> deseadas = new LinkedHashMap<>();
                    for (Definicion definicion : porRa.getValue()) {
                        deseadas.put(definicion.orden(), definicion);
                    }

                    Map<Integer, Criterio> existentes = new HashMap<>();
                    for (Criterio criterio : criterioRepository.findByRaIdOrderByOrdenAsc(ra.getId())) {
                        existentes.put(criterio.getOrden(), criterio);
                    }

                    for (Definicion definicion : deseadas.values()) {
                        Criterio criterio = existentes.get(definicion.orden());
                        if (criterio == null) {
                            criterioRepository.save(new Criterio(definicion.orden(),
                                    definicion.texto(), ra));
                            creados++;
                        } else if (!definicion.texto().equals(criterio.getDescripcion())) {
                            criterio.setDescripcion(definicion.texto());
                            criterioRepository.save(criterio);
                            actualizados++;
                        }
                    }

                    for (Criterio criterio : existentes.values()) {
                        if (!deseadas.containsKey(criterio.getOrden())) {
                            criterioRepository.delete(criterio);
                            borrados++;
                        }
                    }

                    if (!deseadas.isEmpty()) {
                        rasConCriterios++;
                    }
                }
            }
        }

        if (creados > 0 || actualizados > 0 || borrados > 0) {
            log.info("Criterios de evaluación: {} creados, {} actualizados, {} borrados",
                    creados, actualizados, borrados);
        }
        log.info("{} criterios de evaluación en {} resultados de aprendizaje",
                criterioRepository.count(), rasConCriterios);
    }

    /** Módulos de cada ciclo con criterios, indexados por su código oficial. */
    private Map<String, Map<String, Modulo>> indexarModulos() {
        Map<String, Map<String, Modulo>> porSigla = new LinkedHashMap<>();

        Set<String> todos = new LinkedHashSet<>();
        for (List<String> siglas : APLICAR.values()) {
            todos.addAll(siglas);
        }

        for (String sigla : todos) {
            Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase(sigla).orElse(null);
            if (ciclo == null) {
                log.warn("No existe el ciclo {}: no se dan de alta sus criterios", sigla);
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

    /** Lee y valida las líneas de {@code criterios.txt}. */
    private List<Definicion> leerFichero() throws IOException {
        List<Definicion> definiciones = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ClassPathResource("criterios.txt").getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            int numero = 0;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                String[] campos = linea.split("\\|", 5);
                if (campos.length != 5) {
                    log.warn("Línea {} de criterios.txt con {} campos: se ignora",
                            numero, campos.length);
                    continue;
                }
                try {
                    definiciones.add(new Definicion(campos[0].trim(), campos[1].trim(),
                            Integer.parseInt(campos[2].trim()),
                            Integer.parseInt(campos[3].trim()), campos[4].trim()));
                } catch (NumberFormatException e) {
                    log.warn("Orden no numérico en la línea {} de criterios.txt", numero);
                }
            }
        }

        return definiciones;
    }
}
