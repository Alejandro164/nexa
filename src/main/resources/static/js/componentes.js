/**
 * EstudiaFácil — Sistema de Diseño & Componentes UI
 * Script de interactividad para la visualización y pruebas de componentes.
 * Compatible con navegación HTMX: se inicializa en DOMContentLoaded y htmx:afterSettle.
 */

function initComponentes() {
    if (!document.querySelector('.comp-section')) return;

    initColorSwatches();
    initCopyCodeButtons();
    initToggleCodeButtons();
    initProgressBar();
    initAlertInteractions();
    initInteractiveModal();
    initDestructiveModal();
    initTabs();
    initToggleSwitch();
    initStepIndicator();
    initTableFilter();
    initScrollSpy();
    initToastDemoSpawner();
}

// ── Paleta de colores ──
function initColorSwatches() {
    document.querySelectorAll('.color-swatch:not([data-initialized])').forEach(function (swatch) {
        swatch.setAttribute('data-initialized', 'true');
        swatch.style.cursor = 'pointer';
        swatch.addEventListener('click', function () {
            var hexEl = swatch.querySelector('.hex');
            if (hexEl) {
                copyToClipboard(hexEl.textContent, 'Color ' + hexEl.textContent + ' copiado al portapapeles');
            }
        });
    });
}

// ── Copiado de código ──
function initCopyCodeButtons() {
    document.querySelectorAll('.btn-copy-code:not([data-initialized])').forEach(function (btn) {
        btn.setAttribute('data-initialized', 'true');
        btn.addEventListener('click', function () {
            var targetId = btn.getAttribute('data-target');
            var codeElement = document.getElementById(targetId);
            if (codeElement) {
                var codeText = codeElement.textContent || codeElement.innerText;
                copyToClipboard(codeText, 'Código del componente copiado');
            }
        });
    });
}

// ── Mostrar/ocultar código ──
function initToggleCodeButtons() {
    document.querySelectorAll('.btn-toggle-code:not([data-initialized])').forEach(function (btn) {
        btn.setAttribute('data-initialized', 'true');
        btn.addEventListener('click', function () {
            var targetId = btn.getAttribute('data-target');
            var codeContainer = document.getElementById(targetId + '-container');
            if (codeContainer) {
                if (codeContainer.style.display === 'none' || !codeContainer.style.display) {
                    codeContainer.style.display = 'block';
                    btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px;"><polyline points="18 15 12 9 6 15"/></svg> Ocultar Código';
                } else {
                    codeContainer.style.display = 'none';
                    btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px;"><polyline points="6 9 12 15 18 9"/></svg> Ver Código';
                }
            }
        });
    });
}

// ── Barra de progreso demo ──
function initProgressBar() {
    var progressFill = document.querySelector('.progress-bar-fill');
    var progressText = document.querySelector('.progress-bar-track + p');
    var btnDecrease = document.getElementById('btn-decrease-progress');
    var btnIncrease = document.getElementById('btn-increase-progress');

    if (!progressFill || !btnDecrease || !btnIncrease) return;
    if (btnDecrease.hasAttribute('data-initialized')) return;

    btnDecrease.setAttribute('data-initialized', 'true');
    btnIncrease.setAttribute('data-initialized', 'true');

    var currentProgress = 68;
    function updateProgress(value) {
        currentProgress = Math.max(0, Math.min(100, currentProgress + value));
        progressFill.style.width = currentProgress + '%';
        if (progressText) {
            progressText.textContent = currentProgress + '% completado';
        }
    }

    btnDecrease.addEventListener('click', function () { updateProgress(-10); });
    btnIncrease.addEventListener('click', function () { updateProgress(10); });
}

// ── Interacciones de alerta ──
function initAlertInteractions() {
    document.querySelectorAll('.alert-demo:not([data-initialized])').forEach(function (alert) {
        alert.setAttribute('data-initialized', 'true');
        alert.style.cursor = 'pointer';
        alert.addEventListener('click', function () {
            var message = alert.textContent.trim();
            showNotification(message);
        });
    });
}

