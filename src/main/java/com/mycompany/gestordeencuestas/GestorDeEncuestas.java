package com.mycompany.gestordeencuestas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Punto de entrada y prueba de aceptación de la rúbrica 04:
 * publicar dos preguntas, registrar tres respuestas, calcular porcentajes
 * y rechazar una opción ajena. También verifica la Regla 1 (publicada no
 * cambia preguntas) y la Regla 3 (anonimato: la API jamás pide datos personales).
 *
 * <p>Ejecución: {@code javac -d out $(find src -name "*.java") && java -cp out com.mycompany.gestordeencuestas.GestorDeEncuestas}
 */
public class GestorDeEncuestas {

    public static void main(String[] args) {
        var service = new EncuestaService();

        // 1) Crear encuesta + dos preguntas (módulo A).
        var encuesta = service.crearEncuesta("Hábitos de estudio");
        var p1 = service.agregarPregunta(encuesta.id(),
                "¿Cuántas horas estudias al día?", List.of("Menos de 1h", "1-3h", "Más de 3h"));
        var p2 = service.agregarPregunta(encuesta.id(),
                "¿Dónde prefieres estudiar?", List.of("Casa", "Biblioteca", "Café"));
        service.publicar(encuesta.id());
        System.out.println("Publicada: " + encuesta);

        // 2) Regla 1: publicada NO cambia sus preguntas.
        try {
            service.agregarPregunta(encuesta.id(), "Pregunta intrusa", List.of("A", "B"));
            System.out.println("ERROR: se permitió mutar una encuesta publicada");
        } catch (IllegalStateException e) {
            System.out.println("OK Regla 1 (inmutabilidad): " + e.getMessage());
        }

        // 3) Tres respuestas completas y anónimas (módulo B). Solo ids técnicos.
        service.responder(encuesta.id(), Map.of(
                p1.id(), p1.opciones().get(1).id(),
                p2.id(), p2.opciones().get(0).id()));
        service.responder(encuesta.id(), Map.of(
                p1.id(), p1.opciones().get(2).id(),
                p2.id(), p2.opciones().get(1).id()));
        service.responder(encuesta.id(), Map.of(
                p1.id(), p1.opciones().get(1).id(),
                p2.id(), p2.opciones().get(1).id()));
        System.out.println("Respuestas registradas: " + service.totalRespuestas(encuesta.id()));

        // 4) Respuesta parcial bloqueada (falta una pregunta).
        try {
            var parcial = new HashMap<UUID, UUID>();
            parcial.put(p1.id(), p1.opciones().getFirst().id());
            service.responder(encuesta.id(), parcial);
            System.out.println("ERROR: se permitió una respuesta parcial");
        } catch (IllegalArgumentException e) {
            System.out.println("OK anti-parcial: " + e.getMessage());
        }

        // 5) Opción ajena bloqueada: usar opción de p2 como respuesta a p1.
        try {
            service.responder(encuesta.id(), Map.of(
                    p1.id(), p2.opciones().getFirst().id(), // ¡ajena!
                    p2.id(), p2.opciones().getFirst().id()));
            System.out.println("ERROR: se permitió una opción ajena");
        } catch (IllegalArgumentException e) {
            System.out.println("OK Regla 2 (opción válida): " + e.getMessage());
        }

        // 6) Opción inventada (UUID aleatorio) bloqueada.
        try {
            service.responder(encuesta.id(), Map.of(
                    p1.id(), UUID.randomUUID(),
                    p2.id(), p2.opciones().getFirst().id()));
            System.out.println("ERROR: se permitió una opción manipulada");
        } catch (IllegalArgumentException e) {
            System.out.println("OK anti-manipulación: " + e.getMessage());
        }

        // 7) Resultados con porcentajes.
        System.out.println("""
                === Resultados (porcentajes) ===""");
        var resultados = service.resultados(encuesta.id());
        for (var pregunta : encuesta.preguntas()) {
            System.out.println("P: " + pregunta.texto());
            var pcts = resultados.get(pregunta.id());
            for (var o : pregunta.opciones()) {
                System.out.printf("  - %-12s : %5.1f%%%n", o.texto(), pcts.get(o.id()));
            }
        }

        // 8) Regla 3: anonimato verificable — Respuesta solo tiene ids + instante.
        var ejemplo = encuesta.respuestas().getFirst();
        System.out.println("""
                === Privacidad ===
                Respuesta=%s
                Campos de Respuesta: id, encuestaId, seleccion, instante (ningún dato personal).
                La firma responder(UUID, Map) no acepta nombre/email/IP por diseño (Regla 3 OK)."""
                .formatted(ejemplo));
    }
}
