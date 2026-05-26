/**
 * EstudiaFácil — Centro Educativo Settings Script
 */
(function() {
    console.log("Configuración: Centro Educativo JS cargado correctamente.");

    const btnUpload = document.getElementById("btn-upload-logo");
    const btnSave = document.getElementById("btn-save-centro");
    const formCentro = document.getElementById("form-centro-educativo");

    if (btnUpload) {
        btnUpload.addEventListener("click", function() {
            if (typeof showNotification === "function") {
                showNotification("Simulación: Subiendo nuevo logotipo institucional...");
            } else {
                alert("Simulación: Subiendo nuevo logotipo institucional...");
            }
        });
    }

    if (formCentro) {
        formCentro.addEventListener("submit", function(e) {
            e.preventDefault();
            const nombre = document.getElementById("centro-nombre").value;
            if (typeof showNotification === "function") {
                showNotification(`Ajustes del Centro Educativo "${nombre}" guardados exitosamente.`);
            } else {
                alert("Ajustes del Centro Educativo guardados.");
            }
        });
    }
})();
