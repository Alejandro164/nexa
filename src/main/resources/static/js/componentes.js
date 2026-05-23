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
    initTabs();
    initToggleSwitch();
}

function initColorSwatches() {
    document.querySelectorAll('.color-swatch:not([data-initialized])').forEach(function(swatch) {
        swatch.setAttribute('data-initialized', 'true');
        swatch.style.cursor = 'pointer';
        swatch.addEventListener('click', function() {
            var hexEl = swatch.querySelector('.hex');
            if (hexEl) {
                copyToClipboard(hexEl.textContent, 'Color ' + hexEl.textContent + ' copiado al portapapeles');
            }
        });
    });
}

function initCopyCodeButtons() {
    document.querySelectorAll('.btn-copy-code:not([data-initialized])').forEach(function(btn) {
        btn.setAttribute('data-initialized', 'true');
        btn.addEventListener('click', function() {
            var targetId = btn.getAttribute('data-target');
            var codeElement = document.getElementById(targetId);
            if (codeElement) {
                var codeText = codeElement.textContent || codeElement.innerText;
                copyToClipboard(codeText, 'Código del componente copiado al portapapeles');
            }
        });
    });
}

function initToggleCodeButtons() {
    document.querySelectorAll('.btn-toggle-code:not([data-initialized])').forEach(function(btn) {
        btn.setAttribute('data-initialized', 'true');
        btn.addEventListener('click', function() {
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

    btnDecrease.addEventListener('click', function() { updateProgress(-10); });
    btnIncrease.addEventListener('click', function() { updateProgress(10); });
}

function initAlertInteractions() {
    document.querySelectorAll('.alert-demo:not([data-initialized])').forEach(function(alert) {
        alert.setAttribute('data-initialized', 'true');
        alert.style.cursor = 'pointer';
        alert.addEventListener('click', function() {
            var message = alert.textContent.trim();
            showNotification(message);
        });
    });
}

function initInteractiveModal() {
    var modalTrigger = document.getElementById('btn-trigger-modal');
    var interactiveModal = document.getElementById('interactive-modal');
    if (!modalTrigger || !interactiveModal) return;
    if (modalTrigger.hasAttribute('data-initialized')) return;

    modalTrigger.setAttribute('data-initialized', 'true');

    modalTrigger.addEventListener('click', function() {
        interactiveModal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    });

    var closeButtons = interactiveModal.querySelectorAll('.close-btn, .btn-secondary, .modal-backdrop');
    closeButtons.forEach(function(btn) {
        btn.addEventListener('click', function(e) {
            if (btn.classList.contains('modal-backdrop') && e.target !== btn) return;
            interactiveModal.style.display = 'none';
            document.body.style.overflow = '';
        });
    });

    var modalForm = interactiveModal.querySelector('form');
    if (modalForm) {
        modalForm.addEventListener('submit', function(e) {
            e.preventDefault();
            interactiveModal.style.display = 'none';
            document.body.style.overflow = '';
            showNotification('Usuario guardado correctamente (Simulación)');
        });
    }
}

function initTabs() {
    var tabButtons = document.querySelectorAll('.tab-demo-btn:not([data-initialized])');
    if (tabButtons.length === 0) return;

    tabButtons.forEach(function(btn) {
        btn.setAttribute('data-initialized', 'true');
        btn.addEventListener('click', function() {
            tabButtons.forEach(function(b) { b.classList.remove('active'); });
            btn.classList.add('active');
            var tabDemoBar = document.querySelector('.tab-demo-bar');
            if (tabDemoBar) {
                var tabContentText = tabDemoBar.nextElementSibling;
                if (tabContentText) {
                    tabContentText.textContent = 'Contenido de la pestaña: ' + btn.textContent.trim() + '. Vista cambiada dinámicamente.';
                }
            }
        });
    });
}

function initToggleSwitch() {
    var toggleInput = document.querySelector('.toggle-switch input');
    if (!toggleInput) return;
    if (toggleInput.hasAttribute('data-initialized')) return;

    toggleInput.setAttribute('data-initialized', 'true');
    toggleInput.addEventListener('change', function() {
        var state = toggleInput.checked ? 'activado' : 'desactivado';
        showNotification('Switch de estado ' + state);
    });
}

function copyToClipboard(text, successMessage) {
    navigator.clipboard.writeText(text).then(function() {
        showNotification(successMessage);
    }).catch(function() {
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

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initComponentes);
} else {
    initComponentes();
}

document.addEventListener('htmx:afterSettle', initComponentes);
