package com.chavescr.nexa.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.chavescr.nexa.dto.ContextoClase;
import com.chavescr.nexa.dto.FilaEntrega;
import com.chavescr.nexa.dto.FilaNota;
import com.chavescr.nexa.dto.RolClase;
import com.chavescr.nexa.dto.TarjetaClase;
import com.chavescr.nexa.entity.ClassroomAdjunto;
import com.chavescr.nexa.entity.ClassroomClase;
import com.chavescr.nexa.entity.ClassroomComentario;
import com.chavescr.nexa.entity.ClassroomEntrega;
import com.chavescr.nexa.entity.ClassroomPublicacion;
import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;
import com.chavescr.nexa.entity.EstadoEntrega;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.TipoPublicacion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.ClassroomAdjuntoRepository;
import com.chavescr.nexa.repository.ClassroomClaseRepository;
import com.chavescr.nexa.repository.ClassroomComentarioRepository;
import com.chavescr.nexa.repository.ClassroomEntregaRepository;
import com.chavescr.nexa.repository.ClassroomPublicacionRepository;
import com.chavescr.nexa.repository.ComponenteRepository;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * Classroom: las clases salen del horario (docente + materia + sección) y sus miembros se derivan
 * de él y de la sección de cada estudiante, así que no hay inscripciones que mantener.
 * Las tareas calificables se apoyan en {@link ComponenteService}: crean su componente de
 * evaluación y registran las notas ahí, para que lleguen a Gestión Académica y a los promedios.
 */
@Service
@Transactional
public class ClassroomService {

    private static final Set<String> ROLES_SUPERVISION = Set.of("ROLE_ADMIN", "ROLE_DIRECTOR");
    private static final int MAX_ADJUNTOS = 10;

    private final ClassroomClaseRepository claseRepository;
    private final ClassroomPublicacionRepository publicacionRepository;
    private final ClassroomEntregaRepository entregaRepository;
    private final ClassroomComentarioRepository comentarioRepository;
    private final ClassroomAdjuntoRepository adjuntoRepository;
    private final HorarioLeccionRepository horarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComponenteRepository componenteRepository;
    private final ComponenteService componenteService;
    private final AlmacenamientoService almacenamientoService;

    public ClassroomService(ClassroomClaseRepository claseRepository,
            ClassroomPublicacionRepository publicacionRepository,
            ClassroomEntregaRepository entregaRepository,
            ClassroomComentarioRepository comentarioRepository,
            ClassroomAdjuntoRepository adjuntoRepository,
            HorarioLeccionRepository horarioRepository,
            UsuarioRepository usuarioRepository,
            ComponenteRepository componenteRepository,
            ComponenteService componenteService,
            AlmacenamientoService almacenamientoService) {
        this.claseRepository = claseRepository;
        this.publicacionRepository = publicacionRepository;
        this.entregaRepository = entregaRepository;
        this.comentarioRepository = comentarioRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.horarioRepository = horarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.componenteRepository = componenteRepository;
        this.componenteService = componenteService;
        this.almacenamientoService = almacenamientoService;
    }

    // ── Clases ────────────────────────────────────────────────────────────────

    // Escritura: materializa la primera vez las clases del horario que aún no existen.
    @Transactional(rollbackFor = Exception.class)
    public List<TarjetaClase> listarClases(Long direccionId, Long usuarioId, Set<String> roles) {
        Usuario usuario = usuario(usuarioId);
        Map<Long, Integer> estudiantesPorNivel = new HashMap<>();
        List<TarjetaClase> tarjetas = new ArrayList<>();

        for (Grupo grupo : gruposHorario(direccionId)) {
            RolClase rol = rolEn(grupo.nivel().getId(), grupo.docentes(), usuario, roles);
            if (rol == null) {
                continue;
            }
            ClassroomClase clase = asegurarClase(direccionId, grupo);
            int totalEstudiantes = estudiantesPorNivel.computeIfAbsent(grupo.nivel().getId(),
                    nivelId -> usuarioRepository.findEstudiantesActivosByNivelId(nivelId).size());
            List<ClassroomPublicacion> proximas = rol.isEstudiante() ? proximasSinEntregar(clase, usuarioId) : List.of();
            long porRevisar = rol.isDocente()
                    ? entregaRepository.countByPublicacionClaseIdAndEstado(clase.getId(), EstadoEntrega.ENTREGADA)
                    : 0;
            tarjetas.add(new TarjetaClase(clase, rol, nombres(grupo.docentes()), totalEstudiantes, proximas,
                    porRevisar));
        }
        return tarjetas;
    }

