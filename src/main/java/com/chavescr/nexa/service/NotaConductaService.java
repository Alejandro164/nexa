package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.FilaNotaConducta;
import com.chavescr.nexa.dto.PanelNotaConducta;
import com.chavescr.nexa.dto.ResumenNotaConducta;
import com.chavescr.nexa.entity.IncidenteConducta;
import com.chavescr.nexa.entity.IncidenteConducta.TipoIncidente;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.NotaConducta;
import com.chavescr.nexa.entity.Notificacion;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.IncidenteConductaRepository;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.NivelAcademicoRepository;
import com.chavescr.nexa.repository.NotaConductaRepository;
import com.chavescr.nexa.repository.PeriodoAcademicoRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class NotaConductaService {

    private static final int NOTA_INICIAL = 100;
    private static final String[] COLORES_AVATAR = {
            "#2d5a87", "#059669", "#0284c7", "#7c3aed",
            "#e11d48", "#ca8a04", "#db2777", "#0891b2"
    };

    private final UsuarioRepository usuarioRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final NivelAcademicoRepository nivelRepository;
    private final IncidenteConductaRepository incidenteRepository;
    private final NotaConductaRepository notaRepository;
    private final InstitucionRepository institucionRepository;
    private final DocenteGuiaService docenteGuiaService;
    private final AlcanceDocenteService alcanceDocenteService;
    private final NotificacionService notificacionService;

    public NotaConductaService(UsuarioRepository usuarioRepository,
            PeriodoAcademicoRepository periodoRepository,
            NivelAcademicoRepository nivelRepository,
            IncidenteConductaRepository incidenteRepository,
            NotaConductaRepository notaRepository,
            InstitucionRepository institucionRepository,
            DocenteGuiaService docenteGuiaService,
            AlcanceDocenteService alcanceDocenteService,
            NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.periodoRepository = periodoRepository;
        this.nivelRepository = nivelRepository;
        this.incidenteRepository = incidenteRepository;
        this.notaRepository = notaRepository;
        this.institucionRepository = institucionRepository;
        this.docenteGuiaService = docenteGuiaService;
        this.alcanceDocenteService = alcanceDocenteService;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PanelNotaConducta cargarPanel(Long institucionId, Long periodoId, Integer grado, Long nivelId,
            Long docenteId) {
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
            return new PanelNotaConducta(periodos, grados, List.of(), List.of(),
                    new ResumenNotaConducta(0, 0, 0, 0), null, grado, nivelId,
                    "Configure un período académico para registrar las notas de conducta.");
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

        List<Usuario> estudiantes = listarEstudiantes(institucionId, grado, nivelId, nivelesVisibles, docenteId != null);
        List<Long> estudianteIds = estudiantes.stream().map(Usuario::getId).toList();

        Map<Long, List<IncidenteConducta>> porEstudiante = agruparIncidentes(
                institucionId, periodo.getId(), estudianteIds);

        Set<Long> enviadas = estudianteIds.isEmpty()
                ? Set.of()
                : new HashSet<>(notaRepository.findEstudianteIdsEnviados(institucionId, periodo.getId(), estudianteIds));

        List<FilaNotaConducta> filas = new ArrayList<>(estudiantes.size());
        List<IncidenteConducta> incidentesFiltrados = new ArrayList<>();
        for (Usuario estudiante : estudiantes) {
            List<IncidenteConducta> delEstudiante = porEstudiante.getOrDefault(estudiante.getId(), List.of());
            incidentesFiltrados.addAll(delEstudiante);
            filas.add(construirFila(estudiante, periodo, delEstudiante, enviadas.contains(estudiante.getId())));
        }
        filas.sort(Comparator
                .comparing((FilaNotaConducta f) -> f.getEstudiante().getNivelAcademico() == null
                        ? Integer.MAX_VALUE
                        : f.getEstudiante().getNivelAcademico().getGrado())
                .thenComparing(f -> f.getEstudiante().getNivelAcademico() == null
                        ? ""
                        : f.getEstudiante().getNivelAcademico().getSeccion())
                .thenComparing(f -> f.getEstudiante().getNombre(), String.CASE_INSENSITIVE_ORDER));

        return new PanelNotaConducta(periodos, grados, secciones, filas,
                resumir(filas, incidentesFiltrados), periodo.getId(), grado, nivelId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public String enviar(Long institucionId, Long periodoId, Long estudianteId, Long docenteId) {
        if (periodoId == null) {
            throw new IllegalArgumentException("Seleccione un período académico.");
        }
        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(periodoId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        exigirAlcance(institucionId, docenteId, estudiante);

        List<IncidenteConducta> incidentes = incidenteRepository
                .findByInstitucionIdAndPeriodoIdAndEstudianteId(institucionId, periodoId, estudianteId);
        FilaNotaConducta fila = construirFila(estudiante, periodo, incidentes, false);

        List<Usuario> padres = usuarioRepository.findPadresByEstudianteId(estudianteId);
        if (padres.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay encargados vinculados a " + estudiante.getNombre() + ". Vincule un padre o madre primero.");
        }

        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
        Map<Long, NotaConducta> existentes = new HashMap<>();
        notaRepository.findByInstitucionIdAndPeriodoIdAndEstudianteId(institucionId, periodoId, estudianteId)
                .ifPresent(n -> existentes.put(estudianteId, n));
        notaRepository.save(prepararEnvio(institucion, periodo, estudiante, fila, existentes));
        notificacionService.crearTodas(padres, mensajePadres(estudiante, fila), "/portal-padres");
        return "Nota de " + estudiante.getNombre() + " enviada a " + padres.size()
                + (padres.size() == 1 ? " encargado." : " encargados.");
    }

    @Transactional(rollbackFor = Exception.class)
    public String enviarTodas(Long institucionId, Long periodoId, Integer grado, Long nivelId, Long docenteId) {
        PanelNotaConducta panel = cargarPanel(institucionId, periodoId, grado, nivelId, docenteId);
        if (panel.getAvisoPeriodo() != null) {
            throw new IllegalArgumentException(panel.getAvisoPeriodo());
        }
        if (panel.getFilas().isEmpty()) {
            throw new IllegalArgumentException("No hay estudiantes para enviar en el filtro actual.");
        }

        PeriodoAcademico periodo = periodoRepository.findByIdAndInstitucionId(panel.getPeriodoId(), institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Período no encontrado"));
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));

        List<Long> estudianteIds = panel.getFilas().stream().map(f -> f.getEstudiante().getId()).toList();
        Map<Long, List<Usuario>> padresPorEstudiante = agruparPadres(estudianteIds);
        Map<Long, NotaConducta> existentes = notasExistentes(institucionId, periodo.getId(), estudianteIds);

        List<NotaConducta> aGuardar = new ArrayList<>();
        List<Notificacion> notificaciones = new ArrayList<>();
        int enviados = 0;
        int sinEncargado = 0;
        for (FilaNotaConducta fila : panel.getFilas()) {
            List<Usuario> padres = padresPorEstudiante.getOrDefault(fila.getEstudiante().getId(), List.of());
            if (padres.isEmpty()) {
                sinEncargado++;
                continue;
            }
            aGuardar.add(prepararEnvio(institucion, periodo, fila.getEstudiante(), fila, existentes));
            String mensaje = mensajePadres(fila.getEstudiante(), fila);
            for (Usuario padre : padres) {
                notificaciones.add(notificacionService.nueva(padre, mensaje, "/portal-padres"));
            }
            enviados++;
        }
        if (!aGuardar.isEmpty()) {
            notaRepository.saveAll(aGuardar);
        }
        notificacionService.guardarTodas(notificaciones);

        if (enviados == 0) {
            throw new IllegalArgumentException(
                    "Ninguna nota se envió: los estudiantes del filtro no tienen encargados vinculados.");
        }
        String mensaje = "Se enviaron " + enviados
                + (enviados == 1 ? " nota de conducta." : " notas de conducta.");
        if (sinEncargado > 0) {
            mensaje += " " + sinEncargado
                    + (sinEncargado == 1 ? " estudiante no tiene" : " estudiantes no tienen")
                    + " encargado vinculado.";
        }
        return mensaje;
    }

    private NotaConducta prepararEnvio(Institucion institucion, PeriodoAcademico periodo, Usuario estudiante,
            FilaNotaConducta fila, Map<Long, NotaConducta> existentes) {
        NotaConducta nota = existentes.get(estudiante.getId());
        if (nota == null) {
            nota = new NotaConducta();
            nota.setInstitucion(institucion);
            nota.setPeriodo(periodo);
            nota.setEstudiante(estudiante);
            existentes.put(estudiante.getId(), nota);
        }
        nota.setNota(fila.getNota());
        nota.setObservaciones(fila.getObservaciones());
        nota.setEnviada(true);
        nota.setFechaEnvio(LocalDateTime.now());
        return nota;
    }

    private String mensajePadres(Usuario estudiante, FilaNotaConducta fila) {
        return "Nota de conducta de " + estudiante.getNombre() + ": " + fila.getNota()
                + " (" + fila.getCategoria() + ") — período " + fila.getPeriodoCodigo() + ".";
    }

    private Map<Long, List<Usuario>> agruparPadres(List<Long> estudianteIds) {
        if (estudianteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Usuario>> porEstudiante = new HashMap<>();
        for (Object[] fila : usuarioRepository.findPadresByEstudianteIds(estudianteIds)) {
            Long estudianteId = (Long) fila[0];
            Usuario padre = (Usuario) fila[1];
            porEstudiante.computeIfAbsent(estudianteId, id -> new ArrayList<>()).add(padre);
        }
        return porEstudiante;
    }

    private Map<Long, List<IncidenteConducta>> agruparIncidentes(Long institucionId, Long periodoId,
            List<Long> estudianteIds) {
        if (estudianteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<IncidenteConducta>> porEstudiante = new HashMap<>();
        for (Object[] fila : incidenteRepository.findDeEstudiantes(institucionId, periodoId, estudianteIds)) {
            porEstudiante.computeIfAbsent((Long) fila[0], id -> new ArrayList<>())
                    .add((IncidenteConducta) fila[1]);
        }
        return porEstudiante;
    }

    private Map<Long, NotaConducta> notasExistentes(Long institucionId, Long periodoId, List<Long> estudianteIds) {
        Map<Long, NotaConducta> existentes = new HashMap<>();
        if (estudianteIds.isEmpty()) {
            return existentes;
        }
        for (Object[] fila : notaRepository.findDeEstudiantes(institucionId, periodoId, estudianteIds)) {
            existentes.put((Long) fila[0], (NotaConducta) fila[1]);
        }
        return existentes;
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
            throw new IllegalArgumentException("No tiene permiso para enviar la nota de este estudiante.");
        }
    }

    private FilaNotaConducta construirFila(Usuario estudiante, PeriodoAcademico periodo,
            List<IncidenteConducta> incidentes, boolean enviada) {
        int descuento = incidentes.stream()
                .filter(i -> i.getTipo() != null && i.getTipo().afectaNota())
                .mapToInt(i -> i.getPuntosDescontados() != null ? i.getPuntosDescontados() : 0)
                .sum();
        int nota = Math.max(0, NOTA_INICIAL - descuento);
        String categoriaCss = categoriaCss(nota);
        long llamadas = contar(incidentes, TipoIncidente.LLAMADA_ATENCION);
        long boletas = contar(incidentes, TipoIncidente.BOLETA);

        String seccion = "Sin sección";
        if (estudiante.getNivelAcademico() != null) {
            NivelAcademico nivel = estudiante.getNivelAcademico();
            seccion = nivel.getGrado() + "-" + nivel.getSeccion();
        }

        return new FilaNotaConducta(estudiante, seccion, nota, etiquetaCategoria(categoriaCss), categoriaCss,
                observaciones(nota, llamadas, boletas), periodo.getCodigo(), enviada,
                iniciales(estudiante.getNombre()), colorAvatar(estudiante.getId()));
    }

    private ResumenNotaConducta resumir(List<FilaNotaConducta> filas, List<IncidenteConducta> incidentes) {
        int promedio = filas.isEmpty() ? 0
                : (int) Math.round(filas.stream().mapToInt(FilaNotaConducta::getNota).average().orElse(0));
        long llamadas = contar(incidentes, TipoIncidente.LLAMADA_ATENCION);
        long boletas = contar(incidentes, TipoIncidente.BOLETA);
        return new ResumenNotaConducta(promedio, llamadas, boletas, filas.size());
    }

    static String observaciones(int nota, long llamadas, long boletas) {
        if (boletas > 0) {
            return "Conducta deficiente. " + cantidad(boletas, "boleta", "boletas")
                    + " por falta grave. Debe mejorar urgentemente su comportamiento.";
        }
        if (llamadas >= 3) {
            return "Requiere mejorar su conducta. Ha recibido "
                    + cantidad(llamadas, "llamada de atención", "llamadas de atención") + " este período.";
        }
        if (llamadas > 0) {
            return "Generalmente cumple con las normas. Ha tenido "
                    + cantidad(llamadas, "llamado de atención", "llamados de atención") + ".";
        }
        if (nota >= 100) {
            return "Estudiante ejemplar. Nunca ha tenido llamadas de atención este período.";
        }
        if (nota >= 90) {
            return "Excelente comportamiento. Mantiene una actitud positiva en clase.";
        }
        return "Cumple las normas de convivencia del centro educativo.";
    }

    static String categoriaCss(int nota) {
        if (nota >= 90) {
            return "excelente";
        }
        if (nota >= 80) {
            return "bueno";
        }
        if (nota >= 65) {
            return "regular";
        }
        return "deficiente";
    }

    private static String etiquetaCategoria(String css) {
        return switch (css) {
            case "excelente" -> "Excelente";
            case "bueno" -> "Bueno";
            case "regular" -> "Regular";
            default -> "Deficiente";
        };
    }

    private static String cantidad(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static long contar(List<IncidenteConducta> incidentes, TipoIncidente tipo) {
        return incidentes.stream().filter(i -> i.getTipo() == tipo).count();
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

    static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    static String colorAvatar(Long id) {
        if (id == null) {
            return COLORES_AVATAR[0];
        }
        return COLORES_AVATAR[Math.floorMod(Long.hashCode(id), COLORES_AVATAR.length)];
    }
}