// ── Modal interactivo estándar ──
function initInteractiveModal() {
    var modalTrigger = document.getElementById('btn-trigger-modal');
    var interactiveModal = document.getElementById('interactive-modal');
    if (!modalTrigger || !interactiveModal) return;
    if (modalTrigger.hasAttribute('data-initialized')) return;

    modalTrigger.setAttribute('data-initialized', 'true');

    modalTrigger.addEventListener('click', function () {
        interactiveModal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    });

    var closeButtons = interactiveModal.querySelectorAll('.close-btn, .btn-secondary, .modal-backdrop');
    closeButtons.forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (btn.classList.contains('modal-backdrop') && e.target !== btn) return;
            interactiveModal.style.display = 'none';
            document.body.style.overflow = '';
        });
    });

    var modalForm = interactiveModal.querySelector('form');
    if (modalForm) {
        modalForm.addEventListener('submit', function (e) {
            e.preventDefault();
            interactiveModal.style.display = 'none';
            document.body.style.overflow = '';
            showNotification('Usuario guardado correctamente (Simulación)');
        });
    }
}

// ── Modal de confirmación destructiva ──
function initDestructiveModal() {
    var trigger = document.getElementById('btn-trigger-confirm');
    var modal = document.getElementById('destructive-modal');
    if (!trigger || !modal) return;
    if (trigger.hasAttribute('data-initialized')) return;

    trigger.setAttribute('data-initialized', 'true');

    trigger.addEventListener('click', function () {
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    });

    var closeButtons = modal.querySelectorAll('.close-btn, .btn-secondary, .modal-backdrop, .btn-destructive');
    closeButtons.forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (btn.classList.contains('modal-backdrop') && e.target !== btn) return;
            modal.style.display = 'none';
            document.body.style.overflow = '';
            if (btn.classList.contains('btn-destructive')) {
                showNotification('Elemento eliminado permanentemente');
            }
        });
    });
}

// ── Pestañas (Tabs) interactivos ──
function initTabs() {
    var tabDemoContainers = document.querySelectorAll('.tab-demo-container:not([data-initialized])');
    tabDemoContainers.forEach(function (container) {
        container.setAttribute('data-initialized', 'true');
        var tabButtons = container.querySelectorAll('.tab-demo-btn');
        var tabContents = container.querySelectorAll('.tab-demo-content');

        tabButtons.forEach(function (btn, index) {
            btn.addEventListener('click', function () {
                tabButtons.forEach(function (b) { b.classList.remove('active'); });
                tabContents.forEach(function (c) { c.style.display = 'none'; });

                btn.classList.add('active');
                if (tabContents[index]) {
                    tabContents[index].style.display = 'block';
                }
            });
        });
    });
}

// ── Toggle Switch interaction ──
function initToggleSwitch() {
    var toggleInput = document.querySelector('.toggle-switch input');
    if (!toggleInput) return;
    if (toggleInput.hasAttribute('data-initialized')) return;

    toggleInput.setAttribute('data-initialized', 'true');
    toggleInput.addEventListener('change', function () {
        var state = toggleInput.checked ? 'activado' : 'desactivado';
        showNotification('Switch de estado ' + state);
    });
}

// ── Wizard (Indicador de Pasos) ──
function initStepIndicator() {
    var steps = document.querySelectorAll('.step-node');
    var progress = document.querySelector('.step-indicator-progress');
    if (steps.length === 0 || !progress) return;
    if (steps[0].hasAttribute('data-initialized')) return;

    steps.forEach(function (step, idx) {
        step.setAttribute('data-initialized', 'true');
        step.addEventListener('click', function () {
            updateSteps(idx);
        });
    });

    function updateSteps(activeIndex) {
        steps.forEach(function (step, idx) {
            if (idx < activeIndex) {
                step.classList.add('completed');
                step.classList.remove('active');
            } else if (idx === activeIndex) {
                step.classList.remove('completed');
                step.classList.add('active');
            } else {
                step.classList.remove('completed');
                step.classList.remove('active');
            }
        });

        var pct = (activeIndex / (steps.length - 1)) * 100;
        progress.style.width = pct + '%';
    }
}

