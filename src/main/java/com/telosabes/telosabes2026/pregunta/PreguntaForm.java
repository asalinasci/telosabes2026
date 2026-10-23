package com.telosabes.telosabes2026.pregunta;

import com.telosabes.telosabes2026.criterio.Criterio;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de una pregunta (crear y editar).
 *
 * El criterio llega como id, no como objeto: así el navegador no puede enviar
 * una relación inventada y el controlador comprueba que existe antes de
 * guardar. Las respuestas y la correcta solo se tienen en cuenta en las
 * preguntas de test; en las de texto se vacían al guardar.
 */
public class PreguntaForm {

    @NotNull(message = "Elige el criterio de evaluacion")
    private Long criterioId;

    @NotNull(message = "Elige el tipo de pregunta")
    private TipoPregunta tipo = TipoPregunta.TEST;

    @NotBlank(message = "El enunciado es obligatorio")
    @Size(max = 2000, message = "El enunciado no puede superar los 2000 caracteres")
    private String enunciado;

    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    private String respuesta1;

    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    private String respuesta2;

    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    private String respuesta3;

    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    private String respuesta4;

    @Min(value = 1, message = "La respuesta correcta debe ser 1, 2, 3 o 4")
    @Max(value = 4, message = "La respuesta correcta debe ser 1, 2, 3 o 4")
    private Integer correcta;

    /**
     * Una pregunta de test necesita las cuatro respuestas y la correcta. Las
     * de texto no llevan ninguna de las dos cosas.
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

    /** Copia los datos de una pregunta guardada al formulario de edición. */
    public static PreguntaForm desde(Pregunta pregunta) {
        PreguntaForm formulario = new PreguntaForm();
        formulario.criterioId = pregunta.getCriterio().getId();
        formulario.tipo = pregunta.getTipo();
        formulario.enunciado = pregunta.getEnunciado();
        formulario.respuesta1 = pregunta.getRespuesta1();
        formulario.respuesta2 = pregunta.getRespuesta2();
        formulario.respuesta3 = pregunta.getRespuesta3();
        formulario.respuesta4 = pregunta.getRespuesta4();
        formulario.correcta = pregunta.getCorrecta();
        return formulario;
    }

    /** Crea una pregunta nueva con los datos del formulario. */
    public Pregunta aPregunta(Criterio criterio) {
        Pregunta pregunta = new Pregunta();
        aplicarA(pregunta, criterio);
        return pregunta;
    }

    /**
     * Vuelca el formulario en una pregunta (nueva o existente). En las de texto
     * deja las respuestas y la correcta a null, aunque el navegador las envíe.
     */
    public void aplicarA(Pregunta pregunta, Criterio criterio) {
        pregunta.setCriterio(criterio);
        pregunta.setTipo(tipo);
        pregunta.setEnunciado(limpiar(enunciado));

        if (tipo == TipoPregunta.TEXTO) {
            pregunta.setRespuesta1(null);
            pregunta.setRespuesta2(null);
            pregunta.setRespuesta3(null);
            pregunta.setRespuesta4(null);
            pregunta.setCorrecta(null);
        } else {
            pregunta.setRespuesta1(limpiar(respuesta1));
            pregunta.setRespuesta2(limpiar(respuesta2));
            pregunta.setRespuesta3(limpiar(respuesta3));
            pregunta.setRespuesta4(limpiar(respuesta4));
            pregunta.setCorrecta(correcta);
        }
    }

    private static boolean vacia(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String limpiar(String texto) {
        return texto == null ? null : texto.trim();
    }

    public Long getCriterioId() {
        return criterioId;
    }

    public void setCriterioId(Long criterioId) {
        this.criterioId = criterioId;
    }

    public TipoPregunta getTipo() {
        return tipo;
    }

    public void setTipo(TipoPregunta tipo) {
        this.tipo = tipo;
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
}
