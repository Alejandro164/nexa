package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.ResponseBody;

import com.chavescr.nexa.dto.HaciendaContribuyenteDTO;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.HaciendaService;
import com.chavescr.nexa.service.PersonalService;

@Controller
@RequestMapping("/estudiantes")
public class EstudiantesController {

    private static final String ROL_ESTUDIANTE = "ROLE_ESTUDIANTE";
    private static final String ROL_PADRE = "ROLE_PADRE";

    @Autowired
    private PersonalService personalService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NivelAcademicoRepository nivelAcademicoRepository;

    @Autowired
    private HaciendaService haciendaService;

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
        model.addAttribute("niveles", nivelAcademicoRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId));
        return "estudiantes/expedientes/formulario :: form-content";
    }

    @GetMapping("/expedientes/form/{id}")
    public String expedientesFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("usuario", personalService.obtenerPorId(institucionId, id));
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
            @RequestParam(required = false) Long nivelId,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            // El rol de una cuenta creada desde este módulo siempre es Estudiante — no lo elige el admin
            // (a diferencia de Personal, que sí permite cualquier combinación de roles).
            List<Long> rolIds = List.of(personalService.obtenerRolPorNombre(ROL_ESTUDIANTE).getId());
            personalService.guardar(institucionId, id, nombre, email, usuario, cedula, password, activo, rolIds, nivelId);
            model.addAttribute("estudiantes", personalService.listarPorRol(institucionId, ROL_ESTUDIANTE));
            return "estudiantes/expedientes/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#estudiantes-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", id == null ? new Usuario() : personalService.obtenerPorId(institucionId, id));
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

    // ─── PESTAÑA PADRES (dentro del modal de crear/editar estudiante) ──

    @GetMapping("/expedientes/{id}/padres")
    public String padresTab(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("estudiante", personalService.obtenerPorId(institucionId, id));
        model.addAttribute("padresVinculados", personalService.listarPadresDe(institucionId, id));
        return "estudiantes/expedientes/padres-tab :: content";
    }

    @GetMapping("/buscar-padre")
    @ResponseBody
    public Map<String, Object> buscarPadre(@RequestParam String cedula, HttpSession session) {
        Map<String, Object> resultado = new HashMap<>();
        Long institucionId = institucionId(session);
        Optional<Usuario> padreOpt = institucionId == null ? Optional.empty()
                : usuarioRepository.findPadreByCedulaAndInstitucionId(cedula.trim(), institucionId);
        if (padreOpt.isPresent()) {
            Usuario padre = padreOpt.get();
            resultado.put("encontrado", true);
            resultado.put("padreId", padre.getId());
            resultado.put("nombre", padre.getNombre());
        } else {
            resultado.put("encontrado", false);
        }
        return resultado;
    }

    @GetMapping("/buscar-hacienda")
    @ResponseBody
    public Map<String, Object> buscarHacienda(@RequestParam String identificacion) {
        Map<String, Object> resultado = new HashMap<>();
        Optional<HaciendaContribuyenteDTO> contribuyente = haciendaService.consultar(identificacion);
        if (contribuyente.isPresent()) {
            resultado.put("encontrado", true);
            resultado.put("nombre", contribuyente.get().getNombre());
        } else {
            resultado.put("encontrado", false);
        }
        return resultado;
    }

    @PostMapping("/expedientes/{id}/padres/vincular")
    public String vincularPadre(@PathVariable Long id, @RequestParam Long padreId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.vincularPadre(institucionId, id, padreId);
        return padresTab(id, model, session);
    }

    @PostMapping("/expedientes/{id}/padres/crear")
    public String crearYVincularPadre(@PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam String usuario,
            @RequestParam(required = false) String cedula,
            @RequestParam String password,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        try {
            Long rolPadreId = personalService.obtenerRolPorNombre(ROL_PADRE).getId();
            Usuario nuevoPadre = personalService.guardar(institucionId, null, nombre, email, usuario, cedula,
                    password, true, List.of(rolPadreId));
            personalService.vincularPadre(institucionId, id, nuevoPadre.getId());
            return padresTab(id, model, session);
        } catch (Exception e) {
            model.addAttribute("errorPadre", e.getMessage());
            model.addAttribute("estudiante", personalService.obtenerPorId(institucionId, id));
            model.addAttribute("padresVinculados", personalService.listarPadresDe(institucionId, id));
            return "estudiantes/expedientes/padres-tab :: content";
        }
    }

    @DeleteMapping("/expedientes/{id}/padres/{padreId}")
    public String desvincularPadre(@PathVariable Long id, @PathVariable Long padreId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.desvincularPadre(institucionId, id, padreId);
        return padresTab(id, model, session);
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
