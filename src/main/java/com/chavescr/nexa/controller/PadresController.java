package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.List;
import java.util.Set;

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
import com.chavescr.nexa.service.PersonalService;

@Controller
@RequestMapping("/padres")
public class PadresController {

    private static final String ROL_PADRE = "ROLE_PADRE";

    @Autowired
    private PersonalService personalService;

    // ─── PADRES REGISTRADOS ──────────────────────────────────────

    @GetMapping("/registrados")
    public String registrados(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("padres", personalService.listarPorRol(institucionId, ROL_PADRE));
        return "gestion-padres/registrados/directorio :: content";
    }

    @GetMapping("/registrados/lista")
    public String registradosLista(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("padres", personalService.listarPorRol(institucionId, ROL_PADRE));
        return "gestion-padres/registrados/lista :: content";
    }

    @GetMapping("/registrados/form")
    public String registradosFormCrear(Model model) {
        Usuario nuevo = new Usuario();
        nuevo.setRoles(Set.of(personalService.obtenerRolPorNombre(ROL_PADRE)));
        model.addAttribute("usuario", nuevo);
        model.addAttribute("roles", personalService.listarRoles());
        return "gestion-padres/registrados/formulario :: form-content";
    }

    @GetMapping("/registrados/form/{id}")
    public String registradosFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("usuario", personalService.obtenerPorId(institucionId, id));
        model.addAttribute("roles", personalService.listarRoles());
        return "gestion-padres/registrados/formulario :: form-content";
    }

    @PostMapping("/registrados")
    public String registradosGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String usuario,
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestParam(required = false) List<Long> rolIds,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            personalService.guardar(institucionId, id, nombre, email, usuario, cedula, password, activo, rolIds);
            model.addAttribute("padres", personalService.listarPorRol(institucionId, ROL_PADRE));
            return "gestion-padres/registrados/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#padres-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", id == null ? new Usuario() : personalService.obtenerPorId(institucionId, id));
            model.addAttribute("roles", personalService.listarRoles());
            return "gestion-padres/registrados/formulario :: form-content";
        }
    }

    @DeleteMapping("/registrados/{id}")
    public String registradosEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.eliminar(institucionId, id);
        model.addAttribute("padres", personalService.listarPorRol(institucionId, ROL_PADRE));
        return "gestion-padres/registrados/lista :: content";
    }

    @PutMapping("/registrados/{id}/activo")
    public String registradosToggleActivo(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.toggleActivo(institucionId, id);
        model.addAttribute("padres", personalService.listarPorRol(institucionId, ROL_PADRE));
        return "gestion-padres/registrados/lista :: content";
    }

    // ─── HELPERS ───────────────────────────────────────────────

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = institucionId(session);
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }
}
