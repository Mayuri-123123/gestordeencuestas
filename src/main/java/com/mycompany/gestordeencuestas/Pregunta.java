package com.mycompany.gestordeencuestas;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Representa UNA PREGUNTA de opción única con sus opciones.
// Ejemplo: "¿Dónde prefieres estudiar?" con opciones [Casa, Biblioteca, Café].
// ----------------------------------------------------------------------------
// Es INMUTABLE: una vez creada, su texto y sus opciones NO cambian nunca.
// Esto apoya la Regla 1 (encuesta publicada no cambia sus preguntas).
// ----------------------------------------------------------------------------
// Su método más importante es contieneOpcion(): dice si una opción
// pertenece REALMENTE a esta pregunta. El servicio lo usa para rechazar
// opciones ajenas o falsificadas (Regla 2).
// ============================================================================

/**
 * Pregunta de opción única. Inmutable tras su construcción.
 *
 * <p>Privacidad: solo guarda el texto de la pregunta y sus opciones.
 * No contiene ningún dato personal.
 */
public final class Pregunta {

    // ---- Datos internos (private final = nadie los cambia desde fuera) ----
    private final UUID id;            // Identificador único de la pregunta
    private final String texto;       // Enunciado, ej: "¿Dónde prefieres estudiar?"
    private final List<Opcion> opciones; // Opciones entre las que se elige UNA

    /**
     * Crea una pregunta validando todo estrictamente.
     *
     * @param id       identificador (lo genera el sistema, no el usuario)
     * @param texto    enunciado de la pregunta (no vacío, máx. 500 caracteres)
     * @param opciones lista de opciones (mínimo 2, máximo 10, sin duplicados)
     */
    public Pregunta(UUID id, String texto, List<Opcion> opciones) {
        // 1) Nada puede ser null: si falta algo, se avisa de inmediato.
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(texto, "texto no puede ser null");
        Objects.requireNonNull(opciones, "opciones no puede ser null");

        // 2) El enunciado debe tener contenido real (no vacío ni solo espacios).
        String limpio = texto.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("texto de pregunta no puede estar vacío");
        }
        if (limpio.length() > 500) {
            throw new IllegalArgumentException("texto de pregunta supera 500 caracteres");
        }

        // 3) Una pregunta de "opción única" necesita al menos 2 opciones
        //    (con 1 no habría nada que elegir) y como máximo 10 por claridad.
        if (opciones.size() < 2) {
            throw new IllegalArgumentException("una pregunta requiere al menos 2 opciones");
        }
        if (opciones.size() > 10) {
            throw new IllegalArgumentException("una pregunta admite como máximo 10 opciones");
        }

        // 4) Cada opción debe tener un id ÚNICO: si alguien intenta colar dos
        //    veces la misma opción (mismo id), se detecta y se rechaza.
        var ids = new HashSet<UUID>();
        for (var o : opciones) {
            Objects.requireNonNull(o, "opción no puede ser null");
            if (!ids.add(o.id())) {
                throw new IllegalArgumentException("opciones duplicadas (id repetido): " + o.id());
            }
        }

        this.texto = limpio;
        // 5) Copia defensiva INMODIFICABLE: aunque quien nos pasó la lista la
        //    cambie después, nuestra pregunta queda intacta.
        this.opciones = List.copyOf(opciones);
    }

    /**
     * Forma RECOMENDADA de crear una pregunta: tú das los textos y el sistema
     * genera todos los ids seguros automáticamente.
     *
     * @param texto           enunciado de la pregunta
     * @param textosOpciones  textos de las opciones (ej: List.of("Casa", "Café"))
     * @return la pregunta lista para agregarse a una encuesta en BORRADOR
     */
    public static Pregunta de(String texto, List<String> textosOpciones) {
        Objects.requireNonNull(textosOpciones, "textosOpciones no puede ser null");
        // Convierte cada texto en una Opcion con id generado por el sistema.
        var opciones = textosOpciones.stream()
                .map(Opcion::de)
                .toList();
        return new Pregunta(UUID.randomUUID(), texto, opciones);
    }

    /** Devuelve el identificador único de la pregunta. */
    public UUID id() {
        return id;
    }

    /** Devuelve el enunciado de la pregunta. */
    public String texto() {
        return texto;
    }

    /**
     * Devuelve las opciones. La lista es INMODIFICABLE: quien la recibe
     * puede leerla pero no puede agregarle ni quitarle opciones.
     */
    public List<Opcion> opciones() {
        return opciones; // List.copyOf ya es inmodificable
    }

    /**
     * Verificación ESTRICTA de pertenencia (clave de la Regla 2).
     * Responde: "¿esta opción fue creada para ESTA pregunta?".
     * Compara por UUID, así una opción de otra pregunta (aunque tenga el
     * mismo texto) da FALSE y será rechazada.
     *
     * @param opcionId id de la opción a verificar
     * @return true solo si la opción pertenece a esta pregunta
     */
    public boolean contieneOpcion(UUID opcionId) {
        Objects.requireNonNull(opcionId, "opcionId no puede ser null");
        return opciones.stream().anyMatch(o -> o.id().equals(opcionId));
    }

    /**
     * Busca y devuelve la opción con ese id, o lanza error si es ajena.
     *
     * @param opcionId id buscado
     * @return la Opcion correspondiente
     */
    public Opcion obtenerOpcion(UUID opcionId) {
        return buscarOpcion(opcionId).orElseThrow(() ->
                new IllegalArgumentException(
                        "Opción ajena o manipulada: " + opcionId
                        + " no pertenece a la pregunta " + id));
    }

    /** Busca la opción sin lanzar error: devuelve Optional (vacío si no existe). */
    public Optional<Opcion> buscarOpcion(UUID opcionId) {
        return opciones.stream().filter(o -> o.id().equals(opcionId)).findFirst();
    }

    // Dos preguntas son "la misma" si tienen el mismo id (aunque el texto coincida).
    @Override
    public boolean equals(Object o) {
        return (o instanceof Pregunta otra) && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Pregunta[id=%s, texto=%s, opciones=%d]".formatted(id, texto, opciones.size());
    }
}
