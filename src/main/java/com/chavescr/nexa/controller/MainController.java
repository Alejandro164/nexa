package com.chavescr.nexa.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.DashboardService;
import com.chavescr.nexa.service.DireccionService;
import com.chavescr.nexa.service.SesionDireccionService;
import com.chavescr.nexa.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

    private static final Locale ES = Locale.forLanguageTag("es-CR");

    /** Modos del menú lateral ("Ver el menú como"). */
    private static final Set<String> MODOS = Set.of("admin", "system", "director", "docente", "estudiante", "padre",
            "auxiliar", "guarda");

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DireccionService direccionService;

    @Autowired
    private SesionDireccionService sesionDireccionService;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails usuario, Model model,
            HttpServletRequest request, HttpSession session) {

        session.setAttribute("SESSION_USUARIO_ID", usuario.getId());

        var resultado = sesionDireccionService.resolver(session, request.isUserInRole("ROLE_ADMIN"),
                request.isUserInRole("ROLE_SYSTEM_CONFIG"));
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

        cargarDashboard(model, session, request, null);
        return "inicio/inicio";
    }

    /**
     * @param modo el "Modo" del menú lateral (admin, director, docente, padre, estudiante...). Solo lo
     *             respetan admin y system config, para previsualizar el inicio de otro rol.
     */
    @GetMapping("/inicio")
    public String inicio(@RequestParam(required = false) String modo, Model model, HttpServletRequest request,
            HttpSession session) {
        cargarDashboard(model, session, request, modo);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "inicio/inicio :: htmx-content";
        }
        return "inicio/inicio";
    }

    /** Rendimiento del período: recorre las notas de toda la dirección, por eso llega aparte del inicio. */
    @GetMapping("/inicio/rendimiento")
    public String rendimiento(Model model, HttpServletRequest request, HttpServletResponse response,
            HttpSession session) throws IOException {
        Long direccionId = direccionActual(session);
        if (direccionId == null || !esGestion(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        model.addAttribute("rendimiento", dashboardService.rendimiento(direccionId));
        return "inicio/paneles/rendimiento :: rendimiento";
    }

    @GetMapping("/inicio/direcciones-modal")
    public String direccionesModal(@RequestParam(required = false) String origen, Model model,
            HttpServletRequest request, HttpSession session) {
        model.addAttribute("instituciones", sesionDireccionService
                .navegacion(veTodasLasInstituciones(request), direccionActual(session)).instituciones());
        if ("login".equals(origen)) {
            return "auth/seleccionar-direccion-modal :: modal-content";
        }
        return "inicio/direcciones-modal :: modal-content";
    }

    @PostMapping("/inicio/cambiar-direccion")
    public String cambiarDireccion(@RequestParam Long direccionId,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model) throws IOException {
        Long usuarioId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        boolean veTodas = veTodasLasInstituciones(request);
        if (!sesionDireccionService.cambiar(session, usuarioId, direccionId, veTodas)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        if (!"true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
            response.sendRedirect("/inicio");
            return null;
        }
        // Sin recarga del documento: el contenido y el selector cambian en el mismo paso.
        // Si la institución tiene una sola dirección, el selector llega vacío y no aparece.
        Object actual = session.getAttribute("SESSION_DIRECCION_ID");
        Long actualId = actual instanceof Long id ? id : null;
        var nav = sesionDireccionService.navegacion(veTodas, actualId);
        model.addAttribute("menuDireccion", nav.menu());
        model.addAttribute("puedeCambiarInstitucion", nav.puedeCambiarInstitucion());
        session.setAttribute("SESSION_PUEDE_CAMBIAR_INSTITUCION", nav.puedeCambiarInstitucion());
        model.addAttribute("sincronizarSwitcher", true);
        cargarDashboard(model, session, request, null);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("HX-Retarget", ".content-area");
        response.setHeader("HX-Reswap", "innerHTML");
        response.setHeader("HX-Push-Url", "/inicio");
        response.setHeader("HX-Trigger", "direccionCambiada");
        return "inicio/inicio :: htmx-content";
    }

    private static Long direccionActual(HttpSession session) {
        Object actual = session.getAttribute("SESSION_DIRECCION_ID");
        return actual instanceof Long id ? id : null;
    }

    private static boolean veTodasLasInstituciones(HttpServletRequest request) {
        return request.isUserInRole("ROLE_SYSTEM_CONFIG");
    }

    private static String saludo(LocalTime hora) {
        if (hora.getHour() < 12) {
            return "Buenos días";
        }
        return hora.getHour() < 19 ? "Buenas tardes" : "Buenas noches";
    }

    /** Admin, dirección y system config ven el panel de la dirección (system config ve todas). */
    private static boolean esGestion(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR")
                || request.isUserInRole("ROLE_SYSTEM_CONFIG");
    }

    /** Puede cambiar el "Modo" del menú y ver el inicio de otro rol. */
    private static boolean puedePrevisualizar(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_SYSTEM_CONFIG");
    }

    /** El modo en que se ve el menú cuando no se eligió otro: el del rol real. */
    private static String modoPropio(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") ? "admin" : "system";
    }

    /**
     * Cada rol ve su propio panel: la dirección lo que pasa hoy y lo que requiere su atención, el docente
     * sus lecciones y pendientes, y la familia o el estudiante cómo va en el período.
     * Admin y system config pueden pedir el de otro rol con {@code modo}; se arma con un usuario de muestra
     * de la dirección, porque ellos no tienen lecciones ni hijos propios.
     */
    private void cargarDashboard(Model model, HttpSession session, HttpServletRequest request, String modo) {
        String direccionActivaNombre = (String) session.getAttribute("SESSION_DIRECCION_NOMBRE");
        model.addAttribute("direccionActivaNombre", direccionActivaNombre);
        model.addAttribute("saludo", saludo(LocalTime.now(DashboardService.ZONA)));
        String fecha = LocalDate.now(DashboardService.ZONA).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES));
        model.addAttribute("fechaHoy", Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CustomUserDetails u
                && u.getNombre() != null && !u.getNombre().isBlank()) {
            model.addAttribute("primerNombre", u.getNombre().trim().split("\\s+")[0]);
        }

        boolean previsualiza = puedePrevisualizar(request);
        String modoInicio = previsualiza && modo != null && MODOS.contains(modo) ? modo : null;
        model.addAttribute("puedePrevisualizar", previsualiza);
        model.addAttribute("modoInicio", previsualiza ? (modoInicio != null ? modoInicio : modoPropio(request)) : "");

        Long direccionId = direccionActual(session);
        if (direccionId == null) {
            model.addAttribute("sinDireccion", true);
            model.addAttribute("vistaInicio", "global");
            cargarResumenGlobal(model);
            return;
        }

        if (modoInicio != null && cargarVistaPrevia(model, direccionId, modoInicio)) {
            return;
        }

        Object idSesion = session.getAttribute("SESSION_USUARIO_ID");
        Long usuarioId = idSesion instanceof Long id ? id : null;
        if (esGestion(request)) {
            model.addAttribute("vistaInicio", "direccion");
            model.addAttribute("panel", dashboardService.direccion(direccionId, usuarioId));
        } else if (request.isUserInRole("ROLE_DOCENTE")) {
            model.addAttribute("vistaInicio", "docente");
            model.addAttribute("panel", dashboardService.docente(direccionId, usuarioId));
        } else if (request.isUserInRole("ROLE_PADRE") || request.isUserInRole("ROLE_ESTUDIANTE")) {
            boolean esEstudiante = !request.isUserInRole("ROLE_PADRE");
            model.addAttribute("vistaInicio", "familia");
            model.addAttribute("panel", dashboardService.familia(direccionId, usuarioId, esEstudiante));
        } else {
            model.addAttribute("vistaInicio", "vacio");
        }
    }

    /**
     * Inicio de docente, padre o estudiante visto por admin o system config, con un usuario de muestra.
     * Devuelve false en los modos que ven el panel de la dirección (admin, director, auxiliar, guarda...).
     */
    private boolean cargarVistaPrevia(Model model, Long direccionId, String modo) {
        Optional<Usuario> muestra;
        String rol;
        switch (modo) {
            case "docente" -> {
                muestra = dashboardService.docenteDeMuestra(direccionId);
                rol = "docente";
            }
            case "padre" -> {
                muestra = dashboardService.padreDeMuestra(direccionId);
                rol = "padre de familia";
            }
            case "estudiante" -> {
                muestra = dashboardService.estudianteDeMuestra(direccionId);
                rol = "estudiante";
            }
            default -> {
                return false;
            }
        }
        model.addAttribute("vistaPreviaRol", rol);
        if (muestra.isEmpty()) {
            model.addAttribute("vistaInicio", "sin-muestra");
            return true;
        }
        Usuario usuario = muestra.get();
        model.addAttribute("vistaPreviaNombre", usuario.getNombre());
        if ("docente".equals(modo)) {
            model.addAttribute("vistaInicio", "docente");
            model.addAttribute("panel", dashboardService.docente(direccionId, usuario.getId()));
        } else {
            model.addAttribute("vistaInicio", "familia");
            model.addAttribute("panel", dashboardService.familia(direccionId, usuario.getId(), "estudiante".equals(modo)));
        }
        return true;
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
