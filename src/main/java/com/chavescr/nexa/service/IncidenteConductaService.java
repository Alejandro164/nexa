package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaIncidenteConducta;
import com.chavescr.nexa.dto.PanelIncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.EstadoIncidente;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class IncidenteConductaService {

    private final IncidenteConductaRepository incidenteRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitucionRepository institucionRepository;
    private final DocenteGuiaService docenteGuiaService;
    private final AlcanceDocenteService alcanceDocenteService;

    public IncidenteConductaService(IncidenteConductaRepository incidenteRepository,
            PeriodoAcademicoRepository periodoRepository, NivelAcademicoRepository nivelRepository,
            UsuarioRepository usuarioRepository, InstitucionRepository institucionRepository,
            DocenteGuiaService docenteGuiaService, AlcanceDocenteService alcanceDocenteService) {
        this.incidenteRepository = incidenteRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.usuarioRepository = usuarioRepository;
        this.institucionRepository = institucionRepository;
        this.docenteGuiaService = docenteGuiaService;
        this.alcanceDocenteService = alcanceDocenteService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PanelIncidenteConducta cargarPanel(TipoIncidente tipo, Long institucionId, Long periodoId, Integer grado,
            Long nivelId, Long docenteId) {
        List<PeriodoAcademico> periodos = periodoRepository.findByInstitucionIdOrderByFechaInicioDesc(institucionId);
        List<NivelAcademico> nivelesVisibles = nivelesVisibles(institucionId, docenteId);
        List<Integer> grados = nivelesVisibles.stream()
                .map(NivelAcademico::getGrado)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        PeriodoAcademico periodo = resolverPeriodo(periodos, periodoId);
        if (periodo == null) {
            return new PanelIncidenteConducta(periodos, grados, List.of(), List.of(), null, grado, nivelId,
                    "Configure un período académico para registrar la conducta.");
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

        List<IncidenteConducta> incidentes = listar(tipo, institucionId, periodo.getId(), grado, nivelId,
                docenteId != null, nivelesVisibles);
        List<FilaIncidenteConducta> filas = incidentes.stream().map(this::construirFila).toList();

        return new PanelIncidenteConducta(periodos, grados, secciones, filas, periodo.getId(), grado, nivelId, null);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> listarEstudiantes(Long institucionId, Integer grado, Long nivelId, Long docenteId) {
        List<NivelAcademico> nivelesVisibles = nivelesVisibles(institucionId, docenteId);
        Integer gradoFiltro = grado;
        Long seccionFiltro = nivelId;
        if (gradoFiltro != null) {
            boolean gradoValido = nivelesVisibles.stream().map(NivelAcademico::getGrado).anyMatch(gradoFiltro::equals);
            if (!gradoValido) {
                gradoFiltro = null;
                seccionFiltro = null;
            }
        }
        if (seccionFiltro != null) {
            Long seccionId = seccionFiltro;
            Integer gradoActual = gradoFiltro;
            boolean seccionValida = nivelesVisibles.stream().anyMatch(n -> n.getId().equals(seccionId)
                    && (gradoActual == null || gradoActual.equals(n.getGrado())));
            if (!seccionValida) {
                seccionFiltro = null;
            }
        }
        return listarEstudiantes(institucionId, gradoFiltro, seccionFiltro, nivelesVisibles, docenteId != null);
    }

    @Transactional(rollbackFor = Exception.class)
    public String crear(TipoIncidente tipo, Long institucionId, Long periodoId, Long estudianteId, LocalDate fecha,
            String motivo, String descripcion, Integer puntos, Long registradoPorId, Long docenteId) {
        if (periodoId == null) {
            throw new IllegalArgumentException("Seleccione un período académico.");
        }
        if (estudianteId == null) {
            throw new IllegalArgumentException("Seleccione un estudiante.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("Indique la fecha.");
        }
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) {
            throw new IllegalArgumentException(tipo == TipoIncidente.BOLETA
                    ? "Indique la falta."
                    : "Indique el motivo de la llamada de atención.");
        }
        if (motivoLimpio.length() > 500) {
            throw new IllegalArgumentException("El texto no puede superar 500 caracteres.");
        }
        String descripcionLimpia = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (descripcionLimpia != null && descripcionLimpia.length() > 2000) {
            throw new IllegalArgumentException("La descripción no puede superar 2000 caracteres.");
        }
        int puntosDescontados = resolverPuntos(tipo, puntos);

        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        exigirAlcance(institucionId, docenteId, estudiante);
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        IncidenteConducta incidente = new IncidenteConducta();
        incidente.setInstitucion(institucion);
        incidente.setPeriodo(periodo);
        incidente.setEstudiante(estudiante);
        incidente.setTipo(tipo);
        incidente.setFecha(fecha);
        incidente.setMotivo(motivoLimpio);
        incidente.setDescripcion(descripcionLimpia);
        incidente.setEstado(EstadoIncidente.PENDIENTE);
        incidente.setPuntosDescontados(puntosDescontados);
        if (registradoPorId != null) {
            usuarioRepository.findActivoByIdAndInstitucionId(registradoPorId, institucionId)
                    .ifPresent(incidente::setRegistradoPor);
        }
        incidenteRepository.save(incidente);
        if (tipo.afectaNota()) {
            return etiqueta(tipo) + " registrada para " + estudiante.getNombre() + ". Se descontaron "
                    + puntosDescontados + (puntosDescontados == 1 ? " punto" : " puntos") + " de conducta.";
        }
        return etiqueta(tipo) + " registrada para " + estudiante.getNombre() + ".";
    }

    @Transactional(rollbackFor = Exception.class)
    public String resolver(TipoIncidente tipo, Long institucionId, Long incidenteId, Long docenteId) {
        if (incidenteId == null) {
            throw new IllegalArgumentException(etiqueta(tipo) + " no encontrada");
        }
        IncidenteConducta incidente = incidenteRepository.findByIdAndInstitucionId(incidenteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException(etiqueta(tipo) + " no encontrada"));
        if (incidente.getTipo() != tipo) {
            throw new IllegalArgumentException(etiqueta(tipo) + " no encontrada");
        }
        exigirAlcance(institucionId, docenteId, incidente.getEstudiante());
        if (incidente.getEstado() == EstadoIncidente.RESUELTO) {
            return "Este registro ya estaba resuelto.";
        }
        incidente.setEstado(EstadoIncidente.RESUELTO);
        incidenteRepository.save(incidente);
        return etiqueta(tipo) + " de " + incidente.getEstudiante().getNombre() + " marcada como resuelta.";
    }

    private List<IncidenteConducta> listar(TipoIncidente tipo, Long institucionId, Long periodoId, Integer grado,
            Long nivelId, boolean limitarANiveles, List<NivelAcademico> nivelesVisibles) {
        if (!limitarANiveles) {
            return incidenteRepository.findDelPeriodo(institucionId, periodoId, tipo, grado, nivelId);
        }
        List<Long> nivelIds = nivelesVisibles.stream().map(NivelAcademico::getId).toList();
        if (nivelIds.isEmpty()) {
            return List.of();
        }
        return incidenteRepository.findDelPeriodoEnNiveles(institucionId, periodoId, tipo, nivelIds, grado, nivelId);
    }

    private List<Usuario> listarEstudiantes(Long institucionId, Integer grado, Long nivelId,
            List<NivelAcademico> nivelesVisibles, boolean limitarANiveles) {
        if (!limitarANiveles) {
            return usuarioRepository.findEstudiantesActivosConNivel(institucionId, grado, nivelId);
        }
        List<Long> nivelIds = nivelesVisibles.stream().map(NivelAcademico::getId).toList();
        if (nivelIds.isEmpty()) {
            return List.of();
        }
        return usuarioRepository.findEstudiantesActivosConNivelEn(institucionId, nivelIds, grado, nivelId);
    }

    private FilaIncidenteConducta construirFila(IncidenteConducta incidente) {
        Usuario estudiante = incidente.getEstudiante();
        String seccion = "Sin sección";
        if (estudiante.getNivelAcademico() != null) {
            NivelAcademico nivel = estudiante.getNivelAcademico();
            seccion = nivel.getGrado() + "-" + nivel.getSeccion();
        }
        String docenteNombre = incidente.getRegistradoPor() != null
                ? incidente.getRegistradoPor().getNombre()
                : "—";
        EstadoIncidente estado = incidente.getEstado();
        int puntos = incidente.getPuntosDescontados() != null ? incidente.getPuntosDescontados() : 0;
        return new FilaIncidenteConducta(incidente.getId(), numeroRegistro(incidente), estudiante, seccion,
                incidente.getFecha(), incidente.getMotivo(), incidente.getDescripcion(), puntos, docenteNombre,
                estadoCss(estado), estadoLabel(estado), puedeResolver(estado),
                NotaConductaService.iniciales(estudiante.getNombre()),
                NotaConductaService.colorAvatar(estudiante.getId()));
    }

    private static int resolverPuntos(TipoIncidente tipo, Integer puntos) {
        if (!tipo.afectaNota()) {
            return 0;
        }
        if (puntos == null) {
            throw new IllegalArgumentException("Indique cuántos puntos se descuentan de la nota de conducta.");
        }
        if (puntos < 1) {
            throw new IllegalArgumentException("Los puntos a descontar deben ser al menos 1.");
        }
        if (puntos > 100) {
            throw new IllegalArgumentException("Los puntos a descontar no pueden superar 100.");
        }
        return puntos;
    }

    private static String etiqueta(TipoIncidente tipo) {
        return switch (tipo) {
            case BOLETA -> "Boleta";
            case LLAMADA_ATENCION -> "Llamada de atención";
        };
    }

    private static String numeroRegistro(IncidenteConducta incidente) {
        int anio = incidente.getFecha() != null ? incidente.getFecha().getYear() : 0;
        String prefijo = incidente.getTipo() == TipoIncidente.BOLETA ? "B" : "L";
        return String.format("%s-%d-%03d", prefijo, anio, incidente.getId());
    }

    private void exigirAlcance(Long institucionId, Long docenteId, Usuario estudiante) {
        if (docenteId == null) {
            return;
        }
        List<NivelAcademico> niveles = nivelesVisibles(institucionId, docenteId);
        Long nivelEstudiante = estudiante.getNivelAcademico() != null ? estudiante.getNivelAcademico().getId() : null;
        boolean visible = nivelEstudiante != null
                && niveles.stream().anyMatch(n -> n.getId().equals(nivelEstudiante));
        if (!visible) {
            throw new IllegalArgumentException("No tiene permiso para gestionar a este estudiante.");
        }
    }

    private List<NivelAcademico> nivelesVisibles(Long institucionId, Long docenteId) {
        if (docenteId == null) {
            return nivelRepository.findByInstitucionIdAndActivoTrueOrderByGradoAscSeccionAsc(institucionId);
        }
        List<NivelAcademico> guias = docenteGuiaService.listarSecciones(institucionId, docenteId);
        if (!guias.isEmpty()) {
            return guias;
        }
        return alcanceDocenteService.nivelesVisibles(institucionId, docenteId);
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

    private static String estadoCss(EstadoIncidente estado) {
        return switch (estado) {
            case PENDIENTE -> "pendiente";
            case EN_PROCESO -> "enproceso";
            case RESUELTO -> "resuelto";
            case APELADO -> "apelado";
        };
    }

    private static String estadoLabel(EstadoIncidente estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En Proceso";
            case RESUELTO -> "Resuelto";
            case APELADO -> "Apelado";
        };
    }

    private static boolean puedeResolver(EstadoIncidente estado) {
        return estado == EstadoIncidente.PENDIENTE || estado == EstadoIncidente.EN_PROCESO;
    }
}
