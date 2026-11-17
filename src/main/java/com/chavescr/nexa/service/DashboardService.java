package com.chavescr.nexa.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.EventoCalendarioDTO;
import com.chavescr.nexa.dto.FilaEntregaAcademica;
import com.chavescr.nexa.dto.NotasCatalogo.AusenciaPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo.EstudianteOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.MateriaNota;
import com.chavescr.nexa.dto.NotasCatalogo.NotaPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo.SeguimientoPeriodo;
import com.chavescr.nexa.dto.PaginaEntregas;
import com.chavescr.nexa.dto.PanelInicio;
import com.chavescr.nexa.dto.PanelInicio.AsistenciaHoy;
import com.chavescr.nexa.dto.PanelInicio.EstudianteEnRiesgo;
import com.chavescr.nexa.dto.PanelInicio.EventoAgenda;
import com.chavescr.nexa.dto.PanelInicio.Leccion;
import com.chavescr.nexa.dto.PanelInicio.Pendiente;
import com.chavescr.nexa.dto.PanelInicio.Rendimiento;
import com.chavescr.nexa.dto.PanelInicio.SeccionGuia;
import com.chavescr.nexa.dto.PanelInicio.SeccionPromedio;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.CicloEducativo;
import com.chavescr.nexa.entity.EntregaAcademicaDocente.EstadoPresentacion;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.IncidenteConducta.EstadoIncidente;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.RetiroEstudiante.EstadoRetiro;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.entity.Visita.EstadoVisita;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.EntregaAcademicaDocenteRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Arma el dashboard de inicio de cada rol con datos que ya calculan los demás módulos: asistencia,
 * conducta, entregas académicas, control de acceso, agenda y notas.
 */
