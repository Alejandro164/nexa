/**
 * EstudiaFácil — Institucion Educativo Settings Script
 */
(function() {
    console.log("Configuración: Institucion Educativo JS cargado correctamente.");

    const btnUpload = document.getElementById("btn-upload-logo");
    const btnSave = document.getElementById("btn-save-institucion");
    const formInstitucion = document.getElementById("form-institucion-educativo");

    if (btnUpload) {
        btnUpload.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Subiendo nuevo logotipo institucional...");
            } else {
                alert("Simulación: Subiendo nuevo logotipo institucional...");
            }
        });
    }

    if (formInstitucion) {
        formInstitucion.addEventListener("submit", function(e) {
            e.preventDefault();
            const nombre = document.getElementById("institucion-nombre").value;
            if (typeof showNotification === "function") {
                showNotification(`Ajustes del Institucion Educativo "${nombre}" guardados exitosamente.`);
            } else {
                alert("Ajustes del Institucion Educativo guardados.");
            }
        });
    }
})();
