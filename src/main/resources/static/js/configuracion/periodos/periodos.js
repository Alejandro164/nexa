/**
 * EstudiaFácil — Períodos Settings Script
 */
(function() {
    console.log("Configuración: Períodos JS cargado correctamente.");

    const btnNuevo = document.getElementById("btn-nuevo-periodo");
    const viewButtons = document.querySelectorAll(".btn-ver-periodo");

    if (btnNuevo) {
        btnNuevo.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Abriendo formulario para agregar período...");
            } else {
                alert("Simulación: Abriendo formulario para agregar período...");
            }
        });
    }

    viewButtons.forEach(btn => {
        btn.addEventListener("click", function() {
            const periodo = btn.getAttribute("data-periodo");
            if (typeof showNotification === "function") {
                showNotification(`Detalles: Inspeccionando ponderaciones para "${periodo}".`);
            } else {
                alert(`Detalles: Inspeccionando ponderaciones para "${periodo}".`);
            }
        });
    });
})();