// ── Filtro en Tiempo Real para Tablas ──
function initTableFilter() {
    var filterInput = document.getElementById('table-demo-search');
    var tableRows = document.querySelectorAll('.comp-table tbody tr');
    if (!filterInput || tableRows.length === 0) return;
    if (filterInput.hasAttribute('data-initialized')) return;

    filterInput.setAttribute('data-initialized', 'true');
    filterInput.addEventListener('input', function () {
        var query = filterInput.value.toLowerCase().trim();
        tableRows.forEach(function (row) {
            var text = row.innerText.toLowerCase();
            if (text.indexOf(query) > -1) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    });
}

// ── Scroll Spy para Menú de Navegación Lateral ──
function initScrollSpy() {
    var links = document.querySelectorAll('.comp-nav-link');
    var sections = document.querySelectorAll('.comp-section');
    if (links.length === 0 || sections.length === 0) return;

    window.addEventListener('scroll', function () {
        var scrollPosition = window.scrollY + 120;
        sections.forEach(function (section) {
            var top = section.offsetTop;
            var height = section.offsetHeight;
            var id = section.getAttribute('id');

            if (scrollPosition >= top && scrollPosition < top + height) {
                links.forEach(function (link) {
                    link.classList.remove('active');
                    if (link.getAttribute('href') === '#' + id) {
                        link.classList.add('active');
                    }
                });
            }
        });
    });
}

// ── Demostración de Toasts Semánticos ──
function initToastDemoSpawner() {
    var triggers = document.querySelectorAll('.btn-toast-trigger:not([data-initialized])');
    var container = document.getElementById('toast-preview-container');
    if (triggers.length === 0 || !container) return;

    triggers.forEach(function (btn) {
        btn.setAttribute('data-initialized', 'true');
        btn.addEventListener('click', function () {
            var type = btn.getAttribute('data-type');
            var msg = btn.getAttribute('data-msg');
            spawnToast(msg, type);
        });
    });

    function spawnToast(message, type) {
        var toast = document.createElement('div');
        toast.className = 'toast-notification ' + type;

        var iconSvg = '';
        if (type === 'success') {
            iconSvg = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>';
        } else if (type === 'error') {
            iconSvg = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--error)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>';
        } else if (type === 'warning') {
            iconSvg = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--warm)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
        } else {
            iconSvg = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
        }

        toast.innerHTML = iconSvg + '<span>' + message + '</span><button class="toast-close">&times;</button>';
        container.appendChild(toast);

        toast.querySelector('.toast-close').addEventListener('click', function () {
            toast.remove();
        });

        setTimeout(function () {
            toast.style.animation = 'slideDown 0.25s ease-out reverse';
            setTimeout(function () {
                toast.remove();
            }, 230);
        }, 4000);
    }
}

// ── Utilitarias ──
function copyToClipboard(text, successMessage) {
    navigator.clipboard.writeText(text).then(function () {
        showNotification(successMessage);
    }).catch(function () {
        var textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            showNotification(successMessage);
        } catch (err) {
            // silent
        }
        document.body.removeChild(textarea);
    });
}

function showNotification(message) {
    var event = new CustomEvent('notify', { detail: message });
    window.dispatchEvent(event);
}

// ── Registro de inicializadores ──
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initComponentes);
} else {
    initComponentes();
}

document.addEventListener('htmx:afterSettle', initComponentes);

// ── Funciones globales de Proyectos (llamadas via onclick en contenido HTMX) ─

function agregarMiembro(id) {
    var sel = document.getElementById('select-usuario-' + id);
    if (!sel || !sel.value) return;
    htmx.ajax('POST', '/agenda/proyectos/' + id + '/miembros', {
        target: '#miembros-container', swap: 'innerHTML',
        values: { usuarioId: sel.value, rol: 'MIEMBRO' }
    }).then(function () {
        htmx.ajax('GET', '/agenda/proyectos/' + id + '/dashboard', { target: '#dashboard-container', swap: 'innerHTML' });
        htmx.ajax('GET', '/agenda/proyectos/buscar?filtro=', { target: '#tabla-proyectos-container', swap: 'innerHTML' });
    });
}

