package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.DetalleNotaConducta;
import com.chavescr.nexa.dto.DetalleNotaConducta.Concepto;
import com.chavescr.nexa.dto.DetalleNotaConducta.Registro;
import com.chavescr.nexa.dto.FilaNotaConducta;
import com.chavescr.nexa.dto.PanelNotaConducta;
import com.chavescr.nexa.dto.ResumenNotaConducta;
import com.chavescr.nexa.dto.VistaEscala;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.NotaConducta;
import com.chavescr.nexa.entity.Notificacion;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.NotaConductaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class NotaConductaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLORES_AVATAR = {
            "#2d5a87", "#059669", "#0284c7", "#7c3aed",
            "#e11d48", "#ca8a04", "#db2777", "#0891b2"
    };

    private final UsuarioRepository usuarioRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final IncidenteConductaRepository incidenteRepository;
    private final NotaConductaRepository notaRepository;
    private final DireccionRepository direccionRepository;
    private final DocenteGuiaService docenteGuiaService;
    private final AlcanceDocenteService alcanceDocenteService;
    private final NotificacionService notificacionService;
    private final EscalaNotasService escalaNotasService;
    private final RebajaConductaService rebajaConductaService;
    private final AsistenciaEstudianteRepository asistenciaRepository;

    public NotaConductaService(UsuarioRepository usuarioRepository,
            PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository,
            IncidenteConductaRepository incidenteRepository,
            NotaConductaRepository notaRepository,
            DireccionRepository direccionRepository,
            DocenteGuiaService docenteGuiaService,
            AlcanceDocenteService alcanceDocenteService,
            NotificacionService notificacionService,
            EscalaNotasService escalaNotasService,
            RebajaConductaService rebajaConductaService,
            AsistenciaEstudianteRepository asistenciaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.incidenteRepository = incidenteRepository;
        this.notaRepository = notaRepository;
        this.direccionRepository = direccionRepository;
        this.docenteGuiaService = docenteGuiaService;
        this.alcanceDocenteService = alcanceDocenteService;
        this.notificacionService = notificacionService;
        this.escalaNotasService = escalaNotasService;
        this.rebajaConductaService = rebajaConductaService;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PanelNotaConducta cargarPanel(Long direccionId, Long periodoId, Integer grado, Long nivelId,
            Long docenteId) {
        List<PeriodoAcademico> periodos = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId);
        List<NivelAcademico> nivelesVisibles = nivelesVisibles(direccionId, docenteId);
        List<Integer> grados = nivelesVisibles.stream()
                .map(NivelAcademico::getGrado)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        PeriodoAcademico periodo = resolverPeriodo(periodos, periodoId);
        VistaEscala escala = escalaNotasService.vista(direccionId);
        if (periodo == null) {
            return new PanelNotaConducta(periodos, grados, List.of(), List.of(),
                    new ResumenNotaConducta(0, 0, 0, 0), null, grado, nivelId,
                    "Configure un período académico para registrar las notas de conducta.", escala);
        }

        if (grado != null && grados.stream().noneMatch(grado::equals)) {
            grado = null;
            nivelId = null;
        }

        List<NivelAcademico> secciones = seccionesDeGrado(nivelesVisibles, grado);
        if (nivelId != null) {
            Long seccionId = nivelId;
            boolean seccionValida = secciones.stream().anyMatch(s -> s.getId().equals(seccionId));
            if (!seccionValida) {
                nivelId = null;
            }
        }

        List<Usuario> estudiantes = listarEstudiantes(direccionId, grado, nivelId, nivelesVisibles, docenteId != null);
        List<Long> estudianteIds = estudiantes.stream().map(Usuario::getId).toList();

        Map<Long, List<IncidenteConducta>> porEstudiante = agruparIncidentes(
                direccionId, periodo.getId(), estudianteIds);
        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        Map<Long, int[]> asistencias = conteosAsistencia(direccionId, periodo, estudianteIds, calculo);

        Set<Long> enviadas = estudianteIds.isEmpty()
                ? Set.of()
                : new HashSet<>(notaRepository.findEstudianteIdsEnviados(direccionId, periodo.getId(), estudianteIds));

        List<FilaNotaConducta> filas = new ArrayList<>(estudiantes.size());
        List<IncidenteConducta> incidentesFiltrados = new ArrayList<>();
        for (Usuario estudiante : estudiantes) {
            List<IncidenteConducta> delEstudiante = porEstudiante.getOrDefault(estudiante.getId(), List.of());
            incidentesFiltrados.addAll(delEstudiante);
            filas.add(construirFila(estudiante, periodo, delEstudiante,
                    enviadas.contains(estudiante.getId()), escala, calculo, asistencias.get(estudiante.getId())));
        }
        filas.sort(Comparator
                .comparing((FilaNotaConducta f) -> f.getEstudiante().getNivelAcademico() == null
                        ? Integer.MAX_VALUE
                        : f.getEstudiante().getNivelAcademico().getGrado())
                .thenComparing(f -> f.getEstudiante().getNivelAcademico() == null
                        ? ""
                        : f.getEstudiante().getNivelAcademico().getSeccion())
                .thenComparing(f -> f.getEstudiante().getNombre(), String.CASE_INSENSITIVE_ORDER));

        return new PanelNotaConducta(periodos, grados, secciones, filas,
                resumir(filas, incidentesFiltrados), periodo.getId(), grado, nivelId, null, escala);
    }

    /**
     * Cuenta del año del período indicado. Las ausencias y tardías solo entran cuando la institución
     * las rebaja en la nota de conducta. Un docente solo ve estudiantes de sus secciones.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public DetalleNotaConducta detalle(Long direccionId, Long estudianteId, Long periodoId, Long docenteId) {
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        exigirConsulta(direccionId, docenteId, estudiante);

        List<PeriodoAcademico> todos = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId);
        PeriodoAcademico ancla = resolverPeriodo(todos == null ? List.of() : todos, periodoId);
        if (ancla == null || ancla.getFechaInicio() == null) {
            throw new IllegalArgumentException("No hay un período académico para consultar la nota.");
        }
        int anio = ancla.getFechaInicio().getYear();
        List<PeriodoAcademico> delAnio = (todos == null ? List.<PeriodoAcademico>of() : todos).stream()
                .filter(p -> p.getFechaInicio() != null && p.getFechaFin() != null
                        && p.getFechaInicio().getYear() == anio)
                .sorted(Comparator.comparing(PeriodoAcademico::getFechaInicio))
                .toList();
        if (delAnio.isEmpty()) {
            throw new IllegalArgumentException("No hay períodos en el año lectivo");
        }
        PeriodoAcademico elegido = delAnio.stream().filter(p -> p.getId().equals(periodoId)).findFirst()
                .orElseGet(() -> AsistenciaService.periodoPorDefecto(delAnio, LocalDate.now()));

        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        boolean aplicaAsistencia = calculo.asistenciaRebajaConducta();
        List<Object[]> asistencias = List.of();
        if (aplicaAsistencia) {
            LocalDate desde = delAnio.get(0).getFechaInicio();
            LocalDate hasta = delAnio.get(0).getFechaFin();
            for (PeriodoAcademico periodo : delAnio) {
                if (periodo.getFechaFin().isAfter(hasta)) {
                    hasta = periodo.getFechaFin();
                }
            }
            asistencias = asistenciaRepository.findAusenciasDeEstudiante(direccionId, estudianteId, desde, hasta,
                    EstadoAsistencia.PRESENTE);
        }

        List<DetalleNotaConducta.Periodo> bloques = new ArrayList<>();
        for (PeriodoAcademico periodo : delAnio) {
            List<IncidenteConducta> incidentes = new ArrayList<>(incidenteRepository
                    .findByDireccionIdAndPeriodoIdAndEstudianteId(direccionId, periodo.getId(), estudianteId));
            incidentes.sort(Comparator.comparing(IncidenteConducta::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(i -> i.getId() == null ? 0L : i.getId()));
            List<Object[]> delPeriodo = asistencias.stream()
                    .filter(fila -> caeEn(fila, periodo))
                    .toList();
            bloques.add(bloque(periodo, incidentes, delPeriodo, calculo, aplicaAsistencia,
                    periodo.getId().equals(elegido.getId())));
        }
        return new DetalleNotaConducta(estudiante.getId(), texto(estudiante.getNombre()),
                iniciales(estudiante.getNombre()), colorAvatar(estudiante.getId()), meta(estudiante),
                aplicaAsistencia, bloques);
    }

    @Transactional(rollbackFor = Exception.class)
    public String enviar(Long direccionId, Long periodoId, Long estudianteId, Long docenteId) {
        if (periodoId == null) {
            throw new IllegalArgumentException("Seleccione un período académico.");
        }
        PeriodoAcademico periodo = periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        exigirAlcance(direccionId, docenteId, estudiante);

        List<IncidenteConducta> incidentes = incidenteRepository
                .findByDireccionIdAndPeriodoIdAndEstudianteId(direccionId, periodoId, estudianteId);
        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        Map<Long, int[]> asistencias = conteosAsistencia(direccionId, periodo, List.of(estudianteId), calculo);
        FilaNotaConducta fila = construirFila(estudiante, periodo, incidentes, false,
                escalaNotasService.vista(direccionId), calculo, asistencias.get(estudianteId));

        List<Usuario> padres = usuarioRepository.findPadresByEstudianteId(estudianteId);
        if (padres.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay encargados vinculados a " + estudiante.getNombre() + ". Vincule un padre o madre primero.");
        }

        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
        Map<Long, NotaConducta> existentes = new HashMap<>();
        notaRepository.findByDireccionIdAndPeriodoIdAndEstudianteId(direccionId, periodoId, estudianteId)
                .ifPresent(n -> existentes.put(estudianteId, n));
        notaRepository.save(prepararEnvio(direccion, periodo, estudiante, fila, existentes));
        notificacionService.crearTodas(padres, mensajePadres(estudiante, fila), "/portal-padres");
        return "Nota de " + estudiante.getNombre() + " enviada a " + padres.size()
                + (padres.size() == 1 ? " encargado." : " encargados.");
    }

    @Transactional(rollbackFor = Exception.class)
    public String enviarTodas(Long direccionId, Long periodoId, Integer grado, Long nivelId, Long docenteId) {
        PanelNotaConducta panel = cargarPanel(direccionId, periodoId, grado, nivelId, docenteId);
        if (panel.getAvisoPeriodo() != null) {
            throw new IllegalArgumentException(panel.getAvisoPeriodo());
        }
        if (panel.getFilas().isEmpty()) {
            throw new IllegalArgumentException("No hay estudiantes para enviar en el filtro actual.");
        }

        PeriodoAcademico periodo = periodoRepository.findByIdAndDireccionId(panel.getPeriodoId(), direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Direccion direccion = direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));

        List<Long> estudianteIds = panel.getFilas().stream().map(f -> f.getEstudiante().getId()).toList();
        Map<Long, List<Usuario>> padresPorEstudiante = agruparPadres(estudianteIds);
        Map<Long, NotaConducta> existentes = notasExistentes(direccionId, periodo.getId(), estudianteIds);

        List<NotaConducta> aGuardar = new ArrayList<>();
        List<Notificacion> notificaciones = new ArrayList<>();
        int enviados = 0;
        int sinEncargado = 0;
        for (FilaNotaConducta fila : panel.getFilas()) {
            List<Usuario> padres = padresPorEstudiante.getOrDefault(fila.getEstudiante().getId(), List.of());
            if (padres.isEmpty()) {
                sinEncargado++;
                continue;
            }
            aGuardar.add(prepararEnvio(direccion, periodo, fila.getEstudiante(), fila, existentes));
            String mensaje = mensajePadres(fila.getEstudiante(), fila);
            for (Usuario padre : padres) {
                notificaciones.add(notificacionService.nueva(padre, mensaje, "/portal-padres"));
            }
            enviados++;
        }
        if (!aGuardar.isEmpty()) {
            notaRepository.saveAll(aGuardar);
        }
        notificacionService.guardarTodas(notificaciones);

        if (enviados == 0) {
            throw new IllegalArgumentException(
                    "Ninguna nota se envió: los estudiantes del filtro no tienen encargados vinculados.");
        }
        String mensaje = "Se enviaron " + enviados
                + (enviados == 1 ? " nota de conducta." : " notas de conducta.");
        if (sinEncargado > 0) {
            mensaje += " " + sinEncargado
                    + (sinEncargado == 1 ? " estudiante no tiene" : " estudiantes no tienen")
                    + " encargado vinculado.";
        }
        return mensaje;
    }

    private NotaConducta prepararEnvio(Direccion direccion, PeriodoAcademico periodo, Usuario estudiante,
            FilaNotaConducta fila, Map<Long, NotaConducta> existentes) {
        NotaConducta nota = existentes.get(estudiante.getId());
        if (nota == null) {
            nota = new NotaConducta();
            nota.setDireccion(direccion);
            nota.setPeriodo(periodo);
            nota.setEstudiante(estudiante);
            existentes.put(estudiante.getId(), nota);
        }
        nota.setNota(fila.getNota());
        nota.setObservaciones(fila.getObservaciones());
        nota.setEnviada(true);
        nota.setFechaEnvio(LocalDateTime.now());
        return nota;
    }

    private String mensajePadres(Usuario estudiante, FilaNotaConducta fila) {
        return "Nota de conducta de " + estudiante.getNombre() + ": " + fila.getNota()
                + " (" + fila.getCategoria() + ") — período " + fila.getPeriodoCodigo() + ".";
    }

    private Map<Long, List<Usuario>> agruparPadres(List<Long> estudianteIds) {
        if (estudianteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Usuario>> porEstudiante = new HashMap<>();
        for (Object[] fila : usuarioRepository.findPadresByEstudianteIds(estudianteIds)) {
            Long estudianteId = (Long) fila[0];
            Usuario padre = (Usuario) fila[1];
            porEstudiante.computeIfAbsent(estudianteId, id -> new ArrayList<>()).add(padre);
        }
        return porEstudiante;
    }

    private Map<Long, List<IncidenteConducta>> agruparIncidentes(Long direccionId, Long periodoId,
            List<Long> estudianteIds) {
        if (estudianteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<IncidenteConducta>> porEstudiante = new HashMap<>();
        for (Object[] fila : incidenteRepository.findDeEstudiantes(direccionId, periodoId, estudianteIds)) {
            porEstudiante.computeIfAbsent((Long) fila[0], id -> new ArrayList<>())
                    .add((IncidenteConducta) fila[1]);
        }
        return porEstudiante;
    }

    private Map<Long, NotaConducta> notasExistentes(Long direccionId, Long periodoId, List<Long> estudianteIds) {
        Map<Long, NotaConducta> existentes = new HashMap<>();
        if (estudianteIds.isEmpty()) {
            return existentes;
        }
        for (Object[] fila : notaRepository.findDeEstudiantes(direccionId, periodoId, estudianteIds)) {
            existentes.put((Long) fila[0], (NotaConducta) fila[1]);
        }
        return existentes;
    }

    private void exigirConsulta(Long direccionId, Long docenteId, Usuario estudiante) {
        if (docenteId == null) {
            return;
        }
        Long nivelEstudiante = estudiante.getNivelAcademico() != null ? estudiante.getNivelAcademico().getId() : null;
        boolean visible = nivelEstudiante != null
                && nivelesVisibles(direccionId, docenteId).stream().anyMatch(n -> n.getId().equals(nivelEstudiante));
        if (!visible) {
            throw new IllegalArgumentException("No tiene permiso para consultar la nota de este estudiante.");
        }
    }

    private DetalleNotaConducta.Periodo bloque(PeriodoAcademico periodo, List<IncidenteConducta> incidentes,
            List<Object[]> asistencias, CalculoRebaja calculo, boolean aplicaAsistencia, boolean actual) {
        List<IncidenteConducta> boletas = incidentes.stream().filter(i -> i.getTipo() == TipoIncidente.BOLETA).toList();
        List<IncidenteConducta> llamadas = incidentes.stream()
                .filter(i -> i.getTipo() == TipoIncidente.LLAMADA_ATENCION).toList();
        int puntosBoletas = boletas.stream().mapToInt(i -> i.getPuntosDescontados() == null ? 0 : i.getPuntosDescontados()).sum();
        int[] marcasLlamada = calculo.puntosPorRegistro(TipoRebaja.LLAMADA, llamadas.size());

        List<Concepto> conceptos = new ArrayList<>();
        conceptos.add(conceptoIncidente("boleta", "Boletas", boletas, null));
        conceptos.add(conceptoIncidente("llamada", "Llamadas de atención", llamadas, marcasLlamada));
        int[] conteo = null;
        if (aplicaAsistencia) {
            conteo = new int[4];
            for (Object[] fila : asistencias) {
                CalculoRebaja.sumar(conteo, (EstadoAsistencia) fila[2], 1);
            }
            conceptos.add(conceptoAsistencia("ausencia-injustificada", "Ausencias injustificadas",
                    TipoRebaja.AUSENCIA_INJUSTIFICADA, EstadoAsistencia.AUSENTE, asistencias, calculo));
            conceptos.add(conceptoAsistencia("ausencia-justificada", "Ausencias justificadas",
                    TipoRebaja.AUSENCIA_JUSTIFICADA, EstadoAsistencia.JUSTIFICADA, asistencias, calculo));
            conceptos.add(conceptoAsistencia("tardia-injustificada", "Tardías injustificadas",
                    TipoRebaja.TARDIA_INJUSTIFICADA, EstadoAsistencia.TARDIA, asistencias, calculo));
            conceptos.add(conceptoAsistencia("tardia-justificada", "Tardías justificadas",
                    TipoRebaja.TARDIA_JUSTIFICADA, EstadoAsistencia.TARDIA_JUSTIFICADA, asistencias, calculo));
        }
        int nota = calculo.nota(puntosBoletas, llamadas.size(), conteo);
        Concepto base = new Concepto("base", "Nota base", String.valueOf(CalculoRebaja.NOTA_INICIAL), false,
                String.valueOf(CalculoRebaja.NOTA_INICIAL), List.of(new Registro("—", "—", "—", "—",
                        "El período parte de 100 puntos.", false, String.valueOf(CalculoRebaja.NOTA_INICIAL))));
        String codigo = periodo.getCodigo() == null || periodo.getCodigo().isBlank() ? "Período" : periodo.getCodigo();
        String rango = periodo.getFechaInicio().format(FECHA) + " – " + periodo.getFechaFin().format(FECHA);
        return new DetalleNotaConducta.Periodo(periodo.getId(), codigo, rango, actual, String.valueOf(nota), base,
                conceptos);
    }

    private Concepto conceptoIncidente(String clave, String etiqueta, List<IncidenteConducta> incidentes, int[] marcas) {
        List<Registro> registros = new ArrayList<>();
        int puntos = 0;
        for (int i = 0; i < incidentes.size(); i++) {
            int rebaja = marcas == null
                    ? (incidentes.get(i).getPuntosDescontados() == null ? 0 : incidentes.get(i).getPuntosDescontados())
                    : marcas[i];
            puntos += rebaja;
            registros.add(registroIncidente(incidentes.get(i), rebaja));
        }
        if (marcas != null) {
            puntos = 0;
            for (int marca : marcas) {
                puntos += marca;
            }
        }
        return new Concepto(clave, etiqueta, String.valueOf(incidentes.size()), puntos == 0, puntosTexto(puntos),
                registros);
    }

    private Concepto conceptoAsistencia(String clave, String etiqueta, TipoRebaja tipo, EstadoAsistencia estado,
            List<Object[]> filas, CalculoRebaja calculo) {
        List<Object[]> delTipo = filas.stream().filter(fila -> fila[2] == estado).toList();
        int[] marcas = calculo.puntosPorRegistro(tipo, delTipo.size());
        List<Registro> registros = new ArrayList<>();
        int puntos = 0;
        for (int i = 0; i < delTipo.size(); i++) {
            puntos += marcas[i];
            registros.add(registroAsistencia(delTipo.get(i), marcas[i]));
        }
        return new Concepto(clave, etiqueta, String.valueOf(delTipo.size()), puntos == 0, puntosTexto(puntos),
                registros);
    }

    private static Registro registroIncidente(IncidenteConducta incidente, int puntos) {
        String detalle = texto(incidente.getDescripcion());
        if (detalle.isEmpty()) {
            detalle = texto(incidente.getMotivo());
        }
        String profesor = incidente.getRegistradoPor() == null ? "" : texto(incidente.getRegistradoPor().getNombre());
        String fecha = incidente.getFecha() == null ? "—" : incidente.getFecha().format(FECHA);
        return new Registro(fecha, "—", "—", oGuion(profesor), oGuion(detalle), puntos == 0, puntosTexto(puntos));
    }

    private static Registro registroAsistencia(Object[] fila, int puntos) {
        LocalDate fecha = (LocalDate) fila[0];
        String leccion = fila[1] instanceof Number numero ? String.valueOf(numero.intValue()) : "";
        return new Registro(fecha == null ? "—" : fecha.format(FECHA), oGuion(fila[3]), oGuion(leccion),
                oGuion(fila[5]), oGuion(fila[4]), puntos == 0, puntosTexto(puntos));
    }

    private static boolean caeEn(Object[] fila, PeriodoAcademico periodo) {
        LocalDate fecha = (LocalDate) fila[0];
        return fecha != null && !fecha.isBefore(periodo.getFechaInicio()) && !fecha.isAfter(periodo.getFechaFin());
    }

    private static String meta(Usuario estudiante) {
        String cedula = texto(estudiante.getCedula());
        String seccion = "Sin sección";
        if (estudiante.getNivelAcademico() != null) {
            NivelAcademico nivel = estudiante.getNivelAcademico();
            seccion = nivel.getGrado() + "-" + nivel.getSeccion();
        }
        String textoSeccion = "Sin sección".equals(seccion) ? seccion : "Sección " + seccion;
        if (!cedula.isEmpty()) {
            return cedula + " · " + textoSeccion;
        }
        return textoSeccion;
    }

    private static String puntosTexto(int puntos) {
        return puntos == 0 ? "0" : "−" + puntos;
    }

    private static String oGuion(Object valor) {
        String texto = texto(valor == null ? null : String.valueOf(valor));
        return texto.isEmpty() ? "—" : texto;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void exigirAlcance(Long direccionId, Long docenteId, Usuario estudiante) {
        if (docenteId == null) {
            return;
        }
        List<NivelAcademico> niveles = nivelesVisibles(direccionId, docenteId);
        Long nivelEstudiante = estudiante.getNivelAcademico() != null ? estudiante.getNivelAcademico().getId() : null;
        boolean visible = nivelEstudiante != null
                && niveles.stream().anyMatch(n -> n.getId().equals(nivelEstudiante));
        if (!visible) {
            throw new IllegalArgumentException("No tiene permiso para enviar la nota de este estudiante.");
        }
    }

    private Map<Long, int[]> conteosAsistencia(Long direccionId, PeriodoAcademico periodo,
            List<Long> estudianteIds, CalculoRebaja calculo) {
        if (!calculo.asistenciaRebajaConducta() || estudianteIds.isEmpty()
                || periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
            return Map.of();
        }
        Map<Long, int[]> conteos = new HashMap<>();
        for (Object[] fila : asistenciaRepository.contarEstadosPorEstudiante(direccionId, estudianteIds,
                periodo.getFechaInicio(), periodo.getFechaFin(), EstadoAsistencia.PRESENTE)) {
            Long estudianteId = (Long) fila[0];
            int[] conteo = conteos.computeIfAbsent(estudianteId, id -> new int[4]);
            int cantidad = fila[2] == null ? 0 : ((Number) fila[2]).intValue();
            CalculoRebaja.sumar(conteo, (EstadoAsistencia) fila[1], cantidad);
        }
        return conteos;
    }

    private FilaNotaConducta construirFila(Usuario estudiante, PeriodoAcademico periodo,
            List<IncidenteConducta> incidentes, boolean enviada, VistaEscala escala, CalculoRebaja calculo,
            int[] asistencia) {
        int puntosBoletas = incidentes.stream()
                .filter(i -> i.getTipo() != null && i.getTipo().afectaNota())
                .mapToInt(i -> i.getPuntosDescontados() != null ? i.getPuntosDescontados() : 0)
                .sum();
        long llamadas = contar(incidentes, TipoIncidente.LLAMADA_ATENCION);
        long boletas = contar(incidentes, TipoIncidente.BOLETA);
        int puntosAsistencia = calculo.puntosAsistencia(asistencia);
        int nota = calculo.nota(puntosBoletas, (int) llamadas, asistencia);
        VistaEscala.Tramo tramo = escala.tramo(nota);

        String seccion = "Sin sección";
        if (estudiante.getNivelAcademico() != null) {
            NivelAcademico nivel = estudiante.getNivelAcademico();
            seccion = nivel.getGrado() + "-" + nivel.getSeccion();
        }

        return new FilaNotaConducta(estudiante, seccion, nota, tramo.nombre(), tramo.tono(),
                observaciones(nota, llamadas, boletas, tramo.nombre(), puntosAsistencia), periodo.getCodigo(),
                enviada, iniciales(estudiante.getNombre()), colorAvatar(estudiante.getId()));
    }

    private ResumenNotaConducta resumir(List<FilaNotaConducta> filas, List<IncidenteConducta> incidentes) {
        int promedio = filas.isEmpty() ? 0
                : (int) Math.round(filas.stream().mapToInt(FilaNotaConducta::getNota).average().orElse(0));
        long llamadas = contar(incidentes, TipoIncidente.LLAMADA_ATENCION);
        long boletas = contar(incidentes, TipoIncidente.BOLETA);
        return new ResumenNotaConducta(promedio, llamadas, boletas, filas.size());
    }

    static String observaciones(int nota, long llamadas, long boletas, String categoria, int puntosAsistencia) {
        String texto;
        if (boletas > 0) {
            texto = "Conducta deficiente. " + cantidad(boletas, "boleta", "boletas")
                    + " por falta grave. Debe mejorar urgentemente su comportamiento.";
        } else if (llamadas >= 3) {
            texto = "Requiere mejorar su conducta. Ha recibido "
                    + cantidad(llamadas, "llamada de atención", "llamadas de atención") + " este período.";
        } else if (llamadas > 0) {
            texto = "Generalmente cumple con las normas. Ha tenido "
                    + cantidad(llamadas, "llamado de atención", "llamados de atención") + ".";
        } else if (puntosAsistencia > 0) {
            return "Las ausencias y tardías rebajan " + cantidad(puntosAsistencia, "punto", "puntos") + " de la nota.";
        } else if (nota >= 100) {
            return "Estudiante ejemplar. Nunca ha tenido llamadas de atención este período.";
        } else {
            texto = categoria + ". Cumple las normas de convivencia de la institución educativa.";
        }
        if (puntosAsistencia > 0) {
            return texto + " Las ausencias y tardías rebajan " + cantidad(puntosAsistencia, "punto", "puntos") + ".";
        }
        return texto;
    }

    private static String cantidad(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static long contar(List<IncidenteConducta> incidentes, TipoIncidente tipo) {
        return incidentes.stream().filter(i -> i.getTipo() == tipo).count();
    }

    private List<Usuario> listarEstudiantes(Long direccionId, Integer grado, Long nivelId,
            List<NivelAcademico> nivelesVisibles, boolean limitarANiveles) {
        if (!limitarANiveles) {
            return usuarioRepository.findEstudiantesActivosConNivel(direccionId, grado, nivelId);
        }
        List<Long> nivelIds = nivelesVisibles.stream().map(NivelAcademico::getId).toList();
        if (nivelIds.isEmpty()) {
            return List.of();
        }
        return usuarioRepository.findEstudiantesActivosConNivelEn(direccionId, nivelIds, grado, nivelId);
    }

    private List<NivelAcademico> nivelesVisibles(Long direccionId, Long docenteId) {
        if (docenteId == null) {
            return nivelRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
        }
        List<NivelAcademico> guias = docenteGuiaService.listarSecciones(direccionId, docenteId);
        if (!guias.isEmpty()) {
            return guias;
        }
        return alcanceDocenteService.nivelesVisibles(direccionId, docenteId);
    }

    private List<NivelAcademico> seccionesDeGrado(List<NivelAcademico> niveles, Integer grado) {
        if (grado == null) {
            return niveles;
        }
        return niveles.stream().filter(n -> grado.equals(n.getGrado())).toList();
    }

    private PeriodoAcademico resolverPeriodo(List<PeriodoAcademico> periodos, Long periodoId) {
        if (periodos.isEmpty()) {
            return null;
        }
        if (periodoId != null) {
            return periodos.stream().filter(p -> p.getId().equals(periodoId)).findFirst().orElse(periodos.get(0));
        }
        return periodos.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).findFirst().orElse(periodos.get(0));
    }

    static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    static String colorAvatar(Long id) {
        if (id == null) {
            return COLORES_AVATAR[0];
        }
        return COLORES_AVATAR[Math.floorMod(Long.hashCode(id), COLORES_AVATAR.length)];
    }
}
