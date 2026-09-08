package com.chavescr.nexa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.chavescr.nexa.service.RegistroAccesoService;

import jakarta.servlet.http.HttpSession;

@Controller
public class SeguridadController {

    @Autowired
    private RegistroAccesoService registroAccesoService;

    @GetMapping("/seguridad")
    public String index(Model model, HttpSession session,
            @RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        model.addAttribute("sesionIp", session.getAttribute("SESSION_LOGIN_IP"));
        model.addAttribute("sesionDispositivo", describirUserAgent((String) session.getAttribute("SESSION_LOGIN_USER_AGENT")));
        model.addAttribute("sesionFecha", session.getAttribute("SESSION_LOGIN_FECHA"));
        model.addAttribute("accesosRecientes", registroAccesoService.listarRecientes());
        return htmxRequest ? "seguridad/index :: htmx-content" : "seguridad/index";
    }

    /** Descripción legible del dispositivo a partir del User-Agent, sin depender de una librería externa. */
    private static String describirUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Dispositivo desconocido";
        }
        String ua = userAgent.toLowerCase();

        String navegador;
        if (ua.contains("edg/")) {
            navegador = "Microsoft Edge";
        } else if (ua.contains("opr/") || ua.contains("opera")) {
            navegador = "Opera";
        } else if (ua.contains("chrome/")) {
            navegador = "Google Chrome";
        } else if (ua.contains("firefox/")) {
            navegador = "Mozilla Firefox";
        } else if (ua.contains("safari/")) {
            navegador = "Safari";
        } else {
            navegador = "Navegador desconocido";
        }

        String sistema;
        if (ua.contains("windows")) {
            sistema = "Windows";
        } else if (ua.contains("mac os") || ua.contains("macintosh")) {
            sistema = "macOS";
        } else if (ua.contains("android")) {
            sistema = "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            sistema = "iOS";
        } else if (ua.contains("linux")) {
            sistema = "Linux";
        } else {
            sistema = "sistema desconocido";
        }

        return navegador + " en " + sistema;
    }
}
