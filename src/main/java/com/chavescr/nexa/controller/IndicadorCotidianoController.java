package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.IndicadorCotidiano;
import com.chavescr.nexa.entity.ModuloAcademico;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.EvaluacionCotidianaService;
import com.chavescr.nexa.service.HistorialCambioService;
import com.chavescr.nexa.service.IndicadorCotidianoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/cotidiano/indicadores")
public class IndicadorCotidianoController {

    private final IndicadorCotidianoService service;
    private final AlcanceDocenteService alcanceDocenteService;
    private final EvaluacionCotidianaService evaluacionService;
    private final HistorialCambioService historialService;

    public IndicadorCotidianoController(IndicadorCotidianoService service,
            AlcanceDocenteService alcanceDocenteService, EvaluacionCotidianaService evaluacionService,
            HistorialCambioService historialService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
        this.evaluacionService = evaluacionService;
        this.historialService = historialService;
    }

    @GetMapping
    public String indicadores(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    @GetMapping("/form")
    public String nuevoIndicador(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model) {
        model.addAttribute("indicador", new IndicadorCotidiano());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/cotidiano/indicador-form :: form-content";
    }

    @GetMapping("/form/{id}")
    public String editarIndicador(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("indicador", service.obtenerIndicador(institucionId, id));
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        return "gestion-academica/cotidiano/indicador-form :: form-content";
    }

    @PostMapping
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @ModelAttribute IndicadorCotidiano indicador, Model model, HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        Long institucionId = requerirInstitucion(session);
        boolean esNuevo = indicador.getId() == null;
        try {
            IndicadorCotidiano guardado = service.guardarIndicador(institucionId, nivelId, materiaId, indicador);
            historialService.registrar(institucionId, nivelId, materiaId, ModuloAcademico.COTIDIANO, guardado.getId(),
                    guardado.getTitulo(), esNuevo ? AccionHistorial.CREAR : AccionHistorial.EDITAR,
                    usuario != null ? usuario.getId() : null, usuario != null ? usuario.getNombre() : null);
            notificarGuardado(response, esNuevo ? "Indicador creado correctamente" : "Indicador actualizado correctamente");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        Long institucionId = requerirInstitucion(session);
        try {
            IndicadorCotidiano indicador = service.obtenerIndicador(institucionId, id);
            service.eliminarIndicador(institucionId, id);
            historialService.registrar(institucionId, nivelId, materiaId, ModuloAcademico.COTIDIANO, indicador.getId(),
                    indicador.getTitulo(), AccionHistorial.ELIMINAR,
                    usuario != null ? usuario.getId() : null, usuario != null ? usuario.getNombre() : null);
            notificarGuardado(response, "Indicador eliminado correctamente");
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/cotidiano/indicadores :: content";
    }

    private void notificarGuardado(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoGuardado\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                + "\"promedioDesactualizado\":\"\"}");
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void cargarPanel(Model model, Long institucionId, Long nivelId, Long materiaId, Long docenteId) {
        var niveles = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }
        List<IndicadorCotidiano> indicadores = nivelId != null && materiaId != null
                ? service.listarIndicadores(institucionId, nivelId, materiaId)
                : List.of();
        int total = indicadores.stream().mapToInt(IndicadorCotidiano::getPorcentaje).sum();

        var periodoActivo = evaluacionService.obtenerPeriodoActivoOpcional(institucionId);
        int totalEstudiantesSeccion = nivelId != null ? evaluacionService.contarEstudiantesActivos(nivelId) : 0;
        var evaluadosPorIndicador = evaluacionService.contarEvaluadosPorIndicador(institucionId,
                indicadores.stream().map(IndicadorCotidiano::getId).toList(),
                periodoActivo != null ? periodoActivo.getId() : null);
        var promedioPorIndicador = evaluacionService.calcularPromedioPorIndicador(institucionId,
                indicadores.stream().map(IndicadorCotidiano::getId).toList(),
                periodoActivo != null ? periodoActivo.getId() : null);

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("indicadores", indicadores);
        model.addAttribute("totalAsignado", total);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("totalEstudiantesSeccion", totalEstudiantesSeccion);
        model.addAttribute("evaluadosPorIndicador", evaluadosPorIndicador);
        model.addAttribute("promedioPorIndicador", promedioPorIndicador);
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
