package com.mycompany.gestordeencuestas;

// ============================================================================
// ¿PARA QUÉ SIRVE ESTE ARCHIVO?
// ----------------------------------------------------------------------------
// Define los ESTADOS por los que pasa una encuesta durante su vida útil.
// Es un "enum": una lista cerrada de valores permitidos, así nadie puede
// inventar un estado inexistente (por ejemplo "APROBADA") por error.
// ============================================================================

/**
 * Ciclo de vida de una {@link Encuesta}.
 *
 * <p>Transiciones permitidas (solo hacia adelante, nunca hacia atrás):
 * <pre>
 *   BORRADOR  --publicar()-->  PUBLICADA  --cerrar()-->  CERRADA
 * </pre>
 *
 * <ul>
 *   <li><b>BORRADOR:</b> la encuesta se está armando; SÍ se pueden agregar preguntas.</li>
 *   <li><b>PUBLICADA:</b> la encuesta está abierta al público; YA NO se pueden
 *       cambiar sus preguntas (Regla 1) pero SÍ recibe respuestas.</li>
 *   <li><b>CERRADA:</b> la encuesta terminó; ya no acepta respuestas y sus
 *       preguntas siguen intactas (solo se pueden consultar resultados).</li>
 * </ul>
 */
public enum EstadoEncuesta {
    /** La encuesta se está creando; aún admite preguntas nuevas. */
    BORRADOR,
    /** La encuesta está abierta; recibe respuestas pero no cambia preguntas. */
    PUBLICADA,
    /** La encuesta terminó; solo permite ver resultados. */
    CERRADA
}
