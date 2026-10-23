package com.telosabes.telosabes2026.pregunta;

import com.telosabes.telosabes2026.criterio.Criterio;
import com.telosabes.telosabes2026.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Pregunta para evaluar un criterio de evaluación (tabla "preguntas").
 *
 * Cada pregunta cuelga de un criterio oficial (ver {@link Criterio}) y puede
 * ser de dos tipos:
 *
 * <ul>
 *   <li>{@link TipoPregunta#TEST}: cuatro respuestas posibles
 *       ({@code respuesta1} a {@code respuesta4}) y una correcta, guardada en
 *       {@code correcta} como número del 1 al 4.</li>
 *   <li>{@link TipoPregunta#TEXTO}: sin respuestas; el alumno escribe la
 *       suya, así que {@code respuesta1} a {@code respuesta4} y
 *       {@code correcta} quedan vacíos.</li>
 * </ul>
 *
 * Los contadores {@code aciertos} y {@code errores} empiezan en cero y no se
 * tocan al dar de alta la pregunta: se irán sumando cuando los alumnos la
 * contesten.
 *
 * El autor es el usuario que tenía la sesión iniciada al crear la pregunta. Se
 * rellena siempre en el controlador, nunca desde el formulario.
 *
 * Al igual que en el resto del proyecto, la relación es {@code @ManyToOne}
 * desde la pregunta hacia su criterio: no hay {@code @OneToMany} en
 * {@link Criterio}.
 */
@Entity
@Table(name = "preguntas")
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Enunciado completo de la pregunta. */
    @NotBlank(message = "El enunciado es obligatorio")
    @Size(max = 2000, message = "El enunciado no puede superar los 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String enunciado;

    /** Primera respuesta posible (solo en las preguntas de test). */
    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    @Column(length = 500)
    private String respuesta1;

    /** Segunda respuesta posible (solo en las preguntas de test). */
    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    @Column(length = 500)
    private String respuesta2;

    /** Tercera respuesta posible (solo en las preguntas de test). */
    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    @Column(length = 500)
    private String respuesta3;

    /** Cuarta respuesta posible (solo en las preguntas de test). */
    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    @Column(length = 500)
    private String respuesta4;

    /**
     * Número de la respuesta correcta (1 = respuesta1, 2 = respuesta2,
     * 3 = respuesta3 y 4 = respuesta4). En las preguntas de texto es null.
     */
    @Min(value = 1, message = "La respuesta correcta debe ser 1, 2, 3 o 4")
    @Max(value = 4, message = "La respuesta correcta debe ser 1, 2, 3 o 4")
    @Column
    private Integer correcta;

    /** Criterio de evaluación que comprueba la pregunta. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pregunta_criterio"))
    private Criterio criterio;

    /**
     * Autor de la pregunta: el usuario que tenía la sesión iniciada al
     * crearla. No se puede cambiar desde el formulario.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pregunta_autor"))
    private Usuario autor;

    /** Test (cuatro respuestas) o texto (la escribe el alumno). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPregunta tipo;

    /** Veces que los alumnos han acertado la pregunta. */
    @Min(value = 0, message = "Los aciertos no pueden ser negativos")
    @Column(nullable = false)
    private int aciertos = 0;

    /** Veces que los alumnos han fallado la pregunta. */
    @Min(value = 0, message = "Los errores no pueden ser negativos")
    @Column(nullable = false)
    private int errores = 0;

    protected Pregunta() {
        // Requerido por JPA
    }

    /** Pregunta de texto: el alumno escribe la respuesta. */
    public Pregunta(String enunciado, Criterio criterio, Usuario autor) {
        this.enunciado = enunciado;
        this.criterio = criterio;
        this.autor = autor;
        this.tipo = TipoPregunta.TEXTO;
    }

    /** Pregunta de test con sus cuatro respuestas y la correcta (1 a 4). */
    public Pregunta(String enunciado, Criterio criterio, Usuario autor,
                    String respuesta1, String respuesta2, String respuesta3, String respuesta4,
                    Integer correcta) {
        this.enunciado = enunciado;
        this.criterio = criterio;
        this.autor = autor;
        this.respuesta1 = respuesta1;
        this.respuesta2 = respuesta2;
        this.respuesta3 = respuesta3;
        this.respuesta4 = respuesta4;
        this.correcta = correcta;
        this.tipo = TipoPregunta.TEST;
    }

    /**
     * Comprueba que una pregunta de test tiene las cuatro respuestas y la
     * correcta marcada. Las de texto no necesitan nada de eso.
     */
    @AssertTrue(message = "Una pregunta de test necesita las cuatro respuestas y marcar la correcta")
    public boolean isTestBienFormada() {
        if (tipo != TipoPregunta.TEST) {
            return true;
        }
        return !vacia(respuesta1) && !vacia(respuesta2)
                && !vacia(respuesta3) && !vacia(respuesta4)
                && correcta != null;
    }

    private static boolean vacia(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Respuestas posibles de la pregunta; vacío en las de texto. */
    public List<String> getRespuestas() {
        List<String> respuestas = new ArrayList<>(4);
        if (!vacia(respuesta1)) {
            respuestas.add(respuesta1);
        }
        if (!vacia(respuesta2)) {
            respuestas.add(respuesta2);
        }
        if (!vacia(respuesta3)) {
            respuestas.add(respuesta3);
        }
        if (!vacia(respuesta4)) {
            respuestas.add(respuesta4);
        }
        return respuestas;
    }

    /** Texto de la respuesta correcta, o null si no hay (preguntas de texto). */
    public String getRespuestaCorrecta() {
        if (correcta == null) {
            return null;
        }
        return switch (correcta) {
            case 1 -> respuesta1;
            case 2 -> respuesta2;
            case 3 -> respuesta3;
            case 4 -> respuesta4;
            default -> null;
        };
    }

    /** Letra de la respuesta correcta: a), b), c) o d); null en las de texto. */
    public String getLetraCorrecta() {
        if (correcta == null) {
            return null;
        }
        return String.valueOf((char) ('a' + correcta - 1)) + ")";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getRespuesta1() {
        return respuesta1;
    }

    public void setRespuesta1(String respuesta1) {
        this.respuesta1 = respuesta1;
    }

    public String getRespuesta2() {
        return respuesta2;
    }

    public void setRespuesta2(String respuesta2) {
        this.respuesta2 = respuesta2;
    }

    public String getRespuesta3() {
        return respuesta3;
    }

    public void setRespuesta3(String respuesta3) {
        this.respuesta3 = respuesta3;
    }

    public String getRespuesta4() {
        return respuesta4;
    }

    public void setRespuesta4(String respuesta4) {
        this.respuesta4 = respuesta4;
    }

    public Integer getCorrecta() {
        return correcta;
    }

    public void setCorrecta(Integer correcta) {
        this.correcta = correcta;
    }

    public Criterio getCriterio() {
        return criterio;
    }

    public void setCriterio(Criterio criterio) {
        this.criterio = criterio;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public TipoPregunta getTipo() {
        return tipo;
    }

    public void setTipo(TipoPregunta tipo) {
        this.tipo = tipo;
    }

    public int getAciertos() {
        return aciertos;
    }

    public void setAciertos(int aciertos) {
        this.aciertos = aciertos;
    }

    public int getErrores() {
        return errores;
    }

    public void setErrores(int errores) {
        this.errores = errores;
    }

    @Override
    public String toString() {
        return tipo + ": " + enunciado;
    }
}
