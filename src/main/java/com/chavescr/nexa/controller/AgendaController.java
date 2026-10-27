package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chavescr.nexa.entity.ActividadInstitucionalPropia;
import com.chavescr.nexa.entity.PlanTrabajo;
import com.chavescr.nexa.entity.Recordatorio;
import com.chavescr.nexa.entity.TareaPlanTrabajo;
import com.chavescr.nexa.service.ActividadInstitucionalService;
import com.chavescr.nexa.service.CalendarioService;
import com.chavescr.nexa.service.ParticipacionService;
import com.chavescr.nexa.service.PlanTrabajoService;
import com.chavescr.nexa.service.RecordatorioService;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private ParticipacionService participacionService;

    @Autowired
    private PlanTrabajoService planTrabajoService;

    @Autowired
    private RecordatorioService recordatorioService;

    @Autowired
    private ActividadInstitucionalService actividadInstitucionalService;

    @Autowired
    private CalendarioService calendarioService;

    // ─── CALENDARIO / TAREAS / ACTIVIDAD / RECORDATORIOS ───────

    @GetMapping("/calendario")
    public String calendario(@RequestParam(required = false) String vista,
            @RequestParam(required = false) String fecha,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);

        String vistaActual = (vista == null || vista.isBlank()) ? "semana" : vista;
        java.time.LocalDate referencia = parsearFecha(fecha);

        switch (vistaActual) {
            case "semana" -> model.addAttribute("semana",
                    calendarioService.construirSemana(direccionId, usuarioId, referencia));
            case "dia" -> model.addAttribute("dia",
                    calendarioService.construirDia(direccionId, usuarioId, referencia));
            case "agenda" -> model.addAttribute("agendaDias",
                    calendarioService.construirAgenda(direccionId, usuarioId, referencia));
            default -> model.addAttribute("semanas",
                    calendarioService.construirMes(direccionId, usuarioId, referencia));
        }

        model.addAttribute("miniSemanas", calendarioService.construirMesSimple(referencia));
        model.addAttribute("miniLabel", capitalizar(
                referencia.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es"))
                        + " " + referencia.getYear()));
        model.addAttribute("miniAnterior", referencia.minusMonths(1).toString());
        model.addAttribute("miniSiguiente", referencia.plusMonths(1).toString());

        model.addAttribute("vista", vistaActual);
        model.addAttribute("fechaRef", referencia.toString());
        model.addAttribute("tituloLabel", construirTituloLabel(vistaActual, referencia));
        model.addAttribute("hoyIso", java.time.LocalDate.now().toString());
        model.addAttribute("fechaAnterior", calcularAdyacente(vistaActual, referencia, -1).toString());
        model.addAttribute("fechaSiguiente", calcularAdyacente(vistaActual, referencia, 1).toString());

        return "agenda/calendario/calendario :: content";
    }

    private java.time.LocalDate parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return java.time.LocalDate.now();
        try {
            return java.time.LocalDate.parse(fecha);
        } catch (java.time.format.DateTimeParseException e) {
            return java.time.LocalDate.now();
        }
    }

    private java.time.LocalDate calcularAdyacente(String vista, java.time.LocalDate referencia, int direccion) {
        return switch (vista) {
            case "semana" -> referencia.plusWeeks(direccion);
            case "dia" -> referencia.plusDays(direccion);
            default -> referencia.plusMonths(direccion);
        };
    }

    private String construirTituloLabel(String vista, java.time.LocalDate referencia) {
        java.util.Locale locale = new java.util.Locale("es");
        switch (vista) {
            case "semana" -> {
                java.time.LocalDate inicio = referencia.with(java.time.DayOfWeek.MONDAY);
                java.time.LocalDate fin = referencia.with(java.time.DayOfWeek.SUNDAY);
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", locale);
                return capitalizar(inicio.format(fmt)) + " – " + capitalizar(fin.format(fmt)) + " " + fin.getYear();
            }
            case "dia" -> {
                String diaSemana = referencia.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, locale);
                String mes = referencia.getMonth().getDisplayName(java.time.format.TextStyle.FULL, locale);
                return capitalizar(diaSemana) + " " + referencia.getDayOfMonth() + " de " + mes + " de "
                        + referencia.getYear();
            }
            default -> {
                String mes = referencia.getMonth().getDisplayName(java.time.format.TextStyle.FULL, locale);
                return capitalizar(mes) + " " + referencia.getYear();
            }
        }
    }

    private String capitalizar(String texto) {
        return texto == null || texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    @GetMapping("/tareas")
    public String tareas(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("tareas", planTrabajoService.listarTareasDireccion(direccionId));
        return "agenda/tareas/tareas :: content";
    }

    @GetMapping("/tareas/lista")
    public String tareasLista(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("tareas", planTrabajoService.listarTareasDireccion(direccionId));
        return "agenda/tareas/tareas :: tabla-tareas";
    }

    @GetMapping("/reuniones")
    public String reuniones(HttpSession session) {
        requerirDireccion(session);
        return "agenda/reuniones/reuniones :: content";
    }

    @GetMapping("/reuniones/form")
    public String reunionesForm(HttpSession session) {
        requerirDireccion(session);
        return "agenda/reuniones/formulario :: form-content";
    }

    @GetMapping("/tareas/form")
    public String tareaFormCrear(@RequestParam(required = false) Long miembroId,
                                 Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("tarea", new com.chavescr.nexa.entity.TareaPlanTrabajo());
        model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
        model.addAttribute("personal", planTrabajoService.listarPersonalActivo(direccionId));
        if (miembroId != null) {
            model.addAttribute("miembroLock", planTrabajoService.obtenerMiembro(direccionId, miembroId));
        }
        return "agenda/tareas/formulario :: form-content";
    }

    @GetMapping("/tareas/form/{id}")
    public String tareaFormEditar(@PathVariable Long id, @RequestParam(required = false) Long miembroId,
                                  Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("tarea", planTrabajoService.obtenerTareaDireccion(direccionId, id));
        model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
        model.addAttribute("personal", planTrabajoService.listarPersonalActivo(direccionId));
        if (miembroId != null) {
            model.addAttribute("miembroLock", planTrabajoService.obtenerMiembro(direccionId, miembroId));
        }
        return "agenda/tareas/formulario :: form-content";
    }

    @GetMapping("/tareas/miembros")
    public String tareasMiembrosDePlanTrabajo(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("personal", planTrabajoService.listarPersonalActivo(direccionId));
        return "agenda/tareas/miembros-options :: options";
    }

    @PostMapping("/tareas")
    public String tareaGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String asignarA,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaLimite,
            @RequestParam(required = false) com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea estado,
            @RequestParam(required = false) com.chavescr.nexa.entity.TareaPlanTrabajo.PrioridadTarea prioridad,
            Model model, HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            Long planTrabajoId = null;
            Long usuarioId = null;
            Long miembroId = null;
            if (asignarA != null && !asignarA.isBlank()) {
                if (asignarA.startsWith("p_")) planTrabajoId = Long.parseLong(asignarA.substring(2));
                else if (asignarA.startsWith("u_")) usuarioId = Long.parseLong(asignarA.substring(2));
                else if (asignarA.startsWith("m_")) miembroId = Long.parseLong(asignarA.substring(2));
            }
            planTrabajoService.guardarTareaGeneral(direccionId, id, planTrabajoId, usuarioId, miembroId,
                    titulo, descripcion, fechaLimite, estado, prioridad);
            model.addAttribute("tareas", planTrabajoService.listarTareasDireccion(direccionId));
            return "agenda/tareas/tareas :: tabla-tareas";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tarea", new com.chavescr.nexa.entity.TareaPlanTrabajo());
            model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
            model.addAttribute("personal", planTrabajoService.listarPersonalActivo(direccionId));
            return "agenda/tareas/formulario :: form-content";
        }
    }

    @DeleteMapping("/tareas/{id}")
    public String tareaEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        planTrabajoService.eliminarTareaGeneral(direccionId, id);
        model.addAttribute("tareas", planTrabajoService.listarTareasDireccion(direccionId));
        return "agenda/tareas/tareas :: tabla-tareas";
    }

    @PutMapping("/tareas/{id}/estado")
    public String tareaEstado(@PathVariable Long id,
            @RequestParam com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea estado,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        com.chavescr.nexa.entity.TareaPlanTrabajo t = planTrabajoService.obtenerTareaDireccion(direccionId, id);
        t.setEstado(estado);
        if (estado == com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea.FINALIZADO)
            t.setFechaCompletada(java.time.LocalDateTime.now());
        else t.setFechaCompletada(null);
        planTrabajoService.cambiarEstadoTarea(t.getPlanTrabajo().getId(), id, estado);
        model.addAttribute("tareas", planTrabajoService.listarTareasDireccion(direccionId));
        return "agenda/tareas/tareas :: tabla-tareas";
    }

    @GetMapping("/actividad")
    public String actividad(Model model, HttpSession session, HttpServletRequest request) {
        cargarActividadesInstitucion(model, requerirDireccion(session), null, null, null, request);
        return "agenda/actividad/actividad :: content";
    }

    @GetMapping("/actividad/institucion")
    public String actividadInstitucion(@RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String filtro,
            Model model, HttpSession session, HttpServletRequest request) {
        cargarActividadesInstitucion(model, requerirDireccion(session), mes, categoria, filtro, request);
        if ("actividad-institucion-tabla".equals(request.getHeader("HX-Target"))) {
            return "agenda/actividad/institucion/institucion :: tabla-con-contador";
        }
        return "agenda/actividad/institucion/institucion :: content";
    }

    @GetMapping("/actividad/mep")
    public String actividadMep(@RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String filtro,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirDirectorOAdmin(request);
        cargarActividadesMep(model, requerirDireccion(session), mes, categoria, filtro, request);
        if ("actividad-mep-tabla".equals(request.getHeader("HX-Target"))) {
            return "agenda/actividad/mep/mep :: tabla-con-contador";
        }
        return "agenda/actividad/mep/mep :: content";
    }

    @GetMapping("/actividad/mep/{id}/titulo")
    public String actividadMepFormTitulo(@PathVariable String id, @RequestParam(defaultValue = "mep") String origen,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirDirectorOAdmin(request);
        model.addAttribute("evento", actividadInstitucionalService.obtenerEvento(requerirDireccion(session), id)
                .orElseThrow(() -> new IllegalArgumentException("La actividad del MEP no existe")));
        model.addAttribute("origen", origen);
        return "agenda/actividad/mep/titulo :: form-content";
    }

    /** Agrega la actividad del MEP al calendario de la institución, o cambia su título si ya estaba. */
    @PostMapping("/actividad/mep/{id}/seleccion")
    public String actividadMepSeleccionar(@PathVariable String id,
            @RequestParam(required = false) String titulo,
            @RequestParam(defaultValue = "mep") String origen,
            Model model, HttpSession session, HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        exigirDirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        try {
            actividadInstitucionalService.seleccionarEventoMep(direccionId, requerirUsuario(session), id, titulo);
        } catch (IllegalArgumentException e) {
            response.setHeader("HX-Retarget", "#actividad-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("evento", actividadInstitucionalService.obtenerEvento(direccionId, id).orElse(null));
            model.addAttribute("origen", origen);
            return "agenda/actividad/mep/titulo :: form-content";
        }
        if ("institucion".equals(origen)) {
            cargarActividadesInstitucion(model, direccionId, null, null, null, request);
            return "agenda/actividad/institucion/institucion :: content";
        }
        cargarActividadesMep(model, direccionId, null, null, null, request);
        return "agenda/actividad/mep/mep :: content";
    }

    @DeleteMapping("/actividad/mep/{id}/seleccion")
    public String actividadMepQuitar(@PathVariable String id,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String filtro,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirDirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        actividadInstitucionalService.quitarEventoMep(direccionId, id);
        // Se puede quitar desde la tabla de la institución o desde la del MEP.
        if ("actividad-institucion-tabla".equals(request.getHeader("HX-Target"))) {
            cargarActividadesInstitucion(model, direccionId, mes, categoria, filtro, request);
            return "agenda/actividad/institucion/institucion :: tabla-con-contador";
        }
        cargarActividadesMep(model, direccionId, mes, categoria, filtro, request);
        return "agenda/actividad/mep/mep :: tabla-con-contador";
    }

    @GetMapping("/actividad/detalle/{id}")
    public String actividadDetalle(@PathVariable String id, Model model, HttpSession session,
            HttpServletRequest request) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("evento", actividadInstitucionalService.obtenerEvento(direccionId, id).orElse(null));
        model.addAttribute("puedeCrearActividad", esDirectorOAdmin(request));
        return "agenda/actividad/actividad :: detalle-modal";
    }

    @GetMapping("/actividad/form")
    public String actividadFormCrear(Model model, HttpServletRequest request) {
        exigirDirectorOAdmin(request);
        model.addAttribute("actividad", new ActividadInstitucionalPropia());
        return "agenda/actividad/formulario :: form-content";
    }

    @GetMapping("/actividad/form/{id}")
    public String actividadFormEditar(@PathVariable Long id, Model model, HttpSession session,
            HttpServletRequest request) {
        exigirDirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("actividad", actividadInstitucionalService.obtenerActividadPropiaEntidad(direccionId, id));
        return "agenda/actividad/formulario :: form-content";
    }

    @PostMapping("/actividad")
    public String actividadGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String enlace,
            Model model, HttpSession session, HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        exigirDirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        try {
            actividadInstitucionalService.guardarActividadPropia(direccionId, usuarioId, id,
                    titulo, descripcion, fechaInicio, fechaFin, categoria, enlace);
            cargarActividadesInstitucion(model, direccionId, null, null, null, request);
            return "agenda/actividad/actividad :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#actividad-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            ActividadInstitucionalPropia actividad = new ActividadInstitucionalPropia();
            actividad.setId(id);
            actividad.setTitulo(titulo);
            actividad.setDescripcion(descripcion);
            actividad.setFechaInicio(fechaInicio);
            actividad.setFechaFin(fechaFin);
            actividad.setCategoria(categoria);
            actividad.setEnlace(enlace);
            model.addAttribute("actividad", actividad);
            return "agenda/actividad/formulario :: form-content";
        }
    }

    @DeleteMapping("/actividad/{id}")
    public String actividadEliminar(@PathVariable Long id, Model model, HttpSession session,
            HttpServletRequest request) {
        exigirDirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        actividadInstitucionalService.eliminarActividadPropia(direccionId, id);
        cargarActividadesInstitucion(model, direccionId, null, null, null, request);
        return "agenda/actividad/actividad :: content";
    }

    @GetMapping("/recordatorios")
    public String recordatorios(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(direccionId, usuarioId));
        return "agenda/recordatorio/recordatorios :: content";
    }

    @GetMapping("/recordatorios/form")
    public String recordatorioFormCrear(Model model) {
        model.addAttribute("recordatorio", new Recordatorio());
        return "agenda/recordatorio/formulario :: form-content";
    }

    @GetMapping("/recordatorios/form/{id}")
    public String recordatorioFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        model.addAttribute("recordatorio", recordatorioService.obtenerRecordatorio(direccionId, usuarioId, id));
        return "agenda/recordatorio/formulario :: form-content";
    }

    @PostMapping("/recordatorios")
    public String recordatorioGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime fechaLimite,
            @RequestParam(required = false) Recordatorio.EstadoRecordatorio estado,
            Model model, HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        try {
            recordatorioService.guardarRecordatorio(direccionId, usuarioId, id, titulo, descripcion, fechaLimite, estado);
            model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(direccionId, usuarioId));
            return "agenda/recordatorio/recordatorios :: tabla-recordatorios";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#recordatorios-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            Recordatorio recordatorio = new Recordatorio();
            recordatorio.setId(id);
            recordatorio.setTitulo(titulo);
            recordatorio.setDescripcion(descripcion);
            recordatorio.setFechaLimite(fechaLimite);
            recordatorio.setEstado(estado);
            model.addAttribute("recordatorio", recordatorio);
            return "agenda/recordatorio/formulario :: form-content";
        }
    }

    @DeleteMapping("/recordatorios/{id}")
    public String recordatorioEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        recordatorioService.eliminarRecordatorio(direccionId, usuarioId, id);
        model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(direccionId, usuarioId));
        return "agenda/recordatorio/recordatorios :: tabla-recordatorios";
    }

    @PutMapping("/recordatorios/{id}/estado")
    public String recordatorioEstado(@PathVariable Long id,
            @RequestParam Recordatorio.EstadoRecordatorio estado,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Long usuarioId = requerirUsuario(session);
        recordatorioService.cambiarEstadoRecordatorio(direccionId, usuarioId, id, estado);
        model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(direccionId, usuarioId));
        return "agenda/recordatorio/recordatorios :: tabla-recordatorios";
    }

    // ─── PARTICIPACIÓN ─────────────────────────────────────────

    @GetMapping("/participacion")
    public String participacion(@RequestParam(required = false) String filtro,
                                @RequestParam(required = false) String rol,
                                @RequestParam(required = false) String estado,
                                Model model, HttpSession session, HttpServletRequest request) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("participantes", participacionService.listarParticipantes(direccionId, filtro, rol, estado));
        model.addAttribute("filtro", filtro);
        model.addAttribute("rolSel", rol);
        model.addAttribute("estadoSel", estado);
        if ("part-tabla-container".equals(request.getHeader("HX-Target"))) {
            return "agenda/participacion/contenido :: tabla";
        }
        return "agenda/participacion/contenido :: content";
    }

    @GetMapping("/participacion/detalle/{usuarioId}")
    public String participacionDetalle(@PathVariable Long usuarioId, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("detalle", participacionService.obtenerDetalle(direccionId, usuarioId));
        return "agenda/participacion/detalle :: contenido";
    }

    // ─── PLAN DE TRABAJO ─────────────────────────────────────────────

    @GetMapping("/plan-trabajo")
    public String planesTrabajo(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
        return "agenda/plan-trabajo/contenido :: content";
    }

    @GetMapping("/plan-trabajo/form")
    public String planTrabajoFormCrear(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("planTrabajo", new PlanTrabajo());
        model.addAttribute("personal", planTrabajoService.listarPersonalActivo(direccionId));
        model.addAttribute("miembrosIds", java.util.List.of());
        return "agenda/plan-trabajo/formulario :: form-content";
    }

    @GetMapping("/plan-trabajo/form/{id}")
    public String planTrabajoFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("planTrabajo", planTrabajoService.obtenerPlanTrabajo(direccionId, id));
        model.addAttribute("personal", planTrabajoService.listarPersonalActivo(direccionId));
        model.addAttribute("miembrosIds", planTrabajoService.listarIdsMiembros(id));
        return "agenda/plan-trabajo/formulario :: form-content";
    }

    @PostMapping("/plan-trabajo")
    public String planTrabajoGuardar(@ModelAttribute PlanTrabajo datos,
            @RequestParam(required = false) java.util.List<Long> miembroIds,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        PlanTrabajo guardado = planTrabajoService.guardarPlanTrabajo(direccionId, datos);
        planTrabajoService.sincronizarMiembrosFormulario(direccionId, guardado.getId(), miembroIds);
        model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
        return "agenda/plan-trabajo/contenido :: tabla-planes-trabajo";
    }

    @DeleteMapping("/plan-trabajo/{id}")
    public String planTrabajoEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        planTrabajoService.eliminarPlanTrabajo(direccionId, id);
        model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
        return "agenda/plan-trabajo/contenido :: tabla-planes-trabajo";
    }

    @PutMapping("/plan-trabajo/{id}/toggle")
    public String planTrabajoToggleActivo(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        planTrabajoService.toggleActivoPlanTrabajo(direccionId, id);
        model.addAttribute("planesTrabajo", planTrabajoService.listarPlanesTrabajo(direccionId));
        return "agenda/plan-trabajo/contenido :: tabla-planes-trabajo";
    }

    @GetMapping("/plan-trabajo/buscar")
    public String planTrabajoBuscar(@RequestParam String filtro, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("planesTrabajo", planTrabajoService.buscarPlanesTrabajo(direccionId, filtro));
        return "agenda/plan-trabajo/contenido :: tabla-planes-trabajo";
    }

    @GetMapping("/plan-trabajo/{id}/miembros")
    public String planTrabajoMiembros(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("planTrabajo", planTrabajoService.obtenerPlanTrabajo(direccionId, id));
        model.addAttribute("miembros", planTrabajoService.listarMiembros(id));
        model.addAttribute("usuariosDisponibles", planTrabajoService.listarUsuariosDisponibles(direccionId, id));
        return "agenda/plan-trabajo/miembros :: contenido";
    }

    @PostMapping("/plan-trabajo/{id}/miembros")
    public String planTrabajoAgregarMiembro(@PathVariable Long id,
                                         @RequestParam Long usuarioId,
                                         @RequestParam(defaultValue = "MIEMBRO") String rol,
                                         Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        planTrabajoService.agregarMiembro(direccionId, id, usuarioId, rol);
        model.addAttribute("planTrabajo", planTrabajoService.obtenerPlanTrabajo(direccionId, id));
        model.addAttribute("miembros", planTrabajoService.listarMiembros(id));
        model.addAttribute("usuariosDisponibles", planTrabajoService.listarUsuariosDisponibles(direccionId, id));
        return "agenda/plan-trabajo/miembros :: contenido";
    }

    @DeleteMapping("/plan-trabajo/{id}/miembros/{miembroId}")
    public String planTrabajoEliminarMiembro(@PathVariable Long id, @PathVariable Long miembroId,
                                          Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        planTrabajoService.eliminarMiembro(direccionId, id, miembroId);
        model.addAttribute("planTrabajo", planTrabajoService.obtenerPlanTrabajo(direccionId, id));
        model.addAttribute("miembros", planTrabajoService.listarMiembros(id));
        model.addAttribute("usuariosDisponibles", planTrabajoService.listarUsuariosDisponibles(direccionId, id));
        return "agenda/plan-trabajo/miembros :: contenido";
    }

    @GetMapping("/plan-trabajo/{id}/dashboard")
    public String planTrabajoDashboard(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("dashboard", planTrabajoService.obtenerDashboard(direccionId, id));
        model.addAttribute("planTrabajoId", id);
        return "agenda/plan-trabajo/dashboard :: contenido";
    }

    @GetMapping("/plan-trabajo/miembros/{miembroId}/tareas")
    public String planTrabajoTareasMiembro(@PathVariable Long miembroId,
                                        @RequestParam Long planTrabajoId, Model model) {
        model.addAttribute("tareas", planTrabajoService.listarTareasDeMiembro(miembroId));
        model.addAttribute("miembroId", miembroId);
        model.addAttribute("planTrabajoId", planTrabajoId);
        return "agenda/plan-trabajo/tareas :: tabla-tareas";
    }

    @DeleteMapping("/plan-trabajo/tareas/{tareaId}")
    public String planTrabajoEliminarTarea(@PathVariable Long tareaId,
                                        @RequestParam Long planTrabajoId,
                                        @RequestParam Long miembroId, Model model) {
        planTrabajoService.eliminarTarea(planTrabajoId, tareaId);
        model.addAttribute("tareas", planTrabajoService.listarTareasDeMiembro(miembroId));
        model.addAttribute("miembroId", miembroId);
        return "agenda/plan-trabajo/tareas :: tabla-tareas";
    }

    // ─── HELPERS ───────────────────────────────────────────────

    private Long direccionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_DIRECCION_ID");
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = direccionId(session);
        if (id == null) throw new DireccionNoSeleccionadaException();
        return id;
    }

    private Long requerirUsuario(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_USUARIO_ID");
        if (id == null) throw new IllegalArgumentException("No hay usuario en sesión");
        return id;
    }

    private void cargarActividadesInstitucion(Model model, Long direccionId, Integer mes, String categoria,
            String filtro, HttpServletRequest request) {
        model.addAttribute("eventos", actividadInstitucionalService.buscarEventosInstitucion(direccionId, mes, categoria, filtro));
        model.addAttribute("categorias", actividadInstitucionalService.listarCategoriasInstitucion(direccionId));
        cargarFiltrosActividad(model, mes, categoria, filtro, request);
    }

    private void cargarActividadesMep(Model model, Long direccionId, Integer mes, String categoria,
            String filtro, HttpServletRequest request) {
        model.addAttribute("eventos", actividadInstitucionalService.buscarEventosMep(direccionId, mes, categoria, filtro));
        model.addAttribute("categorias", actividadInstitucionalService.listarCategoriasMep());
        cargarFiltrosActividad(model, mes, categoria, filtro, request);
    }

    private void cargarFiltrosActividad(Model model, Integer mes, String categoria, String filtro,
            HttpServletRequest request) {
        model.addAttribute("meses", ActividadInstitucionalService.MESES);
        model.addAttribute("mesSel", mes);
        model.addAttribute("categoriaSel", categoria);
        model.addAttribute("filtro", filtro);
        model.addAttribute("puedeCrearActividad", esDirectorOAdmin(request));
    }

    private boolean esDirectorOAdmin(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR");
    }

    private void exigirDirectorOAdmin(HttpServletRequest request) {
        if (!esDirectorOAdmin(request)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo Director o Admin pueden crear o modificar actividades institucionales");
        }
    }
}
