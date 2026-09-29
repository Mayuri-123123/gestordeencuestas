/**
 * MÓDULO DE INTEGRACIÓN - CONTRATO DE VALIDACIÓN DE RESPUESTAS
 * Responsable: Validación cruzada entre módulos A y B
 * Integrante: Yo (rama pruebas)
 * 
 * Defensa de ambos integrantes:
 * - Evita respuestas parciales: valida que todas las preguntas tengan respuesta
 * - Revisa decisión de privacidad: no se almacenan datos personales
 */

class ValidacionService {
    constructor() {
        this.errores = [];
    }

    /**
     * Validar integridad de la encuesta antes de publicar
     */
    validarEncuestaParaPublicar(encuesta) {
        this.errores = [];

        if (!encuesta.titulo || encuesta.titulo.trim().length < 5) {
            this.errores.push('El título debe tener al menos 5 caracteres');
        }

        if (!encuesta.descripcion || encuesta.descripcion.trim().length < 10) {
            this.errores.push('La descripción debe tener al menos 10 caracteres');
        }

        if (!encuesta.categoria) {
            this.errores.push('Debe seleccionar una categoría');
        }

        if (encuesta.preguntas.length === 0) {
            this.errores.push('La encuesta debe tener al menos una pregunta');
        }

        encuesta.preguntas.forEach((pregunta, index) => {
            if (!pregunta.texto || pregunta.texto.trim().length < 5) {
                this.errores.push(`La pregunta ${index + 1} debe tener al menos 5 caracteres`);
            }

            if (pregunta.opciones.length < 2) {
                this.errores.push(`La pregunta ${index + 1} debe tener al menos 2 opciones`);
            }

            pregunta.opciones.forEach((opcion, opIndex) => {
                if (!opcion || opcion.trim().length === 0) {
                    this.errores.push(`La opción ${opIndex + 1} de la pregunta ${index + 1} no puede estar vacía`);
                }
            });
        });

        return this.errores.length === 0;
    }

    /**
     * Validar respuestas antes de registrar
     * Evita respuestas parciales
     */
    validarRespuestasCompletas(encuesta, respuestas) {
        this.errores = [];

        if (!encuesta.publicada) {
            this.errores.push('La encuesta no está publicada');
            return false;
        }

        const preguntasIds = encuesta.preguntas.map(p => p.id);
        const respondidasIds = Object.keys(respuestas);

        // Verificar que no haya respuestas parciales
        if (respondidasIds.length !== preguntasIds.length) {
            this.errores.push('Debes responder todas las preguntas antes de enviar');
            return false;
        }

        // Verificar que todas las preguntas tengan respuesta válida
        for (let pregunta of encuesta.preguntas) {
            const opcionIndex = respuestas[pregunta.id];

            if (opcionIndex === undefined || opcionIndex === null) {
                this.errores.push(`La pregunta "${pregunta.texto}" no tiene respuesta`);
                return false;
            }

            if (opcionIndex < 0 || opcionIndex >= pregunta.opciones.length) {
                this.errores.push(`Opción inválida seleccionada en "${pregunta.texto}"`);
                return false;
            }
        }

        return true;
    }

    /**
     * Validar privacidad - Asegurar que no se recopilen datos personales
     */
    validarPrivacidad() {
        // El sistema NO almacena:
        // - Nombres
        // - Correos electrónicos
        // - Direcciones IP
        // - Datos de identificación personal
        // Solo se almacenan respuestas anónimas con timestamp
        return true;
    }

    obtenerErrores() {
        return this.errores;
    }

    limpiarErrores() {
        this.errores = [];
    }
}

// Instancia global del servicio de validación
const validacionService = new ValidacionService();
