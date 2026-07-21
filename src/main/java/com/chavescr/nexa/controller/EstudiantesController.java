package com.chavescr.nexa.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.PersonalService;

@Controller
@RequestMapping("/estudiantes")
public class EstudiantesController {

    private static final String ROL_ESTUDIANTE = "ROLE_ESTUDIANTE";

    @Autowired
    private PersonalService personalService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NivelAcademicoRepository nivelAcademicoRepository;

    // ─── EXPEDIENTES ─────────────────────────────────────────────

    @GetMapping("/expedientes")
    public String expedientes(@RequestParam(required = false) String q, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("estudiantes", personalService.listarPorRol(institucionId, ROL_ESTUDIANTE, q));
        model.addAttribute("q", q);
        return "estudiantes/expedientes/directorio :: content";
    }

    @GetMapping("/expedientes/lista")
    public String expedientesLista(@RequestParam(required = false) String q, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("estudiantes", personalService.listarPorRol(institucionId, ROL_ESTUDIANTE, q));
        model.addAttribute("q", q);
        return "estudiantes/expedientes/lista :: content";
    }

    @GetMapping("/expedientes/ficha/{id}")
    public String expedientesFicha(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Usuario estudiante = personalService.obtenerPorId(institucionId, id);
        model.addAttribute("estudiante", estudiante);
        model.addAttribute("padres", usuarioRepository.findPadresByEstudianteId(id));
        return "estudiantes/expedientes/ficha :: modal";
    }

    @GetMapping("/expedientes/form")
    public String expedientesFormCrear(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Usuario nuevo = new Usuario();
        nuevo.setRoles(java.util.Set.of(personalService.obtenerRolPorNombre(ROL_ESTUDIANTE)));
        model.addAttribute("usuario", nuevo);
        model.addAttribute("roles", personalService.listarRoles());
        model.addAttribute("niveles", nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId));
        return "estudiantes/expedientes/formulario :: form-content";
    }

    @GetMapping("/expedientes/form/{id}")
    public String expedientesFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("usuario", personalService.obtenerPorId(institucionId, id));
        model.addAttribute("roles", personalService.listarRoles());
        model.addAttribute("niveles", nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId));
        return "estudiantes/expedientes/formulario :: form-content";
    }

    @PostMapping("/expedientes")
    public String expedientesGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String usuario,
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestParam(required = false) List<Long> rolIds,
            @RequestParam(required = false) Long nivelId,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            personalService.guardar(institucionId, id, nombre, email, usuario, cedula, password, activo, rolIds, nivelId);
            model.addAttribute("estudiantes", personalService.listarPorRol(institucionId, ROL_ESTUDIANTE));
            return "estudiantes/expedientes/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#estudiantes-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", id == null ? new Usuario() : personalService.obtenerPorId(institucionId, id));
            model.addAttribute("roles", personalService.listarRoles());
            model.addAttribute("niveles", nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId));
            return "estudiantes/expedientes/formulario :: form-content";
        }
    }

    @DeleteMapping("/expedientes/{id}")
    public String expedientesEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.eliminar(institucionId, id);
        model.addAttribute("estudiantes", personalService.listarPorRol(institucionId, ROL_ESTUDIANTE));
        return "estudiantes/expedientes/lista :: content";
    }

    @PutMapping("/expedientes/{id}/activo")
    public String expedientesToggleActivo(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.toggleActivo(institucionId, id);
        model.addAttribute("estudiantes", personalService.listarPorRol(institucionId, ROL_ESTUDIANTE));
        return "estudiantes/expedientes/lista :: content";
    }

    // ─── HELPERS ───────────────────────────────────────────────

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
}
