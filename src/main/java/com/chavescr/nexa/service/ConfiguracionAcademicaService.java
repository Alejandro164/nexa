package com.chavescr.nexa.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Aula;
import com.chavescr.nexa.entity.BloqueoLeccion;
import com.chavescr.nexa.entity.ConfiguracionDireccion;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoAula;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AulaRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.TipoAulaRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ConfiguracionAcademicaService {

    private final DireccionRepository direccionRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final MateriaRepository materiaRepository;
    private final TipoMateriaRepository tipoMateriaRepository;
    private final TipoAulaRepository tipoAulaRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AulaRepository aulaRepository;
    private final DocenteMateriaService docenteMateriaService;
    private final DocenteGuiaService docenteGuiaService;
    private final DocenteBloqueoService docenteBloqueoService;
    private final BloqueoLeccionService bloqueoLeccionService;
    private final SeccionBloqueoService seccionBloqueoService;
    private final EnvioNotasDocenteService envioNotasDocenteService;
    private final ConfiguracionDireccionService configuracionDireccionService;

    public ConfiguracionAcademicaService(DireccionRepository direccionRepository,
            PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository,
            MateriaRepository materiaRepository,
            TipoMateriaRepository tipoMateriaRepository,
            TipoAulaRepository tipoAulaRepository,
            HorarioLeccionRepository horarioRepository,
            UsuarioRepository usuarioRepository,
            AulaRepository aulaRepository,
            DocenteMateriaService docenteMateriaService,
            DocenteGuiaService docenteGuiaService,
            DocenteBloqueoService docenteBloqueoService,
            BloqueoLeccionService bloqueoLeccionService,
            SeccionBloqueoService seccionBloqueoService,
            EnvioNotasDocenteService envioNotasDocenteService,
            ConfiguracionDireccionService configuracionDireccionService) {
        this.direccionRepository = direccionRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.materiaRepository = materiaRepository;
        this.tipoMateriaRepository = tipoMateriaRepository;
        this.tipoAulaRepository = tipoAulaRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.aulaRepository = aulaRepository;
        this.docenteMateriaService = docenteMateriaService;
        this.docenteGuiaService = docenteGuiaService;
        this.docenteBloqueoService = docenteBloqueoService;
        this.bloqueoLeccionService = bloqueoLeccionService;
        this.seccionBloqueoService = seccionBloqueoService;
        this.envioNotasDocenteService = envioNotasDocenteService;

        this.configuracionDireccionService = configuracionDireccionService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ConfiguracionDireccion obtenerConfiguracion(Long direccionId) {
        return configuracionDireccionService.obtener(direccionId);
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodos(Long direccionId) {
        return periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodosActivos(Long direccionId) {
        return periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId);
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodo(Long direccionId, Long id) {
        return periodoRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
    }

    public PeriodoAcademico guardarPeriodo(Long direccionId, PeriodoAcademico datos) {
        if (datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        if (datos.getId() == null) {
            periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId)
                    .stream().findFirst()
                    .ifPresent(actual -> {
                        var pendientes = envioNotasDocenteService.listarPendientes(direccionId, actual.getId());
                        if (!pendientes.isEmpty()) {
                            String nombres = pendientes.stream()
                                    .map(EnvioNotasDocenteService.DocentePendiente::nombre)
                                    .collect(Collectors.joining(", "));
                            throw new IllegalArgumentException(
                                    "No se puede crear un nuevo período: faltan " + pendientes.size()
                                            + " docente(s) por enviar notas del período actual (" + nombres + ")");
                        }
                    });
        }
        PeriodoAcademico periodo = datos.getId() == null
                ? new PeriodoAcademico()
                : obtenerPeriodo(direccionId, datos.getId());
        periodo.setDireccion(obtenerDireccion(direccionId));
        periodo.setCodigo(datos.getCodigo().trim().toUpperCase());
        periodo.setDescripcion(datos.getDescripcion().trim());
        periodo.setFechaInicio(datos.getFechaInicio());
        periodo.setFechaFin(datos.getFechaFin());
        periodo.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        return periodoRepository.save(periodo);
    }

    public void eliminarPeriodo(Long direccionId, Long id) {
        PeriodoAcademico periodo = obtenerPeriodo(direccionId, id);
        horarioRepository.deleteByDireccionIdAndPeriodoId(direccionId, id);
        docenteBloqueoService.eliminarPorPeriodo(direccionId, id);
        seccionBloqueoService.eliminarPorPeriodo(direccionId, id);
        envioNotasDocenteService.eliminarPorPeriodo(direccionId, id);
        periodoRepository.delete(periodo);
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarNiveles(Long direccionId) {
        return nivelRepository.findByDireccionIdOrderByGradoAscSeccionAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarNivelesActivos(Long direccionId) {
        return nivelRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public NivelAcademico obtenerNivel(Long direccionId, Long id) {
        return nivelRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado"));
    }

    public NivelAcademico guardarNivel(Long direccionId, NivelAcademico datos) {
        NivelAcademico nivel = datos.getId() == null
                ? new NivelAcademico()
                : obtenerNivel(direccionId, datos.getId());
        nivel.setDireccion(obtenerDireccion(direccionId));
        nivel.setGrado(datos.getGrado());
        nivel.setSeccion(datos.getSeccion().trim().toUpperCase());
        nivel.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        return nivelRepository.save(nivel);
    }

    public void eliminarNivel(Long direccionId, Long id) {
        NivelAcademico nivel = obtenerNivel(direccionId, id);
        horarioRepository.deleteByDireccionIdAndNivelId(direccionId, id);
        docenteGuiaService.eliminarPorNivel(direccionId, id);
        seccionBloqueoService.eliminarPorNivel(direccionId, id);
        nivelRepository.delete(nivel);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMaterias(Long direccionId) {
        return materiaRepository.findByDireccionIdOrderByNombreAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long direccionId) {
        return materiaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasParaHorario(Long direccionId, Long nivelId, String dia,
            Integer numeroLeccion, Long materiaActualId) {
        NivelAcademico nivel = obtenerNivel(direccionId, nivelId);
        return bloqueoLeccionService.filtrarMaterias(direccionId, nivel.getGrado(), dia, numeroLeccion,
                listarMateriasActivas(direccionId), materiaActualId);
    }

    @Transactional(readOnly = true)
    public BloqueoLeccion bloqueoDeCelda(Long direccionId, Long nivelId, String dia, Integer numeroLeccion) {
        NivelAcademico nivel = obtenerNivel(direccionId, nivelId);
        return bloqueoLeccionService.vigente(direccionId, nivel.getGrado(), dia, numeroLeccion);
    }

    @Transactional(readOnly = true)
    public List<TipoMateria> listarTiposMateriaActivos() {
        return tipoMateriaRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public Materia obtenerMateria(Long direccionId, Long id) {
        return materiaRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
    }

    public Materia guardarMateria(Long direccionId, Materia datos) {
        Materia materia = datos.getId() == null ? new Materia() : obtenerMateria(direccionId, datos.getId());
        TipoMateria tipoMateria = resolverTipoMateria(datos);
        materia.setDireccion(obtenerDireccion(direccionId));
        materia.setNombre(datos.getNombre().trim());
        materia.setTipoMateria(tipoMateria);
        materia.setTipo(tipoMateria.getNombre());
        materia.setColor(normalizarColor(datos.getColor()));
        materia.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        return materiaRepository.save(materia);
    }

    private TipoMateria resolverTipoMateria(Materia datos) {
        Long tipoMateriaId = datos.getTipoMateria() != null ? datos.getTipoMateria().getId() : null;
        if (tipoMateriaId == null) {
            throw new IllegalArgumentException("Debe seleccionar un tipo de materia");
        }
        return tipoMateriaRepository.findById(tipoMateriaId)
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de materia no encontrado"));
    }

    public void eliminarMateria(Long direccionId, Long id) {
        Materia materia = obtenerMateria(direccionId, id);
        horarioRepository.deleteByDireccionIdAndMateriaId(direccionId, id);
        docenteMateriaService.eliminarPorMateria(direccionId, id);
        materiaRepository.delete(materia);
    }

    @Transactional(readOnly = true)
    public List<Aula> listarAulas(Long direccionId) {
        return aulaRepository.findByDireccionIdOrderByNombreAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<Aula> listarAulasActivas(Long direccionId) {
        return aulaRepository.findByDireccionIdAndActivoTrueOrderByNombreAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<TipoAula> listarTiposAulaActivos() {
        return tipoAulaRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public Aula obtenerAula(Long direccionId, Long id) {
        return aulaRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada"));
    }

    public Aula guardarAula(Long direccionId, Aula datos) {
        if (datos.getCapacidad() == null || datos.getCapacidad() <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a cero");
        }
        Aula aula = datos.getId() == null ? new Aula() : obtenerAula(direccionId, datos.getId());
        TipoAula tipoAula = resolverTipoAula(datos);
        aula.setDireccion(obtenerDireccion(direccionId));
        aula.setNombre(datos.getNombre().trim());
        aula.setCapacidad(datos.getCapacidad());
        aula.setTipoAula(tipoAula);
        aula.setTipo(tipoAula.getNombre());
        aula.setUbicacion(datos.getUbicacion() != null && !datos.getUbicacion().isBlank()
                ? datos.getUbicacion().trim() : null);
        aula.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        return aulaRepository.save(aula);
    }

    private TipoAula resolverTipoAula(Aula datos) {
        Long tipoAulaId = datos.getTipoAula() != null ? datos.getTipoAula().getId() : null;
        if (tipoAulaId == null) {
            throw new IllegalArgumentException("Debe seleccionar un tipo de aula");
        }
        return tipoAulaRepository.findById(tipoAulaId)
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de aula no encontrado"));
    }

    public void eliminarAula(Long direccionId, Long id) {
        Aula aula = obtenerAula(direccionId, id);
        horarioRepository.deleteByDireccionIdAndAulaId(direccionId, id);
        aulaRepository.delete(aula);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarDocentes(Long direccionId) {
        return usuarioRepository.findActivosByDireccionIdAndRol(direccionId, "ROLE_DOCENTE");
    }

    /**
     * Profesores activos asociados a la materia. Si se edita una lección cuyo
     * docente ya no está asignado, se incluye para no romper el formulario.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarDocentesPorMateria(Long direccionId, Long materiaId, Long docenteSeleccionadoId) {
        if (materiaId == null) {
            return List.of();
        }
        List<Usuario> docentes = new ArrayList<>(
                docenteMateriaService.listarDocentesPorMateria(direccionId, materiaId));
        if (docenteSeleccionadoId != null
                && docentes.stream().noneMatch(docente -> docente.getId().equals(docenteSeleccionadoId))) {
            usuarioRepository.findActivoByIdAndDireccionId(docenteSeleccionadoId, direccionId)
                    .ifPresent(docente -> docentes.add(0, docente));
        }
        return docentes;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarDocentesDisponibles(Long direccionId, Long materiaId, Long docenteSeleccionadoId,
            Long periodoId, String dia, Integer numeroLeccion, Long leccionId) {
        List<Usuario> docentes = listarDocentesPorMateria(direccionId, materiaId, docenteSeleccionadoId);
        if (periodoId == null || dia == null || numeroLeccion == null) {
            return docentes;
        }
        Set<Long> noDisponibles = new HashSet<>(horarioRepository.findDocenteIdsEnBloque(
                direccionId, periodoId, dia, numeroLeccion, leccionId));
        noDisponibles.addAll(docenteBloqueoService.docenteIds(direccionId, periodoId, dia, numeroLeccion));
        return docentes.stream()
                .filter(d -> Objects.equals(d.getId(), docenteSeleccionadoId) || !noDisponibles.contains(d.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<HorarioLeccion>> obtenerHorario(Long direccionId, Long periodoId, Long nivelId) {
        Map<String, List<HorarioLeccion>> horario = new LinkedHashMap<>();
        if (periodoId == null || nivelId == null) return horario;
        horarioRepository.findByDireccionIdAndPeriodoIdAndNivelIdOrderByNumeroLeccionAsc(
                direccionId, periodoId, nivelId)
                .forEach(l -> horario
                        .computeIfAbsent(clave(l.getDia(), l.getNumeroLeccion()), k -> new ArrayList<>())
                        .add(l));
        return horario;
    }

    /** Igual que {@link #obtenerHorario}, pero acotado al horario de un docente en particular (para Disponibilidad). */
    @Transactional(readOnly = true)
    public Map<String, List<HorarioLeccion>> obtenerHorarioPorDocente(Long direccionId, Long periodoId, Long docenteId) {
        Map<String, List<HorarioLeccion>> horario = new LinkedHashMap<>();
        if (periodoId == null || docenteId == null) return horario;
        horarioRepository.findByDireccionIdAndPeriodoIdAndDocenteIdOrderByDiaAscNumeroLeccionAsc(
                direccionId, periodoId, docenteId)
                .forEach(l -> horario
                        .computeIfAbsent(clave(l.getDia(), l.getNumeroLeccion()), k -> new ArrayList<>())
                        .add(l));
        return horario;
    }

    /** Lecciones semanales por docente en un período (para el directorio). */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarLeccionesPorDocente(Long direccionId, Long periodoId) {
        Map<Long, Long> conteo = new LinkedHashMap<>();
        if (periodoId == null) {
            return conteo;
        }
        for (Object[] fila : horarioRepository.countLeccionesGroupedByDocente(direccionId, periodoId)) {
            Long docenteId = (Long) fila[0];
            long total = ((Number) fila[1]).longValue();
            conteo.put(docenteId, total);
        }
        return conteo;
    }

    @Transactional(readOnly = true)
    public HorarioLeccion obtenerLeccionPorId(Long direccionId, Long id) {
        return horarioRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Lección no encontrada"));
    }

    public HorarioLeccion guardarLeccion(Long direccionId, Long id, Long periodoId, Long nivelId,
            Long materiaId, Long docenteId, Long aulaId, String dia, Integer numeroLeccion) {
        ConfiguracionDireccion config = obtenerConfiguracion(direccionId);
        if (!config.getDias().contains(dia) || !config.getLecciones().contains(numeroLeccion)) {
            throw new IllegalArgumentException("Día o número de lección inválido");
        }
        Materia materia = obtenerMateria(direccionId, materiaId);
        seccionBloqueoService.tipoBloqueado(direccionId, periodoId, nivelId, dia, numeroLeccion)
                .filter(tipo -> materia.getTipoMateria() == null
                        || !materia.getTipoMateria().getId().equals(tipo.getId()))
                .ifPresent(tipo -> {
                    throw new IllegalArgumentException(
                            "Esta lección está bloqueada para materias de tipo " + tipo.getNombre());
                });
        validarDocenteDeMateria(direccionId, materiaId, docenteId, id);

        boolean docenteOcupado = horarioRepository
                .findByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                        direccionId, periodoId, docenteId, dia, numeroLeccion)
                .stream()
                .anyMatch(e -> !e.getId().equals(id));
        if (docenteOcupado) {
            throw new IllegalArgumentException("El docente ya tiene otra lección asignada en este horario");
        }
        if (docenteBloqueoService.estaBloqueado(direccionId, periodoId, docenteId, dia, numeroLeccion)) {
            throw new IllegalArgumentException("El docente no está disponible en esta lección");
        }
        NivelAcademico nivel = obtenerNivel(direccionId, nivelId);
        bloqueoLeccionService.validarAsignacion(direccionId, nivel.getGrado(), dia, numeroLeccion, materia);

        HorarioLeccion leccion;
        if (id != null) {
            leccion = obtenerLeccionPorId(direccionId, id);
        } else {
            List<HorarioLeccion> existentes = horarioRepository
                    .findByDireccionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccionOrderByIdAsc(
                            direccionId, periodoId, nivelId, dia, numeroLeccion);
            if (existentes.size() >= 3) {
                throw new IllegalArgumentException("No se pueden asignar más de 3 materias por lección");
            }
            boolean yaAsignada = existentes.stream()
                    .anyMatch(e -> e.getMateria().getId().equals(materiaId));
            if (yaAsignada) {
                throw new IllegalArgumentException("Esta materia ya está asignada en esta lección");
            }
            leccion = new HorarioLeccion();
        }

        leccion.setDireccion(obtenerDireccion(direccionId));
        leccion.setPeriodo(obtenerPeriodo(direccionId, periodoId));
        leccion.setNivel(nivel);
        leccion.setMateria(materia);
        leccion.setDocente(usuarioRepository.findActivoByIdAndDireccionId(docenteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Docente no válido para la dirección")));
        leccion.setAula(obtenerAula(direccionId, aulaId));
        leccion.setDia(dia);
        leccion.setNumeroLeccion(numeroLeccion);
        var jornada = config.jornada();
        leccion.setHoraInicio(jornada.horaInicioLeccion(numeroLeccion));
        leccion.setHoraFin(jornada.horaFinLeccion(numeroLeccion));
        return horarioRepository.save(leccion);
    }

    public void eliminarLeccion(Long direccionId, Long id) {
        horarioRepository.delete(horarioRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Lección no encontrada")));
    }

    @Transactional(readOnly = true)
    public Map<String, TipoMateria> obtenerBloqueosSeccion(Long direccionId, Long periodoId, Long nivelId) {
        return seccionBloqueoService.mapa(direccionId, periodoId, nivelId);
    }

    /** Materias activas, acotadas al tipo bloqueado en ese slot (si lo hay). */
    @Transactional(readOnly = true)
    public List<Materia> listarMateriasDisponibles(Long direccionId, Long nivelId, Long periodoId, String dia,
            Integer numeroLeccion, Long materiaSeleccionadaId) {
        List<Materia> materias = listarMateriasActivas(direccionId);
        Optional<TipoMateria> tipoBloqueado = seccionBloqueoService.tipoBloqueado(
                direccionId, periodoId, nivelId, dia, numeroLeccion);
        if (tipoBloqueado.isEmpty()) {
            return materias;
        }
        Long tipoId = tipoBloqueado.get().getId();
        return materias.stream()
                .filter(m -> Objects.equals(m.getId(), materiaSeleccionadaId)
                        || (m.getTipoMateria() != null && m.getTipoMateria().getId().equals(tipoId)))
                .toList();
    }

    public void alternarBloqueoSeccion(Long direccionId, Long periodoId, Long nivelId, String dia,
            Integer numeroLeccion, Long tipoMateriaId) {
        seccionBloqueoService.alternar(direccionId, periodoId, nivelId, dia, numeroLeccion, tipoMateriaId);
    }

    public static String clave(String dia, Integer numeroLeccion) {
        return numeroLeccion + "-" + dia;
    }

    private void validarDocenteDeMateria(Long direccionId, Long materiaId, Long docenteId, Long leccionId) {
        if (docenteMateriaService.estaAsignado(direccionId, docenteId, materiaId)) {
            return;
        }
        if (leccionId != null) {
            HorarioLeccion actual = obtenerLeccionPorId(direccionId, leccionId);
            if (actual.getDocente().getId().equals(docenteId)
                    && actual.getMateria().getId().equals(materiaId)) {
                return;
            }
        }
        throw new IllegalArgumentException("El profesor no está asociado a esa materia");
    }

    private Direccion obtenerDireccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }

    private String normalizarColor(String color) {
        if (color == null || !color.matches("^#[0-9a-fA-F]{6}$")) {
            return "#2d5a87";
        }
        return color.toLowerCase();
    }
}
