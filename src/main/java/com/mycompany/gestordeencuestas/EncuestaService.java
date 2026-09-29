package com.mycompany.gestordeencuestas;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Es el SERVICIO que coordina todo: crear encuestas, publicarlas, recibir
// respuestas y calcular resultados. La interfaz gráfica (EncuestaGUI) y el
// programa de consola (GestorDeEncuestas) NO tocan los datos directamente:
// siempre pasan por aquí, así todas las reglas se aplican en un solo lugar.
// ----------------------------------------------------------------------------
// Se divide en dos módulos (reparto del trabajo en equipo):
//   MÓDULO A (creación/publicación): crearEncuesta, agregarPregunta, publicar
//   MÓDULO B (respuestas/estadísticas): responder, resultados, conteo
// ----------------------------------------------------------------------------
// Aquí vive el CONTRATO DE VALIDACIÓN (integración A+B): los 5 controles que
// toda respuesta debe pasar antes de guardarse (ver método responder).
// ============================================================================

/**
 * Servicio de aplicación: creación/publicación (módulo A) y
 * respuestas/estadísticas (módulo B).
 */
public final class EncuestaService {

    // Almacén en memoria de todas las encuestas, por id.
    // ConcurrentHashMap = seguro aunque varios usuarios voten a la vez.
    private final Map<UUID, Encuesta> encuestas = new ConcurrentHashMap<>();

    // ================= MÓDULO A: creación / publicación =================

    /**
     * ¿QUÉ HACE? Crea una encuesta nueva en estado BORRADOR y la guarda.
     * ¿PARA QUÉ? Es el primer paso: sin encuesta no hay preguntas.
     *
     * @param titulo título visible de la encuesta
     * @return la encuesta creada (en BORRADOR, lista para agregar preguntas)
     */
    public Encuesta crearEncuesta(String titulo) {
        var encuesta = Encuesta.nueva(titulo);
        encuestas.put(encuesta.id(), encuesta);
        return encuesta;
    }

    /**
     * ¿QUÉ HACE? Crea una pregunta (con sus opciones) y la agrega a una
     * encuesta que aún esté en BORRADOR.
     * ¿PARA QUÉ? Para armar el cuestionario antes de publicar.
     * SEGURIDAD: los ids los genera el sistema; quien llama solo da textos.
     *
     * @param encuestaId     a qué encuesta agregar la pregunta
     * @param textoPregunta  enunciado, ej: "¿Dónde prefieres estudiar?"
     * @param textosOpciones opciones, ej: List.of("Casa", "Biblioteca", "Café")
     * @return la pregunta creada y agregada
     */
    public Pregunta agregarPregunta(UUID encuestaId, String textoPregunta, List<String> textosOpciones) {
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        var encuesta = obtenerEncuesta(encuestaId);
        var pregunta = Pregunta.de(textoPregunta, textosOpciones);
        // Si la encuesta ya está publicada, aquí salta IllegalStateException (Regla 1).
        encuesta.agregarPregunta(pregunta);
        return pregunta;
    }

    /**
     * ¿QUÉ HACE? Publica la encuesta (BORRADOR -&gt; PUBLICADA).
     * ¿PARA QUÉ? Abre la encuesta al público y CONGELA el cuestionario.
     */
    public Encuesta publicar(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        encuesta.publicar();
        return encuesta;
    }

    /**
     * ¿QUÉ HACE? Cierra la encuesta (PUBLICADA -&gt; CERRADA).
     * ¿PARA QUÉ? Termina la recolección; solo quedan visibles los resultados.
     */
    public Encuesta cerrar(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        encuesta.cerrar();
        return encuesta;
    }

    /**
     * ¿QUÉ HACE? Busca una encuesta por su id.
     * ¿PARA QUÉ? Todos los métodos la usan para encontrar la encuesta antes
     * de operar con ella.
     *
     * @throws NoSuchElementException si el id no existe
     */
    public Encuesta obtenerEncuesta(UUID encuestaId) {
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        var encuesta = encuestas.get(encuestaId);
        if (encuesta == null) {
            throw new NoSuchElementException("encuesta no encontrada: " + encuestaId);
        }
        return encuesta;
    }

    // ================= MÓDULO B: respuestas y estadísticas =================

