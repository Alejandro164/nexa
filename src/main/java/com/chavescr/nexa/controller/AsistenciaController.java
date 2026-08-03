package com.chavescr.nexa.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.AsistenciaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/asistencia")
public class AsistenciaController {

    private final AsistenciaService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public AsistenciaController(AsistenciaService service, AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String asistencia(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Integer numeroLeccion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, institucionId, nivelId, materiaId, numeroLeccion, fecha, docenteIdSiAplica(request, session));
        return "gestion-academica/asistencia/asistencia :: content";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Long estudianteId,
            @RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String observaciones,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        Long registradoPorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            service.registrarEstado(institucionId, estudianteId, nivelId, materiaId, numeroLeccion, fecha, estado,
                    observaciones, registradoPorId);
            notificarPromedioDesactualizado(response);
        } catch (DataIntegrityViolationException e) {
            // otra petición concurrente insertó el registro primero; reintentar una vez ya que existe
            try {
                service.registrarEstado(institucionId, estudianteId, nivelId, materiaId, numeroLeccion, fecha, estado,
                        observaciones, registradoPorId);
                notificarPromedioDesactualizado(response);
            } catch (IllegalArgumentException e2) {
                model.addAttribute("error", e2.getMessage());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, institucionId, nivelId, materiaId, numeroLeccion, fecha, docenteIdSiAplica(request, session));
        return "gestion-academica/asistencia/asistencia :: content";
    }

    @PostMapping("/copiar-leccion-anterior")
    public String copiarLeccionAnterior(@RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        String dia = DIA_ES.get(fecha.getDayOfWeek());
        var lecciones = alcanceDocenteService.leccionesVisibles(institucionId, nivelId, materiaId, dia, docenteId);
        Integer leccionOrigen = leccionAnterior(lecciones, numeroLeccion);
        if (leccionOrigen == null) {
            model.addAttribute("error", "No hay una lección anterior de la que copiar.");
        } else {
            Long registradoPorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
            int copiados = service.copiarDeLeccionAnterior(institucionId, nivelId, materiaId, fecha, leccionOrigen,
                    numeroLeccion, registradoPorId);
            if (copiados == 0) {
                model.addAttribute("error", "No hay asistencia registrada en la lección anterior para copiar.");
            } else {
                notificarPromedioDesactualizado(response);
            }
        }
        cargarPanel(model, institucionId, nivelId, materiaId, numeroLeccion, fecha, docenteId);
        return "gestion-academica/asistencia/asistencia :: content";
    }

    private void notificarPromedioDesactualizado(HttpServletResponse response) {
        response.setHeader("HX-Trigger", "promedioDesactualizado");
    }

    private static final Map<DayOfWeek, String> DIA_ES = Map.of(
            DayOfWeek.MONDAY, "LUNES",
            DayOfWeek.TUESDAY, "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY, "JUEVES",
            DayOfWeek.FRIDAY, "VIERNES",
            DayOfWeek.SATURDAY, "SABADO",
            DayOfWeek.SUNDAY, "DOMINGO");

    private void cargarPanel(Model model, Long institucionId, Long nivelId, Long materiaId, Integer numeroLeccion,
            LocalDate fecha, Long docenteId) {
        var secciones = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        if (nivelId == null && !secciones.isEmpty()) {
            nivelId = secciones.get(0).getId();
        }

        // Las materias disponibles dependen de la sección: solo las que el horario asigna ahí.
        var materias = alcanceDocenteService.materiasVisiblesEnNivel(institucionId, nivelId, docenteId);
        Long materiaSolicitada = materiaId;
        if (materiaSolicitada == null || materias.stream().noneMatch(m -> m.getId().equals(materiaSolicitada))) {
            materiaId = materias.isEmpty() ? null : materias.get(0).getId();
        }

        if (fecha == null) {
            fecha = LocalDate.now();
        }
        String dia = DIA_ES.get(fecha.getDayOfWeek());

        // Las lecciones disponibles dependen de la sección, la materia y el día de la semana de la fecha elegida.
        var lecciones = alcanceDocenteService.leccionesVisibles(institucionId, nivelId, materiaId, dia, docenteId);
        if (numeroLeccion == null || !lecciones.contains(numeroLeccion)) {
            numeroLeccion = lecciones.isEmpty() ? null : lecciones.get(0);
        }

        // Botón "copiar de la lección anterior": solo aparece si la lección justo antes (numeroLeccion - 1)
        // es también de esta materia, en esta sección, ese día (dos lecciones seguidas).
        Integer leccionAnterior = leccionAnterior(lecciones, numeroLeccion);

        model.addAttribute("secciones", secciones);
        model.addAttribute("materias", materias);
        model.addAttribute("lecciones", lecciones);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("numeroLeccion", numeroLeccion);
        model.addAttribute("leccionAnterior", leccionAnterior);
        model.addAttribute("fecha", fecha);
        model.addAttribute("filas", nivelId != null && materiaId != null && numeroLeccion != null
                ? service.listarFilas(institucionId, nivelId, fecha, materiaId, numeroLeccion)
                : List.of());
    }

    /**
     * La lección {@code actual - 1}, solo si esa lección consecutiva también existe para esta
     * materia/sección/día (es decir, son dos lecciones seguidas de la misma materia). Si la lección
     * previa del horario no es consecutiva (p. ej. materia en Lección 1 y luego en Lección 5), no aplica.
     */
    private Integer leccionAnterior(List<Integer> lecciones, Integer actual) {
        if (actual == null) {
            return null;
        }
        int anterior = actual - 1;
        return lecciones.contains(anterior) ? anterior : null;
    }

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = institucionId(session);
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }

    private void exigirDocenteODirectorOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden registrar asistencia");
        }
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
