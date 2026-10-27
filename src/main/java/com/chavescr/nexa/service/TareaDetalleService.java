package com.chavescr.nexa.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.entity.ActividadTarea;
import com.chavescr.nexa.entity.ActividadTarea.TipoActividad;
import com.chavescr.nexa.entity.AdjuntoTarea;
import com.chavescr.nexa.entity.ChecklistItemTarea;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.EtiquetaTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo;
import com.chavescr.nexa.entity.TareaPlanTrabajo.EstadoTarea;
import com.chavescr.nexa.entity.TareaPlanTrabajo.RecordatorioTarea;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.AdjuntoTareaRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.EtiquetaTareaRepository;
import com.chavescr.nexa.repository.TareaPlanTrabajoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Funciones de detalle de una tarea del plan de trabajo, al estilo de una tarjeta de Trello:
 * estado, etiquetas, fechas con recordatorio, checklist, miembros, adjuntos y el panel de
 * comentarios y actividad. Cada cambio deja una entrada en la actividad de la tarea.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TareaDetalleService {

    private static final Logger log = LoggerFactory.getLogger(TareaDetalleService.class);

    public static final int MAX_ADJUNTOS = 20;
    private static final long MAX_BYTES_ARCHIVO = 25L * 1024 * 1024;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    /** Colores de la paleta del selector de etiquetas. */
    public static final List<String> COLORES_ETIQUETA = List.of("#61bd4f", "#f2d600", "#ff9f1a", "#eb5a46",
            "#c377e0", "#0079bf", "#00c2e0", "#51e898", "#ff78cb", "#7b8794");

    private static final List<String[]> ETIQUETAS_BASE = List.of(
            new String[] { "Planeamiento", "#61bd4f" },
            new String[] { "Evaluación", "#0079bf" },
            new String[] { "Administrativo", "#c377e0" },
            new String[] { "Circular MEP", "#ff9f1a" },
            new String[] { "Urgente", "#eb5a46" });

    private final TareaPlanTrabajoRepository tareaRepository;
    private final EtiquetaTareaRepository etiquetaRepository;
    private final AdjuntoTareaRepository adjuntoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final AlmacenamientoService almacenamientoService;
    private final NotificacionService notificacionService;

    public TareaDetalleService(TareaPlanTrabajoRepository tareaRepository,
            EtiquetaTareaRepository etiquetaRepository, AdjuntoTareaRepository adjuntoRepository,
            UsuarioRepository usuarioRepository, DireccionRepository direccionRepository,
            AlmacenamientoService almacenamientoService, NotificacionService notificacionService) {
        this.tareaRepository = tareaRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
        this.almacenamientoService = almacenamientoService;
        this.notificacionService = notificacionService;
    }

    // ─── Consulta ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TareaPlanTrabajo obtener(Long direccionId, Long tareaId) {
        return tareaRepository.findByIdAndPlanTrabajo_Direccion_Id(tareaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));
    }

    /** Catálogo de etiquetas de la dirección; la primera vez se crea uno base para no empezar en blanco. */
    public List<EtiquetaTarea> listarEtiquetas(Long direccionId) {
        List<EtiquetaTarea> etiquetas = etiquetaRepository.findByDireccionIdOrderByNombreAsc(direccionId);
        if (!etiquetas.isEmpty()) {
            return etiquetas;
        }
        Direccion direccion = obtenerDireccion(direccionId);
        for (String[] base : ETIQUETAS_BASE) {
            EtiquetaTarea etiqueta = new EtiquetaTarea();
            etiqueta.setDireccion(direccion);
            etiqueta.setNombre(base[0]);
            etiqueta.setColor(base[1]);
            etiquetaRepository.save(etiqueta);
        }
        return etiquetaRepository.findByDireccionIdOrderByNombreAsc(direccionId);
    }

    /** Personal de la dirección que todavía no participa en la tarea (candidatos para "Miembros"). */
    @Transactional(readOnly = true)
    public List<Usuario> listarCandidatos(Long direccionId, TareaPlanTrabajo tarea) {
        Long responsableId = tarea.getMiembro() != null ? tarea.getMiembro().getUsuario().getId() : null;
        return usuarioRepository.findActivosByDireccionId(direccionId).stream()
                .filter(u -> !u.getId().equals(responsableId))
                .filter(u -> tarea.getColaboradores().stream().noneMatch(c -> c.getId().equals(u.getId())))
                .toList();
    }

    // ─── Contenido y estado ───────────────────────────────────────────────────

    public void cambiarEstado(Long direccionId, Long usuarioId, Long tareaId, EstadoTarea nuevo) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        if (nuevo == null || nuevo == tarea.getEstado()) {
            return;
        }
        EstadoTarea anterior = tarea.getEstado();
        tarea.setEstado(nuevo);
        tarea.setFechaCompletada(nuevo.isCerrado() ? LocalDateTime.now() : null);
        registrar(tarea, usuarioId, "cambió el estado de «" + anterior.getEtiqueta() + "» a «" + nuevo.getEtiqueta() + "»");
        tareaRepository.save(tarea);
    }

    public void actualizarContenido(Long direccionId, Long usuarioId, Long tareaId, String titulo, String descripcion) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        String nuevoTitulo = titulo != null ? titulo.trim() : "";
        if (nuevoTitulo.isEmpty()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (nuevoTitulo.length() > 200) {
            throw new IllegalArgumentException("El título no puede superar los 200 caracteres");
        }
        String nuevaDescripcion = descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null;
        if (nuevaDescripcion != null && nuevaDescripcion.length() > 1000) {
            throw new IllegalArgumentException("La descripción no puede superar los 1000 caracteres");
        }
        if (!nuevoTitulo.equals(tarea.getTitulo())) {
            registrar(tarea, usuarioId, "cambió el título de «" + tarea.getTitulo() + "» a «" + nuevoTitulo + "»");
            tarea.setTitulo(nuevoTitulo);
        }
        if (!Objects.equals(nuevaDescripcion, tarea.getDescripcion())) {
            registrar(tarea, usuarioId, "actualizó la descripción");
            tarea.setDescripcion(nuevaDescripcion);
        }
        tareaRepository.save(tarea);
    }

    // ─── Fechas y recordatorio ────────────────────────────────────────────────

    public void actualizarFechas(Long direccionId, Long usuarioId, Long tareaId, LocalDate inicio,
            LocalDate limite, RecordatorioTarea recordatorio) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        if (limite == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (inicio != null && inicio.isAfter(limite)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior al vencimiento");
        }
        RecordatorioTarea nuevoRecordatorio = recordatorio != null ? recordatorio : RecordatorioTarea.NINGUNO;
        boolean cambio = !Objects.equals(inicio, tarea.getFechaInicio()) || !limite.equals(tarea.getFechaLimite())
                || nuevoRecordatorio != tarea.getRecordatorio();
        if (!cambio) {
            return;
        }
        if (!limite.equals(tarea.getFechaLimite()) || nuevoRecordatorio != tarea.getRecordatorio()) {
            tarea.setRecordatorioEnviado(false);
        }
        tarea.setFechaInicio(inicio);
        tarea.setFechaLimite(limite);
        tarea.setRecordatorio(nuevoRecordatorio);
        registrar(tarea, usuarioId, "cambió las fechas: " + (inicio != null ? "inicio " + inicio.format(FECHA) + ", " : "")
                + "vence " + limite.format(FECHA) + ", recordatorio «" + nuevoRecordatorio.getEtiqueta() + "»");
        tareaRepository.save(tarea);
    }

    // ─── Etiquetas ────────────────────────────────────────────────────────────

    public void alternarEtiqueta(Long direccionId, Long usuarioId, Long tareaId, Long etiquetaId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        EtiquetaTarea etiqueta = etiquetaRepository.findByIdAndDireccionId(etiquetaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Etiqueta no encontrada"));
        boolean quitada = tarea.getEtiquetas().removeIf(e -> e.getId().equals(etiquetaId));
        if (!quitada) {
            tarea.getEtiquetas().add(etiqueta);
        }
        registrar(tarea, usuarioId, (quitada ? "quitó" : "agregó") + " la etiqueta «" + etiqueta.getNombre() + "»");
        tareaRepository.save(tarea);
    }

    /** Crea una etiqueta nueva en el catálogo de la dirección y la agrega a la tarea. */
    public void crearEtiqueta(Long direccionId, Long usuarioId, Long tareaId, String nombre, String color) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        String limpio = nombre != null ? nombre.trim() : "";
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la etiqueta es obligatorio");
        }
        if (limpio.length() > 60) {
            throw new IllegalArgumentException("El nombre de la etiqueta no puede superar los 60 caracteres");
        }
        if (color == null || !COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("Elige un color para la etiqueta");
        }
        if (etiquetaRepository.existsByDireccionIdAndNombreIgnoreCase(direccionId, limpio)) {
            throw new IllegalArgumentException("Ya existe una etiqueta con ese nombre");
        }
        EtiquetaTarea etiqueta = new EtiquetaTarea();
        etiqueta.setDireccion(obtenerDireccion(direccionId));
        etiqueta.setNombre(limpio);
        etiqueta.setColor(color.toLowerCase());
        etiqueta = etiquetaRepository.save(etiqueta);
        tarea.getEtiquetas().add(etiqueta);
        registrar(tarea, usuarioId, "creó y agregó la etiqueta «" + limpio + "»");
        tareaRepository.save(tarea);
    }

    /** Borra la etiqueta del catálogo y la quita de todas las tareas que la usaban. */
    public void eliminarEtiqueta(Long direccionId, Long etiquetaId) {
        EtiquetaTarea etiqueta = etiquetaRepository.findByIdAndDireccionId(etiquetaId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Etiqueta no encontrada"));
        for (TareaPlanTrabajo tarea : tareaRepository.findByEtiquetas_Id(etiquetaId)) {
            tarea.getEtiquetas().removeIf(e -> e.getId().equals(etiquetaId));
            tareaRepository.save(tarea);
        }
        etiquetaRepository.delete(etiqueta);
        log.info("Etiqueta de tarea eliminada: id={}, nombre={}", etiquetaId, etiqueta.getNombre());
    }

    // ─── Checklist ────────────────────────────────────────────────────────────

    public void agregarItem(Long direccionId, Long usuarioId, Long tareaId, String texto) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        String limpio = texto != null ? texto.trim() : "";
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Escribe la subtarea");
        }
        if (limpio.length() > 300) {
            throw new IllegalArgumentException("La subtarea no puede superar los 300 caracteres");
        }
        ChecklistItemTarea item = new ChecklistItemTarea();
        item.setTarea(tarea);
        item.setTexto(limpio);
        item.setOrden(tarea.getChecklist().stream().mapToInt(ChecklistItemTarea::getOrden).max().orElse(-1) + 1);
        tarea.getChecklist().add(item);
        registrar(tarea, usuarioId, "agregó «" + limpio + "» a la checklist");
        tareaRepository.save(tarea);
    }

    public void alternarItem(Long direccionId, Long usuarioId, Long tareaId, Long itemId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        ChecklistItemTarea item = buscarItem(tarea, itemId);
        item.setCompletado(!item.isCompletado());
        registrar(tarea, usuarioId, (item.isCompletado() ? "completó" : "reabrió") + " «" + item.getTexto() + "»");
        tareaRepository.save(tarea);
    }

    public void eliminarItem(Long direccionId, Long usuarioId, Long tareaId, Long itemId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        ChecklistItemTarea item = buscarItem(tarea, itemId);
        tarea.getChecklist().remove(item);
        registrar(tarea, usuarioId, "eliminó «" + item.getTexto() + "» de la checklist");
        tareaRepository.save(tarea);
    }

    private ChecklistItemTarea buscarItem(TareaPlanTrabajo tarea, Long itemId) {
        return tarea.getChecklist().stream().filter(i -> itemId.equals(i.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Subtarea no encontrada"));
    }

    // ─── Miembros ─────────────────────────────────────────────────────────────

    public void agregarColaborador(Long direccionId, Long usuarioId, Long tareaId, Long colaboradorId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        Usuario colaborador = usuarioRepository.findActivosByDireccionId(direccionId).stream()
                .filter(u -> u.getId().equals(colaboradorId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El usuario no pertenece a esta institución"));
        boolean esResponsable = tarea.getMiembro() != null && tarea.getMiembro().getUsuario().getId().equals(colaboradorId);
        if (esResponsable || tarea.getColaboradores().stream().anyMatch(c -> c.getId().equals(colaboradorId))) {
            throw new IllegalArgumentException("Ya participa en la tarea");
        }
        tarea.getColaboradores().add(colaborador);
        registrar(tarea, usuarioId, "agregó a " + colaborador.getNombre() + " a la tarea");
        tareaRepository.save(tarea);
        if (!colaborador.getId().equals(usuarioId)) {
            notificacionService.crear(colaborador, recortar("Te agregaron a la tarea «" + tarea.getTitulo() + "»", 300), "/agenda");
        }
    }

    public void quitarColaborador(Long direccionId, Long usuarioId, Long tareaId, Long colaboradorId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        Usuario quitado = tarea.getColaboradores().stream().filter(c -> c.getId().equals(colaboradorId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El usuario no es colaborador de la tarea"));
        tarea.getColaboradores().remove(quitado);
        registrar(tarea, usuarioId, "quitó a " + quitado.getNombre() + " de la tarea");
        tareaRepository.save(tarea);
    }

    // ─── Adjuntos ─────────────────────────────────────────────────────────────

    public void agregarAdjuntos(Long direccionId, Long usuarioId, Long tareaId, List<MultipartFile> archivos,
            String enlace) throws IOException {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        List<MultipartFile> validos = archivos == null ? List.of()
                : archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
        String url = enlaceValido(enlace);
        if (validos.isEmpty() && url == null) {
            throw new IllegalArgumentException("Elige un archivo o escribe un enlace");
        }
        if (tarea.getAdjuntos().size() + validos.size() + (url != null ? 1 : 0) > MAX_ADJUNTOS) {
            throw new IllegalArgumentException("Máximo " + MAX_ADJUNTOS + " adjuntos por tarea");
        }
        Usuario autor = obtenerUsuario(usuarioId);
        Direccion direccion = tarea.getPlanTrabajo().getDireccion();
        for (MultipartFile archivo : validos) {
            if (archivo.getSize() > MAX_BYTES_ARCHIVO) {
                throw new IllegalArgumentException("«" + nombreArchivo(archivo) + "» supera el máximo de 25 MB");
            }
            String ruta = almacenamientoService.guardar(archivo, direccion, "agenda-tareas", String.valueOf(tareaId));
            // Si la transacción se revierte, el archivo recién escrito no debe quedar huérfano en disco
            alRevertir(() -> almacenamientoService.eliminar(ruta));
            String nombre = nombreArchivo(archivo);
            AdjuntoTarea adjunto = nuevoAdjunto(tarea, autor, nombre);
            adjunto.setRuta(ruta);
            adjunto.setTamanoBytes(archivo.getSize());
            int punto = nombre.lastIndexOf('.');
            if (punto > 0 && nombre.length() - punto <= 20) {
                adjunto.setExtension(nombre.substring(punto + 1).toUpperCase());
            }
            tarea.getAdjuntos().add(0, adjunto);
            registrar(tarea, usuarioId, "adjuntó el archivo «" + nombre + "»");
        }
        if (url != null) {
            AdjuntoTarea adjunto = nuevoAdjunto(tarea, autor, url.length() > 255 ? url.substring(0, 255) : url);
            adjunto.setUrl(url);
            tarea.getAdjuntos().add(0, adjunto);
            registrar(tarea, usuarioId, "adjuntó el enlace " + url);
        }
        tareaRepository.save(tarea);
    }

    public void eliminarAdjunto(Long direccionId, Long usuarioId, Long tareaId, Long adjuntoId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        AdjuntoTarea adjunto = tarea.getAdjuntos().stream().filter(a -> adjuntoId.equals(a.getId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado"));
        tarea.getAdjuntos().remove(adjunto);
        registrar(tarea, usuarioId, "eliminó el adjunto «" + adjunto.getNombre() + "»");
        tareaRepository.save(tarea);
        if (adjunto.getRuta() != null) {
            alConfirmar(() -> almacenamientoService.eliminar(adjunto.getRuta()));
        }
    }

    @Transactional(readOnly = true)
    public AdjuntoTarea adjuntoParaDescarga(Long direccionId, Long adjuntoId) {
        AdjuntoTarea adjunto = adjuntoRepository.findByIdAndTarea_PlanTrabajo_Direccion_Id(adjuntoId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado"));
        if (adjunto.getRuta() == null) {
            throw new IllegalArgumentException("El adjunto es un enlace, no un archivo");
        }
        return adjunto;
    }

    public Path rutaFisica(AdjuntoTarea adjunto) {
        return almacenamientoService.resolver(adjunto.getRuta());
    }

    // ─── Comentarios ──────────────────────────────────────────────────────────

    public void comentar(Long direccionId, Long usuarioId, Long tareaId, String texto) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        String limpio = texto != null ? texto.trim() : "";
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Escribe un comentario");
        }
        if (limpio.length() > 2000) {
            throw new IllegalArgumentException("El comentario no puede superar los 2000 caracteres");
        }
        tarea.getActividad().add(0, nuevaEntrada(tarea, usuarioId, TipoActividad.COMENTARIO, limpio));
        tareaRepository.save(tarea);
    }

    /** Solo el autor (o quien supervisa: administrador/director) puede borrar un comentario. */
    public void eliminarComentario(Long direccionId, Long usuarioId, boolean supervisor, Long tareaId, Long entradaId) {
        TareaPlanTrabajo tarea = obtener(direccionId, tareaId);
        ActividadTarea entrada = tarea.getActividad().stream()
                .filter(a -> entradaId.equals(a.getId()) && a.isComentario()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));
        if (!supervisor && !entrada.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("Solo puedes eliminar tus propios comentarios");
        }
        tarea.getActividad().remove(entrada);
        tareaRepository.save(tarea);
    }

    // ─── Recordatorios ────────────────────────────────────────────────────────

    /**
     * Envía a la campanita del responsable y de los colaboradores el aviso de las tareas cuyo
     * recordatorio ya llegó. Lo invoca el job programado; devuelve cuántas tareas se avisaron.
     */
    public int enviarRecordatoriosPendientes() {
        LocalDate hoy = LocalDate.now();
        int avisadas = 0;
        List<TareaPlanTrabajo> candidatas = tareaRepository.findConRecordatorioPendiente(
                RecordatorioTarea.NINGUNO, EstadoTarea.FINALIZADO);
        for (TareaPlanTrabajo tarea : candidatas) {
            LocalDate fechaAviso = tarea.getFechaLimite().minusDays(tarea.getRecordatorio().getDiasAntes());
            if (hoy.isBefore(fechaAviso)) {
                continue;
            }
            List<Usuario> destinatarios = new ArrayList<>();
            if (tarea.getMiembro() != null) {
                destinatarios.add(tarea.getMiembro().getUsuario());
            }
            tarea.getColaboradores().stream().filter(c -> !destinatarios.contains(c)).forEach(destinatarios::add);
            String cuando = tarea.getFechaLimite().isBefore(hoy) ? "venció el " + tarea.getFechaLimite().format(FECHA)
                    : tarea.getFechaLimite().equals(hoy) ? "vence hoy"
                    : tarea.getFechaLimite().equals(hoy.plusDays(1)) ? "vence mañana"
                    : "vence el " + tarea.getFechaLimite().format(FECHA);
            notificacionService.crearTodas(destinatarios,
                    recortar("Recordatorio: la tarea «" + tarea.getTitulo() + "» " + cuando, 300), "/agenda");
            tarea.setRecordatorioEnviado(true);
            tareaRepository.save(tarea);
            avisadas++;
        }
        if (avisadas > 0) {
            log.info("Recordatorios de tareas enviados: {}", avisadas);
        }
        return avisadas;
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    private void registrar(TareaPlanTrabajo tarea, Long usuarioId, String texto) {
        tarea.getActividad().add(0, nuevaEntrada(tarea, usuarioId, TipoActividad.ACTIVIDAD, texto));
    }

    private ActividadTarea nuevaEntrada(TareaPlanTrabajo tarea, Long usuarioId, TipoActividad tipo, String texto) {
        ActividadTarea entrada = new ActividadTarea();
        entrada.setTarea(tarea);
        entrada.setUsuario(obtenerUsuario(usuarioId));
        entrada.setTipo(tipo);
        entrada.setTexto(recortar(texto, 2000));
        return entrada;
    }

    private AdjuntoTarea nuevoAdjunto(TareaPlanTrabajo tarea, Usuario autor, String nombre) {
        AdjuntoTarea adjunto = new AdjuntoTarea();
        adjunto.setTarea(tarea);
        adjunto.setSubidoPor(autor);
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
