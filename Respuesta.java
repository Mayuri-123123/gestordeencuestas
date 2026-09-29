package com.mycompany.gestordeencuestas;

import java.util.Date;

/**
 * MÓDULO B - CLASE RESPUESTA
 * Representa una respuesta anónima a una pregunta
 * Integrante: Yo (rama pruebas)
 * 
 * NOTA: No se almacenan datos personales (privacidad garantizada)
 */
public class Respuesta {
    private String id;
    private String encuestaId;
    private String preguntaId;
    private int opcionIndex;
    private Date fecha;

    public Respuesta(String encuestaId, String preguntaId, int opcionIndex) {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.encuestaId = encuestaId;
        this.preguntaId = preguntaId;
        this.opcionIndex = opcionIndex;
        this.fecha = new Date();
    }

    // Getters
    public String getId() { return id; }
    public String getEncuestaId() { return encuestaId; }
    public String getPreguntaId() { return preguntaId; }
    public int getOpcionIndex() { return opcionIndex; }
    public Date getFecha() { return fecha; }

    @Override
    public String toString() {
        return "Respuesta anónima - Opción: " + (opcionIndex + 1);
    }
}
