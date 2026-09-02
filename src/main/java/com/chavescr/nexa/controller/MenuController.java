package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class MenuController {

    @GetMapping("/estudiantes")
    public String estudiantes(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "estudiantes/index :: htmx-content" : "estudiantes/index";
    }

    @GetMapping("/gestion-academica")
    public String gestionAcademica(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "gestion-academica/index :: htmx-content" : "gestion-academica/index";
    }

    @GetMapping("/gestion-especial")
    public String gestionEspecial(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "gestion-especial/index :: htmx-content" : "gestion-especial/index";
    }

    @GetMapping("/coordinacion-academica")
    public String coordinacionAcademica(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "coordinacion-academica/index :: htmx-content" : "coordinacion-academica/index";
    }

    @GetMapping("/evaluacion-academica")
    public String evaluacionAcademica(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "evaluacion-academica/index :: htmx-content" : "evaluacion-academica/index";
    }

    @GetMapping("/comedor")
    public String comedor(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "comedor/index :: htmx-content" : "comedor/index";
    }

    @GetMapping("/conducta")
    public String conducta(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "conducta/index :: htmx-content" : "conducta/index";
    }

    @GetMapping("/personal")
    public String personal(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "personal/index :: htmx-content" : "personal/index";
    }

    @GetMapping("/padres")
    public String padres(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "gestion-padres/index :: htmx-content" : "gestion-padres/index";
    }

    @GetMapping("/comunicacion")
    public String comunicacion(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "comunicacion/index :: htmx-content" : "comunicacion/index";
    }

    @GetMapping("/agenda")
    public String agenda(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "agenda/index :: htmx-content" : "agenda/index";
    }

    @GetMapping("/reportes")
    public String reportes(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "reportes/index :: htmx-content" : "reportes/index";
    }

    @GetMapping("/archivo-graduados")
    public String archivoGraduados(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "archivo/index :: htmx-content" : "archivo/index";
    }

}
