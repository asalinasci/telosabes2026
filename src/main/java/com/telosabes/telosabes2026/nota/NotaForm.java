package com.telosabes.telosabes2026.nota;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de una nota.
 *
 * Fijate en que NO tiene campo de autor: el autor se toma de la sesion
 * en el controlador, asi que no se puede falsificar desde el navegador.
 */
public class NotaForm {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 100, message = "El titulo no puede superar los 100 caracteres")
    private String titulo;

    @Size(max = 500, message = "El contenido no puede superar los 500 caracteres")
    private String contenido;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
