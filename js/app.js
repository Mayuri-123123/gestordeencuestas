/**
 * APLICACIÓN PRINCIPAL - GESTOR DE ENCUESTAS
 * Responsable: Interfaz de usuario y experiencia
 * Integrante: Yo (rama pruebas)
 */

// Estado global de la aplicación
let currentStep = 1;
let currentCategory = 'comida';
let preguntasTemp = [];
let encuestaEditando = null;

// ============================================
// INICIALIZACIÓN
// ============================================
document.addEventListener('DOMContentLoaded', function() {
    // Simular carga
    setTimeout(() => {
        document.getElementById('loading-screen').classList.add('hidden');
        initApp();
    }, 1500);
});

function initApp() {
    setupNavigation();
    setupCategorySelection();
    updateStats();
    loadEncuestasPublicadas();
    loadResultados();
}

// ============================================
// NAVEGACIÓN
// ============================================
function setupNavigation() {
    const navBtns = document.querySelectorAll('.nav-btn');
    navBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const section = this.dataset.section;
            navigateTo(section);
        });
    });
}

function navigateTo(sectionId) {
    // Actualizar botones de navegación
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.section === sectionId) {
            btn.classList.add('active');
        }
    });

    // Mostrar sección correspondiente
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(sectionId).classList.add('active');

    // Cargar datos según la sección
    if (sectionId === 'responder') {
        loadEncuestasPublicadas();
    } else if (sectionId === 'resultados') {
        loadResultados();
    } else if (sectionId === 'crear') {
        resetForm();
    }
}

// ============================================
// FORMULARIO DE CREACIÓN
// ============================================
function setupCategorySelection() {
    const categoryCards = document.querySelectorAll('.category-card');
    categoryCards.forEach(card => {
        card.addEventListener('click', function() {
            categoryCards.forEach(c => c.classList.remove('selected'));
            this.classList.add('selected');
            currentCategory = this.dataset.category;
        });
    });
}

function nextStep(step) {
    if (step === 2) {
        // Validar paso 1
        const titulo = document.getElementById('encuesta-titulo').value.trim();
        const descripcion = document.getElementById('encuesta-descripcion').value.trim();

        if (titulo.length < 5) {
            showToast('error', 'Título muy corto', 'El título debe tener al menos 5 caracteres');
            return;
        }
        if (descripcion.length < 10) {
            showToast('error', 'Descripción muy corta', 'La descripción debe tener al menos 10 caracteres');
            return;
        }
    }

    if (step === 3) {
        // Validar preguntas
        if (preguntasTemp.length === 0) {
            showToast('error', 'Sin preguntas', 'Agrega al menos una pregunta a tu encuesta');
            return;
        }

        for (let i = 0; i < preguntasTemp.length; i++) {
            const p = preguntasTemp[i];
            if (p.texto.trim().length < 5) {
                showToast('error', 'Pregunta incompleta', `La pregunta ${i + 1} debe tener al menos 5 caracteres`);
                return;
            }
            if (p.opciones.length < 2) {
                showToast('error', 'Opciones insuficientes', `La pregunta ${i + 1} debe tener al menos 2 opciones`);
                return;
            }
            for (let j = 0; j < p.opciones.length; j++) {
                if (p.opciones[j].trim() === '') {
                    showToast('error', 'Opción vacía', `La opción ${j + 1} de la pregunta ${i + 1} no puede estar vacía`);
                    return;
                }
            }
        }
        renderReview();
    }

    document.querySelectorAll('.form-step').forEach(s => s.classList.remove('active'));
    document.getElementById('step-' + step).classList.add('active');
    currentStep = step;
}

function prevStep(step) {
    document.querySelectorAll('.form-step').forEach(s => s.classList.remove('active'));
    document.getElementById('step-' + step).classList.add('active');
    currentStep = step;
}

function resetForm() {
    currentStep = 1;
    preguntasTemp = [];
    document.getElementById('encuesta-titulo').value = '';
    document.getElementById('encuesta-descripcion').value = '';
    document.querySelectorAll('.category-card').forEach(c => c.classList.remove('selected'));
    document.querySelector('.category-card[data-category="comida"]').classList.add('selected');
    currentCategory = 'comida';
    document.getElementById('preguntas-container').innerHTML = '';
    document.querySelectorAll('.form-step').forEach(s => s.classList.remove('active'));
    document.getElementById('step-1').classList.add('active');
}

