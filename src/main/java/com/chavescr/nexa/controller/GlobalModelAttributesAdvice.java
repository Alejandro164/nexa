package com.chavescr.nexa.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.NotificacionService;

/** Expone datos comunes a todas las vistas (ej. el contador de notificaciones del topbar). */
@ControllerAdvice
public class GlobalModelAttributesAdvice {

    private final NotificacionService notificacionService;

    public GlobalModelAttributesAdvice(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @ModelAttribute("notificacionesNoLeidas")
    public long notificacionesNoLeidas(@AuthenticationPrincipal CustomUserDetails usuario) {
        return usuario == null ? 0 : notificacionService.contarNoLeidas(usuario.getId());
    }
}
