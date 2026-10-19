package com.telosabes.telosabes2026.nota;

import com.telosabes.telosabes2026.usuario.Usuario;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entidad de ejemplo: una nota sencilla.
 * Hibernate creara la tabla "notas" en MariaDB automaticamente
 * (spring.jpa.hibernate.ddl-auto=update).
 */
@Entity
@Table(name = "notas")
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 100, message = "El titulo no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String titulo;

    @Size(max = 500, message = "El contenido no puede superar los 500 caracteres")
    @Column(length = 500)
    private String contenido;

    /**
     * Autor de la nota: el usuario que tenia la sesion iniciada al crearla.
     *
     * Se rellena SIEMPRE en el controlador a partir de la sesion, nunca
     * desde el formulario: asi nadie puede hacerse pasar por otro usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notas_autor"))
    private Usuario autor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Nota() {
        // Requerido por JPA
    }

    public Nota(String titulo, String contenido, Usuario autor) {
        this.titulo = titulo;
        this.contenido = contenido;
        this.autor = autor;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