// ============================================
// GESTIÓN DE PREGUNTAS
// ============================================
function addPregunta() {
    const container = document.getElementById('preguntas-container');
    const numero = preguntasTemp.length + 1;

    const preguntaDiv = document.createElement('div');
    preguntaDiv.className = 'pregunta-card';
    preguntaDiv.innerHTML = `
        <div class="pregunta-header">
            <span class="pregunta-number">Pregunta ${numero}</span>
            <button class="btn-remove" onclick="removePregunta(this)">
                <i class="fas fa-trash"></i>
            </button>
        </div>
        <div class="form-group">
            <input type="text" class="form-input pregunta-texto" placeholder="Escribe tu pregunta aquí..." onchange="updatePreguntaTexto(this)">
        </div>
        <div class="opciones-container">
            <div class="opcion-input">
                <input type="text" class="form-input opcion-texto" placeholder="Opción 1" onchange="updateOpcion(this, 0)">
            </div>
            <div class="opcion-input">
                <input type="text" class="form-input opcion-texto" placeholder="Opción 2" onchange="updateOpcion(this, 1)">
            </div>
        </div>
        <button class="btn-add-opcion" onclick="addOpcion(this)">
            <i class="fas fa-plus"></i> Agregar Opción
        </button>
    `;

    container.appendChild(preguntaDiv);

    // Agregar a temporal
    preguntasTemp.push({
        texto: '',
        opciones: ['', '']
    });

    // Animación de entrada
    preguntaDiv.style.opacity = '0';
    preguntaDiv.style.transform = 'translateY(20px)';
    setTimeout(() => {
        preguntaDiv.style.opacity = '1';
        preguntaDiv.style.transform = 'translateY(0)';
    }, 50);
}

function removePregunta(btn) {
    const card = btn.closest('.pregunta-card');
    const index = Array.from(document.querySelectorAll('.pregunta-card')).indexOf(card);

    card.style.opacity = '0';
    card.style.transform = 'translateX(-100px)';

    setTimeout(() => {
        card.remove();
        preguntasTemp.splice(index, 1);
        updatePreguntaNumbers();
    }, 300);
}

function updatePreguntaNumbers() {
    const cards = document.querySelectorAll('.pregunta-card');
    cards.forEach((card, index) => {
        card.querySelector('.pregunta-number').textContent = `Pregunta ${index + 1}`;
    });
}

function updatePreguntaTexto(input) {
    const card = input.closest('.pregunta-card');
    const index = Array.from(document.querySelectorAll('.pregunta-card')).indexOf(card);
    preguntasTemp[index].texto = input.value;
}

function updateOpcion(input, opIndex) {
    const card = input.closest('.pregunta-card');
    const index = Array.from(document.querySelectorAll('.pregunta-card')).indexOf(card);
    preguntasTemp[index].opciones[opIndex] = input.value;
}

function addOpcion(btn) {
    const card = btn.closest('.pregunta-card');
    const index = Array.from(document.querySelectorAll('.pregunta-card')).indexOf(card);
    const opcionesContainer = card.querySelector('.opciones-container');
    const opIndex = preguntasTemp[index].opciones.length;

    const opcionDiv = document.createElement('div');
    opcionDiv.className = 'opcion-input';
    opcionDiv.innerHTML = `
        <input type="text" class="form-input opcion-texto" placeholder="Opción ${opIndex + 1}" onchange="updateOpcion(this, ${opIndex})">
    `;

    opcionesContainer.appendChild(opcionDiv);
    preguntasTemp[index].opciones.push('');

    // Animación
    opcionDiv.style.opacity = '0';
    setTimeout(() => {
        opcionDiv.style.opacity = '1';
    }, 50);
}

