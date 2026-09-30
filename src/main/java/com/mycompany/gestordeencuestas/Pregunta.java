package com.mycompany.gestordeencuestas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pregunta de encuesta con opciones de respuesta cerradas. */
public final class Pregunta {

    private final UUID id = UUID.randomUUID();
    private final String texto;
    private final List<String> opciones;

    public Pregunta(String texto, List<String> opciones) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escribe el texto de la pregunta.");
        }
        if (opciones == null || opciones.size() < 2) {
            throw new IllegalArgumentException("Agrega al menos dos opciones.");
        }

        List<String> limpias = opciones.stream()
                .map(opcion -> opcion == null ? "" : opcion.trim())
                .toList();
        if (limpias.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Las opciones no pueden estar vacias.");
        }

        Set<String> unicas = new HashSet<>();
        for (String opcion : limpias) {
            if (!unicas.add(opcion.toLowerCase())) {
                throw new IllegalArgumentException("No repitas opciones en la pregunta.");
            }
        }

        this.texto = texto.trim();
        this.opciones = List.copyOf(limpias);
    }

    public UUID id() {
        return id;
    }

    public String texto() {
        return texto;
    }

    public List<String> opciones() {
        return opciones;
    }

    public boolean contieneOpcion(String opcion) {
        return opcion != null && opciones.contains(opcion);
    }
}