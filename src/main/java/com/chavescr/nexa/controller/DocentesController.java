package com.chavescr.nexa.controller;

import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.CargaLaboralDocenteDTO;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.ConfiguracionAcademicaService;
import com.chavescr.nexa.service.PersonalService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/docentes")
public class DocentesController {

    private static final String ROL_DOCENTE = "ROLE_DOCENTE";

    @Autowired
    private PersonalService personalService;

    @Autowired
    private AlcanceDocenteService alcanceDocenteService;

    @Autowired
    private ConfiguracionAcademicaService configuracionAcademicaService;

    @Autowired
    private HorarioLeccionRepository horarioLeccionRepository;

    @GetMapping
    public String docentes(Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            cargarDirectorio(model, institucionId, null);
            cargarDisponibilidad(model, institucionId, null, null);
            cargarAsignaciones(model, institucionId, null);
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "docentes/index :: htmx-content";
        }
        return "docentes/index";
    }

    // ─── DIRECTORIO ──────────────────────────────────────────────

    @GetMapping("/directorio")
    public String directorio(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarDirectorio(model, institucionId(session), q);
        return "docentes/directorio/directorio :: content";
    }

    @GetMapping("/directorio/lista")
    public String directorioLista(@RequestParam(required = false) String q, Model model, HttpSession session) {
        cargarDirectorio(model, institucionId(session), q);
        return "docentes/directorio/lista :: content";
    }

    @GetMapping("/directorio/ficha/{id}")
    public String directorioFicha(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Usuario docente = personalService.obtenerPorId(institucionId, id);
        model.addAttribute("docente", docente);
        model.addAttribute("materias", alcanceDocenteService.materiasVisibles(institucionId, id));
        model.addAttribute("niveles", alcanceDocenteService.nivelesVisibles(institucionId, id));
        return "docentes/directorio/ficha :: modal";
    }

    @GetMapping("/directorio/form")
    public String directorioFormCrear(Model model, HttpSession session) {
        requerirInstitucion(session);
        Usuario nuevo = new Usuario();
        nuevo.setRoles(java.util.Set.of(personalService.obtenerRolPorNombre(ROL_DOCENTE)));
        model.addAttribute("usuario", nuevo);
        model.addAttribute("roles", personalService.listarRoles());
        return "docentes/directorio/formulario :: form-content";
    }

    @GetMapping("/directorio/form/{id}")
    public String directorioFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        model.addAttribute("usuario", personalService.obtenerPorId(institucionId, id));
        model.addAttribute("roles", personalService.listarRoles());
        return "docentes/directorio/formulario :: form-content";
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
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            personalService.guardar(institucionId, id, nombre, email, usuario, cedula, password, activo, rolIds);
            cargarDirectorio(model, institucionId, null);
            return "docentes/directorio/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#docentes-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("usuario", id == null ? new Usuario() : personalService.obtenerPorId(institucionId, id));
            model.addAttribute("roles", personalService.listarRoles());
            return "docentes/directorio/formulario :: form-content";
        }
    }

    @DeleteMapping("/directorio/{id}")
    public String directorioEliminar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.eliminar(institucionId, id);
        cargarDirectorio(model, institucionId, null);
        return "docentes/directorio/lista :: content";
    }

    @PutMapping("/directorio/{id}/activo")
    public String directorioToggleActivo(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.toggleActivo(institucionId, id);
        cargarDirectorio(model, institucionId, null);
        return "docentes/directorio/lista :: content";
    }

    // ─── DISPONIBILIDAD ──────────────────────────────────────────

    @GetMapping("/disponibilidad")
    public String disponibilidad(@RequestParam(required = false) Long docenteId,
            @RequestParam(required = false) Long periodoId, Model model, HttpSession session) {
        cargarDisponibilidad(model, institucionId(session), docenteId, periodoId);
        return "docentes/disponibilidad/disponibilidad :: disponibilidad-panel";
    }

    // ─── CARGA LABORAL ───────────────────────────────────────────

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(required = false) Long periodoId, Model model, HttpSession session) {
        cargarAsignaciones(model, institucionId(session), periodoId);
        return "docentes/asignaciones/asignaciones :: asignaciones-panel";
    }

    // ─── CARGA DE DATOS (compartida entre la carga inicial de /docentes y cada pestaña) ──

    private void cargarDirectorio(Model model, Long institucionId, String q) {
        List<Usuario> docentes = institucionId == null ? List.of()
                : personalService.listarPorRol(institucionId, ROL_DOCENTE, q);
        model.addAttribute("docentes", docentes);
        model.addAttribute("q", q);
    }

    private void cargarDisponibilidad(Model model, Long institucionId, Long docenteId, Long periodoId) {
        List<Usuario> docentes = institucionId == null ? List.of()
                : personalService.listarPorRol(institucionId, ROL_DOCENTE);
        List<PeriodoAcademico> periodos = institucionId == null ? List.of()
                : configuracionAcademicaService.listarPeriodosActivos(institucionId);

        if (docenteId == null && !docentes.isEmpty()) {
            docenteId = docentes.get(0).getId();
        }
        if (periodoId == null && !periodos.isEmpty()) {
            periodoId = periodos.get(0).getId();
        }

        Map<String, List<HorarioLeccion>> horario = institucionId == null ? Map.of()
                : configuracionAcademicaService.obtenerHorarioPorDocente(institucionId, periodoId, docenteId);

        model.addAttribute("docentesDisponibilidad", docentes);
        model.addAttribute("periodosActivosDisponibilidad", periodos);
        model.addAttribute("docenteSeleccionado", docenteId);
        model.addAttribute("periodoSeleccionadoDisponibilidad", periodoId);
        model.addAttribute("dias", ConfiguracionAcademicaService.DIAS);
        model.addAttribute("lecciones", ConfiguracionAcademicaService.LECCIONES);
        model.addAttribute("horarioDocente", horario);
    }

    private void cargarAsignaciones(Model model, Long institucionId, Long periodoId) {
        List<PeriodoAcademico> periodos = institucionId == null ? List.of()
                : configuracionAcademicaService.listarPeriodosActivos(institucionId);
        if (periodoId == null && !periodos.isEmpty()) {
            periodoId = periodos.get(0).getId();
        }

        List<CargaLaboralDocenteDTO> carga = new ArrayList<>();
        if (institucionId != null) {
            List<Usuario> docentes = personalService.listarPorRol(institucionId, ROL_DOCENTE);
            for (Usuario docente : docentes) {
                carga.add(construirCargaLaboral(institucionId, periodoId, docente));
            }
            carga.sort(Comparator.comparing(c -> c.getDocente().getNombre()));
        }

        model.addAttribute("periodosActivosAsignaciones", periodos);
        model.addAttribute("periodoSeleccionadoAsignaciones", periodoId);
        model.addAttribute("carga", carga);
    }

    private CargaLaboralDocenteDTO construirCargaLaboral(Long institucionId, Long periodoId, Usuario docente) {
        List<HorarioLeccion> lecciones = periodoId == null ? List.of()
                : horarioLeccionRepository.findByInstitucionIdAndPeriodoIdAndDocenteIdOrderByDiaAscNumeroLeccionAsc(
                        institucionId, periodoId, docente.getId());

        Map<String, Long> conteoPorAsignacion = new LinkedHashMap<>();
        for (HorarioLeccion leccion : lecciones) {
            String clave = leccion.getMateria().getNombre() + " · " + leccion.getNivel().getNombreCompleto();
            conteoPorAsignacion.merge(clave, 1L, Long::sum);
        }
        List<CargaLaboralDocenteDTO.Asignacion> asignaciones = conteoPorAsignacion.entrySet().stream()
                .map(e -> {
                    String[] partes = e.getKey().split(" · ", 2);
                    return new CargaLaboralDocenteDTO.Asignacion(partes[0], partes[1], e.getValue());
                })
                .toList();

        return new CargaLaboralDocenteDTO(docente, lecciones.size(), asignaciones);
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
