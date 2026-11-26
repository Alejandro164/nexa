package com.chavescr.nexa.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.NombresRubros;
import com.chavescr.nexa.entity.ComponenteEvaluacion;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.repository.ComponenteEvaluacionRepository;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;

/**
 * Componentes de evaluación de cada dirección. Una dirección sin componentes recibe "General" y los casos más
 * comunes de los artículos 40 y 41 del Reglamento de Evaluación de los Aprendizajes; después el director los
 * administra libremente. Las notas leen los porcentajes de aquí a través de la materia.
 */
@Service
@Transactional
public class ComponenteEvaluacionService {

    public static final String NOMBRE_GENERAL = "General";

    private static final int LARGO_NOMBRE_RUBRO = 60;
    private static final int LARGO_EMOJI = 16;

    private static final List<Predeterminado> PREDETERMINADOS = List.of(
            new Predeterminado(NOMBRE_GENERAL, "Reparto por defecto de Nexa", 40, 15, 20, 20, 5),
            new Predeterminado("I y II Ciclo — Básicas",
                    "Art. 40 a) · Matemática, Estudios Sociales y Cívica, Ciencias, Español, Lengua Extranjera",
                    50, 10, 0, 35, 5),
            new Predeterminado("I y II Ciclo — Complementarias",
                    "Art. 40 b) · Ed. Musical, Ed. Física, Ed. para el Hogar, Artes Industriales",
                    60, 10, 0, 25, 5),
            new Predeterminado("III Ciclo — Básicas",
                    "Art. 41 a) · Matemática, Español, Estudios Sociales, Ciencias, Lengua Extranjera",
                    45, 10, 0, 40, 5),
            new Predeterminado("III Ciclo — Complementarias",
                    "Art. 41 b) · Cívica, Artes Plásticas, Ed. para el Hogar, Artes Industriales, Ed. Física, "
                            + "Ed. Musical, Psicología",
                    45, 10, 40, 0, 5),
            new Predeterminado("III Ciclo — Valor agregado", "Art. 41 c)", 35, 10, 20, 30, 5),
            new Predeterminado("Educación Religiosa", "Art. 40 c) y 41 e)", 70, 25, 0, 0, 5));

    private final ComponenteEvaluacionRepository repository;
    private final DireccionRepository direccionRepository;
    private final MateriaRepository materiaRepository;