    /**
     * ¿QUÉ HACE? Registra UNA respuesta anónima y completa.
     * ¿PARA QUÉ? Es la operación de "votar": el público elige una opción
     * por cada pregunta, sin dar ningún dato personal.
     *
     * <h2>CONTRATO DE VALIDACIÓN (la respuesta debe pasar los 5 controles):</h2>
     * <ol>
     *   <li>La encuesta existe y está PUBLICADA (si no, error).</li>
     *   <li>La selección no es null ni vacía.</li>
     *   <li>RESPUESTA COMPLETA: el mapa trae EXACTAMENTE las preguntas de la
     *       encuesta (ni una menos = parcial bloqueada, ni una más = extra).</li>
     *   <li>OPCIÓN VÁLIDA Y PROPIA: cada opción pertenece estrictamente a su
     *       pregunta; lo ajeno o inventado se rechaza y NO se guarda nada.</li>
     *   <li>ANONIMATO: no se pide ni se guarda ningún dato personal.</li>
     * </ol>
     *
     * @param encuestaId id de la encuesta PUBLICADA
     * @param seleccion  mapa preguntaId -&gt; opcionId con TODAS las preguntas
     * @return la Respuesta anónima registrada
     */
    public Respuesta responder(UUID encuestaId, Map<UUID, UUID> seleccion) {
        Objects.requireNonNull(encuestaId, "encuestaId no puede ser null");
        Objects.requireNonNull(seleccion, "seleccion no puede ser null (use un mapa pregunta->opcion)");
        var encuesta = obtenerEncuesta(encuestaId);

        // Control 1: solo una encuesta ABIERTA recibe votos.
        if (!encuesta.estaPublicada()) {
            throw new IllegalStateException(
                    "solo una encuesta PUBLICADA acepta respuestas (estado=%s)".formatted(encuesta.estado()));
        }
        // Control 2: no se aceptan votos vacíos.
        if (seleccion.isEmpty()) {
            throw new IllegalArgumentException("respuesta vacía bloqueada: debe responder todas las preguntas");
        }

        var preguntas = encuesta.preguntas();
        // Conjunto de preguntas que la encuesta EXIGE responder.
        var idsEsperados = preguntas.stream().map(Pregunta::id).collect(Collectors.toSet());

        // Control 3: COMPLETITUD EXACTA. Compara lo recibido con lo exigido:
        // si falta alguna pregunta (parcial) o sobra alguna (extra), se bloquea
        // y se informa CUÁLES faltan y cuáles sobran para facilitar la defensa.
        if (!seleccion.keySet().equals(idsEsperados)) {
            var faltantes = new HashSet<>(idsEsperados);
            faltantes.removeAll(seleccion.keySet());
            var sobrantes = new HashSet<>(seleccion.keySet());
            sobrantes.removeAll(idsEsperados);
            throw new IllegalArgumentException(
                    "respuesta incompleta o con extras bloqueada: faltantes=%s sobrantes=%s (se exigen %d preguntas)"
                            .formatted(faltantes, sobrantes, idsEsperados.size()));
        }

        // Control 4: PERTENENCIA ESTRICTA. Cada opción debe ser DE SU pregunta.
        // Se usa un mapa auxiliar preguntaId -> Pregunta para buscar rápido.
        var porId = new HashMap<UUID, Pregunta>();
        for (var p : preguntas) {
            porId.put(p.id(), p);
        }
        for (var entry : seleccion.entrySet()) {
            var pregunta = porId.get(entry.getKey()); // existe (control 3 ya pasó)
            var opcionId = Objects.requireNonNull(entry.getValue(),
                    "opcionId no puede ser null para pregunta " + entry.getKey());
            if (!pregunta.contieneOpcion(opcionId)) {
                throw new IllegalArgumentException(
                        "opción ajena o manipulada bloqueada: %s no pertenece a la pregunta %s ('%s')"
                                .formatted(opcionId, pregunta.id(), pregunta.texto()));
            }
        }

        // Todo válido: se crea la respuesta anónima y se guarda. Nada personal.
        var respuesta = Respuesta.anonima(encuestaId, seleccion);
        encuesta.agregarRespuesta(respuesta);
        return respuesta;
    }

    /**
     * ¿QUÉ HACE? Calcula el PORCENTAJE (0-100) de cada opción en cada pregunta.
     * ¿PARA QUÉ? Para mostrar resultados tipo "1-3h: 66.7%".
     * Si aún no hay votos, todo es 0.0 (así se evita dividir entre cero).
     *
     * @return mapa preguntaId -&gt; (opcionId -&gt; porcentaje), inmodificable
     */
    public Map<UUID, Map<UUID, Double>> resultados(UUID encuestaId) {
        var encuesta = obtenerEncuesta(Objects.requireNonNull(encuestaId));
        var preguntas = encuesta.preguntas();
        var respuestas = encuesta.respuestas();
        double total = respuestas.size();

        var salida = new LinkedHashMap<UUID, Map<UUID, Double>>();
        for (var pregunta : preguntas) {
            // Contamos votos por opción...
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
            // ...y los convertimos a porcentaje.
            var porcentajes = new LinkedHashMap<UUID, Double>();
            for (var o : pregunta.opciones()) {
                double pct = (total == 0) ? 0.0 : (conteo.get(o.id()) * 100.0 / total);
                porcentajes.put(o.id(), pct);
            }
            salida.put(pregunta.id(), Collections.unmodifiableMap(porcentajes));
        }
        return Collections.unmodifiableMap(salida);
    }

    /**
     * ¿QUÉ HACE? Cuenta los VOTOS (números absolutos) de cada opción.
     * ¿PARA QUÉ? Complementa a resultados(): "Biblioteca: 2 votos (66.7%)".
     *
     * @return mapa preguntaId -&gt; (opcionId -&gt; votos), inmodificable
     */
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

    /**
     * ¿QUÉ HACE? Dice cuántas respuestas anónimas tiene la encuesta.
     */
    public int totalRespuestas(UUID encuestaId) {
        return obtenerEncuesta(encuestaId).totalRespuestas();
    }
}