function eliminarMiembro(proyectoId, miembroId) {
    if (!confirm('¿Quitar este miembro?')) return;
    htmx.ajax('DELETE', '/agenda/proyectos/' + proyectoId + '/miembros/' + miembroId, {
        target: '#miembros-container', swap: 'innerHTML'
    }).then(function () {
        htmx.ajax('GET', '/agenda/proyectos/' + proyectoId + '/dashboard', { target: '#dashboard-container', swap: 'innerHTML' });
        htmx.ajax('GET', '/agenda/proyectos/buscar?filtro=', { target: '#tabla-proyectos-container', swap: 'innerHTML' });
    });
}

function abrirFormTarea(miembroId, proyectoId, tareaId) {
    var url = tareaId
        ? '/agenda/proyectos/miembros/' + miembroId + '/tareas/form/' + tareaId + '?proyectoId=' + proyectoId
        : '/agenda/proyectos/miembros/' + miembroId + '/tareas/form?proyectoId=' + proyectoId;
    htmx.ajax('GET', url, { target: '#tarea-form-container-' + miembroId, swap: 'innerHTML' });
}

function cambiarEstadoTarea(tareaId, proyectoId, miembroId, nuevoEstado) {
    htmx.ajax('PUT', '/agenda/proyectos/tareas/' + tareaId + '/estado?' + new URLSearchParams({ proyectoId: proyectoId, estado: nuevoEstado }), { target: '#none' })
        .then(function () { cargarTareas(miembroId, proyectoId); });
}

function eliminarTarea(tareaId, proyectoId, miembroId) {
    if (!confirm('¿Eliminar esta tarea?')) return;
    htmx.ajax('DELETE', '/agenda/proyectos/tareas/' + tareaId + '?' + new URLSearchParams({ proyectoId: proyectoId, miembroId: miembroId }), { target: '#tareas-' + miembroId, swap: 'innerHTML' });
}

function cargarTareas(miembroId, proyectoId) {
    var c = document.getElementById('tareas-' + miembroId);
    if (c) { if (c.style.display !== 'none') { c.style.display = 'none'; return; } c.style.display = 'block'; }
    htmx.ajax('GET', '/agenda/proyectos/miembros/' + miembroId + '/tareas?' + new URLSearchParams({ proyectoId: proyectoId }), { target: '#tareas-' + miembroId, swap: 'innerHTML' });
}

// ── Componente Alpine: Proyectos ─────────────────────────────────────────────

function proyectoComponente() {
    return {
        filtro: '', dashboardProyectoId: '', activeTab: 'proyectos',

        buscarProyectos() {
            htmx.ajax('GET', '/agenda/proyectos/buscar?' + new URLSearchParams({ filtro: this.filtro }), { target: '#tabla-proyectos-container', swap: 'innerHTML' });
        },
        verMiembros(id) {
            this.dashboardProyectoId = String(id);
            document.getElementById('btn-tab-eficiencia')?.click();
            this.$nextTick(() => {
                var wrap = this.$el.querySelector('.academic-select');
                if (wrap && wrap._academicSync) wrap._academicSync();
            });
            htmx.ajax('GET', '/agenda/proyectos/' + id + '/dashboard', { target: '#dashboard-container', swap: 'innerHTML' });
            htmx.ajax('GET', '/agenda/proyectos/' + id + '/miembros', { target: '#miembros-container', swap: 'innerHTML' });
        }
    };
}

