package com.mycompany.gestordeencuestas;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Representa UNA RESPUESTA anónima a una encuesta: qué opción se eligió
// en cada pregunta. Ejemplo: {pregunta1 -> "1-3h", pregunta2 -> "Casa"}.
// ----------------------------------------------------------------------------
// ANONIMATO ABSOLUTO (Regla 3): este objeto SOLO guarda identificadores
// técnicos (ids) y la hora del registro. NO tiene nombre, email, teléfono,
// IP ni ningún dato personal. La privacidad se logra POR DISEÑO: como los
// campos no existen, es imposible filtrarlos.
// ----------------------------------------------------------------------------
// Es un "record" INMUTABLE y su mapa de selección es completo: UNA opción
// por CADA pregunta (las respuestas a medias se rechazan en el servicio).
// ============================================================================

/**
 * Respuesta anónima a una encuesta.
 *
 * @param id         id anónimo de la respuesta (generado por el sistema)
 * @param encuestaId encuesta a la que pertenece esta respuesta
 * @param seleccion  mapa preguntaId -&gt; opcionId con la elección de cada pregunta
 * @param instante   momento del registro (dato técnico, no personal)
 */
public record Respuesta(UUID id, UUID encuestaId, Map<UUID, UUID> seleccion, Instant instante) {

    // Constructor compacto: valida que la respuesta venga completa y sana.
    public Respuesta {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        Objects.requireNonNull(seleccion, "seleccion no puede ser null");
        Objects.requireNonNull(instante, "instante no puede ser null");
        // Una respuesta vacía es una respuesta "a medias": se bloquea aquí mismo.
        if (seleccion.isEmpty()) {
            throw new IllegalArgumentException("seleccion no puede estar vacía (respuesta parcial bloqueada)");
        }
        // Ninguna clave (pregunta) ni valor (opción) puede ser null.
        for (var e : seleccion.entrySet()) {
            Objects.requireNonNull(e.getKey(), "preguntaId no puede ser null");
            Objects.requireNonNull(e.getValue(), "opcionId no puede ser null");
        }
        // Copia defensiva INMODIFICABLE del mapa.
        seleccion = Map.copyOf(seleccion);
    }

    /**
     * Forma RECOMENDADA de crear una respuesta anónima.
     * El sistema genera el id y la hora; quien responde SOLO aporta los ids
     * técnicos de pregunta/opción. Nunca se piden datos personales.
     *
     * @param encuestaId encuesta que se está respondiendo
     * @param seleccion  mapa preguntaId -&gt; opcionId (completo)
     * @return la respuesta anónima lista para guardarse
     */
    public static Respuesta anonima(UUID encuestaId, Map<UUID, UUID> seleccion) {
        return new Respuesta(UUID.randomUUID(), encuestaId, seleccion, Instant.now());
    }
}
