package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.service.AsistenteConfiguracionService;
import com.chavescr.nexa.service.AsistenteService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Conexión del Chat Asistente con Grok de xAI (Configuración → Integraciones → Asistente IA). Es una sola
 * para todo el sistema; /configuracion/** solo lo abre System Config.
 */
@Controller
@RequestMapping("/configuracion/asistente")
public class AsistenteConfiguracionController {

    private final AsistenteConfiguracionService configuracionService;
    private final AsistenteService asistenteService;

    public AsistenteConfiguracionController(AsistenteConfiguracionService configuracionService,
            AsistenteService asistenteService) {
        this.configuracionService = configuracionService;
        this.asistenteService = asistenteService;
    }

    @PostMapping
    public String guardar(@RequestParam(required = false) String apiKey, @RequestParam String modelo,
            @RequestParam(defaultValue = "false") boolean activo, Model model, HttpServletResponse response) {
        try {
            configuracionService.guardar(apiKey, modelo, activo);
            notificar(response, "asistenteGuardado", activo
                    ? "Asistente guardado y activado. Los usuarios lo verán al recargar la página."
                    : "Configuración del asistente guardada.");
        } catch (IllegalArgumentException e) {
            notificar(response, "asistenteError", e.getMessage());
        }
        cargar(model);
        return "configuracion/asistente/asistente :: content";
    }

    @PostMapping("/probar")
    public String probar(Model model, HttpServletResponse response) {
        try {
            asistenteService.probarConexion();
            notificar(response, "asistenteGuardado", "Conexión con Grok verificada correctamente.");
        } catch (AsistenteService.AsistenteException e) {
            notificar(response, "asistenteError", e.getMessage());
        }
        cargar(model);
        return "configuracion/asistente/asistente :: content";
    }

    private void cargar(Model model) {
        model.addAttribute("asistenteConfig", configuracionService.obtener());
        model.addAttribute("modelosAsistente", AsistenteConfiguracionService.MODELOS);
    }

    private static void notificar(HttpServletResponse response, String evento, String mensaje) {
        String texto = mensaje == null ? "" : mensaje.replace("\\", "\\\\").replace("\"", "'");
        response.setHeader("HX-Trigger", "{\"" + evento + "\":{\"mensaje\":\"" + texto + "\"}}");
    }
}
