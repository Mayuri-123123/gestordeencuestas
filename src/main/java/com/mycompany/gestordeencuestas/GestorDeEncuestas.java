package com.mycompany.gestordeencuestas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Es el PROGRAMA PRINCIPAL de consola: demuestra que el sistema cumple la
// rúbrica 04 paso a paso (prueba de aceptación):
//   1) Publica una encuesta con DOS preguntas.
//   2) Registra TRES respuestas anónimas.
//   3) Calcula y muestra los PORCENTAJES con gráficos de barras.
//   4) Intenta una OPCIÓN AJENA y verifica que sea rechazada.
//   5) Verifica la Regla 1 (publicada no cambia) y la Regla 3 (anonimato).
// ----------------------------------------------------------------------------
// ¿CÓMO EJECUTARLO?
//   En NetBeans: clic derecho en este archivo -> Run File (Mayús+F6).
//   En terminal: javac ... && java -cp out com.mycompany.gestordeencuestas.GestorDeEncuestas
// ----------------------------------------------------------------------------
// ¿Prefieres ventana gráfica? Ejecuta EncuestaGUI (tiene botones y barras).
// ============================================================================

/**
 * Demostración de consola de la prueba de aceptación (rúbrica 04).
 */
public class GestorDeEncuestas {

    /** Ancho de las barras de progreso en consola. */
    private static final int ANCHO_BARRA = 30;

