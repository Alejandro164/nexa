package com.chavescr.nexa.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.ConfiguracionAcademicaService;
import com.chavescr.nexa.service.HistorialCambioService;
import com.chavescr.nexa.service.InstitucionService;
import com.chavescr.nexa.service.RegistroAsistenciaService;
import com.chavescr.nexa.service.SesionInstitucionService;
import com.chavescr.nexa.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private InstitucionService institucionService;

    @Autowired
    private SesionInstitucionService sesionInstitucionService;

    @Autowired
    private ConfiguracionAcademicaService configuracionAcademicaService;

    @Autowired
    private RegistroAsistenciaService registroAsistenciaService;

    @Autowired
    private HistorialCambioService historialCambioService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails usuario, Model model,
            HttpServletRequest request, HttpSession session) {

        session.setAttribute("SESSION_USUARIO_ID", usuario.getId());

        var resultado = sesionInstitucionService.resolver(session, request.isUserInRole("ROLE_ADMIN"));
        if (resultado.estado() != SesionInstitucionService.Estado.RESUELTA) {
            // La selección de institución ahora se resuelve en el login (modal por AJAX); si se
            // llega aquí sin institución resuelta (JS deshabilitado, navegación directa a /, etc.)
            // se cierra la sesión y se manda de vuelta al login para que pase por ese flujo.
            session.invalidate();
            return "redirect:/login";
        }

        // El @ModelAttribute global se calculó ANTES de este handler, así que si resolver() acaba
        // de auto-seleccionar institución (efecto secundario del propio resolver) puede haber quedado
        // desactualizado — se recalcula aquí con el estado de sesión ya resuelto.
        model.addAttribute("sinInstitucionAdmin",
                request.isUserInRole("ROLE_ADMIN") && session.getAttribute("SESSION_INSTITUCION_ID") == null);

        cargarDashboard(model, session);
        return "inicio/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(Model model, HttpServletRequest request, HttpSession session) {
        cargarDashboard(model, session);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "inicio/inicio :: htmx-content";
        }
        return "inicio/inicio";
    }

    @GetMapping("/inicio/instituciones-modal")
    public String institucionesModal(@RequestParam(required = false) String origen, Model model,
            HttpServletRequest request, HttpSession session) {
        if (request.isUserInRole("ROLE_ADMIN")) {
            model.addAttribute("instituciones", institucionService.obtenerTodasDTO());
        } else {
            model.addAttribute("instituciones", usuarioService.obtenerInstitucionesDelUsuarioActual());
        }
        model.addAttribute("institucionActualId", session.getAttribute("SESSION_INSTITUCION_ID"));
        if ("login".equals(origen)) {
            return "auth/seleccionar-institucion-modal :: modal-content";
        }
        return "inicio/instituciones-modal :: modal-content";
    }

    @PostMapping("/inicio/cambiar-institucion")
    public void cambiarInstitucion(@RequestParam Long institucionId,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");

        if (request.isUserInRole("ROLE_ADMIN")) {
            institucionService.findById(institucionId).ifPresent(inst -> {
                session.setAttribute("SESSION_INSTITUCION_ID", institucionId);
                session.setAttribute("SESSION_INSTITUCION_NOMBRE", inst.getNombre());
                usuarioService.actualizarUltimaInstitucion(usuarioId, inst);
            });
        } else {
            usuarioService.obtenerInstitucionesDelUsuarioActual().stream()
                    .filter(inst -> inst.getId().equals(institucionId))
                    .findFirst()
                    .ifPresent(inst -> {
                        session.setAttribute("SESSION_INSTITUCION_ID", institucionId);
                        session.setAttribute("SESSION_INSTITUCION_NOMBRE", inst.getNombre());
                        institucionService.findById(institucionId)
                                .ifPresent(entidad -> usuarioService.actualizarUltimaInstitucion(usuarioId, entidad));
                    });
        }

        // Este endpoint se llama tanto por htmx (modal "Cambiar de Institución", con hx-target
        // apuntando al modal) como por un form normal (modal de selección tras login); en el caso
        // htmx un "redirect:" de Spring solo recargaría el contenido DENTRO del modal, dejando el
        // dashboard de fondo con los datos de la institución anterior — por eso se fuerza una
        // recarga completa del navegador vía HX-Redirect en vez de un redirect normal.
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", "/inicio");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        response.sendRedirect("/inicio");
    }

    @PostMapping("/inicio/salir-institucion")
    public void salirInstitucion(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        // Solo ROLE_ADMIN puede operar sin institución seleccionada (ver SesionInstitucionService.resolver).
        if (!request.isUserInRole("ROLE_ADMIN")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        session.removeAttribute("SESSION_INSTITUCION_ID");
        session.removeAttribute("SESSION_INSTITUCION_NOMBRE");
        // Se olvida también la institución recordada: si no, el próximo login la auto-seleccionaría
        // de nuevo (seleccionarRecordada) y "salir" no tendría efecto duradero.
        usuarioService.actualizarUltimaInstitucion(usuarioId, null);

        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", "/inicio");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        response.sendRedirect("/inicio");
    }

    private void cargarDashboard(Model model, HttpSession session) {
        String institucionActivaNombre = (String) session.getAttribute("SESSION_INSTITUCION_NOMBRE");
        model.addAttribute("institucionActivaNombre", institucionActivaNombre);

        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            model.addAttribute("sinInstitucion", true);
            cargarResumenGlobal(model);
            return;
        }

        long totalEstudiantes = usuarioService.contarActivosPorInstitucionYRol(institucionId, "ROLE_ESTUDIANTE");
        long totalDocentes = usuarioService.contarActivosPorInstitucionYRol(institucionId, "ROLE_DOCENTE");

        Map<String, Long> asistenciaHoy = registroAsistenciaService.obtenerConteoPersonalPresente(institucionId);

        List<PeriodoAcademico> periodosActivos = configuracionAcademicaService.listarPeriodosActivos(institucionId);
        PeriodoAcademico periodoActivo = periodosActivos.isEmpty() ? null : periodosActivos.get(0);

        List<NivelAcademico> niveles = configuracionAcademicaService.listarNivelesActivos(institucionId);

        model.addAttribute("totalEstudiantes", totalEstudiantes);
        model.addAttribute("totalDocentes", totalDocentes);
        model.addAttribute("personalPresenteHoy", asistenciaHoy.getOrDefault("presentes", 0L));
        model.addAttribute("totalRegistrosHoy", asistenciaHoy.getOrDefault("totalRegistros", 0L));
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("niveles", niveles);
        model.addAttribute("actividadReciente", historialCambioService.listarRecientes(institucionId));
    }

    /**
     * Panel para el admin sin institución seleccionada: en vez del detalle de una institución
     * (que no aplica aquí), muestra un resumen global del sistema — mismos indicadores que ya
     * existían en el dashboard antes de que este pasara a estar scoped a una institución.
     */
    private void cargarResumenGlobal(Model model) {
        List<UsuarioDTO> usuarios = usuarioService.obtenerTodosDTO();
        List<InstitucionDTO> instituciones = institucionService.obtenerTodasDTO();

        int totalUsuarios = usuarios.size();
        long usuariosActivos = usuarios.stream().filter(UsuarioDTO::isActivo).count();
        int totalInstituciones = instituciones.size();
        long institucionesActivas = instituciones.stream().filter(InstitucionDTO::isActiva).count();
        int porcentajeActivos = totalUsuarios > 0
                ? (int) Math.round((double) usuariosActivos / totalUsuarios * 100)
                : 0;
        long totalRoles = usuarios.stream()
                .flatMap(u -> u.getRoles().stream())
                .distinct()
                .count();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("totalInstituciones", totalInstituciones);
        model.addAttribute("institucionesActivas", institucionesActivas);
        model.addAttribute("porcentajeActivos", porcentajeActivos);
        model.addAttribute("totalRoles", totalRoles);

        model.addAttribute("ultimosUsuarios",
                usuarios.stream().sorted((a, b) -> b.getId().compareTo(a.getId())).limit(5).toList());
        model.addAttribute("ultimasInstituciones",
                instituciones.stream().sorted((a, b) -> b.getId().compareTo(a.getId())).limit(5).toList());
    }
}
