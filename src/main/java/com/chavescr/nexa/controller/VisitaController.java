package com.chavescr.nexa.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.chavescr.nexa.dto.HaciendaContribuyenteDTO;
import com.chavescr.nexa.entity.RegistroAsistencia;
import com.chavescr.nexa.entity.RetiroEstudiante;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.entity.Visita;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.chavescr.nexa.service.HaciendaService;
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

    private static final DateTimeFormatter FECHA_HORA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

    @Autowired
    private HaciendaService haciendaService;

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

            List<Usuario> usuarios = usuarioService.obtenerPersonalActivoPorInstitucion(institucionId);
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

    @GetMapping("/buscar-visitante")
    @ResponseBody
    public Map<String, Object> buscarVisitante(@RequestParam String identificacion, HttpSession session) {
        Map<String, Object> resultado = new HashMap<>();
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        Optional<Visita> visitaOpt = institucionId == null
                ? Optional.empty()
                : visitaService.buscarVisitanteRecurrente(identificacion.trim(), institucionId);
        if (visitaOpt.isPresent()) {
            resultado.put("encontrado", true);
            resultado.put("nombre", visitaOpt.get().getNombreVisitante());
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
    public String historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model, HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        LocalDate desdeReal = desde != null ? desde : LocalDate.now().minusDays(7);
        LocalDate hastaReal = hasta != null ? hasta : LocalDate.now();
        model.addAttribute("desde", desdeReal);
        model.addAttribute("hasta", hastaReal);
        if (institucionId != null) {
            List<Visita> visitas = visitaService.obtenerVisitasPorRango(institucionId, desdeReal, hastaReal);
            model.addAttribute("visitas", visitas);
            agregarStats(model, visitas);
        }
        return "control-acceso/historial/historial :: tabla-historial";
    }

    @GetMapping("/historial/exportar")
    public ResponseEntity<byte[]> exportarHistorial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        LocalDate desdeReal = desde != null ? desde : LocalDate.now().minusDays(7);
        LocalDate hastaReal = hasta != null ? hasta : LocalDate.now();
        List<Visita> visitas = institucionId == null ? List.of()
                : visitaService.obtenerVisitasPorRango(institucionId, desdeReal, hastaReal);

        StringBuilder csv = new StringBuilder();
        csv.append("Visitante,Identificacion,Persona Visitada,Tipo,Motivo,Cita Previa,Estado,Fecha Registro,Hora Ingreso,Hora Salida\n");
        for (Visita v : visitas) {
            csv.append(csvCampo(v.getNombreVisitante())).append(',')
                    .append(csvCampo(v.getIdentificacion())).append(',')
                    .append(csvCampo(v.getPersonaVisitada() != null ? v.getPersonaVisitada().getNombre() : null)).append(',')
                    .append(v.getTipoDestinatario()).append(',')
                    .append(csvCampo(v.getMotivo())).append(',')
                    .append(Boolean.TRUE.equals(v.getTieneCita()) ? "Si" : "No").append(',')
                    .append(v.getEstado()).append(',')
                    .append(csvFecha(v.getFechaRegistro())).append(',')
                    .append(csvFecha(v.getFechaHoraIngreso())).append(',')
                    .append(csvFecha(v.getFechaHoraSalida()))
                    .append('\n');
        }
        return csvResponse(csv, "visitas.csv");
    }

    @GetMapping("/asistencia/exportar")
    public ResponseEntity<byte[]> exportarAsistencia(HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        List<RegistroAsistencia> registros = institucionId == null ? List.of()
                : registroAsistenciaService.obtenerRegistrosDelDia(institucionId);

        StringBuilder csv = new StringBuilder();
        csv.append("Funcionario,Correo,Tipo,Hora,Observaciones\n");
        for (RegistroAsistencia r : registros) {
            csv.append(csvCampo(r.getUsuario().getNombre())).append(',')
                    .append(csvCampo(r.getUsuario().getEmail())).append(',')
                    .append(r.getTipo()).append(',')
                    .append(csvFecha(r.getFechaHora())).append(',')
                    .append(csvCampo(r.getObservaciones()))
                    .append('\n');
        }
        return csvResponse(csv, "asistencia-personal.csv");
    }

    @GetMapping("/retiros/exportar")
    public ResponseEntity<byte[]> exportarRetiros(HttpSession session) {
        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        List<RetiroEstudiante> retiros = institucionId == null ? List.of()
                : retiroEstudianteService.obtenerRetirosDelDia(institucionId);

        StringBuilder csv = new StringBuilder();
        csv.append("Estudiante,Padre,Motivo,Estado,Solicitado,Salida,Retirado por,Identificacion de quien retira\n");
        for (RetiroEstudiante r : retiros) {
            csv.append(csvCampo(r.getEstudiante().getNombre())).append(',')
                    .append(csvCampo(r.getPadre().getNombre())).append(',')
                    .append(csvCampo(r.getMotivo())).append(',')
                    .append(r.getEstado()).append(',')
                    .append(csvFecha(r.getFechaHoraSolicitud())).append(',')
                    .append(csvFecha(r.getFechaHoraSalida())).append(',')
                    .append(csvCampo(r.getRetiradoPorNombre())).append(',')
                    .append(csvCampo(r.getRetiradoPorIdentificacion()))
                    .append('\n');
        }
        return csvResponse(csv, "retiros-estudiantes.csv");
    }

    private String csvCampo(String valor) {
        return valor == null ? "" : "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private String csvFecha(java.time.LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA_CSV);
    }

    private ResponseEntity<byte[]> csvResponse(StringBuilder csv, String nombreArchivo) {
        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam String nombreVisitante,
            @RequestParam(required = false) String identificacion,
            @RequestParam String motivo,
            @RequestParam String tipoDestinatario,
            @RequestParam Long personaVisitadaId,
            @RequestParam(defaultValue = "false") boolean tieneCita,
            @RequestParam(required = false) String observaciones,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long institucionId = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (institucionId == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "No hay institución activa.");
            return "redirect:/control-de-acceso";
        }

        Usuario personaVisitada = usuarioRepository.findById(personaVisitadaId).orElse(null);
        if (personaVisitada == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "La persona a visitar seleccionada no es válida.");
            return "redirect:/control-de-acceso";
        }

        Visita visita = new Visita();
        visita.setNombreVisitante(nombreVisitante);
        visita.setIdentificacion(identificacion);
        visita.setMotivo(motivo);
        visita.setTipoDestinatario(Visita.TipoDestinatario.valueOf(tipoDestinatario));
        visita.setPersonaVisitada(personaVisitada);
        visita.setTieneCita(tieneCita);
        visita.setObservaciones(observaciones);

        visitaService.registrarVisita(visita, institucionId);
        redirectAttributes.addFlashAttribute("successMsg", "Visita registrada exitosamente.");
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/autorizar")
    public String autorizar(@RequestParam Long visitaId, RedirectAttributes redirectAttributes) {
        try {
            visitaService.autorizarEntrada(visitaId);
            redirectAttributes.addFlashAttribute("successMsg", "Entrada autorizada.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/denegar")
    public String denegar(@RequestParam Long visitaId,
            @RequestParam(required = false) String motivoDenegacion,
            RedirectAttributes redirectAttributes) {
        try {
            visitaService.denegarEntrada(visitaId, motivoDenegacion);
            redirectAttributes.addFlashAttribute("successMsg", "Entrada denegada.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/salida")
    public String salida(@RequestParam Long visitaId, RedirectAttributes redirectAttributes) {
        try {
            visitaService.registrarSalida(visitaId);
            redirectAttributes.addFlashAttribute("successMsg", "Salida registrada.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
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

            List<Usuario> usuarios = usuarioService.obtenerPersonalActivoPorInstitucion(institucionId);
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
        try {
            registroAsistenciaService.registrar(usuarioId, tipoRegistro, observaciones, institucionId);
            redirectAttributes.addFlashAttribute("successMsg",
                    tipoRegistro == RegistroAsistencia.TipoRegistro.ENTRADA
                            ? "Entrada registrada exitosamente."
                            : "Salida registrada exitosamente.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
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
        try {
            retiroEstudianteService.autorizar(retiroId);
            redirectAttributes.addFlashAttribute("successMsg", "Retiro autorizado.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/retiros/denegar")
    public String denegarRetiro(@RequestParam Long retiroId,
            @RequestParam(required = false) String motivoDenegacion,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        exigirPersonalAutorizado(request);
        try {
            retiroEstudianteService.denegar(retiroId, motivoDenegacion);
            redirectAttributes.addFlashAttribute("successMsg", "Retiro denegado.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/control-de-acceso";
    }

    @PostMapping("/retiros/salida")
    public String salidaRetiro(@RequestParam Long retiroId,
            @RequestParam(required = false) String retiradoPorNombre,
            @RequestParam(required = false) String retiradoPorIdentificacion,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        exigirPersonalAutorizado(request);
        try {
            retiroEstudianteService.registrarSalida(retiroId, retiradoPorNombre, retiradoPorIdentificacion);
            redirectAttributes.addFlashAttribute("successMsg", "Salida del estudiante registrada.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/control-de-acceso";
    }
}
