package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Representa UNA ENCUESTA: un título, su lista de preguntas y las respuestas
// anónimas recibidas. Es el "corazón" del dominio.
// ----------------------------------------------------------------------------
// REGLA 1 (la más importante de esta clase): mientras la encuesta está en
// BORRADOR se pueden agregar preguntas; en cuanto se PUBLICA, el
// cuestionario queda CONGELADO para siempre. Cualquier intento de agregar
// preguntas después de publicar lanza IllegalStateException.
// ----------------------------------------------------------------------------
// Además, los métodos preguntas() y respuestas() devuelven COPIAS
// inmodificables, así nadie puede alterar el contenido "por la puerta
// de atrás" usando la lista devuelta.
// ============================================================================

/**
 * Encuesta con preguntas de opción única y respuestas anónimas.
 *
 * <p>Hilos: los métodos que modifican datos son {@code synchronized} y el
 * estado es {@code volatile}, para que el programa sea seguro aunque varias
 * personas respondan al mismo tiempo.
 */
public final class Encuesta {

    // ---- Datos internos (nadie los toca directamente desde fuera) ----
    private final UUID id;                 // Identificador único de la encuesta
    private final String titulo;           // Título visible, ej: "Hábitos de estudio"
    private final String tema;             // Categoría elegida para la encuesta
    private volatile EstadoEncuesta estado; // Estado actual (empieza en BORRADOR)
    private final List<Pregunta> preguntas = new ArrayList<>();   // Cuestionario
    private final List<Respuesta> respuestas = new ArrayList<>(); // Respuestas anónimas

    /**
     * Crea una encuesta nueva en BORRADOR con el tema General.
     *
     * @param id identificador generado por el sistema
     * @param titulo título visible (no vacío, máx. 150 caracteres)
     */
    public Encuesta(UUID id, String titulo) {
        this(id, titulo, "General");
    }

    /** Crea una encuesta nueva con su tema elegido. */
    public Encuesta(UUID id, String titulo, String tema) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(titulo, "titulo no puede ser null");
        Objects.requireNonNull(tema, "tema no puede ser null");
        String limpio = titulo.strip();
        String temaLimpio = tema.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("titulo no puede estar vacío");
        }
        if (limpio.length() > 150) {
            throw new IllegalArgumentException("titulo supera 150 caracteres");
        }
        if (temaLimpio.isEmpty() || temaLimpio.length() > 60) {
            throw new IllegalArgumentException("tema debe tener entre 1 y 60 caracteres");
        }
        this.titulo = limpio;
        this.tema = temaLimpio;
        this.estado = EstadoEncuesta.BORRADOR;
    }

    /** Forma recomendada de crear una encuesta con el tema General. */
    public static Encuesta nueva(String titulo) {
        return nueva(titulo, "General");
    }

    /** Forma recomendada de crear una encuesta con el tema elegido. */
    public static Encuesta nueva(String titulo, String tema) {
        return new Encuesta(UUID.randomUUID(), titulo, tema);
    }

    /** Identificador único de la encuesta. */
    public UUID id() {
        return id;
    }

    /** Título visible de la encuesta. */
    public String titulo() {
        return titulo;
    }

    /** Tema o categoría elegida para la encuesta. */
    public String tema() {
        return tema;
    }

    /** Estado actual: BORRADOR, PUBLICADA o CERRADA. */
    public EstadoEncuesta estado() {
        return estado;
    }

    /** Atajo útil: true si ya está abierta al público. */
    public boolean estaPublicada() {
        return estado == EstadoEncuesta.PUBLICADA;
    }

    /**
     * Agrega una pregunta al cuestionario. SOLO funciona en BORRADOR.
     * ---- Aplica la Regla 1 ----
     *
     * @param pregunta pregunta ya validada que se quiere agregar
     * @throws IllegalStateException si la encuesta ya está publicada o cerrada
     */
    public synchronized void agregarPregunta(Pregunta pregunta) {
        Objects.requireNonNull(pregunta, "pregunta no puede ser null");
        // REGLA 1: publicada o cerrada = cuestionario congelado, sin excepciones.
        if (estado != EstadoEncuesta.BORRADOR) {
            throw new IllegalStateException(
                    "Regla 1: encuesta publicada/cerrada no puede cambiar sus preguntas (estado=%s)".formatted(estado));
        }
        // No se permite agregar dos veces la misma pregunta (mismo id).
        boolean duplicada = preguntas.stream().anyMatch(p -> p.id().equals(pregunta.id()));
        if (duplicada) {
            throw new IllegalArgumentException("pregunta duplicada: " + pregunta.id());
        }
        preguntas.add(pregunta);
    }

    /**
     * PUBLICA la encuesta: pasa de BORRADOR a PUBLICADA para siempre.
     * Exige al menos 1 pregunta (no tiene sentido publicar una encuesta vacía).
     * Es IRREVERSIBLE: no hay forma de volver a BORRADOR.
     */
    public synchronized void publicar() {
        if (estado != EstadoEncuesta.BORRADOR) {
            throw new IllegalStateException("solo una encuesta en BORRADOR puede publicarse (estado=%s)".formatted(estado));
        }
        if (preguntas.isEmpty()) {
            throw new IllegalStateException("no se puede publicar una encuesta sin preguntas");
        }
        estado = EstadoEncuesta.PUBLICADA;
    }

    /**
     * CIERRA la encuesta: deja de aceptar respuestas.
     * Las preguntas y respuestas ya guardadas NO se alteran.
     */
    public synchronized void cerrar() {
        if (estado != EstadoEncuesta.PUBLICADA) {
            throw new IllegalStateException("solo una encuesta PUBLICADA puede cerrarse (estado=%s)".formatted(estado));
        }
        estado = EstadoEncuesta.CERRADA;
    }

    /**
     * Guarda una respuesta ya validada por el servicio.
     * Es de uso INTERNO (package-private): solo EncuestaService puede llamarlo,
     * después de verificar que la respuesta sea completa y válida.
     */
    synchronized void agregarRespuesta(Respuesta respuesta) {
        Objects.requireNonNull(respuesta, "respuesta no puede ser null");
        // Solo una encuesta abierta recibe respuestas.
        if (estado != EstadoEncuesta.PUBLICADA) {
            throw new IllegalStateException("solo una encuesta PUBLICADA acepta respuestas (estado=%s)".formatted(estado));
        }
        // La respuesta debe ser DE ESTA encuesta, no de otra.
        if (!respuesta.encuestaId().equals(id)) {
            throw new IllegalArgumentException("la respuesta pertenece a otra encuesta");
        }
        respuestas.add(respuesta);
    }

    /**
     * Devuelve las preguntas como lista INMODIFICABLE (solo lectura).
     * Aunque la encuesta esté publicada, nadie puede alterar el cuestionario
     * usando esta lista.
     */
    public synchronized List<Pregunta> preguntas() {
        return List.copyOf(preguntas);
    }

    /** Devuelve las respuestas anónimas como lista INMODIFICABLE (solo lectura). */
    public synchronized List<Respuesta> respuestas() {
        return List.copyOf(respuestas);
    }

    /** ¿Cuántas respuestas anónimas se han registrado hasta ahora? */
    public synchronized int totalRespuestas() {
        return respuestas.size();
    }

    @Override
    public String toString() {
        return "Encuesta[id=%s, titulo=%s, estado=%s, preguntas=%d, respuestas=%d]"
                .formatted(id, titulo, estado, preguntas.size(), respuestas.size());
    }
}