    public ComponenteEvaluacionService(ComponenteEvaluacionRepository repository,
            DireccionRepository direccionRepository, MateriaRepository materiaRepository) {
        this.repository = repository;
        this.direccionRepository = direccionRepository;
        this.materiaRepository = materiaRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ComponenteEvaluacion> listar(Long direccionId) {
        asegurarPredeterminados(direccionId);
        return repository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ComponenteEvaluacion> listarActivos(Long direccionId) {
        asegurarPredeterminados(direccionId);
        return repository.findByDireccionIdAndActivoTrueOrderByOrdenAscNombreAsc(direccionId);
    }

    /** Cantidad de materias por id de componente; los componentes sin materias no aparecen. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, Long> contarMaterias(Long direccionId) {
        Map<Long, Long> conteo = new HashMap<>();
        for (Object[] fila : materiaRepository.contarPorComponente(direccionId)) {
            conteo.put((Long) fila[0], (Long) fila[1]);
        }
        return conteo;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ComponenteEvaluacion obtener(Long direccionId, Long id) {
        return repository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Componente no encontrado"));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ComponenteEvaluacion obtenerActivo(Long direccionId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un componente de evaluación");
        }
        return repository.findByIdAndDireccionId(id, direccionId)
                .filter(componente -> Boolean.TRUE.equals(componente.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Componente de evaluación no encontrado"));
    }

    /** Crea el componente si {@code datos.id} es null; si no, lo actualiza. */
    @Transactional(rollbackFor = Exception.class)
    public ComponenteEvaluacion guardar(Long direccionId, ComponenteEvaluacion datos) {
        String nombre = normalizarNombre(datos.getNombre());
        boolean repetido = datos.getId() == null
                ? repository.existsByDireccionIdAndNombreIgnoreCase(direccionId, nombre)
                : repository.existsByDireccionIdAndNombreIgnoreCaseAndIdNot(direccionId, nombre, datos.getId());
        if (repetido) {
            throw new IllegalArgumentException("Ya existe un componente con ese nombre");
        }
        String nombreCotidiano = nombreRubro(datos.getNombreCotidiano(), NombresRubros.COTIDIANO);
        String nombreTareas = nombreRubro(datos.getNombreTareas(), NombresRubros.TAREAS);
        String nombreProyecto = nombreRubro(datos.getNombreProyecto(), NombresRubros.PROYECTO);
        String nombrePruebas = nombreRubro(datos.getNombrePruebas(), NombresRubros.PRUEBAS);
        String nombreAsistencia = nombreRubro(datos.getNombreAsistencia(), NombresRubros.ASISTENCIA);
        NombresRubros nombres = new NombresRubros(nombreCotidiano, nombreTareas, nombreProyecto, nombrePruebas,
                nombreAsistencia);
        validarNombresDistintos(nombres);
        String emojiCotidiano = emojiRubro(datos.getEmojiCotidiano(), NombresRubros.EMOJI_COTIDIANO,
                nombres.getCotidiano());
        String emojiTareas = emojiRubro(datos.getEmojiTareas(), NombresRubros.EMOJI_TAREAS, nombres.getTareas());
        String emojiProyecto = emojiRubro(datos.getEmojiProyecto(), NombresRubros.EMOJI_PROYECTO,
                nombres.getProyecto());
        String emojiPruebas = emojiRubro(datos.getEmojiPruebas(), NombresRubros.EMOJI_PRUEBAS, nombres.getPruebas());
        String emojiAsistencia = emojiRubro(datos.getEmojiAsistencia(), NombresRubros.EMOJI_ASISTENCIA,
                nombres.getAsistencia());

        int cotidiano = porcentaje(datos.getCotidiano(), nombres.getCotidiano());
        int tareas = porcentaje(datos.getTareas(), nombres.getTareas());
        int proyecto = porcentaje(datos.getProyecto(), nombres.getProyecto());
        int pruebas = porcentaje(datos.getPruebas(), nombres.getPruebas());
        int asistencia = porcentaje(datos.getAsistencia(), nombres.getAsistencia());
        int total = cotidiano + tareas + proyecto + pruebas + asistencia;
        if (total != 100) {
            throw new IllegalArgumentException(
                    "La suma de los porcentajes debe ser exactamente 100% (actual: " + total + "%)");
        }

        ComponenteEvaluacion componente;
        if (datos.getId() == null) {
            componente = new ComponenteEvaluacion();
            componente.setDireccion(direccion(direccionId));
            componente.setOrden(siguienteOrden(direccionId));
        } else {
            componente = obtener(direccionId, datos.getId());
        }
        componente.setNombre(nombre);
        componente.setDescripcion(normalizarDescripcion(datos.getDescripcion()));
        componente.setCotidiano(cotidiano);
        componente.setTareas(tareas);
        componente.setProyecto(proyecto);
        componente.setPruebas(pruebas);
        componente.setAsistencia(asistencia);
        componente.setNombreCotidiano(nombreCotidiano);
        componente.setNombreTareas(nombreTareas);
        componente.setNombreProyecto(nombreProyecto);
        componente.setNombrePruebas(nombrePruebas);
        componente.setNombreAsistencia(nombreAsistencia);
        componente.setEmojiCotidiano(emojiCotidiano);
        componente.setEmojiTareas(emojiTareas);
        componente.setEmojiProyecto(emojiProyecto);
        componente.setEmojiPruebas(emojiPruebas);
        componente.setEmojiAsistencia(emojiAsistencia);
        componente.setActivo(datos.getActivo() == null || datos.getActivo());
        return repository.save(componente);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long direccionId, Long id) {
        ComponenteEvaluacion componente = obtener(direccionId, id);
        if (materiaRepository.existsByComponenteEvaluacionId(id)) {
            throw new IllegalArgumentException(
                    "Este componente tiene materias asignadas; cámbialas de componente o desactívalo");
        }
        repository.delete(componente);
    }

    /** Nombres de los rubros según el componente de la materia; sin componente, los predeterminados. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public NombresRubros nombresDeMateria(Long direccionId, Long materiaId) {
        if (materiaId == null) {
            return NombresRubros.PREDETERMINADOS;
        }
        return materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                .map(Materia::getComponenteEvaluacion)
                .map(ComponenteEvaluacion::getNombresRubros)
                .orElse(NombresRubros.PREDETERMINADOS);
    }

    /** Nombres de los rubros de cada materia de la dirección que tiene componente, por id de materia. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, NombresRubros> nombresPorMateria(Long direccionId) {
        Map<Long, NombresRubros> nombres = new HashMap<>();
        for (Materia materia : materiaRepository.findByDireccionIdOrderByNombreAsc(direccionId)) {
            if (materia.getComponenteEvaluacion() != null) {
                nombres.put(materia.getId(), materia.getComponenteEvaluacion().getNombresRubros());
            }
        }
        return nombres;
    }

    /** Pesos de la materia (cotidiano, tareas, proyecto, pruebas, asistencia). */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public int[] pesosDeMateria(Long direccionId, Long materiaId) {
        return materiaRepository.findByIdAndDireccionId(materiaId, direccionId)
                .map(Materia::getComponenteEvaluacion)
                .map(ComponenteEvaluacion::pesos)
                .orElseGet(() -> ComponenteEvaluacion.PESOS_POR_DEFECTO.clone());
    }

    /** Pesos de cada materia de la dirección, por id de materia. */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Map<Long, int[]> pesosPorMateria(Long direccionId) {
        Map<Long, int[]> pesos = new HashMap<>();
        for (Materia materia : materiaRepository.findByDireccionIdOrderByNombreAsc(direccionId)) {
            if (materia.getComponenteEvaluacion() != null) {
                pesos.put(materia.getId(), materia.getComponenteEvaluacion().pesos());
            }
        }
        return pesos;
    }

    /**
     * Asegura los componentes de la dirección y asigna uno a las materias que no tienen. Usa "General" o, si
     * el director lo renombró o eliminó, el primero de la lista. Devuelve cuántas materias se asignaron.
     */
    @Transactional(rollbackFor = Exception.class)
    public int asignarAMateriasSinComponente(Long direccionId) {
        asegurarPredeterminados(direccionId);
        return repository.findFirstByDireccionIdAndNombreIgnoreCase(direccionId, NOMBRE_GENERAL)
                .or(() -> repository.findFirstByDireccionIdOrderByOrdenAscNombreAsc(direccionId))
                .map(componente -> materiaRepository.asignarComponenteFaltante(direccionId, componente))
                .orElse(0);
    }

    /** Solo siembra cuando la dirección no tiene ningún componente, para no revivir los que el director borró. */
    private void asegurarPredeterminados(Long direccionId) {
        if (repository.existsByDireccionId(direccionId)) {
            return;
        }
        Direccion direccion = direccion(direccionId);
        int orden = 1;
        for (Predeterminado predeterminado : PREDETERMINADOS) {
            ComponenteEvaluacion componente = new ComponenteEvaluacion();
            componente.setDireccion(direccion);
            componente.setNombre(predeterminado.nombre());
            componente.setDescripcion(predeterminado.descripcion());
            componente.setCotidiano(predeterminado.cotidiano());
            componente.setTareas(predeterminado.tareas());
            componente.setProyecto(predeterminado.proyecto());
            componente.setPruebas(predeterminado.pruebas());
            componente.setAsistencia(predeterminado.asistencia());
            componente.setOrden(orden++);
            componente.setActivo(true);
            try {
                repository.saveAndFlush(componente);
            } catch (DataIntegrityViolationException ignored) {
                // Otro request ya creó el predeterminado.
            }
        }
    }

    private Direccion direccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }

    private int siguienteOrden(Long direccionId) {
        return repository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId).stream()
                .map(ComponenteEvaluacion::getOrden)
                .filter(orden -> orden != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Nombre propio de un rubro: se guarda null si viene vacío o igual al predeterminado, para que el
     * componente siga usando el nombre estándar.
     */
    private static String nombreRubro(String nombre, String predeterminado) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || limpio.equals(predeterminado)) {
            return null;
        }
        if (limpio.length() > LARGO_NOMBRE_RUBRO) {
            throw new IllegalArgumentException("El nombre del rubro «" + predeterminado + "» admite hasta "
                    + LARGO_NOMBRE_RUBRO + " caracteres");
        }
        return limpio;
    }

