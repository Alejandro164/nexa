package com.chavescr.nexa.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.ComunicadosVista;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.NubeNodo;
import com.chavescr.nexa.entity.Oficio;
import com.chavescr.nexa.entity.OficioDestinatario;
import com.chavescr.nexa.entity.OficioLectura;
import com.chavescr.nexa.entity.Rol;
import com.chavescr.nexa.entity.TipoDestinatario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.OficioLecturaRepository;
import com.chavescr.nexa.repository.OficioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Comunicados: los oficios ya emitidos vistos por sus destinatarios internos (personas y grupos).
 *
 * <p>
 * Administración y dirección supervisan: eligen un destinatario y ven sus oficios con el progreso de lectura.
 * Docentes, familias y estudiantes solo ven los oficios que les llegan. Un oficio le llega a una persona si
 * se le dirigió a ella, a su rol (docentes, familias, estudiantes), a toda la comunidad o a las familias de la
 * sección de alguno de sus hijos. La lectura se registra la primera vez que abre o descarga el documento.
 */
@Service
@Transactional
public class ComunicadoService {

    private static final Locale ES = Locale.forLanguageTag("es-CR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d MMM yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM yyyy", ES);
    private static final int MAX_PERSONAS = 15;
    private static final int MAX_MENSAJE_NOTIFICACION = 290;

    private final OficioRepository oficioRepository;
    private final OficioLecturaRepository lecturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final NotificacionService notificacionService;
    private final NubeNodoService nubeNodoService;

    public ComunicadoService(OficioRepository oficioRepository, OficioLecturaRepository lecturaRepository,
            UsuarioRepository usuarioRepository, NivelAcademicoRepository nivelRepository,
            NotificacionService notificacionService, NubeNodoService nubeNodoService) {
        this.oficioRepository = oficioRepository;
        this.lecturaRepository = lecturaRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelRepository = nivelRepository;
        this.notificacionService = notificacionService;
        this.nubeNodoService = nubeNodoService;
    }

    public static String enlace(Long oficioId) {
        return "/comunicacion?tab=comunicados&o=" + oficioId;
    }

    // ── Destinatarios del oficio (formulario de oficios) ──────────────────────

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<NivelAcademico> secciones(Long direccionId) {
        return nivelRepository.findByDireccionIdAndActivoTrueOrderByGradoAscSeccionAsc(direccionId);
    }

    /** Convierte las claves del formulario ("COMUNIDAD", "SECCION-12", "PERSONA-50") en destinatarios válidos. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<OficioDestinatario> resolver(Oficio oficio, Long direccionId, Collection<String> claves) {
        List<OficioDestinatario> resueltos = new ArrayList<>();
        if (claves == null) {
            return resueltos;
        }
        for (String clave : new LinkedHashSet<>(claves)) {
            if (clave == null || clave.isBlank()) {
                continue;
            }
            OficioDestinatario d = desdeClave(clave.trim(), direccionId);
            d.setOficio(oficio);
            resueltos.add(d);
        }
        return resueltos;
    }

    /** Reemplaza los destinatarios del oficio por los de {@code claves}. */
    public void asignarDestinatarios(Oficio oficio, Long direccionId, Collection<String> claves) {
        List<OficioDestinatario> nuevos = resolver(oficio, direccionId, claves);
        oficio.getDestinatarios().clear();
        oficio.getDestinatarios().addAll(nuevos);
    }

