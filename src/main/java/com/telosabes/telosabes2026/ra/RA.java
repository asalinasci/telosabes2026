package com.telosabes.telosabes2026.ra;

import com.telosabes.telosabes2026.modulo.Modulo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Resultado de aprendizaje (RA) de un módulo profesional (tabla "ras").
 *
 * Los textos son los oficiales. Los módulos propios de cada título los fija el
 * Real Decreto que establece ese título (RD 450/2010 en DAM, RD 686/2010 en
 * DAW, RD 1629/2009 en ASIR y RD 1691/2007 en SMR) y los módulos transversales
 * el RD 659/2023, salvo el Proyecto intermodular de grado medio (RD 499/2024).
 * Por eso el mismo código puede tener redacciones ligeramente distintas según
 * el ciclo: cada módulo guarda los RA de la fuente de su propio título.
 *
 * Se dan de alta automáticamente al arrancar (ver {@link RasIniciales}).
 *
 * Al igual que en el resto del proyecto, la relación es {@code @ManyToOne}
 * desde el RA hacia el módulo: no hay {@code @OneToMany} en {@link Modulo}.
 */
@Entity
@Table(name = "ras",
        uniqueConstraints = @UniqueConstraint(name = "uq_ra_modulo_orden",
                columnNames = {"modulo_id", "orden"}))
public class RA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Posición del resultado de aprendizaje dentro del módulo, empezando en 1
     * (el 1 del "1." de la tabla oficial).
     */
    @Min(value = 1, message = "El orden debe ser 1 o mayor")
    @Column(nullable = false)
    private Integer orden;

    /** Texto oficial completo del resultado de aprendizaje. */
    @Size(max = 2000, message = "El resultado de aprendizaje no puede superar los 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    /** Módulo profesional al que pertenece el resultado de aprendizaje. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ra_modulo"))
    private Modulo modulo;

    protected RA() {
        // Requerido por JPA
    }

    public RA(Integer orden, String descripcion, Modulo modulo) {
        this.orden = orden;
        this.descripcion = descripcion;
        this.modulo = modulo;
    }

    /** Código visible del resultado de aprendizaje: RA1, RA2, RA3... */
    public String getCodigo() {
        return "RA" + orden;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Modulo getModulo() {
        return modulo;
    }

    public void setModulo(Modulo modulo) {
        this.modulo = modulo;
    }

    @Override
    public String toString() {
        return getCodigo() + " de " + modulo;
    }
}
