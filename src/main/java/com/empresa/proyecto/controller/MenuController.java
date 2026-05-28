package com.empresa.proyecto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class MenuController {

    @GetMapping("/usuarios")
    public String usuarios(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "usuarios/index :: htmx-content" : "usuarios/index";
    }

    @GetMapping("/estudiantes")
    public String estudiantes(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "estudiantes/index :: htmx-content" : "estudiantes/index";
    }

    @GetMapping("/academico")
    public String academico(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "academico/index :: htmx-content" : "academico/index";
    }

    @GetMapping("/comedor")
    public String comedor(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "comedor/index :: htmx-content" : "comedor/index";
    }

    @GetMapping("/personal")
    public String personal(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "personal/index :: htmx-content" : "personal/index";
    }

    @GetMapping("/actividades-institucionales")
    public String actividades(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "actividades/index :: htmx-content" : "actividades/index";
    }

    @GetMapping("/comunicacion")
    public String comunicacion(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "comunicacion/index :: htmx-content" : "comunicacion/index";
    }

    @GetMapping("/agenda")
    public String agenda(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "agenda/index :: htmx-content" : "agenda/index";
    }

    @GetMapping("/portal-padres")
    public String portalPadres(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "padres/index :: htmx-content" : "padres/index";
    }

    @GetMapping("/reportes")
    public String reportes(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "reportes/index :: htmx-content" : "reportes/index";
    }

    @GetMapping("/seguridad")
    public String seguridad(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "seguridad/index :: htmx-content" : "seguridad/index";
    }
}
