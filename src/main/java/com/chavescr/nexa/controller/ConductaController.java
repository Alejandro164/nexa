package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.PanelNotaConducta;
import com.chavescr.nexa.service.NotaConductaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/conducta")
public class ConductaController {

    private static final String FRAGMENTO_NOTAS = "conducta/notas/notas :: content";

    private final NotaConductaService notaConductaService;

    public ConductaController(NotaConductaService notaConductaService) {
        this.notaConductaService = notaConductaService;
    }

    @GetMapping
    public String index(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest) {
        return htmxRequest ? "conducta/index :: htmx-content" : "conducta/index";
    }

    @GetMapping("/notas")
    public String notas(@RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            Model model, HttpSession session, HttpServletRequest request) {
        cargarNotas(model, requerirInstitucion(session), parseId(periodoId), parseEntero(grado), parseId(nivelId),
                docenteIdSiAplica(request, session));
        return FRAGMENTO_NOTAS;
    }

    @PostMapping("/notas/enviar")
    public String enviar(@RequestParam(required = false) String estudianteId,
            @RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        Long periodo = parseId(periodoId);
        Integer gradoNum = parseEntero(grado);
        Long seccion = parseId(nivelId);
        try {
            String mensaje = parseId(estudianteId) == null
                    ? notaConductaService.enviarTodas(institucionId, periodo, gradoNum, seccion, docenteId)
                    : notaConductaService.enviar(institucionId, periodo, parseId(estudianteId), docenteId);
            notificar(response, mensaje, false);
        } catch (IllegalArgumentException e) {
            notificar(response, e.getMessage(), true);
        }
        cargarNotas(model, institucionId, periodo, gradoNum, seccion, docenteId);
        return FRAGMENTO_NOTAS;
    }

    private void cargarNotas(Model model, Long institucionId, Long periodoId, Integer grado, Long nivelId,
            Long docenteId) {
        PanelNotaConducta panel = notaConductaService.cargarPanel(institucionId, periodoId, grado, nivelId, docenteId);
        model.addAttribute("periodos", panel.getPeriodos());
        model.addAttribute("grados", panel.getGrados());
        model.addAttribute("secciones", panel.getSecciones());
        model.addAttribute("filas", panel.getFilas());
        model.addAttribute("resumen", panel.getResumen());
        model.addAttribute("periodoId", panel.getPeriodoId());
        model.addAttribute("grado", panel.getGrado());
        model.addAttribute("nivelId", panel.getNivelId());
        model.addAttribute("avisoPeriodo", panel.getAvisoPeriodo());
    }

    private void notificar(HttpServletResponse response, String mensaje, boolean error) {
        String evento = error ? "academicoError" : "academicoGuardado";
        response.setHeader("HX-Trigger", "{\"" + evento + "\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }

    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseEntero(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
