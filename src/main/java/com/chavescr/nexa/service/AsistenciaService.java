package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.dto.DetalleAsistencia;
import com.chavescr.nexa.dto.DetalleAsistencia.Cuadro;
import com.chavescr.nexa.dto.DetalleAsistencia.Fila;
import com.chavescr.nexa.dto.FilaAsistencia;
import com.chavescr.nexa.entity.AsistenciaEstudiante;
import com.chavescr.nexa.entity.AsistenciaEstudiante.EstadoAsistencia;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoRebaja;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AsistenciaEstudianteRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class AsistenciaService {

    private static final DateTimeFormatter FECHA_PERIODO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final long DOCUMENTO_MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> DOCUMENTO_EXTENSIONES = Set.of("pdf", "jpg", "jpeg", "png", "webp");

    private final AsistenciaEstudianteRepository asistenciaRepository;
    private final NivelAcademicoRepository nivelAcademicoRepository;
    private final MateriaRepository materiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final HorarioLeccionRepository horarioLeccionRepository;
    private final AlmacenamientoService almacenamientoService;
    private final AlcanceDocenteService alcanceDocenteService;
    private final RebajaConductaService rebajaConductaService;
    private final SubgrupoService subgrupoService;

    public AsistenciaService(AsistenciaEstudianteRepository asistenciaRepository,
            NivelAcademicoRepository nivelAcademicoRepository,
            MateriaRepository materiaRepository,
            UsuarioRepository usuarioRepository,
            PeriodoAcademicoRepository periodoRepository,
            HorarioLeccionRepository horarioLeccionRepository,
            AlmacenamientoService almacenamientoService,
            AlcanceDocenteService alcanceDocenteService,
            RebajaConductaService rebajaConductaService,
            SubgrupoService subgrupoService) {
        this.asistenciaRepository = asistenciaRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.materiaRepository = materiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.periodoRepository = periodoRepository;
        this.horarioLeccionRepository = horarioLeccionRepository;
        this.almacenamientoService = almacenamientoService;
        this.alcanceDocenteService = alcanceDocenteService;
        this.rebajaConductaService = rebajaConductaService;
        this.subgrupoService = subgrupoService;
    }

    @Transactional(readOnly = true)
    public PeriodoAcademico obtenerUltimoPeriodoActivo(Long direccionId) {
        return periodoRepository.findByDireccionIdAndActivoTrueOrderByFechaInicioDesc(direccionId).stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String validarPeriodoParaAsistencia(Long direccionId, LocalDate fecha, PeriodoAcademico periodo) {
        if (periodo == null) {
            return "No hay un período académico activo. Actívalo en Configuración académica antes de pasar lista.";
        }
        if (!periodo.contiene(fecha)) {
            return "La fecha seleccionada no pertenece al período activo " + periodo.getCodigo()
                    + " (" + periodo.getFechaInicio().format(FECHA_PERIODO)
                    + " – " + periodo.getFechaFin().format(FECHA_PERIODO) + ").";
        }
        if (!nivelAcademicoRepository.existsByDireccionIdAndActivoTrue(direccionId)) {
            return "El período activo no tiene secciones registradas. Créalas en Configuración académica.";
        }
        if (!horarioLeccionRepository.existsByDireccionIdAndPeriodoId(direccionId, periodo.getId())) {
            return "El período activo no tiene lecciones registradas en el horario. Configúralas en Configuración académica.";
        }
        return null;
    }

    /**
     * Tardías y ausencias del estudiante en los períodos del año del período activo.
     * Los puntos salen de la regla de la institución. Un docente solo ve estudiantes de sus secciones.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public DetalleAsistencia detalleEstudiante(Long direccionId, Long estudianteId, Long docenteId) {
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        exigirEstudianteVisible(direccionId, docenteId, estudiante);
        List<PeriodoAcademico> periodos = periodosDelAnioActivo(direccionId);
        if (periodos.isEmpty()) {
            throw new IllegalArgumentException("No hay períodos en el año lectivo");
        }
        LocalDate desde = periodos.get(0).getFechaInicio();
        LocalDate hasta = periodos.get(0).getFechaFin();
        for (PeriodoAcademico periodo : periodos) {
            if (periodo.getFechaFin().isAfter(hasta)) {
                hasta = periodo.getFechaFin();
            }
        }
        List<Object[]> filas = asistenciaRepository.findAusenciasDeEstudiante(direccionId, estudianteId, desde, hasta,
                EstadoAsistencia.PRESENTE);
        CalculoRebaja calculo = rebajaConductaService.calculo(direccionId);
        Long actualId = periodoPorDefecto(periodos, LocalDate.now()).getId();
        List<DetalleAsistencia.Periodo> bloques = new ArrayList<>();
        for (PeriodoAcademico periodo : periodos) {
            bloques.add(bloque(periodo, filas, calculo, periodo.getId().equals(actualId)));
        }
        return new DetalleAsistencia(texto(estudiante.getNombre()), iniciales(estudiante.getNombre()),
                meta(estudiante), bloques);
    }

    private List<Usuario> estudiantesDeLaLeccion(Long direccionId, Long nivelId, LocalDate fecha,
            Integer numeroLeccion, Long materiaId) {
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosByNivelId(nivelId);
        PeriodoAcademico periodo = obtenerUltimoPeriodoActivo(direccionId);
        if (periodo == null) {
            return estudiantes;
        }
        return subgrupoService.paraAsistencia(direccionId, periodo.getId(), nivelId, fecha, numeroLeccion, materiaId,
                estudiantes);
    }

    @Transactional(readOnly = true)
    public List<FilaAsistencia> listarFilas(Long direccionId, Long nivelId, LocalDate fecha, Long materiaId,
            Integer numeroLeccion) {
        List<Usuario> estudiantes = estudiantesDeLaLeccion(direccionId, nivelId, fecha, numeroLeccion, materiaId);
        Map<Long, AsistenciaEstudiante> registros = asistenciaRepository
                .findByDireccionIdAndNivelAcademicoIdAndFechaAndMateriaIdAndNumeroLeccion(
                        direccionId, nivelId, fecha, materiaId, numeroLeccion)
                .stream()
                .collect(Collectors.toMap(a -> a.getEstudiante().getId(), a -> a));
        return estudiantes.stream()
                .map(e -> construirFila(e, registros.get(e.getId())))
                .toList();
    }

    public FilaAsistencia registrarEstado(Long direccionId, Long estudianteId, Long nivelId, Long materiaId,
            Integer numeroLeccion, LocalDate fecha, String estado, String observaciones, Long registradoPorId) {
        exigirPeriodoListoParaAsistencia(direccionId, fecha);
        NivelAcademico nivel = nivelAcademicoRepository.findByIdAndDireccionId(nivelId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
        Materia materia = materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        Usuario estudiante = usuarioRepository.findActivoByIdAndDireccionId(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        AsistenciaEstudiante registro = asistenciaRepository
                .findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                        direccionId, estudianteId, fecha, materiaId, numeroLeccion)
                .orElseGet(AsistenciaEstudiante::new);
        registro.setDireccion(nivel.getDireccion());
        registro.setNivelAcademico(nivel);
        registro.setEstudiante(estudiante);
        registro.setMateria(materia);
        registro.setNumeroLeccion(numeroLeccion);
        registro.setFecha(fecha);

        EstadoAsistencia estadoFinal = registro.getEstado();
        if (estado != null && !estado.isBlank()) {
            estadoFinal = EstadoAsistencia.valueOf(estado);
        }
        String observacionesFinal = registro.getObservaciones();
        if (observaciones != null) {
            observacionesFinal = observaciones.isBlank() ? null : observaciones.trim();
        }
        registro.setEstado(estadoFinal);
        registro.setObservaciones(observacionesFinal);

        if (registradoPorId != null) {
            usuarioRepository.findById(registradoPorId).ifPresent(registro::setRegistradoPor);
        }
        registro.setActualizadoEn(LocalDateTime.now());
        asistenciaRepository.save(registro);

        return construirFila(estudiante, registro);
    }

    /**
     * Copia el estado de asistencia de cada estudiante registrado en {@code leccionOrigen} hacia
     * {@code leccionDestino}, para la misma sección/materia/fecha. Estudiantes sin registro en el
     * origen se dejan intactos. Devuelve cuántos registros se copiaron.
     */
    public int copiarDeLeccionAnterior(Long direccionId, Long nivelId, Long materiaId, LocalDate fecha,
            Integer leccionOrigen, Integer leccionDestino, Long registradoPorId) {
        exigirPeriodoListoParaAsistencia(direccionId, fecha);
        List<Usuario> estudiantes = estudiantesDeLaLeccion(direccionId, nivelId, fecha, leccionDestino, materiaId);
        int copiados = 0;
        for (Usuario estudiante : estudiantes) {
            AsistenciaEstudiante origen = asistenciaRepository
                    .findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                            direccionId, estudiante.getId(), fecha, materiaId, leccionOrigen)
                    .orElse(null);
            if (origen == null || origen.getEstado() == null) {
                continue;
            }
            registrarEstado(direccionId, estudiante.getId(), nivelId, materiaId, leccionDestino, fecha,
                    origen.getEstado().name(), origen.getObservaciones(), registradoPorId);
            if (origen.getDocumentoRuta() != null) {
                asistenciaRepository
                        .findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                                direccionId, estudiante.getId(), fecha, materiaId, leccionDestino)
                        .ifPresent(destino -> aplicarDocumento(destino, origen.getDocumentoRuta(),
                                origen.getDocumentoNombre()));
            }
            copiados++;
        }
        return copiados;
    }

    /**
     * Marca como presentes a los estudiantes de la lección que aún no tienen estado.
     * Quienes ya tienen asistencia no se modifican. Devuelve cuántos se marcaron.
     */
    public int marcarPendientesPresentes(Long direccionId, Long nivelId, Long materiaId, LocalDate fecha,
            Integer numeroLeccion, Long registradoPorId) {
        exigirPeriodoListoParaAsistencia(direccionId, fecha);
        int marcados = 0;
        for (FilaAsistencia fila : listarFilas(direccionId, nivelId, fecha, materiaId, numeroLeccion)) {
            if (fila.getEstado() != null) {
                continue;
            }
            registrarEstado(direccionId, fila.getEstudiante().getId(), nivelId, materiaId, numeroLeccion, fecha,
                    EstadoAsistencia.PRESENTE.name(), null, registradoPorId);
            marcados++;
        }
        return marcados;
    }

    private void exigirPeriodoListoParaAsistencia(Long direccionId, LocalDate fecha) {
        String aviso = validarPeriodoParaAsistencia(direccionId, fecha, obtenerUltimoPeriodoActivo(direccionId));
        if (aviso != null) {
            throw new IllegalArgumentException(aviso);
        }
    }

    public void guardarDocumento(Long direccionId, Long estudianteId, Long nivelId, Long materiaId,
            Integer numeroLeccion, LocalDate fecha, MultipartFile archivo) {
        exigirPeriodoListoParaAsistencia(direccionId, fecha);
        validarDocumento(archivo);
        AsistenciaEstudiante registro = asistenciaRepository
                .findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                        direccionId, estudianteId, fecha, materiaId, numeroLeccion)
                .orElseThrow(() -> new IllegalArgumentException("Primero marca la justificación"));
        if (registro.getEstado() == null || !registro.getEstado().esJustificada()) {
            throw new IllegalArgumentException("El documento solo se adjunta cuando la ausencia o la tardía está justificada");
        }
        if (!registro.getNivelAcademico().getId().equals(nivelId) || !registro.getMateria().getId().equals(materiaId)) {
            throw new IllegalArgumentException("La justificación no corresponde a esta lección");
        }
        String ruta;
        try {
            ruta = almacenamientoService.guardar(archivo, registro.getDireccion(), "asistencia");
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar el documento de justificación");
        }
        aplicarDocumento(registro, ruta, nombreVisible(archivo.getOriginalFilename()));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public DocumentoJustificacion obtenerDocumento(Long direccionId, Long estudianteId, Long materiaId,
            Integer numeroLeccion, LocalDate fecha) {
        AsistenciaEstudiante registro = asistenciaRepository
                .findByDireccionIdAndEstudianteIdAndFechaAndMateriaIdAndNumeroLeccion(
                        direccionId, estudianteId, fecha, materiaId, numeroLeccion)
                .filter(r -> r.getDocumentoRuta() != null)
                .orElseThrow(() -> new IllegalArgumentException("No hay documento de justificación"));
        Path ruta = almacenamientoService.resolver(registro.getDocumentoRuta());
        String nombre = registro.getDocumentoNombre() != null ? registro.getDocumentoNombre() : "justificacion";
        return new DocumentoJustificacion(ruta, nombre);
    }

    private void aplicarDocumento(AsistenciaEstudiante registro, String ruta, String nombre) {
        String anterior = registro.getDocumentoRuta();
        registro.setDocumentoRuta(ruta);
        registro.setDocumentoNombre(nombre);
        registro.setActualizadoEn(LocalDateTime.now());
        asistenciaRepository.save(registro);
        if (anterior != null && !anterior.equals(ruta) && asistenciaRepository.countByDocumentoRuta(anterior) == 0) {
            almacenamientoService.eliminar(anterior);
        }
    }

    private void validarDocumento(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un documento");
        }
        if (archivo.getSize() > DOCUMENTO_MAX_BYTES) {
            throw new IllegalArgumentException("El documento no puede superar 10 MB");
        }
        String nombre = archivo.getOriginalFilename();
        int punto = nombre == null ? -1 : nombre.lastIndexOf('.');
        String extension = punto >= 0 ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        if (!DOCUMENTO_EXTENSIONES.contains(extension)) {
            throw new IllegalArgumentException("El documento debe ser PDF o imagen (JPG, PNG o WEBP)");
        }
    }

    private String nombreVisible(String original) {
        if (original == null || original.isBlank()) {
            return "justificacion";
        }
        String nombre = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        if (nombre.length() > 255) {
            nombre = nombre.substring(nombre.length() - 255);
        }
        return nombre.isEmpty() ? "justificacion" : nombre;
    }

    private void exigirEstudianteVisible(Long direccionId, Long docenteId, Usuario estudiante) {
        if (docenteId == null) {
            return;
        }
        Long nivelId = estudiante.getNivelAcademico() == null ? null : estudiante.getNivelAcademico().getId();
        boolean visible = nivelId != null && alcanceDocenteService.nivelesVisibles(direccionId, docenteId).stream()
                .anyMatch(nivel -> nivelId.equals(nivel.getId()));
        if (!visible) {
            throw new IllegalArgumentException("No puede consultar la asistencia de este estudiante");
        }
    }

    private List<PeriodoAcademico> periodosDelAnioActivo(Long direccionId) {
        PeriodoAcademico activo = obtenerUltimoPeriodoActivo(direccionId);
        if (activo == null || activo.getFechaInicio() == null) {
            return List.of();
        }
        int anio = activo.getFechaInicio().getYear();
        List<PeriodoAcademico> periodos = periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId);
        if (periodos == null) {
            return List.of();
        }
        return periodos.stream()
                .filter(periodo -> periodo.getFechaInicio() != null && periodo.getFechaFin() != null
                        && periodo.getFechaInicio().getYear() == anio)
                .sorted(Comparator.comparing(PeriodoAcademico::getFechaInicio))
                .toList();
    }

    /** El período que contiene hoy, o el de fechas más cercanas si hoy queda fuera de todos. */
    static PeriodoAcademico periodoPorDefecto(List<PeriodoAcademico> periodos, LocalDate hoy) {
        PeriodoAcademico contiene = null;
        for (PeriodoAcademico periodo : periodos) {
            if (!hoy.isBefore(periodo.getFechaInicio()) && !hoy.isAfter(periodo.getFechaFin())) {
                contiene = periodo;
            }
        }
        if (contiene != null) {
            return contiene;
        }
        PeriodoAcademico cercano = periodos.get(0);
        long mejor = Long.MAX_VALUE;
        for (PeriodoAcademico periodo : periodos) {
            long distancia = hoy.isBefore(periodo.getFechaInicio())
                    ? ChronoUnit.DAYS.between(hoy, periodo.getFechaInicio())
                    : ChronoUnit.DAYS.between(periodo.getFechaFin(), hoy);
            if (distancia < mejor) {
                mejor = distancia;
                cercano = periodo;
            }
        }
        return cercano;
    }

    private DetalleAsistencia.Periodo bloque(PeriodoAcademico periodo, List<Object[]> filas, CalculoRebaja calculo,
            boolean actual) {
        List<Object[]> delPeriodo = new ArrayList<>();
        for (Object[] fila : filas) {
            LocalDate fecha = (LocalDate) fila[0];
            if (fecha != null && !fecha.isBefore(periodo.getFechaInicio()) && !fecha.isAfter(periodo.getFechaFin())) {
                delPeriodo.add(fila);
            }
        }
        int[] conteo = new int[4];
        List<Fila> lista = new ArrayList<>();
        for (Object[] fila : delPeriodo) {
            EstadoAsistencia estado = (EstadoAsistencia) fila[2];
            CalculoRebaja.sumar(conteo, estado, 1);
            lista.add(filaDe(fila, estado));
        }
        String codigo = periodo.getCodigo() == null || periodo.getCodigo().isBlank() ? "Período" : periodo.getCodigo();
        String rango = periodo.getFechaInicio().format(FECHA_PERIODO) + " – " + periodo.getFechaFin().format(FECHA_PERIODO);
        return new DetalleAsistencia.Periodo(periodo.getId(), codigo, rango, cuadros(conteo, calculo), lista, actual);
    }

    private static List<Cuadro> cuadros(int[] conteo, CalculoRebaja calculo) {
        return List.of(
                cuadro("is-ausente-injustificada", "Ausencias injustificadas", conteo[1],
                        TipoRebaja.AUSENCIA_INJUSTIFICADA, calculo),
                cuadro("is-ausente-justificada", "Ausencias justificadas", conteo[0],
                        TipoRebaja.AUSENCIA_JUSTIFICADA, calculo),
                cuadro("is-tardia-injustificada", "Tardías injustificadas", conteo[3],
                        TipoRebaja.TARDIA_INJUSTIFICADA, calculo),
                cuadro("is-tardia-justificada", "Tardías justificadas", conteo[2],
                        TipoRebaja.TARDIA_JUSTIFICADA, calculo));
    }

    private static Cuadro cuadro(String clase, String etiqueta, int cantidad, TipoRebaja tipo, CalculoRebaja calculo) {
        return new Cuadro(clase, etiqueta, cantidad, calculo.puntos(tipo, cantidad));
    }

    private static Fila filaDe(Object[] fila, EstadoAsistencia estado) {
        LocalDate fecha = (LocalDate) fila[0];
        Integer leccion = fila[1] instanceof Number numero ? numero.intValue() : null;
        return new Fila(
                fecha == null ? "" : fecha.format(FECHA_PERIODO),
                oGuion(fila[3]),
                leccion == null ? "—" : String.valueOf(leccion),
                etiqueta(estado),
                claseEstado(estado),
                oGuion(fila[5]),
                oGuion(fila[4]));
    }

    private static String etiqueta(EstadoAsistencia estado) {
        if (estado == null) {
            return "";
        }
        return switch (estado) {
            case AUSENTE -> "Ausencia";
            case JUSTIFICADA -> "Ausencia justificada";
            case TARDIA -> "Tardía";
            case TARDIA_JUSTIFICADA -> "Tardía justificada";
            case PRESENTE -> "";
        };
    }

    private static String claseEstado(EstadoAsistencia estado) {
        if (estado == null) {
            return "";
        }
        return estado.esTardia() ? "is-tardia" : "is-ausente";
    }

    private static String meta(Usuario estudiante) {
        String cedula = texto(estudiante.getCedula());
        String seccion = estudiante.getNivelAcademico() == null ? ""
                : "Sección " + estudiante.getNivelAcademico().getNombreCompleto();
        if (!cedula.isEmpty() && !seccion.isEmpty()) {
            return cedula + " · " + seccion;
        }
        return cedula.isEmpty() ? seccion : cedula;
    }

    private static String iniciales(String nombre) {
        String limpio = texto(nombre);
        if (limpio.isEmpty()) {
            return "?";
        }
        String[] partes = limpio.split("\\s+");
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (partes[0].substring(0, 1) + segunda).toUpperCase(Locale.ROOT);
    }

    private static String oGuion(Object valor) {
        String texto = texto(valor == null ? null : String.valueOf(valor));
        return texto.isEmpty() ? "—" : texto;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private FilaAsistencia construirFila(Usuario estudiante, AsistenciaEstudiante registro) {
        return new FilaAsistencia(estudiante,
                registro != null ? registro.getEstado() : null,
                registro != null ? registro.getObservaciones() : null,
                registro != null ? registro.getDocumentoNombre() : null);
    }

    public record DocumentoJustificacion(Path ruta, String nombre) {
    }
}
