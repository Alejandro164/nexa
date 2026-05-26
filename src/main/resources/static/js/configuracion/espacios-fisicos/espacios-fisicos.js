/**
 * EstudiaFácil — Espacios Físicos Settings Script
 */
(function() {
    console.log("Configuración: Espacios Físicos JS cargado correctamente.");

    const btnNuevo = document.getElementById("btn-nuevo-espacio");
    const viewButtons = document.querySelectorAll(".btn-ver-espacio");

    if (btnNuevo) {
        btnNuevo.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Abriendo formulario de inventario de aulas...");
            } else {
                alert("Simulación: Abriendo formulario de inventario de aulas...");
            }
        });
    }

    viewButtons.forEach(btn => {
        btn.addEventListener("click", function() {
            const aula = btn.getAttribute("data-aula");
            if (typeof showNotification === "function") {
                showNotification(`Inventario: Abriendo hoja de auditoría técnica del espacio físico "${aula}".`);
            } else {
                alert(`Inventario: Abriendo hoja de auditoría técnica del espacio físico "${aula}".`);
            }
        });
    });
})();
