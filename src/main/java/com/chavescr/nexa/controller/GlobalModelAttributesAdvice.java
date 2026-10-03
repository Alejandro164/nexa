package com.chavescr.nexa.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.NotificacionService;
import com.chavescr.nexa.service.SesionDireccionService;
import com.chavescr.nexa.service.SesionDireccionService.Navegacion;

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
     * En la página completa se leen las direcciones una sola vez. En cada fragmento HTMX el menú
     * no se vuelve a pintar; el botón de cambiar institución usa la marca guardada en la sesión
     * para no repetir la consulta en cada navegación.
     */
    @ModelAttribute
    public void navegacion(@AuthenticationPrincipal CustomUserDetails usuario, HttpServletRequest request,
            HttpSession session, Model model) {
        if (usuario == null) {
            model.addAttribute("menuDireccion", null);
            model.addAttribute("puedeCambiarInstitucion", false);
            return;
        }
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            model.addAttribute("menuDireccion", null);
            model.addAttribute("puedeCambiarInstitucion",
                    Boolean.TRUE.equals(session.getAttribute("SESSION_PUEDE_CAMBIAR_INSTITUCION")));
            return;
        }
        Object actual = session.getAttribute("SESSION_DIRECCION_ID");
        Long actualId = actual instanceof Long id ? id : null;
        boolean veTodas = usuario.getRoles().contains("ROLE_SYSTEM_CONFIG");
        Navegacion nav = sesionDireccionService.navegacion(veTodas, actualId);
        model.addAttribute("menuDireccion", nav.menu());
        model.addAttribute("puedeCambiarInstitucion", nav.puedeCambiarInstitucion());
        session.setAttribute("SESSION_PUEDE_CAMBIAR_INSTITUCION", nav.puedeCambiarInstitucion());
    }
}