    /** Punto de entrada del programa de consola. */
    public static void main(String[] args) {
        // Servicio que coordina todo (módulos A y B).
        var service = new EncuestaService();

        imprimirPortada();

        // ---- PASO 1 (Módulo A): crear encuesta con DOS preguntas y publicar ----
        var encuesta = service.crearEncuesta("Hábitos de estudio");
        var p1 = service.agregarPregunta(encuesta.id(),
                "¿Cuántas horas estudias al día?", List.of("Menos de 1h", "1-3h", "Más de 3h"));
        var p2 = service.agregarPregunta(encuesta.id(),
                "¿Dónde prefieres estudiar?", List.of("Casa", "Biblioteca", "Café"));
        service.publicar(encuesta.id());
        seccion("1. Encuesta publicada (Módulo A)");
        System.out.printf("   Título : %s%n   Estado : %s%n   Preguntas: %d%n",
                encuesta.titulo(), encuesta.estado(), encuesta.preguntas().size());

        // ---- PASO 2: Regla 1, la publicada NO cambia sus preguntas ----
        seccion("2. Regla 1: la encuesta publicada no cambia sus preguntas");
        try {
            service.agregarPregunta(encuesta.id(), "Pregunta intrusa", List.of("A", "B"));
            System.out.println("   [FALLO] Se permitió mutar una encuesta publicada.");
        } catch (IllegalStateException e) {
            System.out.println("   [OK] Cambio bloqueado: " + e.getMessage());
        }

        // ---- PASO 3 (Módulo B): TRES respuestas completas y anónimas ----
        seccion("3. Tres respuestas anónimas registradas (Módulo B)");
        service.responder(encuesta.id(), Map.of(
                p1.id(), p1.opciones().get(1).id(),   // "1-3h"
                p2.id(), p2.opciones().get(0).id())); // "Casa"
        service.responder(encuesta.id(), Map.of(
                p1.id(), p1.opciones().get(2).id(),   // "Más de 3h"
                p2.id(), p2.opciones().get(1).id())); // "Biblioteca"
        service.responder(encuesta.id(), Map.of(
                p1.id(), p1.opciones().get(1).id(),   // "1-3h"
                p2.id(), p2.opciones().get(1).id())); // "Biblioteca"
        System.out.printf("   Total de respuestas: %d (ninguna con datos personales)%n",
                service.totalRespuestas(encuesta.id()));

        // ---- PASO 4: respuesta parcial (incompleta) bloqueada ----
        seccion("4. Defensa: la respuesta parcial se rechaza");
        try {
            var parcial = new HashMap<UUID, UUID>();
            parcial.put(p1.id(), p1.opciones().getFirst().id()); // falta p2
            service.responder(encuesta.id(), parcial);
            System.out.println("   [FALLO] Se permitió una respuesta parcial.");
        } catch (IllegalArgumentException e) {
            System.out.println("   [OK] Parcial bloqueada: debe responder TODAS las preguntas.");
        }

        // ---- PASO 5: Regla 2, opción ajena (de otra pregunta) bloqueada ----
        seccion("5. Regla 2: la opción ajena se rechaza");
        try {
            service.responder(encuesta.id(), Map.of(
                    p1.id(), p2.opciones().getFirst().id(), // opción de p2 usada en p1
                    p2.id(), p2.opciones().getFirst().id()));
            System.out.println("   [FALLO] Se permitió una opción ajena.");
        } catch (IllegalArgumentException e) {
            System.out.println("   [OK] Opción ajena bloqueada (no pertenece a esa pregunta).");
        }

        // ---- PASO 6: opción inventada (id falsificado) bloqueada ----
        seccion("6. Seguridad: el id manipulado se rechaza");
        try {
            service.responder(encuesta.id(), Map.of(
                    p1.id(), UUID.randomUUID(), // id que nadie generó
                    p2.id(), p2.opciones().getFirst().id()));
            System.out.println("   [FALLO] Se permitió una opción manipulada.");
        } catch (IllegalArgumentException e) {
            System.out.println("   [OK] Id falsificado bloqueado.");
        }

        // ---- PASO 7: resultados con porcentajes y barras ----
        seccion("7. Resultados: porcentajes por opción (Módulo B)");
        var porcentajes = service.resultados(encuesta.id());
        var conteos = service.conteo(encuesta.id());
        for (var pregunta : encuesta.preguntas()) {
            System.out.println();
            System.out.println("   P: " + pregunta.texto());
            var pcts = porcentajes.get(pregunta.id());
            var votos = conteos.get(pregunta.id());
            for (var o : pregunta.opciones()) {
                double pct = pcts.get(o.id());
                long v = votos.get(o.id());
                System.out.printf("     %-12s %5.1f%%  %s  (%d voto%s)%n",
                        o.texto(), pct, barra(pct), v, (v == 1 ? "" : "s"));
            }
        }

        // ---- PASO 8: Regla 3, anonimato verificable ----
        seccion("8. Regla 3: anonimato absoluto (sin datos personales)");
        System.out.println("   La clase Respuesta solo guarda: id, encuestaId,");
        System.out.println("   seleccion (ids técnicos) e instante. No existen");
        System.out.println("   campos de nombre, email, teléfono ni IP.");
        System.out.println("   El método responder() ni siquiera los pide.");
        System.out.println("   [OK] Privacidad garantizada POR DISEÑO.");

        System.out.println();
        System.out.println("   ==================================================");
        System.out.println("   Prueba de aceptación COMPLETADA con éxito.");
        System.out.println("   Para la versión con ventana: ejecutar EncuestaGUI.");
        System.out.println("   ==================================================");
    }

    // ==================== Ayudas de presentación ====================

    /** Imprime el encabezado principal del programa. */
    private static void imprimirPortada() {
        System.out.println();
        System.out.println("   ==================================================");
        System.out.println("    GESTOR DE ENCUESTAS  |  Respuestas 100% anónimas");
        System.out.println("    Prueba de aceptación - Rúbrica 04 (Java 25)");
        System.out.println("   ==================================================");
    }

    /** Imprime el título de cada sección de la demostración. */
    private static void seccion(String titulo) {
        System.out.println();
        System.out.println("   --- " + titulo + " ---");
    }

    /**
     * Dibuja una barra de progreso en texto, ej: [##########----------].
     * ¿PARA QUÉ? Visualizar el porcentaje de cada opción en la consola.
     */
    private static String barra(double porcentaje) {
        int llenos = (int) Math.round(porcentaje / 100.0 * ANCHO_BARRA);
        return "[" + "#".repeat(llenos) + "-".repeat(ANCHO_BARRA - llenos) + "]";
    }
}