    private OficioDestinatario desdeClave(String clave, Long direccionId) {
        try {
            if (clave.startsWith("PERSONA-")) {
                Usuario usuario = usuarioRepository.findById(Long.parseLong(clave.substring(8)))
                        .filter(u -> Boolean.TRUE.equals(u.getActivo()) && enDireccion(u, direccionId))
                        .orElseThrow(() -> new IllegalArgumentException("Una de las personas elegidas ya no está disponible."));
                return new OficioDestinatario(null, TipoDestinatario.PERSONA, usuario, null);
            }
            if (clave.startsWith("SECCION-")) {
                NivelAcademico nivel = nivelRepository.findById(Long.parseLong(clave.substring(8)))
                        .filter(n -> n.getDireccion().getId().equals(direccionId))
                        .orElseThrow(() -> new IllegalArgumentException("Una de las secciones elegidas no existe."));
                return new OficioDestinatario(null, TipoDestinatario.FAMILIAS_SECCION, null, nivel);
            }
            TipoDestinatario tipo = TipoDestinatario.valueOf(clave);
            if (!tipo.esGrupoSimple()) {
                throw new IllegalArgumentException("Destinatario no válido.");
            }
            return new OficioDestinatario(null, tipo, null, null);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Destinatario no válido.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("No enum constant")) {
                throw new IllegalArgumentException("Destinatario no válido.");
            }
            throw e;
        }
    }

    public Set<String> clavesDe(Oficio oficio) {
        return oficio.getDestinatarios().stream().map(OficioDestinatario::clave)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Personas ya elegidas (para volver a pintar el formulario). */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ComunicadosVista.Persona> personasElegidas(Long direccionId, Collection<String> claves) {
        List<ComunicadosVista.Persona> personas = new ArrayList<>();
        if (claves == null) {
            return personas;
        }
        for (String clave : new LinkedHashSet<>(claves)) {
            if (clave != null && clave.startsWith("PERSONA-")) {
                try {
                    usuarioRepository.findById(Long.parseLong(clave.substring(8))).ifPresent(u -> personas.add(persona(u)));
                } catch (NumberFormatException e) {
                    // clave inválida: se ignora, la validación al guardar avisará
                }
            }
        }
        return personas;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ComunicadosVista.Persona> buscarPersonas(Long direccionId, String busqueda) {
        String filtro = FormatoVista.normalizar(busqueda);
        return usuarioRepository.findActivosByDireccionId(direccionId).stream()
                .filter(u -> filtro.isEmpty() || FormatoVista.normalizar(u.getNombre()).contains(filtro))
                .limit(MAX_PERSONAS)
                .map(this::persona)
                .toList();
    }

    private ComunicadosVista.Persona persona(Usuario u) {
        String rol = RolVista.principal(u);
        return new ComunicadosVista.Persona(u.getId(), u.getNombre(), detallePersona(u, rol), RolVista.iniciales(u.getNombre()),
                RolVista.clase(rol));
    }

    private String detallePersona(Usuario u, String rol) {
        String etiqueta = RolVista.etiqueta(rol);
        if ("ROLE_PADRE".equals(rol) && !u.getEstudiantes().isEmpty()) {
            String hijos = u.getEstudiantes().stream().map(Usuario::getNombre).sorted().limit(2)
                    .collect(Collectors.joining(", "));
            return etiqueta + " de " + hijos;
        }
        if ("ROLE_ESTUDIANTE".equals(rol) && u.getNivelAcademico() != null) {
            return etiqueta + " · " + u.getNivelAcademico().getNombreCompleto();
        }
        return etiqueta;
    }

    // ── A quién le llega un oficio ────────────────────────────────────────────

    /** Las personas activas que reciben el oficio por ese destinatario. */
    private Map<Long, Usuario> audiencia(OficioDestinatario d, Long direccionId, Map<String, Map<Long, Usuario>> cache) {
        return cache.computeIfAbsent(d.clave(), clave -> {
            List<Usuario> personas = switch (d.getTipo()) {
                case COMUNIDAD -> usuarioRepository.findActivosByDireccionId(direccionId);
                case DOCENTES -> usuarioRepository.findActivosByDireccionIdAndRol(direccionId, "ROLE_DOCENTE");
                case FAMILIAS -> usuarioRepository.findActivosByDireccionIdAndRol(direccionId, "ROLE_PADRE");
                case ESTUDIANTES -> usuarioRepository.findActivosByDireccionIdAndRol(direccionId, "ROLE_ESTUDIANTE");
                case FAMILIAS_SECCION -> familiasDeSeccion(d.getNivel().getId());
                case PERSONA -> Boolean.TRUE.equals(d.getUsuario().getActivo()) ? List.of(d.getUsuario()) : List.of();
            };
            Map<Long, Usuario> mapa = new LinkedHashMap<>();
            personas.forEach(u -> mapa.put(u.getId(), u));
            return mapa;
        });
    }

    private Map<Long, Usuario> audienciaDe(Oficio oficio, Long direccionId, Map<String, Map<Long, Usuario>> cache) {
        Map<Long, Usuario> union = new LinkedHashMap<>();
        for (OficioDestinatario d : oficio.getDestinatarios()) {
            union.putAll(audiencia(d, direccionId, cache));
        }
        return union;
    }

    private List<Usuario> familiasDeSeccion(Long nivelId) {
        Map<Long, Usuario> familias = new LinkedHashMap<>();
        for (Usuario estudiante : usuarioRepository.findEstudiantesActivosByNivelId(nivelId)) {
            for (Usuario padre : estudiante.getPadres()) {
                if (Boolean.TRUE.equals(padre.getActivo())) {
                    familias.put(padre.getId(), padre);
                }
            }
        }
        return new ArrayList<>(familias.values());
    }

    /** ¿Le llega este oficio a esta persona? (sin armar la lista completa de cada grupo) */
    private boolean alcanza(Oficio oficio, Usuario usuario) {
        Set<String> roles = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
        for (OficioDestinatario d : oficio.getDestinatarios()) {
            boolean llega = switch (d.getTipo()) {
                case COMUNIDAD -> true;
                case DOCENTES -> roles.contains("ROLE_DOCENTE");
                case FAMILIAS -> roles.contains("ROLE_PADRE");
                case ESTUDIANTES -> roles.contains("ROLE_ESTUDIANTE");
                case FAMILIAS_SECCION -> roles.contains("ROLE_PADRE") && usuario.getEstudiantes().stream()
                        .anyMatch(e -> e.getNivelAcademico() != null
                                && e.getNivelAcademico().getId().equals(d.getNivel().getId()));
                case PERSONA -> d.getUsuario().getId().equals(usuario.getId());
            };
            if (llega) {
                return true;
            }
        }
        return false;
    }

    // ── Lectura ───────────────────────────────────────────────────────────────

    /** {oficioId -> {usuarioId -> fecha de lectura}} */
    private Map<Long, Map<Long, LocalDateTime>> lecturas(Collection<Long> oficioIds) {
        Map<Long, Map<Long, LocalDateTime>> mapa = new HashMap<>();
        if (oficioIds.isEmpty()) {
            return mapa;
        }
        for (Object[] fila : lecturaRepository.lecturasDe(oficioIds)) {
            mapa.computeIfAbsent((Long) fila[0], k -> new HashMap<>()).put((Long) fila[1], (LocalDateTime) fila[2]);
        }
        return mapa;
    }

    private ComunicadosVista.Lectura lecturaGrupo(Oficio oficio, Long direccionId,
            Map<String, Map<Long, Usuario>> cache, Map<Long, LocalDateTime> lecturasDelOficio) {
        Map<Long, Usuario> audiencia = audienciaDe(oficio, direccionId, cache);
        long leidos = audiencia.keySet().stream().filter(lecturasDelOficio::containsKey).count();
        return new ComunicadosVista.Lectura(true, leidos, audiencia.size(), null);
    }

    private void registrarLectura(Oficio oficio, Usuario usuario) {
        if (lecturaRepository.existsByOficioIdAndUsuarioId(oficio.getId(), usuario.getId())) {
            return;
        }
        OficioLectura lectura = new OficioLectura();
        lectura.setOficio(oficio);
        lectura.setUsuario(usuario);
        try {
            lecturaRepository.saveAndFlush(lectura);
        } catch (DataIntegrityViolationException e) {
            // Dos pestañas abrieron el documento a la vez: ya quedó registrada.
        }
    }

    // ── Panel de destinatarios (supervisión) ──────────────────────────────────

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ComunicadosVista.Destinatario> destinatarios(Long direccionId, String busqueda) {
        List<Oficio> emitidos = oficioRepository.emitidosConDestinatarios(direccionId);
        String filtro = FormatoVista.normalizar(busqueda);
        Map<String, Map<Long, Usuario>> cache = new HashMap<>();
        Map<Long, Map<Long, LocalDateTime>> lecturas = lecturas(emitidos.stream().map(Oficio::getId).toList());

        // Grupos: cuántos oficios se les dirigieron y si alguno tiene lectura pendiente.
        class Grupo {
            final OficioDestinatario muestra;
            long oficios;
            boolean pendiente;

            Grupo(OficioDestinatario muestra) {
                this.muestra = muestra;
            }
        }
        Map<String, Grupo> grupos = new LinkedHashMap<>();
        Map<Long, Usuario> personasDirectas = new LinkedHashMap<>();
        for (Oficio oficio : emitidos) {
            Boolean pendiente = null;
            for (OficioDestinatario d : oficio.getDestinatarios()) {
                if (d.getTipo() == TipoDestinatario.PERSONA) {
                    personasDirectas.put(d.getUsuario().getId(), d.getUsuario());
                    continue;
                }
                if (pendiente == null) {
                    pendiente = lecturaGrupo(oficio, direccionId, cache,
                            lecturas.getOrDefault(oficio.getId(), Map.of())).pendientes() > 0;
                }
                Grupo grupo = grupos.computeIfAbsent(d.clave(), k -> new Grupo(d));
                grupo.oficios++;
                grupo.pendiente |= pendiente;
            }
        }

        List<ComunicadosVista.Destinatario> resultado = new ArrayList<>();
        grupos.values().stream()
                .sorted(Comparator.comparing((Grupo g) -> g.muestra.getTipo().ordinal())
                        .thenComparing(g -> g.muestra.etiqueta()))
                .filter(g -> filtro.isEmpty() || FormatoVista.normalizar(g.muestra.etiqueta()).contains(filtro))
                .forEach(g -> resultado.add(new ComunicadosVista.Destinatario(g.muestra.clave(), true,
                        g.muestra.etiqueta(), detalleGrupo(g.muestra, direccionId, cache), "", claseGrupo(g.muestra),
                        g.oficios, g.pendiente)));

        // Personas: las que recibieron oficios directos; al buscar, cualquiera de la dirección.
        Map<Long, Usuario> personas = new LinkedHashMap<>();
        if (filtro.isEmpty()) {
            personas.putAll(personasDirectas);
        } else {
            usuarioRepository.findActivosByDireccionId(direccionId).stream()
                    .filter(u -> FormatoVista.normalizar(u.getNombre()).contains(filtro))
                    .limit(MAX_PERSONAS).forEach(u -> personas.put(u.getId(), u));
        }
        personas.values().stream()
                .sorted(Comparator.comparing(u -> FormatoVista.normalizar(u.getNombre())))
                .forEach(u -> {
                    List<Oficio> reciben = emitidos.stream().filter(o -> alcanza(o, u)).toList();
                    boolean pendiente = reciben.stream()
                            .anyMatch(o -> !lecturas.getOrDefault(o.getId(), Map.of()).containsKey(u.getId()));
                    String rol = RolVista.principal(u);
                    resultado.add(new ComunicadosVista.Destinatario("PERSONA-" + u.getId(), false, u.getNombre(),
                            detallePersona(u, rol), RolVista.iniciales(u.getNombre()), RolVista.clase(rol),
                            reciben.size(), pendiente));
                });
        return resultado;
    }

    private String detalleGrupo(OficioDestinatario d, Long direccionId, Map<String, Map<Long, Usuario>> cache) {
        int personas = audiencia(d, direccionId, cache).size();
        String base = d.getTipo() == TipoDestinatario.FAMILIAS_SECCION ? "Familias de los estudiantes"
                : d.getTipo().getDescripcion();
        return base + " · " + personas + (personas == 1 ? " persona" : " personas");
    }

    private String claseGrupo(OficioDestinatario d) {
        return switch (d.getTipo()) {
            case DOCENTES -> "docente";
            case FAMILIAS, FAMILIAS_SECCION -> "familia";
            case ESTUDIANTES -> "estudiante";
            default -> "grupo";
        };
    }

    // ── Panel de oficios ──────────────────────────────────────────────────────

    /**
     * Los oficios del destinatario elegido. {@code clave} es un grupo o una persona; quien no supervisa solo puede
     * pedir su propia persona.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ComunicadosVista.Panel panel(String clave, Long direccionId, Long viewerId, boolean supervision,
            String busqueda, String tipo, Integer anio) {
        Usuario viewer = usuario(viewerId);
        List<Oficio> emitidos = oficioRepository.emitidosConDestinatarios(direccionId);
        Map<String, Map<Long, Usuario>> cache = new HashMap<>();

        boolean esPersona = clave.startsWith("PERSONA-");
        Usuario sujeto = null;
        String titulo;
        String subtitulo;
        String iniciales = "";
        String claseRol;
        List<Oficio> base;
        if (esPersona) {
            sujeto = usuario(parseId(clave, "PERSONA-"));
            if (!supervision && !sujeto.getId().equals(viewerId)) {
                throw new IllegalArgumentException("Destinatario no disponible");
            }
            Usuario persona = sujeto;
            base = emitidos.stream().filter(o -> alcanza(o, persona)).toList();
            String rol = RolVista.principal(sujeto);
            titulo = supervision ? sujeto.getNombre() : "Mis comunicados";
            subtitulo = supervision ? "Persona · " + detallePersona(sujeto, rol) : "Oficios dirigidos a ti";
            iniciales = RolVista.iniciales(sujeto.getNombre());
            claseRol = RolVista.clase(rol);
        } else {
            if (!supervision) {
                throw new IllegalArgumentException("Destinatario no disponible");
            }
            OficioDestinatario grupo = desdeClave(clave, direccionId);
            base = emitidos.stream()
                    .filter(o -> o.getDestinatarios().stream().anyMatch(d -> d.clave().equals(clave))).toList();
            int personas = audiencia(grupo, direccionId, cache).size();
            titulo = grupo.etiqueta();
            subtitulo = "Grupo · " + personas + (personas == 1 ? " persona" : " personas");
            claseRol = claseGrupo(grupo);
        }

        List<Integer> anios = base.stream().map(o -> fechaDe(o).getYear()).distinct()
                .sorted(Comparator.reverseOrder()).toList();
        String filtro = FormatoVista.normalizar(busqueda);
        List<Oficio> seleccion = base.stream()
                .filter(o -> anio == null || fechaDe(o).getYear() == anio)
                .filter(o -> "CIRCULAR".equals(tipo) ? o.getNumeroCircular() != null
                        : !"OFICIO".equals(tipo) || o.getNumeroCircular() == null)
                .filter(o -> filtro.isEmpty() || FormatoVista.normalizar(o.getAsunto()).contains(filtro)
                        || FormatoVista.normalizar(o.getNumero()).contains(filtro)
                        || FormatoVista.normalizar(o.getNumeroCircular()).contains(filtro))
                .toList();

        Map<Long, Map<Long, LocalDateTime>> lecturas = lecturas(seleccion.stream().map(Oficio::getId).toList());
        List<ComunicadosVista.Oficio> vistas = new ArrayList<>(seleccion.size());
        long sinLeer = 0;
        for (Oficio o : seleccion) {
            Map<Long, LocalDateTime> deEste = lecturas.getOrDefault(o.getId(), Map.of());
            ComunicadosVista.Lectura lectura;
            boolean nuevo;
            if (esPersona) {
                LocalDateTime fecha = deEste.get(sujeto.getId());
                lectura = new ComunicadosVista.Lectura(false, fecha == null ? 0 : 1, 1,
                        fecha == null ? null : dia(fecha) + " · " + fecha.format(HORA));
                nuevo = fecha == null;
                if (nuevo) {
                    sinLeer++;
                }
            } else {
                lectura = lecturaGrupo(o, direccionId, cache, deEste);
                nuevo = alcanza(o, viewer) && !deEste.containsKey(viewerId);
                if (lectura.pendientes() > 0) {
                    sinLeer++;
                }
            }
            vistas.add(vista(o, nuevo, lectura));
        }
        return new ComunicadosVista.Panel(clave, titulo, subtitulo, iniciales, claseRol, !esPersona, !supervision,
                sinLeer, vistas, anios, anio, tipo == null ? "" : tipo, busqueda == null ? "" : busqueda);
    }

    private ComunicadosVista.Oficio vista(Oficio o, boolean nuevo, ComunicadosVista.Lectura lectura) {
        LocalDateTime emision = o.getFechaEmision();
        String cuando = emision != null ? dia(emision) + " · " + emision.format(HORA) : dia(o.getFecha());
        String redactor = o.getRedactadoPor() == null ? "Dirección"
                : o.getRedactadoPor().getNombre() + " · " + RolVista.etiqueta(RolVista.principal(o.getRedactadoPor()));
        NubeNodo nodo = o.getNubeNodo();
        ComunicadosVista.Archivo archivo = nodo == null ? null
                : new ComunicadosVista.Archivo(nodo.getNombre(), FormatoVista.tamano(nodo.getTamanoBytes()),
                        nodo.getExtension() != null && nodo.getExtension().toLowerCase(ES).startsWith("doc"));
        return new ComunicadosVista.Oficio(o.getId(), "OF-" + o.getNumero(), o.getNumeroCircular(), o.getAsunto(),
                redactor, cuando, FormatoVista.capitalizar(fechaDe(o).format(MES)), nuevo, archivo, lectura);
    }

    /** "13 oct 2026": el formato de fecha en español trae un punto tras el mes abreviado, el diseño no. */
    private static String dia(TemporalAccessor fecha) {
        return DIA.format(fecha).replace(".", "");
    }

    private LocalDate fechaDe(Oficio o) {
        return o.getFechaEmision() != null ? o.getFechaEmision().toLocalDate() : o.getFecha();
    }

    /** Cuántos oficios tiene esta persona sin leer (para destacar la pestaña o el aviso). */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long sinLeer(Long direccionId, Long usuarioId) {
        Usuario usuario = usuario(usuarioId);
        List<Oficio> reciben = oficioRepository.emitidosConDestinatarios(direccionId).stream()
                .filter(o -> alcanza(o, usuario)).toList();
        Map<Long, Map<Long, LocalDateTime>> lecturas = lecturas(reciben.stream().map(Oficio::getId).toList());
        return reciben.stream().filter(o -> !lecturas.getOrDefault(o.getId(), Map.of()).containsKey(usuarioId)).count();
    }

    // ── Documento ─────────────────────────────────────────────────────────────

    /**
     * Entrega el documento y, si el oficio le llega a quien lo pide, deja constancia de que lo leyó.
     * Administración y dirección pueden abrir cualquier oficio emitido; el resto, solo los suyos.
     */
    @Transactional(rollbackFor = Exception.class)
    public ComunicadosVista.Documento documento(Long oficioId, Long direccionId, Long viewerId, boolean supervision,
            boolean descargar) {
        Oficio oficio = oficioRepository.emitidoConDestinatarios(oficioId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Oficio no encontrado"));
        Usuario viewer = usuario(viewerId);
        boolean recibe = alcanza(oficio, viewer);
        if (!supervision && !recibe) {
            throw new IllegalArgumentException("Oficio no encontrado");
        }
        NubeNodo nodo = oficio.getNubeNodo();
        if (nodo == null || nodo.getUrlArchivo() == null) {
            throw new IllegalArgumentException("El oficio no tiene documento");
        }
        Path raiz = Paths.get(nubeNodoService.getRutaRecursos()).toAbsolutePath().normalize();
        Path ruta = raiz.resolve(nodo.getUrlArchivo()).normalize();
        if (!ruta.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta de archivo inválida");
        }
        if (recibe) {
            registrarLectura(oficio, viewer);
        }
        boolean pdf = nodo.getExtension() != null && "pdf".equalsIgnoreCase(nodo.getExtension());
        return new ComunicadosVista.Documento(ruta, nodo.getNombre(),
                pdf ? "application/pdf" : "application/octet-stream", pdf && !descargar);
    }

    // ── Emisión ───────────────────────────────────────────────────────────────

    /** Avisa a cada destinatario interno (campanita) de que el oficio ya está disponible en Comunicados. */
    @Transactional(rollbackFor = Exception.class)
    public int publicar(Oficio oficio) {
        if (oficio.getDestinatarios().isEmpty()) {
            return 0;
        }
        Map<Long, Usuario> audiencia = audienciaDe(oficio, oficio.getDireccion().getId(), new HashMap<>());
        if (oficio.getRedactadoPor() != null) {
            audiencia.remove(oficio.getRedactadoPor().getId());
        }
        String mensaje = "Nuevo oficio OF-" + oficio.getNumero() + ": " + oficio.getAsunto();
        if (mensaje.length() > MAX_MENSAJE_NOTIFICACION) {
            mensaje = mensaje.substring(0, MAX_MENSAJE_NOTIFICACION - 1).stripTrailing() + "…";
        }
        notificacionService.crearTodas(new ArrayList<>(audiencia.values()), mensaje, enlace(oficio.getId()));
        return audiencia.size();
    }

    /** Resumen de los destinatarios para la tabla de oficios ("Toda la comunidad, Familias de 8 - 2"). */
    public String resumen(Oficio oficio) {
        return new TreeSet<>(oficio.getDestinatarios().stream().map(OficioDestinatario::etiqueta)
                .collect(Collectors.toList())).stream().collect(Collectors.joining(", "));
    }

    // ── Apoyo ─────────────────────────────────────────────────────────────────

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private Long parseId(String clave, String prefijo) {
        try {
            return Long.parseLong(clave.substring(prefijo.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Destinatario no válido");
        }
    }

    private boolean enDireccion(Usuario usuario, Long direccionId) {
        return usuario.getDirecciones().stream().anyMatch(d -> d.getId().equals(direccionId));
    }
}
