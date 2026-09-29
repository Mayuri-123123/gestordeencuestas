/**
 * MÓDULO A - GESTIÓN DE ENCUESTAS
 * Responsable: Creación y publicación de encuestas
 * Integrante: Yo (rama pruebas)
 */

class Encuesta {
    constructor(titulo, descripcion, categoria) {
        this.id = Date.now().toString(36) + Math.random().toString(36).substr(2);
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.preguntas = [];
        this.publicada = false;
        this.fechaCreacion = new Date();
        this.fechaPublicacion = null;
    }

    agregarPregunta(pregunta) {
        this.preguntas.push(pregunta);
    }

    publicar() {
        if (this.preguntas.length === 0) {
            throw new Error('La encuesta debe tener al menos una pregunta');
        }
        this.publicada = true;
        this.fechaPublicacion = new Date();
    }
}

class Pregunta {
    constructor(texto) {
        this.id = Date.now().toString(36) + Math.random().toString(36).substr(2);
        this.texto = texto;
        this.opciones = [];
    }

    agregarOpcion(opcion) {
        this.opciones.push(opcion);
    }
}

class EncuestaService {
    constructor() {
        this.encuestas = [];
        this.cargarDeLocalStorage();
    }

    cargarDeLocalStorage() {
        const data = localStorage.getItem('encuestas');
        if (data) {
            this.encuestas = JSON.parse(data);
        }
    }

    guardarEnLocalStorage() {
        localStorage.setItem('encuestas', JSON.stringify(this.encuestas));
    }

    crearEncuesta(titulo, descripcion, categoria) {
        const encuesta = new Encuesta(titulo, descripcion, categoria);
        this.encuestas.push(encuesta);
        this.guardarEnLocalStorage();
        return encuesta;
    }

    obtenerEncuestas() {
        return this.encuestas;
    }

    obtenerEncuestasPublicadas() {
        return this.encuestas.filter(e => e.publicada);
    }

    obtenerEncuestaPorId(id) {
        return this.encuestas.find(e => e.id === id);
    }

    publicarEncuesta(id) {
        const encuesta = this.obtenerEncuestaPorId(id);
        if (!encuesta) {
            throw new Error('Encuesta no encontrada');
        }
        encuesta.publicar();
        this.guardarEnLocalStorage();
        return encuesta;
    }

    eliminarEncuesta(id) {
        this.encuestas = this.encuestas.filter(e => e.id !== id);
        this.guardarEnLocalStorage();
    }

    // Estadísticas
    obtenerTotalEncuestas() {
        return this.encuestas.length;
    }

    obtenerTotalPreguntas() {
        return this.encuestas.reduce((total, e) => total + e.preguntas.length, 0);
    }
}

// Instancia global del servicio
const encuestaService = new EncuestaService();
