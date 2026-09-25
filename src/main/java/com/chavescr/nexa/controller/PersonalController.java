package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.access.AccessDeniedException;
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

import com.chavescr.nexa.entity.RegimenDisciplinario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.service.PersonalService;
import com.chavescr.nexa.service.RegimenDisciplinarioService;
import com.chavescr.nexa.service.SolicitudService;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/personal")
public class PersonalController {

    @Autowired
    private RegimenDisciplinarioService regimenService;

    @Autowired
    private PersonalService personalService;

    @Autowired
    private SolicitudService solicitudService;

    @GetMapping("/asistencia")
    public String asistencia() {
        return "personal/asistencia/asistencia :: content";
    }

    @GetMapping("/tareas")
    public String tareas() {
        return "personal/tareas/tareas :: content";
    }

    @GetMapping("/evaluacion")
    public String evaluacion() {
        return "personal/evaluacion/evaluacion :: content";
    }

    @GetMapping("/presencia")
    public String presencia() {
        return "personal/presencia/presencia :: content";
    }

    // ─── DIRECTORIO DE PERSONAL ────────────────────────────────

    @GetMapping("/directorio")
    public String directorio(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("personal", personalService.listarTodos(direccionId));
        return "personal/directorio/directorio :: content";
    }

    @GetMapping("/directorio/lista")
    public String directorioLista(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("personal", personalService.listarTodos(direccionId));
        return "personal/directorio/lista :: content";
    }

    @GetMapping("/directorio/form")
    public String directorioFormCrear(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", personalService.listarRoles());
        return "personal/directorio/formulario :: form-content";
    }

