package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.DireccionNoSeleccionadaException;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.dto.FilaAsistencia;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.AsistenciaService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/gestion-academica/asistencia")
public class AsistenciaController {

    private static final String FRAGMENTO = "gestion-academica/asistencia/asistencia :: content";
    private static final Map<DayOfWeek, String> DIA_ES = Map.of(
            DayOfWeek.MONDAY, "LUNES",
            DayOfWeek.TUESDAY, "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY, "JUEVES",
            DayOfWeek.FRIDAY, "VIERNES",
            DayOfWeek.SATURDAY, "SABADO",
            DayOfWeek.SUNDAY, "DOMINGO");

    private final AsistenciaService service;
    private final AlcanceDocenteService alcanceDocenteService;

    public AsistenciaController(AsistenciaService service, AlcanceDocenteService alcanceDocenteService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @GetMapping
    public String asistencia(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Integer numeroLeccion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, HttpSession session, HttpServletRequest request) {
        cargarPanel(model, requerirDireccion(session), nivelId, materiaId, numeroLeccion, fecha,
                docenteIdSiAplica(request, session));
        return FRAGMENTO;
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Long estudianteId,
            @RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) boolean pasarLista,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        Long registradoPorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            exigirLeccionACargo(direccionId, docenteIdSiAplica(request, session), nivelId, materiaId, numeroLeccion,
                    fecha);
            service.registrarEstado(direccionId, estudianteId, nivelId, materiaId, numeroLeccion, fecha, estado,
                    observaciones, registradoPorId);
            notificarPromedioDesactualizado(response);
        } catch (DataIntegrityViolationException e) {
            // otra petición concurrente insertó el registro primero; reintentar una vez ya que existe
            try {
                service.registrarEstado(direccionId, estudianteId, nivelId, materiaId, numeroLeccion, fecha, estado,
                        observaciones, registradoPorId);
                notificarPromedioDesactualizado(response);
            } catch (IllegalArgumentException e2) {
                model.addAttribute("error", e2.getMessage());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, direccionId, nivelId, materiaId, numeroLeccion, fecha, docenteIdSiAplica(request, session));
        if (pasarLista) {
            model.addAttribute("pasarListaAbierta", true);
        }
        return FRAGMENTO;
    }

    @PostMapping("/documento")
    public String subirDocumento(@RequestParam Long estudianteId,
            @RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("archivo") MultipartFile archivo,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        try {
            exigirLeccionACargo(direccionId, docenteIdSiAplica(request, session), nivelId, materiaId, numeroLeccion,
                    fecha);
            service.guardarDocumento(direccionId, estudianteId, nivelId, materiaId, numeroLeccion, fecha, archivo);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, direccionId, nivelId, materiaId, numeroLeccion, fecha, docenteIdSiAplica(request, session));
        return FRAGMENTO;
    }

