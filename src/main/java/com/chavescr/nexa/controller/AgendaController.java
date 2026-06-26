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
import com.chavescr.nexa.entity.TareaProyecto;
import com.chavescr.nexa.service.ParticipacionService;
import com.chavescr.nexa.service.ProyectoService;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    @Autowired
    private ParticipacionService participacionService;

    @Autowired
    private ProyectoService proyectoService;

    // ─── CALENDARIO / TAREAS / ACTIVIDAD / RECORDATORIOS ───────

    @GetMapping("/calendario")
    public String calendario() {
        return "agenda/calendario/calendario :: content";
    }

    @GetMapping("/tareas")
    public String tareas() {
        return "agenda/tareas/tareas :: content";
    }

    @GetMapping("/actividad")
    public String actividad() {
        return "agenda/actividad/actividad :: content";
    }

    @GetMapping("/recordatorios")
    public String recordatorios() {
        return "agenda/recordatorio/recordatorios :: content";
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
}
