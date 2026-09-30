package com.mycompany.gestordeencuestas;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Almacena y permite modificar la opcion elegida para cada pregunta. */
public final class Respuestas {

    private final Map<UUID, String> seleccionadas = new LinkedHashMap<>();

    public void responder(Pregunta pregunta, String opcion) {
        validar(pregunta, opcion);
        if (seleccionadas.containsKey(pregunta.id())) {
            throw new IllegalStateException("La respuesta ya existe; usa modificarRespuesta.");
        }
        seleccionadas.put(pregunta.id(), opcion);
    }

    public void modificarRespuesta(Pregunta pregunta, String opcion) {
        validar(pregunta, opcion);
        if (!seleccionadas.containsKey(pregunta.id())) {
            throw new IllegalStateException("Todavia no hay una respuesta para modificar.");
        }
        seleccionadas.put(pregunta.id(), opcion);
    }

    public boolean tieneRespuesta(Pregunta pregunta) {
        return pregunta != null && seleccionadas.containsKey(pregunta.id());
    }

    public String respuestaPara(Pregunta pregunta) {
        return pregunta == null ? null : seleccionadas.get(pregunta.id());
    }

    public Map<UUID, String> todas() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(seleccionadas));
    }

    private static void validar(Pregunta pregunta, String opcion) {
        if (pregunta == null || !pregunta.contieneOpcion(opcion)) {
            throw new IllegalArgumentException("La opcion elegida no pertenece a la pregunta.");
        }
    }
}