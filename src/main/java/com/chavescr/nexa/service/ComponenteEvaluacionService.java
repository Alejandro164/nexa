package com.chavescr.nexa.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        int cotidiano = porcentaje(datos.getCotidiano(), "Trabajo cotidiano");
        int tareas = porcentaje(datos.getTareas(), "Tareas");
        int proyecto = porcentaje(datos.getProyecto(), "Proyecto");
        int pruebas = porcentaje(datos.getPruebas(), "Pruebas");
        int asistencia = porcentaje(datos.getAsistencia(), "Asistencia");
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
