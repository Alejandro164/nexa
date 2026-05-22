/**
 * EstudiaFácil — Sistema de Diseño & Componentes UI
 * Script de interactividad para la visualización y pruebas de componentes.
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Copiar color hexadecimal al portapapeles al hacer click en un swatch
    const swatches = document.querySelectorAll('.color-swatch');
    swatches.forEach(swatch => {
        swatch.style.cursor = 'pointer';
        swatch.addEventListener('click', () => {
            const hexText = swatch.querySelector('.hex').textContent;
            copyToClipboard(hexText, `Color ${hexText} copiado al portapapeles`);
        });
    });

    // 2. Copiar fragmentos de código de componentes
    const copyButtons = document.querySelectorAll('.btn-copy-code');
    copyButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const targetId = btn.getAttribute('data-target');
            const codeElement = document.getElementById(targetId);
            if (codeElement) {
                // Si es un bloque de código oculto o visible, copiamos su contenido
                const codeText = codeElement.textContent || codeElement.innerText;
                copyToClipboard(codeText, 'Código del componente copiado al portapapeles');
            } else {
                // Si no hay bloque de código explícito, copiamos el HTML del elemento previo
                const previewSibling = btn.closest('.comp-demo-card')?.querySelector('.comp-row') 
                                    || btn.closest('.comp-demo-card')?.querySelector('.input-demo')
                                    || btn.closest('.comp-demo-card')?.querySelector('div:not(.comp-demo-card-header)');
                
                if (previewSibling) {
                    // Limpiamos un poco el HTML para presentarlo limpio
                    let cleanHTML = previewSibling.outerHTML
                        .replace(/\s*x-data="[^"]*"/g, '')
                        .replace(/\s*@click="[^"]*"/g, '')
                        .replace(/\s*x-show="[^"]*"/g, '')
                        .replace(/\s*x-transition[^=]*="[^"]*"/g, '')
                        .replace(/\s*autofocus=""/g, '')
                        .replace(/\s*style="[^"]*"/g, ''); // Remueve estilos inline si los hubiera
                    
                    copyToClipboard(cleanHTML, 'HTML del componente copiado al portapapeles');
                }
            }
        });
    });

    // 3. Mostrar/ocultar bloques de código (Acordeón de Código)
    const toggleCodeButtons = document.querySelectorAll('.btn-toggle-code');
    toggleCodeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');
            const codeContainer = document.getElementById(targetId + '-container');
            if (codeContainer) {
                if (codeContainer.style.display === 'none' || !codeContainer.style.display) {
                    codeContainer.style.display = 'block';
                    btn.classList.add('active');
                    btn.innerHTML = `
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px;"><polyline points="18 15 12 9 6 15"/></svg>
                        Ocultar Código
                    `;
                } else {
                    codeContainer.style.display = 'none';
                    btn.classList.remove('active');
                    btn.innerHTML = `
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px;"><polyline points="6 9 12 15 18 9"/></svg>
                        Ver Código
                    `;
                }
            }
        });
    });

    // 4. Barra de progreso interactiva (Demo)
    const progressFill = document.querySelector('.progress-bar-fill');
    const progressText = document.querySelector('.progress-bar-track + p');
    const btnDecreaseProgress = document.getElementById('btn-decrease-progress');
    const btnIncreaseProgress = document.getElementById('btn-increase-progress');

    if (progressFill && btnDecreaseProgress && btnIncreaseProgress) {
        let currentProgress = 68; // valor inicial

        const updateProgress = (value) => {
            currentProgress = Math.max(0, Math.min(100, currentProgress + value));
            progressFill.style.width = `${currentProgress}%`;
            if (progressText) {
                progressText.textContent = `${currentProgress}% completado`;
            }
        };

        btnDecreaseProgress.addEventListener('click', () => updateProgress(-10));
        btnIncreaseProgress.addEventListener('click', () => updateProgress(10));
    }

    // 5. Interacciones de Alertas en vivo
    const alertDemos = document.querySelectorAll('.alert-demo');
    alertDemos.forEach(alert => {
        alert.style.cursor = 'pointer';
        alert.addEventListener('click', () => {
            const message = alert.textContent.trim();
            showNotification(message);
        });
    });

    // 6. Modal interactiva completa (Triggers de apertura/cierre)
    const modalTrigger = document.getElementById('btn-trigger-modal');
    const interactiveModal = document.getElementById('interactive-modal');
    if (modalTrigger && interactiveModal) {
        modalTrigger.addEventListener('click', () => {
            interactiveModal.style.display = 'flex';
            document.body.style.overflow = 'hidden'; // Evita scroll de fondo
        });

        // Cerrar modal al hacer click en el botón close-btn o btn-secondary de cancelar
        const closeButtons = interactiveModal.querySelectorAll('.close-btn, .btn-secondary, .modal-backdrop');
        closeButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                // Si es el backdrop, solo cerrar si se hizo click exactamente en el backdrop
                if (btn.classList.contains('modal-backdrop') && e.target !== btn) {
                    return;
                }
                interactiveModal.style.display = 'none';
                document.body.style.overflow = '';
            });
        });

        // Guardar simulado
        const modalForm = interactiveModal.querySelector('form');
        if (modalForm) {
            modalForm.addEventListener('submit', (e) => {
                e.preventDefault();
                interactiveModal.style.display = 'none';
                document.body.style.overflow = '';
                showNotification('Usuario guardado correctamente (Simulación)');
            });
        }
    }

    // 7. Tabs Interactivos
    const tabButtons = document.querySelectorAll('.tab-demo-btn');
    const tabDemoBar = document.querySelector('.tab-demo-bar');
    if (tabButtons.length > 0 && tabDemoBar) {
        const tabContentText = tabDemoBar.nextElementSibling;
        tabButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                tabButtons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                
                const tabName = btn.textContent.trim();
                if (tabContentText) {
                    tabContentText.textContent = `Contenido de la pestaña: ${tabName}. Vista cambiada dinámicamente.`;
                }
            });
        });
    }

    // 8. Toggle Switch interactivo
    const toggleInput = document.querySelector('.toggle-switch input');
    if (toggleInput) {
        toggleInput.addEventListener('change', () => {
            const state = toggleInput.checked ? 'activado' : 'desactivado';
            showNotification(`Switch de estado ${state}`);
        });
    }

    /**
     * Función auxiliar para copiar texto al portapapeles
     */
    function copyToClipboard(text, successMessage) {
        navigator.clipboard.writeText(text).then(() => {
            showNotification(successMessage);
        }).catch(err => {
            console.error('Error al copiar texto: ', err);
            // Fallback usando textarea antigua
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'fixed'; // Evita scroll
            document.body.appendChild(textarea);
            textarea.select();
            try {
                document.execCommand('copy');
                showNotification(successMessage);
            } catch (err2) {
                console.error('Fallback fallido: ', err2);
            }
            document.body.removeChild(textarea);
        });
    }

    /**
     * Función auxiliar para lanzar notificación usando el Alpine setup si está disponible
     */
    function showNotification(message) {
        // En EstudiaFácil, Alpine.js escucha en window por "notify"
        const event = new CustomEvent('notify', { detail: message });
        window.dispatchEvent(event);
    }
});
