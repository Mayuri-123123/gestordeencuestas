package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;

/**
 * MÓDULO A - CLASE PREGUNTA
 * Representa una pregunta con sus opciones de respuesta
 * Integrante: Yo (rama pruebas)
 */
public class Pregunta {
    private String id;
    private String texto;
    private List<String> opciones;

    public Pregunta(String texto) {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.texto = texto;
        this.opciones = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public List<String> getOpciones() { return opciones; }

    public void agregarOpcion(String opcion) {
        this.opciones.add(opcion);
    }

    public void eliminarOpcion(int index) {
        if (index >= 0 && index < opciones.size()) {
            opciones.remove(index);
        }
    }

    @Override
    public String toString() {
        return texto + " (" + opciones.size() + " opciones)";
    }
}