// ============================================
// REVISIÓN Y PUBLICACIÓN
// ============================================
function renderReview() {
    const container = document.getElementById('review-content');
    const titulo = document.getElementById('encuesta-titulo').value;
    const descripcion = document.getElementById('encuesta-descripcion').value;

    let html = `
        <div class="review-item">
            <div class="review-label">Título</div>
            <div class="review-value">${titulo}</div>
        </div>
        <div class="review-item">
            <div class="review-label">Descripción</div>
            <div class="review-value">${descripcion}</div>
        </div>
        <div class="review-item">
            <div class="review-label">Categoría</div>
            <div class="review-value"><span class="encuesta-category" style="background: var(--bg-${currentCategory}); padding: 0.25rem 0.75rem; border-radius: 4px; color: white; font-size: 0.875rem;">${currentCategory.charAt(0).toUpperCase() + currentCategory.slice(1)}</span></div>
        </div>
        <div class="review-item">
            <div class="review-label">Preguntas (${preguntasTemp.length})</div>
    `;

    preguntasTemp.forEach((p, i) => {
        html += `
            <div style="margin: 1rem 0; padding: 1rem; background: white; border-radius: 8px;">
                <strong>${i + 1}. ${p.texto}</strong>
                <ul style="margin-top: 0.5rem; padding-left: 1.5rem;">
                    ${p.opciones.map(o => `<li>${o}</li>`).join('')}
                </ul>
            </div>
        `;
    });

    html += '</div>';
    container.innerHTML = html;
}

function publicarEncuesta() {
    try {
        // Validar con servicio de integración
        const encuestaTemp = {
            titulo: document.getElementById('encuesta-titulo').value,
            descripcion: document.getElementById('encuesta-descripcion').value,
            categoria: currentCategory,
            preguntas: preguntasTemp.map(p => ({
                texto: p.texto,
                opciones: [...p.opciones]
            }))
        };

        if (!validacionService.validarEncuestaParaPublicar(encuestaTemp)) {
            const errores = validacionService.obtenerErrores();
            showToast('error', 'Error de validación', errores[0]);
            return;
        }

        // Crear encuesta
        const encuesta = encuestaService.crearEncuesta(
            encuestaTemp.titulo,
            encuestaTemp.descripcion,
            encuestaTemp.categoria
        );

        // Agregar preguntas
        preguntasTemp.forEach(p => {
            const pregunta = new Pregunta(p.texto);
            p.opciones.forEach(o => pregunta.agregarOpcion(o));
            encuesta.agregarPregunta(pregunta);
        });

        // Publicar
        encuestaService.publicarEncuesta(encuesta.id);

        showToast('success', '¡Encuesta publicada!', 'Tu encuesta ya está disponible para responder');

        // Reset y navegar
        setTimeout(() => {
            resetForm();
            navigateTo('responder');
            updateStats();
        }, 1500);

    } catch (error) {
        showToast('error', 'Error', error.message);
    }
}

