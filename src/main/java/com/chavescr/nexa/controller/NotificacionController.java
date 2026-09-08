package com.chavescr.nexa.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.NotificacionService;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public String dropdown(@AuthenticationPrincipal CustomUserDetails usuario, Model model) {
        model.addAttribute("notificaciones", notificacionService.listarNoLeidas(usuario.getId()));
        return "layout/notificaciones-dropdown :: dropdown-content";
    }

    @PostMapping("/{id}/leer")
    public String marcarLeida(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails usuario, Model model) {
        notificacionService.marcarLeida(id);
        model.addAttribute("notificaciones", notificacionService.listarNoLeidas(usuario.getId()));
        return "layout/notificaciones-dropdown :: dropdown-content";
    }
}
