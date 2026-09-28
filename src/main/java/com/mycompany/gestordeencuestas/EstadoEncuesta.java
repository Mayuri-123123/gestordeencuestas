package com.mycompany.gestordeencuestas;

/**
 * Estados del ciclo de vida de una encuesta.
 * Transición permitida: BORRADOR -&gt; PUBLICADA -&gt; CERRADA.
 * No existe transición de regreso: una encuesta publicada jamás
 * vuelve a borrador (Regla 1 del dominio).
 */
public enum EstadoEncuesta {
    BORRADOR,
    PUBLICADA,
    CERRADA
}