function toggleTipoAsignacion(tipo) {
    var wrapP = document.getElementById('wrap-asig-proyecto');
    var wrapU = document.getElementById('wrap-asig-personal');
    var selP  = document.getElementById('asig-proyecto');
    var selU  = document.getElementById('asig-personal');
    if (!wrapP || !wrapU) return;

    function resetAcademicSelect(sel) {
        if (!sel) return;
        sel.value = '';
        var wrap = sel.closest('.academic-select');
        if (wrap) {
            wrap.classList.remove('open');
            if (wrap._academicSync) wrap._academicSync();
            if (wrap._academicPanel) wrap._academicPanel.classList.remove('is-open');
        }
    }

    if (tipo === 'p') {
        wrapP.style.display = '';
        if (selP) selP.disabled = false;
        wrapU.style.display = 'none';
        if (selU) { selU.disabled = true; resetAcademicSelect(selU); }
    } else {
        wrapU.style.display = '';
        if (selU) selU.disabled = false;
        wrapP.style.display = 'none';
        if (selP) { selP.disabled = true; resetAcademicSelect(selP); }
    }
}

function abrirTareasModal() {
    var el = document.getElementById('tareas-modal-container');
    if (!el) return;
    el.style.display = 'block';
    setTimeout(function () {
        el.classList.remove('dir-modal-closing');
        el.classList.add('dir-modal-visible');
    }, 0);
}

function cerrarTareasModal() {
    var el = document.getElementById('tareas-modal-container');
    if (!el) return;
    el.classList.remove('dir-modal-visible');
    el.classList.add('dir-modal-closing');
    setTimeout(function () {
        el.style.display = 'none';
        el.classList.remove('dir-modal-closing');
        el.innerHTML = '';
    }, 180);
}

function abrirRecordatoriosModal() {
    var el = document.getElementById('recordatorios-modal-container');
    if (!el) return;
    el.style.display = 'block';
    setTimeout(function () {
        el.classList.remove('dir-modal-closing');
        el.classList.add('dir-modal-visible');
    }, 0);
}

function cerrarRecordatoriosModal() {
    var el = document.getElementById('recordatorios-modal-container');
    if (!el) return;
    el.classList.remove('dir-modal-visible');
    el.classList.add('dir-modal-closing');
    setTimeout(function () {
        el.style.display = 'none';
        el.classList.remove('dir-modal-closing');
        el.innerHTML = '';
    }, 180);
}

function abrirActividadModal() {
    var el = document.getElementById('actividad-modal-container');
    if (!el) return;
    el.style.display = 'block';
    setTimeout(function () {
        el.classList.remove('dir-modal-closing');
        el.classList.add('dir-modal-visible');
    }, 0);
}

function cerrarActividadModal() {
    var el = document.getElementById('actividad-modal-container');
    if (!el) return;
    el.classList.remove('dir-modal-visible');
    el.classList.add('dir-modal-closing');
    setTimeout(function () {
        el.style.display = 'none';
        el.classList.remove('dir-modal-closing');
        el.innerHTML = '';
    }, 180);
}

function animarEvento(el, visible) {
    if (visible) {
        el.style.display = '';
        requestAnimationFrame(function () {
            el.classList.remove('oculto');
        });
        return;
    }
    el.classList.add('oculto');
    el.addEventListener('transitionend', function ocultarAlTerminar(e) {
        if (e.target !== el) return;
        el.removeEventListener('transitionend', ocultarAlTerminar);
        if (el.classList.contains('oculto')) {
            el.style.display = 'none';
        }
    });
}

function toggleCalendarioTipo(tipo, visible) {
    document.querySelectorAll('.cal-event.event-' + tipo).forEach(function (el) {
        animarEvento(el, visible);
    });
    if (tipo === 'general') {
        document.querySelectorAll('.cal-bandas-wrapper').forEach(function (wrapper) {
            if (visible) {
                var expandido = wrapper.classList.contains('expanded');
                wrapper.style.maxHeight = (expandido ? wrapper.dataset.expandedHeight : wrapper.dataset.collapsedHeight) + 'px';
            } else {
                wrapper.style.maxHeight = '0px';
            }
        });
        document.querySelectorAll('.cal-banda-mas').forEach(function (btn) {
            btn.classList.toggle('oculto', !visible);
        });
    }
}

