package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;

/**
 * MÓDULO A - SERVICIO DE ENCUESTAS
 * Responsable: Creación y publicación de encuestas
 * Integrante: Yo (rama pruebas)
 */
public class EncuestaService {
    private List<Encuesta> encuestas;

    public EncuestaService() {
        this.encuestas = new ArrayList<>();
    }

    /**
     * Crear una nueva encuesta
     */
    public Encuesta crearEncuesta(String titulo, String descripcion, String categoria) {
        Encuesta encuesta = new Encuesta(titulo, descripcion, categoria);
        encuestas.add(encuesta);
        return encuesta;
    }

    /**
     * Obtener todas las encuestas
     */
    public List<Encuesta> obtenerEncuestas() {
        return new ArrayList<>(encuestas);
    }

    /**
     * Obtener solo encuestas publicadas
     */
    public List<Encuesta> obtenerEncuestasPublicadas() {
        List<Encuesta> publicadas = new ArrayList<>();
        for (Encuesta e : encuestas) {
            if (e.isPublicada()) {
                publicadas.add(e);
            }
        }
        return publicadas;
    }

    /**
     * Buscar encuesta por ID
     */
    public Encuesta obtenerEncuestaPorId(String id) {
        for (Encuesta e : encuestas) {
            if (e.getId().equals(id)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Publicar una encuesta
     */
    public void publicarEncuesta(String id) throws Exception {
        Encuesta encuesta = obtenerEncuestaPorId(id);
        if (encuesta == null) {
            throw new Exception("Encuesta no encontrada");
        }
        encuesta.publicar();
    }

    /**
     * Eliminar una encuesta
     */
    public boolean eliminarEncuesta(String id) {
        return encuestas.removeIf(e -> e.getId().equals(id));
    }

    /**
     * Obtener total de encuestas
     */
    public int obtenerTotalEncuestas() {
        return encuestas.size();
    }

    /**
     * Obtener total de preguntas en todas las encuestas
     */
    public int obtenerTotalPreguntas() {
        int total = 0;
        for (Encuesta e : encuestas) {
            total += e.getPreguntas().size();
        }
        return total;
    }
}
