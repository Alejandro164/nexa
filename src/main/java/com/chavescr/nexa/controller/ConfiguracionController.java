package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.entity.CorreoConfiguracion;
import com.chavescr.nexa.entity.WhatsAppConfiguracion;
import com.chavescr.nexa.service.CorreoConfiguracionService;
import com.chavescr.nexa.service.WhatsAppConfiguracionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private final WhatsAppConfiguracionService whatsAppConfiguracionService;
    private final CorreoConfiguracionService correoConfiguracionService;

    public ConfiguracionController(WhatsAppConfiguracionService whatsAppConfiguracionService,
            CorreoConfiguracionService correoConfiguracionService) {
        this.whatsAppConfiguracionService = whatsAppConfiguracionService;
        this.correoConfiguracionService = correoConfiguracionService;
    }

    @GetMapping
    public String configuracion(Model model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("activeTab", "institucion-educativo");
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        model.addAttribute("whatsappConfig", direccionId != null
                ? whatsAppConfiguracionService.obtener(direccionId)
                : WhatsAppConfiguracion.predeterminada(null));
        model.addAttribute("correoConfig", direccionId != null
                ? correoConfiguracionService.obtener(direccionId)
                : CorreoConfiguracion.predeterminada(null));
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion/index :: htmx-content";
        }
        return "configuracion/index";
    }
}