function toggleBandasSemana(btn) {
    var wrapper = btn.previousElementSibling;
    if (!wrapper || !wrapper.classList.contains('cal-bandas-wrapper')) return;
    var expandido = wrapper.classList.toggle('expanded');
    wrapper.style.maxHeight = (expandido ? wrapper.dataset.expandedHeight : wrapper.dataset.collapsedHeight) + 'px';
    btn.classList.toggle('expanded', expandido);
    var label = btn.querySelector('span');
    if (label) label.textContent = expandido ? 'Ver menos' : btn.dataset.masLabel;
}

function mostrarDetalleEvento(el, evt) {
    evt.stopPropagation();
    var pop = document.getElementById('evento-popover');
    if (!pop) return;

    pop.querySelector('.evento-popover-icono').textContent = el.dataset.icono || '';
    pop.querySelector('.evento-popover-tipo').textContent = el.dataset.tipoLabel || '';
    pop.querySelector('.evento-popover-tipo').className = 'evento-popover-tipo ' + (el.dataset.tipoClase || '');
    pop.querySelector('.evento-popover-titulo').textContent = el.dataset.titulo || '';
    pop.querySelector('.evento-popover-fecha').textContent = el.dataset.fecha || '';

    var desc = pop.querySelector('.evento-popover-desc');
    if (el.dataset.descripcion) {
        desc.textContent = el.dataset.descripcion;
        desc.style.display = '';
    } else {
        desc.style.display = 'none';
    }

    var link = pop.querySelector('.evento-popover-link');
    if (el.dataset.enlace) {
        link.href = el.dataset.enlace;
        link.style.display = '';
    } else {
        link.style.display = 'none';
    }

    pop.style.display = 'block';
    pop.style.visibility = 'hidden';
    pop.classList.remove('visible');

    var rect = el.getBoundingClientRect();
    var margin = 12;
    var popWidth = pop.offsetWidth;
    var popHeight = pop.offsetHeight;

    var left = rect.right + margin;
    var origenIzquierda = true;
    if (left + popWidth > window.innerWidth - 8) {
        left = rect.left - popWidth - margin;
        origenIzquierda = false;
    }
    if (left < 8) left = Math.max(8, Math.min(rect.left, window.innerWidth - popWidth - 8));

    var top = rect.top;
    if (top + popHeight > window.innerHeight - 8) top = window.innerHeight - popHeight - 8;
    if (top < 8) top = 8;

    pop.style.left = left + 'px';
    pop.style.top = top + 'px';
    pop.style.transformOrigin = origenIzquierda ? 'left top' : 'right top';
    pop.style.visibility = 'visible';

    requestAnimationFrame(function () { pop.classList.add('visible'); });
}

function cerrarEventoPopover() {
    var pop = document.getElementById('evento-popover');
    if (!pop || pop.style.display === 'none') return;
    pop.classList.remove('visible');
    setTimeout(function () { pop.style.display = 'none'; }, 150);
}

document.addEventListener('click', function (e) {
    var pop = document.getElementById('evento-popover');
    if (pop && pop.style.display !== 'none' && !pop.contains(e.target)) {
        cerrarEventoPopover();
    }
});

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') cerrarEventoPopover();
});

function _desmontarAcademicSelect(sel) {
    if (!sel || !sel.classList.contains('academic-native-select')) return;
    var wrap = sel.parentElement;
    if (!wrap || !wrap.classList.contains('academic-select')) return;
    if (wrap._academicPanel) wrap._academicPanel.remove();
    wrap.parentNode.insertBefore(sel, wrap);
    wrap.remove();
    sel.classList.remove('academic-native-select');
    delete sel.dataset.academicSelect;
}

function cargarMiembrosFormTarea(proyectoId) {
    var sel = document.getElementById('form-tarea-miembro');
    if (!sel) return;

    _desmontarAcademicSelect(sel);

    if (!proyectoId) {
        sel.innerHTML = '<option value="">-- Seleccionar miembro --</option>';
        inicializarSelectoresAcademicos(sel.parentNode);
        return;
    }

    htmx.ajax('GET', '/agenda/tareas/miembros', {
        target: '#form-tarea-miembro', swap: 'innerHTML'
    }).then(function () {
        inicializarSelectoresAcademicos(sel.parentNode);
    });
}

