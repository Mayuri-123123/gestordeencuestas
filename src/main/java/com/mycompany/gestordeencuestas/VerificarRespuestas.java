package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;

/** Comprueba que todas las preguntas tengan una opcion valida. */
public final class VerificarRespuestas {

    private VerificarRespuestas() {
    }

    public static boolean verificar(List<Pregunta> preguntas, Respuestas respuestas) {
        return errores(preguntas, respuestas).isEmpty();
    }

    public static List<String> errores(List<Pregunta> preguntas, Respuestas respuestas) {
        if (preguntas == null || respuestas == null) {
            throw new IllegalArgumentException("Se requieren preguntas y respuestas.");
        }

        List<String> errores = new ArrayList<>();
        for (Pregunta pregunta : preguntas) {
            String seleccion = respuestas.respuestaPara(pregunta);
            if (seleccion == null) {
                errores.add("Falta responder: " + pregunta.texto());
            } else if (!pregunta.contieneOpcion(seleccion)) {
                errores.add("La opcion no es valida para: " + pregunta.texto());
            }
        }
        if (preguntas.isEmpty()) {
            errores.add("Agrega al menos una pregunta antes de verificar.");
        }
        return List.copyOf(errores);
    }
}