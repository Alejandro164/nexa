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
import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;
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
        Long direccionId = direccionId(session);
        if (direccionId != null) {
            cargarEstudiantes(model, direccionId, null);
            cargarPersonal(model, direccionId, null);
            cargarPadres(model, direccionId, null);
            cargarDirecciones(model, direccionId, null);
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "contacto/index :: htmx-content";
        }
        return "contacto/index";
    }

    // ─── ESTUDIANTES ─────────────────────────────────────────────

    @GetMapping("/estudiantes")
    public String estudiantes(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarEstudiantes(model, direccionId(session), q);
        return "contacto/estudiantes/estudiantes :: content";
    }

    // ─── PERSONAL ────────────────────────────────────────────────

    @GetMapping("/personal")
    public String personal(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarPersonal(model, direccionId(session), q);
        return "contacto/personal/personal :: content";
    }

    // ─── PADRES ──────────────────────────────────────────────────

    @GetMapping("/padres")
    public String padres(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarPadres(model, direccionId(session), q);
        return "contacto/padres/padres :: content";
    }

    // ─── DIRECCIONES (contactos externos) ─────────────────────

    @GetMapping("/direcciones")
    public String direcciones(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarDirecciones(model, direccionId(session), q);
        return "contacto/direcciones/direcciones :: content";
    }

    @GetMapping("/direcciones/lista")
    public String direccionesLista(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarDirecciones(model, direccionId(session), q);
        return "contacto/direcciones/lista :: content";
    }

    @GetMapping("/direcciones/form")
    public String direccionesFormCrear(Model model, HttpSession session) {
        requerirDireccion(session);
        model.addAttribute("contacto", new ContactoExterno());
        return "contacto/direcciones/formulario :: form-content";
    }

    @GetMapping("/direcciones/form/{id}")
    public String direccionesFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("contacto", contactoExternoService.obtenerPorId(direccionId, id));
        return "contacto/direcciones/formulario :: form-content";
    }

    @PostMapping("/direcciones")
    public String direccionesGuardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam String tipo,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sitioWeb,
            @RequestParam(defaultValue = "true") boolean activo,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            contactoExternoService.guardar(direccionId, id, nombre, tipo, direccion, telefono, email, sitioWeb, activo);
            cargarDirecciones(model, direccionId, null);
            return "contacto/direcciones/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#contacto-direcciones-modal");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            ContactoExterno contacto = new ContactoExterno();
            contacto.setId(id);
            contacto.setNombre(nombre);
            contacto.setTipo(tipo);
            contacto.setDireccionFisica(direccion);
            contacto.setTelefono(telefono);
            contacto.setEmail(email);
            contacto.setSitioWeb(sitioWeb);
            contacto.setActivo(activo);
            model.addAttribute("contacto", contacto);
            return "contacto/direcciones/formulario :: form-content";
        }
    }

    @DeleteMapping("/direcciones/{id}")
    public String direccionesEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        contactoExternoService.eliminar(direccionId, id);
        cargarDirecciones(model, direccionId, null);
        return "contacto/direcciones/lista :: content";
    }

    // ─── CARGA DE DATOS ──────────────────────────────────────────

    private void cargarEstudiantes(Model model, Long direccionId, String q) {
        List<Usuario> estudiantes = direccionId == null ? List.of()
                : personalService.listarPorRol(direccionId, ROL_ESTUDIANTE, q);
        model.addAttribute("estudiantes", estudiantes);
        model.addAttribute("qEstudiantes", q);
    }

    private void cargarPersonal(Model model, Long direccionId, String q) {
        List<Usuario> personal = direccionId == null ? List.of()
                : filtrarPorTexto(usuarioService.obtenerPersonalActivoPorDireccion(direccionId), q);
        model.addAttribute("personal", personal);
        model.addAttribute("qPersonal", q);
    }

    private void cargarPadres(Model model, Long direccionId, String q) {
        List<Usuario> padres = direccionId == null ? List.of()
                : personalService.listarPorRol(direccionId, ROL_PADRE, q);
        Map<Long, List<Usuario>> estudiantesPorPadre = new LinkedHashMap<>();
        for (Usuario padre : padres) {
            estudiantesPorPadre.put(padre.getId(), usuarioRepository.findEstudiantesByPadreId(padre.getId()));
        }
        model.addAttribute("padres", padres);
        model.addAttribute("estudiantesPorPadre", estudiantesPorPadre);
        model.addAttribute("qPadres", q);
    }

    private void cargarDirecciones(Model model, Long direccionId, String q) {
        List<ContactoExterno> direcciones = direccionId == null ? List.of()
                : contactoExternoService.listar(direccionId, q);
        model.addAttribute("direcciones", direcciones);
        model.addAttribute("qDirecciones", q);
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

    private Long direccionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_DIRECCION_ID");
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = direccionId(session);
        if (id == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return id;
    }
}
