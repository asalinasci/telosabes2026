package com.telosabes.telosabes2026.criterio;

import com.telosabes.telosabes2026.ra.RA;
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
 * Criterio de evaluación de un resultado de aprendizaje (tabla "criterios").
 *
 * Son las letras a), b), c)... de la tabla oficial de cada módulo: cada RA se
 * evalúa con sus criterios, que son los mismos que fija el Real Decreto del
 * título (las mismas fuentes que los RA, ver {@link RA} y {@link CriteriosIniciales}).
 *
 * Se dan de alta automáticamente al arrancar (ver {@link CriteriosIniciales}).
 *
 * Al igual que en el resto del proyecto, la relación es {@code @ManyToOne} desde
 * el criterio hacia su RA: no hay {@code @OneToMany} en {@link RA}.
 */
@Entity
@Table(name = "criterios",
        uniqueConstraints = @UniqueConstraint(name = "uq_criterio_ra_orden",
                columnNames = {"ra_id", "orden"}))
public class Criterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Posición del criterio dentro del RA, empezando en 1 (la «a)» de la
     * tabla oficial).
     */
    @Min(value = 1, message = "El orden debe ser 1 o mayor")
    @Column(nullable = false)
    private Integer orden;

    /** Texto oficial completo del criterio de evaluación. */
    @Size(max = 8000, message = "El criterio de evaluación no puede superar los 8000 caracteres")
    @Column(nullable = false, length = 8000)
    private String descripcion;

    /** Resultado de aprendizaje al que pertenece el criterio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ra_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_criterio_ra"))
    private RA ra;

    protected Criterio() {
        // Requerido por JPA
    }

    public Criterio(Integer orden, String descripcion, RA ra) {
        this.orden = orden;
        this.descripcion = descripcion;
        this.ra = ra;
    }

    /** Letra visible del criterio: a), b), c)... según su orden. */
    public String getLetra() {
        return String.valueOf((char) ('a' + orden - 1)) + ")";
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

    public RA getRa() {
        return ra;
    }

    public void setRa(RA ra) {
        this.ra = ra;
    }

    @Override
    public String toString() {
        return getLetra() + " de " + ra;
    }
}
