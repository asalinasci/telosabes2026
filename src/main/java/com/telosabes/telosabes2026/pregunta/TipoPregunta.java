package com.telosabes.telosabes2026.pregunta;

/**
 * Tipo de pregunta de un criterio de evaluación.
 *
 * El valor de cada constante es el que se guarda en la base de datos (columna
 * preguntas.tipo), por eso no se deben cambiar los nombres.
 */
public enum TipoPregunta {

    /** Pregunta de test: cuatro respuestas posibles y una sola correcta. */
    TEST("Test"),

    /** Pregunta de desarrollo: el alumno escribe la respuesta. */
    TEXTO("Texto");

    private final String etiqueta;

    TipoPregunta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre legible para mostrar en las pantallas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
