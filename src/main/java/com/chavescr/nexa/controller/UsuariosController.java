package com.chavescr.nexa.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.chavescr.nexa.repository.RolRepository;
import com.chavescr.nexa.service.InstitucionService;
import com.chavescr.nexa.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** CRUD global de usuarios (todas las instituciones) — solo accesible para ROLE_ADMIN sin institución seleccionada. */
@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private InstitucionService institucionService;

    @GetMapping
    public String index(@RequestParam(required = false) String q, Model model, HttpServletRequest request) {
        cargarLista(model, q);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "usuarios/index :: htmx-content";
        }
        return "usuarios/index";
    }

    @GetMapping("/lista")
    public String lista(@RequestParam(required = false) String q, Model model) {
        cargarLista(model, q);
        return "usuarios/lista :: content";
    }

    @GetMapping("/form")
    public String formCrear(Model model) {
        model.addAttribute("usuario", new Usuario());
        cargarOpciones(model);
        return "usuarios/formulario :: form-content";
    }

    @GetMapping("/form/{id}")
    public String formEditar(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.obtenerPorId(id));
        cargarOpciones(model);
        return "usuarios/formulario :: form-content";
    }

    @PostMapping
    public String guardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String usuario,
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "false") boolean activo,
            @RequestParam(required = false) List<Long> rolIds,
            @RequestParam(required = false) List<Long> institucionIds,
            Model model, HttpServletResponse response) {
        try {
            usuarioService.guardar(id, nombre, email, usuario, cedula, password, activo, rolIds, institucionIds);
            cargarLista(model, null);
            return "usuarios/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#usuarios-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", id == null ? new Usuario() : usuarioService.obtenerPorId(id));
            cargarOpciones(model);
            return "usuarios/formulario :: form-content";
        }
    }

    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable Long id, Model model, HttpServletResponse response) {
        try {
            usuarioService.eliminar(id);
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            notificarError(response, "No se puede eliminar: el usuario tiene registros asociados en el sistema.");
        }
        cargarLista(model, null);
        return "usuarios/lista :: content";
    }

    @PutMapping("/{id}/activo")
    public String toggleActivo(@PathVariable Long id, Model model) {
        usuarioService.toggleActivo(id);
        cargarLista(model, null);
        return "usuarios/lista :: content";
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        String mensajeSaneado = mensaje.replace("\"", "'").replaceAll("[\\r\\n]+", " ").trim();
        response.setHeader("HX-Trigger", "{\"usuarioError\":{\"mensaje\":\"" + mensajeSaneado + "\"}}");
    }

    private void cargarLista(Model model, String q) {
        model.addAttribute("usuarios", usuarioService.listarTodosConInstituciones(q));
        model.addAttribute("q", q);
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("instituciones", institucionService.obtenerTodasDTO());
    }
}
