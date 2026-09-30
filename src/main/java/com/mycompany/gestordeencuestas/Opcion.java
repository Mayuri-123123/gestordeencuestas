package com.mycompany.gestordeencuestas;

import java.util.Objects;
import java.util.UUID;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Representa UNA OPCIÓN elegible dentro de una pregunta de opción única.
// Ejemplo: pregunta "¿Dónde prefieres estudiar?" -> opciones "Casa",
// "Biblioteca", "Café". Cada opción es un objeto Opcion.
// ----------------------------------------------------------------------------
// Es un "record" (característica moderna de Java): una clase pequeña,
// INMUTABLE (sus datos no cambian nunca) ideal para datos simples.
// ----------------------------------------------------------------------------
// SEGURIDAD: el identificador (id) lo genera SIEMPRE el sistema con
// UUID.randomUUID(), nunca el usuario. Así nadie puede adivinar ni
// falsificar ids (si fueran 1, 2, 3... sería fácil inventar uno).
// ============================================================================

/**
 * Opción de respuesta de una {@link Pregunta} de opción única.
 *
 * @param id    identificador único generado por el sistema (anti-manipulación)
 * @param texto texto visible de la opción (1 a 200 caracteres, sin espacios de más)
 */
public record Opcion(UUID id, String texto) {

    // Constructor compacto del record: se ejecuta SIEMPRE al crear una Opcion
    // y valida que los datos sean correctos. Si algo está mal, lanza error
    // y la opción NO se crea (validación estricta).
    public Opcion {
        // El id nunca puede faltar: lo genera el sistema, no el usuario.
        Objects.requireNonNull(id, "id no puede ser null (debe generarlo el sistema)");
        // El texto tampoco puede faltar.
        Objects.requireNonNull(texto, "texto no puede ser null");
        // strip() quita espacios al inicio y al final ("  Casa  " -> "Casa").
        String limpio = texto.strip();
        // No se permite una opción vacía o de solo espacios.
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("texto de opción no puede estar vacío");
        }
        // Límite de 200 caracteres para evitar textos abusivos.
        if (limpio.length() > 200) {
            throw new IllegalArgumentException("texto de opción supera 200 caracteres");
        }
        // Guardamos el texto ya limpio (reasignación permitida en records).
        texto = limpio;
    }

    /**
     * Forma RECOMENDADA de crear una opción.
     * Solo pides el texto y el método genera el id seguro por ti.
     *
     * @param texto texto visible de la opción
     * @return una Opcion nueva con id generado por el sistema
     */
    public static Opcion de(String texto) {
        return new Opcion(UUID.randomUUID(), texto);
    }
}
