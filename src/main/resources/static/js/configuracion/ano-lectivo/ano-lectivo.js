/**
 * EstudiaFácil — Año Lectivo Settings Script
 */
(function() {
    console.log("Configuración: Año Lectivo JS cargado correctamente.");

    const btnNuevo = document.getElementById("btn-nuevo-ano");
    const editButtons = document.querySelectorAll(".btn-edit-ano");

    if (btnNuevo) {
        btnNuevo.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Aperturando un nuevo Ciclo Lectivo...");
            } else {
                alert("Simulación: Aperturando un nuevo Ciclo Lectivo...");
            }
        });
    }

    editButtons.forEach(btn => {
        btn.addEventListener("click", function() {
            const year = btn.getAttribute("data-year");
            if (typeof showNotification === "function") {
                showNotification(`Auditoría: Accediendo al historial de configuración del Año Lectivo ${year}.`);
            } else {
                alert(`Auditoría: Accediendo al historial de configuración del Año Lectivo ${year}.`);
            }
        });
    });
})();
