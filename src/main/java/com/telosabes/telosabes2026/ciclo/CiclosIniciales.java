package com.telosabes.telosabes2026.ciclo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Da de alta los ciclos formativos al arrancar la aplicacion.
 *
 * Se ejecuta en cada arranque, pero solo inserta los ciclos que faltan
 * (busca por siglas), asi que se puede arrancar la aplicacion tantas
 * veces como se quiera sin duplicar datos.
 *
 * Va antes que los modulos ({@code @Order(1)}): cada modulo necesita su ciclo.
 */
@Component
@Order(1)
public class CiclosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CiclosIniciales.class);

    /** Siglas -> nombre completo, en el orden en que se muestran. */
    private static final Map<String, String> CICLOS = new LinkedHashMap<>();

    static {
        CICLOS.put("DAM", "Desarrollo de Aplicaciones Multiplataforma");
        CICLOS.put("DAW", "Desarrollo de Aplicaciones Web");
        CICLOS.put("ASIR", "Administración de Sistemas Informáticos y Redes");
        CICLOS.put("SMR", "Sistemas Microinformáticos y Redes");
    }

    private final CicloRepository cicloRepository;

    public CiclosIniciales(CicloRepository cicloRepository) {
        this.cicloRepository = cicloRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        int creados = 0;

        for (Map.Entry<String, String> ciclo : CICLOS.entrySet()) {
            String siglas = ciclo.getKey();
            if (cicloRepository.existsBySiglasIgnoreCase(siglas)) {
                continue;
            }
            cicloRepository.save(new Ciclo(ciclo.getValue(), siglas));
            log.info("Ciclo de alta: {} - {}", siglas, ciclo.getValue());
            creados++;
        }

        if (creados > 0) {
            log.info("Se han creado {} ciclos formativos", creados);
        }
    }
}
