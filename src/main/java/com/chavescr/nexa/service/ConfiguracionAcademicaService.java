package com.chavescr.nexa.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Aula;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoAula;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AulaRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.TipoAulaRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class ConfiguracionAcademicaService {

    public static final List<String> DIAS = List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES");
    public static final List<Integer> LECCIONES = List.of(1, 2, 3, 4, 5, 6, 7, 8);

    private final InstitucionRepository institucionRepository;
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

    public ConfiguracionAcademicaService(InstitucionRepository institucionRepository,
            PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository,
            MateriaRepository materiaRepository,
            TipoMateriaRepository tipoMateriaRepository,
            TipoAulaRepository tipoAulaRepository,
            HorarioLeccionRepository horarioRepository,
            UsuarioRepository usuarioRepository,
            AulaRepository aulaRepository,
            DocenteMateriaService docenteMateriaService,
            DocenteGuiaService docenteGuiaService) {
        this.institucionRepository = institucionRepository;
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
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodos(Long institucionId) {
        return periodoRepository.findByInstitucionIdOrderByFechaInicioDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodosActivos(Long institucionId) {
        return periodoRepository.findByInstitucionIdAndActivoTrueOrderByFechaInicioDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerPeriodo(Long institucionId, Long id) {
        return periodoRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
    }

    public PeriodoAcademico guardarPeriodo(Long institucionId, PeriodoAcademico datos) {
        if (datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        PeriodoAcademico periodo = datos.getId() == null
                ? new PeriodoAcademico()
                : obtenerPeriodo(institucionId, datos.getId());
        periodo.setInstitucion(obtenerInstitucion(institucionId));
        periodo.setCodigo(datos.getCodigo().trim().toUpperCase());
        periodo.setDescripcion(datos.getDescripcion().trim());
        periodo.setFechaInicio(datos.getFechaInicio());
        periodo.setFechaFin(datos.getFechaFin());
        periodo.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        return periodoRepository.save(periodo);
    }

    public void eliminarPeriodo(Long institucionId, Long id) {
        PeriodoAcademico periodo = obtenerPeriodo(institucionId, id);
        horarioRepository.deleteByInstitucionIdAndPeriodoId(institucionId, id);
        periodoRepository.delete(periodo);
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarNiveles(Long institucionId) {
        return nivelRepository.findByInstitucionIdOrderByGradoAscSeccionAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<NivelAcademico> listarNivelesActivos(Long institucionId) {
        return nivelRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public NivelAcademico obtenerNivel(Long institucionId, Long id) {
        return nivelRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado"));
    }

    public NivelAcademico guardarNivel(Long institucionId, NivelAcademico datos) {
        NivelAcademico nivel = datos.getId() == null
                ? new NivelAcademico()
                : obtenerNivel(institucionId, datos.getId());
        nivel.setInstitucion(obtenerInstitucion(institucionId));
        nivel.setGrado(datos.getGrado());
        nivel.setSeccion(datos.getSeccion().trim().toUpperCase());
        nivel.setActivo(Boolean.TRUE.equals(datos.getActivo()));
        return nivelRepository.save(nivel);
    }

    public void eliminarNivel(Long institucionId, Long id) {
        NivelAcademico nivel = obtenerNivel(institucionId, id);
        horarioRepository.deleteByInstitucionIdAndNivelId(institucionId, id);
        docenteGuiaService.eliminarPorNivel(institucionId, id);
        nivelRepository.delete(nivel);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMaterias(Long institucionId) {
        return materiaRepository.findByInstitucionIdOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Materia> listarMateriasActivas(Long institucionId) {
        return materiaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<TipoMateria> listarTiposMateriaActivos() {
        return tipoMateriaRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public Materia obtenerMateria(Long institucionId, Long id) {
        return materiaRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
    }

    public Materia guardarMateria(Long institucionId, Materia datos) {
        Materia materia = datos.getId() == null ? new Materia() : obtenerMateria(institucionId, datos.getId());
        TipoMateria tipoMateria = resolverTipoMateria(datos);
        materia.setInstitucion(obtenerInstitucion(institucionId));
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

    public void eliminarMateria(Long institucionId, Long id) {
        Materia materia = obtenerMateria(institucionId, id);
        horarioRepository.deleteByInstitucionIdAndMateriaId(institucionId, id);
        docenteMateriaService.eliminarPorMateria(institucionId, id);
        materiaRepository.delete(materia);
    }

    @Transactional(readOnly = true)
    public List<Aula> listarAulas(Long institucionId) {
        return aulaRepository.findByInstitucionIdOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<Aula> listarAulasActivas(Long institucionId) {
        return aulaRepository.findByInstitucionIdAndActivoTrueOrderByNombreAsc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<TipoAula> listarTiposAulaActivos() {
        return tipoAulaRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public Aula obtenerAula(Long institucionId, Long id) {
        return aulaRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Aula no encontrada"));
    }

    public Aula guardarAula(Long institucionId, Aula datos) {
        if (datos.getCapacidad() == null || datos.getCapacidad() <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a cero");
        }
        Aula aula = datos.getId() == null ? new Aula() : obtenerAula(institucionId, datos.getId());
        TipoAula tipoAula = resolverTipoAula(datos);
        aula.setInstitucion(obtenerInstitucion(institucionId));
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

    public void eliminarAula(Long institucionId, Long id) {
        Aula aula = obtenerAula(institucionId, id);
        horarioRepository.deleteByInstitucionIdAndAulaId(institucionId, id);
        aulaRepository.delete(aula);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarDocentes(Long institucionId) {
        return usuarioRepository.findActivosByInstitucionIdAndRol(institucionId, "ROLE_DOCENTE");
    }

    /**
     * Profesores activos asociados a la materia. Si se edita una lección cuyo
     * docente ya no está asignado, se incluye para no romper el formulario.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarDocentesPorMateria(Long institucionId, Long materiaId, Long docenteSeleccionadoId) {
        if (materiaId == null) {
            return List.of();
        }
        List<Usuario> docentes = new ArrayList<>(
                docenteMateriaService.listarDocentesPorMateria(institucionId, materiaId));
        if (docenteSeleccionadoId != null
                && docentes.stream().noneMatch(docente -> docente.getId().equals(docenteSeleccionadoId))) {
            usuarioRepository.findActivoByIdAndInstitucionId(docenteSeleccionadoId, institucionId)
                    .ifPresent(docente -> docentes.add(0, docente));
        }
        return docentes;
    }

    @Transactional(readOnly = true)
    public Map<String, List<HorarioLeccion>> obtenerHorario(Long institucionId, Long periodoId, Long nivelId) {
        Map<String, List<HorarioLeccion>> horario = new LinkedHashMap<>();
        if (periodoId == null || nivelId == null) return horario;
        horarioRepository.findByInstitucionIdAndPeriodoIdAndNivelIdOrderByNumeroLeccionAsc(
                institucionId, periodoId, nivelId)
                .forEach(l -> horario
                        .computeIfAbsent(clave(l.getDia(), l.getNumeroLeccion()), k -> new ArrayList<>())
                        .add(l));
        return horario;
    }

    /** Igual que {@link #obtenerHorario}, pero acotado al horario de un docente en particular (para Disponibilidad). */
    @Transactional(readOnly = true)
    public Map<String, List<HorarioLeccion>> obtenerHorarioPorDocente(Long institucionId, Long periodoId, Long docenteId) {
        Map<String, List<HorarioLeccion>> horario = new LinkedHashMap<>();
        if (periodoId == null || docenteId == null) return horario;
        horarioRepository.findByInstitucionIdAndPeriodoIdAndDocenteIdOrderByDiaAscNumeroLeccionAsc(
                institucionId, periodoId, docenteId)
                .forEach(l -> horario
                        .computeIfAbsent(clave(l.getDia(), l.getNumeroLeccion()), k -> new ArrayList<>())
                        .add(l));
        return horario;
    }

    @Transactional(readOnly = true)
    public HorarioLeccion obtenerLeccionPorId(Long institucionId, Long id) {
        return horarioRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Lección no encontrada"));
    }

    public HorarioLeccion guardarLeccion(Long institucionId, Long id, Long periodoId, Long nivelId,
            Long materiaId, Long docenteId, Long aulaId, String dia, Integer numeroLeccion,
            LocalTime horaInicio, LocalTime horaFin) {
        if (!DIAS.contains(dia) || !LECCIONES.contains(numeroLeccion)) {
            throw new IllegalArgumentException("Día o número de lección inválido");
        }
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora final debe ser posterior a la hora inicial");
        }
        validarDocenteDeMateria(institucionId, materiaId, docenteId, id);

        boolean docenteOcupado = horarioRepository
                .findByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
                        institucionId, periodoId, docenteId, dia, numeroLeccion)
                .stream()
                .anyMatch(e -> !e.getId().equals(id));
        if (docenteOcupado) {
            throw new IllegalArgumentException("El docente ya tiene otra lección asignada en este horario");
        }

        HorarioLeccion leccion;
        if (id != null) {
            leccion = obtenerLeccionPorId(institucionId, id);
        } else {
            List<HorarioLeccion> existentes = horarioRepository
                    .findByInstitucionIdAndPeriodoIdAndNivelIdAndDiaAndNumeroLeccionOrderByIdAsc(
                            institucionId, periodoId, nivelId, dia, numeroLeccion);
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

        leccion.setInstitucion(obtenerInstitucion(institucionId));
        leccion.setPeriodo(obtenerPeriodo(institucionId, periodoId));
        leccion.setNivel(obtenerNivel(institucionId, nivelId));
        leccion.setMateria(obtenerMateria(institucionId, materiaId));
        leccion.setDocente(usuarioRepository.findActivoByIdAndInstitucionId(docenteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Docente no válido para la institución")));
        leccion.setAula(obtenerAula(institucionId, aulaId));
        leccion.setDia(dia);
        leccion.setNumeroLeccion(numeroLeccion);
        leccion.setHoraInicio(horaInicio);
        leccion.setHoraFin(horaFin);
        return horarioRepository.save(leccion);
    }

    public void eliminarLeccion(Long institucionId, Long id) {
        horarioRepository.delete(horarioRepository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Lección no encontrada")));
    }

    public String clave(String dia, Integer numeroLeccion) {
        return numeroLeccion + "-" + dia;
    }

    private void validarDocenteDeMateria(Long institucionId, Long materiaId, Long docenteId, Long leccionId) {
        if (docenteMateriaService.estaAsignado(institucionId, docenteId, materiaId)) {
            return;
        }
        if (leccionId != null) {
            HorarioLeccion actual = obtenerLeccionPorId(institucionId, leccionId);
            if (actual.getDocente().getId().equals(docenteId)
                    && actual.getMateria().getId().equals(materiaId)) {
                return;
            }
        }
        throw new IllegalArgumentException("El profesor no está asociado a esa materia");
    }

    private Institucion obtenerInstitucion(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }

    private String normalizarColor(String color) {
        if (color == null || !color.matches("^#[0-9a-fA-F]{6}$")) {
            return "#2d5a87";
        }
        return color.toLowerCase();
    }
}
