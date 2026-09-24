package com.chavescr.nexa.controller;

import com.chavescr.nexa.exception.InstitucionNoSeleccionadaException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chavescr.nexa.dto.FilaRubro;
import com.chavescr.nexa.dto.VistaComponente;
import com.chavescr.nexa.entity.AccionHistorial;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.ModuloAcademico;
import com.chavescr.nexa.security.CustomUserDetails;
import com.chavescr.nexa.service.AlcanceDocenteService;
import com.chavescr.nexa.service.ComponenteService;
import com.chavescr.nexa.service.HistorialCambioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class ComponenteController {

    private static final List<Perfil> PERFILES = List.of(
            new Perfil(ClaveComponente.COTIDIANO, ModuloAcademico.COTIDIANO,
                    "/gestion-academica/cotidiano/indicadores",
                    "/gestion-academica/cotidiano/evaluacion/modal", "indicadorId",
                    "indicadores-panel", "Indicador", "Nuevo Indicador",
                    "No hay indicadores definidos.",
                    "Debes crear al menos una sección y una materia para definir indicadores.",
                    "Indicador creado correctamente", "Indicador actualizado correctamente",
                    "Indicador eliminado correctamente", "cotidianoCalificado"),
            new Perfil(ClaveComponente.TAREA, ModuloAcademico.TAREA,
                    "/gestion-academica/tareas/definiciones",
                    "/gestion-academica/tareas/evaluacion/modal", "tareaId",
                    "tareas-definiciones-panel", "Tarea", "Nueva Tarea",
                    "No hay tareas asignadas.",
                    "Debes crear al menos una sección y una materia para asignar tareas.",
                    "Tarea creada correctamente", "Tarea actualizada correctamente",
                    "Tarea eliminada correctamente", "tareaCalificada"),
            new Perfil(ClaveComponente.PROYECTO, ModuloAcademico.PROYECTO,
                    "/gestion-academica/proyectos",
                    "/gestion-academica/proyectos/evaluacion/modal", "proyectoId",
                    "proyectos-panel", "Proyecto", "Nuevo Proyecto",
                    "No hay proyectos asignados.",
                    "Debes crear al menos una sección y una materia para asignar proyectos.",
                    "Proyecto creado correctamente", "Proyecto actualizado correctamente",
                    "Proyecto eliminado correctamente", "proyectoCalificado"),
            new Perfil(ClaveComponente.EXAMEN, ModuloAcademico.EXAMEN,
                    "/gestion-academica/examenes",
                    "/gestion-academica/examenes/evaluacion/modal", "examenId",
                    "examenes-panel", "Prueba", "Nueva Prueba",
                    "No hay pruebas programadas.",
                    "Debes crear al menos una sección y una materia para programar pruebas.",
                    "Prueba creada correctamente", "Prueba actualizada correctamente",
                    "Prueba eliminada correctamente", "examenCalificado"));

    private final ComponenteService service;
    private final AlcanceDocenteService alcanceDocenteService;
    private final HistorialCambioService historialService;

    public ComponenteController(ComponenteService service, AlcanceDocenteService alcanceDocenteService,
            HistorialCambioService historialService) {
        this.service = service;
        this.alcanceDocenteService = alcanceDocenteService;
        this.historialService = historialService;
    }

    @GetMapping({
            "/gestion-academica/cotidiano/indicadores",
            "/gestion-academica/tareas/definiciones",
            "/gestion-academica/proyectos",
            "/gestion-academica/examenes"
    })
    public String listar(@RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) Long materiaId, Model model, HttpSession session,
            HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarPanel(model, perfil(request), institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/componente/lista :: content";
    }

    @GetMapping({
            "/gestion-academica/cotidiano/indicadores/form",
            "/gestion-academica/tareas/definiciones/form",
            "/gestion-academica/proyectos/form",
            "/gestion-academica/examenes/form"
    })
    public String nuevo(@RequestParam Long nivelId, @RequestParam Long materiaId, Model model,
            HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        Perfil perfil = perfil(request);
        model.addAttribute("rubro", new Componente());
        model.addAttribute("vista", perfil.vista());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        cargarContextoPorcentaje(model, perfil, institucionId, nivelId, materiaId, null);
        return "gestion-academica/componente/formulario :: form-content";
    }

    @GetMapping({
            "/gestion-academica/cotidiano/indicadores/form/{id}",
            "/gestion-academica/tareas/definiciones/form/{id}",
            "/gestion-academica/proyectos/form/{id}",
            "/gestion-academica/examenes/form/{id}"
    })
    public String editar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        Perfil perfil = perfil(request);
        model.addAttribute("rubro", service.obtener(institucionId, id));
        model.addAttribute("vista", perfil.vista());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        cargarContextoPorcentaje(model, perfil, institucionId, nivelId, materiaId, id);
        return "gestion-academica/componente/formulario :: form-content";
    }

    @PostMapping({
            "/gestion-academica/cotidiano/indicadores",
            "/gestion-academica/tareas/definiciones",
            "/gestion-academica/proyectos",
            "/gestion-academica/examenes"
    })
    public String guardar(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @ModelAttribute Componente componente, Model model, HttpSession session,
            HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        Long institucionId = requerirInstitucion(session);
        Perfil perfil = perfil(request);
        boolean esNuevo = componente.getId() == null;
        try {
            Componente guardado = service.guardar(institucionId, perfil.clave(), nivelId, materiaId, componente);
            historialService.registrar(institucionId, nivelId, materiaId, perfil.modulo(), guardado.getId(),
                    guardado.getTitulo(), esNuevo ? AccionHistorial.CREAR : AccionHistorial.EDITAR,
                    usuario != null ? usuario.getId() : null, usuario != null ? usuario.getNombre() : null);
            notificarGuardado(response, esNuevo ? perfil.mensajeCreado() : perfil.mensajeActualizado());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, perfil, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/componente/lista :: content";
    }

    @DeleteMapping({
            "/gestion-academica/cotidiano/indicadores/{id}",
            "/gestion-academica/tareas/definiciones/{id}",
            "/gestion-academica/proyectos/{id}",
            "/gestion-academica/examenes/{id}"
    })
    public String eliminar(@PathVariable Long id, @RequestParam Long nivelId, @RequestParam Long materiaId,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        Long institucionId = requerirInstitucion(session);
        Perfil perfil = perfil(request);
        try {
            Componente componente = service.obtener(institucionId, id);
            service.eliminar(institucionId, id);
            historialService.registrar(institucionId, nivelId, materiaId, perfil.modulo(), componente.getId(),
                    componente.getTitulo(), AccionHistorial.ELIMINAR,
                    usuario != null ? usuario.getId() : null, usuario != null ? usuario.getNombre() : null);
            notificarGuardado(response, perfil.mensajeEliminado());
        } catch (IllegalArgumentException e) {
            notificarError(response, e.getMessage());
        }
        cargarPanel(model, perfil, institucionId, nivelId, materiaId, docenteIdSiAplica(request, session));
        return "gestion-academica/componente/lista :: content";
    }

    @GetMapping({
            "/gestion-academica/cotidiano/evaluacion/modal",
            "/gestion-academica/tareas/evaluacion/modal",
            "/gestion-academica/proyectos/evaluacion/modal",
            "/gestion-academica/examenes/evaluacion/modal"
    })
    public String modal(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @RequestParam(required = false) Long indicadorId, @RequestParam(required = false) Long tareaId,
            @RequestParam(required = false) Long proyectoId, @RequestParam(required = false) Long examenId,
            Model model, HttpSession session, HttpServletRequest request) {
        Long institucionId = requerirInstitucion(session);
        cargarModal(model, perfil(request), institucionId, nivelId, materiaId,
                componenteId(perfil(request), indicadorId, tareaId, proyectoId, examenId));
        return "gestion-academica/componente/evaluacion :: modal-content";
    }

    @PostMapping({
            "/gestion-academica/cotidiano/evaluacion/guardar-lote",
            "/gestion-academica/tareas/evaluacion/guardar-lote",
            "/gestion-academica/proyectos/evaluacion/guardar-lote",
            "/gestion-academica/examenes/evaluacion/guardar-lote"
    })
    public String guardarLote(@RequestParam Long nivelId, @RequestParam Long materiaId,
            @RequestParam(required = false) Long indicadorId, @RequestParam(required = false) Long tareaId,
            @RequestParam(required = false) Long proyectoId, @RequestParam(required = false) Long examenId,
            @RequestParam(required = false) List<Long> estudianteId,
            @RequestParam(required = false) List<String> calificacion,
            @RequestParam(required = false) List<String> puntosObtenidos,
            @RequestParam(required = false) List<String> observacion,
            Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails usuario) {
        exigirDocenteODirectorOAdmin(request);
        Long institucionId = requerirInstitucion(session);
        Perfil perfil = perfil(request);
        Long componenteId = componenteId(perfil, indicadorId, tareaId, proyectoId, examenId);

        List<String> errores = new ArrayList<>();
        List<String> calificados = new ArrayList<>();
        if (estudianteId != null) {
            for (int i = 0; i < estudianteId.size(); i++) {
                String calStr = valorEn(calificacion, i);
                String puntosStr = valorEn(puntosObtenidos, i);
                if ((calStr == null || calStr.isBlank()) && (puntosStr == null || puntosStr.isBlank())) {
                    continue;
                }
                String obs = valorEn(observacion, i);
                try {
                    Integer cal = calStr != null && !calStr.isBlank() ? Integer.valueOf(calStr.trim()) : null;
                    Integer puntos = puntosStr != null && !puntosStr.isBlank() ? Integer.valueOf(puntosStr.trim()) : null;
                    var fila = service.registrarNota(institucionId, componenteId, estudianteId.get(i), cal, puntos, obs);
                    calificados.add(fila.getEstudiante().getNombre() + ": " + fila.getCalificacion() + "%");
                } catch (NumberFormatException e) {
                    errores.add("un valor inválido");
                } catch (IllegalArgumentException e) {
                    errores.add(e.getMessage());
                }
            }
        }
        if (!calificados.isEmpty()) {
            String titulo = service.obtener(institucionId, componenteId).getTitulo();
            historialService.registrar(institucionId, nivelId, materiaId, perfil.modulo(), componenteId, titulo,
                    AccionHistorial.CALIFICAR, usuario != null ? usuario.getId() : null,
                    usuario != null ? usuario.getNombre() : null, String.join(", ", calificados));
        }
        String evento = "\"" + perfil.evento() + "\":{\"nivelId\":" + nivelId + ",\"materiaId\":" + materiaId + "}";
        if (!errores.isEmpty()) {
            String mensaje = errores.size() + " calificación(es) no se guardaron: "
                    + String.join("; ", errores.stream().distinct().toList());
            response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                    + "\"promedioDesactualizado\":\"\"," + evento + "}");
        } else {
            response.setHeader("HX-Trigger",
                    "{\"academicoGuardado\":{\"mensaje\":\"Calificaciones guardadas correctamente\"},"
                            + "\"promedioDesactualizado\":\"\"," + evento + "}");
        }
        cargarModal(model, perfil, institucionId, nivelId, materiaId, componenteId);
        return "gestion-academica/componente/evaluacion :: modal-content";
    }

    private void cargarModal(Model model, Perfil perfil, Long institucionId, Long nivelId, Long materiaId,
            Long componenteId) {
        var rubro = service.obtener(institucionId, componenteId);
        model.addAttribute("rubro", rubro);
        model.addAttribute("guardarUrl", perfil.guardarUrl());
        model.addAttribute("idParam", perfil.evalParam());
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("periodoActivo", service.periodoVisible(institucionId, rubro));
        model.addAttribute("filas", service.listarNotas(institucionId, componenteId));
    }

    private Long componenteId(Perfil perfil, Long indicadorId, Long tareaId, Long proyectoId, Long examenId) {
        Long id = switch (perfil.clave()) {
            case COTIDIANO -> indicadorId;
            case TAREA -> tareaId;
            case PROYECTO -> proyectoId;
            case EXAMEN -> examenId;
        };
        if (id == null) {
            throw new IllegalArgumentException("Debes indicar el componente a evaluar");
        }
        return id;
    }

    private String valorEn(List<String> valores, int indice) {
        return valores != null && indice < valores.size() ? valores.get(indice) : null;
    }

    private void cargarPanel(Model model, Perfil perfil, Long institucionId, Long nivelId, Long materiaId,
            Long docenteId) {
        var niveles = alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
        var materias = alcanceDocenteService.materiasVisibles(institucionId, docenteId);
        if (nivelId == null && !niveles.isEmpty()) {
            nivelId = niveles.get(0).getId();
        }
        if (materiaId == null && !materias.isEmpty()) {
            materiaId = materias.get(0).getId();
        }

        var periodoActivo = service.obtenerPeriodoActivoOpcional(institucionId);
        Long periodoId = periodoActivo != null ? periodoActivo.getId() : null;
        List<Componente> componentes = nivelId != null && materiaId != null
                ? service.listar(institucionId, perfil.clave(), nivelId, materiaId, periodoId)
                : List.of();
        var pesosEfectivos = service.calcularPesosEfectivos(componentes);
        double total = pesosEfectivos.values().stream().mapToDouble(Double::doubleValue).sum();

        List<Long> ids = componentes.stream().map(Componente::getId).toList();
        int totalEstudiantesSeccion = nivelId != null ? service.contarEstudiantesActivos(nivelId) : 0;
        var evaluados = service.contarEvaluados(institucionId, perfil.clave(), ids, periodoId);
        var promedios = service.calcularPromedio(institucionId, perfil.clave(), ids, periodoId);

        model.addAttribute("niveles", niveles);
        model.addAttribute("materias", materias);
        model.addAttribute("nivelId", nivelId);
        model.addAttribute("materiaId", materiaId);
        model.addAttribute("pesosEfectivos", pesosEfectivos);
        model.addAttribute("totalAsignado", total);
        model.addAttribute("periodoActivo", periodoActivo);
        model.addAttribute("totalEstudiantesSeccion", totalEstudiantesSeccion);
        model.addAttribute("vista", perfil.vista());
        model.addAttribute("rubros", componentes.stream()
                .map(c -> new FilaRubro(c.getId(), c.getTitulo(), c.getFecha(), c.isPonderado(),
                        c.getPuntosTotales(), promedios.get(c.getId()), pesosEfectivos.get(c.getId()),
                        evaluados.get(c.getId())))
                .toList());
    }

    private void cargarContextoPorcentaje(Model model, Perfil perfil, Long institucionId, Long nivelId,
            Long materiaId, Long componenteId) {
        var periodoActivo = service.obtenerPeriodoActivoOpcional(institucionId);
        List<Componente> componentes = service.listar(institucionId, perfil.clave(), nivelId, materiaId,
                periodoActivo != null ? periodoActivo.getId() : null);
        int sumaFijosOtros = componentes.stream()
                .filter(c -> componenteId == null || !c.getId().equals(componenteId))
                .filter(c -> c.getPorcentaje() != null)
                .mapToInt(Componente::getPorcentaje)
                .sum();
        long ponderadosOtros = componentes.stream()
                .filter(c -> componenteId == null || !c.getId().equals(componenteId))
                .filter(Componente::isPonderado)
                .count();
        int topeFijos = ponderadosOtros > 0 ? 99 : 100;
        int disponible = Math.max(0, topeFijos - sumaFijosOtros);
        long nPonderados = ponderadosOtros + 1;
        model.addAttribute("porcentajeDisponible", disponible);
        model.addAttribute("ponderadosOtros", ponderadosOtros);
        model.addAttribute("sinCupoPorcentaje", sumaFijosOtros >= 100);
        model.addAttribute("pesoPonderadoEstimado",
                nPonderados == 0 ? 0.0 : Math.max(0, 100 - sumaFijosOtros) / (double) nPonderados);
    }

    private Perfil perfil(HttpServletRequest request) {
        String path = request.getServletPath();
        return PERFILES.stream()
                .filter(perfil -> perfil.atiende(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Componente no reconocido"));
    }

    private void notificarGuardado(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoGuardado\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"},"
                + "\"promedioDesactualizado\":\"\"}");
    }

    private void notificarError(HttpServletResponse response, String mensaje) {
        response.setHeader("HX-Trigger", "{\"academicoError\":{\"mensaje\":\"" + escaparJson(mensaje) + "\"}}");
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Long requerirInstitucion(HttpSession session) {
        Long id = (Long) session.getAttribute("SESSION_INSTITUCION_ID");
        if (id == null) {
            throw new InstitucionNoSeleccionadaException();
        }
        return id;
    }

    private Long docenteIdSiAplica(HttpServletRequest request, HttpSession session) {
        boolean soloDocente = request.isUserInRole("ROLE_DOCENTE")
                && !request.isUserInRole("ROLE_ADMIN")
                && !request.isUserInRole("ROLE_DIRECTOR");
        return soloDocente ? (Long) session.getAttribute("SESSION_USUARIO_ID") : null;
    }

    private void exigirDocenteODirectorOAdmin(HttpServletRequest request) {
        if (!request.isUserInRole("ROLE_DOCENTE") && !request.isUserInRole("ROLE_DIRECTOR")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Solo docentes, directores o administradores pueden evaluar");
        }
    }

    private record Perfil(ClaveComponente clave, ModuloAcademico modulo, String baseUrl, String evalUrl,
            String evalParam, String panelId, String etiqueta, String botonNuevo, String mensajeVacio,
            String mensajeSinSeccion, String mensajeCreado, String mensajeActualizado, String mensajeEliminado,
            String evento) {

        VistaComponente vista() {
            return new VistaComponente(panelId, baseUrl, baseUrl, evalUrl, evalParam, etiqueta, botonNuevo,
                    mensajeVacio, mensajeSinSeccion);
        }

        String guardarUrl() {
            return evalUrl.substring(0, evalUrl.lastIndexOf('/')) + "/guardar-lote";
        }

        boolean atiende(String path) {
            int desde = "/gestion-academica/".length();
            int corte = baseUrl.indexOf('/', desde);
            String prefijo = corte < 0 ? baseUrl : baseUrl.substring(0, corte);
            return path.startsWith(prefijo);
        }
    }
}
