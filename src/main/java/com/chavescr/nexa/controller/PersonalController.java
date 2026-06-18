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

import com.chavescr.nexa.entity.RegimenDisciplinario;
import com.chavescr.nexa.service.RegimenDisciplinarioService;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/personal")
public class PersonalController {

    @Autowired
    private RegimenDisciplinarioService regimenService;

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

    // ─── RÉGIMEN DISCIPLINARIO ──────────────────────────────────

    @GetMapping("/regimen")
    public String regimen(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/index :: content";
    }

    @GetMapping("/regimen/todos")
    public String regimenTodos(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        return "personal/regimen/todos/todos :: content";
    }

    @GetMapping("/regimen/llamadas")
    public String regimenLlamadas(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("registros", regimenService.listarPorTipo(institucionId,
                RegimenDisciplinario.TipoRegimen.LLAMADA_ATENCION));
        return "personal/regimen/llamadas/llamadas :: content";
    }

    @GetMapping("/regimen/amonestaciones")
    public String regimenAmonestaciones(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("registros", regimenService.listarPorTipo(institucionId,
                RegimenDisciplinario.TipoRegimen.AMONESTACION));
        return "personal/regimen/amonestaciones/amonestaciones :: content";
    }

    @GetMapping("/regimen/procesos")
    public String regimenProcesos(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        model.addAttribute("registros", regimenService.listarPorTipo(institucionId,
                RegimenDisciplinario.TipoRegimen.PROCESO_DISCIPLINARIO));
        return "personal/regimen/procesos/procesos :: content";
    }

    @GetMapping("/regimen/form")
    public String regimenFormCrear(Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        model.addAttribute("registro", new RegimenDisciplinario());
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/formulario :: form-content";
    }

    @GetMapping("/regimen/form/{id}")
    public String regimenFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("registro", regimenService.obtenerPorId(institucionId, id));
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/formulario :: form-content";
    }

    @PostMapping("/regimen")
    public String regimenGuardar(@ModelAttribute RegimenDisciplinario datos,
                                  Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        regimenService.guardar(institucionId, datos);
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        model.addAttribute("tipoActivo", "TODOS");
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/todos/todos :: content";
    }

    @DeleteMapping("/regimen/{id}")
    public String regimenEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        regimenService.eliminar(institucionId, id);
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        return "personal/regimen/todos/todos :: content";
    }

    @PutMapping("/regimen/{id}/estado")
    public String regimenCambiarEstado(@PathVariable Long id,
                                        @RequestParam String estado,
                                        Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        regimenService.cambiarEstado(institucionId, id, RegimenDisciplinario.EstadoRegimen.valueOf(estado));
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        return "personal/regimen/todos/todos :: content";
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
