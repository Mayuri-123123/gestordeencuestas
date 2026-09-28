package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Encuesta con preguntas de opción única y respuestas anónimas.
 *
 * <p><b>Regla 1 — Inmutabilidad tras publicar:</b> mientras el estado sea
 * {@link EstadoEncuesta#BORRADOR} pueden añadirse preguntas. En cuanto se
 * invoca {@link #publicar()}, el estado cambia a
 * {@link EstadoEncuesta#PUBLICADA} de forma irreversible y cualquier intento
 * de {@link #agregarPregunta} lanza {@link IllegalStateException}.
 * Además, {@link #preguntas()} y {@link #respuestas()} devuelven copias
 * inmodificables ({@code List.copyOf}), por lo que ni siquiera por referencia
 * puede mutarse el interior.
 *
 * <p>Hilos: métodos de mutación sincronizados; el estado es {@code volatile}
 * para visibilidad inmediata entre hilos.
 */
public final class Encuesta {

    private final UUID id;
    private final String titulo;
    private volatile EstadoEncuesta estado;
    private final List<Pregunta> preguntas = new ArrayList<>();
    private final List<Respuesta> respuestas = new ArrayList<>();

    public Encuesta(UUID id, String titulo) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(titulo, "titulo no puede ser null");
        String limpio = titulo.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("titulo no puede estar vacío");
        }
        if (limpio.length() > 150) {
            throw new IllegalArgumentException("titulo supera 150 caracteres");
        }
        this.titulo = limpio;
        this.estado = EstadoEncuesta.BORRADOR;
    }

    public static Encuesta nueva(String titulo) {
        return new Encuesta(UUID.randomUUID(), titulo);
    }

    public UUID id() {
        return id;
    }

    public String titulo() {
        return titulo;
    }

    public EstadoEncuesta estado() {
        return estado;
    }

    public boolean estaPublicada() {
        return estado == EstadoEncuesta.PUBLICADA;
    }

    /**
     * Añade una pregunta. SOLO en BORRADOR.
     *
     * @throws IllegalStateException si la encuesta ya está publicada o cerrada
     */
    public synchronized void agregarPregunta(Pregunta pregunta) {
        Objects.requireNonNull(pregunta, "pregunta no puede ser null");
        if (estado != EstadoEncuesta.BORRADOR) {
            throw new IllegalStateException(
                    "Regla 1: encuesta publicada/cerrada no puede cambiar sus preguntas (estado=%s)".formatted(estado));
        }
        boolean duplicada = preguntas.stream().anyMatch(p -> p.id().equals(pregunta.id()));
        if (duplicada) {
            throw new IllegalArgumentException("pregunta duplicada: " + pregunta.id());
        }
        preguntas.add(pregunta);
    }

    /**
     * Publica la encuesta. Exige al menos una pregunta con al menos
     * dos opciones cada una (ya garantizado por {@link Pregunta}).
     * Transición irreversible BORRADOR -&gt; PUBLICADA.
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

    /** Cierra la encuesta: deja de aceptar respuestas, sin mutar preguntas. */
    public synchronized void cerrar() {
        if (estado != EstadoEncuesta.PUBLICADA) {
            throw new IllegalStateException("solo una encuesta PUBLICADA puede cerrarse (estado=%s)".formatted(estado));
        }
        estado = EstadoEncuesta.CERRADA;
    }

    /**
     * Registra una respuesta ya validada por {@link EncuestaService}.
     * Solo se aceptan respuestas en estado PUBLICADA.
     */
    synchronized void agregarRespuesta(Respuesta respuesta) {
        Objects.requireNonNull(respuesta, "respuesta no puede ser null");
        if (estado != EstadoEncuesta.PUBLICADA) {
            throw new IllegalStateException("solo una encuesta PUBLICADA acepta respuestas (estado=%s)".formatted(estado));
        }
        if (!respuesta.encuestaId().equals(id)) {
            throw new IllegalArgumentException("la respuesta pertenece a otra encuesta");
        }
        respuestas.add(respuesta);
    }

    /** Copia inmodificable: publicar congela de facto el cuestionario. */
    public synchronized List<Pregunta> preguntas() {
        return List.copyOf(preguntas);
    }

    /** Copia inmodificable de respuestas anónimas. */
    public synchronized List<Respuesta> respuestas() {
        return List.copyOf(respuestas);
    }

    public synchronized int totalRespuestas() {
        return respuestas.size();
    }

    @Override
    public String toString() {
        return "Encuesta[id=%s, titulo=%s, estado=%s, preguntas=%d, respuestas=%d]"
                .formatted(id, titulo, estado, preguntas.size(), respuestas.size());
    }
}
