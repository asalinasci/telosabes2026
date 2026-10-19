package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
import com.telosabes.telosabes2026.ciclo.CicloRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Da de alta los módulos profesionales de cada ciclo al arrancar la aplicación.
 *
 * Los datos son los de las tablas "Aspectos organizativos y curriculares" de la
 * Consejería de Educación y Formación Profesional de la Región de Murcia para
 * los ciclos de grado medio y de grado superior (curso 2025/2026, nuevo sistema
 * de Formación Profesional). Cada ciclo tiene 2000 horas repartidas en dos
 * cursos: la parte troncal obligatoria (módulos del catálogo, módulos
 * transversales y proyecto intermodular) y el módulo optativo.
 *
 * Igual que {@link com.telosabes.telosabes2026.ciclo.CiclosIniciales}, solo
 * inserta lo que falta, así que se puede arrancar la aplicación tantas veces
 * como se quiera sin duplicar datos. Se ejecuta después de los ciclos
 * ({@code @Order(2)}) porque cada módulo necesita su ciclo.
 */
@Component
@Order(2)
public class ModulosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModulosIniciales.class);

    /**
     * Un módulo de la tabla oficial.
     *
     * @param codigo          código del módulo (null en el módulo optativo)
     * @param nombre          nombre del módulo
     * @param curso           1 o 2
     * @param horasSemanales  horas a la semana en el centro
     * @param horasCentro     horas que se imparten en el centro
     * @param horasEmpresa    horas de la fase de formación en la empresa
     */
    private record Definicion(String codigo, String nombre, int curso,
                              int horasSemanales, int horasCentro, int horasEmpresa) {
    }

    /** Siglas del ciclo -> módulos, en el orden de la tabla oficial. */
    private static final Map<String, List<Definicion>> CICLOS = new LinkedHashMap<>();

    static {
        // ------------------------- DAM -------------------------
        CICLOS.put("DAM", List.of(
                // Primer curso
                new Definicion("0373", "Lenguajes de marcas y sistemas de gestión de información", 1, 4, 110, 25),
                new Definicion("0483", "Sistemas informáticos", 1, 5, 140, 35),
                new Definicion("0484", "Bases de datos", 1, 5, 140, 35),
                new Definicion("0485", "Programación", 1, 7, 195, 45),
                new Definicion("0487", "Entornos de desarrollo", 1, 3, 85, 20),
                new Definicion("1709", "Itinerario personal para la empleabilidad I", 1, 3, 90, 0),
                new Definicion("1665", "Digitalización aplicada a los sectores productivos", 1, 1, 30, 0),
                new Definicion("0179", "Inglés profesional", 1, 2, 70, 0),
                // Segundo curso
                new Definicion("0486", "Acceso a datos", 2, 5, 95, 65),
                new Definicion("0488", "Desarrollo de interfaces", 2, 6, 110, 80),
                new Definicion("0489", "Programación multimedia y dispositivos móviles", 2, 4, 80, 50),
                new Definicion("0490", "Programación de servicios y procesos", 2, 4, 80, 50),
                new Definicion("0491", "Sistemas de gestión empresarial", 2, 5, 95, 65),
                new Definicion(null, "Módulo optativo", 2, 2, 50, 30),
                new Definicion("1710", "Itinerario personal para la empleabilidad II", 2, 2, 50, 0),
                new Definicion("1708", "Sostenibilidad aplicada al sistema productivo", 2, 1, 30, 0),
                new Definicion("0492", "Proyecto intermodular de desarrollo de aplicaciones multiplataforma", 2, 1, 50, 0)));

        // ------------------------- DAW -------------------------
        CICLOS.put("DAW", List.of(
                // Primer curso (los mismos módulos que en DAM)
                new Definicion("0373", "Lenguajes de marcas y sistemas de gestión de información", 1, 4, 110, 25),
                new Definicion("0483", "Sistemas informáticos", 1, 5, 140, 35),
                new Definicion("0484", "Bases de datos", 1, 5, 140, 35),
                new Definicion("0485", "Programación", 1, 7, 195, 45),
                new Definicion("0487", "Entornos de desarrollo", 1, 3, 85, 20),
                new Definicion("1709", "Itinerario personal para la empleabilidad I", 1, 3, 90, 0),
                new Definicion("1665", "Digitalización aplicada a los sectores productivos", 1, 1, 30, 0),
                new Definicion("0179", "Inglés profesional", 1, 2, 70, 0),
                // Segundo curso
                new Definicion("0612", "Desarrollo web en entorno cliente", 2, 6, 115, 80),
                new Definicion("0613", "Desarrollo web en entorno servidor", 2, 8, 150, 100),
                new Definicion("0614", "Despliegue de aplicaciones web", 2, 4, 80, 50),
                new Definicion("0615", "Diseño de interfaces web", 2, 6, 115, 80),
                new Definicion(null, "Módulo optativo", 2, 2, 50, 30),
                new Definicion("1708", "Sostenibilidad aplicada al sistema productivo", 2, 1, 30, 0),
                new Definicion("1710", "Itinerario personal para la empleabilidad II", 2, 2, 50, 0),
                new Definicion("0616", "Proyecto intermodular de desarrollo de aplicaciones web", 2, 1, 50, 0)));

        // ------------------------- ASIR -------------------------
        CICLOS.put("ASIR", List.of(
                // Primer curso
                new Definicion("0369", "Implantación de sistemas operativos", 1, 7, 195, 45),
                new Definicion("0370", "Planificación y administración de redes", 1, 5, 140, 35),
                new Definicion("0371", "Fundamentos de hardware", 1, 3, 85, 20),
                new Definicion("0372", "Gestión de bases de datos", 1, 5, 140, 35),
                new Definicion("0373", "Lenguajes de marcas y sistemas de gestión de información", 1, 4, 110, 25),
                new Definicion("1709", "Itinerario personal para la empleabilidad I", 1, 3, 90, 0),
                new Definicion("1665", "Digitalización aplicada a los sectores productivos", 1, 1, 30, 0),
                new Definicion("0179", "Inglés profesional", 1, 2, 70, 0),
                // Segundo curso
                new Definicion("0374", "Administración de sistemas operativos", 2, 7, 135, 95),
                new Definicion("0375", "Servicios de red e internet", 2, 6, 115, 80),
                new Definicion("0376", "Implantación de aplicaciones web", 2, 4, 80, 50),
                new Definicion("0377", "Administración de sistemas gestores de bases de datos", 2, 3, 55, 35),
                new Definicion("0378", "Seguridad y alta disponibilidad", 2, 4, 75, 50),
                new Definicion(null, "Módulo optativo", 2, 2, 50, 30),
                new Definicion("1710", "Itinerario personal para la empleabilidad II", 2, 2, 50, 0),
                new Definicion("1708", "Sostenibilidad aplicada al sistema productivo", 2, 1, 30, 0),
                new Definicion("0379", "Proyecto intermodular de administración de sistemas informáticos en red", 2, 1, 50, 0)));

        // ------------------------- SMR -------------------------
        CICLOS.put("SMR", List.of(
                // Primer curso
                new Definicion("0221", "Montaje y mantenimiento de equipos", 1, 7, 190, 45),
                new Definicion("0222", "Sistemas operativos monopuesto", 1, 5, 140, 35),
                new Definicion("0223", "Aplicaciones ofimáticas", 1, 6, 170, 40),
                new Definicion("0225", "Redes locales", 1, 6, 170, 40),
                new Definicion("1709", "Itinerario personal para la empleabilidad I", 1, 3, 90, 0),
                new Definicion("1664", "Digitalización aplicada a los sectores productivos", 1, 1, 30, 0),
                new Definicion("0156", "Inglés profesional", 1, 2, 70, 0),
                // Segundo curso
                new Definicion("0224", "Sistemas operativos en red", 2, 8, 140, 100),
                new Definicion("0226", "Seguridad informática", 2, 5, 100, 65),
                new Definicion("0227", "Servicios en red", 2, 7, 140, 95),
                new Definicion("0228", "Aplicaciones web", 2, 4, 80, 50),
                new Definicion(null, "Módulo optativo", 2, 2, 50, 30),
                new Definicion("1710", "Itinerario personal para la empleabilidad II", 2, 2, 50, 0),
                new Definicion("1708", "Sostenibilidad aplicada al sistema productivo", 2, 1, 30, 0),
                new Definicion("1713", "Proyecto intermodular", 2, 1, 50, 0)));
    }

    private final CicloRepository cicloRepository;
    private final ModuloRepository moduloRepository;

    public ModulosIniciales(CicloRepository cicloRepository, ModuloRepository moduloRepository) {
        this.cicloRepository = cicloRepository;
        this.moduloRepository = moduloRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        int creados = 0;

        for (Map.Entry<String, List<Definicion>> entrada : CICLOS.entrySet()) {
            String siglas = entrada.getKey();

            Ciclo ciclo = cicloRepository.findBySiglasIgnoreCase(siglas).orElse(null);
            if (ciclo == null) {
                log.warn("No existe el ciclo {}: no se dan de alta sus módulos", siglas);
                continue;
            }

            List<Definicion> definiciones = entrada.getValue();
            for (int i = 0; i < definiciones.size(); i++) {
                Definicion definicion = definiciones.get(i);

                if (moduloRepository.existsByCicloIdAndNombre(ciclo.getId(), definicion.nombre())) {
                    continue;
                }

                moduloRepository.save(new Modulo(
                        definicion.codigo(),
                        definicion.nombre(),
                        definicion.curso(),
                        i + 1,
                        definicion.horasSemanales(),
                        definicion.horasCentro(),
                        definicion.horasEmpresa(),
                        ciclo));
                creados++;
            }

            log.info("Ciclo {}: {} módulos de alta", siglas, definiciones.size());
        }

        if (creados > 0) {
            log.info("Se han creado {} módulos profesionales", creados);
        }
    }
}
