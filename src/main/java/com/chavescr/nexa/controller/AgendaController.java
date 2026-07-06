package com.chavescr.nexa.controller;

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

import com.chavescr.nexa.entity.Proyecto;
import com.chavescr.nexa.entity.Recordatorio;
import com.chavescr.nexa.entity.TareaProyecto;
import com.chavescr.nexa.service.ActividadInstitucionalService;
import com.chavescr.nexa.service.CalendarioService;
import com.chavescr.nexa.service.ParticipacionService;
import com.chavescr.nexa.service.ProyectoService;
import com.chavescr.nexa.service.RecordatorioService;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private ParticipacionService participacionService;

    @Autowired
    private ProyectoService proyectoService;

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
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = requerirUsuario(session);

        String vistaActual = (vista == null || vista.isBlank()) ? "semana" : vista;
        java.time.LocalDate referencia = parsearFecha(fecha);

        switch (vistaActual) {
            case "semana" -> model.addAttribute("semana",
                    calendarioService.construirSemana(institucionId, usuarioId, referencia));
            case "dia" -> model.addAttribute("dia",
                    calendarioService.construirDia(institucionId, usuarioId, referencia));
            case "agenda" -> model.addAttribute("agendaDias",
                    calendarioService.construirAgenda(institucionId, usuarioId, referencia));
            default -> model.addAttribute("semanas",
                    calendarioService.construirMes(institucionId, usuarioId, referencia));
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
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tareas", proyectoService.listarTareasInstitucion(institucionId));
        return "agenda/tareas/tareas :: content";
    }

    @GetMapping("/tareas/lista")
    public String tareasLista(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tareas", proyectoService.listarTareasInstitucion(institucionId));
        return "agenda/tareas/tareas :: tabla-tareas";
    }

    @GetMapping("/tareas/form")
    public String tareaFormCrear(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tarea", new com.chavescr.nexa.entity.TareaProyecto());
        model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
        model.addAttribute("personal", proyectoService.listarPersonalActivo(institucionId));
        return "agenda/tareas/formulario :: form-content";
    }

    @GetMapping("/tareas/form/{id}")
    public String tareaFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("tarea", proyectoService.obtenerTareaInstitucion(institucionId, id));
        model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
        model.addAttribute("personal", proyectoService.listarPersonalActivo(institucionId));
        return "agenda/tareas/formulario :: form-content";
    }

    @GetMapping("/tareas/miembros")
    public String tareasMiembrosDeProyecto(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("personal", proyectoService.listarPersonalActivo(institucionId));
        return "agenda/tareas/miembros-options :: options";
    }

    @PostMapping("/tareas")
    public String tareaGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String asignarA,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaLimite,
            @RequestParam(required = false) com.chavescr.nexa.entity.TareaProyecto.EstadoTarea estado,
            @RequestParam(required = false) com.chavescr.nexa.entity.TareaProyecto.PrioridadTarea prioridad,
            Model model, HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            Long proyectoId = null;
            Long usuarioId = null;
            if (asignarA != null && !asignarA.isBlank()) {
                if (asignarA.startsWith("p_")) proyectoId = Long.parseLong(asignarA.substring(2));
                else if (asignarA.startsWith("u_")) usuarioId = Long.parseLong(asignarA.substring(2));
            }
            proyectoService.guardarTareaGeneral(institucionId, id, proyectoId, usuarioId,
                    titulo, descripcion, fechaLimite, estado, prioridad);
            model.addAttribute("tareas", proyectoService.listarTareasInstitucion(institucionId));
            return "agenda/tareas/tareas :: tabla-tareas";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#tareas-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("tarea", new com.chavescr.nexa.entity.TareaProyecto());
            model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
            model.addAttribute("personal", proyectoService.listarPersonalActivo(institucionId));
            return "agenda/tareas/formulario :: form-content";
        }
    }

    @DeleteMapping("/tareas/{id}")
    public String tareaEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        proyectoService.eliminarTareaGeneral(institucionId, id);
        model.addAttribute("tareas", proyectoService.listarTareasInstitucion(institucionId));
        return "agenda/tareas/tareas :: tabla-tareas";
    }

    @PutMapping("/tareas/{id}/estado")
    public String tareaEstado(@PathVariable Long id,
            @RequestParam com.chavescr.nexa.entity.TareaProyecto.EstadoTarea estado,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        com.chavescr.nexa.entity.TareaProyecto t = proyectoService.obtenerTareaInstitucion(institucionId, id);
        t.setEstado(estado);
        if (estado == com.chavescr.nexa.entity.TareaProyecto.EstadoTarea.COMPLETADA)
            t.setFechaCompletada(java.time.LocalDateTime.now());
        else t.setFechaCompletada(null);
        proyectoService.cambiarEstadoTarea(t.getProyecto().getId(), id, estado);
        model.addAttribute("tareas", proyectoService.listarTareasInstitucion(institucionId));
        return "agenda/tareas/tareas :: tabla-tareas";
    }

    @GetMapping("/actividad")
    public String actividad(@RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String filtro,
            Model model) {
        model.addAttribute("eventos", actividadInstitucionalService.buscarEventos(mes, categoria, filtro));
        model.addAttribute("categorias", actividadInstitucionalService.listarCategorias());
        model.addAttribute("meses", com.chavescr.nexa.service.ActividadInstitucionalService.MESES);
        model.addAttribute("mesSel", mes);
        model.addAttribute("categoriaSel", categoria);
        model.addAttribute("filtro", filtro);
        return "agenda/actividad/actividad :: content";
    }

    @GetMapping("/actividad/detalle/{id}")
    public String actividadDetalle(@PathVariable String id, Model model) {
        model.addAttribute("evento", actividadInstitucionalService.obtenerEvento(id).orElse(null));
        return "agenda/actividad/actividad :: detalle-modal";
    }

    @GetMapping("/recordatorios")
    public String recordatorios(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = requerirUsuario(session);
        model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(institucionId, usuarioId));
        return "agenda/recordatorio/recordatorios :: content";
    }

    @GetMapping("/recordatorios/form")
    public String recordatorioFormCrear(Model model) {
        model.addAttribute("recordatorio", new Recordatorio());
        return "agenda/recordatorio/formulario :: form-content";
    }

    @GetMapping("/recordatorios/form/{id}")
    public String recordatorioFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = requerirUsuario(session);
        model.addAttribute("recordatorio", recordatorioService.obtenerRecordatorio(institucionId, usuarioId, id));
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
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = requerirUsuario(session);
        try {
            recordatorioService.guardarRecordatorio(institucionId, usuarioId, id, titulo, descripcion, fechaLimite, estado);
            model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(institucionId, usuarioId));
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
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = requerirUsuario(session);
        recordatorioService.eliminarRecordatorio(institucionId, usuarioId, id);
        model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(institucionId, usuarioId));
        return "agenda/recordatorio/recordatorios :: tabla-recordatorios";
    }

    @PutMapping("/recordatorios/{id}/estado")
    public String recordatorioEstado(@PathVariable Long id,
            @RequestParam Recordatorio.EstadoRecordatorio estado,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Long usuarioId = requerirUsuario(session);
        recordatorioService.cambiarEstadoRecordatorio(institucionId, usuarioId, id, estado);
        model.addAttribute("recordatorios", recordatorioService.listarRecordatorios(institucionId, usuarioId));
        return "agenda/recordatorio/recordatorios :: tabla-recordatorios";
    }

    // ─── PARTICIPACIÓN ─────────────────────────────────────────

    @GetMapping("/participacion")
    public String participacion(@RequestParam(required = false) String filtro,
                                @RequestParam(required = false) String rol,
                                @RequestParam(required = false) String estado,
                                Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("participantes", participacionService.listarParticipantes(institucionId, filtro, rol, estado));
        model.addAttribute("filtro", filtro);
        model.addAttribute("rolSel", rol);
        model.addAttribute("estadoSel", estado);
        return "agenda/participacion/contenido :: content";
    }

    @GetMapping("/participacion/detalle/{usuarioId}")
    public String participacionDetalle(@PathVariable Long usuarioId, Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("detalle", participacionService.obtenerDetalle(institucionId, usuarioId));
        return "agenda/participacion/detalle :: contenido";
    }

    // ─── PROYECTOS ─────────────────────────────────────────────

    @GetMapping("/proyectos")
    public String proyectos(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
        return "agenda/proyecto/contenido :: content";
    }

    @GetMapping("/proyectos/form")
    public String proyectoFormCrear(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("personal", proyectoService.listarPersonalActivo(institucionId));
        model.addAttribute("miembrosIds", java.util.List.of());
        return "agenda/proyecto/formulario :: form-content";
    }

    @GetMapping("/proyectos/form/{id}")
    public String proyectoFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("proyecto", proyectoService.obtenerProyecto(institucionId, id));
        model.addAttribute("personal", proyectoService.listarPersonalActivo(institucionId));
        model.addAttribute("miembrosIds", proyectoService.listarIdsMiembros(id));
        return "agenda/proyecto/formulario :: form-content";
    }

    @PostMapping("/proyectos")
    public String proyectoGuardar(@ModelAttribute Proyecto datos,
            @RequestParam(required = false) java.util.List<Long> miembroIds,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Proyecto guardado = proyectoService.guardarProyecto(institucionId, datos);
        proyectoService.sincronizarMiembrosFormulario(institucionId, guardado.getId(), miembroIds);
        model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
        return "agenda/proyecto/contenido :: tabla-proyectos";
    }

    @DeleteMapping("/proyectos/{id}")
    public String proyectoEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        proyectoService.eliminarProyecto(institucionId, id);
        model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
        return "agenda/proyecto/contenido :: tabla-proyectos";
    }

    @PutMapping("/proyectos/{id}/toggle")
    public String proyectoToggleActivo(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        proyectoService.toggleActivoProyecto(institucionId, id);
        model.addAttribute("proyectos", proyectoService.listarProyectos(institucionId));
        return "agenda/proyecto/contenido :: tabla-proyectos";
    }

    @GetMapping("/proyectos/buscar")
    public String proyectoBuscar(@RequestParam String filtro, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("proyectos", proyectoService.buscarProyectos(institucionId, filtro));
        return "agenda/proyecto/contenido :: tabla-proyectos";
    }

    @GetMapping("/proyectos/{id}/miembros")
    public String proyectoMiembros(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("proyecto", proyectoService.obtenerProyecto(institucionId, id));
        model.addAttribute("miembros", proyectoService.listarMiembros(id));
        model.addAttribute("usuariosDisponibles", proyectoService.listarUsuariosDisponibles(institucionId, id));
        return "agenda/proyecto/miembros :: contenido";
    }

    @PostMapping("/proyectos/{id}/miembros")
    public String proyectoAgregarMiembro(@PathVariable Long id,
                                         @RequestParam Long usuarioId,
                                         @RequestParam(defaultValue = "MIEMBRO") String rol,
                                         Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        proyectoService.agregarMiembro(institucionId, id, usuarioId, rol);
        model.addAttribute("proyecto", proyectoService.obtenerProyecto(institucionId, id));
        model.addAttribute("miembros", proyectoService.listarMiembros(id));
        model.addAttribute("usuariosDisponibles", proyectoService.listarUsuariosDisponibles(institucionId, id));
        return "agenda/proyecto/miembros :: contenido";
    }

    @DeleteMapping("/proyectos/{id}/miembros/{miembroId}")
    public String proyectoEliminarMiembro(@PathVariable Long id, @PathVariable Long miembroId,
                                          Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        proyectoService.eliminarMiembro(institucionId, id, miembroId);
        model.addAttribute("proyecto", proyectoService.obtenerProyecto(institucionId, id));
        model.addAttribute("miembros", proyectoService.listarMiembros(id));
        model.addAttribute("usuariosDisponibles", proyectoService.listarUsuariosDisponibles(institucionId, id));
        return "agenda/proyecto/miembros :: contenido";
    }

    @GetMapping("/proyectos/{id}/dashboard")
    public String proyectoDashboard(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("dashboard", proyectoService.obtenerDashboard(institucionId, id));
        model.addAttribute("proyectoId", id);
        return "agenda/proyecto/dashboard :: contenido";
    }

    @GetMapping("/proyectos/miembros/{miembroId}/tareas")
    public String proyectoTareasMiembro(@PathVariable Long miembroId,
                                        @RequestParam Long proyectoId, Model model) {
        model.addAttribute("tareas", proyectoService.listarTareasDeMiembro(miembroId));
        model.addAttribute("miembroId", miembroId);
        model.addAttribute("proyectoId", proyectoId);
        return "agenda/proyecto/tareas :: tabla-tareas";
    }

    @GetMapping("/proyectos/miembros/{miembroId}/tareas/form")
    public String proyectoFormTarea(@PathVariable Long miembroId,
                                    @RequestParam Long proyectoId, Model model) {
        model.addAttribute("tarea", new TareaProyecto());
        model.addAttribute("miembroId", miembroId);
        model.addAttribute("proyectoId", proyectoId);
        return "agenda/proyecto/tarea-form :: form-content";
    }

    @GetMapping("/proyectos/miembros/{miembroId}/tareas/form/{tareaId}")
    public String proyectoFormEditarTarea(@PathVariable Long miembroId,
                                          @PathVariable Long tareaId,
                                          @RequestParam Long proyectoId, Model model) {
        model.addAttribute("tarea", proyectoService.obtenerTarea(proyectoId, tareaId));
        model.addAttribute("miembroId", miembroId);
        model.addAttribute("proyectoId", proyectoId);
        return "agenda/proyecto/tarea-form :: form-content";
    }

    @PostMapping("/proyectos/miembros/{miembroId}/tareas")
    public String proyectoGuardarTarea(@PathVariable Long miembroId,
                                       @RequestParam Long proyectoId,
                                       @ModelAttribute TareaProyecto datos, Model model) {
        proyectoService.guardarTarea(proyectoId, miembroId, datos);
        model.addAttribute("tareas", proyectoService.listarTareasDeMiembro(miembroId));
        model.addAttribute("miembroId", miembroId);
        return "agenda/proyecto/tareas :: tabla-tareas";
    }

    @PutMapping("/proyectos/tareas/{tareaId}/estado")
    @ResponseBody
    public String proyectoCambiarEstadoTarea(@PathVariable Long tareaId,
                                             @RequestParam Long proyectoId,
                                             @RequestParam String estado) {
        TareaProyecto.EstadoTarea nuevoEstado = TareaProyecto.EstadoTarea.valueOf(estado.toUpperCase());
        proyectoService.cambiarEstadoTarea(proyectoId, tareaId, nuevoEstado);
        return "ok";
    }

    @DeleteMapping("/proyectos/tareas/{tareaId}")
    public String proyectoEliminarTarea(@PathVariable Long tareaId,
                                        @RequestParam Long proyectoId,
                                        @RequestParam Long miembroId, Model model) {
        proyectoService.eliminarTarea(proyectoId, tareaId);
        model.addAttribute("tareas", proyectoService.listarTareasDeMiembro(miembroId));
        model.addAttribute("miembroId", miembroId);
        return "agenda/proyecto/tareas :: tabla-tareas";
    }

    // ─── HELPERS ───────────────────────────────────────────────

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = institucionId(session);
        if (id == null) throw new IllegalArgumentException("No hay institución seleccionada");
        return id;
    }

    private Long requerirUsuario(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_USUARIO_ID");
        if (id == null) throw new IllegalArgumentException("No hay usuario en sesión");
        return id;
    }
}
