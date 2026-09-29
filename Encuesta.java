package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MÓDULO A - CLASE ENCUESTA
 * Representa una encuesta con sus preguntas
 * Integrante: Yo (rama pruebas)
 */
public class Encuesta {
    private String id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private List<Pregunta> preguntas;
    private boolean publicada;
    private Date fechaCreacion;
    private Date fechaPublicacion;

    public Encuesta(String titulo, String descripcion, String categoria) {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.preguntas = new ArrayList<>();
        this.publicada = false;
        this.fechaCreacion = new Date();
        this.fechaPublicacion = null;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public List<Pregunta> getPreguntas() { return preguntas; }
    public boolean isPublicada() { return publicada; }
    public Date getFechaCreacion() { return fechaCreacion; }
    public Date getFechaPublicacion() { return fechaPublicacion; }

    public void agregarPregunta(Pregunta pregunta) {
        this.preguntas.add(pregunta);
    }

    public void publicar() throws Exception {
        if (preguntas.isEmpty()) {
            throw new Exception("La encuesta debe tener al menos una pregunta");
        }
        this.publicada = true;
        this.fechaPublicacion = new Date();
    }

    @Override
    public String toString() {
        return "Encuesta: " + titulo + " [" + categoria + "] - " + 
               (publicada ? "Publicada" : "Borrador") + 
               " - " + preguntas.size() + " preguntas";
    }
}
