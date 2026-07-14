package com.chavescr.nexa.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.chavescr.nexa.entity.RegistroAsistencia;
import com.chavescr.nexa.entity.RetiroEstudiante;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.entity.Visita;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.PersonalService;
import com.chavescr.nexa.service.RegistroAsistenciaService;
import com.chavescr.nexa.service.RetiroEstudianteService;
import com.chavescr.nexa.service.UsuarioService;
import com.chavescr.nexa.service.VisitaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/control-de-acceso")
public class VisitaController {

    @Autowired
    private VisitaService visitaService;

    @Autowired
    private RegistroAsistenciaService registroAsistenciaService;

    @Autowired
    private RetiroEstudianteService retiroEstudianteService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PersonalService personalService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String index(Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            List<Visita> visitasDelDia = visitaService.obtenerVisitasDelDia(institucionId);
            model.addAttribute("visitas", visitasDelDia);
            agregarStats(model, visitasDelDia);

            model.addAttribute("personas", personalService.listarPorRol(institucionId, rolParaTipo("DOCENTE")));

            List<RegistroAsistencia> asistenciasDelDia = registroAsistenciaService.obtenerRegistrosDelDia(institucionId);
            model.addAttribute("asistencias", asistenciasDelDia);
            Map<String, Long> conteo = registroAsistenciaService.obtenerConteoPersonalPresente(institucionId);
            model.addAttribute("asistenciaPresentes", conteo.get("presentes"));
            model.addAttribute("asistenciaTotalRegistros", conteo.get("totalRegistros"));

            List<Usuario> usuarios = usuarioService.obtenerTodos();
            model.addAttribute("usuarios", usuarios);

            List<RetiroEstudiante> retirosDelDia = retiroEstudianteService.obtenerRetirosDelDia(institucionId);
            model.addAttribute("retiros", retirosDelDia);
            agregarStatsRetiros(model, retirosDelDia);
        }
        model.addAttribute("puedeAutorizarRetiros", esPersonalAutorizado(request));
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "control-acceso/index :: htmx-content";
        }
        return "control-acceso/index";
    }

    private void agregarStatsRetiros(Model model, List<RetiroEstudiante> retiros) {
        long pendientes = retiros.stream().filter(r -> r.getEstado() == RetiroEstudiante.EstadoRetiro.PENDIENTE).count();
        long autorizados = retiros.stream().filter(r -> r.getEstado() == RetiroEstudiante.EstadoRetiro.AUTORIZADO).count();
        long finalizados = retiros.stream().filter(r -> r.getEstado() == RetiroEstudiante.EstadoRetiro.FINALIZADO).count();
        model.addAttribute("retirosStatsPendientes", pendientes);
        model.addAttribute("retirosStatsAutorizados", autorizados);
        model.addAttribute("retirosStatsFinalizados", finalizados);
        model.addAttribute("retirosStatsTotal", (long) retiros.size());
    }

    private boolean esPersonalAutorizado(HttpServletRequest request) {
        return request.isUserInRole("ROLE_ADMIN") || request.isUserInRole("ROLE_DIRECTOR")
                || request.isUserInRole("ROLE_DOCENTE");
    }

    private void exigirPersonalAutorizado(HttpServletRequest request) {
        if (!esPersonalAutorizado(request)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo el personal de la institución puede autorizar retiros de estudiantes");
        }
    }

    private void agregarStats(Model model, List<Visita> visitas) {
        long pendientes = visitas.stream().filter(v -> v.getEstado() == Visita.EstadoVisita.PENDIENTE).count();
        long autorizadas = visitas.stream().filter(v -> v.getEstado() == Visita.EstadoVisita.AUTORIZADA).count();
        long finalizadas = visitas.stream().filter(v -> v.getEstado() == Visita.EstadoVisita.FINALIZADA).count();
        long denegadas = visitas.stream().filter(v -> v.getEstado() == Visita.EstadoVisita.DENEGADA).count();
        model.addAttribute("statsPendientes", pendientes);
        model.addAttribute("statsAutorizadas", autorizadas);
        model.addAttribute("statsFinalizadas", finalizadas);
        model.addAttribute("statsDenegadas", denegadas);
        model.addAttribute("statsTotal", (long) visitas.size());
    }

    @GetMapping("/buscar-padre")
    @ResponseBody
    public Map<String, Object> buscarPadre(@RequestParam String cedula, HttpSession session) {
        Map<String, Object> resultado = new HashMap<>();
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        Optional<Usuario> padreOpt = institucionId == null
                ? Optional.empty()
                : usuarioRepository.findPadreByCedulaAndInstitucionId(cedula.trim(), institucionId);
        if (padreOpt.isPresent()) {
            Usuario padre = padreOpt.get();
            resultado.put("encontrado", true);
            resultado.put("nombre", padre.getNombre());
            resultado.put("identificacion", padre.getCedula());
        } else {
            resultado.put("encontrado", false);
        }
        return resultado;
    }

    @GetMapping("/personas")
    public String personas(@RequestParam String tipoDestinatario, Model model, HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            model.addAttribute("personas", personalService.listarPorRol(institucionId, rolParaTipo(tipoDestinatario)));
        }
        return "control-acceso/registrar/registrar :: campo-persona";
    }

    private String rolParaTipo(String tipoDestinatario) {
        return switch (tipoDestinatario) {
            case "DIRECTORA" -> "ROLE_DIRECTOR";
            case "ADMINISTRATIVO" -> "ROLE_ADMIN";
            default -> "ROLE_DOCENTE";
        };
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam String filtro, Model model, HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null && filtro != null && !filtro.isBlank()) {
            List<Visita> resultados = visitaService.buscarPorFiltro(filtro, institucionId);
            model.addAttribute("resultados", resultados);
            model.addAttribute("filtro", filtro);
        }
        return "control-acceso/index :: resultados-busqueda";
    }

    @GetMapping("/historial")
    public String historial(Model model, HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            List<Visita> visitasDelDia = visitaService.obtenerVisitasDelDia(institucionId);
            model.addAttribute("visitas", visitasDelDia);
            agregarStats(model, visitasDelDia);
        }
        return "control-acceso/historial/historial :: tabla-historial";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam String nombreVisitante,
            @RequestParam(required = false) String identificacion,
            @RequestParam String motivo,
            @RequestParam String tipoDestinatario,
            @RequestParam String personaAVisitar,
            @RequestParam(defaultValue = "false") boolean tieneCita,
            @RequestParam(required = false) String observaciones,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "No hay institución activa.");
            return "redirect:/control-de-acceso";
        }

        Visita visita = new Visita();
        visita.setNombreVisitante(nombreVisitante);
        visita.setIdentificacion(identificacion);
        visita.setMotivo(motivo);
        visita.setTipoDestinatario(Visita.TipoDestinatario.valueOf(tipoDestinatario));
        visita.setPersonaAVisitar(personaAVisitar);
        visita.setTieneCita(tieneCita);
        visita.setObservaciones(observaciones);

        visitaService.registrarVisita(visita, institucionId);
        redirectAttributes.addFlashAttribute("successMsg", "Visita registrada exitosamente.");
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/autorizar")
    public String autorizar(@RequestParam Long visitaId, RedirectAttributes redirectAttributes) {
        visitaService.autorizarEntrada(visitaId);
        redirectAttributes.addFlashAttribute("successMsg", "Entrada autorizada.");
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/denegar")
    public String denegar(@RequestParam Long visitaId,
            @RequestParam(required = false) String motivoDenegacion,
            RedirectAttributes redirectAttributes) {
        visitaService.denegarEntrada(visitaId, motivoDenegacion);
        redirectAttributes.addFlashAttribute("successMsg", "Entrada denegada.");
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/salida")
    public String salida(@RequestParam Long visitaId, RedirectAttributes redirectAttributes) {
        visitaService.registrarSalida(visitaId);
        redirectAttributes.addFlashAttribute("successMsg", "Salida registrada.");
        return "redirect:/control-de-acceso";
    }

    @GetMapping("/asistencia/refresh")
    public String refreshAsistencia(Model model, HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            List<RegistroAsistencia> asistenciasDelDia = registroAsistenciaService.obtenerRegistrosDelDia(institucionId);
            model.addAttribute("asistencias", asistenciasDelDia);
            Map<String, Long> conteo = registroAsistenciaService.obtenerConteoPersonalPresente(institucionId);
            model.addAttribute("asistenciaPresentes", conteo.get("presentes"));
            model.addAttribute("asistenciaTotalRegistros", conteo.get("totalRegistros"));

            List<Usuario> usuarios = usuarioService.obtenerTodos();
            model.addAttribute("usuarios", usuarios);
        }
        return "control-acceso/asistencia/asistencia :: tabla-asistencia";
    }

    @PostMapping("/asistencia/registrar")
    public String registrarAsistencia(@RequestParam Long usuarioId,
            @RequestParam String tipo,
            @RequestParam(required = false) String observaciones,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "No hay institución activa.");
            return "redirect:/control-de-acceso";
        }

        RegistroAsistencia.TipoRegistro tipoRegistro = RegistroAsistencia.TipoRegistro.valueOf(tipo);
        registroAsistenciaService.registrar(usuarioId, tipoRegistro, observaciones, institucionId);
        redirectAttributes.addFlashAttribute("successMsg",
                tipoRegistro == RegistroAsistencia.TipoRegistro.ENTRADA
                        ? "Entrada registrada exitosamente."
                        : "Salida registrada exitosamente.");
        return "redirect:/control-de-acceso";
    }

    // ─── RETIRO DE ESTUDIANTES (solicitado por padres desde Portal Padres) ──

    @GetMapping("/retiros/refresh")
    public String refreshRetiros(Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId != null) {
            List<RetiroEstudiante> retirosDelDia = retiroEstudianteService.obtenerRetirosDelDia(institucionId);
            model.addAttribute("retiros", retirosDelDia);
            agregarStatsRetiros(model, retirosDelDia);
        }
        model.addAttribute("puedeAutorizarRetiros", esPersonalAutorizado(request));
        return "control-acceso/retiros/retiros :: tabla-retiros";
    }

    @PostMapping("/retiros/autorizar")
    public String autorizarRetiro(@RequestParam Long retiroId, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        exigirPersonalAutorizado(request);
        retiroEstudianteService.autorizar(retiroId);
        redirectAttributes.addFlashAttribute("successMsg", "Retiro autorizado.");
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/retiros/denegar")
    public String denegarRetiro(@RequestParam Long retiroId,
            @RequestParam(required = false) String motivoDenegacion,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        exigirPersonalAutorizado(request);
        retiroEstudianteService.denegar(retiroId, motivoDenegacion);
        redirectAttributes.addFlashAttribute("successMsg", "Retiro denegado.");
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/retiros/salida")
    public String salidaRetiro(@RequestParam Long retiroId, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        exigirPersonalAutorizado(request);
        retiroEstudianteService.registrarSalida(retiroId);
        redirectAttributes.addFlashAttribute("successMsg", "Salida del estudiante registrada.");
        return "redirect:/control-de-acceso";
    }
}