// ── Posicionamiento inteligente de dropdowns ─────────────────────────────────

function posicionarDropdown(trigger, panel, gap) {
    gap = gap || 6;
    var rect       = trigger.getBoundingClientRect();
    var maxH       = parseInt(window.getComputedStyle(panel).maxHeight) || 240;
    var spaceBelow = window.innerHeight - rect.bottom - gap;
    var spaceAbove = rect.top - gap;

    panel.style.width = rect.width + 'px';
    panel.style.left  = rect.left + 'px';
    panel.style.right = 'auto';

    if (spaceBelow >= maxH) {
        panel.style.top       = (rect.bottom + gap) + 'px';
        panel.style.bottom    = 'auto';
        panel.style.maxHeight = maxH + 'px';
    } else if (spaceAbove >= spaceBelow) {
        panel.style.top       = 'auto';
        panel.style.bottom    = (window.innerHeight - rect.top + gap) + 'px';
        panel.style.maxHeight = Math.min(maxH, Math.max(spaceAbove, 80)) + 'px';
    } else {
        panel.style.top       = (rect.bottom + gap) + 'px';
        panel.style.bottom    = 'auto';
        panel.style.maxHeight = Math.min(maxH, Math.max(spaceBelow, 80)) + 'px';
    }
}

// ── Modal global (#modal-container) ──────────────────────────────────────────

function abrirModalAcademico() {
    var modal = document.getElementById('modal-container');
    if (!modal) return;
    modal.classList.remove('modal-closing');
    modal.classList.add('show');
}

function cerrarModalAcademico() {
    var modal = document.getElementById('modal-container');
    if (!modal || !modal.classList.contains('show')) return;
    modal.classList.add('modal-closing');
    setTimeout(function () {
        modal.classList.remove('show', 'modal-closing');
        modal.innerHTML = '';
    }, 180);
}

// Close modal on backdrop click or Escape
if (!window._modalGlobalEventsRegistered) {
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') cerrarModalAcademico();
    });
    document.addEventListener('click', function (e) {
        var modal = document.getElementById('modal-container');
        if (modal && modal.classList.contains('show') && e.target === modal) {
            cerrarModalAcademico();
        }
    });
    window._modalGlobalEventsRegistered = true;
}

// ── Delete confirmation modal ─────────────────────────────────────────────────

function abrirConfirmacionAcademica(trigger) {
    window.academicDeleteRequest = {
        url: trigger.dataset.deleteUrl,
        target: trigger.dataset.deleteTarget,
        swap: trigger.dataset.deleteSwap || 'innerHTML'
    };
    document.getElementById('academic-delete-title').textContent =
        trigger.dataset.deleteTitle || 'Confirmar eliminación';
    document.getElementById('academic-delete-message').textContent =
        trigger.dataset.deleteMessage || '¿Deseas eliminar este registro?';
    var modal = document.getElementById('academic-delete-modal');
    modal.style.display = 'flex';
    void modal.offsetWidth;
    modal.classList.add('modal-visible');
}

function cerrarConfirmacionAcademica() {
    var modal = document.getElementById('academic-delete-modal');
    if (modal && modal.style.display !== 'none') {
        modal.classList.remove('modal-visible');
        modal.classList.add('modal-closing');
        setTimeout(function () {
            modal.style.display = 'none';
            modal.classList.remove('modal-closing');
        }, 180);
    }
    window.academicDeleteRequest = null;
}

function confirmarEliminacionAcademica() {
    var req = window.academicDeleteRequest;
    if (!req || !req.url || !req.target) { cerrarConfirmacionAcademica(); return; }
    htmx.ajax('DELETE', req.url, { target: req.target, swap: req.swap })
        .then(function () { cerrarConfirmacionAcademica(); });
}

// ── Custom Select (academic-select) ──────────────────────────────────────────

function cerrarSelectoresAcademicos(excepto) {
    document.querySelectorAll('.academic-select.open').forEach(function (s) {
        if (s !== excepto) {
            s.classList.remove('open');
            var t = s.querySelector('.academic-select-trigger');
            if (t) t.setAttribute('aria-expanded', 'false');
            var panel = s._academicPanel;
            if (panel) panel.classList.remove('is-open');
        }
    });
}

