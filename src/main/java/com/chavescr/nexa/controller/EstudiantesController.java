package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import java.time.Year;
import java.util.ArrayList;
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
    private static final int TAMANO_PAGINA_DEFECTO = 10;

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
    public String expedientes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        cargarListaExpedientes(model, direccionId, q, campo, periodo, pagina, tamano);
        model.addAttribute("periodos", personalService.listarPeriodosEstudiantes(direccionId));
        return "estudiantes/expedientes/directorio :: content";
    }

    @GetMapping("/expedientes/lista")
    public String expedientesLista(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        cargarListaExpedientes(model, direccionId, q, campo, periodo, pagina, tamano);
        return "estudiantes/expedientes/lista :: content";
    }

    @GetMapping("/expedientes/ficha/{id}")
    public String expedientesFicha(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Usuario estudiante = personalService.obtenerPorId(direccionId, id);
        model.addAttribute("estudiante", estudiante);
        model.addAttribute("padres", usuarioRepository.findPadresByEstudianteId(id));
        return "estudiantes/expedientes/ficha :: modal";
    }

    @GetMapping("/expedientes/form")
    public String expedientesFormCrear(Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        Usuario nuevo = new Usuario();
        nuevo.setRoles(java.util.Set.of(personalService.obtenerRolPorNombre(ROL_ESTUDIANTE)));
        nuevo.setAnioMatricula(Year.now().getValue());
        model.addAttribute("usuario", nuevo);
        model.addAttribute("anioActual", Year.now().getValue());
        model.addAttribute("niveles", nivelAcademicoRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId));
        return "estudiantes/expedientes/formulario :: form-content";
    }

    @GetMapping("/expedientes/form/{id}")
    public String expedientesFormEditar(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("usuario", personalService.obtenerPorId(direccionId, id));
        model.addAttribute("anioActual", Year.now().getValue());
        model.addAttribute("niveles", nivelAcademicoRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId));
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
            @RequestParam(required = false) Integer anioMatricula,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            Model model, HttpSession session, HttpServletResponse response) {
        Long direccionId = requerirDireccion(session);
        try {
            // El rol de una cuenta creada desde este módulo siempre es Estudiante — no lo elige el admin
            // (a diferencia de Personal, que sí permite cualquier combinación de roles).
            List<Long> rolIds = List.of(personalService.obtenerRolPorNombre(ROL_ESTUDIANTE).getId());
            Usuario guardado = personalService.guardar(direccionId, id, nombre, email, usuario, cedula, password,
                    activo, rolIds, nivelId, anioMatricula);
            cargarListaExpedientes(model, direccionId, q, campo, periodo, pagina, tamano);
            if (guardado.getAnioMatricula() != null) {
                response.setHeader("HX-Trigger",
                        "{\"estudianteGuardado\":{\"periodo\":" + guardado.getAnioMatricula() + "}}");
            }
            return "estudiantes/expedientes/lista :: content";
        } catch (Exception e) {
            response.setHeader("HX-Retarget", "#estudiantes-modal-container");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("anioActual", Year.now().getValue());
            model.addAttribute("usuario", id == null ? new Usuario() : personalService.obtenerPorId(direccionId, id));
            model.addAttribute("niveles", nivelAcademicoRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId));
            return "estudiantes/expedientes/formulario :: form-content";
        }
    }

    @DeleteMapping("/expedientes/{id}")
    public String expedientesEliminar(
            @PathVariable Long id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        personalService.eliminar(direccionId, id);
        cargarListaExpedientes(model, direccionId, q, campo, periodo, pagina, tamano);
        return "estudiantes/expedientes/lista :: content";
    }

    @PutMapping("/expedientes/{id}/activo")
    public String expedientesToggleActivo(
            @PathVariable Long id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) Integer periodo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        personalService.toggleActivo(direccionId, id);
        cargarListaExpedientes(model, direccionId, q, campo, periodo, pagina, tamano);
        return "estudiantes/expedientes/lista :: content";
    }

    private void cargarListaExpedientes(Model model, Long direccionId, String q, String campo,
            Integer periodo, Integer paginaSolicitada, Integer tamanoSolicitado) {
        String campoNormalizado = PersonalService.normalizarCampoEstudiante(campo);
        List<Usuario> filtrados = personalService.listarEstudiantes(direccionId, q, campoNormalizado, periodo);
        int total = filtrados.size();
        int tamano = normalizarTamanoPagina(tamanoSolicitado);
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamano));
        int pagina = paginaSolicitada == null ? 1 : Math.max(1, Math.min(paginaSolicitada, totalPaginas));

        int inicio = (pagina - 1) * tamano;
        int fin = Math.min(inicio + tamano, total);
        List<Usuario> paginaEstudiantes = inicio >= total ? List.of() : filtrados.subList(inicio, fin);

        boolean hayFiltros = (q != null && !q.isBlank()) || periodo != null;
        model.addAttribute("estudiantes", paginaEstudiantes);
        model.addAttribute("totalEstudiantes", total);
        model.addAttribute("pagina", pagina);
        model.addAttribute("tamano", tamano);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("desde", total == 0 ? 0 : inicio + 1);
        model.addAttribute("hasta", fin);
        model.addAttribute("paginasVisibles", construirPaginasVisibles(pagina, totalPaginas));
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("campo", campoNormalizado);
        model.addAttribute("periodo", periodo);
        model.addAttribute("hayFiltros", hayFiltros);
    }

    private static int normalizarTamanoPagina(Integer tamano) {
        if (tamano == null) {
            return TAMANO_PAGINA_DEFECTO;
        }
        return switch (tamano) {
            case 10, 25, 50 -> tamano;
            default -> TAMANO_PAGINA_DEFECTO;
        };
    }

    private static List<Integer> construirPaginasVisibles(int actual, int totalPaginas) {
        List<Integer> paginas = new ArrayList<>();
        if (totalPaginas <= 7) {
            for (int i = 1; i <= totalPaginas; i++) {
                paginas.add(i);
            }
            return paginas;
        }
        paginas.add(1);
        if (actual > 3) {
            paginas.add(-1);
        }
        int rangoInicio = Math.max(2, actual - 1);
        int rangoFin = Math.min(totalPaginas - 1, actual + 1);
        for (int i = rangoInicio; i <= rangoFin; i++) {
            paginas.add(i);
        }
        if (actual < totalPaginas - 2) {
            paginas.add(-1);
        }
        paginas.add(totalPaginas);
        return paginas;
    }

    // ─── PESTAÑA PADRES (dentro del modal de crear/editar estudiante) ──

    @GetMapping("/expedientes/{id}/padres")
    public String padresTab(@PathVariable Long id, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        model.addAttribute("estudiante", personalService.obtenerPorId(direccionId, id));
        model.addAttribute("padresVinculados", personalService.listarPadresDe(direccionId, id));
        return "estudiantes/expedientes/padres-tab :: content";
    }

    @GetMapping("/buscar-padre")
    @ResponseBody
    public Map<String, Object> buscarPadre(@RequestParam String cedula, HttpSession session) {
        Map<String, Object> resultado = new HashMap<>();
        Long direccionId = direccionId(session);
        Optional<Usuario> padreOpt = direccionId == null ? Optional.empty()
                : personalService.buscarPadrePorCedula(direccionId, cedula.trim());
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
        Long direccionId = requerirDireccion(session);
        personalService.vincularPadre(direccionId, id, padreId);
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
        Long direccionId = requerirDireccion(session);
        try {
            Long rolPadreId = personalService.obtenerRolPorNombre(ROL_PADRE).getId();
            Usuario nuevoPadre = personalService.guardar(direccionId, null, nombre, email, usuario, cedula,
                    password, true, List.of(rolPadreId));
            personalService.vincularPadre(direccionId, id, nuevoPadre.getId());
            return padresTab(id, model, session);
        } catch (Exception e) {
            model.addAttribute("errorPadre", e.getMessage());
            model.addAttribute("estudiante", personalService.obtenerPorId(direccionId, id));
            model.addAttribute("padresVinculados", personalService.listarPadresDe(direccionId, id));
            return "estudiantes/expedientes/padres-tab :: content";
        }
    }

    @DeleteMapping("/expedientes/{id}/padres/{padreId}")
    public String desvincularPadre(@PathVariable Long id, @PathVariable Long padreId, Model model, HttpSession session) {
        Long direccionId = requerirDireccion(session);
        personalService.desvincularPadre(direccionId, id, padreId);
        return padresTab(id, model, session);
    }

    // ─── HELPERS ───────────────────────────────────────────────

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
