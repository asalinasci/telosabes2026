package com.telosabes.telosabes2026.ciclo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ciclo formativo que puede estudiar un alumno (tabla "ciclos").
 *
 * Los cuatro ciclos que usa la aplicacion se dan de alta automaticamente
 * al arrancar (ver {@link CiclosIniciales}).
 */
@Entity
@Table(name = "ciclos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ciclos_siglas", columnNames = "siglas"),
                @UniqueConstraint(name = "uk_ciclos_nombre", columnNames = "nombre")
        })
public class Ciclo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del ciclo es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nombre;

    /** Siglas oficiales del ciclo: DAM, DAW, ASIR, SMR... */
    @NotBlank(message = "Las siglas del ciclo son obligatorias")
    @Size(max = 10, message = "Las siglas no pueden superar los 10 caracteres")
    @Column(nullable = false, length = 10)
    private String siglas;

    protected Ciclo() {
        // Requerido por JPA
    }

    public Ciclo(String nombre, String siglas) {
        this.nombre = nombre;
        this.siglas = siglas;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSiglas() {
        return siglas;
    }

    public void setSiglas(String siglas) {
        this.siglas = siglas;
    }

    /** Texto para listados desplegables: "DAM - Desarrollo de Aplicaciones Multiplataforma". */
    public String getEtiqueta() {
        return siglas + " - " + nombre;
    }

    @Override
    public String toString() {
        return getEtiqueta();
    }
}
