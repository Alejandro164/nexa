package com.chavescr.nexa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.EscalaNotasService;
import com.chavescr.nexa.service.NotasConsultaService;
import com.chavescr.nexa.service.RebajaConductaService;
import com.chavescr.nexa.service.TipoComponenteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MenuController {

    private final TipoComponenteService tipoComponenteService;
    private final NotasConsultaService notasConsultaService;
    private final EscalaNotasService escalaNotasService;
    private final RebajaConductaService rebajaConductaService;

    public MenuController(TipoComponenteService tipoComponenteService, NotasConsultaService notasConsultaService,
            EscalaNotasService escalaNotasService, RebajaConductaService rebajaConductaService) {
        this.tipoComponenteService = tipoComponenteService;
        this.notasConsultaService = notasConsultaService;
        this.escalaNotasService = escalaNotasService;
        this.rebajaConductaService = rebajaConductaService;
    }

    @GetMapping("/estudiantes")
    public String estudiantes(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "estudiantes/index :: htmx-content" : "estudiantes/index";
    }

    @GetMapping("/gestion-academica")
    public String gestionAcademica(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            Model model, HttpSession session) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        model.addAttribute("tiposComponente", direccionId == null
                ? java.util.List.of()
                : tipoComponenteService.listarActivos(direccionId));
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
    public String comedor(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            Model model, HttpServletRequest request) {
        model.addAttribute("rolComedor", rolDeComedor(request));
        model.addAttribute("puedePrevisualizar",
                request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_SYSTEM_CONFIG"));
        return htmxRequest ? "comedor/index :: htmx-content" : "comedor/index";
    }

    /** Quien publica el menú es el administrador. La familia confirma; dirección y docencia consultan. */
    private static String rolDeComedor(HttpServletRequest request) {
        if (request.isUserInRole("ROLE_ADMIN")) {
            return "admin";
        }
        if (request.isUserInRole("ROLE_DIRECTOR")) {
            return "director";
        }
        if (request.isUserInRole("ROLE_DOCENTE")) {
            return "docente";
        }
        if (request.isUserInRole("ROLE_ESTUDIANTE")) {
            return "estudiante";
        }
        return "padre";
    }

    @GetMapping("/notas")
    public String notas(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            Model model, HttpSession session, HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            throw new DireccionNoSeleccionadaException();
        }
        boolean supervision = request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        model.addAttribute("notasCatalogo", notasConsultaService.consultar(
                direccionId, usuarioId, supervision, supervision));
        model.addAttribute("escala", escalaNotasService.vista(direccionId));
        model.addAttribute("conAsistencia", rebajaConductaService.asistenciaRebajaComponente(direccionId));
        return htmxRequest ? "notas/index :: htmx-content" : "notas/index";
    }

    @GetMapping("/apelaciones")
    public String apelaciones(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            HttpSession session) {
        if (session.getAttribute("SESSION_DIRECCION_ID") == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return htmxRequest ? "apelaciones/index :: htmx-content" : "apelaciones/index";
    }

    @GetMapping("/ampliacion")
    public String ampliacion(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            HttpSession session) {
        if (session.getAttribute("SESSION_DIRECCION_ID") == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return htmxRequest ? "ampliacion/index :: htmx-content" : "ampliacion/index";
    }

    @GetMapping("/personal")
    public String personal(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "personal/index :: htmx-content" : "personal/index";
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
