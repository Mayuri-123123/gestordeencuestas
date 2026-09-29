package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MÓDULO B - SERVICIO DE RESPUESTAS Y ESTADÍSTICAS
 * Responsable: Registro de respuestas y cálculo de estadísticas
 * Integrante: Yo (rama pruebas)
 */
public class RespuestaService {
    private List<Respuesta> respuestas;
    private EncuestaService encuestaService;

    public RespuestaService(EncuestaService encuestaService) {
        this.respuestas = new ArrayList<>();
        this.encuestaService = encuestaService;
    }

    /**
     * Registrar respuestas de una encuesta
     * Validación: todas las preguntas deben tener respuesta (evita respuestas parciales)
     */
    public void registrarRespuestas(String encuestaId, Map<String, Integer> respuestasMap) throws Exception {
        Encuesta encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
        if (encuesta == null) {
            throw new Exception("Encuesta no encontrada");
        }

        if (!encuesta.isPublicada()) {
            throw new Exception("La encuesta no está publicada");
        }

        // Validación: verificar que todas las preguntas tengan respuesta
        List<String> preguntasIds = new ArrayList<>();
        for (Pregunta p : encuesta.getPreguntas()) {
            preguntasIds.add(p.getId());
        }

        if (respuestasMap.size() != preguntasIds.size()) {
            throw new Exception("Debes responder todas las preguntas antes de enviar");
        }

        // Validación: verificar que las opciones sean válidas
        for (Pregunta pregunta : encuesta.getPreguntas()) {
            Integer opcionIndex = respuestasMap.get(pregunta.getId());
            if (opcionIndex == null) {
                throw new Exception("La pregunta '" + pregunta.getTexto() + "' no tiene respuesta");
            }
            if (opcionIndex < 0 || opcionIndex >= pregunta.getOpciones().size()) {
                throw new Exception("Opción inválida para la pregunta '" + pregunta.getTexto() + "'");
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
        List<Respuesta> resultado = new ArrayList<>();
        for (Respuesta r : respuestas) {
            if (r.getEncuestaId().equals(encuestaId)) {
                resultado.add(r);
            }
        }
        return resultado;
    }

    /**
     * Calcular estadísticas de una encuesta
     * Retorna porcentajes por opción para cada pregunta
     */
    public List<EstadisticaPregunta> calcularEstadisticas(String encuestaId) throws Exception {
        Encuesta encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
        if (encuesta == null) {
            throw new Exception("Encuesta no encontrada");
        }

        List<Respuesta> respuestasEncuesta = obtenerRespuestasPorEncuesta(encuestaId);
        List<EstadisticaPregunta> estadisticas = new ArrayList<>();

        for (Pregunta pregunta : encuesta.getPreguntas()) {
            EstadisticaPregunta stat = new EstadisticaPregunta(pregunta.getTexto());
            int totalRespuestas = 0;

            // Contar respuestas por opción
            for (int i = 0; i < pregunta.getOpciones().size(); i++) {
                int count = 0;
                for (Respuesta r : respuestasEncuesta) {
                    if (r.getPreguntaId().equals(pregunta.getId()) && r.getOpcionIndex() == i) {
                        count++;
                    }
                }
                totalRespuestas += count;
                stat.agregarOpcion Estadistica(pregunta.getOpciones().get(i), count);
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
     * Clase interna para estadísticas de una pregunta
     */
    public static class EstadisticaPregunta {
        private String pregunta;
        private List<OpcionEstadistica> opciones;

        public EstadisticaPregunta(String pregunta) {
            this.pregunta = pregunta;
            this.opciones = new ArrayList<>();
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
     * Clase interna para estadísticas de una opción
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
