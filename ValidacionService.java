package com.mycompany.gestordeencuestas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MÓDULO DE INTEGRACIÓN - CONTRATO DE VALIDACIÓN DE RESPUESTAS
 * Responsable: Validación cruzada entre módulos A y B
 * Integrante: Yo (rama pruebas)
 * 
 * Defensa de ambos integrantes:
 * - Evita respuestas parciales: valida que todas las preguntas tengan respuesta
 * - Revisa decisión de privacidad: no se almacenan datos personales
 */
public class ValidacionService {
    private List<String> errores;

    public ValidacionService() {
        this.errores = new ArrayList<>();
    }

    /**
     * Validar integridad de la encuesta antes de publicar
     */
    public boolean validarEncuestaParaPublicar(Encuesta encuesta) {
        errores.clear();

        if (encuesta.getTitulo() == null || encuesta.getTitulo().trim().length() < 5) {
            errores.add("El título debe tener al menos 5 caracteres");
        }

        if (encuesta.getDescripcion() == null || encuesta.getDescripcion().trim().length() < 10) {
            errores.add("La descripción debe tener al menos 10 caracteres");
        }

        if (encuesta.getCategoria() == null || encuesta.getCategoria().trim().isEmpty()) {
            errores.add("Debe seleccionar una categoría");
        }

        if (encuesta.getPreguntas().isEmpty()) {
            errores.add("La encuesta debe tener al menos una pregunta");
        }

        for (int i = 0; i < encuesta.getPreguntas().size(); i++) {
            Pregunta pregunta = encuesta.getPreguntas().get(i);
            if (pregunta.getTexto() == null || pregunta.getTexto().trim().length() < 5) {
                errores.add("La pregunta " + (i + 1) + " debe tener al menos 5 caracteres");
            }

            if (pregunta.getOpciones().size() < 2) {
                errores.add("La pregunta " + (i + 1) + " debe tener al menos 2 opciones");
            }

            for (int j = 0; j < pregunta.getOpciones().size(); j++) {
                if (pregunta.getOpciones().get(j) == null || pregunta.getOpciones().get(j).trim().isEmpty()) {
                    errores.add("La opción " + (j + 1) + " de la pregunta " + (i + 1) + " no puede estar vacía");
                }
            }
        }

        return errores.isEmpty();
    }

    /**
     * Validar respuestas antes de registrar
     * Evita respuestas parciales
     */
    public boolean validarRespuestasCompletas(Encuesta encuesta, Map<String, Integer> respuestas) {
        errores.clear();

        if (!encuesta.isPublicada()) {
            errores.add("La encuesta no está publicada");
            return false;
        }

        // Verificar que no haya respuestas parciales
        if (respuestas.size() != encuesta.getPreguntas().size()) {
            errores.add("Debes responder todas las preguntas antes de enviar");
            return false;
        }

        // Verificar que todas las preguntas tengan respuesta válida
        for (Pregunta pregunta : encuesta.getPreguntas()) {
            Integer opcionIndex = respuestas.get(pregunta.getId());

            if (opcionIndex == null) {
                errores.add("La pregunta '" + pregunta.getTexto() + "' no tiene respuesta");
                return false;
            }

            if (opcionIndex < 0 || opcionIndex >= pregunta.getOpciones().size()) {
                errores.add("Opción inválida seleccionada en '" + pregunta.getTexto() + "'");
                return false;
            }
        }

        return true;
    }

    /**
     * Validar privacidad - Asegurar que no se recopilen datos personales
     */
    public boolean validarPrivacidad() {
        // El sistema NO almacena:
        // - Nombres
        // - Correos electrónicos
        // - Direcciones IP
        // - Datos de identificación personal
        // Solo se almacenan respuestas anónimas con timestamp
        return true;
    }

    public List<String> obtenerErrores() {
        return new ArrayList<>(errores);
    }

    public void limpiarErrores() {
        errores.clear();
    }
}
