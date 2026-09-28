package com.mycompany.gestordeencuestas;

import java.util.Objects;
import java.util.UUID;

/**
 * Opción de respuesta de una {@link Pregunta} de opción única.
 *
 * <p>Diseño Java 25: {@code record} inmutable. La identidad es un {@link UUID}
 * aleatorio generado por el sistema, nunca un entero secuencial predecible,
 * para impedir que un cliente adivine o manipule identificadores.
 *
 * <p>Seguridad: valida en el constructor compacto que el texto no sea nulo,
 * vacío ni excesivo. No contiene ningún dato personal.
 *
 * @param id    identificador único generado por el sistema
 * @param texto texto visible de la opción (1-200 caracteres)
 */
public record Opcion(UUID id, String texto) {

    public Opcion {
        Objects.requireNonNull(id, "id no puede ser null (debe generarlo el sistema)");
        Objects.requireNonNull(texto, "texto no puede ser null");
        String limpio = texto.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("texto de opción no puede estar vacío");
        }
        if (limpio.length() > 200) {
            throw new IllegalArgumentException("texto de opción supera 200 caracteres");
        }
        // Normaliza espacios: reasigna el componente con el valor saneado.
        texto = limpio;
    }

    /**
     * Factoría segura: el id siempre lo genera el sistema.
     * El llamante solo aporta el texto, nunca el identificador.
     */
    public static Opcion de(String texto) {
        return new Opcion(UUID.randomUUID(), texto);
    }
}
