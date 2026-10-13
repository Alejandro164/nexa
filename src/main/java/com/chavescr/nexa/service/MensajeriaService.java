package com.chavescr.nexa.service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.dto.MensajeriaVista;
import com.chavescr.nexa.entity.Conversacion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Mensaje;
import com.chavescr.nexa.entity.MensajeReaccion;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ConversacionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MensajeReaccionRepository;
import com.chavescr.nexa.repository.MensajeRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Mensajería directa entre usuarios de una misma dirección.
 *
 * <p>
 * Quién puede escribirle a quién: el personal (administración, dirección y docentes) puede escribirle
 * a cualquier persona activa de la dirección; una familia o un estudiante solo al personal y a sus
 * familiares directos (padres de un estudiante, hijos de una madre o un padre).
 */
@Service
@Transactional
public class MensajeriaService {

    public static final List<String> REACCIONES_RAPIDAS = List.of("👍", "❤️", "😂", "😮", "😢", "🙏", "👏", "🎉",
            "✅", "👀");

    private static final Locale ES = Locale.forLanguageTag("es-CR");
    private static final int MAX_MENSAJES = 300;
    private static final int MAX_TEXTO = 4000;
    private static final int MAX_CONTACTOS = 30;
    private static final long MAX_ARCHIVO = 15L * 1024 * 1024;