    /** Abre una clase validando que el usuario pertenezca a ella; si no, AccessDeniedException. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ContextoClase contexto(Long direccionId, Long claseId, Long usuarioId, Set<String> roles) {
        ClassroomClase clase = claseRepository.findByIdAndDireccionId(claseId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada"));
        Usuario usuario = usuario(usuarioId);
        List<Usuario> docentes = gruposHorario(direccionId).stream()
                .filter(g -> g.materia().getId().equals(clase.getMateria().getId())
                        && g.nivel().getId().equals(clase.getNivel().getId()))
                .findFirst()
                .map(Grupo::docentes)
                .orElse(List.of());
        RolClase rol = rolEn(clase.getNivel().getId(), docentes, usuario, roles);
        if (rol == null) {
            throw new AccessDeniedException("No perteneces a esta clase");
        }
        return new ContextoClase(clase, rol, usuario, docentes);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> estudiantes(ContextoClase ctx) {
        return usuarioRepository.findEstudiantesActivosByNivelId(ctx.getClase().getNivel().getId());
    }

    // ── Publicaciones (tablón, tareas, materiales) ────────────────────────────

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ClassroomPublicacion> tablon(ContextoClase ctx) {
        return publicacionRepository.findByClaseIdOrderByFechaCreacionDesc(ctx.getClase().getId());
    }

    /** Tareas o materiales agrupados por tema: primero los sin tema (clave ""), luego por nombre. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<String, List<ClassroomPublicacion>> porTema(ContextoClase ctx, TipoPublicacion tipo) {
        List<ClassroomPublicacion> lista = publicacionRepository
                .findByClaseIdAndTipoOrderByFechaCreacionDesc(ctx.getClase().getId(), tipo);
        Map<String, List<ClassroomPublicacion>> grupos = new LinkedHashMap<>();
        List<ClassroomPublicacion> sinTema = lista.stream().filter(p -> p.getTema() == null).toList();
        if (!sinTema.isEmpty()) {
            grupos.put("", sinTema);
        }
        lista.stream()
                .filter(p -> p.getTema() != null)
                .collect(Collectors.groupingBy(ClassroomPublicacion::getTema, () -> new java.util.TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER), Collectors.toList()))
                .forEach(grupos::put);
        return grupos;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Set<String> temas(ContextoClase ctx) {
        Set<String> temas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ClassroomPublicacion p : publicacionRepository.findByClaseIdOrderByFechaCreacionDesc(ctx.getClase().getId())) {
            if (p.getTema() != null) {
                temas.add(p.getTema());
            }
        }
        return temas;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ClassroomPublicacion publicacion(ContextoClase ctx, Long publicacionId) {
        return publicacionRepository.findByIdAndClaseId(publicacionId, ctx.getClase().getId())
                .orElseThrow(() -> new IllegalArgumentException("Publicación no encontrada"));
    }

    /** Porcentaje fijo del componente vinculado a la tarea (null = ponderado automático). */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Integer porcentajeComponente(ContextoClase ctx, ClassroomPublicacion tarea) {
        if (tarea.getComponenteId() == null) {
            return null;
        }
        return componenteRepository.findByIdAndDireccionId(tarea.getComponenteId(), ctx.getDireccionId())
                .map(Componente::getPorcentaje)
                .orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassroomPublicacion guardarPublicacion(ContextoClase ctx, Long publicacionId, TipoPublicacion tipo,
            DatosPublicacion datos) throws IOException {
        exigirDocente(ctx);
        ClassroomPublicacion pub;
        if (publicacionId != null) {
            pub = publicacion(ctx, publicacionId);
            pub.setFechaEdicion(LocalDateTime.now());
        } else {
            if (tipo == null) {
                throw new IllegalArgumentException("Tipo de publicación no válido");
            }
            pub = new ClassroomPublicacion();
            pub.setClase(ctx.getClase());
            pub.setTipo(tipo);
            pub.setAutor(ctx.getUsuario());
        }

        String titulo = texto(datos.titulo(), 200, "El título");
        String contenido = texto(datos.contenido(), 5000, "El contenido");
        List<MultipartFile> archivos = archivosValidos(datos.archivos());
        List<String> enlaces = enlaces(datos.enlaces());
        List<Long> quitar = datos.quitarAdjuntos() != null ? datos.quitarAdjuntos() : List.of();
        long adjuntosFinales = pub.getAdjuntos().stream().filter(a -> !quitar.contains(a.getId())).count()
                + archivos.size() + enlaces.size();
        if (adjuntosFinales > MAX_ADJUNTOS) {
            throw new IllegalArgumentException("Máximo " + MAX_ADJUNTOS + " adjuntos por publicación");
        }

        if (pub.getTipo() == TipoPublicacion.ANUNCIO) {
            if (contenido == null && adjuntosFinales == 0) {
                throw new IllegalArgumentException("Escribe algo o adjunta un archivo para publicar el anuncio");
            }
            pub.setTitulo(null);
        } else {
            if (titulo == null) {
                throw new IllegalArgumentException("El título es obligatorio");
            }
            pub.setTitulo(titulo);
            pub.setTema(texto(datos.tema(), 100, "El tema"));
        }
        pub.setContenido(contenido);

        if (pub.getTipo() == TipoPublicacion.TAREA) {
            if (datos.puntosTotales() != null && datos.puntosTotales() < 1) {
                throw new IllegalArgumentException("Los puntos deben ser mayores a cero");
            }
            pub.setFechaEntrega(datos.fechaEntrega());
            pub.setPuntosTotales(datos.puntosTotales());
        }
        publicacionRepository.save(pub);

        if (pub.getTipo() == TipoPublicacion.TAREA) {
            sincronizarComponente(ctx, pub, datos.claveComponente(), datos.porcentaje());
        }

        List<ClassroomAdjunto> quitados = pub.getAdjuntos().stream().filter(a -> quitar.contains(a.getId())).toList();
        pub.getAdjuntos().removeAll(quitados);
        eliminarArchivosAlConfirmar(quitados);
        agregarAdjuntos(ctx.getClase(), pub.getAdjuntos(), a -> a.setPublicacion(pub), archivos, enlaces);
        return publicacionRepository.save(pub);
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoPublicacion eliminarPublicacion(ContextoClase ctx, Long publicacionId) {
        exigirDocente(ctx);
        ClassroomPublicacion pub = publicacion(ctx, publicacionId);
        if (componenteVigente(ctx, pub)) {
            // Lanza si el componente ya tiene notas: no se borran calificaciones en silencio
            componenteService.eliminar(ctx.getDireccionId(), pub.getComponenteId());
        }
        List<ClassroomAdjunto> adjuntos = new ArrayList<>(pub.getAdjuntos());
        pub.getEntregas().forEach(e -> adjuntos.addAll(e.getAdjuntos()));
        publicacionRepository.delete(pub);
        eliminarArchivosAlConfirmar(adjuntos);
        return pub.getTipo();
    }

    // ── Comentarios ───────────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public ClassroomPublicacion comentar(ContextoClase ctx, Long publicacionId, String contenido) {
        if (ctx.getRol().isSupervisor()) {
            throw new AccessDeniedException("La supervisión de la clase es de solo lectura");
        }
        ClassroomPublicacion pub = publicacion(ctx, publicacionId);
        String texto = texto(contenido, 2000, "El comentario");
        if (texto == null) {
            throw new IllegalArgumentException("Escribe un comentario");
        }
        ClassroomComentario comentario = new ClassroomComentario();
        comentario.setPublicacion(pub);
        comentario.setAutor(ctx.getUsuario());
        comentario.setContenido(texto);
        pub.getComentarios().add(comentario);
        return publicacionRepository.save(pub);
    }

    @Transactional(rollbackFor = Exception.class)
    public ClassroomPublicacion eliminarComentario(ContextoClase ctx, Long comentarioId) {
        ClassroomComentario comentario = comentarioRepository.findById(comentarioId)
                .filter(c -> c.getPublicacion().getClase().getId().equals(ctx.getClase().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado"));
        boolean esAutor = comentario.getAutor().getId().equals(ctx.getUsuario().getId());
        if (!esAutor && !ctx.getRol().isDocente()) {
            throw new AccessDeniedException("Solo el autor o el docente pueden eliminar este comentario");
        }
        ClassroomPublicacion pub = comentario.getPublicacion();
        pub.getComentarios().remove(comentario);
        return publicacionRepository.save(pub);
    }

    // ── Tareas y entregas ─────────────────────────────────────────────────────

    /** Tarea de la clase. Si su componente se eliminó desde Gestión Académica, deja de ser calificable. */
    @Transactional(rollbackFor = Exception.class)
    public ClassroomPublicacion tarea(ContextoClase ctx, Long tareaId) {
        ClassroomPublicacion tarea = publicacion(ctx, tareaId);
        if (tarea.getTipo() != TipoPublicacion.TAREA) {
            throw new IllegalArgumentException("La publicación no es una tarea");
        }
        if (tarea.getComponenteId() != null && !componenteVigente(ctx, tarea)) {
            tarea.setComponenteId(null);
            tarea.setClaveComponente(null);
            publicacionRepository.save(tarea);
        }
        return tarea;
    }

    /** Vista de revisión (docente o supervisión): todos los estudiantes de la sección con su entrega y nota. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<FilaEntrega> filasEntrega(ContextoClase ctx, ClassroomPublicacion tarea) {
        Map<Long, ClassroomEntrega> entregas = entregaRepository.findByPublicacionId(tarea.getId()).stream()
                .collect(Collectors.toMap(e -> e.getEstudiante().getId(), Function.identity()));
        Map<Long, FilaNota> notas = notas(ctx, tarea);
        return estudiantes(ctx).stream()
                .map(est -> {
                    FilaNota nota = notas.get(est.getId());
                    return new FilaEntrega(est, entregas.get(est.getId()),
                            nota != null ? nota.getPuntosObtenidos() : null,
                            nota != null ? nota.getCalificacion() : null);
                })
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ClassroomPublicacion> proximasEntregas(ContextoClase ctx) {
        List<ClassroomPublicacion> proximas = ctx.getRol().isEstudiante()
                ? proximasSinEntregar(ctx.getClase(), ctx.getUsuario().getId())
                : publicacionRepository.findByClaseIdAndTipoAndFechaEntregaAfterOrderByFechaEntregaAsc(
                        ctx.getClase().getId(), TipoPublicacion.TAREA, LocalDateTime.now());
        return proximas.stream().limit(5).toList();
    }

    /** Entregas del estudiante actual en la clase, por id de tarea. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, ClassroomEntrega> entregasPropias(ContextoClase ctx) {
        return entregaRepository.findByPublicacionClaseIdAndEstudianteId(ctx.getClase().getId(), ctx.getUsuario().getId())
                .stream()
                .collect(Collectors.toMap(e -> e.getPublicacion().getId(), Function.identity()));
    }

    /** Cantidad de entregas recibidas (sin revisar) por tarea, para la lista del docente. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, Long> entregasPorRevisar(ContextoClase ctx, Map<String, List<ClassroomPublicacion>> grupos) {
        Map<Long, Long> conteo = new HashMap<>();
        grupos.values().forEach(lista -> lista.forEach(tarea -> conteo.put(tarea.getId(),
                tarea.getEntregas().stream().filter(e -> e.getEstado() == EstadoEntrega.ENTREGADA).count())));
        return conteo;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ClassroomEntrega entregaPropia(ContextoClase ctx, ClassroomPublicacion tarea) {
        return entregaRepository.findByPublicacionIdAndEstudianteId(tarea.getId(), ctx.getUsuario().getId())
                .orElse(null);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public FilaNota notaPropia(ContextoClase ctx, ClassroomPublicacion tarea) {
        return notas(ctx, tarea).get(ctx.getUsuario().getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void entregar(ContextoClase ctx, Long tareaId, String texto, List<MultipartFile> archivos)
            throws IOException {
        exigirEstudiante(ctx);
        ClassroomPublicacion tarea = tarea(ctx, tareaId);
        ClassroomEntrega entrega = entregaRepository
                .findByPublicacionIdAndEstudianteId(tarea.getId(), ctx.getUsuario().getId())
                .orElseGet(() -> nuevaEntrega(tarea, ctx.getUsuario()));
        if (entrega.getEstado() == EstadoEntrega.DEVUELTA) {
            throw new IllegalArgumentException("El docente ya revisó esta tarea");
        }
        if (entrega.getEstado() == EstadoEntrega.ENTREGADA) {
            throw new IllegalArgumentException("Ya entregaste esta tarea. Anula la entrega para modificarla.");
        }
        String respuesta = texto(texto, 5000, "La respuesta");
        List<MultipartFile> nuevos = archivosValidos(archivos);
        if (entrega.getAdjuntos().size() + nuevos.size() > MAX_ADJUNTOS) {
            throw new IllegalArgumentException("Máximo " + MAX_ADJUNTOS + " archivos por entrega");
        }
        if (respuesta == null && entrega.getAdjuntos().isEmpty() && nuevos.isEmpty()) {
            throw new IllegalArgumentException("Adjunta un archivo o escribe tu respuesta antes de entregar");
        }
        entrega.setTexto(respuesta);
        entrega.setEstado(EstadoEntrega.ENTREGADA);
        entrega.setFechaEntrega(LocalDateTime.now());
        entregaRepository.save(entrega);
        agregarAdjuntos(ctx.getClase(), entrega.getAdjuntos(), a -> a.setEntrega(entrega), nuevos, List.of());
        entregaRepository.save(entrega);
    }

    @Transactional(rollbackFor = Exception.class)
    public void anularEntrega(ContextoClase ctx, Long tareaId) {
        exigirEstudiante(ctx);
        ClassroomEntrega entrega = entregaRepository.findByPublicacionIdAndEstudianteId(tareaId, ctx.getUsuario().getId())
                .filter(e -> e.getPublicacion().getClase().getId().equals(ctx.getClase().getId()))
                .orElseThrow(() -> new IllegalArgumentException("No hay entrega que anular"));
        if (entrega.getEstado() != EstadoEntrega.ENTREGADA) {
            throw new IllegalArgumentException("Solo se puede anular una entrega que aún no ha sido revisada");
        }
        entrega.setEstado(EstadoEntrega.ASIGNADA);
        entrega.setFechaEntrega(null);
        entregaRepository.save(entrega);
    }

    /** Quita un archivo de la entrega propia mientras aún no está entregada. Devuelve la tarea. */
    @Transactional(rollbackFor = Exception.class)
    public Long quitarArchivoEntrega(ContextoClase ctx, Long adjuntoId) {
        exigirEstudiante(ctx);
        ClassroomAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .filter(a -> a.getEntrega() != null)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));
        ClassroomEntrega entrega = adjunto.getEntrega();
        if (!entrega.getEstudiante().getId().equals(ctx.getUsuario().getId())
                || !entrega.getPublicacion().getClase().getId().equals(ctx.getClase().getId())) {
            throw new AccessDeniedException("El archivo no es tuyo");
        }
        if (entrega.getEstado() != EstadoEntrega.ASIGNADA) {
            throw new IllegalArgumentException("Anula la entrega antes de quitar archivos");
        }
        entrega.getAdjuntos().remove(adjunto);
        entregaRepository.save(entrega);
        eliminarArchivosAlConfirmar(List.of(adjunto));
        return entrega.getPublicacion().getId();
    }

    /**
     * Califica (si la tarea es calificable) y devuelve el trabajo al estudiante. La nota se guarda
     * en el componente vinculado, igual que si se calificara desde Gestión Académica.
     */
    @Transactional(rollbackFor = Exception.class)
    public void calificar(ContextoClase ctx, Long tareaId, Long estudianteId, Integer puntos, String comentario) {
        exigirDocente(ctx);
        ClassroomPublicacion tarea = tarea(ctx, tareaId);
        Usuario estudiante = estudiantes(ctx).stream()
                .filter(e -> e.getId().equals(estudianteId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El estudiante no pertenece a esta clase"));
        String observacion = texto(comentario, 500, "El comentario");

        if (tarea.isCalificable()) {
            if (puntos == null) {
                throw new IllegalArgumentException("Indica los puntos obtenidos");
            }
            // "" en vez de null: registrarNota interpreta null como "conservar la observación anterior"
            componenteService.registrarNota(ctx.getDireccionId(), tarea.getComponenteId(), estudiante.getId(),
                    null, puntos, observacion != null ? observacion : "");
        }

        ClassroomEntrega entrega = entregaRepository.findByPublicacionIdAndEstudianteId(tarea.getId(), estudiante.getId())
                .orElseGet(() -> nuevaEntrega(tarea, estudiante));
        entrega.setEstado(EstadoEntrega.DEVUELTA);
        entrega.setFechaDevolucion(LocalDateTime.now());
        entrega.setComentarioDocente(observacion);
        entregaRepository.save(entrega);
    }

    // ── Adjuntos ──────────────────────────────────────────────────────────────

    /** Adjunto descargable por el usuario: debe pertenecer a la clase y, si es de una entrega, ser suya o docente. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ClassroomAdjunto adjuntoParaDescarga(Long direccionId, Long adjuntoId, Long usuarioId, Set<String> roles) {
        ClassroomAdjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .filter(a -> !a.isEnlace())
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));
        ClassroomClase clase = adjunto.getPublicacion() != null
                ? adjunto.getPublicacion().getClase()
                : adjunto.getEntrega().getPublicacion().getClase();
        ContextoClase ctx = contexto(direccionId, clase.getId(), usuarioId, roles);
        if (adjunto.getEntrega() != null && ctx.getRol().isEstudiante()
                && !adjunto.getEntrega().getEstudiante().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tienes acceso a este archivo");
        }
        return adjunto;
    }

    public Path rutaFisica(ClassroomAdjunto adjunto) {
        return almacenamientoService.resolver(adjunto.getRuta());
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private record Grupo(Materia materia, NivelAcademico nivel, List<Usuario> docentes) {
    }

    /**
     * Grupos (materia + sección, con sus docentes) del horario del período activo. Si ese período
     * aún no tiene horario, se usa todo el horario de la dirección para no dejar el Classroom vacío.
     */
    private List<Grupo> gruposHorario(Long direccionId) {
        PeriodoAcademico periodo = componenteService.obtenerPeriodoActivoOpcional(direccionId);
        List<Object[]> filas = periodo != null
                ? horarioRepository.findGruposByDireccionIdAndPeriodoId(direccionId, periodo.getId())
                : List.of();
        if (filas.isEmpty()) {
            filas = horarioRepository.findGruposByDireccionId(direccionId);
        }
        Map<String, Grupo> grupos = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            Materia materia = (Materia) fila[0];
            NivelAcademico nivel = (NivelAcademico) fila[1];
            Usuario docente = (Usuario) fila[2];
            if (!Boolean.TRUE.equals(materia.getActivo()) || !Boolean.TRUE.equals(nivel.getActivo())) {
                continue;
            }
            List<Usuario> docentes = grupos.computeIfAbsent(materia.getId() + "-" + nivel.getId(),
                    k -> new Grupo(materia, nivel, new ArrayList<>())).docentes();
            if (docentes.stream().noneMatch(d -> d.getId().equals(docente.getId()))) {
                docentes.add(docente);
            }
        }
        return grupos.values().stream()
                .sorted(Comparator.comparing((Grupo g) -> g.nivel().getGrado())
                        .thenComparing(g -> g.nivel().getSeccion())
                        .thenComparing(g -> g.materia().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // El que imparte la clase es DOCENTE aunque además sea admin; la supervisión es para quien no la imparte.
    private RolClase rolEn(Long nivelId, List<Usuario> docentes, Usuario usuario, Set<String> roles) {
        if (docentes.stream().anyMatch(d -> d.getId().equals(usuario.getId()))) {
            return RolClase.DOCENTE;
        }
        if (roles.stream().anyMatch(ROLES_SUPERVISION::contains)) {
            return RolClase.SUPERVISOR;
        }
        if (roles.contains("ROLE_ESTUDIANTE") && Boolean.TRUE.equals(usuario.getActivo())
                && usuario.getNivelAcademico() != null && usuario.getNivelAcademico().getId().equals(nivelId)) {
            return RolClase.ESTUDIANTE;
        }
        return null;
    }

    private ClassroomClase asegurarClase(Long direccionId, Grupo grupo) {
        return claseRepository.findByDireccionIdAndMateriaIdAndNivelId(direccionId, grupo.materia().getId(),
                grupo.nivel().getId()).orElseGet(() -> {
                    ClassroomClase clase = new ClassroomClase();
                    clase.setDireccion(grupo.nivel().getDireccion());
                    clase.setMateria(grupo.materia());
                    clase.setNivel(grupo.nivel());
                    return claseRepository.save(clase);
                });
    }

    private List<ClassroomPublicacion> proximasSinEntregar(ClassroomClase clase, Long estudianteId) {
        Set<Long> entregadas = entregaRepository.findByPublicacionClaseIdAndEstudianteId(clase.getId(), estudianteId)
                .stream()
                .filter(e -> e.getEstado() != EstadoEntrega.ASIGNADA)
                .map(e -> e.getPublicacion().getId())
                .collect(Collectors.toSet());
        return publicacionRepository.findByClaseIdAndTipoAndFechaEntregaAfterOrderByFechaEntregaAsc(clase.getId(),
                TipoPublicacion.TAREA, LocalDateTime.now()).stream()
                .filter(p -> !entregadas.contains(p.getId()))
                .limit(3)
                .toList();
    }

    /**
     * Crea, actualiza o quita el componente de evaluación de una tarea según el rubro elegido.
     * Cambiar de rubro o dejar de calificar solo se permite si aún no hay notas (lo valida
     * ComponenteService.eliminar), y los porcentajes pasan por las mismas reglas de Gestión Académica.
     */
    private void sincronizarComponente(ContextoClase ctx, ClassroomPublicacion tarea, ClaveComponente clave,
            Integer porcentaje) {
        Long direccionId = ctx.getDireccionId();
        Long existenteId = componenteVigente(ctx, tarea) ? tarea.getComponenteId() : null;

        if (clave == null) {
            if (existenteId != null) {
                componenteService.eliminar(direccionId, existenteId);
            }
            tarea.setComponenteId(null);
            tarea.setClaveComponente(null);
            return;
        }
        if (tarea.getFechaEntrega() == null) {
            throw new IllegalArgumentException("Una tarea calificable necesita fecha de entrega");
        }
        if (tarea.getPuntosTotales() == null) {
            throw new IllegalArgumentException("Una tarea calificable necesita puntos totales");
        }
        if (existenteId != null && tarea.getClaveComponente() != clave) {
            componenteService.eliminar(direccionId, existenteId);
            existenteId = null;
        }

        Componente datos = new Componente();
        datos.setId(existenteId);
        datos.setTitulo(tarea.getTitulo());
        String contenido = tarea.getContenido();
        datos.setDescripcion(contenido != null && contenido.length() > 1000 ? contenido.substring(0, 1000) : contenido);
        datos.setFecha(tarea.getFechaEntrega().toLocalDate());
        datos.setPuntosTotales(tarea.getPuntosTotales());
        datos.setPorcentaje(porcentaje);
        Componente guardado = componenteService.guardar(direccionId, clave, ctx.getClase().getNivel().getId(),
                ctx.getClase().getMateria().getId(), datos);
        tarea.setComponenteId(guardado.getId());
        tarea.setClaveComponente(clave);
        publicacionRepository.save(tarea);
    }

    private boolean componenteVigente(ContextoClase ctx, ClassroomPublicacion pub) {
        return pub.getComponenteId() != null
                && componenteRepository.findByIdAndDireccionId(pub.getComponenteId(), ctx.getDireccionId()).isPresent();
    }

    // Sin período activo no hay notas visibles (las de TAREA/COTIDIANO se leen del período activo).
    private Map<Long, FilaNota> notas(ContextoClase ctx, ClassroomPublicacion tarea) {
        if (!tarea.isCalificable()) {
            return Map.of();
        }
        try {
            return componenteService.listarNotas(ctx.getDireccionId(), tarea.getComponenteId()).stream()
                    .filter(n -> n.getCalificacion() != null)
                    .collect(Collectors.toMap(n -> n.getEstudiante().getId(), Function.identity()));
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    private ClassroomEntrega nuevaEntrega(ClassroomPublicacion tarea, Usuario estudiante) {
        ClassroomEntrega entrega = new ClassroomEntrega();
        entrega.setPublicacion(tarea);
        entrega.setEstudiante(estudiante);
        return entrega;
    }

    private void agregarAdjuntos(ClassroomClase clase, List<ClassroomAdjunto> destino,
            Consumer<ClassroomAdjunto> vincular, List<MultipartFile> archivos, List<String> enlaces)
            throws IOException {
        for (MultipartFile archivo : archivos) {
            String ruta = almacenamientoService.guardar(archivo, clase.getDireccion(), "classroom",
                    String.valueOf(clase.getId()));
            // Si la transacción se revierte, el archivo recién escrito no debe quedar huérfano en disco
            alRevertir(() -> almacenamientoService.eliminar(ruta));
            String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
            nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
            ClassroomAdjunto adjunto = new ClassroomAdjunto();
            adjunto.setNombre(nombre.length() > 255 ? nombre.substring(0, 255) : nombre);
            adjunto.setRuta(ruta);
            adjunto.setTamanoBytes(archivo.getSize());
            int punto = nombre.lastIndexOf('.');
            if (punto > 0 && nombre.length() - punto <= 20) {
                adjunto.setExtension(nombre.substring(punto + 1).toUpperCase());
            }
            vincular.accept(adjunto);
            destino.add(adjunto);
        }
        for (String url : enlaces) {
            ClassroomAdjunto adjunto = new ClassroomAdjunto();
            adjunto.setNombre(url.length() > 255 ? url.substring(0, 255) : url);
            adjunto.setUrl(url);
            vincular.accept(adjunto);
            destino.add(adjunto);
        }
    }

    private List<MultipartFile> archivosValidos(List<MultipartFile> archivos) {
        if (archivos == null) {
            return List.of();
        }
        List<MultipartFile> validos = archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
        if (validos.size() > MAX_ADJUNTOS) {
            throw new IllegalArgumentException("Máximo " + MAX_ADJUNTOS + " archivos a la vez");
        }
        return validos;
    }

    // Un enlace por línea. Se fuerza http(s) para que no se cuelen esquemas como javascript:.
    private List<String> enlaces(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        List<String> enlaces = new ArrayList<>();
        for (String linea : texto.split("\\R")) {
            String url = linea.trim();
            if (url.isEmpty()) {
                continue;
            }
            if (!url.matches("(?i)^https?://.*")) {
                url = "https://" + url;
            }
            try {
                URI uri = URI.create(url);
                if (uri.getHost() == null || url.length() > 1000) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Enlace no válido: " + linea.trim());
            }
            enlaces.add(url);
        }
        return enlaces;
    }

    private void eliminarArchivosAlConfirmar(List<ClassroomAdjunto> adjuntos) {
        List<String> rutas = adjuntos.stream().map(ClassroomAdjunto::getRuta).filter(r -> r != null).toList();
        if (rutas.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rutas.forEach(almacenamientoService::eliminar);
                }
            });
        } else {
            rutas.forEach(almacenamientoService::eliminar);
        }
    }

    private void alRevertir(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        accion.run();
                    }
                }
            });
        }
    }

    private void exigirDocente(ContextoClase ctx) {
        if (!ctx.getRol().isDocente()) {
            throw new AccessDeniedException("Solo el docente de la clase puede hacer esto");
        }
    }

    private void exigirEstudiante(ContextoClase ctx) {
        if (!ctx.getRol().isEstudiante()) {
            throw new AccessDeniedException("Solo los estudiantes de la clase pueden entregar tareas");
        }
    }

    private Usuario usuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private static String nombres(List<Usuario> usuarios) {
        return usuarios.stream().map(Usuario::getNombre).collect(Collectors.joining(", "));
    }

    private static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(campo + " no puede superar " + maximo + " caracteres");
        }
        return limpio;
    }

    /** Datos del formulario de publicación (anuncio, tarea o material). */
    public record DatosPublicacion(String titulo, String contenido, String tema, LocalDateTime fechaEntrega,
            Integer puntosTotales, ClaveComponente claveComponente, Integer porcentaje,
            List<MultipartFile> archivos, String enlaces, List<Long> quitarAdjuntos) {
    }
}
