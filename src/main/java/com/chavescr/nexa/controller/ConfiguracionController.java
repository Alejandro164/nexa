package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.entity.WhatsAppConfiguracion;
import com.chavescr.nexa.service.WhatsAppConfiguracionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    private final WhatsAppConfiguracionService whatsAppConfiguracionService;

    public ConfiguracionController(WhatsAppConfiguracionService whatsAppConfiguracionService) {
        this.whatsAppConfiguracionService = whatsAppConfiguracionService;
    }

    @GetMapping
    public String configuracion(Model model, HttpServletRequest request, HttpSession session) {
        model.addAttribute("activeTab", "centro-educativo");
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        model.addAttribute("whatsappConfig", institucionId != null
                ? whatsAppConfiguracionService.obtener(institucionId)
                : WhatsAppConfiguracion.predeterminada(null));
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion/index :: htmx-content";
        }
        return "configuracion/index";
    }
}
