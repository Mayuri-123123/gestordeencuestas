package com.mycompany.gestordeencuestas;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Respuesta anónima a una encuesta.
 *
 * <p><b>Anonimato absoluto (Regla 3):</b> este record solo almacena
 * identificadores técnicos (ids de encuesta, pregunta y opción) y la marca
 * temporal. No existe ningún campo de nombre, email, teléfono, IP,
 * user-agent ni identificador de usuario, y la API de
 * {@link EncuestaService#responder} jamás solicita ni acepta datos
 * personales. La privacidad se garantiza por diseño (ausencia de campos),
 * no por una promesa documental.
 *
 * <p>{@code seleccion} es {@code Map<preguntaId, opcionId>} inmodificable.
 * Debe ser <i>completa</i>: exactamente una opción por cada pregunta
 * (ver validación en el servicio, que evita respuestas parciales).
 *
 * @param id         id anónimo de la respuesta (generado por el sistema)
 * @param encuestaId encuesta a la que pertenece
 * @param seleccion  mapa pregunta -&gt; opción elegida
 * @param instante   momento del registro (dato técnico, no personal)
 */
public record Respuesta(UUID id, UUID encuestaId, Map<UUID, UUID> seleccion, Instant instante) {

    public Respuesta {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        Objects.requireNonNull(seleccion, "seleccion no puede ser null");
        Objects.requireNonNull(instante, "instante no puede ser null");
        if (seleccion.isEmpty()) {
            throw new IllegalArgumentException("seleccion no puede estar vacía (respuesta parcial bloqueada)");
        }
        for (var e : seleccion.entrySet()) {
            Objects.requireNonNull(e.getKey(), "preguntaId no puede ser null");
            Objects.requireNonNull(e.getValue(), "opcionId no puede ser null");
        }
        // Copia defensiva inmodificable.
        seleccion = Map.copyOf(seleccion);
    }

    /** Factoría anónima: el llamante solo aporta ids técnicos, nunca datos personales. */
    public static Respuesta anonima(UUID encuestaId, Map<UUID, UUID> seleccion) {
        return new Respuesta(UUID.randomUUID(), encuestaId, seleccion, Instant.now());
    }
}
