package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.NotasCatalogo;
import com.chavescr.nexa.dto.NotasCatalogo.AnioOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.EstudianteOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.MateriaNota;
import com.chavescr.nexa.dto.NotasCatalogo.NivelOpcion;
import com.chavescr.nexa.dto.NotasCatalogo.NotaPeriodo;
import com.chavescr.nexa.dto.NotasCatalogo.PeriodoOpcion;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.DistribucionPorcentual;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.ResultadoComponente;
import com.chavescr.nexa.entity.TipoComponente;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.ComponenteRepository;
import com.chavescr.nexa.repository.DistribucionPorcentualRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.ResultadoComponenteRepository;
import com.chavescr.nexa.repository.TipoComponenteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class NotasConsultaService {

    private static final long CONDUCTA_ID = -1L;
    private static final int NOTA_CONDUCTA_INICIAL = 100;
    private static final int[] DISTRIBUCION_DEFECTO = { 40, 15, 20, 20, 5 };
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

    public NotasConsultaService(PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository, UsuarioRepository usuarioRepository,
            DocenteGuiaService docenteGuiaService, HorarioLeccionRepository horarioRepository,
            ComponenteRepository componenteRepository, ResultadoComponenteRepository resultadoRepository,
            DistribucionPorcentualRepository distribucionRepository,
            AsistenciaEstudianteRepository asistenciaRepository,
            TipoComponenteRepository tipoComponenteRepository,
            IncidenteConductaRepository incidenteRepository) {
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

        Map<Long, List<MateriaNota>> materias = materiasPorEstudiante(direccionId, nivelIds, estudiantes, visibles);

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
                        .map(u -> estudiante(u, materias.getOrDefault(u.getId(), List.of())))
                        .toList());
    }

    private Map<Long, List<MateriaNota>> materiasPorEstudiante(Long direccionId, List<Long> nivelIds,
            List<Usuario> estudiantes, List<PeriodoAcademico> periodos) {
        if (nivelIds.isEmpty() || periodos.isEmpty() || estudiantes.isEmpty()) {
            return Map.of();
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
        Map<DistKey, int[]> distribuciones = distribucionesDe(direccionId, periodoIds);
        Map<AsistKey, int[]> asistencia = asistenciaDe(direccionId, nivelIds, periodos);
        Set<ClaveComponente> claves = clavesActivas(direccionId);
        Map<Long, Map<Long, Integer>> descuentosConducta = descuentosConducta(direccionId, periodoIds,
                estudiantes.stream().map(Usuario::getId).toList());
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
                            pesos, distribuciones, asistencia);
                    if (nota != null) {
                        delEstudiante.add(nota);
                    }
                }
                notas.add(new MateriaNota(vista.id, vista.nombre, vista.docente, periodosMateria, delEstudiante));
            }
            notas.add(conducta(periodosOrdenados,
                    descuentosConducta.getOrDefault(estudiante.getId(), Map.of())));
            porEstudiante.put(estudiante.getId(), notas);
        }
        return porEstudiante;
    }

    /**
     * Misma regla que el módulo de conducta: cada período inicia en 100 y solo las boletas descuentan.
     * La fila va siempre al final, después de las materias.
     */
    private MateriaNota conducta(List<Long> periodoIds, Map<Long, Integer> descuentos) {
        List<NotaPeriodo> notas = new ArrayList<>();
        for (Long periodoId : periodoIds) {
            int descuento = descuentos.getOrDefault(periodoId, 0);
            double nota = Math.max(0, NOTA_CONDUCTA_INICIAL - descuento);
            notas.add(new NotaPeriodo(periodoId, nota, null, null));
        }
        return new MateriaNota(CONDUCTA_ID, "Conducta", "", periodoIds, notas);
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
            Map<PesoKey, Map<Long, Double>> pesos, Map<DistKey, int[]> distribuciones,
            Map<AsistKey, int[]> asistencia) {
        List<ResultadoComponente> resultados = porNota.getOrDefault(
                new NotaKey(estudianteId, materiaId, periodoId), List.of());
        int[] asist = asistencia.get(new AsistKey(estudianteId, materiaId, periodoId));
        if (resultados.isEmpty() && (asist == null || asist[1] == 0)) {
            return null;
        }

        int[] dist = distribuciones.getOrDefault(new DistKey(periodoId, materiaId), DISTRIBUCION_DEFECTO);
        double sumaPonderada = 0;
        double sumaPesos = 0;
        for (ClaveComponente clave : claves) {
            Integer promedio = promedioDe(resultados, clave,
                    pesos.getOrDefault(new PesoKey(nivelId, materiaId, periodoDe(clave, periodoId), clave), Map.of()));
            int peso = pesoDe(dist, clave);
            if (promedio == null || peso <= 0) {
                continue;
            }
            sumaPonderada += promedio * peso;
            sumaPesos += peso;
        }
        if (asist != null && asist[1] > 0 && dist[4] > 0) {
            int score = (int) Math.round(asist[0] * 100.0 / asist[1]);
            sumaPonderada += score * dist[4];
            sumaPesos += dist[4];
        }

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

        Double porcentaje = sumaPesos <= 0 ? null : Math.round((sumaPonderada / sumaPesos) * 10) / 10.0;
        if (porcentaje == null && !tienePuntos) {
            return null;
        }
        return new NotaPeriodo(periodoId, porcentaje, tienePuntos ? puntos : null, tienePuntos ? totales : null);
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

    private Map<DistKey, int[]> distribucionesDe(Long direccionId, List<Long> periodoIds) {
        Map<DistKey, int[]> mapa = new HashMap<>();
        for (DistribucionPorcentual dist : distribucionRepository.findByDireccionIdAndPeriodoIdIn(direccionId,
                periodoIds)) {
            mapa.put(new DistKey(dist.getPeriodo().getId(), dist.getMateria().getId()), new int[] {
                    dist.getCotidiano(), dist.getTareas(), dist.getProyectos(), dist.getExamenes(),
                    dist.getAsistencia()
            });
        }
        return mapa;
    }

    private Map<AsistKey, int[]> asistenciaDe(Long direccionId, List<Long> nivelIds, List<PeriodoAcademico> periodos) {
        LocalDate desde = periodos.stream().map(PeriodoAcademico::getFechaInicio).min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate hasta = periodos.stream().map(PeriodoAcademico::getFechaFin).max(LocalDate::compareTo)
                .orElseThrow();
        Map<AsistKey, int[]> mapa = new HashMap<>();
        for (Object[] fila : asistenciaRepository.findEstadosEntre(direccionId, nivelIds, desde, hasta)) {
            LocalDate fecha = (LocalDate) fila[2];
            EstadoAsistencia estado = (EstadoAsistencia) fila[3];
            for (PeriodoAcademico periodo : periodos) {
                if (!periodo.contiene(fecha)) {
                    continue;
                }
                int[] contador = mapa.computeIfAbsent(
                        new AsistKey((Long) fila[0], (Long) fila[1], periodo.getId()), k -> new int[2]);
                contador[1]++;
                if (estado == EstadoAsistencia.PRESENTE || estado == EstadoAsistencia.TARDIA) {
                    contador[0]++;
                }
            }
        }
        return mapa;
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

    private EstudianteOpcion estudiante(Usuario usuario, List<MateriaNota> materias) {
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
                materias);
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

    private record NotaKey(long estudianteId, long materiaId, long periodoId) {
    }

    private record PesoKey(long nivelId, long materiaId, Long periodoId, ClaveComponente clave) {
    }

    private record DistKey(long periodoId, long materiaId) {
    }

    private record AsistKey(long estudianteId, long materiaId, long periodoId) {
    }
}
