package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.WhatsAppConfiguracion;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.WhatsAppConfiguracionService;
import com.chavescr.nexa.service.WhatsAppMensajeService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Conexión con WhatsApp Business (pestaña "Integraciones" de Configuración Institucional). */
@Controller
@RequestMapping("/configuracion/whatsapp")
public class WhatsAppConfiguracionController {

    private final WhatsAppConfiguracionService configuracionService;
    private final WhatsAppMensajeService mensajeService;

    public WhatsAppConfiguracionController(WhatsAppConfiguracionService configuracionService,
            WhatsAppMensajeService mensajeService) {
        this.configuracionService = configuracionService;
        this.mensajeService = mensajeService;
    }

    @PostMapping
    public String guardar(@RequestParam String phoneNumberId,
            @RequestParam(required = false) String businessAccountId,
            @RequestParam(required = false) String numeroMostrar,
            @RequestParam(required = false) String accessToken,
            @RequestParam(defaultValue = "false") boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            WhatsAppConfiguracion config = configuracionService.guardar(direccionId, phoneNumberId,
                    businessAccountId, numeroMostrar, accessToken, activo);
            model.addAttribute("whatsappConfig", config);
            notificarGuardado(response, "Configuración de WhatsApp guardada correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("whatsappConfig", configuracionService.obtener(direccionId));
            notificarError(response, e.getMessage());
        }
        return "configuracion/whatsapp/whatsapp :: content";
    }

    @PostMapping("/probar")
    public String probarConexion(Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            var info = mensajeService.probarConexion(direccionId);
            String detalle = info.numeroVerificado() != null ? " (" + info.numeroVerificado() + ")" : "";
            notificarGuardado(response, "Conexión verificada correctamente con WhatsApp" + detalle);
        } catch (RuntimeException e) {
            notificarError(response, e.getMessage());
        }
        model.addAttribute("whatsappConfig", configuracionService.obtener(direccionId));
        return "configuracion/whatsapp/whatsapp :: content";
    }

    @PostMapping("/prueba-mensaje")
    public String enviarPrueba(@RequestParam String telefonoPrueba, Model model, HttpSession session,
            HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            mensajeService.enviarMensajePrueba(direccionId, telefonoPrueba);
            notificarGuardado(response, "Mensaje de prueba enviado a " + telefonoPrueba);
        } catch (RuntimeException e) {
            notificarError(response, e.getMessage());
        }
        model.addAttribute("whatsappConfig", configuracionService.obtener(direccionId));
        return "configuracion/whatsapp/whatsapp :: content";
    }

    private Long direccionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_DIRECCION_ID");
    }

    private Long requerirDireccion(HttpSession session) {
        Long direccionId = direccionId(session);
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return direccionId;
    }

    private void notificarGuardado(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"whatsappGuardado\":{\"mensaje\":\"" + mensaje + "\"}}");
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"whatsappError\":{\"mensaje\":\"" + escapar(mensaje) + "\"}}");
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("\"", "'");
    }
}
