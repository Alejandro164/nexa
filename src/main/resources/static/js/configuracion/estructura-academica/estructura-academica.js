/**
 * EstudiaFácil — Estructura Académica Settings Script
 */
(function() {
    console.log("Configuración: Estructura Académica JS cargado correctamente.");

    const btnNueva = document.getElementById("btn-nueva-estructura");
    const viewButtons = document.querySelectorAll(".btn-ver-estructura");

    if (btnNueva) {
        btnNueva.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Cargando catálogo curricular nacional...");
            } else {
                alert("Simulación: Cargando catálogo curricular nacional...");
            }
        });
    }

    viewButtons.forEach(btn => {
        btn.addEventListener("click", function() {
            const ciclo = btn.getAttribute("data-ciclo");
            if (typeof showNotification === "function") {
                showNotification(`Detalles: Cargando lista de asignaturas y créditos de "${ciclo}".`);
            } else {
                alert(`Detalles: Cargando lista de asignaturas y créditos de "${ciclo}".`);
            }
        });
    });
})();
