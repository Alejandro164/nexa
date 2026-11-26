package com.chavescr.nexa.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.PestanaRubro;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
import com.chavescr.nexa.service.EscalaNotasService;
import com.chavescr.nexa.service.GestionAcademicaService;
import com.chavescr.nexa.service.NotasConsultaService;
import com.chavescr.nexa.service.RebajaConductaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MenuController {

    private final NotasConsultaService notasConsultaService;
    private final EscalaNotasService escalaNotasService;
    private final RebajaConductaService rebajaConductaService;
    private final GestionAcademicaService gestionAcademicaService;

    public MenuController(NotasConsultaService notasConsultaService,
            EscalaNotasService escalaNotasService, RebajaConductaService rebajaConductaService,
            GestionAcademicaService gestionAcademicaService) {
        this.notasConsultaService = notasConsultaService;
        this.escalaNotasService = escalaNotasService;
        this.rebajaConductaService = rebajaConductaService;
        this.gestionAcademicaService = gestionAcademicaService;
    }

    private static final java.util.Set<String> TABS_ESTUDIANTES = java.util.Set.of(
            "expedientes", "prematricula", "matricula", "consolidar", "familiares", "adecuaciones");

    @GetMapping("/estudiantes")
    public String estudiantes(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @RequestParam(required = false) String tab, Model model) {
        String tabNormalizada = tab != null && TABS_ESTUDIANTES.contains(tab.trim().toLowerCase())
                ? tab.trim().toLowerCase()
                : "expedientes";
        model.addAttribute("tabInicial", tabNormalizada);
        return htmxRequest ? "estudiantes/index :: htmx-content" : "estudiantes/index";
    }

    /**
     * Primero se elige la materia; las pestañas son los rubros que califica su componente de evaluación.
     * La materia queda en sesión para volver a ella; {@code tab} conserva la pestaña al cambiar de materia.
     */
    @GetMapping("/gestion-academica")
    public String gestionAcademica(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            @RequestParam(required = false) Long materiaId, @RequestParam(required = false) String tab,
            Model model, HttpSession session, HttpServletRequest request) {
        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        List<Materia> materias = direccionId == null ? List.of()
                : gestionAcademicaService.materias(direccionId, docenteIdSiAplica(request, session));
        Object recordada = session.getAttribute("SESSION_GESTION_MATERIA_ID");
        Long elegida = gestionAcademicaService.elegirMateria(materias, materiaId,
                recordada instanceof Long id ? id : null);
        if (elegida != null) {
            session.setAttribute("SESSION_GESTION_MATERIA_ID", elegida);
        }
        List<PestanaRubro> pestanas = direccionId == null ? List.of()
                : gestionAcademicaService.pestanas(direccionId, elegida);

        boolean tabValida = "asistencia".equals(tab) || "promedio".equals(tab)
                || pestanas.stream().anyMatch(p -> p.getId().equals(tab));
        model.addAttribute("materias", materias);
        model.addAttribute("materiaId", elegida);
        model.addAttribute("pestanas", pestanas);
        model.addAttribute("tabInicial", tabValida ? tab : "asistencia");
        return htmxRequest ? "gestion-academica/index :: htmx-content" : "gestion-academica/index";
    }

    /** Un docente (sin rol de director ni admin) solo ve las materias de su horario. */
    private static Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
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
        // La escala depende del ciclo de la sección de cada estudiante: la vista elige la suya.
        var escalas = escalaNotasService.vistasPorCiclo(direccionId);
        model.addAttribute("escalasPorCiclo", escalas.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().tramos())));
        model.addAttribute("escala", escalas.get(com.chavescr.nexa.entity.CicloEducativo.PRIMER_CICLO));
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
