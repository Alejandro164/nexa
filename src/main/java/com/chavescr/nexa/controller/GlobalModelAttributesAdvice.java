package com.chavescr.nexa.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.NotificacionService;
import com.chavescr.nexa.service.SesionDireccionService;
import com.chavescr.nexa.service.SesionDireccionService.MenuCambioDireccion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Expone datos comunes a todas las vistas (ej. el contador de notificaciones del topbar). */
@ControllerAdvice
public class GlobalModelAttributesAdvice {

    private final NotificacionService notificacionService;
    private final SesionDireccionService sesionDireccionService;

    public GlobalModelAttributesAdvice(NotificacionService notificacionService,
            SesionDireccionService sesionDireccionService) {
        this.notificacionService = notificacionService;
        this.sesionDireccionService = sesionDireccionService;
    }

    @ModelAttribute("notificacionesNoLeidas")
    public long notificacionesNoLeidas(@AuthenticationPrincipal CustomUserDetails usuario) {
        return usuario == null ? 0 : notificacionService.contarNoLeidas(usuario.getId());
    }

    @ModelAttribute("sinDireccionAdmin")
    public boolean sinDireccionAdmin(@AuthenticationPrincipal CustomUserDetails usuario, HttpSession session) {
        return usuario != null
                && usuario.getRoles().contains("ROLE_ADMIN")
                && session.getAttribute("SESSION_DIRECCION_ID") == null;
    }

    /**
     * Solo en la página completa: el menú lateral no se vuelve a pintar en cada fragmento HTMX.
     * Con una sola dirección el valor existe pero no es visible, y la plantilla no dibuja el selector.
     */
    @ModelAttribute("menuDireccion")
    public MenuCambioDireccion menuDireccion(@AuthenticationPrincipal CustomUserDetails usuario,
            HttpServletRequest request, HttpSession session) {
        if (usuario == null || "true".equalsIgnoreCase(request.getHeader("HX-Request"))
                || !puedeCambiarDireccion(usuario)) {
            return null;
        }
        Object actual = session.getAttribute("SESSION_DIRECCION_ID");
        Long actualId = actual instanceof Long id ? id : null;
        return sesionDireccionService.menu(usuario.getRoles().contains("ROLE_ADMIN"), actualId);
    }

    private static boolean puedeCambiarDireccion(CustomUserDetails usuario) {
        return usuario.getRoles().contains("ROLE_ADMIN") || usuario.getRoles().contains("ROLE_DIRECTOR");
    }
}
