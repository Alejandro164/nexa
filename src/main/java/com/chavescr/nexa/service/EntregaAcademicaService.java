package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.dto.FilaEntregaAcademica;
import com.chavescr.nexa.dto.PaginaEntregas;
import com.chavescr.nexa.entity.AdjuntoEntregaAcademica;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.EntregaAcademica;
import com.chavescr.nexa.entity.EntregaAcademica.TipoEntrega;
import com.chavescr.nexa.entity.EntregaAcademicaDocente;
import com.chavescr.nexa.entity.EntregaAcademicaDocente.EstadoPresentacion;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AdjuntoEntregaAcademicaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.EntregaAcademicaDocenteRepository;
import com.chavescr.nexa.repository.EntregaAcademicaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Entregas académicas: la dirección define qué documentos pide y con qué plazo; cada docente los
 * prepara como borrador (observación + archivos o enlaces), los presenta y la dirección los aprueba
 * o los devuelve con observaciones.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class EntregaAcademicaService {

    private static final Logger log = LoggerFactory.getLogger(EntregaAcademicaService.class);

    public static final int TAMANO_PAGINA = 10;
    public static final int MAX_ADJUNTOS = 15;
    private static final long MAX_BYTES_ARCHIVO = 25L * 1024 * 1024;
    private static final String ROL_DOCENTE = "ROLE_DOCENTE";
    private static final String ENLACE_MODULO = "/entregas-academicas";

    /** Filtros de las tablas (todos opcionales). */
    public record FiltroEntregas(Long entregaId, TipoEntrega tipo, Long periodoId) {
        public static FiltroEntregas ninguno() {
            return new FiltroEntregas(null, null, null);
        }
    }

    private final EntregaAcademicaRepository entregaRepository;
    private final EntregaAcademicaDocenteRepository presentacionRepository;
    private final AdjuntoEntregaAcademicaRepository adjuntoRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final AlmacenamientoService almacenamientoService;
    private final NotificacionService notificacionService;

    public EntregaAcademicaService(EntregaAcademicaRepository entregaRepository,
            EntregaAcademicaDocenteRepository presentacionRepository,
            AdjuntoEntregaAcademicaRepository adjuntoRepository, PeriodoAcademicoRepository periodoRepository,
            UsuarioRepository usuarioRepository, DireccionRepository direccionRepository,
            AlmacenamientoService almacenamientoService, NotificacionService notificacionService) {
        this.entregaRepository = entregaRepository;
        this.presentacionRepository = presentacionRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
        this.almacenamientoService = almacenamientoService;
        this.notificacionService = notificacionService;
    }

    // ─── Tablas de Pendientes y Realizadas ────────────────────────────────────

    /**
     * Pendientes: entregas activas cuyo plazo ya empezó y que el docente no ha presentado (sin iniciar,
     * borrador o devueltas). Realizadas: las presentadas o aprobadas. Un docente solo ve lo suyo; la
     * dirección ve a todos los docentes.
     */
    @Transactional(readOnly = true)
    public PaginaEntregas listar(Long direccionId, Long usuarioId, boolean supervisor, boolean pendientes,
            FiltroEntregas filtro, int pagina) {
        FiltroEntregas f = filtro != null ? filtro : FiltroEntregas.ninguno();
        List<FilaEntregaAcademica> filas = pendientes
                ? filasPendientes(direccionId, usuarioId, supervisor)
                : filasRealizadas(direccionId, usuarioId, supervisor);
        filas = filas.stream()
                .filter(fila -> f.entregaId() == null || f.entregaId().equals(fila.getEntrega().getId()))
                .filter(fila -> f.tipo() == null || f.tipo() == fila.getEntrega().getTipo())
                .filter(fila -> f.periodoId() == null || (fila.getEntrega().getPeriodo() != null
                        && f.periodoId().equals(fila.getEntrega().getPeriodo().getId())))
                .toList();

        int total = filas.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) TAMANO_PAGINA));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int desde = (actual - 1) * TAMANO_PAGINA;
        return new PaginaEntregas(filas.subList(Math.min(desde, total), Math.min(desde + TAMANO_PAGINA, total)),
                actual, totalPaginas, total);
    }

    @Transactional(readOnly = true)
    public long contarPendientes(Long direccionId, Long usuarioId, boolean supervisor) {
        return filasPendientes(direccionId, usuarioId, supervisor).size();
    }

    private List<FilaEntregaAcademica> filasPendientes(Long direccionId, Long usuarioId, boolean supervisor) {
        List<Usuario> docentes = docentesEnAlcance(direccionId, usuarioId, supervisor);
        Map<String, EntregaAcademicaDocente> respuestas = respuestasPorClave(direccionId, docentes);
        List<FilaEntregaAcademica> filas = new ArrayList<>();
        for (EntregaAcademica entrega : entregaRepository
                .findByDireccionIdAndActivaTrueAndFechaInicioLessThanEqualOrderByFechaFinAsc(direccionId, LocalDate.now())) {
            for (Usuario docente : docentes) {
                EntregaAcademicaDocente respuesta = respuestas.get(clave(entrega.getId(), docente.getId()));
                if (respuesta == null || respuesta.isEditable()) {
                    filas.add(new FilaEntregaAcademica(entrega, docente, respuesta));
                }
            }
        }
        return filas;
    }

    private List<FilaEntregaAcademica> filasRealizadas(Long direccionId, Long usuarioId, boolean supervisor) {
        List<EntregaAcademicaDocente> respuestas = supervisor
                ? presentacionRepository.findByEntrega_Direccion_Id(direccionId)
                : presentacionRepository.findByEntrega_Direccion_IdAndDocenteId(direccionId, usuarioId);
        return respuestas.stream()
                .filter(EntregaAcademicaDocente::isRealizada)
                .sorted(Comparator.comparing(EntregaAcademicaDocente::getFechaPresentacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(r -> new FilaEntregaAcademica(r.getEntrega(), r.getDocente(), r))
                .toList();
    }

    private List<Usuario> docentesEnAlcance(Long direccionId, Long usuarioId, boolean supervisor) {
        if (supervisor) {
            return usuarioRepository.findActivosByDireccionIdAndRol(direccionId, ROL_DOCENTE);
        }
        return List.of(obtenerUsuario(usuarioId));
    }

    private Map<String, EntregaAcademicaDocente> respuestasPorClave(Long direccionId, List<Usuario> docentes) {
        Map<String, EntregaAcademicaDocente> mapa = new HashMap<>();
        for (EntregaAcademicaDocente r : presentacionRepository.findByEntrega_Direccion_Id(direccionId)) {
            if (docentes.stream().anyMatch(d -> d.getId().equals(r.getDocente().getId()))) {
                mapa.put(clave(r.getEntrega().getId(), r.getDocente().getId()), r);
            }
        }
        return mapa;
    }

    private String clave(Long entregaId, Long docenteId) {
        return entregaId + ":" + docenteId;
    }

    // ─── Mantenimiento (dirección) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EntregaAcademica> listarEntregas(Long direccionId) {
        return entregaRepository.findByDireccionIdOrderByFechaInicioDescNombreAsc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<PeriodoAcademico> listarPeriodos(Long direccionId) {
        return periodoRepository.findByDireccionIdOrderByFechaInicioDesc(direccionId);
    }

    @Transactional(readOnly = true)
    public EntregaAcademica obtenerEntrega(Long direccionId, Long id) {
        return entregaRepository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada"));
    }

    public EntregaAcademica guardarEntrega(Long direccionId, Long usuarioId, Long id, String nombre,
            String descripcion, TipoEntrega tipo, Long periodoId, LocalDate inicio, LocalDate fin, boolean activa) {
        String limpio = nombre != null ? nombre.trim() : "";
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (limpio.length() > 200) {
            throw new IllegalArgumentException("El nombre no puede superar los 200 caracteres");
        }
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("El plazo de presentación necesita fecha de inicio y de fin");
        }
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
        String detalle = descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null;
        if (detalle != null && detalle.length() > 1000) {
            throw new IllegalArgumentException("La descripción no puede superar los 1000 caracteres");
        }

        boolean nueva = id == null;
        EntregaAcademica entrega = nueva ? new EntregaAcademica() : obtenerEntrega(direccionId, id);
        if (nueva) {
            entrega.setDireccion(obtenerDireccion(direccionId));
            entrega.setCreadoPor(obtenerUsuario(usuarioId));
        }
        entrega.setNombre(limpio);
        entrega.setDescripcion(detalle);
        entrega.setTipo(tipo != null ? tipo : TipoEntrega.OTRA);
        entrega.setPeriodo(periodoId != null
                ? periodoRepository.findByIdAndDireccionId(periodoId, direccionId)
                        .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"))
                : null);
        entrega.setFechaInicio(inicio);
        entrega.setFechaFin(fin);
        entrega.setActiva(activa);
        EntregaAcademica guardada = entregaRepository.save(entrega);
        log.info("Entrega académica guardada: id={}, nombre={}", guardada.getId(), guardada.getNombre());

        if (nueva && activa) {
            avisarADocentes(direccionId, guardada);
        }
        return guardada;
    }

    public void alternarActiva(Long direccionId, Long id) {
        EntregaAcademica entrega = obtenerEntrega(direccionId, id);
        entrega.setActiva(!entrega.isActiva());
        entregaRepository.save(entrega);
    }

    /** Solo se borra una entrega que nadie ha empezado; si ya tiene respuestas se desactiva en su lugar. */
    public void eliminarEntrega(Long direccionId, Long id) {
        EntregaAcademica entrega = obtenerEntrega(direccionId, id);
        if (presentacionRepository.existsByEntregaId(id)) {
            throw new IllegalArgumentException(
                    "La entrega ya tiene respuestas de docentes. Desactívala para que deje de aparecer como pendiente.");
        }
        entregaRepository.delete(entrega);
        log.info("Entrega académica eliminada: id={}", id);
    }

    /** Estado de cada docente de la dirección frente a una entrega. */
    @Transactional(readOnly = true)
    public List<FilaEntregaAcademica> seguimiento(Long direccionId, Long entregaId) {
        EntregaAcademica entrega = obtenerEntrega(direccionId, entregaId);
        Map<Long, EntregaAcademicaDocente> porDocente = new HashMap<>();
        presentacionRepository.findByEntregaId(entregaId).forEach(r -> porDocente.put(r.getDocente().getId(), r));
        return usuarioRepository.findActivosByDireccionIdAndRol(direccionId, ROL_DOCENTE).stream()
                .map(d -> new FilaEntregaAcademica(entrega, d, porDocente.get(d.getId())))
                .toList();
    }

    // ─── Respuesta del docente ────────────────────────────────────────────────

    /** Fila a mostrar en el detalle; el docente solo puede abrir sus propias entregas ya iniciadas. */
    @Transactional(readOnly = true)
    public FilaEntregaAcademica detalle(Long direccionId, Long usuarioId, boolean supervisor, Long entregaId,
            Long docenteId) {
        EntregaAcademica entrega = obtenerEntrega(direccionId, entregaId);
        Long idDocente = supervisor ? docenteId : usuarioId;
        if (idDocente == null) {
            throw new IllegalArgumentException("Docente no indicado");
        }
        EntregaAcademicaDocente respuesta = presentacionRepository.findByEntregaIdAndDocenteId(entregaId, idDocente)
                .orElse(null);
        if (!supervisor && respuesta == null && !disponible(entrega)) {
            throw new IllegalArgumentException("La entrega todavía no está disponible");
        }
        return new FilaEntregaAcademica(entrega, obtenerUsuario(idDocente), respuesta);
    }

    public void guardarBorrador(Long direccionId, Long docenteId, Long entregaId, String observacion) {
        EntregaAcademicaDocente respuesta = respuestaEditable(direccionId, docenteId, entregaId);
        if (observacion != null) {
            respuesta.setObservacion(observacionLimpia(observacion));
        }
        presentacionRepository.save(respuesta);
    }

    public void agregarAdjuntos(Long direccionId, Long docenteId, Long entregaId, List<MultipartFile> archivos,
            String enlace) throws IOException {
        List<MultipartFile> validos = archivos == null ? List.of()
                : archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
        String url = enlaceValido(enlace);
        if (validos.isEmpty() && url == null) {
            throw new IllegalArgumentException("Elige un archivo o escribe un enlace");
        }
        EntregaAcademicaDocente respuesta = respuestaEditable(direccionId, docenteId, entregaId);
        if (respuesta.getAdjuntos().size() + validos.size() + (url != null ? 1 : 0) > MAX_ADJUNTOS) {
            throw new IllegalArgumentException("Máximo " + MAX_ADJUNTOS + " adjuntos por entrega");
        }
        Direccion direccion = respuesta.getEntrega().getDireccion();
        for (MultipartFile archivo : validos) {
            String nombre = nombreArchivo(archivo);
            if (archivo.getSize() > MAX_BYTES_ARCHIVO) {
                throw new IllegalArgumentException("«" + nombre + "» supera el máximo de 25 MB");
            }
            String ruta = almacenamientoService.guardar(archivo, direccion, "entregas-academicas",
                    String.valueOf(entregaId), String.valueOf(docenteId));
            // Si la transacción se revierte, el archivo recién escrito no debe quedar huérfano en disco
            alRevertir(() -> almacenamientoService.eliminar(ruta));
            AdjuntoEntregaAcademica adjunto = nuevoAdjunto(respuesta, nombre);
            adjunto.setRuta(ruta);
            adjunto.setTamanoBytes(archivo.getSize());
            int punto = nombre.lastIndexOf('.');
            if (punto > 0 && nombre.length() - punto <= 20) {
                adjunto.setExtension(nombre.substring(punto + 1).toUpperCase());
            }
            respuesta.getAdjuntos().add(0, adjunto);
        }
        if (url != null) {
            AdjuntoEntregaAcademica adjunto = nuevoAdjunto(respuesta, url.length() > 255 ? url.substring(0, 255) : url);
            adjunto.setUrl(url);
            respuesta.getAdjuntos().add(0, adjunto);
        }
        presentacionRepository.save(respuesta);
    }

    public void eliminarAdjunto(Long direccionId, Long docenteId, Long entregaId, Long adjuntoId) {
        EntregaAcademicaDocente respuesta = respuestaEditable(direccionId, docenteId, entregaId);
        AdjuntoEntregaAcademica adjunto = respuesta.getAdjuntos().stream()
                .filter(a -> adjuntoId.equals(a.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado"));
        respuesta.getAdjuntos().remove(adjunto);
        presentacionRepository.save(respuesta);
        if (adjunto.getRuta() != null) {
            alConfirmar(() -> almacenamientoService.eliminar(adjunto.getRuta()));
        }
    }

    public void presentar(Long direccionId, Long docenteId, Long entregaId, String observacion) {
        EntregaAcademicaDocente respuesta = respuestaEditable(direccionId, docenteId, entregaId);
        // Sin observación nueva se conserva la que ya estaba guardada
        String nota = observacion != null ? observacionLimpia(observacion) : respuesta.getObservacion();
        respuesta.setObservacion(nota);
        if (respuesta.getAdjuntos().isEmpty() && nota == null) {
            throw new IllegalArgumentException("Adjunta un archivo o un enlace, o escribe una observación, antes de presentar");
        }
        respuesta.setEstado(EstadoPresentacion.PRESENTADA);
        respuesta.setFechaPresentacion(LocalDateTime.now());
        // Una nueva presentación reemplaza la revisión anterior
        respuesta.setFechaRevision(null);
        respuesta.setRevisadoPor(null);
        respuesta.setObservacionRevision(null);
        presentacionRepository.save(respuesta);

        EntregaAcademica entrega = respuesta.getEntrega();
        notificacionService.crear(entrega.getCreadoPor(), recortar(
                respuesta.getDocente().getNombre() + " presentó «" + entrega.getNombre() + "»", 300), ENLACE_MODULO);
        log.info("Entrega académica presentada: entrega={}, docente={}", entregaId, docenteId);
    }

    // ─── Revisión (dirección) ─────────────────────────────────────────────────

    public void revisar(Long direccionId, Long revisorId, Long presentacionId, boolean aprobar, String observacion) {
        EntregaAcademicaDocente respuesta = presentacionRepository
                .findByIdAndEntrega_Direccion_Id(presentacionId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada"));
        if (!respuesta.isPorRevisar()) {
            throw new IllegalArgumentException("Solo se revisan las entregas que están presentadas");
        }
        String nota = observacion != null && !observacion.isBlank() ? observacion.trim() : null;
        if (nota != null && nota.length() > 1000) {
            throw new IllegalArgumentException("La observación no puede superar los 1000 caracteres");
        }
        if (!aprobar && nota == null) {
            throw new IllegalArgumentException("Indica qué debe corregir el docente");
        }
        respuesta.setEstado(aprobar ? EstadoPresentacion.APROBADA : EstadoPresentacion.DEVUELTA);
        respuesta.setObservacionRevision(nota);
        respuesta.setRevisadoPor(obtenerUsuario(revisorId));
        respuesta.setFechaRevision(LocalDateTime.now());
        presentacionRepository.save(respuesta);

        String nombre = respuesta.getEntrega().getNombre();
        notificacionService.crear(respuesta.getDocente(), recortar(aprobar
                ? "Tu entrega «" + nombre + "» fue aprobada"
                : "Tu entrega «" + nombre + "» fue devuelta con observaciones", 300), ENLACE_MODULO);
    }

    // ─── Adjuntos: descarga ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdjuntoEntregaAcademica adjuntoParaDescarga(Long direccionId, Long usuarioId, boolean supervisor,
            Long adjuntoId) {
        AdjuntoEntregaAcademica adjunto = adjuntoRepository
                .findByIdAndPresentacion_Entrega_Direccion_Id(adjuntoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado"));
        if (!supervisor && !adjunto.getPresentacion().getDocente().getId().equals(usuarioId)) {
            throw new AccessDeniedException("El adjunto no es tuyo");
        }
        if (adjunto.getRuta() == null) {
            throw new IllegalArgumentException("El adjunto es un enlace, no un archivo");
        }
        return adjunto;
    }

    public Path rutaFisica(AdjuntoEntregaAcademica adjunto) {
        return almacenamientoService.resolver(adjunto.getRuta());
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    /** La entrega ya está abierta para los docentes: activa y con el plazo iniciado. */
    private boolean disponible(EntregaAcademica entrega) {
        return entrega.isActiva() && !entrega.getFechaInicio().isAfter(LocalDate.now());
    }

    /** Respuesta del docente lista para editar; si todavía no existe se crea como borrador. */
    private EntregaAcademicaDocente respuestaEditable(Long direccionId, Long docenteId, Long entregaId) {
        EntregaAcademica entrega = obtenerEntrega(direccionId, entregaId);
        if (!entrega.isActiva()) {
            throw new IllegalArgumentException("La entrega está desactivada");
        }
        EntregaAcademicaDocente respuesta = presentacionRepository.findByEntregaIdAndDocenteId(entregaId, docenteId)
                .orElse(null);
        if (respuesta == null) {
            if (!disponible(entrega)) {
                throw new IllegalArgumentException("La entrega todavía no está disponible");
            }
            respuesta = new EntregaAcademicaDocente();
            respuesta.setEntrega(entrega);
            respuesta.setDocente(obtenerUsuario(docenteId));
            return presentacionRepository.save(respuesta);
        }
        if (!respuesta.isEditable()) {
            throw new IllegalArgumentException("La entrega ya fue presentada y no se puede modificar");
        }
        return respuesta;
    }

    private void avisarADocentes(Long direccionId, EntregaAcademica entrega) {
        if (entrega.getFechaInicio().isAfter(LocalDate.now())) {
            return;
        }
        notificacionService.crearTodas(usuarioRepository.findActivosByDireccionIdAndRol(direccionId, ROL_DOCENTE),
                recortar("Nueva entrega académica: «" + entrega.getNombre() + "»", 300), ENLACE_MODULO);
    }

    private String observacionLimpia(String observacion) {
        String nota = observacion != null && !observacion.isBlank() ? observacion.trim() : null;
        if (nota != null && nota.length() > 2000) {
            throw new IllegalArgumentException("La observación no puede superar los 2000 caracteres");
        }
        return nota;
    }

    private AdjuntoEntregaAcademica nuevoAdjunto(EntregaAcademicaDocente respuesta, String nombre) {
        AdjuntoEntregaAcademica adjunto = new AdjuntoEntregaAcademica();
        adjunto.setPresentacion(respuesta);
        adjunto.setNombre(nombre);
        return adjunto;
    }

    private String nombreArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
        return recortar(nombre.isBlank() ? "archivo" : nombre, 255);
    }

    // Se fuerza http(s) para que no se cuelen esquemas como javascript:.
    private String enlaceValido(String enlace) {
        if (enlace == null || enlace.isBlank()) {
            return null;
        }
        String url = enlace.trim();
        if (!url.matches("(?i)^https?://.*")) {
            url = "https://" + url;
        }
        if (url.length() > 1000 || !url.matches("(?i)^https?://[^\\s]+$")) {
            throw new IllegalArgumentException("El enlace no es válido");
        }
        return url;
    }

    private String recortar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Direccion obtenerDireccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }

    private void alRevertir(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    accion.run();
                }
            }
        });
    }

    private void alConfirmar(Runnable accion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accion.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                accion.run();
            }
        });
    }
}