@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class DashboardService {

    /**
     * Hora de Costa Rica: el servidor puede correr en UTC y el inicio decide con la hora qué lección está
     * en curso y si ya debía tener lista.
     */
    public static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");

    /** Días hacia adelante que muestra la agenda. */
    static final int DIAS_AGENDA = 7;
    private static final int MAX_EVENTOS = 8;
    private static final int MAX_SIN_LISTA = 6;
    private static final int MAX_EN_RIESGO = 8;
    private static final int MAX_SECCIONES = 5;

    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final IncidenteConductaRepository incidenteRepository;
    private final EntregaAcademicaDocenteRepository entregaDocenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionAcademicaService configuracionAcademicaService;
    private final RegistroAsistenciaService registroAsistenciaService;
    private final VisitaService visitaService;
    private final RetiroEstudianteService retiroService;
    private final EntregaAcademicaService entregaAcademicaService;
    private final ComunicadoService comunicadoService;
    private final CalendarioService calendarioService;
    private final NotasConsultaService notasConsultaService;
    private final EscalaNotasService escalaNotasService;
    private final DocenteGuiaService docenteGuiaService;

    public DashboardService(AsistenciaEstudianteRepository asistenciaRepository,
            HorarioLeccionRepository horarioRepository, IncidenteConductaRepository incidenteRepository,
            EntregaAcademicaDocenteRepository entregaDocenteRepository, UsuarioRepository usuarioRepository,
            ConfiguracionAcademicaService configuracionAcademicaService,
            RegistroAsistenciaService registroAsistenciaService, VisitaService visitaService,
            RetiroEstudianteService retiroService, EntregaAcademicaService entregaAcademicaService,
            ComunicadoService comunicadoService, CalendarioService calendarioService,
            NotasConsultaService notasConsultaService, EscalaNotasService escalaNotasService,
            DocenteGuiaService docenteGuiaService) {
        this.asistenciaRepository = asistenciaRepository;
        this.horarioRepository = horarioRepository;
        this.incidenteRepository = incidenteRepository;
        this.entregaDocenteRepository = entregaDocenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionAcademicaService = configuracionAcademicaService;
        this.registroAsistenciaService = registroAsistenciaService;
        this.visitaService = visitaService;
        this.retiroService = retiroService;
        this.entregaAcademicaService = entregaAcademicaService;
        this.comunicadoService = comunicadoService;
        this.calendarioService = calendarioService;
        this.notasConsultaService = notasConsultaService;
        this.escalaNotasService = escalaNotasService;
        this.docenteGuiaService = docenteGuiaService;
    }

    // ── Dirección ────────────────────────────────────────────────────────────

    public PanelInicio.Direccion direccion(Long direccionId, Long usuarioId) {
        PeriodoAcademico periodo = periodoVigente(direccionId);
        LocalDate hoy = LocalDate.now(ZONA);
        List<Leccion> lecciones = leccionesDeHoy(direccionId, periodo, null);
        List<Leccion> dadas = lecciones.stream().filter(l -> l.estado() != Leccion.Estado.PROXIMA).toList();

        Map<EstadoAsistencia, Long> porEstado = new EnumMap<>(EstadoAsistencia.class);
        for (Object[] fila : asistenciaRepository.contarEstudiantesPorEstado(direccionId, hoy)) {
            porEstado.put((EstadoAsistencia) fila[0], ((Number) fila[1]).longValue());
        }
        long personal = registroAsistenciaService.obtenerConteoPersonalPresente(direccionId)
                .getOrDefault("presentes", 0L);
        AsistenciaHoy asistencia = new AsistenciaHoy(
                porEstado.getOrDefault(EstadoAsistencia.AUSENTE, 0L),
                porEstado.getOrDefault(EstadoAsistencia.TARDIA, 0L),
                porEstado.getOrDefault(EstadoAsistencia.JUSTIFICADA, 0L)
                        + porEstado.getOrDefault(EstadoAsistencia.TARDIA_JUSTIFICADA, 0L),
                dadas.size(),
                (int) dadas.stream().filter(Leccion::listaPasada).count(),
                dadas.stream().filter(Leccion::isSinLista).limit(MAX_SIN_LISTA).toList(),
                personal,
                !lecciones.isEmpty());

        return new PanelInicio.Direccion(asistencia, pendientesDireccion(direccionId), periodo(periodo),
                agenda(direccionId, usuarioId, true));
    }

    private List<Pendiente> pendientesDireccion(Long direccionId) {
        List<Pendiente> pendientes = new ArrayList<>();
        agregar(pendientes, "Entregas académicas por revisar", "Presentadas por docentes y sin revisión",
                entregaDocenteRepository.countByEntrega_Direccion_IdAndEstado(direccionId, EstadoPresentacion.PRESENTADA),
                "/entregas-academicas", "info");
        agregar(pendientes, "Apelaciones de conducta", "Llamadas de atención o boletas apeladas",
                incidenteRepository.countByDireccionIdAndEstadoIn(direccionId, List.of(EstadoIncidente.APELADO)),
                "/conducta", "danger");
        agregar(pendientes, "Boletas y llamadas sin resolver", "Pendientes o en proceso",
                incidenteRepository.countByDireccionIdAndEstadoIn(direccionId,
                        List.of(EstadoIncidente.PENDIENTE, EstadoIncidente.EN_PROCESO)),
                "/conducta", "warning");
        agregar(pendientes, "Visitas esperando autorización", "Registradas hoy en control de acceso",
                visitaService.obtenerVisitasDelDia(direccionId).stream()
                        .filter(v -> v.getEstado() == EstadoVisita.PENDIENTE).count(),
                "/control-de-acceso", "warning");
        agregar(pendientes, "Retiros de estudiantes por autorizar", "Solicitados hoy por las familias",
                retiroService.obtenerRetirosDelDia(direccionId).stream()
                        .filter(r -> r.getEstado() == EstadoRetiro.PENDIENTE).count(),
                "/control-de-acceso", "warning");
        return pendientes;
    }

    /**
     * Estudiantes con alguna materia bajo la nota de aprobación y secciones con el promedio más bajo del
     * período vigente. Recorre las notas de toda la dirección, por eso el inicio lo pide aparte.
     */
    public Rendimiento rendimiento(Long direccionId) {
        PeriodoAcademico periodo = periodoVigente(direccionId);
        if (periodo == null) {
            return new Rendimiento(null, 0, 0, List.of(), List.of(), "No hay un período activo.");
        }
        List<NivelAcademico> niveles = configuracionAcademicaService.listarNivelesActivos(direccionId);
        if (niveles.isEmpty()) {
            return new Rendimiento(periodo.getCodigo(), 0, 0, List.of(), List.of(),
                    "No hay secciones activas en esta dirección.");
        }
        Map<Long, NivelAcademico> nivelPorId = new HashMap<>();
        niveles.forEach(n -> nivelPorId.put(n.getId(), n));
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosConNivelEn(direccionId,
                nivelPorId.keySet(), null, null);
        List<EstudianteOpcion> resumen = notasConsultaService.resumenDelPeriodo(direccionId, estudiantes, periodo);

        Map<CicloEducativo, Integer> aprobacionPorCiclo = new EnumMap<>(CicloEducativo.class);
        List<EstudianteEnRiesgo> enRiesgo = new ArrayList<>();
        Map<Long, double[]> porSeccion = new LinkedHashMap<>();   // suma, cantidad de notas, estudiantes, en riesgo
        int conNotas = 0;

        for (EstudianteOpcion estudiante : resumen) {
            NivelAcademico nivel = nivelPorId.get(estudiante.getNivelId());
            int aprobacion = notaAprobacion(direccionId, nivel, aprobacionPorCiclo);
            List<String> bajo = new ArrayList<>();
            double peor = Double.MAX_VALUE;
            double suma = 0;
            int cantidad = 0;
            for (var materia : estudiante.getMaterias()) {
                Double nota = notaDelPeriodo(materia.getNotas(), periodo.getId());
                if (nota == null) {
                    continue;
                }
                // La conducta no entra en el promedio de la sección, pero reprobarla sí pide seguimiento.
                if (!esConducta(materia)) {
                    suma += nota;
                    cantidad++;
                }
                if (nota < aprobacion) {
                    bajo.add(materia.getNombre());
                    peor = Math.min(peor, nota);
                }
            }
            if (cantidad == 0) {
                continue;
            }
            conNotas++;
            double[] seccion = porSeccion.computeIfAbsent(estudiante.getNivelId(), k -> new double[4]);
            seccion[0] += suma / cantidad;
            seccion[1]++;
            if (!bajo.isEmpty()) {
                seccion[3]++;
                enRiesgo.add(new EstudianteEnRiesgo(estudiante.getId(), estudiante.getNombre(),
                        estudiante.getGrado() + "-" + estudiante.getSeccion(), estudiante.getIniciales(),
                        estudiante.getColor(), bajo, redondear(peor)));
            }
        }

        enRiesgo.sort(Comparator.comparingInt((EstudianteEnRiesgo e) -> e.materias().size()).reversed()
                .thenComparingDouble(EstudianteEnRiesgo::peorNota));
        List<SeccionPromedio> secciones = new ArrayList<>();
        porSeccion.forEach((nivelId, datos) -> {
            NivelAcademico nivel = nivelPorId.get(nivelId);
            secciones.add(new SeccionPromedio(nivelId, nivel.getGrado() + "-" + nivel.getSeccion(),
                    redondear(datos[0] / datos[1]), (int) datos[1], (int) datos[3]));
        });
        secciones.sort(Comparator.comparingDouble(SeccionPromedio::promedio));

        String aviso = conNotas == 0 ? "Todavía no hay calificaciones registradas en este período." : null;
        return new Rendimiento(periodo.getCodigo(), conNotas, enRiesgo.size(),
                enRiesgo.stream().limit(MAX_EN_RIESGO).toList(),
                secciones.stream().limit(MAX_SECCIONES).toList(), aviso);
    }

    // ── Docente ──────────────────────────────────────────────────────────────

    public PanelInicio.Docente docente(Long direccionId, Long docenteId) {
        PeriodoAcademico periodo = periodoVigente(direccionId);
        List<Leccion> clases = leccionesDeHoy(direccionId, periodo, docenteId);

        List<Pendiente> pendientes = new ArrayList<>();
        long sinLista = clases.stream().filter(Leccion::isSinLista).count();
        agregar(pendientes, "Lecciones de hoy sin pasar lista", "Ya empezaron o terminaron", sinLista,
                "/asistencia", "danger");
        PaginaEntregas entregas = entregaAcademicaService.listar(direccionId, docenteId, false, true, null, 1);
        if (entregas.getTotal() > 0) {
            FilaEntregaAcademica proxima = entregas.getFilas().get(0);
            String detalle = proxima.getEntrega().getFechaFin() == null
                    ? proxima.getEntrega().getNombre()
                    : proxima.getEntrega().getNombre() + " · vence " + fechaCorta(proxima.getEntrega().getFechaFin());
            boolean vencida = proxima.getEntrega().getFechaFin() != null
                    && proxima.getEntrega().getFechaFin().isBefore(LocalDate.now(ZONA));
            agregar(pendientes, "Entregas académicas por presentar", detalle, entregas.getTotal(),
                    "/entregas-academicas", vencida ? "danger" : "warning");
        }
        agregar(pendientes, "Comunicados sin leer", "Oficios y avisos de la dirección",
                comunicadoService.sinLeer(direccionId, docenteId), "/comunicacion", "info");

        List<SeccionGuia> guia = new ArrayList<>();
        for (NivelAcademico nivel : docenteGuiaService.listarSecciones(direccionId, docenteId)) {
            if (!Boolean.TRUE.equals(nivel.getActivo())) {
                continue;
            }
            List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivel.getId());
            Map<Long, int[]> novedades = novedadesDeHoy(direccionId, estudiantes);
            guia.add(new SeccionGuia(nivel.getId(), nivel.getGrado() + "-" + nivel.getSeccion(), estudiantes.size(),
                    novedades.values().stream().filter(n -> n[0] > 0).count(),
                    novedades.values().stream().filter(n -> n[0] == 0 && n[1] > 0).count()));
        }

        String diaSinClases = null;
        if (clases.isEmpty()) {
            diaSinClases = periodo == null
                    ? "No hay un período activo."
                    : "Hoy no tienes lecciones en el horario.";
        }
        return new PanelInicio.Docente(clases, pendientes, guia, periodo(periodo),
                agenda(direccionId, docenteId, true), diaSinClases);
    }

    // ── Familia y estudiante ─────────────────────────────────────────────────

    /** Para el padre, sus hijos activos en esta dirección; para el estudiante, él mismo. */
    public PanelInicio.Familia familia(Long direccionId, Long usuarioId, boolean esEstudiante) {
        List<Usuario> estudiantes = new ArrayList<>();
        if (esEstudiante) {
            usuarioRepository.findEstudianteActivoConNivel(usuarioId, direccionId).ifPresent(estudiantes::add);
        } else {
            for (Usuario hijo : usuarioRepository.findEstudiantesByPadreId(usuarioId)) {
                usuarioRepository.findEstudianteActivoConNivel(hijo.getId(), direccionId).ifPresent(estudiantes::add);
            }
        }

        List<PanelInicio.Estudiante> tarjetas = resumenEstudiantes(direccionId, estudiantes);
        List<Pendiente> pendientes = new ArrayList<>();
        agregar(pendientes, "Comunicados sin leer", "Avisos de la institución",
                comunicadoService.sinLeer(direccionId, usuarioId), "/comunicacion", "info");
        return new PanelInicio.Familia(tarjetas, pendientes, periodo(periodoVigente(direccionId)),
                agenda(direccionId, usuarioId, false), esEstudiante);
    }

    /**
     * Cómo va cada estudiante en el período vigente: notas por materia, conducta, ausencias, tardías,
     * llamadas, boletas y novedades de hoy. Quien llama ya validó que puede ver a esos estudiantes.
     */
    public List<PanelInicio.Estudiante> resumenEstudiantes(Long direccionId, List<Usuario> estudiantes) {
        PeriodoAcademico periodo = periodoVigente(direccionId);
        Map<Long, EstudianteOpcion> resumen = new HashMap<>();
        for (EstudianteOpcion opcion : notasConsultaService.resumenDelPeriodo(direccionId, estudiantes, periodo)) {
            resumen.put(opcion.getId(), opcion);
        }
        Map<Long, int[]> hoy = novedadesDeHoy(direccionId, estudiantes);
        Map<CicloEducativo, Integer> aprobacionPorCiclo = new EnumMap<>(CicloEducativo.class);

        List<PanelInicio.Estudiante> tarjetas = new ArrayList<>();
        for (Usuario estudiante : estudiantes) {
            NivelAcademico nivel = estudiante.getNivelAcademico();
            int aprobacion = notaAprobacion(direccionId, nivel, aprobacionPorCiclo);
            EstudianteOpcion opcion = resumen.get(estudiante.getId());
            List<PanelInicio.MateriaNota> materias = new ArrayList<>();
            int ausencias = 0;
            int tardias = 0;
            int llamadas = 0;
            int boletas = 0;
            Double conducta = null;
            if (opcion != null && periodo != null) {
                for (var materia : opcion.getMaterias()) {
                    Double nota = notaDelPeriodo(materia.getNotas(), periodo.getId());
                    if (nota == null) {
                        continue;
                    }
                    if (esConducta(materia)) {
                        conducta = nota;
                    } else {
                        materias.add(new PanelInicio.MateriaNota(materia.getNombre(), nota, nota >= aprobacion));
                    }
                }
                for (AusenciaPeriodo a : opcion.getAusencias()) {
                    if (periodo.getId().equals(a.getPeriodoId())) {
                        ausencias += a.getInjustificadas();
                        tardias += a.getTardiasInjustificadas();
                    }
                }
                for (SeguimientoPeriodo s : opcion.getSeguimiento()) {
                    if (periodo.getId().equals(s.getPeriodoId())) {
                        llamadas += s.getLlamadas();
                        boletas += s.getBoletas();
                    }
                }
            }
            materias.sort(Comparator.comparingDouble(PanelInicio.MateriaNota::nota));
            Double promedio = materias.isEmpty() ? null
                    : redondear(materias.stream().mapToDouble(PanelInicio.MateriaNota::nota).average().orElse(0));
            int[] delDia = hoy.getOrDefault(estudiante.getId(), new int[2]);
            tarjetas.add(new PanelInicio.Estudiante(estudiante.getId(), estudiante.getNombre(),
                    nivel == null ? null : nivel.getGrado() + "-" + nivel.getSeccion(),
                    opcion != null ? opcion.getIniciales() : iniciales(estudiante.getNombre()),
                    opcion != null ? opcion.getColor() : null,
                    promedio, aprobacion, materias, conducta, ausencias, tardias, llamadas, boletas, delDia[0],
                    delDia[1]));
        }

        return tarjetas;
    }

    // ── Vista previa (admin y system config) ─────────────────────────────────

    /** El docente con más lecciones en el período; si no hay horario, el primero activo. */
    public Optional<Usuario> docenteDeMuestra(Long direccionId) {
        PeriodoAcademico periodo = periodoVigente(direccionId);
        List<Usuario> docentes = configuracionAcademicaService.listarDocentes(direccionId);
        if (periodo != null) {
            Map<Long, Long> lecciones = new HashMap<>();
            for (Object[] fila : horarioRepository.countLeccionesGroupedByDocente(direccionId, periodo.getId())) {
                if (fila[0] != null) {
                    lecciones.put((Long) fila[0], ((Number) fila[1]).longValue());
                }
            }
            Optional<Usuario> conMasLecciones = docentes.stream()
                    .filter(d -> lecciones.containsKey(d.getId()))
                    .max(Comparator.comparingLong(d -> lecciones.get(d.getId())));
            if (conMasLecciones.isPresent()) {
                return conMasLecciones;
            }
        }
        return docentes.stream().findFirst();
    }

    /** Un estudiante activo con sección en la dirección. */
    public Optional<Usuario> estudianteDeMuestra(Long direccionId) {
        return usuarioRepository.findEstudiantesActivosConNivel(direccionId, null, null).stream()
                .filter(u -> u.getNivelAcademico() != null)
                .findFirst();
    }

    /** Un padre o madre con al menos un hijo activo con sección en la dirección. */
    public Optional<Usuario> padreDeMuestra(Long direccionId) {
        List<Long> estudiantes = usuarioRepository.findEstudiantesActivosConNivel(direccionId, null, null).stream()
                .filter(u -> u.getNivelAcademico() != null)
                .map(Usuario::getId)
                .limit(200)
                .toList();
        if (estudiantes.isEmpty()) {
            return Optional.empty();
        }
        return usuarioRepository.findPadresByEstudianteIds(estudiantes).stream()
                .map(fila -> (Usuario) fila[1])
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .findFirst();
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    /** El período activo que incluye hoy; si ninguno lo incluye, el activo más reciente. */
    PeriodoAcademico periodoVigente(Long direccionId) {
        List<PeriodoAcademico> activos = configuracionAcademicaService.listarPeriodosActivos(direccionId);
        LocalDate hoy = LocalDate.now(ZONA);
        return activos.stream()
                .filter(p -> p.getFechaInicio() != null && p.getFechaFin() != null
                        && !hoy.isBefore(p.getFechaInicio()) && !hoy.isAfter(p.getFechaFin()))
                .findFirst()
                .orElse(activos.isEmpty() ? null : activos.get(0));
    }

    private PanelInicio.Periodo periodo(PeriodoAcademico periodo) {
        if (periodo == null || periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
            return null;
        }
        LocalDate hoy = LocalDate.now(ZONA);
        long total = Math.max(1, ChronoUnit.DAYS.between(periodo.getFechaInicio(), periodo.getFechaFin()));
        long transcurridos = ChronoUnit.DAYS.between(periodo.getFechaInicio(), hoy);
        int avance = (int) Math.max(0, Math.min(100, Math.round(transcurridos * 100.0 / total)));
        long restantes = Math.max(0, ChronoUnit.DAYS.between(hoy, periodo.getFechaFin()));
        return new PanelInicio.Periodo(periodo.getCodigo(), periodo.getDescripcion(), periodo.getFechaInicio(),
                periodo.getFechaFin(), restantes, avance);
    }

    private List<Leccion> leccionesDeHoy(Long direccionId, PeriodoAcademico periodo, Long docenteId) {
        return lecciones(direccionId, periodo, diaDeHoy(), docenteId, null);
    }

    /**
     * Lecciones de un día de la semana ("LUNES"...) en el período vigente, de un docente o de una sección
     * (null = todas). La lista pasada y el estado (en curso, terminada) solo aplican si el día es hoy.
     */
    public List<Leccion> leccionesDelDia(Long direccionId, String dia, Long docenteId, Long nivelId) {
        return lecciones(direccionId, periodoVigente(direccionId), dia, docenteId, nivelId);
    }

    private List<Leccion> lecciones(Long direccionId, PeriodoAcademico periodo, String dia, Long docenteId,
            Long nivelId) {
        if (periodo == null || dia == null || dia.isEmpty()) {
            return List.of();
        }
        boolean esHoy = dia.equals(diaDeHoy());
        LocalDate hoy = LocalDate.now(ZONA);
        LocalTime ahora = esHoy ? LocalTime.now(ZONA) : LocalTime.MIN;
        Set<String> conLista = new HashSet<>();
        if (esHoy) {
            for (Object[] fila : asistenciaRepository.findLeccionesConLista(direccionId, hoy)) {
                conLista.add(clave(fila[0], fila[1], fila[2]));
            }
        }
        List<Leccion> lecciones = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (HorarioLeccion h : horarioRepository.findDelDia(direccionId, periodo.getId(), dia)) {
            if (docenteId != null && (h.getDocente() == null || !docenteId.equals(h.getDocente().getId()))) {
                continue;
            }
            if (nivelId != null && !nivelId.equals(h.getNivel().getId())) {
                continue;
            }
            String clave = clave(h.getNivel().getId(), h.getMateria().getId(), h.getNumeroLeccion());
            if (!vistas.add(clave)) {
                continue;
            }
            lecciones.add(new Leccion(h.getNumeroLeccion(), h.getHoraInicio(), h.getHoraFin(),
                    h.getMateria().getNombre(), h.getNivel().getGrado() + "-" + h.getNivel().getSeccion(),
                    h.getDocente() == null ? null : h.getDocente().getNombre(),
                    h.getAula() == null ? null : h.getAula().getNombre(),
                    conLista.contains(clave), estado(h.getHoraInicio(), h.getHoraFin(), ahora)));
        }
        return lecciones;
    }

    private static Leccion.Estado estado(LocalTime inicio, LocalTime fin, LocalTime ahora) {
        if (inicio == null || ahora.isBefore(inicio)) {
            return Leccion.Estado.PROXIMA;
        }
        if (fin == null || ahora.isBefore(fin)) {
            return Leccion.Estado.EN_CURSO;
        }
        return Leccion.Estado.TERMINADA;
    }

    /** Por estudiante: [lecciones ausente, lecciones tarde] de hoy (justificadas incluidas). */
    private Map<Long, int[]> novedadesDeHoy(Long direccionId, List<Usuario> estudiantes) {
        Map<Long, int[]> mapa = new HashMap<>();
        if (estudiantes.isEmpty()) {
            return mapa;
        }
        List<Long> ids = estudiantes.stream().map(Usuario::getId).toList();
        for (Object[] fila : asistenciaRepository.findNovedadesDelDia(direccionId, LocalDate.now(ZONA), ids,
                EstadoAsistencia.PRESENTE)) {
            EstadoAsistencia estado = (EstadoAsistencia) fila[1];
            int[] par = mapa.computeIfAbsent((Long) fila[0], k -> new int[2]);
            par[estado.esAusencia() ? 0 : 1]++;
        }
        return mapa;
    }

    /**
     * Agenda de los próximos días, con cada evento una sola vez (los de varios días traen su rango).
     * Las tareas del plan de trabajo solo las ve el personal.
     */
    private List<EventoAgenda> agenda(Long direccionId, Long usuarioId, boolean conTareas) {
        LocalDate hoy = LocalDate.now(ZONA);
        return agendaEntre(direccionId, usuarioId, hoy, hoy.plusDays(DIAS_AGENDA), conTareas, MAX_EVENTOS);
    }

    /** Eventos de la agenda entre dos fechas, cada uno una sola vez. */
    public List<EventoAgenda> agendaEntre(Long direccionId, Long usuarioId, LocalDate desde, LocalDate hasta,
            boolean conTareas, int maximo) {
        Map<LocalDate, List<EventoCalendarioDTO>> eventos = calendarioService.eventosEntre(direccionId, usuarioId,
                desde, hasta, conTareas);
        Map<String, EventoAgenda> unicos = new LinkedHashMap<>();
        eventos.forEach((fecha, lista) -> {
            for (EventoCalendarioDTO ev : lista) {
                String clave = ev.getTipo() + "|" + ev.getTitulo() + "|" + (ev.isMultiDia() ? "" : fecha);
                if (unicos.containsKey(clave)) {
                    continue;
                }
                LocalDate fin = ev.isMultiDia() ? finDe(eventos, ev, fecha) : null;
                unicos.put(clave, new EventoAgenda(fecha, fin, ev.getHora(), ev.getTitulo(),
                        ev.getTipo().name().toLowerCase(), ev.getTipoLabel(), ev.getEnlace()));
            }
        });
        return unicos.values().stream().limit(maximo).toList();
    }

    /** Último día visible en que aparece un evento de varios días. */
    private static LocalDate finDe(Map<LocalDate, List<EventoCalendarioDTO>> eventos, EventoCalendarioDTO ev,
            LocalDate desde) {
        LocalDate fin = desde;
        for (var entrada : eventos.entrySet()) {
            boolean aparece = entrada.getValue().stream()
                    .anyMatch(o -> o.getTipo() == ev.getTipo() && Objects.equals(o.getTitulo(), ev.getTitulo()));
            if (aparece && entrada.getKey().isAfter(fin)) {
                fin = entrada.getKey();
            }
        }
        return fin;
    }

    private int notaAprobacion(Long direccionId, NivelAcademico nivel, Map<CicloEducativo, Integer> cache) {
        CicloEducativo ciclo = nivel == null ? null : nivel.getCicloEfectivo();
        if (ciclo == null) {
            return escalaNotasService.obtener(direccionId, null).getNotaAprobacion();
        }
        return cache.computeIfAbsent(ciclo, c -> escalaNotasService.obtener(direccionId, c).getNotaAprobacion());
    }

    private static boolean esConducta(MateriaNota materia) {
        return materia.getId() != null && materia.getId() == NotasConsultaService.CONDUCTA_ID;
    }

    private static Double notaDelPeriodo(List<NotaPeriodo> notas, Long periodoId) {
        return notas.stream()
                .filter(n -> periodoId.equals(n.getPeriodoId()) && n.getPorcentaje() != null)
                .map(NotaPeriodo::getPorcentaje)
                .findFirst()
                .orElse(null);
    }

    private static void agregar(List<Pendiente> pendientes, String titulo, String detalle, long cantidad,
            String enlace, String tono) {
        if (cantidad > 0) {
            pendientes.add(new Pendiente(titulo, detalle, cantidad, enlace, tono));
        }
    }

    private static String clave(Object nivelId, Object materiaId, Object leccion) {
        return nivelId + "|" + materiaId + "|" + leccion;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private static String fechaCorta(LocalDate fecha) {
        return fecha.getDayOfMonth() + "/" + fecha.getMonthValue();
    }

    private static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        return partes.length > 1 ? (primera + partes[1].charAt(0)).toUpperCase() : primera.toUpperCase();
    }

    static String diaDeHoy() {
        DayOfWeek hoy = LocalDate.now(ZONA).getDayOfWeek();
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
