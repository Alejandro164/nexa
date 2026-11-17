package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.ContextoAsistente;
import com.chavescr.nexa.dto.PanelInicio;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Herramientas de solo lectura que el Chat Asistente puede usar. Cada una consulta los mismos servicios
 * que la aplicación y aplica los permisos de quien pregunta:
 * <ul>
 * <li>Administración, dirección y System Config: toda la dirección.</li>
 * <li>Docente: sus lecciones y los estudiantes de las secciones donde es profesor guía (igual que en Notas).</li>
 * <li>Padre o madre: sus hijos activos en la dirección.</li>
 * <li>Estudiante: él mismo.</li>
 * </ul>
 * Las herramientas que un rol no puede usar ni siquiera se le ofrecen al modelo.
 */
@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class AsistenteHerramientas {

    static final String RESUMEN_DE_HOY = "resumen_de_hoy";
    static final String RENDIMIENTO = "rendimiento_del_periodo";
    static final String BUSCAR_ESTUDIANTES = "buscar_estudiantes";
    static final String RESUMEN_ESTUDIANTE = "resumen_de_estudiante";
    static final String LECCIONES = "lecciones_del_dia";
    static final String AGENDA = "agenda";

    private static final List<String> DIAS = List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");
    private static final int MAX_ESTUDIANTES = 30;
    private static final int MAX_DIAS_AGENDA = 62;
    private static final int MAX_EVENTOS = 60;

    private final DashboardService dashboardService;
    private final UsuarioRepository usuarioRepository;
    private final DocenteGuiaService docenteGuiaService;
    private final ConfiguracionAcademicaService configuracionAcademicaService;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public AsistenteHerramientas(DashboardService dashboardService, UsuarioRepository usuarioRepository,
            DocenteGuiaService docenteGuiaService, ConfiguracionAcademicaService configuracionAcademicaService) {
        this.dashboardService = dashboardService;
        this.usuarioRepository = usuarioRepository;
        this.docenteGuiaService = docenteGuiaService;
        this.configuracionAcademicaService = configuracionAcademicaService;
    }

    /** Error que se le devuelve al modelo como resultado de la herramienta (is_error), no al usuario. */
    public static class ErrorHerramienta extends RuntimeException {
        public ErrorHerramienta(String mensaje) {
            super(mensaje);
        }
    }

    // ── Definiciones ─────────────────────────────────────────────────────────

    /**
     * Herramientas que puede usar este usuario, en el formato de Chat Completions
     * ({@code {"type": "function", "function": {...}}}), siempre en el mismo orden para aprovechar la caché.
     */
    public List<Map<String, Object>> definiciones(ContextoAsistente ctx) {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(herramienta(RESUMEN_DE_HOY,
                "Resumen del día del usuario, igual al inicio de Nexa. Dirección: asistencia de hoy, lecciones "
                        + "sin lista, pendientes (entregas por revisar, apelaciones, boletas sin resolver, visitas y "
                        + "retiros por autorizar), período y agenda de 7 días. Docente: sus lecciones de hoy, "
                        + "pendientes, su sección guía, período y agenda. Familia o estudiante: notas, ausencias, "
                        + "conducta y novedades de hoy de cada estudiante, comunicados sin leer, período y agenda.",
                Map.of(), List.of()));
        if (ctx.esGestion()) {
            tools.add(herramienta(RENDIMIENTO,
                    "Rendimiento de toda la dirección en el período vigente: estudiantes con materias o conducta "
                            + "bajo la nota de aprobación de su ciclo (los 8 más críticos y el total) y las 5 "
                            + "secciones con el promedio más bajo.",
                    Map.of(), List.of()));
        }
        tools.add(herramienta(BUSCAR_ESTUDIANTES,
                "Busca estudiantes que el usuario puede consultar, por nombre o sección. Devuelve id, nombre y "
                        + "sección (máximo 30). Úsala para obtener el id antes de pedir el resumen de un estudiante.",
                Map.of(
                        "nombre", Map.of("type", "string", "description", "Parte del nombre o apellido."),
                        "seccion", Map.of("type", "string", "description", "Grado y sección, por ejemplo \"8-2\".")),
                List.of()));
        tools.add(herramienta(RESUMEN_ESTUDIANTE,
                "Cómo va un estudiante en el período vigente: nota de cada materia y si está aprobada, promedio, "
                        + "nota de aprobación de su ciclo, nota de conducta, ausencias y tardías sin justificar, "
                        + "llamadas de atención, boletas y ausencias o tardías de hoy.",
                Map.of("estudiante_id", Map.of("type", "integer", "description", "Id del estudiante.")),
                List.of("estudiante_id")));
        tools.add(herramienta(LECCIONES,
                "Lecciones del horario de un día de la semana en el período vigente, con hora, materia, sección, "
                        + "docente y aula. Si el día es hoy, también si ya se pasó lista. "
                        + (ctx.esGestion() ? "Indica la sección para ver solo esa; sin sección devuelve todas. "
                                : "")
                        + (ctx.esDocente() && !ctx.esGestion() ? "Para un docente son sus propias lecciones. " : "")
                        + (ctx.esPadre() ? "Para la familia, indica el estudiante_id del hijo. " : ""),
                Map.of(
                        "dia", Map.of("type", "string", "enum", DIAS,
                                "description", "Día de la semana. Sin indicarlo, hoy."),
                        "seccion", Map.of("type", "string", "description", "Grado y sección, por ejemplo \"8-2\"."),
                        "estudiante_id", Map.of("type", "integer",
                                "description", "Estudiante cuyo horario se consulta (familia).")),
                List.of()));
        tools.add(herramienta(AGENDA,
                "Actividades de la agenda institucional y del calendario del MEP entre dos fechas (máximo 62 días)"
                        + (ctx.esGestion() || ctx.esDocente() ? ", con las tareas del plan de trabajo y los "
                                + "recordatorios del usuario." : ", con los recordatorios del usuario."),
                Map.of(
                        "desde", Map.of("type", "string", "format", "date",
                                "description", "Fecha inicial AAAA-MM-DD. Sin indicarla, hoy."),
                        "hasta", Map.of("type", "string", "format", "date",
                                "description", "Fecha final AAAA-MM-DD. Sin indicarla, 7 días después de desde.")),
                List.of()));
        return tools;
    }

    private static Map<String, Object> herramienta(String nombre, String descripcion,
            Map<String, Object> propiedades, List<String> requeridos) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("type", "object");
        parametros.put("properties", ordenado(propiedades));
        parametros.put("required", requeridos);
        parametros.put("additionalProperties", false);
        Map<String, Object> funcion = new LinkedHashMap<>();
        funcion.put("name", nombre);
        funcion.put("description", descripcion);
        funcion.put("parameters", parametros);
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", funcion);
        return tool;
    }

    /** Los Map.of no tienen orden fijo: se pasan a TreeMap para que el esquema sea siempre igual. */
    private static Object ordenado(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> ordenadoMapa = new TreeMap<>();
            mapa.forEach((k, v) -> ordenadoMapa.put(String.valueOf(k), ordenado(v)));
            return ordenadoMapa;
        }
        return valor;
    }

    // ── Ejecución ────────────────────────────────────────────────────────────

    /**
     * Ejecuta una herramienta y devuelve el resultado en JSON.
     *
     * @throws ErrorHerramienta si falta un dato, no existe o el usuario no tiene permiso.
     */
    public String ejecutar(String nombre, Map<String, Object> entrada, ContextoAsistente ctx) {
        Map<String, Object> e = entrada == null ? Map.of() : entrada;
        Object resultado = switch (nombre) {
            case RESUMEN_DE_HOY -> resumenDeHoy(ctx);
            case RENDIMIENTO -> rendimiento(ctx);
            case BUSCAR_ESTUDIANTES -> buscarEstudiantes(ctx, texto(e.get("nombre")), texto(e.get("seccion")));
            case RESUMEN_ESTUDIANTE -> resumenEstudiante(ctx, numero(e.get("estudiante_id")));
            case LECCIONES -> lecciones(ctx, texto(e.get("dia")), texto(e.get("seccion")),
                    numero(e.get("estudiante_id")));
            case AGENDA -> agenda(ctx, fecha(e.get("desde")), fecha(e.get("hasta")));
            default -> throw new ErrorHerramienta("La herramienta " + nombre + " no existe.");
        };
        try {
            return json.writeValueAsString(resultado);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo convertir el resultado de " + nombre, ex);
        }
    }

    private Object resumenDeHoy(ContextoAsistente ctx) {
        if (ctx.esGestion()) {
            return dashboardService.direccion(ctx.direccionId(), ctx.usuarioId());
        }
        if (ctx.esDocente()) {
            return dashboardService.docente(ctx.direccionId(), ctx.usuarioId());
        }
        if (ctx.esPadre() || ctx.esEstudiante()) {
            return dashboardService.familia(ctx.direccionId(), ctx.usuarioId(), !ctx.esPadre());
        }
        throw new ErrorHerramienta("El usuario no tiene un rol con resumen del día.");
    }

    private Object rendimiento(ContextoAsistente ctx) {
        if (!ctx.esGestion()) {
            throw new ErrorHerramienta("Solo la dirección puede ver el rendimiento de toda la institución.");
        }
        return dashboardService.rendimiento(ctx.direccionId());
    }

    private Object buscarEstudiantes(ContextoAsistente ctx, String nombre, String seccion) {
        Long nivelId = seccion == null ? null : nivelDe(ctx, seccion).getId();
        String buscado = nombre == null ? null : normalizar(nombre);
        List<Map<String, Object>> encontrados = new ArrayList<>();
        int total = 0;
        for (Usuario u : estudiantesVisibles(ctx).values()) {
            if (nivelId != null && !nivelId.equals(u.getNivelAcademico().getId())) {
                continue;
            }
            if (buscado != null && !normalizar(u.getNombre()).contains(buscado)) {
                continue;
            }
            total++;
            if (encontrados.size() < MAX_ESTUDIANTES) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("id", u.getId());
                fila.put("nombre", u.getNombre());
                fila.put("seccion", seccionDe(u.getNivelAcademico()));
                encontrados.add(fila);
            }
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", total);
        respuesta.put("estudiantes", encontrados);
        if (total > encontrados.size()) {
            respuesta.put("nota", "Hay más resultados; afina la búsqueda por nombre o sección.");
        }
        return respuesta;
    }

    private Object resumenEstudiante(ContextoAsistente ctx, Long estudianteId) {
        Usuario estudiante = estudianteVisible(ctx, estudianteId);
        List<PanelInicio.Estudiante> resumen = dashboardService.resumenEstudiantes(ctx.direccionId(),
                List.of(estudiante));
        return resumen.isEmpty() ? Map.of("nota", "No hay datos del período para este estudiante.") : resumen.get(0);
    }

    private Object lecciones(ContextoAsistente ctx, String dia, String seccion, Long estudianteId) {
        String elegido = dia == null ? DashboardService.diaDeHoy() : dia.toUpperCase(Locale.ROOT);
        if (elegido.isEmpty()) {
            return Map.of("nota", "Hoy es domingo: no hay lecciones.");
        }
        if (!DIAS.contains(elegido)) {
            throw new ErrorHerramienta("El día debe ser uno de " + DIAS + ".");
        }
        Long docenteId = null;
        Long nivelId;
        if (ctx.esGestion()) {
            nivelId = seccion != null ? nivelDe(ctx, seccion).getId()
                    : estudianteId != null ? estudianteVisible(ctx, estudianteId).getNivelAcademico().getId() : null;
        } else if (ctx.esDocente()) {
            docenteId = ctx.usuarioId();
            nivelId = null;
        } else if (ctx.esPadre() || ctx.esEstudiante()) {
            Usuario estudiante = estudianteId != null ? estudianteVisible(ctx, estudianteId) : unicoEstudiante(ctx);
            nivelId = estudiante.getNivelAcademico().getId();
        } else {
            throw new ErrorHerramienta("El usuario no tiene un horario que consultar.");
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("dia", elegido);
        respuesta.put("esHoy", elegido.equals(DashboardService.diaDeHoy()));
        respuesta.put("lecciones", dashboardService.leccionesDelDia(ctx.direccionId(), elegido, docenteId, nivelId));
        return respuesta;
    }

    private Object agenda(ContextoAsistente ctx, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now(DashboardService.ZONA);
        LocalDate fin = hasta != null ? hasta : inicio.plusDays(DashboardService.DIAS_AGENDA);
        if (fin.isBefore(inicio)) {
            throw new ErrorHerramienta("La fecha final es anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) > MAX_DIAS_AGENDA) {
            throw new ErrorHerramienta("El rango máximo es de " + MAX_DIAS_AGENDA + " días.");
        }
        boolean conTareas = ctx.esGestion() || ctx.esDocente();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("desde", inicio);
        respuesta.put("hasta", fin);
        respuesta.put("eventos", dashboardService.agendaEntre(ctx.direccionId(), ctx.usuarioId(), inicio, fin,
                conTareas, MAX_EVENTOS));
        return respuesta;
    }

    // ── Alcance ──────────────────────────────────────────────────────────────

    /** Estudiantes activos con sección que este usuario puede consultar, por id. */
    Map<Long, Usuario> estudiantesVisibles(ContextoAsistente ctx) {
        Map<Long, Usuario> visibles = new LinkedHashMap<>();
        Long direccionId = ctx.direccionId();
        if (ctx.esGestion()) {
            usuarioRepository.findEstudiantesActivosConNivel(direccionId, null, null)
                    .forEach(u -> visibles.put(u.getId(), u));
        }
        if (ctx.esDocente()) {
            for (NivelAcademico nivel : docenteGuiaService.listarSecciones(direccionId, ctx.usuarioId())) {
                usuarioRepository.findEstudiantesActivosConNivel(direccionId, null, nivel.getId())
                        .forEach(u -> visibles.put(u.getId(), u));
            }
        }
        if (ctx.esPadre()) {
            for (Usuario hijo : usuarioRepository.findEstudiantesByPadreId(ctx.usuarioId())) {
                usuarioRepository.findEstudianteActivoConNivel(hijo.getId(), direccionId)
                        .ifPresent(u -> visibles.put(u.getId(), u));
            }
        }
        if (ctx.esEstudiante()) {
            usuarioRepository.findEstudianteActivoConNivel(ctx.usuarioId(), direccionId)
                    .ifPresent(u -> visibles.put(u.getId(), u));
        }
        visibles.values().removeIf(u -> u.getNivelAcademico() == null);
        return visibles;
    }

    private Usuario estudianteVisible(ContextoAsistente ctx, Long estudianteId) {
        if (estudianteId == null) {
            throw new ErrorHerramienta("Falta el estudiante_id; búscalo primero con " + BUSCAR_ESTUDIANTES + ".");
        }
        Usuario estudiante = estudiantesVisibles(ctx).get(estudianteId);
        if (estudiante == null) {
            throw new ErrorHerramienta("El usuario no tiene acceso a ese estudiante, o no existe en esta dirección.");
        }
        return estudiante;
    }

    private Usuario unicoEstudiante(ContextoAsistente ctx) {
        Map<Long, Usuario> visibles = estudiantesVisibles(ctx);
        if (visibles.size() == 1) {
            return visibles.values().iterator().next();
        }
        if (visibles.isEmpty()) {
            throw new ErrorHerramienta("No hay estudiantes con sección asociados a este usuario.");
        }
        throw new ErrorHerramienta("Hay varios estudiantes; indica el estudiante_id.");
    }

    private NivelAcademico nivelDe(ContextoAsistente ctx, String seccion) {
        String buscada = seccion.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        Optional<NivelAcademico> nivel = configuracionAcademicaService.listarNivelesActivos(ctx.direccionId())
                .stream()
                .filter(n -> seccionDe(n).toUpperCase(Locale.ROOT).equals(buscada))
                .findFirst();
        return nivel.orElseThrow(() -> new ErrorHerramienta("No existe la sección " + seccion
                + " en esta dirección. Usa el formato grado-sección, por ejemplo 8-2."));
    }

    private static String seccionDe(NivelAcademico nivel) {
        return nivel.getGrado() + "-" + nivel.getSeccion();
    }

    // ── Entradas ─────────────────────────────────────────────────────────────

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static Long numero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (NumberFormatException e) {
            throw new ErrorHerramienta("Se esperaba un número y llegó \"" + valor + "\".");
        }
    }

    private static LocalDate fecha(Object valor) {
        String texto = texto(valor);
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new ErrorHerramienta("La fecha \"" + texto + "\" debe tener el formato AAAA-MM-DD.");
        }
    }

    /** Sin tildes y en minúscula, para buscar nombres. */
    private static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Nombres de las herramientas que recibe este usuario (para pruebas y registros). */
    public Set<String> nombres(ContextoAsistente ctx) {
        Set<String> nombres = new java.util.LinkedHashSet<>();
        definiciones(ctx).forEach(t -> nombres.add(String.valueOf(((Map<?, ?>) t.get("function")).get("name"))));
        return nombres;
    }
}
