package com.chavescr.nexa.controller;

import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.DiaLaboral;
import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;
import com.chavescr.nexa.service.ConfiguracionInstitucionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion-institucional")
public class ConfiguracionInstitucionalController {

    @Autowired
    private ConfiguracionInstitucionService service;

    @GetMapping
    public String index(Model model, HttpServletRequest request, HttpSession session) {
        cargarJornada(model, requerirInstitucion(session));
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion-institucional/index :: htmx-content";
        }
        return "configuracion-institucional/index";
    }

    @GetMapping("/jornada")
    public String jornada(Model model, HttpSession session) {
        cargarJornada(model, requerirInstitucion(session));
        return "configuracion-institucional/jornada/jornada :: content";
    }

    @PostMapping("/jornada")
    public String guardarJornada(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime inicioJornada,
            @RequestParam Integer minutosLeccion,
            @RequestParam(required = false) String bloquesJornada,
            @RequestParam(name = "dias", required = false) List<String> dias,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            service.guardar(institucionId, inicioJornada, minutosLeccion, bloquesJornada, dias);
            cargarJornada(model, institucionId);
            response.setHeader("HX-Trigger",
                    "{\"institucionalGuardado\":{\"mensaje\":\"Jornada lectiva actualizada\"}}");
        } catch (IllegalArgumentException e) {
            cargarJornada(model, institucionId);
            notificarError(response, e.getMessage());
        }
        return "configuracion-institucional/jornada/jornada :: content";
    }

    @GetMapping("/componentes")
    public String componentes(HttpSession session) {
        requerirInstitucion(session);
        return "configuracion-institucional/componentes/componentes :: content";
    }

    @GetMapping("/bloqueo-leccion")
    public String bloqueoLeccion(HttpSession session) {
        requerirInstitucion(session);
        return "configuracion-institucional/bloqueo-leccion/bloqueo-leccion :: content";
    }

    private void cargarJornada(Model model, Long institucionId) {
        model.addAttribute("configJornada", service.obtener(institucionId));
        model.addAttribute("diasCatalogo", DiaLaboral.CATALOGO);
    }

    private Long requerirInstitucion(HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return institucionId;
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"institucionalError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
