package com.mycompany.gestordeencuestas;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de aplicación: creación/publicación (módulo A) y
 * respuestas/estadísticas (módulo B).
 *
 * <h2>Contrato de validación de respuestas (integración A+B)</h2>
 * <ol>
 *   <li>La encuesta debe existir y estar {@code PUBLICADA}.</li>
 *   <li>La selección no puede ser {@code null} ni vacía.</li>
 *   <li><b>Respuesta completa:</b> el mapa debe contener exactamente las
 *       preguntas de la encuesta, ni una menos (parcial bloqueada) ni una
 *       más (extra bloqueada): {@code seleccion.keySet() == preguntaIds}.</li>
 *   <li><b>Opción válida y propia:</b> cada {@code opcionId} debe pertenecer
 *       estrictamente a su {@code preguntaId} ({@link Pregunta#contieneOpcion});
 *       cualquier opción ajena —de otra pregunta u otra encuesta— o id
 *       inventado se rechaza con {@link IllegalArgumentException} y no se
 *       almacena nada.</li>
 *   <li><b>Anonimato:</b> esta API no recibe ni almacena ningún dato personal.
 *       Los ids los genera el sistema; el cliente solo elige entre ids
 *       previamente publicados.</li>
 * </ol>
 */
public final class EncuestaService {

    private final Map<UUID, Encuesta> encuestas = new ConcurrentHashMap<>();

    // ---------- Módulo A: creación / publicación ----------

    /** Crea una encuesta en BORRADOR y la registra. */
    public Encuesta crearEncuesta(String titulo) {
        var encuesta = Encuesta.nueva(titulo);
        encuestas.put(encuesta.id(), encuesta);
        return encuesta;
    }

    /**
     * Añade una pregunta a una encuesta en BORRADOR.
     * Los UUID de pregunta y opciones los genera el sistema (anti-manipulación).
     */
    public Pregunta agregarPregunta(UUID encuestaId, String textoPregunta, List<String> textosOpciones) {
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        var encuesta = obtenerEncuesta(encuestaId);
        var pregunta = Pregunta.de(textoPregunta, textosOpciones);
        encuesta.agregarPregunta(pregunta); // lanza IllegalStateException si publicada (Regla 1)
        return pregunta;
    }

    /** Publica la encuesta (transición irreversible). */
    public Encuesta publicar(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        encuesta.publicar();
        return encuesta;
    }

    /** Cierra la encuesta (deja de aceptar respuestas). */
    public Encuesta cerrar(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        encuesta.cerrar();
        return encuesta;
    }

    public Encuesta obtenerEncuesta(UUID encuestaId) {
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        var encuesta = encuestas.get(encuestaId);
        if (encuesta == null) {
            throw new NoSuchElementException("encuesta no encontrada: " + encuestaId);
        }
        return encuesta;
    }

    // ---------- Módulo B: respuestas y estadísticas ----------

    /**
     * Registra una respuesta anónima y completa.
     *
     * @param encuestaId id de la encuesta PUBLICADA
     * @param seleccion  mapa preguntaId -&gt; opcionId (copia defensiva interna)
     * @return la {@link Respuesta} anónima registrada
     * @throws IllegalStateException    si la encuesta no está PUBLICADA
     * @throws IllegalArgumentException si la respuesta es parcial, trae extras,
     *                                  referencia preguntas inexistentes u
     *                                  opciones ajenas/manipuladas
     */
    public Respuesta responder(UUID encuestaId, Map<UUID, UUID> seleccion) {
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        Objects.requireNonNull(seleccion, "seleccion no puede ser null (use un mapa pregunta->opcion)");
        var encuesta = obtenerEncuesta(encuestaId);

        if (!encuesta.estaPublicada()) {
            throw new IllegalStateException(
                    "solo una encuesta PUBLICADA acepta respuestas (estado=%s)".formatted(encuesta.estado()));
        }
        if (seleccion.isEmpty()) {
            throw new IllegalArgumentException("respuesta vacía bloqueada: debe responder todas las preguntas");
        }

        var preguntas = encuesta.preguntas();
        var idsEsperados = preguntas.stream().map(Pregunta::id).collect(java.util.stream.Collectors.toSet());

        // 1) Completitud exacta: evita respuesta parcial y extras.
        if (!seleccion.keySet().equals(idsEsperados)) {
            var faltantes = new java.util.HashSet<>(idsEsperados);
            faltantes.removeAll(seleccion.keySet());
            var sobrantes = new java.util.HashSet<>(seleccion.keySet());
            sobrantes.removeAll(idsEsperados);
            throw new IllegalArgumentException(
                    "respuesta incompleta o con extras bloqueada: faltantes=%s sobrantes=%s (se exigen %d preguntas)"
                            .formatted(faltantes, sobrantes, idsEsperados.size()));
        }

        // 2) Pertenencia estricta opción->pregunta (bloquea opción ajena/manipulada).
        var porId = new HashMap<UUID, Pregunta>();
        for (var p : preguntas) {
            porId.put(p.id(), p);
        }
        for (var entry : seleccion.entrySet()) {
            var pregunta = porId.get(entry.getKey()); // existe por el check anterior
            var opcionId = Objects.requireNonNull(entry.getValue(),
                    "opcionId no puede ser null para pregunta " + entry.getKey());
            if (!pregunta.contieneOpcion(opcionId)) {
                throw new IllegalArgumentException(
                        "opción ajena o manipulada bloqueada: %s no pertenece a la pregunta %s ('%s')"
                                .formatted(opcionId, pregunta.id(), pregunta.texto()));
            }
        }

        var respuesta = Respuesta.anonima(encuestaId, seleccion);
        encuesta.agregarRespuesta(respuesta);
        return respuesta;
    }

    /**
     * Calcula porcentajes 0-100 por opción de cada pregunta.
     *
     * @return mapa inmodificable preguntaId -&gt; (opcionId -&gt; porcentaje).
     *         Sin respuestas, todo es 0.0 (evita división por cero).
     */
    public Map<UUID, Map<UUID, Double>> resultados(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        var preguntas = encuesta.preguntas();
        var respuestas = encuesta.respuestas();
        double total = respuestas.size();

        var salida = new LinkedHashMap<UUID, Map<UUID, Double>>();
        for (var pregunta : preguntas) {
            var conteo = new HashMap<UUID, Long>();
            for (var o : pregunta.opciones()) {
                conteo.put(o.id(), 0L);
            }
            for (var r : respuestas) {
                var elegida = r.seleccion().get(pregunta.id());
                if (elegida != null) {
                    conteo.merge(elegida, 1L, Long::sum);
                }
            }
            var porcentajes = new LinkedHashMap<UUID, Double>();
            for (var o : pregunta.opciones()) {
                double pct = (total == 0) ? 0.0 : (conteo.get(o.id()) * 100.0 / total);
                porcentajes.put(o.id(), pct);
            }
            salida.put(pregunta.id(), Collections.unmodifiableMap(porcentajes));
        }
        return Collections.unmodifiableMap(salida);
    }

    /** Conteo absoluto por opción (complementa a {@link #resultados}). */
    public Map<UUID, Map<UUID, Long>> conteo(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        var salida = new LinkedHashMap<UUID, Map<UUID, Long>>();
        for (var pregunta : encuesta.preguntas()) {
            var conteo = new HashMap<UUID, Long>();
            for (var o : pregunta.opciones()) {
                conteo.put(o.id(), 0L);
            }
            for (var r : encuesta.respuestas()) {
                var elegida = r.seleccion().get(pregunta.id());
                if (elegida != null) {
                    conteo.merge(elegida, 1L, Long::sum);
                }
            }
            salida.put(pregunta.id(), Collections.unmodifiableMap(new LinkedHashMap<>(conteo)));
        }
        return Collections.unmodifiableMap(salida);
    }

    public int totalRespuestas(UUID encuestaId) {
        return obtenerEncuesta(encuestaId).totalRespuestas();
    }
}