function inicializarSelectoresAcademicos(root) {
    (root || document).querySelectorAll('select.form-input:not([data-academic-select])').forEach(function (native) {
        native.dataset.academicSelect = 'true';
        native.classList.add('academic-native-select');

        var wrap = document.createElement('div');
        wrap.className = 'academic-select';

        var trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'academic-select-trigger';
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');

        var label = document.createElement('span');
        var arrow = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        arrow.setAttribute('class', 'academic-select-arrow');
        arrow.setAttribute('viewBox', '0 0 24 24');
        arrow.setAttribute('fill', 'none');
        arrow.setAttribute('stroke', 'currentColor');
        arrow.setAttribute('stroke-width', '2');
        arrow.innerHTML = '<polyline points="6 9 12 15 18 9"></polyline>';
        trigger.append(label, arrow);

        var opts = document.createElement('div');
        opts.className = 'academic-select-options';
        opts.setAttribute('role', 'listbox');

        function sync() {
            var sel = native.options[native.selectedIndex];
            label.textContent = sel ? sel.textContent.trim() : '';
            label.classList.toggle('academic-select-placeholder', !sel || !sel.value);
            opts.querySelectorAll('.academic-select-option').forEach(function (o) {
                o.classList.toggle('selected', o.dataset.value === native.value);
            });
        }

        Array.from(native.options).forEach(function (o) {
            if (o.hidden || o.disabled) return;
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'academic-select-option';
            btn.dataset.value = o.value;
            btn.setAttribute('role', 'option');
            if (o.dataset.role) {
                var nameSpan = document.createElement('span');
                nameSpan.className = 'academic-select-option-label';
                nameSpan.textContent = o.textContent.trim();
                var badge = document.createElement('span');
                badge.className = 'badge badge-primary';
                badge.textContent = o.dataset.role;
                btn.append(nameSpan, badge);
            } else {
                btn.textContent = o.textContent.trim();
            }
            btn.addEventListener('click', function () {
                native.value = btn.dataset.value;
                sync();
                wrap.classList.remove('open');
                trigger.setAttribute('aria-expanded', 'false');
                opts.classList.remove('is-open');
                native.dispatchEvent(new Event('change', { bubbles: true }));
            });
            opts.appendChild(btn);
        });

        trigger.addEventListener('click', function () {
            var open = !wrap.classList.contains('open');
            cerrarSelectoresAcademicos(wrap);
            if (open) {
                posicionarDropdown(trigger, opts, 6);
                opts.classList.add('is-open');
            } else {
                opts.classList.remove('is-open');
            }
            wrap.classList.toggle('open', open);
            trigger.setAttribute('aria-expanded', String(open));
        });

        native.addEventListener('change', sync);
        native.parentNode.insertBefore(wrap, native);
        wrap.append(native, trigger);
        // Teleport opts to body (escapes overflow/modal constraints)
        opts.style.position = 'fixed';
        opts.style.right = 'auto';
        document.body.appendChild(opts);
        wrap._academicPanel = opts;
        wrap._academicSync  = sync;
        sync();
    });
}

if (!window.academicSelectEventsRegistered) {
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.academic-select') && !e.target.closest('.academic-select-options')) {
            cerrarSelectoresAcademicos();
        }
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') cerrarSelectoresAcademicos();
    });
    window.addEventListener('scroll', function (e) {
        // No cerrar si el scroll ocurre dentro del panel de opciones
        if (e.target && typeof e.target.closest === 'function' &&
            e.target.closest('.academic-select-options')) return;
        cerrarSelectoresAcademicos();
    }, true);
    document.addEventListener('htmx:load', function (e) {
        inicializarSelectoresAcademicos(e.detail.elt);
    });
    window.academicSelectEventsRegistered = true;
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { inicializarSelectoresAcademicos(document); });
} else {
    inicializarSelectoresAcademicos(document);
}
