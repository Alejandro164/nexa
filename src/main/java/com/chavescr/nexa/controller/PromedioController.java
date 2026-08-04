package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.FilaPromedio;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.PromedioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/promedio")
public class PromedioController {

    private final PromedioService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public PromedioController(PromedioService service, AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String promedio(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        var niveles = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }
        List<FilaPromedio> filas = nivelId != null && materiaId != null
                ? service.calcularPromedio(institucionId, nivelId, materiaId)
                : List.of();
        List<Double> promedios = filas.stream()
                .map(FilaPromedio::getPromedioFinal)
                .filter(p -> p != null)
                .toList();

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("filas", filas);
        model.addAttribute("estudiantesEvaluados", promedios.size());
        model.addAttribute("promedioGrupo", promedios.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        model.addAttribute("notaMasAlta", promedios.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        model.addAttribute("notaMasBaja", promedios.stream().mapToDouble(Double::doubleValue).min().orElse(0));
        return "gestion-academica/promedio/promedio :: content";
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
