package com.chavescr.nexa.controller;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.entity.ContactoExterno;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.ContactoExternoService;
import com.chavescr.nexa.service.PersonalService;
import com.chavescr.nexa.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/contacto")
public class ContactoController {

    private static final String ROL_ESTUDIANTE = "ROLE_ESTUDIANTE";
    private static final String ROL_PADRE = "ROLE_PADRE";

    @Autowired
    private PersonalService personalService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ContactoExternoService contactoExternoService;

    @GetMapping
    public String contacto(Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = institucionId(session);
        if (institucionId != null) {
            cargarEstudiantes(model, institucionId, null);
            cargarPersonal(model, institucionId, null);
            cargarPadres(model, institucionId, null);
            cargarInstituciones(model, institucionId, null);
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "contacto/index :: htmx-content";
        }
        return "contacto/index";
    }

    // ─── ESTUDIANTES ─────────────────────────────────────────────

    @GetMapping("/estudiantes")
    public String estudiantes(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarEstudiantes(model, institucionId(session), q);
        return "contacto/estudiantes/estudiantes :: content";
    }

    // ─── PERSONAL ────────────────────────────────────────────────

    @GetMapping("/personal")
    public String personal(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarPersonal(model, institucionId(session), q);
        return "contacto/personal/personal :: content";
    }

    // ─── PADRES ──────────────────────────────────────────────────

    @GetMapping("/padres")
    public String padres(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarPadres(model, institucionId(session), q);
        return "contacto/padres/padres :: content";
    }

    // ─── INSTITUCIONES (contactos externos) ─────────────────────

    @GetMapping("/instituciones")
    public String instituciones(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarInstituciones(model, institucionId(session), q);
        return "contacto/instituciones/instituciones :: content";
    }

    @GetMapping("/instituciones/lista")
    public String institucionesLista(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarInstituciones(model, institucionId(session), q);
        return "contacto/instituciones/lista :: content";
    }

    @GetMapping("/instituciones/form")
    public String institucionesFormCrear(Model model, HttpSession session) {
        requerirInstitucion(session);
        model.addAttribute("contacto", new ContactoExterno());
        return "contacto/instituciones/formulario :: form-content";
    }

    @GetMapping("/instituciones/form/{id}")
    public String institucionesFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("contacto", contactoExternoService.obtenerPorId(institucionId, id));
        return "contacto/instituciones/formulario :: form-content";
    }

    @PostMapping("/instituciones")
    public String institucionesGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam String tipo,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sitioWeb,
            @RequestParam(defaultValue = "true") boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            contactoExternoService.guardar(institucionId, id, nombre, tipo, direccion, telefono, email, sitioWeb, activo);
            cargarInstituciones(model, institucionId, null);
            return "contacto/instituciones/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#contacto-instituciones-modal");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            ContactoExterno contacto = new ContactoExterno();
            contacto.setId(id);
            contacto.setNombre(nombre);
            contacto.setTipo(tipo);
            contacto.setDireccion(direccion);
            contacto.setTelefono(telefono);
            contacto.setEmail(email);
            contacto.setSitioWeb(sitioWeb);
            contacto.setActivo(activo);
            model.addAttribute("contacto", contacto);
            return "contacto/instituciones/formulario :: form-content";
        }
    }

    @DeleteMapping("/instituciones/{id}")
    public String institucionesEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        contactoExternoService.eliminar(institucionId, id);
        cargarInstituciones(model, institucionId, null);
        return "contacto/instituciones/lista :: content";
    }

    // ─── CARGA DE DATOS ──────────────────────────────────────────

    private void cargarEstudiantes(Model model, Long institucionId, String q) {
        List<Usuario> estudiantes = institucionId == null ? List.of()
                : personalService.listarPorRol(institucionId, ROL_ESTUDIANTE, q);
        model.addAttribute("estudiantes", estudiantes);
        model.addAttribute("qEstudiantes", q);
    }

    private void cargarPersonal(Model model, Long institucionId, String q) {
        List<Usuario> personal = institucionId == null ? List.of()
                : filtrarPorTexto(usuarioService.obtenerPersonalActivoPorInstitucion(institucionId), q);
        model.addAttribute("personal", personal);
        model.addAttribute("qPersonal", q);
    }

    private void cargarPadres(Model model, Long institucionId, String q) {
        List<Usuario> padres = institucionId == null ? List.of()
                : personalService.listarPorRol(institucionId, ROL_PADRE, q);
        Map<Long, List<Usuario>> estudiantesPorPadre = new LinkedHashMap<>();
        for (Usuario padre : padres) {
            estudiantesPorPadre.put(padre.getId(), usuarioRepository.findEstudiantesByPadreId(padre.getId()));
        }
        model.addAttribute("padres", padres);
        model.addAttribute("estudiantesPorPadre", estudiantesPorPadre);
        model.addAttribute("qPadres", q);
    }

    private void cargarInstituciones(Model model, Long institucionId, String q) {
        List<ContactoExterno> instituciones = institucionId == null ? List.of()
                : contactoExternoService.listar(institucionId, q);
        model.addAttribute("instituciones", instituciones);
        model.addAttribute("qInstituciones", q);
    }

    private List<Usuario> filtrarPorTexto(List<Usuario> usuarios, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return usuarios;
        }
        String f = normalizar(filtro.trim());
        return usuarios.stream()
                .filter(u -> normalizar(u.getNombre()).contains(f)
                        || (u.getCedula() != null && normalizar(u.getCedula()).contains(f)))
                .toList();
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }

    // ─── HELPERS ─────────────────────────────────────────────────

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