// ============================================
// CARGAR ENCUESTAS PARA RESPONDER
// ============================================
function loadEncuestasPublicadas() {
    const container = document.getElementById('encuestas-disponibles');
    const encuestas = encuestaService.obtenerEncuestasPublicadas();

    if (encuestas.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <i class="fas fa-inbox"></i>
                <h3>No hay encuestas disponibles</h3>
                <p>Crea tu primera encuesta para que los usuarios puedan responder</p>
            </div>
        `;
        return;
    }

    container.innerHTML = encuestas.map(encuesta => {
        const totalRespuestas = respuestaService.obtenerRespuestasPorEncuesta(encuesta.id).length;
        return `
            <div class="encuesta-card">
                <div class="encuesta-header ${encuesta.categoria}">
                    <span class="encuesta-category">
                        <i class="fas fa-${getCategoryIcon(encuesta.categoria)}"></i>
                        ${encuesta.categoria.charAt(0).toUpperCase() + encuesta.categoria.slice(1)}
                    </span>
                    <h3 class="encuesta-title">${encuesta.titulo}</h3>
                </div>
                <div class="encuesta-body">
                    <p class="encuesta-descripcion">${encuesta.descripcion}</p>
                    <div class="encuesta-meta">
                        <span><i class="fas fa-question-circle"></i> ${encuesta.preguntas.length} preguntas</span>
                        <span><i class="fas fa-users"></i> ${totalRespuestas} respuestas</span>
                    </div>
                    <div class="encuesta-actions">
                        <button class="btn btn-primary btn-sm" onclick="openResponderModal('${encuesta.id}')">
                            <i class="fas fa-edit"></i> Responder
                        </button>
                        <button class="btn btn-secondary btn-sm" onclick="openResultadosModal('${encuesta.id}')">
                            <i class="fas fa-chart-bar"></i> Ver Resultados
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function getCategoryIcon(categoria) {
    const icons = {
        comida: 'utensils',
        productos: 'shopping-bag',
        belleza: 'spa',
        moda: 'tshirt',
        tecnologia: 'laptop',
        deportes: 'futbol',
        viajes: 'plane',
        general: 'globe'
    };
    return icons[categoria] || 'globe';
}

// ============================================
// MODAL RESPONDER ENCUESTA
// ============================================
function openResponderModal(encuestaId) {
    const encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
    if (!encuesta) return;

    document.getElementById('modal-titulo').textContent = encuesta.titulo;

    const body = document.getElementById('modal-body');
    body.innerHTML = `
        <p style="color: var(--color-gray); margin-bottom: 1.5rem;">${encuesta.descripcion}</p>
        <form id="respuestas-form">
            ${encuesta.preguntas.map((pregunta, pIndex) => `
                <div class="pregunta-respuesta">
                    <div class="pregunta-texto">${pIndex + 1}. ${pregunta.texto}</div>
                    ${pregunta.opciones.map((opcion, oIndex) => `
                        <div class="opcion-respuesta">
                            <input type="radio" name="pregunta_${pregunta.id}" id="opt_${pregunta.id}_${oIndex}" value="${oIndex}">
                            <label for="opt_${pregunta.id}_${oIndex}">${opcion}</label>
                        </div>
                    `).join('')}
                </div>
            `).join('')}
            <div class="form-actions">
                <button type="button" class="btn btn-outline" onclick="closeModal()">Cancelar</button>
                <button type="submit" class="btn btn-success">
                    <i class="fas fa-paper-plane"></i> Enviar Respuestas
                </button>
            </div>
        </form>
    `;

    document.getElementById('modal-responder').classList.add('active');

    // Evento submit
    document.getElementById('respuestas-form').addEventListener('submit', function(e) {
        e.preventDefault();
        submitRespuestas(encuestaId);
    });
}

function submitRespuestas(encuestaId) {
    const encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
    const respuestas = {};

    // Recopilar respuestas
    encuesta.preguntas.forEach(pregunta => {
        const selected = document.querySelector(`input[name="pregunta_${pregunta.id}"]:checked`);
        if (selected) {
            respuestas[pregunta.id] = parseInt(selected.value);
        }
    });

    // Validar con servicio de integración
    if (!validacionService.validarRespuestasCompletas(encuesta, respuestas)) {
        const errores = validacionService.obtenerErrores();
        showToast('error', 'Respuestas incompletas', errores[0]);
        return;
    }

    try {
        respuestaService.registrarRespuestas(encuestaId, respuestas);
        showToast('success', '¡Gracias por participar!', 'Tus respuestas han sido registradas de forma anónima');
        closeModal();
        updateStats();
        loadEncuestasPublicadas();
    } catch (error) {
        showToast('error', 'Error', error.message);
    }
}

function closeModal() {
    document.getElementById('modal-responder').classList.remove('active');
}

// ============================================
// MODAL RESULTADOS
// ============================================
function openResultadosModal(encuestaId) {
    const encuesta = encuestaService.obtenerEncuestaPorId(encuestaId);
    if (!encuesta) return;

    document.getElementById('resultados-titulo').textContent = `Resultados: ${encuesta.titulo}`;

    const estadisticas = respuestaService.calcularEstadisticas(encuestaId);
    const body = document.getElementById('resultados-body');

    body.innerHTML = estadisticas.map((stat, index) => `
        <div class="resultado-pregunta">
            <h4>${index + 1}. ${stat.pregunta}</h4>
            <p style="font-size: 0.875rem; color: var(--color-gray); margin-bottom: 1rem;">
                Total de respuestas: ${stat.totalRespuestas}
            </p>
            ${stat.opciones.map((op, oIndex) => `
                <div class="barra-resultado">
                    <div class="barra-label">
                        <span>${op.opcion}</span>
                        <span>${op.count} votos (${op.porcentaje}%)</span>
                    </div>
                    <div class="barra-track">
                        <div class="barra-fill color-${oIndex % 6}" style="width: ${op.porcentaje}%">
                            ${op.porcentaje > 10 ? op.porcentaje + '%' : ''}
                        </div>
                    </div>
                </div>
            `).join('')}
        </div>
    `).join('');

    document.getElementById('modal-resultados').classList.add('active');

    // Animar barras
    setTimeout(() => {
        document.querySelectorAll('.barra-fill').forEach(bar => {
            const width = bar.style.width;
            bar.style.width = '0';
            setTimeout(() => {
                bar.style.width = width;
            }, 100);
        });
    }, 100);
}

function closeResultadosModal() {
    document.getElementById('modal-resultados').classList.remove('active');
}

// ============================================
// RESULTADOS GENERALES
// ============================================
function loadResultados() {
    const container = document.getElementById('resultados-container');
    const encuestas = encuestaService.obtenerEncuestasPublicadas();

    if (encuestas.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-chart-line"></i>
                <h3>Sin resultados aún</h3>
                <p>Cuando los usuarios respondan las encuestas, verás las estadísticas aquí</p>
            </div>
        `;
        return;
    }

    container.innerHTML = '<div class="encuestas-grid">' + encuestas.map(encuesta => {
        const estadisticas = respuestaService.calcularEstadisticas(encuesta.id);
        const totalRespuestas = respuestaService.obtenerRespuestasPorEncuesta(encuesta.id).length;

        return `
            <div class="encuesta-card">
                <div class="encuesta-header ${encuesta.categoria}">
                    <span class="encuesta-category">
                        <i class="fas fa-${getCategoryIcon(encuesta.categoria)}"></i>
                        ${encuesta.categoria.charAt(0).toUpperCase() + encuesta.categoria.slice(1)}
                    </span>
                    <h3 class="encuesta-title">${encuesta.titulo}</h3>
                </div>
                <div class="encuesta-body">
                    <div class="encuesta-meta" style="margin-bottom: 1rem;">
                        <span><i class="fas fa-users"></i> ${totalRespuestas} respuestas</span>
                    </div>
                    ${estadisticas.slice(0, 2).map((stat, index) => `
                        <div class="resultado-pregunta" style="padding: 1rem; margin-bottom: 0.75rem;">
                            <h4 style="font-size: 0.95rem;">${index + 1}. ${stat.pregunta}</h4>
                            ${stat.opciones.slice(0, 3).map((op, oIndex) => `
                                <div class="barra-resultado" style="margin-bottom: 0.5rem;">
                                    <div class="barra-label" style="font-size: 0.8rem;">
                                        <span>${op.opcion}</span>
                                        <span>${op.porcentaje}%</span>
                                    </div>
                                    <div class="barra-track" style="height: 16px;">
                                        <div class="barra-fill color-${oIndex % 6}" style="width: ${op.porcentaje}%; min-width: 30px; font-size: 0.65rem;">
                                        </div>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    `).join('')}
                    <button class="btn btn-primary btn-sm" style="width: 100%; margin-top: 0.5rem;" onclick="openResultadosModal('${encuesta.id}')">
                        <i class="fas fa-eye"></i> Ver Todos los Resultados
                    </button>
                </div>
            </div>
        `;
    }).join('') + '</div>';
}

// ============================================
// ESTADÍSTICAS DEL DASHBOARD
// ============================================
function updateStats() {
    const totalEncuestas = encuestaService.obtenerTotalEncuestas();
    const totalPreguntas = encuestaService.obtenerTotalPreguntas();
    const totalRespuestas = respuestaService.obtenerTotalRespuestas();

    animateNumber('total-encuestas', totalEncuestas);
    animateNumber('total-preguntas', totalPreguntas);
    animateNumber('total-respuestas', totalRespuestas);
}

function animateNumber(elementId, target) {
    const element = document.getElementById(elementId);
    const start = parseInt(element.textContent) || 0;
    const duration = 1000;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const easeOut = 1 - Math.pow(1 - progress, 3);
        const current = Math.round(start + (target - start) * easeOut);
        element.textContent = current;

        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}

// ============================================
// TOAST NOTIFICATIONS
// ============================================
function showToast(type, title, message) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icons = {
        success: 'fa-check-circle',
        error: 'fa-exclamation-circle',
        warning: 'fa-exclamation-triangle',
        info: 'fa-info-circle'
    };

    toast.innerHTML = `
        <i class="fas ${icons[type]} toast-icon"></i>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <i class="fas fa-times"></i>
        </button>
    `;

    container.appendChild(toast);

    // Auto cerrar después de 5 segundos
    setTimeout(() => {
        toast.classList.add('hiding');
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}

// ============================================
// CERRAR MODALES CON ESC
// ============================================
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeModal();
        closeResultadosModal();
    }
});

// Cerrar modales al hacer clic fuera
document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', function() {
        closeModal();
        closeResultadosModal();
    });
});