    @GetMapping("/directorio/form/{id}")
    public String directorioFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("usuario", personalService.obtenerPorId(direccionId, id));
        model.addAttribute("roles", personalService.listarRoles());
        return "personal/directorio/formulario :: form-content";
    }

    @PostMapping("/directorio")
    public String directorioGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String usuario,
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestParam(required = false) List<Long> rolIds,
            Model model, HttpSession session,
            jakarta.servlet.http.HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            personalService.guardar(direccionId, id, nombre, email, usuario, cedula, password, activo, rolIds);
            model.addAttribute("personal", personalService.listarTodos(direccionId));
            return "personal/directorio/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#dir-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", id == null ? new Usuario() : personalService.obtenerPorId(direccionId, id));
            model.addAttribute("roles", personalService.listarRoles());
            return "personal/directorio/formulario :: form-content";
        }
    }

    @DeleteMapping("/directorio/{id}")
    public String directorioEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        personalService.eliminar(direccionId, id);
        model.addAttribute("personal", personalService.listarTodos(direccionId));
        return "personal/directorio/lista :: content";
    }

    @PutMapping("/directorio/{id}/activo")
    public String directorioToggleActivo(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        personalService.toggleActivo(direccionId, id);
        model.addAttribute("personal", personalService.listarTodos(direccionId));
        return "personal/directorio/lista :: content";
    }

    // ─── RÉGIMEN DISCIPLINARIO ──────────────────────────────────

    @GetMapping("/regimen")
    public String regimen(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("registros", regimenService.listarTodos(direccionId));
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(direccionId));
        return "personal/regimen/index :: content";
    }

    @GetMapping("/regimen/todos")
    public String regimenTodos(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("registros", regimenService.listarTodos(direccionId));
        return "personal/regimen/todos/todos :: content";
    }

    @GetMapping("/regimen/llamadas")
    public String regimenLlamadas(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("registros", regimenService.listarPorTipo(direccionId,
                RegimenDisciplinario.TipoRegimen.LLAMADA_ATENCION));
        return "personal/regimen/llamadas/llamadas :: content";
    }

    @GetMapping("/regimen/amonestaciones")
    public String regimenAmonestaciones(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("registros", regimenService.listarPorTipo(direccionId,
                RegimenDisciplinario.TipoRegimen.AMONESTACION));
        return "personal/regimen/amonestaciones/amonestaciones :: content";
    }

    @GetMapping("/regimen/procesos")
    public String regimenProcesos(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("registros", regimenService.listarPorTipo(direccionId,
                RegimenDisciplinario.TipoRegimen.PROCESO_DISCIPLINARIO));
        return "personal/regimen/procesos/procesos :: content";
    }

    @GetMapping("/regimen/form")
    public String regimenFormCrear(Model model, HttpSession session) {
        Long direccionId = direccionId(session);
        model.addAttribute("registro", new RegimenDisciplinario());
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(direccionId));
        return "personal/regimen/formulario :: form-content";
    }

    @GetMapping("/regimen/form/{id}")
    public String regimenFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("registro", regimenService.obtenerPorId(direccionId, id));
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(direccionId));
        return "personal/regimen/formulario :: form-content";
    }

    @PostMapping("/regimen")
    public String regimenGuardar(@ModelAttribute RegimenDisciplinario datos,
                                  Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        regimenService.guardar(direccionId, datos);
        model.addAttribute("registros", regimenService.listarTodos(direccionId));
        model.addAttribute("tipoActivo", "TODOS");
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(direccionId));
        return "personal/regimen/todos/todos :: content";
    }

    @DeleteMapping("/regimen/{id}")
    public String regimenEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        regimenService.eliminar(direccionId, id);
        model.addAttribute("registros", regimenService.listarTodos(direccionId));
        return "personal/regimen/todos/todos :: content";
    }

    @PutMapping("/regimen/{id}/estado")
    public String regimenCambiarEstado(@PathVariable Long id,
                                        @RequestParam String estado,
                                        Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        regimenService.cambiarEstado(direccionId, id, RegimenDisciplinario.EstadoRegimen.valueOf(estado));
        model.addAttribute("registros", regimenService.listarTodos(direccionId));
        return "personal/regimen/todos/todos :: content";
    }

    // ─── SOLICITUDES DE PADRES ──────────────────────────────────

    @GetMapping("/solicitudes")
    public String solicitudes(Model model, HttpSession session, HttpServletRequest request) {
        exigirDocenteODirector(request);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("solicitudes", solicitudService.listarPorDireccion(direccionId));
        return "personal/solicitudes/solicitudes :: content";
    }

    @GetMapping("/solicitudes/lista")
    public String solicitudesLista(Model model, HttpSession session, HttpServletRequest request) {
        exigirDocenteODirector(request);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("solicitudes", solicitudService.listarPorDireccion(direccionId));
        return "personal/solicitudes/solicitudes :: tabla-solicitudes";
    }

    @PostMapping("/solicitudes/{id}/en-proceso")
    public String solicitudEnProceso(@PathVariable Long id, Model model, HttpSession session, HttpServletRequest request) {
        exigirDocenteODirector(request);
        solicitudService.marcarEnProceso(id);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("solicitudes", solicitudService.listarPorDireccion(direccionId));
        return "personal/solicitudes/solicitudes :: tabla-solicitudes";
    }

    @PostMapping("/solicitudes/{id}/resolver")
    public String solicitudResolver(@PathVariable Long id, @RequestParam(required = false) String respuesta,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirDocenteODirector(request);
        solicitudService.resolver(id, respuesta);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("solicitudes", solicitudService.listarPorDireccion(direccionId));
        return "personal/solicitudes/solicitudes :: tabla-solicitudes";
    }

    @PostMapping("/solicitudes/{id}/rechazar")
    public String solicitudRechazar(@PathVariable Long id, @RequestParam(required = false) String respuesta,
            Model model, HttpSession session, HttpServletRequest request) {
        exigirDocenteODirector(request);
        solicitudService.rechazar(id, respuesta);
        Long direccionId = requerirDireccion(session);
        model.addAttribute("solicitudes", solicitudService.listarPorDireccion(direccionId));
        return "personal/solicitudes/solicitudes :: tabla-solicitudes";
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

    private void exigirDocenteODirector(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden ver las solicitudes de padres");
        }
    }
}
