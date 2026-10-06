package com.chavescr.nexa.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.VistaSubgrupos;
import com.chavescr.nexa.dto.VistaSubgrupos.Alumno;
import com.chavescr.nexa.dto.VistaSubgrupos.Franja;
import com.chavescr.nexa.dto.VistaSubgrupos.Opcion;
import com.chavescr.nexa.entity.DiaLaboral;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.SubgrupoEstudiante;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.HorarioLeccionRepository;
import com.chavescr.nexa.repository.SubgrupoEstudianteRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

/**
 * La sección ya relaciona al estudiante con el horario. Si una lección tiene varias materias,
 * aquí se guarda a cuál de esas lecciones asiste. La misma combinación, repetida en la semana,
 * comparte la elección.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SubgrupoService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ConfiguracionAcademicaService academica;
    private final HorarioLeccionRepository horarioRepository;
    private final SubgrupoEstudianteRepository asignacionRepository;
    private final UsuarioRepository usuarioRepository;

    public SubgrupoService(ConfiguracionAcademicaService academica, HorarioLeccionRepository horarioRepository,
            SubgrupoEstudianteRepository asignacionRepository, UsuarioRepository usuarioRepository) {
        this.academica = academica;
        this.horarioRepository = horarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public VistaSubgrupos consultar(Long direccionId, Long periodoId, Long nivelId, String abierta) {
        List<PeriodoAcademico> periodos = academica.listarPeriodosActivos(direccionId);
        List<NivelAcademico> niveles = academica.listarNivelesActivos(direccionId);
        Long periodo = elegir(periodos, periodoId, PeriodoAcademico::getId);
        Long nivel = elegir(niveles, nivelId, NivelAcademico::getId);
        if (periodo == null || nivel == null) {
            return new VistaSubgrupos(periodos, niveles, periodo, nivel, null, 0, List.of(), null, false);
        }
        NivelAcademico seccion = niveles.stream().filter(item -> item.getId().equals(nivel)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado"));
        List<String> dias = academica.obtenerConfiguracion(direccionId).getDias();
        List<Usuario> estudiantes = usuarioRepository.findEstudiantesActivosConNivel(direccionId, null, nivel);
        Map<Long, Set<Long>> leccionesPorEstudiante = leccionesPorEstudiante(direccionId, periodo, nivel);
        List<Franja> franjas = franjas(direccionId, periodo, nivel, dias, estudiantes, leccionesPorEstudiante);
        String abiertaValida = franjas.stream().anyMatch(franja -> franja.clave().equals(abierta)) ? abierta : null;
        return new VistaSubgrupos(periodos, niveles, periodo, nivel, seccion.getNombreCompleto(),
                estudiantes.size(), franjas, abiertaValida, true);
    }

    public VistaSubgrupos asignar(Long direccionId, Long periodoId, Long nivelId, Long estudianteId,
            Long leccionId, String bloque) {
        if (estudianteId == null || periodoId == null || nivelId == null) {
            throw new IllegalArgumentException("Faltan datos para guardar el subgrupo");
        }
        Usuario estudiante = usuarioRepository.findEstudianteActivoConNivel(estudianteId, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("El estudiante no pertenece a esta dirección"));
        if (estudiante.getNivelAcademico() == null || !nivelId.equals(estudiante.getNivelAcademico().getId())) {
            throw new IllegalArgumentException("El estudiante no pertenece a esta sección");
        }
        List<HorarioLeccion> lecciones = horarioRepository.findConsultaPorNivel(direccionId, periodoId, nivelId);
        if (leccionId == null) {
            quitar(estudianteId, lecciones, bloque);
            return consultar(direccionId, periodoId, nivelId, bloque);
        }
        HorarioLeccion elegida = lecciones.stream().filter(leccion -> leccion.getId().equals(leccionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Esa materia no está en el horario de la sección"));
        String clave = guardar(estudiante, elegida, lecciones);
        return consultar(direccionId, periodoId, nivelId, clave);
    }

    /** Horario del estudiante: en una lección partida solo ve la materia que le toca. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<HorarioLeccion> paraEstudiante(Long estudianteId, List<HorarioLeccion> lecciones) {
        if (lecciones.isEmpty()) {
            return lecciones;
        }
        Set<Long> asignadas = new HashSet<>(asignacionRepository.findLeccionIdsByEstudianteId(estudianteId));
        List<HorarioLeccion> visibles = new ArrayList<>();
        for (List<HorarioLeccion> slot : porSlot(lecciones).values()) {
            if (slot.size() < 2) {
                visibles.addAll(slot);
            } else {
                slot.stream().filter(leccion -> asignadas.contains(leccion.getId())).forEach(visibles::add);
            }
        }
        return visibles;
    }

    /**
     * Lista de la materia. Si esa materia solo existe dentro de lecciones partidas,
     * entran los estudiantes asignados a ella. Si también se da sola, entra toda la sección.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> deMateria(Long direccionId, Long periodoId, Long nivelId, Long materiaId,
            List<Usuario> seccion) {
        if (periodoId == null || materiaId == null || seccion.isEmpty()) {
            return seccion;
        }
        List<HorarioLeccion> lecciones = horarioRepository.findConsultaPorNivel(direccionId, periodoId, nivelId);
        Map<String, List<HorarioLeccion>> slots = porSlot(lecciones);
        List<HorarioLeccion> deLaMateria = lecciones.stream()
                .filter(leccion -> materiaId.equals(leccion.getMateria().getId()))
                .toList();
        if (deLaMateria.isEmpty()) {
            return seccion;
        }
        boolean soloEnSubgrupo = deLaMateria.stream().allMatch(leccion -> slots.get(slot(leccion)).size() >= 2);
        if (!soloEnSubgrupo) {
            return seccion;
        }
        Set<Long> alumnos = new HashSet<>(asignacionRepository.findEstudianteIdsByLeccionIdIn(
                deLaMateria.stream().map(HorarioLeccion::getId).toList()));
        return seccion.stream().filter(estudiante -> alumnos.contains(estudiante.getId())).toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public boolean asiste(Long estudianteId, Long direccionId, Long periodoId, Long nivelId, Long materiaId) {
        if (periodoId == null || materiaId == null) {
            return true;
        }
        List<HorarioLeccion> lecciones = horarioRepository.findConsultaPorNivel(direccionId, periodoId, nivelId);
        Map<String, List<HorarioLeccion>> slots = porSlot(lecciones);
        List<HorarioLeccion> deLaMateria = lecciones.stream()
                .filter(leccion -> materiaId.equals(leccion.getMateria().getId()))
                .toList();
        if (deLaMateria.isEmpty()) {
            return true;
        }
        boolean soloEnSubgrupo = deLaMateria.stream().allMatch(leccion -> slots.get(slot(leccion)).size() >= 2);
        if (!soloEnSubgrupo) {
            return true;
        }
        return asignacionRepository
                .existsByEstudiante_IdAndLeccion_Direccion_IdAndLeccion_Periodo_IdAndLeccion_Nivel_IdAndLeccion_Materia_Id(
                        estudianteId, direccionId, periodoId, nivelId, materiaId);
    }

    /** Pase de lista: la materia del subgrupo solo ve a sus estudiantes, también si el día no coincide con el bloque. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Usuario> paraAsistencia(Long direccionId, Long periodoId, Long nivelId, LocalDate fecha,
            Integer numeroLeccion, Long materiaId, List<Usuario> seccion) {
        if (periodoId == null || materiaId == null || seccion.isEmpty()) {
            return seccion;
        }
        List<HorarioLeccion> lecciones = horarioRepository.findConsultaPorNivel(direccionId, periodoId, nivelId);
        Map<String, List<HorarioLeccion>> slots = porSlot(lecciones);
        List<HorarioLeccion> deLaMateria = lecciones.stream()
                .filter(leccion -> materiaId.equals(leccion.getMateria().getId()))
                .toList();
        boolean exclusiva = !deLaMateria.isEmpty()
                && deLaMateria.stream().allMatch(leccion -> slots.get(slot(leccion)).size() >= 2);
        String dia = dia(fecha);
        List<HorarioLeccion> slotHoy = dia == null || numeroLeccion == null
                ? List.of()
                : slots.getOrDefault(numeroLeccion + "-" + dia, List.of());
        boolean horaPartida = slotHoy.size() >= 2
                && slotHoy.stream().anyMatch(leccion -> materiaId.equals(leccion.getMateria().getId()));
        if (!exclusiva && !horaPartida) {
            return seccion;
        }
        List<Long> leccionIds = (horaPartida ? slotHoy.stream()
                .filter(leccion -> materiaId.equals(leccion.getMateria().getId()))
                : deLaMateria.stream())
                .map(HorarioLeccion::getId)
                .toList();
        Set<Long> alumnos = new HashSet<>(asignacionRepository.findEstudianteIdsByLeccionIdIn(leccionIds));
        return seccion.stream().filter(estudiante -> alumnos.contains(estudiante.getId())).toList();
    }

    private String guardar(Usuario estudiante, HorarioLeccion elegida, List<HorarioLeccion> lecciones) {
        Map<String, List<List<HorarioLeccion>>> combinaciones = combinaciones(lecciones, List.of());
        String clave = combinaciones.entrySet().stream()
                .filter(entrada -> entrada.getValue().stream()
                        .anyMatch(slot -> slot.stream().anyMatch(leccion -> leccion.getId().equals(elegida.getId()))))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Esa lección no divide el grupo"));
        for (List<HorarioLeccion> slot : combinaciones.get(clave)) {
            reemplazar(estudiante, slot, elegida.getMateria().getId());
        }
        return clave;
    }

    private void quitar(Long estudianteId, List<HorarioLeccion> lecciones, String bloque) {
        List<List<HorarioLeccion>> slots = combinaciones(lecciones, List.of()).get(bloque);
        if (slots == null) {
            throw new IllegalArgumentException("Esa combinación de materias ya no está en el horario");
        }
        List<Long> ids = slots.stream().flatMap(Collection::stream).map(HorarioLeccion::getId).toList();
        if (!ids.isEmpty()) {
            asignacionRepository.deleteByEstudiante_IdAndLeccion_IdIn(estudianteId, ids);
        }
    }

    private void reemplazar(Usuario estudiante, List<HorarioLeccion> slot, Long materiaId) {
        List<Long> ids = slot.stream().map(HorarioLeccion::getId).toList();
        asignacionRepository.deleteByEstudiante_IdAndLeccion_IdIn(estudiante.getId(), ids);
        asignacionRepository.flush();
        HorarioLeccion destino = slot.stream().filter(leccion -> materiaId.equals(leccion.getMateria().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Esa materia no forma parte de esta lección"));
        SubgrupoEstudiante asignacion = new SubgrupoEstudiante();
        asignacion.setEstudiante(estudiante);
        asignacion.setLeccion(destino);
        asignacionRepository.save(asignacion);
    }

    private List<Franja> franjas(Long direccionId, Long periodoId, Long nivelId, List<String> dias,
            List<Usuario> estudiantes, Map<Long, Set<Long>> leccionesPorEstudiante) {
        List<HorarioLeccion> lecciones = horarioRepository.findConsultaPorNivel(direccionId, periodoId, nivelId);
        List<Franja> franjas = new ArrayList<>();
        for (Map.Entry<String, List<List<HorarioLeccion>>> combinacion : combinaciones(lecciones, dias).entrySet()) {
            franjas.add(franja(combinacion.getKey(), combinacion.getValue(), estudiantes, leccionesPorEstudiante));
        }
        return franjas;
    }

    private Franja franja(String clave, List<List<HorarioLeccion>> slots, List<Usuario> estudiantes,
            Map<Long, Set<Long>> leccionesPorEstudiante) {
        Map<Long, OpcionBuilder> porMateria = new LinkedHashMap<>();
        List<String> horarios = new ArrayList<>();
        for (List<HorarioLeccion> slot : slots) {
            horarios.add(horario(slot.get(0)));
            for (HorarioLeccion leccion : slot) {
                OpcionBuilder opcion = porMateria.computeIfAbsent(leccion.getMateria().getId(),
                        id -> new OpcionBuilder(leccion));
                if (leccion.getAula() != null) {
                    opcion.aulas.add(leccion.getAula().getNombre());
                }
                if (leccion.getDocente() != null) {
                    opcion.docentes.add(leccion.getDocente().getNombre());
                }
            }
        }
        List<OpcionBuilder> opciones = new ArrayList<>(porMateria.values());
        opciones.sort(Comparator.comparing(opcion -> opcion.nombre, String.CASE_INSENSITIVE_ORDER));
        Map<Long, Long> leccionDeMateria = new HashMap<>();
        List<Opcion> columnas = new ArrayList<>();
        Map<Long, List<Alumno>> alumnos = new HashMap<>();
        for (OpcionBuilder opcion : opciones) {
            leccionDeMateria.put(opcion.materiaId, opcion.leccionId);
            alumnos.put(opcion.leccionId, new ArrayList<>());
            columnas.add(new Opcion(opcion.leccionId, opcion.nombre, opcion.color,
                    unir(opcion.aulas) + " · " + unir(opcion.docentes), alumnos.get(opcion.leccionId)));
        }
        List<Alumno> pendientes = new ArrayList<>();
        for (Usuario estudiante : estudiantes) {
            Long leccionId = materiaElegida(estudiante.getId(), slots, leccionesPorEstudiante, leccionDeMateria);
            Alumno alumno = new Alumno(estudiante.getId(), estudiante.getNombre(), leccionId);
            if (leccionId == null) {
                pendientes.add(alumno);
            } else {
                alumnos.get(leccionId).add(alumno);
            }
        }
        return new Franja(clave, titulo(opciones), horarios, estudiantes.size(),
                estudiantes.size() - pendientes.size(), columnas, pendientes);
    }

    private static Long materiaElegida(Long estudianteId, List<List<HorarioLeccion>> slots,
            Map<Long, Set<Long>> leccionesPorEstudiante, Map<Long, Long> leccionDeMateria) {
        Set<Long> asignadas = leccionesPorEstudiante.getOrDefault(estudianteId, Set.of());
        for (List<HorarioLeccion> slot : slots) {
            for (HorarioLeccion leccion : slot) {
                if (asignadas.contains(leccion.getId())) {
                    return leccionDeMateria.get(leccion.getMateria().getId());
                }
            }
        }
        return null;
    }

    private Map<Long, Set<Long>> leccionesPorEstudiante(Long direccionId, Long periodoId, Long nivelId) {
        Map<Long, Set<Long>> mapa = new HashMap<>();
        for (Object[] fila : asignacionRepository.findEstudianteYLeccion(direccionId, periodoId, nivelId)) {
            mapa.computeIfAbsent((Long) fila[0], id -> new HashSet<>()).add((Long) fila[1]);
        }
        return mapa;
    }

    private Map<String, List<List<HorarioLeccion>>> combinaciones(List<HorarioLeccion> lecciones, List<String> dias) {
        List<List<HorarioLeccion>> partidas = new ArrayList<>(porSlot(lecciones).values());
        partidas.removeIf(slot -> slot.size() < 2);
        partidas.sort(Comparator
                .comparingInt((List<HorarioLeccion> slot) -> indice(slot.get(0).getDia(), dias))
                .thenComparingInt(slot -> slot.get(0).getNumeroLeccion()));
        Map<String, List<List<HorarioLeccion>>> grupos = new LinkedHashMap<>();
        for (List<HorarioLeccion> slot : partidas) {
            grupos.computeIfAbsent(claveDe(slot), clave -> new ArrayList<>()).add(slot);
        }
        return grupos;
    }

    private static Map<String, List<HorarioLeccion>> porSlot(List<HorarioLeccion> lecciones) {
        Map<String, List<HorarioLeccion>> slots = new LinkedHashMap<>();
        for (HorarioLeccion leccion : lecciones) {
            slots.computeIfAbsent(slot(leccion), clave -> new ArrayList<>()).add(leccion);
        }
        return slots;
    }

    private static String slot(HorarioLeccion leccion) {
        return leccion.getNumeroLeccion() + "-" + leccion.getDia();
    }

    private static String claveDe(List<HorarioLeccion> slot) {
        return slot.stream().map(leccion -> leccion.getMateria().getId()).sorted().map(String::valueOf)
                .collect(Collectors.joining("-"));
    }

    private static String titulo(List<OpcionBuilder> opciones) {
        List<String> nombres = opciones.stream().map(opcion -> opcion.nombre).toList();
        if (nombres.size() <= 1) {
            return nombres.isEmpty() ? "Subgrupo" : nombres.get(0);
        }
        if (nombres.size() == 2) {
            return nombres.get(0) + " y " + nombres.get(1);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    private static String horario(HorarioLeccion leccion) {
        return DiaLaboral.etiqueta(leccion.getDia()) + " · Lección " + leccion.getNumeroLeccion()
                + " · " + hora(leccion.getHoraInicio()) + " – " + hora(leccion.getHoraFin());
    }

    private static String hora(LocalTime hora) {
        return hora == null ? "--:--" : HORA.format(hora);
    }

    private static String unir(Set<String> valores) {
        String texto = valores.stream().filter(valor -> valor != null && !valor.isBlank())
                .collect(Collectors.joining(", "));
        return texto.isBlank() ? "—" : texto;
    }

    private static int indice(String dia, List<String> dias) {
        int posicion = dias.indexOf(dia);
        return posicion < 0 ? Integer.MAX_VALUE : posicion;
    }

    private static String dia(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        return switch (fecha.getDayOfWeek()) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            default -> null;
        };
    }

    private static <T> Long elegir(List<T> opciones, Long id, Function<T, Long> identificador) {
        if (opciones.isEmpty()) {
            return null;
        }
        if (id != null && opciones.stream().anyMatch(opcion -> id.equals(identificador.apply(opcion)))) {
            return id;
        }
        return identificador.apply(opciones.get(0));
    }

    private static final class OpcionBuilder {
        private final Long materiaId;
        private final Long leccionId;
        private final String nombre;
        private final String color;
        private final Set<String> aulas = new LinkedHashSet<>();
        private final Set<String> docentes = new LinkedHashSet<>();

        private OpcionBuilder(HorarioLeccion leccion) {
            this.materiaId = leccion.getMateria().getId();
            this.leccionId = leccion.getId();
            this.nombre = leccion.getMateria().getNombre();
            String colorMateria = leccion.getMateria().getColor();
            this.color = colorMateria != null && colorMateria.matches("^#[0-9a-fA-F]{6}$") ? colorMateria : "#2d5a87";
        }
    }
}
