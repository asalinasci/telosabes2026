package com.telosabes.telosabes2026.modulo;

import com.telosabes.telosabes2026.ciclo.Ciclo;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Módulo profesional (asignatura) de un ciclo formativo (tabla "modulos").
 *
 * Los datos son los de las tablas oficiales de la Consejería de Educación y
 * Formación Profesional de la Región de Murcia: código, horas semanales y
 * reparto de horas entre el centro y la fase de empresa.
 *
 * Los módulos se dan de alta automaticamente al arrancar (ver
 * {@link ModulosIniciales}).
 */
@Entity
@Table(name = "modulos")
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código oficial del módulo (según el Real Decreto): 0485, 0612, 1709...
     * Puede ser null en el modulo optativo, que no tiene código propio.
     */
    @Size(max = 10, message = "El código no puede superar los 10 caracteres")
    @Column(length = 10)
    private String codigo;

    @NotBlank(message = "El nombre del módulo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    /** Curso en el que se imparte: 1 o 2. */
    @NotNull(message = "El curso es obligatorio")
    @Min(value = 1, message = "El curso debe ser 1 o 2")
    @Max(value = 2, message = "El curso debe ser 1 o 2")
    @Column(nullable = false)
    private Integer curso;

    /** Posición dentro del curso (el orden de la tabla oficial). */
    @Column(nullable = false)
    private Integer orden;

    /** Horas a la semana en el centro. */
    @NotNull(message = "Las horas semanales son obligatorias")
    @Min(value = 0, message = "Las horas no pueden ser negativas")
    @Column(nullable = false)
    private Integer horasSemanales;

    /** Horas que se imparten en el centro. */
    @NotNull(message = "Las horas de centro son obligatorias")
    @Min(value = 0, message = "Las horas no pueden ser negativas")
    @Column(nullable = false)
    private Integer horasCentro;

    /** Horas que se realizan en la empresa (fase de formación en empresa). */
    @NotNull(message = "Las horas de empresa son obligatorias")
    @Min(value = 0, message = "Las horas no pueden ser negativas")
    @Column(nullable = false)
    private Integer horasEmpresa;

    /** Ciclo al que pertenece el módulo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_modulos_ciclo"))
    private Ciclo ciclo;

    protected Modulo() {
        // Requerido por JPA
    }

    public Modulo(String codigo, String nombre, Integer curso, Integer orden,
                  Integer horasSemanales, Integer horasCentro, Integer horasEmpresa,
                  Ciclo ciclo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.curso = curso;
        this.orden = orden;
        this.horasSemanales = horasSemanales;
        this.horasCentro = horasCentro;
        this.horasEmpresa = horasEmpresa;
        this.ciclo = ciclo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getCurso() {
        return curso;
    }

    public void setCurso(Integer curso) {
        this.curso = curso;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getHorasSemanales() {
        return horasSemanales;
    }

    public void setHorasSemanales(Integer horasSemanales) {
        this.horasSemanales = horasSemanales;
    }

    public Integer getHorasCentro() {
        return horasCentro;
    }

    public void setHorasCentro(Integer horasCentro) {
        this.horasCentro = horasCentro;
    }

    public Integer getHorasEmpresa() {
        return horasEmpresa;
    }

    public void setHorasEmpresa(Integer horasEmpresa) {
        this.horasEmpresa = horasEmpresa;
    }

    public Ciclo getCiclo() {
        return ciclo;
    }

    public void setCiclo(Ciclo ciclo) {
        this.ciclo = ciclo;
    }

    /** Horas totales del módulo = horas de centro + horas de empresa. */
    public int getHorasTotales() {
        return horasCentro + horasEmpresa;
    }

    @Override
    public String toString() {
        return (codigo == null ? "" : codigo + " ") + nombre;
    }
}