    /**
     * Emoji propio de un rubro: null si viene vacío o igual al predeterminado (sigue el de la institución). Solo
     * acepta emojis: nada de letras, números ni etiquetas, y a lo sumo {@value #LARGO_EMOJI} caracteres (algunos
     * emojis ocupan varios, como ✏️ o 👨‍🏫).
     */
    private static String emojiRubro(String emoji, String predeterminado, String rubro) {
        String limpio = emoji == null ? "" : emoji.strip();
        if (limpio.isEmpty() || limpio.equals(predeterminado)) {
            return null;
        }
        if (limpio.length() > LARGO_EMOJI || limpio.codePoints().anyMatch(c -> Character.isLetterOrDigit(c)
                || Character.isWhitespace(c) || c == '<' || c == '>' || c == '&' || c == '"' || c == '\'')) {
            throw new IllegalArgumentException("El emoji del rubro «" + rubro + "» no es válido");
        }
        return limpio;
    }

    private static void validarNombresDistintos(NombresRubros nombres) {
        Set<String> usados = new HashSet<>();
        for (String nombre : List.of(nombres.getCotidiano(), nombres.getTareas(), nombres.getProyecto(),
                nombres.getPruebas(), nombres.getAsistencia())) {
            if (!usados.add(nombre.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Dos rubros del componente no pueden llamarse «" + nombre + "»");
            }
        }
    }

    private int porcentaje(Integer valor, String rubro) {
        int numero = valor == null ? 0 : valor;
        if (numero < 0 || numero > 100) {
            throw new IllegalArgumentException("El porcentaje de " + rubro + " debe estar entre 0 y 100");
        }
        return numero;
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Indica el nombre del componente");
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > 60) {
            throw new IllegalArgumentException("El nombre no puede superar 60 caracteres");
        }
        return limpio;
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String limpia = descripcion.trim().replaceAll("\\s+", " ");
        if (limpia.length() > 160) {
            throw new IllegalArgumentException("La descripción no puede superar 160 caracteres");
        }
        return limpia;
    }

    private record Predeterminado(String nombre, String descripcion, int cotidiano, int tareas, int proyecto,
            int pruebas, int asistencia) {
    }
}