    @GetMapping("/documento")
    public ResponseEntity<Resource> verDocumento(@RequestParam Long estudianteId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpSession session, HttpServletRequest request) {
        exigirDocenteODirectorOAdmin(request);
        try {
            var documento = service.obtenerDocumento(requerirDireccion(session), estudianteId, materiaId,
                    numeroLeccion, fecha);
            if (!Files.isReadable(documento.ruta())) {
                return ResponseEntity.notFound().build();
            }
            MediaType tipo = MediaTypeFactory.getMediaType(documento.nombre()).orElse(MediaType.APPLICATION_OCTET_STREAM);
            boolean enLinea = MediaType.APPLICATION_PDF.includes(tipo) || "image".equals(tipo.getType());
            ContentDisposition disposicion = (enLinea ? ContentDisposition.inline() : ContentDisposition.attachment())
                    .filename(documento.nombre(), StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .contentType(tipo)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new UrlResource(documento.ruta().toUri()));
        } catch (IllegalArgumentException | MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/copiar-leccion-anterior")
    public String copiarLeccionAnterior(@RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        Long docenteId = docenteIdSiAplica(request, session);
        try {
            exigirLeccionACargo(direccionId, docenteId, nivelId, materiaId, numeroLeccion, fecha);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            cargarPanel(model, direccionId, nivelId, materiaId, numeroLeccion, fecha, docenteId);
            return FRAGMENTO;
        }
        var periodo = service.obtenerUltimoPeriodoActivo(direccionId);
        if (service.validarPeriodoParaAsistencia(direccionId, fecha, periodo) == null) {
            String dia = DIA_ES.get(fecha.getDayOfWeek());
            var lecciones = alcanceDocenteService.leccionesVisiblesEnPeriodo(
                    direccionId, periodo.getId(), nivelId, materiaId, dia, docenteId);
            Integer leccionOrigen = leccionAnterior(lecciones, numeroLeccion);
            if (leccionOrigen == null) {
                model.addAttribute("error", "No hay una lección anterior de la que copiar.");
            } else {
                Long registradoPorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
                int copiados = service.copiarDeLeccionAnterior(direccionId, nivelId, materiaId, fecha, leccionOrigen,
                        numeroLeccion, registradoPorId);
                if (copiados == 0) {
                    model.addAttribute("error", "No hay asistencia registrada en la lección anterior para copiar.");
                } else {
                    notificarPromedioDesactualizado(response);
                }
            }
        }
        cargarPanel(model, direccionId, nivelId, materiaId, numeroLeccion, fecha, docenteId);
        return FRAGMENTO;
    }

    @PostMapping("/marcar-pendientes")
    public String marcarPendientes(@RequestParam Long nivelId,
            @RequestParam Long materiaId,
            @RequestParam Integer numeroLeccion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        exigirDocenteODirectorOAdmin(request);
        Long direccionId = requerirDireccion(session);
        Long registradoPorId = (Long) session.getAttribute("SESSION_USUARIO_ID");
        try {
            exigirLeccionACargo(direccionId, docenteIdSiAplica(request, session), nivelId, materiaId, numeroLeccion,
                    fecha);
            marcarPendientesPresentes(response, direccionId, nivelId, materiaId, numeroLeccion, fecha,
                    registradoPorId);
        } catch (DataIntegrityViolationException e) {
            try {
                marcarPendientesPresentes(response, direccionId, nivelId, materiaId, numeroLeccion, fecha,
                        registradoPorId);
            } catch (IllegalArgumentException e2) {
                model.addAttribute("error", e2.getMessage());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        cargarPanel(model, direccionId, nivelId, materiaId, numeroLeccion, fecha, docenteIdSiAplica(request, session));
        return FRAGMENTO;
    }

    private void marcarPendientesPresentes(HttpServletResponse response, Long direccionId, Long nivelId,
            Long materiaId, Integer numeroLeccion, LocalDate fecha, Long registradoPorId) {
        int marcados = service.marcarPendientesPresentes(direccionId, nivelId, materiaId, fecha, numeroLeccion,
                registradoPorId);
        if (marcados > 0) {
            notificarPromedioDesactualizado(response);
        }
    }

    private void notificarPromedioDesactualizado(HttpServletResponse response) {
        response.setHeader("HX-Trigger", "promedioDesactualizado");
    }

    private void cargarPanel(Model model, Long direccionId, Long nivelId, Long materiaId, Integer numeroLeccion,
            LocalDate fecha, Long docenteId) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }

        var periodo = service.obtenerUltimoPeriodoActivo(direccionId);
        String avisoPeriodo = service.validarPeriodoParaAsistencia(direccionId, fecha, periodo);

        List<Materia> materias = List.of();
        List<NivelAcademico> secciones = List.of();
        List<Integer> lecciones = List.of();
        Integer leccionAnterior = null;
        List<FilaAsistencia> filas = List.of();
        String mensajeVacio = null;

        if (avisoPeriodo == null) {
            Long periodoId = periodo.getId();
            String dia = DIA_ES.get(fecha.getDayOfWeek());

            materias = alcanceDocenteService.materiasVisiblesEnPeriodo(direccionId, periodoId, docenteId);
            materiaId = elegirId(materiaId, materias, Materia::getId);

            secciones = alcanceDocenteService.nivelesVisiblesEnPeriodoPorMateria(
                    direccionId, periodoId, materiaId, docenteId);
            nivelId = elegirId(nivelId, secciones, NivelAcademico::getId);

            lecciones = alcanceDocenteService.leccionesVisiblesEnPeriodo(
                    direccionId, periodoId, nivelId, materiaId, dia, docenteId);
            if (numeroLeccion == null || !lecciones.contains(numeroLeccion)) {
                numeroLeccion = lecciones.isEmpty() ? null : lecciones.get(0);
            }
            leccionAnterior = leccionAnterior(lecciones, numeroLeccion);
            filas = nivelId != null && materiaId != null && numeroLeccion != null
                    ? service.listarFilas(direccionId, nivelId, fecha, materiaId, numeroLeccion)
                    : List.of();
            mensajeVacio = mensajeTablaVacia(materias, secciones, lecciones, filas);
        }

        model.addAttribute("fecha", fecha);
        model.addAttribute("periodoActivo", periodo);
        model.addAttribute("avisoPeriodo", avisoPeriodo);
        model.addAttribute("secciones", secciones);
        model.addAttribute("materias", materias);
        model.addAttribute("lecciones", lecciones);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("numeroLeccion", numeroLeccion);
        model.addAttribute("leccionAnterior", leccionAnterior);
        model.addAttribute("filas", filas);
        model.addAttribute("mensajeVacio", mensajeVacio);
    }

    private static String mensajeTablaVacia(List<Materia> materias, List<NivelAcademico> secciones,
            List<Integer> lecciones, List<?> filas) {
        if (materias.isEmpty()) {
            return "No hay materias con lecciones registradas en el período activo.";
        }
        if (secciones.isEmpty()) {
            return "Esta materia no tiene secciones asignadas en el período activo.";
        }
        if (lecciones.isEmpty()) {
            return "Esta materia no tiene lección programada ese día en esta sección.";
        }
        if (filas.isEmpty()) {
            return "No hay estudiantes activos en esta sección.";
        }
        return null;
    }

    private static <T> Long elegirId(Long solicitado, List<T> items, Function<T, Long> idDe) {
        if (solicitado != null && items.stream().anyMatch(item -> idDe.apply(item).equals(solicitado))) {
            return solicitado;
        }
        return items.isEmpty() ? null : idDe.apply(items.get(0));
    }

    /** Lección consecutiva anterior (actual - 1) si también existe para esta materia/sección. */
    private Integer leccionAnterior(List<Integer> lecciones, Integer actual) {
        if (actual == null) {
            return null;
        }
        int anterior = actual - 1;
        return lecciones.contains(anterior) ? anterior : null;
    }

    private Long requerirDireccion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_DIRECCION_ID");
        if (id == null) {
            throw new DireccionNoSeleccionadaException();
        }
        return id;
    }

    private void exigirDocenteODirectorOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden registrar asistencia");
        }
    }

    private void exigirLeccionACargo(Long direccionId, Long docenteId, Long nivelId, Long materiaId,
            Integer numeroLeccion, LocalDate fecha) {
        if (docenteId == null) {
            return;
        }
        var periodo = service.obtenerUltimoPeriodoActivo(direccionId);
        if (periodo == null || fecha == null) {
            return;
        }
        String dia = DIA_ES.get(fecha.getDayOfWeek());
        List<Integer> lecciones = alcanceDocenteService.leccionesVisiblesEnPeriodo(
                direccionId, periodo.getId(), nivelId, materiaId, dia, docenteId);
        if (numeroLeccion == null || !lecciones.contains(numeroLeccion)) {
            throw new IllegalArgumentException("Esta lección no está a su cargo");
        }
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }
}
