package com.chavescr.nexa.controller;

import java.time.LocalDate;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.PanelIncidenteConducta;
import com.chavescr.nexa.dto.PanelNotaConducta;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.service.IncidenteConductaService;
import com.chavescr.nexa.service.NotaConductaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/conducta")
public class ConductaController {

    private static final String FRAGMENTO_NOTAS = "conducta/notas/notas :: content";
    private static final String FRAGMENTO_LLAMADAS = "conducta/llamadas/llamadas :: content";
    private static final String FRAGMENTO_FORM_LLAMADA = "conducta/llamadas/formulario :: form-content";
    private static final String FRAGMENTO_BOLETAS = "conducta/boletas/boletas :: content";
    private static final String FRAGMENTO_FORM_BOLETA = "conducta/boletas/formulario :: form-content";

    private final NotaConductaService notaConductaService;
    private final IncidenteConductaService incidenteConductaService;

    public ConductaController(NotaConductaService notaConductaService,
            IncidenteConductaService incidenteConductaService) {
        this.notaConductaService = notaConductaService;
        this.incidenteConductaService = incidenteConductaService;
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

    @GetMapping("/llamadas")
    public String llamadas(@RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            Model model, HttpSession session, HttpServletRequest request) {
        cargarIncidentes(model, TipoIncidente.LLAMADA_ATENCION, requerirInstitucion(session), parseId(periodoId), parseEntero(grado), parseId(nivelId),
                docenteIdSiAplica(request, session));
        return FRAGMENTO_LLAMADAS;
    }

    @GetMapping("/llamadas/form")
    public String formLlamada(@RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        Long periodo = parseId(periodoId);
        Integer gradoNum = parseEntero(grado);
        Long seccion = parseId(nivelId);
        cargarIncidentes(model, TipoIncidente.LLAMADA_ATENCION, institucionId, periodo, gradoNum, seccion, docenteId);
        model.addAttribute("estudiantes",
                incidenteConductaService.listarEstudiantes(institucionId, gradoNum, seccion, docenteId));
        model.addAttribute("fecha", LocalDate.now());
        return FRAGMENTO_FORM_LLAMADA;
    }

    @PostMapping("/llamadas")
    public String guardarLlamada(@RequestParam(required = false) String estudianteId,
            @RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String descripcion,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        Long periodo = parseId(periodoId);
        Integer gradoNum = parseEntero(grado);
        Long seccion = parseId(nivelId);
        try {
            String mensaje = incidenteConductaService.crear(TipoIncidente.LLAMADA_ATENCION, institucionId, periodo,
                    parseId(estudianteId), parseFecha(fecha), motivo, descripcion, null,
                    (Long) session.getAttribute("SESSION_USUARIO_ID"), docenteId);
            notificar(response, mensaje, false);
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            notificar(response, e.getMessage(), true);
            return FRAGMENTO_FORM_LLAMADA;
        }
        cargarIncidentes(model, TipoIncidente.LLAMADA_ATENCION, institucionId, periodo, gradoNum, seccion, docenteId);
        return FRAGMENTO_LLAMADAS;
    }

    @PostMapping("/llamadas/resolver")
    public String resolverLlamada(@RequestParam(required = false) String incidenteId,
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
            String mensaje = incidenteConductaService.resolver(TipoIncidente.LLAMADA_ATENCION, institucionId,
                    parseId(incidenteId), docenteId);
            notificar(response, mensaje, false);
        } catch (IllegalArgumentException e) {
            notificar(response, e.getMessage(), true);
        }
        cargarIncidentes(model, TipoIncidente.LLAMADA_ATENCION, institucionId, periodo, gradoNum, seccion, docenteId);
        return FRAGMENTO_LLAMADAS;
    }

    @GetMapping("/boletas")
    public String boletas(@RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            Model model, HttpSession session, HttpServletRequest request) {
        cargarIncidentes(model, TipoIncidente.BOLETA, requerirInstitucion(session), parseId(periodoId),
                parseEntero(grado), parseId(nivelId), docenteIdSiAplica(request, session));
        return FRAGMENTO_BOLETAS;
    }

    @GetMapping("/boletas/form")
    public String formBoleta(@RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        Long periodo = parseId(periodoId);
        Integer gradoNum = parseEntero(grado);
        Long seccion = parseId(nivelId);
        cargarIncidentes(model, TipoIncidente.BOLETA, institucionId, periodo, gradoNum, seccion, docenteId);
        model.addAttribute("estudiantes",
                incidenteConductaService.listarEstudiantes(institucionId, gradoNum, seccion, docenteId));
        model.addAttribute("fecha", LocalDate.now());
        return FRAGMENTO_FORM_BOLETA;
    }

    @PostMapping("/boletas")
    public String guardarBoleta(@RequestParam(required = false) String estudianteId,
            @RequestParam(required = false) String periodoId,
            @RequestParam(required = false) String grado,
            @RequestParam(required = false) String nivelId,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String puntos,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        Long periodo = parseId(periodoId);
        Integer gradoNum = parseEntero(grado);
        Long seccion = parseId(nivelId);
        try {
            String mensaje = incidenteConductaService.crear(TipoIncidente.BOLETA, institucionId, periodo,
                    parseId(estudianteId), parseFecha(fecha), motivo, descripcion, parseEntero(puntos),
                    (Long) session.getAttribute("SESSION_USUARIO_ID"), docenteId);
            notificar(response, mensaje, false);
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            notificar(response, e.getMessage(), true);
            return FRAGMENTO_FORM_BOLETA;
        }
        cargarIncidentes(model, TipoIncidente.BOLETA, institucionId, periodo, gradoNum, seccion, docenteId);
        return FRAGMENTO_BOLETAS;
    }

    @PostMapping("/boletas/resolver")
    public String resolverBoleta(@RequestParam(required = false) String incidenteId,
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
            String mensaje = incidenteConductaService.resolver(TipoIncidente.BOLETA, institucionId,
                    parseId(incidenteId), docenteId);
            notificar(response, mensaje, false);
        } catch (IllegalArgumentException e) {
            notificar(response, e.getMessage(), true);
        }
        cargarIncidentes(model, TipoIncidente.BOLETA, institucionId, periodo, gradoNum, seccion, docenteId);
        return FRAGMENTO_BOLETAS;
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

    private void cargarIncidentes(Model model, TipoIncidente tipo, Long institucionId, Long periodoId, Integer grado,
            Long nivelId, Long docenteId) {
        PanelIncidenteConducta panel = incidenteConductaService.cargarPanel(tipo, institucionId, periodoId, grado,
                nivelId, docenteId);
        model.addAttribute("periodos", panel.getPeriodos());
        model.addAttribute("grados", panel.getGrados());
        model.addAttribute("secciones", panel.getSecciones());
        model.addAttribute("filas", panel.getFilas());
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

    private LocalDate parseFecha(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
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
