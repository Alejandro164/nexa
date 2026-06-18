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
    public String regimen(@RequestParam(required = false) String tipo,
                          Model model, HttpSession session) {
        Long institucionId = institucionId(session);
        if (institucionId == null) return "redirect:/";
        if (tipo != null && !tipo.isBlank()) {
            model.addAttribute("registros", regimenService.listarPorTipo(institucionId,
                    RegimenDisciplinario.TipoRegimen.valueOf(tipo)));
        } else {
            model.addAttribute("registros", regimenService.listarTodos(institucionId));
        }
        model.addAttribute("tipoActivo", tipo != null ? tipo : "TODOS");
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/regimen :: content";
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
        return "personal/regimen/regimen :: content";
    }

    @DeleteMapping("/regimen/{id}")
    public String regimenEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        regimenService.eliminar(institucionId, id);
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        model.addAttribute("tipoActivo", "TODOS");
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/regimen :: content";
    }

    @PutMapping("/regimen/{id}/estado")
    public String regimenCambiarEstado(@PathVariable Long id,
                                        @RequestParam String estado,
                                        Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        regimenService.cambiarEstado(institucionId, id, RegimenDisciplinario.EstadoRegimen.valueOf(estado));
        model.addAttribute("registros", regimenService.listarTodos(institucionId));
        model.addAttribute("tipoActivo", "TODOS");
        model.addAttribute("funcionarios", regimenService.listarFuncionarios(institucionId));
        return "personal/regimen/tabla :: tabla";
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
