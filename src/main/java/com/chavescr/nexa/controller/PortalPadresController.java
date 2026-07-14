package com.chavescr.nexa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.chavescr.nexa.entity.RegistroAsistencia;
import com.chavescr.nexa.entity.Solicitud;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.PersonalService;
import com.chavescr.nexa.service.RegistroAsistenciaService;
import com.chavescr.nexa.service.RetiroEstudianteService;
import com.chavescr.nexa.service.SolicitudService;
import com.chavescr.nexa.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/portal-padres")
public class PortalPadresController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RetiroEstudianteService retiroEstudianteService;

    @Autowired
    private RegistroAsistenciaService registroAsistenciaService;

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private PersonalService personalService;

    @GetMapping
    public String index(@RequestHeader(value = "HX-Request", required = false) boolean htmxRequest,
            Model model, HttpSession session, HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        exigirPadreOAdmin(request);
        model.addAttribute("hijos", usuarioService.obtenerEstudiantesDelUsuarioActual());
        model.addAttribute("retiros", retiroEstudianteService.obtenerRetirosDelPadre(usuario.getId()));
        model.addAttribute("misSolicitudes", solicitudService.listarPorPadre(usuario.getId()));
        Long institucionId = institucionId(session);
        if (institucionId != null) {
            model.addAttribute("docentesDisponibles", personalService.listarPorRol(institucionId, "ROLE_DOCENTE"));
        }
        return htmxRequest ? "padres/index :: htmx-content" : "padres/index";
    }

    @PostMapping("/retiros")
    public String solicitarRetiro(@RequestParam Long estudianteId,
            @RequestParam(required = false) String motivo,
            HttpServletRequest request, HttpSession session,
            @AuthenticationPrincipal CustomUserDetails usuario,
            RedirectAttributes redirectAttributes) {
        exigirPadreOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        try {
            retiroEstudianteService.solicitarRetiro(usuario.getId(), estudianteId, motivo, institucionId);
            redirectAttributes.addFlashAttribute("successMsg", "Solicitud de retiro enviada. Espera la autorización en portería.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/portal-padres";
    }

    @PostMapping("/ingreso")
    public String registrarIngreso(@RequestParam String tipo,
            HttpServletRequest request, HttpSession session,
            @AuthenticationPrincipal CustomUserDetails usuario,
            RedirectAttributes redirectAttributes) {
        exigirPadreOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        RegistroAsistencia.TipoRegistro tipoRegistro = RegistroAsistencia.TipoRegistro.valueOf(tipo);
        registroAsistenciaService.registrar(usuario.getId(), tipoRegistro, "Autorregistro desde Portal Padres", institucionId);
        redirectAttributes.addFlashAttribute("successMsg",
                tipoRegistro == RegistroAsistencia.TipoRegistro.ENTRADA
                        ? "Ingreso al campus registrado."
                        : "Salida del campus registrada.");
        return "redirect:/portal-padres";
    }

    @PostMapping("/solicitudes")
    public String solicitarTramite(@RequestParam Solicitud.TipoSolicitud tipo,
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long docenteId,
            @RequestParam(required = false) String detalle,
            HttpServletRequest request, HttpSession session,
            @AuthenticationPrincipal CustomUserDetails usuario,
            RedirectAttributes redirectAttributes) {
        exigirPadreOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        try {
            solicitudService.crearSolicitud(usuario.getId(), tipo, estudianteId, docenteId, detalle, institucionId);
            redirectAttributes.addFlashAttribute("successMsg", "Solicitud enviada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/portal-padres";
    }

    private void exigirPadreOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_PADRE") && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo usuarios con rol Padre o Admin pueden acceder al Portal de Padres");
        }
    }

    private Long institucionId(HttpSession session) {
        return (Long) session.getAttribute("SESSION_INSTITUCION_ID");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = institucionId(session);
        if (id == null) {
            throw new IllegalArgumentException("No hay institución seleccionada");
        }
        return id;
    }
}