    private static final List<String> ROLES_PERSONAL = List.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DOCENTE");
    private static final List<String> ORDEN_ROLES = List.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DOCENTE",
            "ROLE_PADRE", "ROLE_ESTUDIANTE");

    private static final Map<String, String> IMAGENES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "gif", "image/gif", "webp",
            "image/webp");
    private static final Map<String, String> DOCUMENTOS = Map.of(
            "pdf", "application/pdf", "txt", "text/plain", "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final MensajeReaccionRepository reaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final NotificacionService notificacionService;
    private final AlmacenamientoService almacenamientoService;

    public MensajeriaService(ConversacionRepository conversacionRepository, MensajeRepository mensajeRepository,
            MensajeReaccionRepository reaccionRepository, UsuarioRepository usuarioRepository,
            DireccionRepository direccionRepository, NotificacionService notificacionService,
            AlmacenamientoService almacenamientoService) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.reaccionRepository = reaccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionRepository = direccionRepository;
        this.notificacionService = notificacionService;
        this.almacenamientoService = almacenamientoService;
    }

    public static String enlace(Long conversacionId) {
        return "/comunicacion?c=" + conversacionId;
    }

    // ── Lista de conversaciones ───────────────────────────────────────────────

    /** {@code direccionId} es la dirección activa: solo sirve para marcar las conversaciones de otras direcciones. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<MensajeriaVista.Resumen> listar(Long usuarioId, Long direccionId) {
        List<Conversacion> conversaciones = conversacionRepository.listarDeUsuario(usuarioId);
        if (conversaciones.isEmpty()) {
            return List.of();
        }
        List<Long> ids = conversaciones.stream().map(Conversacion::getId).toList();
        Map<Long, Mensaje> ultimos = mensajeRepository.ultimosPorConversacion(ids).stream()
                .collect(Collectors.toMap(m -> m.getConversacion().getId(), m -> m));
        Map<Long, Long> noLeidos = new HashMap<>();
        for (Object[] fila : mensajeRepository.contarNoLeidosPorConversacion(ids, usuarioId)) {
            noLeidos.put((Long) fila[0], (Long) fila[1]);
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<MensajeriaVista.Resumen> resumenes = new ArrayList<>(conversaciones.size());
        for (Conversacion c : conversaciones) {
            Usuario otro = c.getOtro(usuarioId);
            Mensaje ultimo = ultimos.get(c.getId());
            String preview = ultimo == null ? "Conversación nueva" : vistaPrevia(ultimo, usuarioId);
            LocalDateTime cuando = ultimo == null ? c.getFechaCreacion() : ultimo.getFecha();
            String rol = rolPrincipal(otro);
            resumenes.add(new MensajeriaVista.Resumen(c.getId(), otro.getNombre(), iniciales(otro.getNombre()),
                    etiquetaRol(rol), claseRol(rol), preview, horaLista(cuando, ahora),
                    noLeidos.getOrDefault(c.getId(), 0L), otraDireccion(c, direccionId)));
        }
        return resumenes;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long contarNoLeidos(Long usuarioId, Long direccionId) {
        return listar(usuarioId, direccionId).stream().mapToLong(MensajeriaVista.Resumen::noLeidos).sum();
    }

    // ── Hilo ──────────────────────────────────────────────────────────────────

    /** Abre la conversación: la marca como leída para quien la abre y apaga su notificación. */
    @Transactional(rollbackFor = Exception.class)
    public MensajeriaVista.Hilo abrir(Long conversacionId, Long usuarioId, Long direccionId) {
        Conversacion conversacion = requerir(conversacionId, usuarioId);
        marcarLeida(conversacion, usuarioId);
        return construirHilo(conversacion, usuarioId, direccionId);
    }

    /**
     * Sondeo del hilo abierto: devuelve vacío si nada cambió desde la {@code firma} que ya tiene el
     * navegador, para no repintar (ni perder el scroll) cada pocos segundos sin necesidad.
     */
    @Transactional(rollbackFor = Exception.class)
    public Optional<MensajeriaVista.Hilo> refrescar(Long conversacionId, Long usuarioId,
            Long direccionId, String firmaConocida) {
        Conversacion conversacion = requerir(conversacionId, usuarioId);
        marcarLeida(conversacion, usuarioId);
        String firma = firma(conversacion, usuarioId);
        if (firma.equals(firmaConocida)) {
            return Optional.empty();
        }
        return Optional.of(construirHilo(conversacion, usuarioId, direccionId));
    }

    private void marcarLeida(Conversacion conversacion, Long usuarioId) {
        long ultimo = mensajeRepository.ultimoId(conversacion.getId());
        if (ultimo > conversacion.getUltimoLeido(usuarioId)) {
            conversacion.marcarLeidoHasta(usuarioId, ultimo);
            conversacionRepository.save(conversacion);
        }
        notificacionService.marcarLeidasPorEnlace(usuarioId, enlace(conversacion.getId()));
    }

    private MensajeriaVista.Hilo construirHilo(Conversacion conversacion, Long usuarioId, Long direccionActivaId) {
        Usuario otro = conversacion.getOtro(usuarioId);
        Long otroLeido = conversacion.getUltimoLeido(otro.getId());

        List<Mensaje> mensajes = new ArrayList<>(
                mensajeRepository.recientes(conversacion.getId(), PageRequest.of(0, MAX_MENSAJES)));
        Collections.reverse(mensajes);

        Map<Long, List<MensajeReaccion>> reacciones = mensajes.isEmpty() ? Map.of()
                : reaccionRepository.findByMensajeIds(mensajes.stream().map(Mensaje::getId).toList()).stream()
                        .collect(Collectors.groupingBy(r -> r.getMensaje().getId()));

        LocalDateTime ahora = LocalDateTime.now();
        List<MensajeriaVista.Mensaje> vista = new ArrayList<>(mensajes.size());
        for (Mensaje m : mensajes) {
            vista.add(vistaMensaje(m, usuarioId, otroLeido, reacciones.getOrDefault(m.getId(), List.of()), ahora));
        }
        String rol = rolPrincipal(otro);
        return new MensajeriaVista.Hilo(conversacion.getId(), otro.getNombre(), iniciales(otro.getNombre()),
                etiquetaRol(rol), claseRol(rol), otraDireccion(conversacion, direccionActivaId), vista,
                firma(conversacion, usuarioId));
    }

    private MensajeriaVista.Mensaje vistaMensaje(Mensaje m, Long usuarioId, Long otroLeido,
            List<MensajeReaccion> reacciones, LocalDateTime ahora) {
        boolean mio = m.getAutor().getId().equals(usuarioId);
        String rol = rolPrincipal(m.getAutor());

        MensajeriaVista.Cita cita = null;
        Mensaje citado = m.getRespuestaA();
        if (citado != null) {
            String textoCitado = citado.getTexto() != null && !citado.getTexto().isBlank() ? citado.getTexto()
                    : citado.getArchivoNombre();
            cita = new MensajeriaVista.Cita(citado.getAutor().getId().equals(usuarioId) ? "Tú"
                    : citado.getAutor().getNombre(), recortar(textoCitado, 140));
        }
        MensajeriaVista.Archivo archivo = m.tieneArchivo()
                ? new MensajeriaVista.Archivo(m.getId(), m.getArchivoNombre(), tamano(m.getArchivoTamano()),
                        Boolean.TRUE.equals(m.getArchivoImagen()))
                : null;

        Map<String, List<MensajeReaccion>> porEmoji = new LinkedHashMap<>();
        for (MensajeReaccion r : reacciones) {
            porEmoji.computeIfAbsent(r.getEmoji(), e -> new ArrayList<>()).add(r);
        }
        List<MensajeriaVista.Reaccion> chips = new ArrayList<>();
        porEmoji.forEach((emoji, lista) -> {
            boolean yo = lista.stream().anyMatch(r -> r.getUsuario().getId().equals(usuarioId));
            chips.add(new MensajeriaVista.Reaccion(emoji, lista.size(), yo, tituloReaccion(emoji, lista.size(), yo)));
        });

        return new MensajeriaVista.Mensaje(m.getId(), mio, mio ? "Tú" : m.getAutor().getNombre(),
                iniciales(m.getAutor().getNombre()), mio ? "yo" : claseRol(rol), etiquetaDia(m.getFecha(), ahora),
                m.getFecha().format(DateTimeFormatter.ofPattern("HH:mm")), m.getTexto(), cita, archivo, chips,
                mio && m.getId() <= otroLeido);
    }

    private String firma(Conversacion conversacion, Long usuarioId) {
        Long id = conversacion.getId();
        Long otroLeido = conversacion.getUltimoLeido(conversacion.getOtro(usuarioId).getId());
        return mensajeRepository.ultimoId(id) + ":" + reaccionRepository.contarDeConversacion(id) + ":"
                + reaccionRepository.ultimoIdDeConversacion(id) + ":" + otroLeido;
    }

    // ── Envío ─────────────────────────────────────────────────────────────────

    /**
     * Guarda el texto (si hay) y cada archivo como mensajes separados. Si se responde a otro mensaje,
     * solo el primero de ellos lleva la cita.
     */
    @Transactional(rollbackFor = Exception.class)
    public void enviar(Long conversacionId, Long usuarioId, String texto, Long respuestaId,
            List<MultipartFile> archivos) throws IOException {
        Conversacion conversacion = requerir(conversacionId, usuarioId);
        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        verificarVigente(conversacion, autor);

        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() > MAX_TEXTO) {
            throw new IllegalArgumentException("El mensaje es demasiado largo (máximo " + MAX_TEXTO + " caracteres).");
        }
        List<MultipartFile> adjuntos = archivos == null ? List.of()
                : archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
        if (limpio.isEmpty() && adjuntos.isEmpty()) {
            throw new IllegalArgumentException("Escribe un mensaje o adjunta un archivo.");
        }
        for (MultipartFile adjunto : adjuntos) {
            validarArchivo(adjunto);
        }

        Mensaje citado = null;
        if (respuestaId != null) {
            citado = mensajeRepository.findById(respuestaId)
                    .filter(m -> m.getConversacion().getId().equals(conversacionId))
                    .orElseThrow(() -> new IllegalArgumentException("El mensaje al que respondes ya no existe."));
        }

        Mensaje ultimo = null;
        if (!limpio.isEmpty()) {
            ultimo = guardarMensaje(conversacion, autor, limpio, citado);
            citado = null;
        }
        for (MultipartFile adjunto : adjuntos) {
            Mensaje m = nuevoMensaje(conversacion, autor, null, citado);
            citado = null;
            String ext = extension(adjunto.getOriginalFilename());
            m.setArchivoRuta(almacenamientoService.guardar(adjunto, conversacion.getDireccion(), "mensajeria",
                    String.valueOf(conversacion.getId())));
            m.setArchivoNombre(nombreVisible(adjunto.getOriginalFilename()));
            m.setArchivoTamano(adjunto.getSize());
            m.setArchivoImagen(IMAGENES.containsKey(ext));
            m.setArchivoTipoContenido(IMAGENES.containsKey(ext) ? IMAGENES.get(ext) : DOCUMENTOS.get(ext));
            ultimo = mensajeRepository.save(m);
        }

        conversacion.setFechaUltimoMensaje(ultimo.getFecha());
        conversacion.marcarLeidoHasta(usuarioId, ultimo.getId());
        conversacionRepository.save(conversacion);
        notificar(conversacion, autor, ultimo);
    }

    private Mensaje guardarMensaje(Conversacion conversacion, Usuario autor, String texto, Mensaje citado) {
        return mensajeRepository.save(nuevoMensaje(conversacion, autor, texto, citado));
    }

    private Mensaje nuevoMensaje(Conversacion conversacion, Usuario autor, String texto, Mensaje citado) {
        Mensaje m = new Mensaje();
        m.setConversacion(conversacion);
        m.setAutor(autor);
        m.setTexto(texto);
        m.setRespuestaA(citado);
        return m;
    }

    private void validarArchivo(MultipartFile archivo) {
        String ext = extension(archivo.getOriginalFilename());
        if (!IMAGENES.containsKey(ext) && !DOCUMENTOS.containsKey(ext)) {
            throw new IllegalArgumentException(
                    "Formato no permitido. Usa PDF, Word, Excel, PowerPoint, texto o imágenes (JPG, PNG, GIF, WEBP).");
        }
        if (archivo.getSize() > MAX_ARCHIVO) {
            throw new IllegalArgumentException("Cada archivo puede pesar como máximo 15 MB.");
        }
    }

    /** Una notificación sin leer por conversación, con el conteo de lo que el destinatario no ha visto. */
    private void notificar(Conversacion conversacion, Usuario autor, Mensaje ultimo) {
        Usuario destino = conversacion.getOtro(autor.getId());
        long pendientes = mensajeRepository.contarPendientes(conversacion.getId(), destino.getId(),
                conversacion.getUltimoLeido(destino.getId()));
        String texto = pendientes <= 1
                ? autor.getNombre() + ": " + vistaPrevia(ultimo, destino.getId())
                : autor.getNombre() + " te envió " + pendientes + " mensajes nuevos";
        notificacionService.reemplazarPorEnlace(destino, recortar(texto, 290), enlace(conversacion.getId()));
    }

    // ── Reacciones ────────────────────────────────────────────────────────────

    /** Pone o quita la reacción del usuario y devuelve el id de la conversación afectada. */
    @Transactional(rollbackFor = Exception.class)
    public Long reaccionar(Long mensajeId, String emoji, Long usuarioId) {
        String limpio = emoji == null ? "" : emoji.strip();
        if (limpio.isEmpty() || limpio.length() > 16 || limpio.codePoints().anyMatch(Character::isLetter)) {
            throw new IllegalArgumentException("Reacción no válida.");
        }
        Mensaje mensaje = mensajeRepository.findByIdConConversacion(mensajeId)
                .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));
        Conversacion conversacion = mensaje.getConversacion();
        validarAcceso(conversacion, usuarioId);

        var existente = reaccionRepository.buscar(mensajeId, usuarioId, limpio);
        if (existente.isPresent()) {
            reaccionRepository.delete(existente.get());
        } else {
            MensajeReaccion reaccion = new MensajeReaccion();
            reaccion.setMensaje(mensaje);
            reaccion.setUsuario(usuarioRepository.getReferenceById(usuarioId));
            reaccion.setEmoji(limpio);
            reaccionRepository.save(reaccion);
        }
        return conversacion.getId();
    }

    // ── Contactos y nuevas conversaciones ─────────────────────────────────────

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<MensajeriaVista.Contacto> contactos(Long usuarioId, Long direccionId, String busqueda) {
        Usuario yo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Map<Long, Usuario> candidatos = new LinkedHashMap<>();
        if (esPersonal(yo)) {
            usuarioRepository.findActivosByDireccionId(direccionId).forEach(u -> candidatos.put(u.getId(), u));
        } else {
            usuarioRepository.findActivosByDireccionIdAndRolIn(direccionId, ROLES_PERSONAL)
                    .forEach(u -> candidatos.put(u.getId(), u));
            familiares(yo).stream().filter(u -> Boolean.TRUE.equals(u.getActivo()) && enDireccion(u, direccionId))
                    .forEach(u -> candidatos.put(u.getId(), u));
        }
        candidatos.remove(usuarioId);

        // Los familiares directos van primero para que el tope de resultados no los deje fuera.
        Set<Long> familiaresIds = familiares(yo).stream().map(Usuario::getId).collect(Collectors.toSet());
        String filtro = normalizar(busqueda);
        return candidatos.values().stream()
                .filter(u -> filtro.isEmpty() || normalizar(u.getNombre()).contains(filtro)
                        || normalizar(u.getEmail()).contains(filtro))
                .sorted(Comparator.<Usuario, Boolean>comparing(u -> !familiaresIds.contains(u.getId()))
                        .thenComparing(u -> normalizar(u.getNombre())))
                .limit(MAX_CONTACTOS)
                .map(u -> {
                    String rol = rolPrincipal(u);
                    return new MensajeriaVista.Contacto(u.getId(), u.getNombre(), iniciales(u.getNombre()),
                            etiquetaRol(rol), claseRol(rol));
                })
                .toList();
    }

    /** Devuelve la conversación existente con esa persona o crea una nueva si el permiso lo admite. */
    @Transactional(rollbackFor = Exception.class)
    public Long abrirOCrear(Long usuarioId, Long otroId, Long direccionId) {
        Usuario yo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Usuario otro = usuarioRepository.findById(otroId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!puedeEscribir(yo, otro, direccionId)) {
            throw new IllegalArgumentException("No puedes iniciar una conversación con esa persona.");
        }
        Long a = Math.min(usuarioId, otroId);
        Long b = Math.max(usuarioId, otroId);
        return conversacionRepository.buscarPar(direccionId, a, b).map(Conversacion::getId).orElseGet(() -> {
            Direccion direccion = direccionRepository.findById(direccionId)
                    .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
            Conversacion nueva = new Conversacion();
            nueva.setDireccion(direccion);
            nueva.setUsuarioA(a.equals(usuarioId) ? yo : otro);
            nueva.setUsuarioB(a.equals(usuarioId) ? otro : yo);
            return conversacionRepository.save(nueva).getId();
        });
    }

    // ── Archivos adjuntos ─────────────────────────────────────────────────────

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public MensajeriaVista.ArchivoDescarga archivo(Long mensajeId, Long usuarioId) {
        Mensaje mensaje = mensajeRepository.findByIdConConversacion(mensajeId)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));
        validarAcceso(mensaje.getConversacion(), usuarioId);
        if (!mensaje.tieneArchivo()) {
            throw new IllegalArgumentException("El mensaje no tiene archivo");
        }
        return new MensajeriaVista.ArchivoDescarga(almacenamientoService.resolver(mensaje.getArchivoRuta()),
                mensaje.getArchivoNombre(), mensaje.getArchivoTipoContenido(),
                Boolean.TRUE.equals(mensaje.getArchivoImagen()));
    }

    // ── Reglas de acceso ──────────────────────────────────────────────────────

    private Conversacion requerir(Long conversacionId, Long usuarioId) {
        Conversacion conversacion = conversacionRepository.findByIdConUsuarios(conversacionId)
                .orElseThrow(() -> new IllegalArgumentException("Conversación no encontrada"));
        validarAcceso(conversacion, usuarioId);
        return conversacion;
    }

    private void validarAcceso(Conversacion conversacion, Long usuarioId) {
        if (!conversacion.participa(usuarioId)) {
            throw new IllegalArgumentException("Conversación no encontrada");
        }
    }

    /** Antes de escribir se revisa que el permiso siga vigente (por ejemplo, que nadie haya sido desactivado). */
    private void verificarVigente(Conversacion conversacion, Usuario yo) {
        Usuario otro = conversacion.getOtro(yo.getId());
        if (!puedeEscribir(yo, otro, conversacion.getDireccion().getId())) {
            throw new IllegalArgumentException("Ya no puedes enviar mensajes a esta persona.");
        }
    }

    private boolean puedeEscribir(Usuario yo, Usuario otro, Long direccionId) {
        if (yo.getId().equals(otro.getId()) || !Boolean.TRUE.equals(otro.getActivo())
                || !enDireccion(otro, direccionId)) {
            return false;
        }
        return esPersonal(yo) || esPersonal(otro)
                || familiares(yo).stream().anyMatch(f -> f.getId().equals(otro.getId()));
    }

    private boolean esPersonal(Usuario usuario) {
        return usuario.getRoles().stream().anyMatch(r -> ROLES_PERSONAL.contains(r.getNombre()));
    }

    private Set<Usuario> familiares(Usuario usuario) {
        Set<Usuario> familiares = new HashSet<>();
        Set<String> roles = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
        if (roles.contains("ROLE_PADRE")) {
            familiares.addAll(usuario.getEstudiantes());
        }
        if (roles.contains("ROLE_ESTUDIANTE")) {
            familiares.addAll(usuario.getPadres());
        }
        return familiares;
    }

    private boolean enDireccion(Usuario usuario, Long direccionId) {
        return usuario.getDirecciones().stream().anyMatch(d -> d.getId().equals(direccionId));
    }

    // ── Formato ───────────────────────────────────────────────────────────────

    /** Nombre de la dirección de la conversación cuando no es la activa; null si es la misma. */
    private static String otraDireccion(Conversacion conversacion, Long direccionActivaId) {
        Direccion direccion = conversacion.getDireccion();
        return direccion.getId().equals(direccionActivaId) ? null : direccion.getPresentacion();
    }

    private static String rolPrincipal(Usuario usuario) {
        Set<String> nombres = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
        return ORDEN_ROLES.stream().filter(nombres::contains).findFirst().orElse("");
    }

    private static String etiquetaRol(String rol) {
        return switch (rol) {
            case "ROLE_ADMIN" -> "Administración";
            case "ROLE_DIRECTOR" -> "Dirección";
            case "ROLE_DOCENTE" -> "Docente";
            case "ROLE_PADRE" -> "Familia";
            case "ROLE_ESTUDIANTE" -> "Estudiante";
            default -> "";
        };
    }

    private static String claseRol(String rol) {
        return switch (rol) {
            case "ROLE_ADMIN", "ROLE_DIRECTOR" -> "institucion";
            case "ROLE_DOCENTE" -> "docente";
            case "ROLE_PADRE" -> "familia";
            case "ROLE_ESTUDIANTE" -> "estudiante";
            default -> "";
        };
    }

    static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.strip().split("\\s+");
        String primera = partes[0].substring(0, 1);
        String segunda = partes.length > 1 ? partes[1].substring(0, 1) : "";
        return (primera + segunda).toUpperCase(ES);
    }

    private String vistaPrevia(Mensaje mensaje, Long usuarioId) {
        String cuerpo;
        if (mensaje.getTexto() != null && !mensaje.getTexto().isBlank()) {
            cuerpo = recortar(mensaje.getTexto().replaceAll("\\s+", " "), 80);
        } else if (Boolean.TRUE.equals(mensaje.getArchivoImagen())) {
            cuerpo = "📷 Foto";
        } else {
            cuerpo = "📎 " + mensaje.getArchivoNombre();
        }
        return mensaje.getAutor().getId().equals(usuarioId) ? "Tú: " + cuerpo : cuerpo;
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max - 1).stripTrailing() + "…";
    }

    private static String horaLista(LocalDateTime fecha, LocalDateTime ahora) {
        LocalDate dia = fecha.toLocalDate();
        LocalDate hoy = ahora.toLocalDate();
        if (dia.equals(hoy)) {
            return fecha.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        if (dia.equals(hoy.minusDays(1))) {
            return "Ayer";
        }
        if (dia.isAfter(hoy.minusDays(7))) {
            return capitalizar(fecha.format(DateTimeFormatter.ofPattern("EEE", ES)).replace(".", ""));
        }
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
    }

    private static String etiquetaDia(LocalDateTime fecha, LocalDateTime ahora) {
        LocalDate dia = fecha.toLocalDate();
        LocalDate hoy = ahora.toLocalDate();
        if (dia.equals(hoy)) {
            return "Hoy";
        }
        if (dia.equals(hoy.minusDays(1))) {
            return "Ayer";
        }
        return capitalizar(fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)));
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }

    private static String tituloReaccion(String emoji, long cantidad, boolean yo) {
        if (yo) {
            return cantidad == 1 ? "Reaccionaste con " + emoji
                    : "Tú y " + (cantidad - 1) + (cantidad == 2 ? " persona más" : " más") + " reaccionaron con "
                            + emoji;
        }
        return cantidad + (cantidad == 1 ? " persona reaccionó con " : " personas reaccionaron con ") + emoji;
    }

    private static String tamano(Long bytes) {
        if (bytes == null) {
            return "";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return String.format(ES, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String extension(String nombre) {
        if (nombre == null || !nombre.contains(".")) {
            return "";
        }
        return nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /** Nombre para mostrar: solo el último tramo (algunos navegadores envían la ruta completa). */
    private static String nombreVisible(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "archivo";
        }
        String limpio = nombre.replace('\\', '/');
        return recortar(limpio.substring(limpio.lastIndexOf('/') + 1), 200);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(ES).strip();
    }
}
