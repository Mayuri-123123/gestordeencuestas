/**
 * MÓDULO B - RESPUESTAS Y ESTADÍSTICAS
 * Responsable: Registro de respuestas y cálculo de estadísticas
 * Integrante: Yo (rama pruebas)
 */

class Respuesta {
    constructor(encuestaId, preguntaId, opcionIndex) {
        this.id = Date.now().toString(36) + Math.random().toString(36).substr(2);
        this.encuestaId = encuestaId;
        this.preguntaId = preguntaId;
        this.opcionIndex = opcionIndex;
        this.fecha = new Date();
    }
}

class RespuestaService {
    constructor() {
        this.respuestas = [];
        this.cargarDeLocalStorage();
    }

    cargarDeLocalStorage() {
        const data = localStorage.getItem('respuestas');
        if (data) {
            this.respuestas = JSON.parse(data);
        }
    }

    guardarEnLocalStorage() {
        localStorage.setItem('respuestas', JSON.stringify(this.respuestas));
    }

    /**
     * Registrar respuestas de una encuesta
     * Validación: todas las preguntas deben tener respuesta (evita respuestas parciales)
     */
    registrarRespuestas(encuestaId, respuestas) {
        const encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
        if (!encuesta) {
            throw new Error('Encuesta no encontrada');
        }

        if (!encuesta.publicada) {
            throw new Error('La encuesta no está publicada');
        }

        // Validación: verificar que todas las preguntas tengan respuesta
        const preguntasIds = encuesta.preguntas.map(p => p.id);
        const respondidasIds = Object.keys(respuestas);

        if (preguntasIds.length !== respondidasIds.length) {
            throw new Error('Debes responder todas las preguntas antes de enviar');
        }

        // Validación: verificar que las opciones sean válidas
        for (let pregunta of encuesta.preguntas) {
            const opcionIndex = respuestas[pregunta.id];
            if (opcionIndex === undefined || opcionIndex === null) {
                throw new Error(`La pregunta "${pregunta.texto}" no tiene respuesta`);
            }
            if (opcionIndex < 0 || opcionIndex >= pregunta.opciones.length) {
                throw new Error(`Opción inválida para la pregunta "${pregunta.texto}"`);
            }
        }

        // Registrar respuestas
        for (let pregunta of encuesta.preguntas) {
            const respuesta = new Respuesta(
                encuestaId,
                pregunta.id,
                respuestas[pregunta.id]
            );
            this.respuestas.push(respuesta);
        }

        this.guardarEnLocalStorage();
        return true;
    }

    obtenerRespuestasPorEncuesta(encuestaId) {
        return this.respuestas.filter(r => r.encuestaId === encuestaId);
    }

    /**
     * Calcular estadísticas de una encuesta
     * Retorna porcentajes por opción para cada pregunta
     */
    calcularEstadisticas(encuestaId) {
        const encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
        if (!encuesta) {
            throw new Error('Encuesta no encontrada');
        }

        const respuestas = this.obtenerRespuestasPorEncuesta(encuestaId);
        const estadisticas = [];

        for (let pregunta of encuesta.preguntas) {
            const preguntaRespuestas = respuestas.filter(r => r.preguntaId === pregunta.id);
            const totalRespuestas = preguntaRespuestas.length;

            const opcionStats = pregunta.opciones.map((opcion, index) => {
                const count = preguntaRespuestas.filter(r => r.opcionIndex === index).length;
                const porcentaje = totalRespuestas > 0 ? (count / totalRespuestas) * 100 : 0;

                return {
                    opcion: opcion,
                    count: count,
                    porcentaje: Math.round(porcentaje * 100) / 100
                };
            });

            estadisticas.push({
                pregunta: pregunta.texto,
                totalRespuestas: totalRespuestas,
                opciones: opcionStats
            });
        }

        return estadisticas;
    }

    obtenerTotalRespuestas() {
        return this.respuestas.length;
    }
}

// Instancia global del servicio
const respuestaService = new RespuestaService();
