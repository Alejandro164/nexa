/**
 * EstudiaFácil — Grupos y Secciones Settings Script
 */
(function() {
    console.log("Configuración: Grupos y Secciones JS cargado correctamente.");

    const btnNuevo = document.getElementById("btn-nuevo-grupo");
    const viewButtons = document.querySelectorAll(".btn-ver-grupo");

    if (btnNuevo) {
        btnNuevo.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Abriendo creador de grupos de matrícula...");
            } else {
                alert("Simulación: Abriendo creador de grupos de matrícula...");
            }
        });
    }

    viewButtons.forEach(btn => {
        btn.addEventListener("click", function() {
            const seccion = btn.getAttribute("data-seccion");
            if (typeof showNotification === "function") {
                showNotification(`Lista de Clase: Abriendo padrón de estudiantes matriculados en la sección ${seccion}.`);
            } else {
                alert(`Lista de Clase: Abriendo padrón de estudiantes matriculados en la sección ${seccion}.`);
            }
        });
    });
})();
