package com.chavescr.nexa.controller;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.service.ConfiguracionAcademicaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/configuracion-academica")
public class ConfiguracionAcademicaController {

    private final ConfiguracionAcademicaService service;

    public ConfiguracionAcademicaController(ConfiguracionAcademicaService service) {
        this.service = service;
    }

    @GetMapping
    public String configuracionAcademica(Model model, HttpServletRequest request, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) {
            return "redirect:/";
        }

        cargarPagina(model, institucionId);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "configuracion-academica/index :: htmx-content";
        }
        return "configuracion-academica/index";
    }

    @GetMapping("/periodos/form")
    public String nuevoPeriodo(Model model) {
        model.addAttribute("periodo", new PeriodoAcademico());
        return "configuracion-academica/periodos/form :: form-content";
    }

    @GetMapping("/periodos/form/{id}")
    public String editarPeriodo(@PathVariable Long id, Model model, HttpSession session) {
        model.addAttribute("periodo", service.obtenerPeriodo(requerirInstitucion(session), id));
        return "configuracion-academica/periodos/form :: form-content";
    }

    @PostMapping("/periodos")
    public String guardarPeriodo(@ModelAttribute PeriodoAcademico periodo, Model model,
            HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        service.guardarPeriodo(institucionId, periodo);
        model.addAttribute("periodos", service.listarPeriodos(institucionId));
        notificarGuardado(response, "Período guardado correctamente");
        return "configuracion-academica/periodos/periodos :: content";
    }

    @DeleteMapping("/periodos/{id}")
    public String eliminarPeriodo(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarPeriodo(institucionId, id);
        model.addAttribute("periodos", service.listarPeriodos(institucionId));
        return "configuracion-academica/periodos/periodos :: content";
    }

    @GetMapping("/niveles/form")
    public String nuevoNivel(Model model) {
        model.addAttribute("nivel", new NivelAcademico());
        return "configuracion-academica/niveles/form :: form-content";
    }

    @GetMapping("/niveles/form/{id}")
    public String editarNivel(@PathVariable Long id, Model model, HttpSession session) {
        model.addAttribute("nivel", service.obtenerNivel(requerirInstitucion(session), id));
        return "configuracion-academica/niveles/form :: form-content";
    }

    @PostMapping("/niveles")
    public String guardarNivel(@ModelAttribute NivelAcademico nivel, Model model,
            HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        service.guardarNivel(institucionId, nivel);
        model.addAttribute("niveles", service.listarNiveles(institucionId));
        notificarGuardado(response, "Nivel guardado correctamente");
        return "configuracion-academica/niveles/niveles :: content";
    }

    @DeleteMapping("/niveles/{id}")
    public String eliminarNivel(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarNivel(institucionId, id);
        model.addAttribute("niveles", service.listarNiveles(institucionId));
        return "configuracion-academica/niveles/niveles :: content";
    }

    @GetMapping("/materias/form")
    public String nuevaMateria(Model model) {
        model.addAttribute("materia", new Materia());
        return "configuracion-academica/materias/form :: form-content";
    }

    @GetMapping("/materias/form/{id}")
    public String editarMateria(@PathVariable Long id, Model model, HttpSession session) {
        model.addAttribute("materia", service.obtenerMateria(requerirInstitucion(session), id));
        return "configuracion-academica/materias/form :: form-content";
    }

    @PostMapping("/materias")
    public String guardarMateria(@ModelAttribute Materia materia, Model model,
            HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        service.guardarMateria(institucionId, materia);
        model.addAttribute("materias", service.listarMaterias(institucionId));
        notificarGuardado(response, "Materia guardada correctamente");
        return "configuracion-academica/materias/materias :: content";
    }

    @DeleteMapping("/materias/{id}")
    public String eliminarMateria(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarMateria(institucionId, id);
        model.addAttribute("materias", service.listarMaterias(institucionId));
        return "configuracion-academica/materias/materias :: content";
    }

    @GetMapping("/horario")
    public String horario(@RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) Long nivelId, Model model, HttpSession session) {
        cargarHorario(model, requerirInstitucion(session), periodoId, nivelId);
        return "configuracion-academica/horario/horario :: content";
    }

    @GetMapping("/horario/form")
    public String formularioHorario(@RequestParam Long periodoId,
            @RequestParam Long nivelId,
            @RequestParam String dia,
            @RequestParam Integer numeroLeccion,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        HorarioLeccion leccion = service.obtenerLeccion(institucionId, periodoId, nivelId, dia, numeroLeccion);
        if (leccion.getHoraInicio() == null) {
            LocalTime inicio = LocalTime.of(7, 0).plusMinutes((long) (numeroLeccion - 1) * 50);
            leccion.setHoraInicio(inicio);
            leccion.setHoraFin(inicio.plusMinutes(40));
            leccion.setDia(dia);
            leccion.setNumeroLeccion(numeroLeccion);
        }
        model.addAttribute("leccion", leccion);
        model.addAttribute("periodoId", periodoId);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materias", service.listarMateriasActivas(institucionId));
        model.addAttribute("docentes", service.listarDocentes(institucionId));
        return "configuracion-academica/horario/form :: form-content";
    }

    @PostMapping("/horario")
    public String guardarHorario(@RequestParam(required = false) Long id,
            @RequestParam Long periodoId,
            @RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Long docenteId,
            @RequestParam String dia,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        service.guardarLeccion(institucionId, id, periodoId, nivelId, materiaId, docenteId,
                dia, numeroLeccion, horaInicio, horaFin);
        cargarHorario(model, institucionId, periodoId, nivelId);
        notificarGuardado(response, "Lección asignada correctamente");
        return "configuracion-academica/horario/horario :: content";
    }

    @DeleteMapping("/horario/{id}")
    public String eliminarHorario(@PathVariable Long id,
            @RequestParam Long periodoId,
            @RequestParam Long nivelId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        service.eliminarLeccion(institucionId, id);
        cargarHorario(model, institucionId, periodoId, nivelId);
        return "configuracion-academica/horario/horario :: content";
    }

    private void cargarPagina(Model model, Long institucionId) {
        model.addAttribute("periodos", service.listarPeriodos(institucionId));
        model.addAttribute("niveles", service.listarNiveles(institucionId));
        model.addAttribute("materias", service.listarMaterias(institucionId));
        cargarHorario(model, institucionId, null, null);
    }

    private void cargarHorario(Model model, Long institucionId, Long periodoId, Long nivelId) {
        var periodos = service.listarPeriodosActivos(institucionId);
        var niveles = service.listarNivelesActivos(institucionId);
        if (periodoId == null && !periodos.isEmpty()) {
            periodoId = periodos.get(0).getId();
        }
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        model.addAttribute("periodosActivos", periodos);
        model.addAttribute("nivelesActivos", niveles);
        model.addAttribute("periodoSeleccionado", periodoId);
        model.addAttribute("nivelSeleccionado", nivelId);
        model.addAttribute("dias", ConfiguracionAcademicaService.DIAS);
        model.addAttribute("lecciones", ConfiguracionAcademicaService.LECCIONES);
        model.addAttribute("horario", service.obtenerHorario(institucionId, periodoId, nivelId));
    }

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) {
            throw new IllegalStateException("Debe seleccionar una institución");
        }
        return institucionId;
    }

    private void notificarGuardado(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoGuardado\":{\"mensaje\":\"" + mensaje + "\"}}");
    }
}
