package com.chavescr.nexa.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.ConfiguracionAcademicaService;
import com.chavescr.nexa.service.DocenteBloqueoService;
import com.chavescr.nexa.service.DocenteGuiaService;
import com.chavescr.nexa.service.DocenteMateriaService;
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

    @Autowired
    private DocenteMateriaService docenteMateriaService;

    @Autowired
    private DocenteGuiaService docenteGuiaService;

    @Autowired
    private DocenteBloqueoService docenteBloqueoService;

    @GetMapping
    public String docentes(Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            cargarDirectorio(model, institucionId, null);
            cargarDisponibilidad(model, institucionId, null, null);
            cargarAsignaciones(model, institucionId, null, null);
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "docentes/index :: htmx-content";
        }
        return "docentes/index";
    }

    // ─── DIRECTORIO ──────────────────────────────────────────────

    @GetMapping("/directorio")
    public String directorio(@RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String materiaId,
            Model model, HttpSession session) {
        cargarDirectorio(model, institucionId(session), q, estado, parseId(materiaId));
        return "docentes/directorio/directorio :: content";
    }

    @GetMapping("/directorio/lista")
    public String directorioLista(@RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String materiaId,
            Model model, HttpSession session) {
        cargarDirectorio(model, institucionId(session), q, estado, parseId(materiaId));
        return "docentes/directorio/lista :: content";
    }

    @GetMapping("/directorio/ficha/{id}")
    public String directorioFicha(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Usuario docente = personalService.obtenerPorId(institucionId, id);
        model.addAttribute("docente", docente);
        model.addAttribute("materiasAsignadas", docenteMateriaService.listarMaterias(institucionId, id));
        model.addAttribute("materiasHorario", alcanceDocenteService.materiasVisibles(institucionId, id));
        model.addAttribute("seccionesGuia", docenteGuiaService.listarSecciones(institucionId, id));
        model.addAttribute("niveles", alcanceDocenteService.nivelesVisibles(institucionId, id));
        return "docentes/directorio/ficha :: modal";
    }

    @GetMapping("/directorio/form")
    public String directorioFormCrear(Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Usuario nuevo = new Usuario();
        nuevo.setRoles(java.util.Set.of(personalService.obtenerRolPorNombre(ROL_DOCENTE)));
        cargarFormulario(model, institucionId, nuevo, List.of(), false, List.of());
        return "docentes/directorio/formulario :: form-content";
    }

    @GetMapping("/directorio/form/{id}")
    public String directorioFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        cargarFormulario(model, institucionId, personalService.obtenerPorId(institucionId, id), null, null, null);
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
            @RequestParam(required = false) List<Long> materiaIds,
            @RequestParam(defaultValue = "false") boolean profesorGuia,
            @RequestParam(required = false) List<Long> nivelGuiaIds,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String materiaId,
            Model model, HttpSession session, HttpServletResponse response) {
        Long institucionId = requerirInstitucion(session);
        try {
            // El rol de una cuenta creada desde este módulo siempre es Docente — no lo elige el admin
            // (a diferencia de Personal, que sí permite cualquier combinación de roles).
            List<Long> rolIds = List.of(personalService.obtenerRolPorNombre(ROL_DOCENTE).getId());
            Usuario guardado = personalService.guardar(
                    institucionId, id, nombre, email, usuario, cedula, password, activo, rolIds);
            docenteMateriaService.reemplazar(institucionId, guardado.getId(), materiaIds);
            docenteGuiaService.reemplazar(institucionId, guardado.getId(), profesorGuia, nivelGuiaIds);
            cargarDirectorio(model, institucionId, q, estado, parseId(materiaId));
            return "docentes/directorio/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#docentes-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            Usuario formUsuario = id == null ? new Usuario() : personalService.obtenerPorId(institucionId, id);
            cargarFormulario(model, institucionId, formUsuario,
                    materiaIds == null ? List.of() : materiaIds,
                    profesorGuia,
                    nivelGuiaIds == null ? List.of() : nivelGuiaIds);
            return "docentes/directorio/formulario :: form-content";
        }
    }

    @DeleteMapping("/directorio/{id}")
    public String directorioEliminar(@PathVariable Long id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.eliminar(institucionId, id);
        cargarDirectorio(model, institucionId, q, estado, parseId(materiaId));
        return "docentes/directorio/lista :: content";
    }

    @PutMapping("/directorio/{id}/activo")
    public String directorioToggleActivo(@PathVariable Long id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String materiaId,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        personalService.toggleActivo(institucionId, id);
        cargarDirectorio(model, institucionId, q, estado, parseId(materiaId));
        return "docentes/directorio/lista :: content";
    }

    // ─── DISPONIBILIDAD ──────────────────────────────────────────

    @GetMapping("/disponibilidad")
    public String disponibilidad(@RequestParam(required = false) Long docenteId,
            @RequestParam(required = false) Long periodoId, Model model, HttpSession session) {
        cargarDisponibilidad(model, institucionId(session), docenteId, periodoId);
        return "docentes/disponibilidad/disponibilidad :: disponibilidad-panel";
    }

    @PostMapping("/disponibilidad/bloqueo")
    public String alternarBloqueo(@RequestParam Long docenteId,
            @RequestParam Long periodoId,
            @RequestParam String dia,
            @RequestParam Integer numeroLeccion,
            Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        docenteBloqueoService.alternar(institucionId, docenteId, periodoId, dia, numeroLeccion);
        cargarDisponibilidad(model, institucionId, docenteId, periodoId);
        return "docentes/disponibilidad/disponibilidad :: disponibilidad-panel";
    }

    @GetMapping("/disponibilidad/reporte")
    public String reporteDisponibilidad(@RequestParam Long docenteId,
            @RequestParam Long periodoId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        Usuario docente = personalService.obtenerPorId(institucionId, docenteId);
        boolean esDocente = docente.getRoles().stream()
                .anyMatch(rol -> ROL_DOCENTE.equals(rol.getNombre()));
        if (!esDocente) {
            throw new IllegalArgumentException("Docente no encontrado");
        }
        PeriodoAcademico periodo = configuracionAcademicaService.obtenerPeriodo(institucionId, periodoId);
        Map<String, List<HorarioLeccion>> horario = configuracionAcademicaService
                .obtenerHorarioPorDocente(institucionId, periodoId, docenteId);
        Set<String> bloqueos = docenteBloqueoService.claves(institucionId, periodoId, docenteId);

        int totalBloques = ConfiguracionAcademicaService.DIAS.size()
                * ConfiguracionAcademicaService.LECCIONES.size();
        int bloquesOcupados = 0;
        int leccionesAsignadas = 0;
        for (List<HorarioLeccion> items : horario.values()) {
            if (items != null && !items.isEmpty()) {
                bloquesOcupados++;
                leccionesAsignadas += items.size();
            }
        }
        int bloquesBloqueados = (int) bloqueos.stream()
                .filter(clave -> {
                    List<HorarioLeccion> items = horario.get(clave);
                    return items == null || items.isEmpty();
                })
                .count();

        model.addAttribute("institucionNombre", session.getAttribute("SESSION_INSTITUCION_NOMBRE"));
        model.addAttribute("docente", docente);
        model.addAttribute("periodo", periodo);
        model.addAttribute("dias", ConfiguracionAcademicaService.DIAS);
        model.addAttribute("lecciones", ConfiguracionAcademicaService.LECCIONES);
        model.addAttribute("horarioDocente", horario);
        model.addAttribute("bloqueos", bloqueos);
        model.addAttribute("bloquesOcupados", bloquesOcupados);
        model.addAttribute("bloquesBloqueados", bloquesBloqueados);
        model.addAttribute("bloquesLibres", totalBloques - bloquesOcupados - bloquesBloqueados);
        model.addAttribute("leccionesAsignadas", leccionesAsignadas);
        model.addAttribute("totalBloques", totalBloques);
        model.addAttribute("fechaGeneracion", LocalDateTime.now());
        return "docentes/disponibilidad/reporte";
    }

    // ─── CARGA LABORAL ───────────────────────────────────────────

    @GetMapping("/asignaciones")
    public String asignaciones(@RequestParam(required = false) Long docenteId,
            @RequestParam(required = false) Long periodoId, Model model, HttpSession session) {
        cargarAsignaciones(model, institucionId(session), periodoId, docenteId);
        return "docentes/asignaciones/asignaciones :: asignaciones-panel";
    }

    @GetMapping("/asignaciones/reporte")
    public String reporteAsignaciones(@RequestParam(required = false) Long docenteId,
            @RequestParam Long periodoId, Model model, HttpSession session) {
        Long institucionId = requerirInstitucion(session);
        PeriodoAcademico periodo = configuracionAcademicaService.obtenerPeriodo(institucionId, periodoId);
        List<Usuario> docentes = personalService.listarPorRol(institucionId, ROL_DOCENTE);
        if (docenteId != null) {
            docentes = docentes.stream().filter(d -> docenteId.equals(d.getId())).toList();
            if (docentes.isEmpty()) {
                throw new IllegalArgumentException("Docente no encontrado");
            }
        }
        List<CargaLaboralDocenteDTO> carga = new ArrayList<>();
        int totalLecciones = 0;
        int docentesConCarga = 0;
        for (Usuario docente : docentes) {
            CargaLaboralDocenteDTO fila = construirCargaLaboral(institucionId, periodoId, docente);
            carga.add(fila);
            totalLecciones += fila.getTotalLecciones();
            if (fila.getTotalLecciones() > 0) {
                docentesConCarga++;
            }
        }
        carga.sort(Comparator.comparing(c -> c.getDocente().getNombre()));

        model.addAttribute("institucionNombre", session.getAttribute("SESSION_INSTITUCION_NOMBRE"));
        model.addAttribute("periodo", periodo);
        model.addAttribute("carga", carga);
        model.addAttribute("docenteFiltro", docenteId == null ? null : docentes.get(0));
        model.addAttribute("totalLecciones", totalLecciones);
        model.addAttribute("docentesConCarga", docentesConCarga);
        model.addAttribute("totalDocentes", carga.size());
        model.addAttribute("fechaGeneracion", LocalDateTime.now());
        return "docentes/asignaciones/reporte";
    }

    // ─── CARGA DE DATOS (compartida entre la carga inicial de /docentes y cada pestaña) ──

    private void cargarDirectorio(Model model, Long institucionId, String q) {
        cargarDirectorio(model, institucionId, q, null, null);
    }

    private void cargarDirectorio(Model model, Long institucionId, String q, String estado, Long materiaId) {
        List<Usuario> docentes = institucionId == null ? List.of()
                : personalService.listarPorRol(institucionId, ROL_DOCENTE, q);
        Map<Long, List<Materia>> materiasPorDocente = institucionId == null ? Map.of()
                : docenteMateriaService.mapearPorDocente(institucionId);
        Map<Long, List<NivelAcademico>> guiasPorDocente = institucionId == null ? Map.of()
                : docenteGuiaService.mapearPorDocente(institucionId);

        if (estado == null) {
            estado = "activo";
        }
        if ("activo".equalsIgnoreCase(estado) || "inactivo".equalsIgnoreCase(estado)) {
            boolean activo = "activo".equalsIgnoreCase(estado);
            docentes = docentes.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getActivo()) == activo)
                    .toList();
        }
        if (materiaId != null) {
            docentes = docentes.stream()
                    .filter(d -> materiasPorDocente.getOrDefault(d.getId(), List.of()).stream()
                            .anyMatch(m -> materiaId.equals(m.getId())))
                    .toList();
        }

        List<Materia> materiasFiltro = institucionId == null ? List.of()
                : configuracionAcademicaService.listarMateriasActivas(institucionId);

        Long periodoActivoId = null;
        if (institucionId != null) {
            List<PeriodoAcademico> periodos = configuracionAcademicaService.listarPeriodosActivos(institucionId);
            if (!periodos.isEmpty()) {
                periodoActivoId = periodos.get(0).getId();
            }
        }
        Map<Long, Long> leccionesPorDocente = institucionId == null ? Map.of()
                : configuracionAcademicaService.contarLeccionesPorDocente(institucionId, periodoActivoId);

        model.addAttribute("docentes", docentes);
        model.addAttribute("materiasPorDocente", materiasPorDocente);
        model.addAttribute("guiasPorDocente", guiasPorDocente);
        model.addAttribute("materiasFiltro", materiasFiltro);
        model.addAttribute("leccionesPorDocente", leccionesPorDocente);
        model.addAttribute("q", q);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("materiaFiltro", materiaId);
    }

    private Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void cargarFormulario(Model model, Long institucionId, Usuario usuario,
            List<Long> materiaIds, Boolean profesorGuia, List<Long> nivelGuiaIds) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("materiasCatalogo",
                docenteMateriaService.catalogoParaFormulario(institucionId, usuario.getId()));
        model.addAttribute("seccionesCatalogo",
                docenteGuiaService.catalogoParaFormulario(institucionId, usuario.getId()));
        List<Long> materiasSeleccionadas = materiaIds != null
                ? materiaIds
                : (usuario.getId() == null
                        ? List.of()
                        : docenteMateriaService.listarMateriaIds(institucionId, usuario.getId()));
        List<Long> nivelesSeleccionados = nivelGuiaIds != null
                ? nivelGuiaIds
                : (usuario.getId() == null
                        ? List.of()
                        : docenteGuiaService.listarNivelIds(institucionId, usuario.getId()));
        boolean esGuia = profesorGuia != null
                ? profesorGuia
                : !nivelesSeleccionados.isEmpty();
        model.addAttribute("materiaIdsSeleccionadas", new HashSet<>(materiasSeleccionadas));
        model.addAttribute("nivelGuiaIdsSeleccionados", new HashSet<>(nivelesSeleccionados));
        model.addAttribute("profesorGuia", esGuia);
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
        Set<String> bloqueos = institucionId == null ? Set.of()
                : docenteBloqueoService.claves(institucionId, periodoId, docenteId);

        model.addAttribute("docentesDisponibilidad", docentes);
        model.addAttribute("periodosActivosDisponibilidad", periodos);
        model.addAttribute("docenteSeleccionado", docenteId);
        model.addAttribute("periodoSeleccionadoDisponibilidad", periodoId);
        model.addAttribute("dias", ConfiguracionAcademicaService.DIAS);
        model.addAttribute("lecciones", ConfiguracionAcademicaService.LECCIONES);
        model.addAttribute("horarioDocente", horario);
        model.addAttribute("bloqueos", bloqueos);
    }

    private void cargarAsignaciones(Model model, Long institucionId, Long periodoId, Long docenteId) {
        List<PeriodoAcademico> periodos = institucionId == null ? List.of()
                : configuracionAcademicaService.listarPeriodosActivos(institucionId);
        List<Usuario> docentes = institucionId == null ? List.of()
                : personalService.listarPorRol(institucionId, ROL_DOCENTE);
        if (periodoId == null && !periodos.isEmpty()) {
            periodoId = periodos.get(0).getId();
        }

        List<Usuario> docentesCarga = docentes;
        if (docenteId != null) {
            docentesCarga = docentes.stream().filter(d -> docenteId.equals(d.getId())).toList();
        }

        List<CargaLaboralDocenteDTO> carga = new ArrayList<>();
        for (Usuario docente : docentesCarga) {
            carga.add(construirCargaLaboral(institucionId, periodoId, docente));
        }
        carga.sort(Comparator.comparing(c -> c.getDocente().getNombre()));

        model.addAttribute("docentesAsignaciones", docentes);
        model.addAttribute("docenteSeleccionadoAsignaciones", docenteId);
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
