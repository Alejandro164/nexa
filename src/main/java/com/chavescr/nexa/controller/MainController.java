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

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.ConfiguracionAcademicaService;
import com.chavescr.nexa.service.HistorialCambioService;
import com.chavescr.nexa.service.DireccionService;
import com.chavescr.nexa.service.RegistroAsistenciaService;
import com.chavescr.nexa.service.SesionDireccionService;
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
    private DireccionService direccionService;

    @Autowired
    private SesionDireccionService sesionDireccionService;

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

        var resultado = sesionDireccionService.resolver(session, request.isUserInRole("ROLE_ADMIN"));
        if (resultado.estado() != SesionDireccionService.Estado.RESUELTA) {
            // La selección de dirección ahora se resuelve en el login (modal por AJAX); si se
            // llega aquí sin dirección resuelta (JS deshabilitado, navegación directa a /, etc.)
            // se cierra la sesión y se manda de vuelta al login para que pase por ese flujo.
            session.invalidate();
            return "redirect:/login";
        }

        // El @ModelAttribute global se calculó ANTES de este handler, así que si resolver() acaba
        // de auto-seleccionar dirección (efecto secundario del propio resolver) puede haber quedado
        // desactualizado — se recalcula aquí con el estado de sesión ya resuelto.
        model.addAttribute("sinDireccionAdmin",
                request.isUserInRole("ROLE_ADMIN") && session.getAttribute("SESSION_DIRECCION_ID") == null);

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

    @GetMapping("/inicio/direcciones-modal")
    public String direccionesModal(@RequestParam(required = false) String origen, Model model,
            HttpServletRequest request, HttpSession session) {
        if (request.isUserInRole("ROLE_ADMIN")) {
            model.addAttribute("direcciones", direccionService.obtenerTodasDTO());
        } else {
            model.addAttribute("direcciones", usuarioService.obtenerDireccionesDelUsuarioActual());
        }
        model.addAttribute("direccionActualId", session.getAttribute("SESSION_DIRECCION_ID"));
        if ("login".equals(origen)) {
            return "auth/seleccionar-direccion-modal :: modal-content";
        }
        return "inicio/direcciones-modal :: modal-content";
    }

    @PostMapping("/inicio/cambiar-direccion")
    public void cambiarDireccion(@RequestParam Long direccionId,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");

        if (request.isUserInRole("ROLE_ADMIN")) {
            direccionService.findById(direccionId).ifPresent(inst -> {
                session.setAttribute("SESSION_DIRECCION_ID", direccionId);
                session.setAttribute("SESSION_DIRECCION_NOMBRE", inst.getPresentacion());
                usuarioService.actualizarUltimaDireccion(usuarioId, inst);
            });
        } else {
            usuarioService.obtenerDireccionesDelUsuarioActual().stream()
                    .filter(inst -> inst.getId().equals(direccionId))
                    .findFirst()
                    .ifPresent(inst -> {
                        session.setAttribute("SESSION_DIRECCION_ID", direccionId);
                        session.setAttribute("SESSION_DIRECCION_NOMBRE", inst.getNombre());
                        direccionService.findById(direccionId)
                                .ifPresent(entidad -> usuarioService.actualizarUltimaDireccion(usuarioId, entidad));
                    });
        }

        // Este endpoint se llama tanto por htmx (modal "Cambiar de Dirección", con hx-target
        // apuntando al modal) como por un form normal (modal de selección tras login); en el caso
        // htmx un "redirect:" de Spring solo recargaría el contenido DENTRO del modal, dejando el
        // dashboard de fondo con los datos de la dirección anterior — por eso se fuerza una
        // recarga completa del navegador vía HX-Redirect en vez de un redirect normal.
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", "/inicio");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        response.sendRedirect("/inicio");
    }

    @PostMapping("/inicio/salir-direccion")
    public void salirDireccion(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        // Solo ROLE_ADMIN puede operar sin dirección seleccionada (ver SesionDireccionService.resolver).
        if (!request.isUserInRole("ROLE_ADMIN")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        session.removeAttribute("SESSION_DIRECCION_ID");
        session.removeAttribute("SESSION_DIRECCION_NOMBRE");
        // Se olvida también la dirección recordada: si no, el próximo login la auto-seleccionaría
        // de nuevo (seleccionarRecordada) y "salir" no tendría efecto duradero.
        usuarioService.actualizarUltimaDireccion(usuarioId, null);

        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", "/inicio");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        response.sendRedirect("/inicio");
    }

    private void cargarDashboard(Model model, HttpSession session) {
        String direccionActivaNombre = (String) session.getAttribute("SESSION_DIRECCION_NOMBRE");
        model.addAttribute("direccionActivaNombre", direccionActivaNombre);

        Long direccionId = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (direccionId == null) {
            model.addAttribute("sinDireccion", true);
            cargarResumenGlobal(model);
            return;
        }

        long totalEstudiantes = usuarioService.contarActivosPorDireccionYRol(direccionId, "ROLE_ESTUDIANTE");
        long totalDocentes = usuarioService.contarActivosPorDireccionYRol(direccionId, "ROLE_DOCENTE");

        Map<String, Long> asistenciaHoy = registroAsistenciaService.obtenerConteoPersonalPresente(direccionId);

        List<PeriodoAcademico> periodosActivos = configuracionAcademicaService.listarPeriodosActivos(direccionId);
        PeriodoAcademico periodoActivo = periodosActivos.isEmpty() ? null : periodosActivos.get(0);

        List<NivelAcademico> niveles = configuracionAcademicaService.listarNivelesActivos(direccionId);

        model.addAttribute("totalEstudiantes", totalEstudiantes);
        model.addAttribute("totalDocentes", totalDocentes);
        model.addAttribute("personalPresenteHoy", asistenciaHoy.getOrDefault("presentes", 0L));
        model.addAttribute("totalRegistrosHoy", asistenciaHoy.getOrDefault("totalRegistros", 0L));
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("niveles", niveles);
        model.addAttribute("actividadReciente", historialCambioService.listarRecientes(direccionId));
    }

    /**
     * Panel para el admin sin dirección seleccionada: en vez del detalle de una dirección
     * (que no aplica aquí), muestra un resumen global del sistema — mismos indicadores que ya
     * existían en el dashboard antes de que este pasara a estar scoped a una dirección.
     */
    private void cargarResumenGlobal(Model model) {
        List<UsuarioDTO> usuarios = usuarioService.obtenerTodosDTO();
        List<DireccionDTO> direcciones = direccionService.obtenerTodasDTO();

        int totalUsuarios = usuarios.size();
        long usuariosActivos = usuarios.stream().filter(UsuarioDTO::isActivo).count();
        int totalDirecciones = direcciones.size();
        long direccionesActivas = direcciones.stream().filter(DireccionDTO::isActiva).count();
        int porcentajeActivos = totalUsuarios > 0
                ? (int) Math.round((double) usuariosActivos / totalUsuarios * 100)
                : 0;
        long totalRoles = usuarios.stream()
                .flatMap(u -> u.getRoles().stream())
                .distinct()
                .count();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("totalDirecciones", totalDirecciones);
        model.addAttribute("direccionesActivas", direccionesActivas);
        model.addAttribute("porcentajeActivos", porcentajeActivos);
        model.addAttribute("totalRoles", totalRoles);

        model.addAttribute("ultimosUsuarios",
                usuarios.stream().sorted((a, b) -> b.getId().compareTo(a.getId())).limit(5).toList());
        model.addAttribute("ultimasDirecciones",
                direcciones.stream().sorted((a, b) -> b.getId().compareTo(a.getId())).limit(5).toList());
    }
}
