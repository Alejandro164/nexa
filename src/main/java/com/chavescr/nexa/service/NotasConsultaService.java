package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.NotasBoletaDetalle;
import com.chavescr.nexa.dto.NotasConductaDetalle;
import com.chavescr.nexa.dto.NotasConductaDetalle.Cuenta;
import com.chavescr.nexa.dto.NotasConductaDetalle.Linea;
import com.chavescr.nexa.dto.NotasConductaDetalle.Registro;
import com.chavescr.nexa.dto.NotasBoletasPeriodo;
import com.chavescr.nexa.dto.NotasAusenciaDetalle;
import com.chavescr.nexa.dto.NotasAusenciasPeriodo;
import com.chavescr.nexa.dto.NotasLlamadaDetalle;
import com.chavescr.nexa.dto.NotasLlamadasPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo;
import com.chavescr.nexa.dto.NotasDesgloseFila;
import com.chavescr.nexa.dto.NotasMateriaDetalle;
import com.chavescr.nexa.dto.NotasRubroDetalle;
import com.chavescr.nexa.dto.NotasCatalogo.AnioOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.AusenciaPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo.EstudianteOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.MateriaNota;
import com.chavescr.nexa.dto.NotasCatalogo.NivelOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.NotaPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo.ObservacionPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo.PeriodoOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.SeguimientoPeriodo;
import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.EstadoIncidente;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ResultadoComponente;
import com.chavescr.nexa.entity.TipoComponente;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.ComponenteRepository;
import com.chavescr.nexa.repository.DistribucionPorcentualRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.ObservacionGuiaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ResultadoComponenteRepository;
import com.chavescr.nexa.repository.TipoComponenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class NotasConsultaService {

    private static final long CONDUCTA_ID = -1L;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLORES = {
            "#2d5a87", "#059669", "#0284c7", "#7c3aed", "#e11d48", "#ca8a04", "#db2777", "#0891b2"
    };

    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocenteGuiaService docenteGuiaService;
    private final HorarioLeccionRepository horarioRepository;
    private final ComponenteRepository componenteRepository;
    private final ResultadoComponenteRepository resultadoRepository;
    private final DistribucionPorcentualRepository distribucionRepository;
    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final TipoComponenteRepository tipoComponenteRepository;
    private final IncidenteConductaRepository incidenteRepository;
    private final ObservacionGuiaRepository observacionRepository;
    private final MateriaRepository materiaRepository;
    private final RebajaConductaService rebajaConductaService;

    public NotasConsultaService(PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository, UsuarioRepository usuarioRepository,
            DocenteGuiaService docenteGuiaService, HorarioLeccionRepository horarioRepository,
            ComponenteRepository componenteRepository, ResultadoComponenteRepository resultadoRepository,
            DistribucionPorcentualRepository distribucionRepository,
            AsistenciaEstudianteRepository asistenciaRepository,
            TipoComponenteRepository tipoComponenteRepository,
            IncidenteConductaRepository incidenteRepository,
            ObservacionGuiaRepository observacionRepository,
            MateriaRepository materiaRepository,
            RebajaConductaService rebajaConductaService) {
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.usuarioRepository = usuarioRepository;
        this.docenteGuiaService = docenteGuiaService;
        this.horarioRepository = horarioRepository;
        this.componenteRepository = componenteRepository;
        this.resultadoRepository = resultadoRepository;
        this.distribucionRepository = distribucionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.tipoComponenteRepository = tipoComponenteRepository;
        this.incidenteRepository = incidenteRepository;
        this.observacionRepository = observacionRepository;
        this.materiaRepository = materiaRepository;
        this.rebajaConductaService = rebajaConductaService;
    }

    /**
     * @param elegirAnio        director o administrador: puede cambiar de año lectivo
     * @param catalogoCompleto  administrador o director: todos los grados y secciones de la dirección
     */
    public NotasCatalogo consultar(Long direccionId, Long usuarioId, boolean elegirAnio,
            boolean catalogoCompleto) {
        List<PeriodoAcademico> periodos = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId);
        PeriodoAcademico activo = periodos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .findFirst()
                .orElse(null);
        PeriodoAcademico referencia = activo != null ? activo : (periodos.isEmpty() ? null : periodos.get(0));
        Integer anio = referencia == null ? null : referencia.getFechaInicio().getYear();

        List<AnioOpcion> anios = periodos.stream()
                .map(p -> p.getFechaInicio().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .map(year -> new AnioOpcion(year, String.valueOf(year)))
                .toList();

        List<PeriodoAcademico> visibles = periodos.stream()
                .filter(p -> elegirAnio || (anio != null && p.getFechaInicio().getYear() == anio))
                .sorted(Comparator.comparing(PeriodoAcademico::getFechaInicio))
                .toList();

        List<NivelAcademico> niveles = nivelesVisibles(direccionId, usuarioId, catalogoCompleto);
        List<Long> nivelIds = niveles.stream().map(NivelAcademico::getId).toList();
        List<Usuario> estudiantes = nivelIds.isEmpty()
                ? List.of()
                : usuarioRepository.findEstudiantesActivosConNivelEn(direccionId, nivelIds, null, null).stream()
                        .filter(u -> u.getNivelAcademico() != null)
                        .toList();

        NotasArmadas armadas = materiasPorEstudiante(direccionId, nivelIds, estudiantes, visibles);
        Map<Long, List<ObservacionPeriodo>> observaciones = observacionesDe(direccionId, estudiantes, visibles);
        Map<Long, List<SeguimientoPeriodo>> seguimiento = seguimientoDe(direccionId, estudiantes, visibles);

        String aviso = null;
        if (periodos.isEmpty()) {
            aviso = "No hay períodos académicos en esta dirección.";
        } else if (niveles.isEmpty()) {
            aviso = catalogoCompleto
                    ? "No hay grados ni secciones activos en esta dirección."
                    : "No tiene secciones asignadas como profesor guía.";
        }

        List<AnioOpcion> aniosVisibles = elegirAnio
                ? anios
                : anios.stream().filter(a -> anio != null && anio.equals(a.getAnio())).toList();

        return new NotasCatalogo(
                elegirAnio,
                anio,
                anio == null ? "Sin períodos" : String.valueOf(anio),
                aviso,
                aniosVisibles,
                visibles.stream().map(this::periodo).toList(),
                niveles.stream().map(this::nivel).toList(),
                estudiantes.stream()
                        .map(u -> estudiante(u,
                                armadas.materias().getOrDefault(u.getId(), List.of()),
                                armadas.ausencias().getOrDefault(u.getId(), List.of()),
                                observaciones.getOrDefault(u.getId(), List.of()),
                                seguimiento.getOrDefault(u.getId(), List.of())))
                        .toList());
    }

    /**
     * Mismas materias que la tabla del año, con el desglose del período elegido.
     * La lista sale del horario y de las notas de toda la sección en ese año;
     * las calificaciones que se calculan son solo las del período.
     */
    public List<NotasDesgloseFila> desglose(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId) {
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        NivelAcademico nivel = estudiante.getNivelAcademico();
        if (nivel == null) {
            throw new IllegalArgumentException("El estudiante no tiene sección asignada");
        }
        if (!supervision && nivelesVisibles(direccionId, usuarioId, false).stream()
                .noneMatch(n -> nivel.getId().equals(n.getId()))) {
            throw new IllegalArgumentException("No puede consultar las notas de este estudiante");
        }
        PeriodoAcademico periodo = periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));

        int anio = periodo.getFechaInicio().getYear();
        List<PeriodoAcademico> delAnio = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId)
                .stream()
                .filter(p -> p.getFechaInicio().getYear() == anio)
                .toList();
        Long nivelId = nivel.getId();
        List<Long> nivelIds = List.of(nivelId);
        List<Long> periodoIdsAnio = delAnio.stream().map(PeriodoAcademico::getId).toList();
        Map<Long, Long> ordenPeriodo = new HashMap<>();
        for (PeriodoAcademico delPeriodo : delAnio) {
            ordenPeriodo.put(delPeriodo.getId(), delPeriodo.getFechaInicio().toEpochDay());
        }

        Map<Long, Map<Long, MateriaVista>> porNivel = new HashMap<>();
        for (Object[] fila : horarioRepository.findAsignacionesNivelMateria(direccionId, periodoIdsAnio, nivelIds)) {
            registrar(porNivel, (Long) fila[0], (Long) fila[1], (String) fila[2], (String) fila[3],
                    (Long) fila[4], ordenPeriodo);
        }
        for (Object[] fila : resultadoRepository.findMateriasConNota(direccionId, nivelId, periodoIdsAnio)) {
            registrar(porNivel, nivelId, (Long) fila[0], (String) fila[1], null, periodoId, ordenPeriodo);
        }

        Map<Long, List<ResultadoComponente>> porMateria = new HashMap<>();
        for (ResultadoComponente resultado : resultadoRepository.findDeEstudianteEnPeriodo(
                direccionId, nivelId, estudianteId, periodoId)) {
            Componente componente = resultado.getComponente();
            if (componente.getMateria() == null) {
                continue;
            }
            porMateria.computeIfAbsent(componente.getMateria().getId(), k -> new ArrayList<>()).add(resultado);
        }

        List<Long> periodoIds = List.of(periodoId);
        Map<PesoKey, Map<Long, Double>> pesos = pesosDe(
                componenteRepository.findParaNotas(direccionId, nivelIds), periodoIds);
        boolean conAsistencia = rebajaConductaService.asistenciaRebajaComponente(direccionId);
        CalculoRebaja calculo = conAsistencia ? rebajaConductaService.calculo(direccionId) : null;
        Distribuciones distribuciones = distribucionesDe(direccionId, periodoIds, conAsistencia);
        Map<Long, int[]> asistencia = conAsistencia
                ? asistenciaDeEstudiante(direccionId, estudianteId, periodo)
                : Map.of();
        Set<ClaveComponente> claves = clavesActivas(direccionId);

        List<NotasDesgloseFila> filas = new ArrayList<>();
        for (MateriaVista vista : porNivel.getOrDefault(nivelId, Map.of()).values().stream()
                .sorted(Comparator.comparing((MateriaVista v) -> v.nombre, String.CASE_INSENSITIVE_ORDER))
                .toList()) {
            ComponentesNota nota = ponderar(
                    porMateria.getOrDefault(vista.id, List.of()),
                    asistencia.get(vista.id),
                    distribuciones.de(periodoId, vista.id),
                    claves,
                    pesosDeMateria(pesos, nivelId, vista.id, periodoId),
                    calculo);
            filas.add(new NotasDesgloseFila(vista.id, vista.nombre, vista.docente, nota.cotidiano(), nota.tareas(),
                    nota.proyecto(), nota.pruebas(), nota.asistencia(), nota.porcentaje()));
        }
        return filas;
    }

    /**
     * Rubros que forman la nota de una celda del desglose. La ponderación es la misma que
     * {@link #desglose}: un rubro entra solo si el componente está activo, tiene calificación y peso.
     */
    public NotasMateriaDetalle detalleMateria(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId, Long materiaId, String claveTexto) {
        PeriodoAcademico periodo = periodoDeEstudianteVisible(direccionId, usuarioId, supervision, estudianteId,
                periodoId);
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        NivelAcademico nivel = estudiante.getNivelAcademico();
        if (nivel == null) {
            throw new IllegalArgumentException("El estudiante no tiene sección asignada");
        }
        Materia materia = materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        exigirMateriaDeLaSeccion(direccionId, nivel.getId(), materiaId, periodo);

        boolean conAsistencia = rebajaConductaService.asistenciaRebajaComponente(direccionId);
        int[] dist = distribucionesDe(direccionId, List.of(periodo.getId()), conAsistencia)
                .de(periodo.getId(), materiaId);
        String nombre = materia.getNombre() == null || materia.getNombre().isBlank() ? "Materia" : materia.getNombre();
        String limpia = claveTexto == null ? "" : claveTexto.trim().toUpperCase(Locale.ROOT);
        if ("ASISTENCIA".equals(limpia)) {
            if (!conAsistencia) {
                throw new IllegalArgumentException(
                        "La asistencia no forma parte de la nota: las ausencias y tardías rebajan la conducta");
            }
            return detalleAsistencia(direccionId, estudianteId, materiaId, periodo, nombre, dist[4]);
        }
        ClaveComponente clave;
        try {
            clave = ClaveComponente.valueOf(limpia);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Componente no válido");
        }
        return detalleComponente(direccionId, nivel.getId(), estudianteId, materiaId, periodo, nombre, clave,
                pesoDe(dist, clave));
    }

    private NotasMateriaDetalle detalleComponente(Long direccionId, Long nivelId, Long estudianteId, Long materiaId,
            PeriodoAcademico periodo, String materia, ClaveComponente clave, int pesoNota) {
        boolean acotadoAlPeriodo = clave == ClaveComponente.PROYECTO || clave == ClaveComponente.EXAMEN;
        List<Componente> componentes = new ArrayList<>(acotadoAlPeriodo
                ? componenteRepository.findByDireccionIdAndClaveAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
                        direccionId, clave, nivelId, materiaId, periodo.getId())
                : componenteRepository.findByDireccionIdAndClaveAndNivelIdAndMateriaIdOrderByFechaAsc(
                        direccionId, clave, nivelId, materiaId));
        componentes.sort(Comparator
                .comparing(Componente::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(c -> c.getTitulo() == null ? "" : c.getTitulo(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(c -> c.getId() == null ? 0L : c.getId()));

        Map<Long, ResultadoComponente> porComponente = new HashMap<>();
        for (ResultadoComponente resultado : resultadoRepository.findDeEstudianteEnPeriodo(
                direccionId, nivelId, estudianteId, periodo.getId())) {
            Componente delResultado = resultado.getComponente();
            if (delResultado.getClave() != clave || delResultado.getMateria() == null
                    || !materiaId.equals(delResultado.getMateria().getId())) {
                continue;
            }
            porComponente.put(delResultado.getId(), resultado);
        }

        boolean activo = clavesActivas(direccionId).contains(clave);
        Map<Long, Double> pesos = pesosEfectivos(componentes);
        List<ResultadoComponente> usados = new ArrayList<>();
        List<NotasRubroDetalle> rubros = new ArrayList<>();
        boolean muestraPuntos = false;
        int entran = 0;
        for (Componente componente : componentes) {
            ResultadoComponente resultado = porComponente.get(componente.getId());
            if (resultado != null) {
                usados.add(resultado);
            }
            double peso = pesos.getOrDefault(componente.getId(), 0.0);
            Integer nota = resultado == null ? null : resultado.getCalificacion();
            boolean entra = activo && nota != null && peso > 0;
            if (entra) {
                entran++;
            }
            String puntos = textoPuntos(resultado == null ? null : resultado.getPuntosObtenidos(),
                    componente.getPuntosTotales());
            if (!puntos.isEmpty()) {
                muestraPuntos = true;
            }
            String observacion = resultado == null || resultado.getObservacion() == null
                    ? ""
                    : resultado.getObservacion().trim();
            String descripcion = componente.getDescripcion() == null ? "" : componente.getDescripcion().trim();
            rubros.add(new NotasRubroDetalle(
                    componente.getTitulo(),
                    descripcion,
                    componente.getFecha() == null ? "" : FECHA.format(componente.getFecha()),
                    textoPorcentaje(peso),
                    nota,
                    puntos,
                    observacion,
                    entra,
                    "",
                    ""));
        }
        Integer promedio = activo ? promedioDe(usados, clave, pesos) : null;
        String etiqueta = etiquetaClave(clave);
        return new NotasMateriaDetalle(materia, etiqueta, false, activo, promedio, pesoNota, componentes.size(),
                entran, leyendaComponente(clave, etiqueta, materia, entran, componentes.size(), pesoNota, activo),
                muestraPuntos, rubros);
    }

    private NotasMateriaDetalle detalleAsistencia(Long direccionId, Long estudianteId, Long materiaId,
            PeriodoAcademico periodo, String materia, int pesoNota) {
        List<AsistenciaEstudiante> lecciones = new ArrayList<>(asistenciaRepository
                .findByDireccionIdAndEstudianteIdAndMateriaIdAndFechaBetween(
                        direccionId, estudianteId, materiaId, periodo.getFechaInicio(), periodo.getFechaFin()));
        lecciones.sort(Comparator.comparing(AsistenciaEstudiante::getFecha)
                .thenComparing(AsistenciaEstudiante::getNumeroLeccion));
        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        List<NotasRubroDetalle> rubros = new ArrayList<>();
        int[] conteo = new int[4];
        int presentes = 0;
        for (AsistenciaEstudiante leccion : lecciones) {
            EstadoAsistencia estado = leccion.getEstado();
            boolean presente = estado != null && estado.cuentaComoPresente();
            if (presente) {
                presentes++;
            }
            CalculoRebaja.sumar(conteo, estado, 1);
            String observacion = leccion.getObservaciones() == null ? "" : leccion.getObservaciones().trim();
            rubros.add(new NotasRubroDetalle(
                    "Lección " + leccion.getNumeroLeccion(),
                    "",
                    FECHA.format(leccion.getFecha()),
                    "",
                    null,
                    "",
                    observacion,
                    presente,
                    etiquetaAsistencia(estado),
                    claseAsistencia(estado)));
        }
        int total = lecciones.size();
        Integer promedio = total == 0 ? null : calculo.notaAsistencia(conteo);
        int rebaja = total == 0 ? 0 : calculo.rebajaAsistencia(conteo);
        String leyenda = total == 0
                ? "No hay lecciones registradas en " + materia + " en este período."
                : "Parte de 100. Las ausencias y tardías rebajan " + rebaja
                        + (rebaja == 1 ? " punto" : " puntos") + ", así que la asistencia queda en "
                        + String.format(Locale.US, "%.2f", promedio.doubleValue())
                        + ". Vale " + pesoNota + "% de la nota de " + materia + ".";
        return new NotasMateriaDetalle(materia, "Asistencia", true, true, promedio, pesoNota, total, presentes,
                leyenda, false, rubros);
    }

    /** La materia tiene que ser de la sección del estudiante en el año del período, igual que el desglose. */
    private void exigirMateriaDeLaSeccion(Long direccionId, Long nivelId, Long materiaId, PeriodoAcademico periodo) {
        int anio = periodo.getFechaInicio().getYear();
        List<Long> periodoIds = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId).stream()
                .filter(p -> p.getFechaInicio().getYear() == anio)
                .map(PeriodoAcademico::getId)
                .toList();
        boolean enHorario = horarioRepository.findAsignacionesNivelMateria(direccionId, periodoIds, List.of(nivelId))
                .stream().anyMatch(fila -> materiaId.equals(fila[1]));
        boolean conNota = resultadoRepository.findMateriasConNota(direccionId, nivelId, periodoIds).stream()
                .anyMatch(fila -> materiaId.equals(fila[0]));
        if (!enHorario && !conNota) {
            throw new IllegalArgumentException("Materia no encontrada");
        }
    }

    private String leyendaComponente(ClaveComponente clave, String etiqueta, String materia, int entran, int total,
            int peso, boolean activo) {
        if (total == 0) {
            return sinRubros(clave, materia);
        }
        if (!activo) {
            String cantidad = total == 1 ? "Hay 1 rubro" : "Hay " + total + " rubros";
            return cantidad + " en " + materia
                    + ". Este componente está inactivo, así que la celda del desglose queda sin nota.";
        }
        String base = "La nota es el promedio ponderado de los rubros con calificación y peso: "
                + entran + " de " + total + ". " + frasePeso(etiqueta, peso, materia);
        if (clave == ClaveComponente.COTIDIANO || clave == ClaveComponente.TAREA) {
            base += " Se listan todos los de la materia; la calificación es la de este período.";
        }
        return base;
    }

    private String sinRubros(ClaveComponente clave, String materia) {
        String frase = switch (clave) {
            case COTIDIANO -> "No hay trabajos cotidianos registrados en ";
            case TAREA -> "No hay tareas registradas en ";
            case PROYECTO -> "No hay proyectos registrados en ";
            case EXAMEN -> "No hay pruebas registradas en ";
        };
        return frase + materia + ".";
    }

    private String frasePeso(String etiqueta, int peso, String materia) {
        boolean plural = "Tareas".equals(etiqueta) || "Pruebas".equals(etiqueta);
        return etiqueta + (plural ? " valen " : " vale ") + peso + "% de la nota de " + materia + ".";
    }

    private String etiquetaClave(ClaveComponente clave) {
        return switch (clave) {
            case COTIDIANO -> "Cotidiano";
            case TAREA -> "Tareas";
            case PROYECTO -> "Proyecto";
            case EXAMEN -> "Pruebas";
        };
    }

    private String textoPorcentaje(double peso) {
        double redondeo = Math.round(peso * 100.0) / 100.0;
        if (Math.abs(redondeo - Math.rint(redondeo)) < 0.001) {
            return String.format(Locale.US, "%.0f%%", redondeo);
        }
        if (Math.abs(redondeo * 10 - Math.rint(redondeo * 10)) < 0.001) {
            return String.format(Locale.US, "%.1f%%", redondeo);
        }
        return String.format(Locale.US, "%.2f%%", redondeo);
    }

    private String textoPuntos(Integer obtenidos, Integer totales) {
        if (obtenidos == null || totales == null) {
            return "";
        }
        return obtenidos + "/" + totales;
    }

    private String etiquetaAsistencia(EstadoAsistencia estado) {
        if (estado == null) {
            return "Sin registro";
        }
        return switch (estado) {
            case PRESENTE -> "Presente";
            case JUSTIFICADA -> "Ausencia justificada";
            case AUSENTE -> "Ausencia injustificada";
            case TARDIA_JUSTIFICADA -> "Tardía justificada";
            case TARDIA -> "Tardía injustificada";
        };
    }

    private String claseAsistencia(EstadoAsistencia estado) {
        if (estado == null) {
            return "vacio";
        }
        return switch (estado) {
            case PRESENTE -> "presente";
            case JUSTIFICADA -> "justificada";
            case AUSENTE -> "injustificada";
            case TARDIA_JUSTIFICADA -> "tardia-justificada";
            case TARDIA -> "tardia";
        };
    }

    /** Cada lección no presente del período, en el mismo orden en que se registró la asistencia. */
    public NotasAusenciasPeriodo ausencias(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId) {
        PeriodoAcademico periodo = periodoDeEstudianteVisible(direccionId, usuarioId, supervision, estudianteId,
                periodoId);
        List<NotasAusenciaDetalle> registros = new ArrayList<>();
        for (Object[] fila : asistenciaRepository.findAusenciasDeEstudiante(
                direccionId, estudianteId, periodo.getFechaInicio(), periodo.getFechaFin(),
                EstadoAsistencia.PRESENTE)) {
            EstadoAsistencia estado = (EstadoAsistencia) fila[2];
            String observacion = fila[4] == null ? "" : ((String) fila[4]).trim();
            String profesor = fila[5] == null ? "" : ((String) fila[5]).trim();
            registros.add(new NotasAusenciaDetalle(
                    FECHA.format((LocalDate) fila[0]),
                    (String) fila[3],
                    ((Number) fila[1]).intValue(),
                    etiquetaAusencia(estado),
                    claseAusencia(estado),
                    observacion,
                    profesor));
        }
        return new NotasAusenciasPeriodo(registros);
    }

    /** Llamadas de atención del estudiante en el período, de la más reciente a la más antigua. */
    public NotasLlamadasPeriodo llamadas(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId) {
        periodoDeEstudianteVisible(direccionId, usuarioId, supervision, estudianteId, periodoId);
        List<NotasLlamadaDetalle> registros = new ArrayList<>();
        for (IncidenteConducta incidente : incidenteRepository.findDeEstudianteEnPeriodo(
                direccionId, periodoId, estudianteId, TipoIncidente.LLAMADA_ATENCION)) {
            String docente = incidente.getRegistradoPor() == null ? "" : incidente.getRegistradoPor().getNombre();
            String descripcion = incidente.getDescripcion() == null ? "" : incidente.getDescripcion().trim();
            EstadoIncidente estado = incidente.getEstado() == null ? EstadoIncidente.PENDIENTE : incidente.getEstado();
            registros.add(new NotasLlamadaDetalle(
                    FECHA.format(incidente.getFecha()),
                    incidente.getMotivo(),
                    descripcion,
                    docente == null ? "" : docente.trim(),
                    etiquetaEstado(estado),
                    claseEstado(estado)));
        }
        return new NotasLlamadasPeriodo(registros);
    }

    /** Boletas del estudiante en el período, de la más reciente a la más antigua. */
    public NotasBoletasPeriodo boletas(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId) {
        periodoDeEstudianteVisible(direccionId, usuarioId, supervision, estudianteId, periodoId);
        List<NotasBoletaDetalle> registros = new ArrayList<>();
        for (IncidenteConducta incidente : incidenteRepository.findDeEstudianteEnPeriodo(
                direccionId, periodoId, estudianteId, TipoIncidente.BOLETA)) {
            String docente = incidente.getRegistradoPor() == null ? "" : incidente.getRegistradoPor().getNombre();
            String descripcion = incidente.getDescripcion() == null ? "" : incidente.getDescripcion().trim();
            EstadoIncidente estado = incidente.getEstado() == null ? EstadoIncidente.PENDIENTE : incidente.getEstado();
            int puntos = incidente.getPuntosDescontados() == null ? 0 : incidente.getPuntosDescontados();
            registros.add(new NotasBoletaDetalle(
                    FECHA.format(incidente.getFecha()),
                    incidente.getMotivo(),
                    puntos,
                    descripcion,
                    docente == null ? "" : docente.trim(),
                    etiquetaEstado(estado),
                    claseEstado(estado)));
        }
        return new NotasBoletasPeriodo(registros);
    }

    /**
     * Cuenta que explica la nota de conducta. Con período, una sola cuenta. Con año, una por
     * período y el promedio, que es el número de la columna Año.
     */
    public NotasConductaDetalle detalleConducta(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId, Integer anio) {
        exigirEstudianteVisible(direccionId, usuarioId, supervision, estudianteId);
        List<PeriodoAcademico> periodos;
        boolean delAnio;
        if (periodoId != null) {
            periodos = List.of(periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Período no encontrado")));
            delAnio = false;
        } else if (anio != null) {
            periodos = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId).stream()
                    .filter(p -> p.getFechaInicio() != null && p.getFechaInicio().getYear() == anio)
                    .sorted(Comparator.comparing(PeriodoAcademico::getFechaInicio))
                    .toList();
            if (periodos.isEmpty()) {
                throw new IllegalArgumentException("No hay períodos en ese año lectivo");
            }
            delAnio = true;
        } else {
            throw new IllegalArgumentException("Seleccione un período");
        }

        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        List<Cuenta> cuentas = new ArrayList<>();
        int suma = 0;
        for (PeriodoAcademico periodo : periodos) {
            Cuenta cuenta = cuentaDe(direccionId, estudianteId, periodo, calculo);
            cuentas.add(cuenta);
            suma += cuenta.getNota();
        }
        double promedio = Math.round((suma / (double) cuentas.size()) * 10) / 10.0;
        double mostrada = delAnio ? promedio : cuentas.get(0).getNota();
        String nota = String.format(Locale.US, "%.2f", mostrada);
        return new NotasConductaDetalle(delAnio, calculo.asistenciaRebajaConducta(), nota, cuentas);
    }

    private Cuenta cuentaDe(Long direccionId, Long estudianteId, PeriodoAcademico periodo, CalculoRebaja calculo) {
        int boletas = 0;
        int puntosBoletas = 0;
        int llamadas = 0;
        for (IncidenteConducta incidente : incidenteRepository.findByDireccionIdAndPeriodoIdAndEstudianteId(
                direccionId, periodo.getId(), estudianteId)) {
            if (incidente.getTipo() == TipoIncidente.BOLETA) {
                boletas++;
                puntosBoletas += incidente.getPuntosDescontados() == null ? 0 : incidente.getPuntosDescontados();
            } else if (incidente.getTipo() == TipoIncidente.LLAMADA_ATENCION) {
                llamadas++;
            }
        }
        List<Linea> lineas = new ArrayList<>();
        lineas.add(new Linea("Boletas", boletas, puntosBoletas));
        lineas.add(new Linea("Llamadas de atención", llamadas, calculo.puntos(TipoRebaja.LLAMADA, llamadas)));

        List<Registro> registros = new ArrayList<>();
        int[] conteo = null;
        if (calculo.asistenciaRebajaConducta() && periodo.getFechaInicio() != null && periodo.getFechaFin() != null) {
            List<Object[]> filas = asistenciaRepository.findAusenciasDeEstudiante(direccionId, estudianteId,
                    periodo.getFechaInicio(), periodo.getFechaFin(), EstadoAsistencia.PRESENTE);
            int[] puntos = puntosDeCadaRegistro(calculo, filas);
            conteo = new int[4];
            List<Registro> lista = new ArrayList<>();
            for (int i = 0; i < filas.size(); i++) {
                Object[] fila = filas.get(i);
                EstadoAsistencia estado = (EstadoAsistencia) fila[2];
                CalculoRebaja.sumar(conteo, estado, 1);
                String materia = fila[3] == null ? "" : ((String) fila[3]).trim();
                lista.add(new Registro(FECHA.format((LocalDate) fila[0]), materia.isEmpty() ? "Materia" : materia,
                        etiquetaAusencia(estado), puntos[i]));
            }
            registros = lista;
            lineas.add(new Linea("Ausencias justificadas", conteo[0],
                    calculo.puntos(TipoRebaja.AUSENCIA_JUSTIFICADA, conteo[0])));
            lineas.add(new Linea("Ausencias injustificadas", conteo[1],
                    calculo.puntos(TipoRebaja.AUSENCIA_INJUSTIFICADA, conteo[1])));
            lineas.add(new Linea("Tardías justificadas", conteo[2],
                    calculo.puntos(TipoRebaja.TARDIA_JUSTIFICADA, conteo[2])));
            lineas.add(new Linea("Tardías injustificadas", conteo[3],
                    calculo.puntos(TipoRebaja.TARDIA_INJUSTIFICADA, conteo[3])));
        }
        int nota = calculo.nota(puntosBoletas, llamadas, conteo);
        String etiqueta = periodo.getCodigo() == null || periodo.getCodigo().isBlank()
                ? "Período" : periodo.getCodigo();
        return new Cuenta(etiqueta, lineas, registros, nota);
    }

    private static int[] puntosDeCadaRegistro(CalculoRebaja calculo, List<Object[]> filas) {
        Map<TipoRebaja, List<Integer>> posiciones = new HashMap<>();
        for (int i = 0; i < filas.size(); i++) {
            TipoRebaja tipo = tipoRebaja((EstadoAsistencia) filas.get(i)[2]);
            if (tipo != null) {
                posiciones.computeIfAbsent(tipo, k -> new ArrayList<>()).add(i);
            }
        }
        int[] puntos = new int[filas.size()];
        posiciones.forEach((tipo, indices) -> {
            int[] marcas = calculo.puntosPorRegistro(tipo, indices.size());
            for (int j = 0; j < marcas.length; j++) {
                puntos[indices.get(j)] = marcas[j];
            }
        });
        return puntos;
    }

    private static TipoRebaja tipoRebaja(EstadoAsistencia estado) {
        if (estado == null) {
            return null;
        }
        return switch (estado) {
            case JUSTIFICADA -> TipoRebaja.AUSENCIA_JUSTIFICADA;
            case AUSENTE -> TipoRebaja.AUSENCIA_INJUSTIFICADA;
            case TARDIA_JUSTIFICADA -> TipoRebaja.TARDIA_JUSTIFICADA;
            case TARDIA -> TipoRebaja.TARDIA_INJUSTIFICADA;
            case PRESENTE -> null;
        };
    }

    private PeriodoAcademico periodoDeEstudianteVisible(Long direccionId, Long usuarioId, boolean supervision,
            Long estudianteId, Long periodoId) {
        exigirEstudianteVisible(direccionId, usuarioId, supervision, estudianteId);
        return periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
    }

    private Usuario exigirEstudianteVisible(Long direccionId, Long usuarioId, boolean supervision, Long estudianteId) {
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        if (estudiante.getNivelAcademico() == null) {
            throw new IllegalArgumentException("El estudiante no tiene sección asignada");
        }
        if (!supervision && nivelesVisibles(direccionId, usuarioId, false).stream()
                .noneMatch(n -> estudiante.getNivelAcademico().getId().equals(n.getId()))) {
            throw new IllegalArgumentException("No puede consultar las notas de este estudiante");
        }
        return estudiante;
    }

    private String etiquetaAusencia(EstadoAsistencia estado) {
        return switch (estado) {
            case JUSTIFICADA -> "Ausencia justificada";
            case AUSENTE -> "Ausencia injustificada";
            case TARDIA_JUSTIFICADA -> "Tardía justificada";
            case TARDIA -> "Tardía injustificada";
            case PRESENTE -> "";
        };
    }

    private String etiquetaEstado(EstadoIncidente estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En proceso";
            case RESUELTO -> "Resuelto";
            case APELADO -> "Apelado";
        };
    }

    private String claseEstado(EstadoIncidente estado) {
        return switch (estado) {
            case PENDIENTE -> "pendiente";
            case EN_PROCESO -> "enproceso";
            case RESUELTO -> "resuelto";
            case APELADO -> "apelado";
        };
    }

    private String claseAusencia(EstadoAsistencia estado) {
        return switch (estado) {
            case JUSTIFICADA -> "justificada";
            case AUSENTE -> "injustificada";
            case TARDIA_JUSTIFICADA -> "tardia-justificada";
            case TARDIA -> "tardia";
            case PRESENTE -> "";
        };
    }

    private NotasArmadas materiasPorEstudiante(Long direccionId, List<Long> nivelIds,
            List<Usuario> estudiantes, List<PeriodoAcademico> periodos) {
        if (nivelIds.isEmpty() || periodos.isEmpty() || estudiantes.isEmpty()) {
            return new NotasArmadas(Map.of(), Map.of());
        }
        List<Long> periodoIds = periodos.stream().map(PeriodoAcademico::getId).toList();
        Map<Long, Long> ordenPeriodo = new HashMap<>();
        for (PeriodoAcademico periodo : periodos) {
            ordenPeriodo.put(periodo.getId(), periodo.getFechaInicio().toEpochDay());
        }

        Map<Long, Map<Long, MateriaVista>> porNivel = new HashMap<>();
        for (Object[] fila : horarioRepository.findAsignacionesNivelMateria(direccionId, periodoIds, nivelIds)) {
            registrar(porNivel, (Long) fila[0], (Long) fila[1], (String) fila[2], (String) fila[3],
                    (Long) fila[4], ordenPeriodo);
        }

        List<ResultadoComponente> resultados = resultadoRepository.findNotasDePeriodos(direccionId, periodoIds,
                nivelIds);
        Map<NotaKey, List<ResultadoComponente>> porNota = new HashMap<>();
        for (ResultadoComponente resultado : resultados) {
            Componente componente = resultado.getComponente();
            if (componente.getMateria() == null || componente.getNivel() == null || resultado.getPeriodo() == null
                    || resultado.getEstudiante() == null) {
                continue;
            }
            Materia materia = componente.getMateria();
            Long periodoId = resultado.getPeriodo().getId();
            registrar(porNivel, componente.getNivel().getId(), materia.getId(), materia.getNombre(), null,
                    periodoId, ordenPeriodo);
            porNota.computeIfAbsent(
                    new NotaKey(resultado.getEstudiante().getId(), materia.getId(), periodoId),
                    k -> new ArrayList<>()).add(resultado);
        }

        Map<PesoKey, Map<Long, Double>> pesos = pesosDe(
                componenteRepository.findParaNotas(direccionId, nivelIds), periodoIds);
        boolean conAsistencia = rebajaConductaService.asistenciaRebajaComponente(direccionId);
        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        Distribuciones distribuciones = distribucionesDe(direccionId, periodoIds, conAsistencia);
        AsistenciaCargada cargada = asistenciaDe(direccionId, nivelIds, periodos);
        Map<AsistKey, int[]> asistencia = conAsistencia ? cargada.porMateria() : Map.of();
        Set<ClaveComponente> claves = clavesActivas(direccionId);
        List<Long> estudianteIds = estudiantes.stream().map(Usuario::getId).toList();
        Map<Long, Map<Long, Integer>> descuentosConducta = descuentosConducta(direccionId, periodoIds, estudianteIds);
        Map<Long, Map<Long, Integer>> llamadasConducta = llamadasDe(direccionId, periodoIds, estudianteIds);
        Map<Long, Map<Long, int[]>> rebajasAsistencia = conteosParaConducta(direccionId, periodos, estudianteIds,
                calculo);
        List<Long> periodosOrdenados = periodos.stream().map(PeriodoAcademico::getId).toList();

        Map<Long, List<MateriaNota>> porEstudiante = new HashMap<>();
        for (Usuario estudiante : estudiantes) {
            Long nivelId = estudiante.getNivelAcademico().getId();
            Map<Long, MateriaVista> materias = porNivel.getOrDefault(nivelId, Map.of());
            List<MateriaNota> notas = new ArrayList<>();
            for (MateriaVista vista : materias.values().stream()
                    .sorted(Comparator.comparing(v -> v.nombre, String.CASE_INSENSITIVE_ORDER))
                    .toList()) {
                List<Long> periodosMateria = vista.periodoIds.stream()
                        .sorted(Comparator.comparing(id -> ordenPeriodo.getOrDefault(id, 0L)))
                        .toList();
                List<NotaPeriodo> delEstudiante = new ArrayList<>();
                for (Long periodoId : periodosMateria) {
                    NotaPeriodo nota = notaDe(estudiante.getId(), nivelId, vista.id, periodoId, claves, porNota,
                            pesos, distribuciones, asistencia, calculo);
                    if (nota != null) {
                        delEstudiante.add(nota);
                    }
                }
                notas.add(new MateriaNota(vista.id, vista.nombre, vista.docente, periodosMateria, delEstudiante));
            }
            notas.add(conducta(periodosOrdenados,
                    descuentosConducta.getOrDefault(estudiante.getId(), Map.of()),
                    llamadasConducta.getOrDefault(estudiante.getId(), Map.of()),
                    rebajasAsistencia.getOrDefault(estudiante.getId(), Map.of()), calculo));
            porEstudiante.put(estudiante.getId(), notas);
        }
        return new NotasArmadas(porEstudiante, ausenciasDe(cargada.porEstudiante()));
    }

    /**
     * Misma regla que el módulo de conducta: cada período inicia en 100. Las boletas descuentan
     * sus puntos, las llamadas usan la regla y, si las ausencias y tardías rebajan la conducta,
     * también ellas. La fila va siempre al final, después de las materias.
     */
    private MateriaNota conducta(List<Long> periodoIds, Map<Long, Integer> descuentos, Map<Long, Integer> llamadas,
            Map<Long, int[]> ausencias, CalculoRebaja calculo) {
        List<NotaPeriodo> notas = new ArrayList<>();
        for (Long periodoId : periodoIds) {
            int nota = calculo.nota(descuentos.getOrDefault(periodoId, 0), llamadas.getOrDefault(periodoId, 0),
                    ausencias.get(periodoId));
            notas.add(new NotaPeriodo(periodoId, (double) nota, null, null));
        }
        return new MateriaNota(CONDUCTA_ID, "Conducta", "", periodoIds, notas);
    }

    private Map<Long, Map<Long, Integer>> llamadasDe(Long direccionId, List<Long> periodoIds, List<Long> estudianteIds) {
        Map<Long, Map<Long, Integer>> llamadas = new HashMap<>();
        if (estudianteIds.isEmpty() || periodoIds.isEmpty()) {
            return llamadas;
        }
        for (Object[] fila : incidenteRepository.contarPorEstudiantePeriodoYTipo(direccionId, periodoIds,
                estudianteIds)) {
            if (!TipoIncidente.LLAMADA_ATENCION.name().equals(String.valueOf(fila[2]))) {
                continue;
            }
            int cantidad = fila[3] == null ? 0 : ((Number) fila[3]).intValue();
            llamadas.computeIfAbsent((Long) fila[0], id -> new HashMap<>()).put((Long) fila[1], cantidad);
        }
        return llamadas;
    }

    private Map<Long, Map<Long, int[]>> conteosParaConducta(Long direccionId, List<PeriodoAcademico> periodos,
            List<Long> estudianteIds, CalculoRebaja calculo) {
        if (!calculo.asistenciaRebajaConducta() || estudianteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<Long, int[]>> conteos = new HashMap<>();
        for (PeriodoAcademico periodo : periodos) {
            if (periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
                continue;
            }
            for (Object[] fila : asistenciaRepository.contarEstadosPorEstudiante(direccionId, estudianteIds,
                    periodo.getFechaInicio(), periodo.getFechaFin(), EstadoAsistencia.PRESENTE)) {
                int[] conteo = conteos
                        .computeIfAbsent((Long) fila[0], id -> new HashMap<>())
                        .computeIfAbsent(periodo.getId(), id -> new int[4]);
                int cantidad = fila[2] == null ? 0 : ((Number) fila[2]).intValue();
                CalculoRebaja.sumar(conteo, (EstadoAsistencia) fila[1], cantidad);
            }
        }
        return conteos;
    }

    private Map<Long, Map<Long, Integer>> descuentosConducta(Long direccionId, List<Long> periodoIds,
            List<Long> estudianteIds) {
        Map<Long, Map<Long, Integer>> descuentos = new HashMap<>();
        for (Object[] fila : incidenteRepository.sumarPuntosPorEstudianteYPeriodo(
                direccionId, periodoIds, estudianteIds, TipoIncidente.BOLETA)) {
            int puntos = fila[2] == null ? 0 : ((Number) fila[2]).intValue();
            descuentos.computeIfAbsent((Long) fila[0], k -> new HashMap<>()).put((Long) fila[1], puntos);
        }
        return descuentos;
    }

    private void registrar(Map<Long, Map<Long, MateriaVista>> porNivel, Long nivelId, Long materiaId, String nombre,
            String docente, Long periodoId, Map<Long, Long> ordenPeriodo) {
        if (nivelId == null || materiaId == null || periodoId == null) {
            return;
        }
        MateriaVista vista = porNivel
                .computeIfAbsent(nivelId, k -> new LinkedHashMap<>())
                .computeIfAbsent(materiaId, k -> new MateriaVista(materiaId, nombre == null ? "" : nombre));
        if (vista.nombre.isBlank() && nombre != null) {
            vista.nombre = nombre;
        }
        vista.periodoIds.add(periodoId);
        if (docente == null || docente.isBlank()) {
            return;
        }
        long orden = ordenPeriodo.getOrDefault(periodoId, 0L);
        if (orden > vista.docenteOrden) {
            vista.docente = docente;
            vista.docenteOrden = orden;
        } else if (orden == vista.docenteOrden && !vista.docente.equals(docente) && !vista.docente.contains(docente)) {
            vista.docente = vista.docente.isBlank() ? docente : vista.docente + " · " + docente;
        }
    }

    private NotaPeriodo notaDe(Long estudianteId, Long nivelId, Long materiaId, Long periodoId,
            Set<ClaveComponente> claves, Map<NotaKey, List<ResultadoComponente>> porNota,
            Map<PesoKey, Map<Long, Double>> pesos, Distribuciones distribuciones,
            Map<AsistKey, int[]> asistencia, CalculoRebaja calculo) {
        List<ResultadoComponente> resultados = porNota.getOrDefault(
                new NotaKey(estudianteId, materiaId, periodoId), List.of());
        int[] asist = asistencia.get(new AsistKey(estudianteId, materiaId, periodoId));
        if (resultados.isEmpty() && !tieneLecciones(asist)) {
            return null;
        }

        int[] dist = distribuciones.de(periodoId, materiaId);
        ComponentesNota componentes = ponderar(resultados, asist, dist, claves,
                pesosDeMateria(pesos, nivelId, materiaId, periodoId), calculo);

        int puntos = 0;
        int totales = 0;
        boolean tienePuntos = false;
        for (ResultadoComponente resultado : resultados) {
            if (resultado.getComponente().getClave() == null
                    || !claves.contains(resultado.getComponente().getClave())) {
                continue;
            }
            Integer obtenidos = resultado.getPuntosObtenidos();
            Integer posibles = resultado.getComponente().getPuntosTotales();
            if (obtenidos == null || posibles == null) {
                continue;
            }
            puntos += obtenidos;
            totales += posibles;
            tienePuntos = true;
        }

        if (componentes.porcentaje() == null && !tienePuntos) {
            return null;
        }
        return new NotaPeriodo(periodoId, componentes.porcentaje(), tienePuntos ? puntos : null,
                tienePuntos ? totales : null);
    }

    private Map<ClaveComponente, Map<Long, Double>> pesosDeMateria(Map<PesoKey, Map<Long, Double>> pesos,
            Long nivelId, Long materiaId, Long periodoId) {
        Map<ClaveComponente, Map<Long, Double>> porClave = new HashMap<>();
        for (ClaveComponente clave : ClaveComponente.values()) {
            porClave.put(clave, pesos.getOrDefault(
                    new PesoKey(nivelId, materiaId, periodoDe(clave, periodoId), clave), Map.of()));
        }
        return porClave;
    }

    /** Misma ponderación que la nota del período: un componente entra solo si tiene calificación y peso. */
    private ComponentesNota ponderar(List<ResultadoComponente> resultados, int[] asist, int[] dist,
            Set<ClaveComponente> claves, Map<ClaveComponente, Map<Long, Double>> pesosPorClave, CalculoRebaja calculo) {
        Integer cotidiano = null;
        Integer tareas = null;
        Integer proyecto = null;
        Integer pruebas = null;
        double sumaPonderada = 0;
        double sumaPesos = 0;
        for (ClaveComponente clave : claves) {
            Integer promedio = promedioDe(resultados, clave, pesosPorClave.getOrDefault(clave, Map.of()));
            switch (clave) {
                case COTIDIANO -> cotidiano = promedio;
                case TAREA -> tareas = promedio;
                case PROYECTO -> proyecto = promedio;
                case EXAMEN -> pruebas = promedio;
            }
            int peso = pesoDe(dist, clave);
            if (promedio == null || peso <= 0) {
                continue;
            }
            sumaPonderada += promedio * peso;
            sumaPesos += peso;
        }
        Integer asistencia = null;
        if (tieneLecciones(asist)) {
            asistencia = calculo.notaAsistencia(asist);
            if (dist[4] > 0) {
                sumaPonderada += asistencia * dist[4];
                sumaPesos += dist[4];
            }
        }
        Double porcentaje = sumaPesos <= 0 ? null : Math.round((sumaPonderada / sumaPesos) * 10) / 10.0;
        return new ComponentesNota(cotidiano, tareas, proyecto, pruebas, asistencia, porcentaje);
    }

    private Map<Long, int[]> asistenciaDeEstudiante(Long direccionId, Long estudianteId, PeriodoAcademico periodo) {
        Map<Long, int[]> mapa = new HashMap<>();
        for (Object[] fila : asistenciaRepository.findEstadosDeEstudiante(
                direccionId, estudianteId, periodo.getFechaInicio(), periodo.getFechaFin())) {
            contarLeccion(mapa.computeIfAbsent((Long) fila[0], k -> conteoAsistencia()),
                    (EstadoAsistencia) fila[1]);
        }
        return mapa;
    }

    private Integer promedioDe(List<ResultadoComponente> resultados, ClaveComponente clave, Map<Long, Double> pesos) {
        double suma = 0;
        double sumaPesos = 0;
        for (ResultadoComponente resultado : resultados) {
            if (resultado.getComponente().getClave() != clave || resultado.getCalificacion() == null) {
                continue;
            }
            double peso = pesos.getOrDefault(resultado.getComponente().getId(), 0.0);
            if (peso <= 0) {
                continue;
            }
            suma += resultado.getCalificacion() * peso;
            sumaPesos += peso;
        }
        if (sumaPesos <= 0) {
            return null;
        }
        return (int) Math.round(suma / sumaPesos);
    }

    private Map<PesoKey, Map<Long, Double>> pesosDe(List<Componente> componentes, List<Long> periodoIds) {
        Set<Long> periodos = Set.copyOf(periodoIds);
        Map<PesoKey, List<Componente>> grupos = new HashMap<>();
        for (Componente componente : componentes) {
            if (componente.getClave() == null || componente.getNivel() == null || componente.getMateria() == null) {
                continue;
            }
            boolean exigePeriodo = componente.getClave() == ClaveComponente.PROYECTO
                    || componente.getClave() == ClaveComponente.EXAMEN;
            Long periodoId = null;
            if (exigePeriodo) {
                if (componente.getPeriodo() == null || !periodos.contains(componente.getPeriodo().getId())) {
                    continue;
                }
                periodoId = componente.getPeriodo().getId();
            }
            grupos.computeIfAbsent(
                    new PesoKey(componente.getNivel().getId(), componente.getMateria().getId(), periodoId,
                            componente.getClave()),
                    k -> new ArrayList<>()).add(componente);
        }
        Map<PesoKey, Map<Long, Double>> pesos = new HashMap<>();
        grupos.forEach((clave, lista) -> pesos.put(clave, pesosEfectivos(lista)));
        return pesos;
    }

    /** Igual que ComponenteService: el porcentaje fijo se respeta y el resto se reparte entre ponderados. */
    private Map<Long, Double> pesosEfectivos(List<Componente> componentes) {
        Map<Long, Double> pesos = new LinkedHashMap<>();
        int sumaFijos = componentes.stream()
                .filter(c -> c.getPorcentaje() != null)
                .mapToInt(Componente::getPorcentaje)
                .sum();
        long ponderados = componentes.stream().filter(Componente::isPonderado).count();
        double resto = Math.max(0, 100 - sumaFijos);
        double pesoPonderado = ponderados == 0 ? 0 : resto / ponderados;
        for (Componente componente : componentes) {
            pesos.put(componente.getId(),
                    componente.isPonderado() ? pesoPonderado : componente.getPorcentaje().doubleValue());
        }
        return pesos;
    }

    private Distribuciones distribucionesDe(Long direccionId, List<Long> periodoIds, boolean conAsistencia) {
        Map<DistKey, int[]> guardadas = new HashMap<>();
        for (DistribucionPorcentual dist : distribucionRepository.findByDireccionIdAndPeriodoIdIn(direccionId,
                periodoIds)) {
            guardadas.put(new DistKey(dist.getPeriodo().getId(), dist.getMateria().getId()), pesos(dist));
        }
        return new Distribuciones(guardadas, pesos(DistribucionPorcentual.predeterminada(conAsistencia)));
    }

    private static int[] pesos(DistribucionPorcentual dist) {
        return new int[] {
                dist.getCotidiano(), dist.getTareas(), dist.getProyectos(), dist.getExamenes(), dist.getAsistencia()
        };
    }

    private AsistenciaCargada asistenciaDe(Long direccionId, List<Long> nivelIds, List<PeriodoAcademico> periodos) {
        LocalDate desde = periodos.stream().map(PeriodoAcademico::getFechaInicio).min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate hasta = periodos.stream().map(PeriodoAcademico::getFechaFin).max(LocalDate::compareTo)
                .orElseThrow();
        Map<AsistKey, int[]> mapa = new HashMap<>();
        Map<Long, Map<Long, int[]>> ausencias = new HashMap<>();
        for (Object[] fila : asistenciaRepository.findEstadosEntre(direccionId, nivelIds, desde, hasta)) {
            LocalDate fecha = (LocalDate) fila[2];
            EstadoAsistencia estado = (EstadoAsistencia) fila[3];
            Long estudianteId = (Long) fila[0];
            for (PeriodoAcademico periodo : periodos) {
                if (!periodo.contiene(fecha)) {
                    continue;
                }
                contarLeccion(mapa.computeIfAbsent(
                        new AsistKey(estudianteId, (Long) fila[1], periodo.getId()), k -> conteoAsistencia()),
                        estado);
                acumularAusencia(ausencias, estudianteId, periodo.getId(), estado);
            }
        }
        return new AsistenciaCargada(mapa, ausencias);
    }

    /** [ausencia justificada, injustificada, tardía justificada, injustificada, lecciones]. */
    private static int[] conteoAsistencia() {
        return new int[5];
    }

    private static void contarLeccion(int[] conteo, EstadoAsistencia estado) {
        conteo[4]++;
        CalculoRebaja.sumar(conteo, estado, 1);
    }

    private static boolean tieneLecciones(int[] asist) {
        return asist != null && asist.length > 4 && asist[4] > 0;
    }

    /** Cada lección cuenta una vez. Presente no entra en el cuadro de ausencias. */
    private void acumularAusencia(Map<Long, Map<Long, int[]>> porEstudiante, Long estudianteId, Long periodoId,
            EstadoAsistencia estado) {
        if (CalculoRebaja.indice(estado) < 0) {
            return;
        }
        int[] conteo = porEstudiante
                .computeIfAbsent(estudianteId, k -> new HashMap<>())
                .computeIfAbsent(periodoId, k -> new int[4]);
        CalculoRebaja.sumar(conteo, estado, 1);
    }

    private Map<Long, List<AusenciaPeriodo>> ausenciasDe(Map<Long, Map<Long, int[]>> porEstudiante) {
        Map<Long, List<AusenciaPeriodo>> resultado = new HashMap<>();
        porEstudiante.forEach((estudianteId, porPeriodo) -> {
            List<AusenciaPeriodo> filas = new ArrayList<>();
            porPeriodo.forEach((periodoId, conteo) -> filas.add(new AusenciaPeriodo(
                    periodoId, conteo[0], conteo[1], conteo[2], conteo[3])));
            resultado.put(estudianteId, filas);
        });
        return resultado;
    }

    private Set<ClaveComponente> clavesActivas(Long direccionId) {
        List<ClaveComponente> claves = tipoComponenteRepository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId)
                .stream()
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()) && tipo.getClave() != null)
                .map(TipoComponente::getClave)
                .distinct()
                .toList();
        if (claves.isEmpty()) {
            return Set.of(ClaveComponente.COTIDIANO, ClaveComponente.TAREA, ClaveComponente.PROYECTO,
                    ClaveComponente.EXAMEN);
        }
        return Set.copyOf(claves);
    }

    private Long periodoDe(ClaveComponente clave, Long periodoId) {
        if (clave == ClaveComponente.PROYECTO || clave == ClaveComponente.EXAMEN) {
            return periodoId;
        }
        return null;
    }

    private int pesoDe(int[] dist, ClaveComponente clave) {
        return switch (clave) {
            case COTIDIANO -> dist[0];
            case TAREA -> dist[1];
            case PROYECTO -> dist[2];
            case EXAMEN -> dist[3];
        };
    }

    private List<NivelAcademico> nivelesVisibles(Long direccionId, Long usuarioId, boolean catalogoCompleto) {
        if (catalogoCompleto) {
            return nivelRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
        }
        if (usuarioId == null) {
            return List.of();
        }
        return docenteGuiaService.listarSecciones(direccionId, usuarioId).stream()
                .filter(n -> Boolean.TRUE.equals(n.getActivo()))
                .toList();
    }

    private PeriodoOpcion periodo(PeriodoAcademico periodo) {
        String titulo = periodo.getDescripcion() == null || periodo.getDescripcion().isBlank()
                ? fechas(periodo)
                : periodo.getDescripcion() + " (" + fechas(periodo) + ")";
        return new PeriodoOpcion(periodo.getId(), periodo.getFechaInicio().getYear(), periodo.getCodigo(), titulo);
    }

    private String fechas(PeriodoAcademico periodo) {
        return FECHA.format(periodo.getFechaInicio()) + " – " + FECHA.format(periodo.getFechaFin());
    }

    private NivelOpcion nivel(NivelAcademico nivel) {
        return new NivelOpcion(nivel.getId(), nivel.getGrado(), nivel.getSeccion(),
                nivel.getGrado() + "-" + nivel.getSeccion());
    }

    private Map<Long, List<ObservacionPeriodo>> observacionesDe(Long direccionId, List<Usuario> estudiantes,
            List<PeriodoAcademico> periodos) {
        if (estudiantes.isEmpty() || periodos.isEmpty()) {
            return Map.of();
        }
        List<Long> estudianteIds = estudiantes.stream().map(Usuario::getId).toList();
        List<Long> periodoIds = periodos.stream().map(PeriodoAcademico::getId).toList();
        Map<Long, List<ObservacionPeriodo>> mapa = new HashMap<>();
        for (Object[] fila : observacionRepository.findTextos(direccionId, estudianteIds, periodoIds)) {
            mapa.computeIfAbsent((Long) fila[0], k -> new ArrayList<>())
                    .add(new ObservacionPeriodo((Long) fila[1], (String) fila[2]));
        }
        return mapa;
    }

    private Map<Long, List<SeguimientoPeriodo>> seguimientoDe(Long direccionId, List<Usuario> estudiantes,
            List<PeriodoAcademico> periodos) {
        if (estudiantes.isEmpty() || periodos.isEmpty()) {
            return Map.of();
        }
        List<Long> estudianteIds = estudiantes.stream().map(Usuario::getId).toList();
        List<Long> periodoIds = periodos.stream().map(PeriodoAcademico::getId).toList();
        Map<Long, Map<Long, int[]>> acumulado = new HashMap<>();
        for (Object[] fila : incidenteRepository.contarPorEstudiantePeriodoYTipo(
                direccionId, periodoIds, estudianteIds)) {
            int[] par = acumulado
                    .computeIfAbsent((Long) fila[0], k -> new HashMap<>())
                    .computeIfAbsent((Long) fila[1], k -> new int[2]);
            int cantidad = fila[3] == null ? 0 : ((Number) fila[3]).intValue();
            String tipo = String.valueOf(fila[2]);
            if (TipoIncidente.LLAMADA_ATENCION.name().equals(tipo)) {
                par[0] = cantidad;
            } else if (TipoIncidente.BOLETA.name().equals(tipo)) {
                par[1] = cantidad;
            }
        }
        Map<Long, List<SeguimientoPeriodo>> mapa = new HashMap<>();
        acumulado.forEach((estudianteId, porPeriodo) -> {
            List<SeguimientoPeriodo> lista = new ArrayList<>();
            porPeriodo.forEach((periodoId, par) -> lista.add(new SeguimientoPeriodo(periodoId, par[0], par[1])));
            mapa.put(estudianteId, lista);
        });
        return mapa;
    }

    private EstudianteOpcion estudiante(Usuario usuario, List<MateriaNota> materias,
            List<AusenciaPeriodo> ausencias, List<ObservacionPeriodo> observaciones,
            List<SeguimientoPeriodo> seguimiento) {
        NivelAcademico nivel = usuario.getNivelAcademico();
        long id = usuario.getId();
        return new EstudianteOpcion(
                id,
                usuario.getNombre(),
                usuario.getCedula() == null ? "" : usuario.getCedula(),
                nivel.getGrado(),
                nivel.getSeccion(),
                nivel.getId(),
                iniciales(usuario.getNombre()),
                COLORES[(int) Math.floorMod(id, COLORES.length)],
                materias,
                ausencias,
                observaciones,
                seguimiento);
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (primera + segunda).toUpperCase();
    }

    private static final class MateriaVista {
        private final long id;
        private String nombre;
        private String docente = "";
        private long docenteOrden = Long.MIN_VALUE;
        private final Set<Long> periodoIds = new LinkedHashSet<>();

        private MateriaVista(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    private record ComponentesNota(Integer cotidiano, Integer tareas, Integer proyecto, Integer pruebas,
            Integer asistencia, Double porcentaje) {
    }

    private record NotaKey(long estudianteId, long materiaId, long periodoId) {
    }

    private record PesoKey(long nivelId, long materiaId, Long periodoId, ClaveComponente clave) {
    }

    private record DistKey(long periodoId, long materiaId) {
    }

    private record Distribuciones(Map<DistKey, int[]> guardadas, int[] predeterminada) {
        int[] de(Long periodoId, Long materiaId) {
            return guardadas.getOrDefault(new DistKey(periodoId, materiaId), predeterminada);
        }
    }

    private record AsistKey(long estudianteId, long materiaId, long periodoId) {
    }

    private record AsistenciaCargada(Map<AsistKey, int[]> porMateria, Map<Long, Map<Long, int[]>> porEstudiante) {
    }

    private record NotasArmadas(Map<Long, List<MateriaNota>> materias, Map<Long, List<AusenciaPeriodo>> ausencias) {
    }
}
