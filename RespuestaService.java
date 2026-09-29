package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MODULO B - SERVICIO DE RESPUESTAS Y ESTADISTICAS
 * Responsable: Registro de respuestas y calculo de estadisticas
 * Integrante: Yo (rama pruebas)
 */
public class RespuestaService {
    private List<Respuesta> respuestas;
    private EncuestaService encuestaService;

    public RespuestaService(EncuestaService encuestaService) {
        this.respuestas = new ArrayList<Respuesta>();
        this.encuestaService = encuestaService;
    }

    /**
     * Registrar respuestas de una encuesta
     * Validacion: todas las preguntas deben tener respuesta (evita respuestas parciales)
     */
    public void registrarRespuestas(String encuestaId, Map<String, Integer> respuestasMap) throws Exception {
        Encuesta encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
        if (encuesta == null) {
            throw new Exception("Encuesta no encontrada");
        }

        if (!encuesta.isPublicada()) {
            throw new Exception("La encuesta no esta publicada");
        }

        // Validacion: verificar que todas las preguntas tengan respuesta
        List<String> preguntasIds = new ArrayList<String>();
        for (Pregunta p : encuesta.getPreguntas()) {
            preguntasIds.add(p.getId());
        }

        if (respuestasMap.size() != preguntasIds.size()) {
            throw new Exception("Debes responder todas las preguntas antes de enviar");
        }

        // Validacion: verificar que las opciones sean validas
        for (Pregunta pregunta : encuesta.getPreguntas()) {
            Integer opcionIndex = respuestasMap.get(pregunta.getId());
            if (opcionIndex == null) {
                throw new Exception("La pregunta '" + pregunta.getTexto() + "' no tiene respuesta");
            }
            if (opcionIndex < 0 || opcionIndex >= pregunta.getOpciones().size()) {
                throw new Exception("Opcion invalida para la pregunta '" + pregunta.getTexto() + "'");
            }
        }

        // Registrar respuestas
        for (Pregunta pregunta : encuesta.getPreguntas()) {
            Respuesta respuesta = new Respuesta(encuestaId, pregunta.getId(), respuestasMap.get(pregunta.getId()));
            respuestas.add(respuesta);
        }
    }

    /**
     * Obtener respuestas por encuesta
     */
    public List<Respuesta> obtenerRespuestasPorEncuesta(String encuestaId) {
        List<Respuesta> resultado = new ArrayList<Respuesta>();
        for (Respuesta r : respuestas) {
            if (r.getEncuestaId().equals(encuestaId)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * Calcular estadisticas de una encuesta
     * Retorna porcentajes por opcion para cada pregunta
     */
    public List<EstadisticaPregunta> calcularEstadisticas(String encuestaId) throws Exception {
        Encuesta encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
        if (encuesta == null) {
            throw new Exception("Encuesta no encontrada");
        }

        List<Respuesta> respuestasEncuesta = obtenerRespuestasPorEncuesta(encuestaId);
        List<EstadisticaPregunta> estadisticas = new ArrayList<EstadisticaPregunta>();

        for (Pregunta pregunta : encuesta.getPreguntas()) {
            EstadisticaPregunta stat = new EstadisticaPregunta(pregunta.getTexto());
            int totalRespuestas = 0;

            // Contar respuestas por opcion
            for (int i = 0; i < pregunta.getOpciones().size(); i++) {
                int count = 0;
                for (Respuesta r : respuestasEncuesta) {
                    if (r.getPreguntaId().equals(pregunta.getId()) && r.getOpcionIndex() == i) {
                        count++;
                    }
                }
                totalRespuestas += count;
                stat.agregarOpcionEstadistica(pregunta.getOpciones().get(i), count);
            }

            // Calcular porcentajes
            stat.calcularPorcentajes(totalRespuestas);
            estadisticas.add(stat);
        }

        return estadisticas;
    }

    /**
     * Obtener total de respuestas
     */
    public int obtenerTotalRespuestas() {
        return respuestas.size();
    }

    /**
     * Clase interna para estadisticas de una pregunta
     */
    public static class EstadisticaPregunta {
        private String pregunta;
        private List<OpcionEstadistica> opciones;

        public EstadisticaPregunta(String pregunta) {
            this.pregunta = pregunta;
            this.opciones = new ArrayList<OpcionEstadistica>();
        }

        public void agregarOpcionEstadistica(String opcion, int count) {
            opciones.add(new OpcionEstadistica(opcion, count));
        }

        public void calcularPorcentajes(int total) {
            for (OpcionEstadistica op : opciones) {
                op.calcularPorcentaje(total);
            }
        }

        public String getPregunta() { return pregunta; }
        public List<OpcionEstadistica> getOpciones() { return opciones; }
    }

    /**
     * Clase interna para estadisticas de una opcion
     */
    public static class OpcionEstadistica {
        private String opcion;
        private int count;
        private double porcentaje;

        public OpcionEstadistica(String opcion, int count) {
            this.opcion = opcion;
            this.count = count;
            this.porcentaje = 0.0;
        }

        public void calcularPorcentaje(int total) {
            if (total > 0) {
                this.porcentaje = Math.round((count * 100.0 / total) * 100.0) / 100.0;
            }
        }

        public String getOpcion() { return opcion; }
        public int getCount() { return count; }
        public double getPorcentaje() { return porcentaje; }
    }
}
