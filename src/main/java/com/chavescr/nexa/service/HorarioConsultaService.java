package com.chavescr.nexa.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.ConsultaHorario;
import com.chavescr.nexa.entity.ConfiguracionDireccion;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class HorarioConsultaService {

    private static final String ROL_ADMIN = "ROLE_ADMIN";
    private static final String ROL_DIRECTOR = "ROLE_DIRECTOR";
    private static final String ROL_DOCENTE = "ROLE_DOCENTE";
    private static final String ROL_ESTUDIANTE = "ROLE_ESTUDIANTE";

    private final ConfiguracionAcademicaService configuracionAcademicaService;
    private final HorarioLeccionRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;

    public HorarioConsultaService(ConfiguracionAcademicaService configuracionAcademicaService,
            HorarioLeccionRepository horarioRepository, UsuarioRepository usuarioRepository) {
        this.configuracionAcademicaService = configuracionAcademicaService;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public ConsultaHorario consultar(Long direccionId, Long usuarioId, Set<String> roles, String vistaPedida,
            Long periodoId, Long personaId) {
        boolean gestion = roles.contains(ROL_ADMIN) || roles.contains(ROL_DIRECTOR);
        boolean docente = !gestion && roles.contains(ROL_DOCENTE);
        boolean estudiante = !gestion && !docente && roles.contains(ROL_ESTUDIANTE);
        if (!gestion && !docente && !estudiante) {
            throw new AccessDeniedException("No tienes permiso para consultar el horario");
        }

        boolean puedeVerDocentes = gestion || docente;
        boolean puedeVerEstudiantes = true;
        String vista = ConsultaHorario.VISTA_ESTUDIANTES.equals(vistaPedida)
                ? ConsultaHorario.VISTA_ESTUDIANTES
                : ConsultaHorario.VISTA_DOCENTES;
        if (!puedeVerDocentes) {
            vista = ConsultaHorario.VISTA_ESTUDIANTES;
        }

        List<PeriodoAcademico> periodos = configuracionAcademicaService.listarPeriodosActivos(direccionId);
        PeriodoAcademico periodo = elegir(periodos, periodoId, PeriodoAcademico::getId);
        ConfiguracionDireccion config = configuracionAcademicaService.obtenerConfiguracion(direccionId);
        String diaHoy = diaDeHoy();

        if (periodo == null) {
            return armar(vista, puedeVerDocentes, puedeVerEstudiantes, false, "Horario",
                    "Consulta semanal", ayuda(vista),
                    "No hay un período activo. Cuando exista, el horario de la semana aparece aquí.",
                    periodos, null, List.of(), null, config, Map.of(), diaHoy);
        }
        if (config.getDias().isEmpty() || config.getLecciones().isEmpty()) {
            return armar(vista, puedeVerDocentes, puedeVerEstudiantes, false, "Horario",
                    null, ayuda(vista),
                    "La jornada de esta dirección todavía no tiene días ni lecciones.",
                    periodos, periodo, List.of(), null, config, Map.of(), diaHoy);
        }

        if (ConsultaHorario.VISTA_DOCENTES.equals(vista)) {
            return consultarDocente(direccionId, usuarioId, gestion, puedeVerDocentes, puedeVerEstudiantes,
                    periodos, periodo, personaId, config, diaHoy);
        }
        return consultarEstudiantes(direccionId, usuarioId, gestion, docente, puedeVerDocentes, puedeVerEstudiantes,
                periodos, periodo, personaId, config, diaHoy);
    }

    private ConsultaHorario consultarDocente(Long direccionId, Long usuarioId, boolean gestion,
            boolean puedeVerDocentes, boolean puedeVerEstudiantes, List<PeriodoAcademico> periodos,
            PeriodoAcademico periodo, Long personaId, ConfiguracionDireccion config, String diaHoy) {
        List<Usuario> docentes = configuracionAcademicaService.listarDocentes(direccionId);
        if (docentes.isEmpty()) {
            return armar(ConsultaHorario.VISTA_DOCENTES, puedeVerDocentes, puedeVerEstudiantes, false,
                    "Docentes", "Lecciones de la semana", ayuda(ConsultaHorario.VISTA_DOCENTES),
                    "No hay docentes activos para consultar.",
                    periodos, periodo, List.of(), null, config, Map.of(), diaHoy);
        }

        Usuario elegido = elegir(docentes, personaId != null ? personaId : usuarioId, Usuario::getId);
        Map<String, List<HorarioLeccion>> horario = agrupar(horarioRepository.findConsultaPorDocente(
                direccionId, periodo.getId(), elegido.getId()));
        boolean propio = !gestion && elegido.getId().equals(usuarioId);
        String titular = propio ? "Tu horario" : elegido.getNombre();
        String detalle = propio ? elegido.getNombre() : null;
        return armar(ConsultaHorario.VISTA_DOCENTES, puedeVerDocentes, puedeVerEstudiantes, true,
                titular, detalle, ayuda(ConsultaHorario.VISTA_DOCENTES), null,
                periodos, periodo, docentes, elegido.getId(), config, horario, diaHoy);
    }

    private ConsultaHorario consultarEstudiantes(Long direccionId, Long usuarioId, boolean gestion, boolean docente,
            boolean puedeVerDocentes, boolean puedeVerEstudiantes, List<PeriodoAcademico> periodos,
            PeriodoAcademico periodo, Long personaId, ConfiguracionDireccion config, String diaHoy) {
        if (!gestion && !docente) {
            return consultarEstudiantePropio(direccionId, usuarioId, puedeVerDocentes, puedeVerEstudiantes,
                    periodos, periodo, config, diaHoy);
        }

        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosConNivel(direccionId, null, null);
        if (estudiantes.isEmpty()) {
            return armar(ConsultaHorario.VISTA_ESTUDIANTES, puedeVerDocentes, puedeVerEstudiantes, false,
                    "Estudiantes", "Horario del estudiante", ayuda(ConsultaHorario.VISTA_ESTUDIANTES),
                    "No hay estudiantes activos para consultar.",
                    periodos, periodo, List.of(), null, config, Map.of(), diaHoy);
        }

        Usuario elegido = elegir(estudiantes, personaId, Usuario::getId);
        return horarioDeEstudiante(direccionId, puedeVerDocentes, puedeVerEstudiantes, true,
                elegido.getNombre(), periodos, periodo, estudiantes, elegido, config, diaHoy);
    }

    private ConsultaHorario consultarEstudiantePropio(Long direccionId, Long usuarioId, boolean puedeVerDocentes,
            boolean puedeVerEstudiantes, List<PeriodoAcademico> periodos, PeriodoAcademico periodo,
            ConfiguracionDireccion config, String diaHoy) {
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(usuarioId, direccionId).orElse(null);
        if (estudiante == null) {
            return armar(ConsultaHorario.VISTA_ESTUDIANTES, puedeVerDocentes, puedeVerEstudiantes, false,
                    "Tu horario", "Consulta semanal", ayuda(ConsultaHorario.VISTA_ESTUDIANTES),
                    "No encontramos tu usuario en esta dirección.",
                    periodos, periodo, List.of(), null, config, Map.of(), diaHoy);
        }
        return horarioDeEstudiante(direccionId, puedeVerDocentes, puedeVerEstudiantes, false,
                "Tu horario", periodos, periodo, List.of(), estudiante, config, diaHoy);
    }

    private ConsultaHorario horarioDeEstudiante(Long direccionId, boolean puedeVerDocentes,
            boolean puedeVerEstudiantes, boolean puedeElegir, String titular, List<PeriodoAcademico> periodos,
            PeriodoAcademico periodo, List<Usuario> estudiantes, Usuario estudiante, ConfiguracionDireccion config,
            String diaHoy) {
        NivelAcademico nivel = estudiante.getNivelAcademico();
        if (nivel == null) {
            String mensaje = puedeElegir
                    ? "Este estudiante no tiene una sección asignada, así que no hay un horario para mostrar."
                    : "Tu usuario no tiene una sección asignada, así que no hay un horario para mostrar.";
            return armar(ConsultaHorario.VISTA_ESTUDIANTES, puedeVerDocentes, puedeVerEstudiantes, puedeElegir,
                    titular, null, ayuda(ConsultaHorario.VISTA_ESTUDIANTES), mensaje,
                    periodos, periodo, estudiantes, estudiante.getId(), config, Map.of(), diaHoy);
        }

        Map<String, List<HorarioLeccion>> horario = agrupar(horarioRepository.findConsultaPorNivel(
                direccionId, periodo.getId(), nivel.getId()));
        String detalle = "Sección " + nivel.getNombreCompleto();
        return armar(ConsultaHorario.VISTA_ESTUDIANTES, puedeVerDocentes, puedeVerEstudiantes, puedeElegir,
                titular, detalle, ayuda(ConsultaHorario.VISTA_ESTUDIANTES), null,
                periodos, periodo, estudiantes, estudiante.getId(), config, horario, diaHoy);
    }

    private ConsultaHorario armar(String vista, boolean puedeVerDocentes, boolean puedeVerEstudiantes,
            boolean puedeElegirUsuario, String titular, String detalle, String ayuda, String mensaje,
            List<PeriodoAcademico> periodos, PeriodoAcademico periodo, List<Usuario> usuarios, Long usuarioId,
            ConfiguracionDireccion config, Map<String, List<HorarioLeccion>> horario, String diaHoy) {
        int total = horario.values().stream().mapToInt(List::size).sum();
        return new ConsultaHorario(vista, puedeVerDocentes, puedeVerEstudiantes, puedeElegirUsuario,
                titular, detalle, ayuda, mensaje, periodos, periodo, usuarios, usuarioId,
                config.getDias(), config.getLecciones(), config.franjas(), config.recreos(), config.almuerzos(),
                horario, total, diaHoy);
    }

    private static String ayuda(String vista) {
        if (ConsultaHorario.VISTA_DOCENTES.equals(vista)) {
            return "Cada bloque muestra la materia, la sección y el aula.";
        }
        return "Cada bloque muestra la materia, el docente y el aula de la sección del estudiante.";
    }

    private static Map<String, List<HorarioLeccion>> agrupar(List<HorarioLeccion> lecciones) {
        Map<String, List<HorarioLeccion>> horario = new LinkedHashMap<>();
        for (HorarioLeccion leccion : lecciones) {
            horario.computeIfAbsent(ConfiguracionAcademicaService.clave(leccion.getDia(), leccion.getNumeroLeccion()),
                    k -> new ArrayList<>()).add(leccion);
        }
        return horario;
    }

    private static <T> T elegir(List<T> lista, Long pedido, Function<T, Long> id) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        if (pedido != null) {
            for (T item : lista) {
                if (pedido.equals(id.apply(item))) {
                    return item;
                }
            }
        }
        return lista.get(0);
    }

    private static String diaDeHoy() {
        DayOfWeek hoy = LocalDate.now().getDayOfWeek();
        return switch (hoy) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            default -> "";
        };
    }
}
