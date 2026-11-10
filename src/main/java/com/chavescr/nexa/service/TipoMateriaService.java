package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.SeccionBloqueoLeccionRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;

/**
 * Tipos de materia de cada dirección. Una dirección sin tipos recibe los predeterminados la primera vez
 * que se listan; después el director los administra (crear, renombrar, desactivar, eliminar) libremente.
 */
@Service
@Transactional
public class TipoMateriaService {

    public static final List<Predeterminado> PREDETERMINADOS = List.of(
            new Predeterminado("Básica", 1),
            new Predeterminado("Complementaria", 2),
            new Predeterminado("Especialidad", 3),
            new Predeterminado("Taller", 4),
            new Predeterminado("Deportiva", 5),
            new Predeterminado("Música", 6),
            new Predeterminado("Arte", 7));

    private final TipoMateriaRepository repository;
    private final DireccionRepository direccionRepository;
    private final MateriaRepository materiaRepository;
    private final SeccionBloqueoLeccionRepository bloqueoRepository;

    public TipoMateriaService(TipoMateriaRepository repository, DireccionRepository direccionRepository,
            MateriaRepository materiaRepository, SeccionBloqueoLeccionRepository bloqueoRepository) {
        this.repository = repository;
        this.direccionRepository = direccionRepository;
        this.materiaRepository = materiaRepository;
        this.bloqueoRepository = bloqueoRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<TipoMateria> listar(Long direccionId) {
        asegurarPredeterminados(direccionId);
        return repository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<TipoMateria> listarActivos(Long direccionId) {
        asegurarPredeterminados(direccionId);
        return repository.findByDireccionIdAndActivoTrueOrderByOrdenAscNombreAsc(direccionId);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public TipoMateria obtenerActivo(Long direccionId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Debe seleccionar un tipo de materia");
        }
        return repository.findByIdAndDireccionId(id, direccionId)
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Tipo de materia no encontrado"));
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoMateria crear(Long direccionId, String nombre, Boolean activo) {
        String limpio = normalizarNombre(nombre);
        if (repository.existsByDireccionIdAndNombreIgnoreCase(direccionId, limpio)) {
            throw new IllegalArgumentException("Ya existe un tipo de materia con ese nombre");
        }
        TipoMateria tipo = new TipoMateria();
        tipo.setDireccion(direccion(direccionId));
        tipo.setNombre(limpio);
        tipo.setActivo(activo == null || activo);
        tipo.setOrden(siguienteOrden(direccionId));
        return repository.save(tipo);
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoMateria actualizar(Long direccionId, Long id, String nombre, Boolean activo) {
        TipoMateria tipo = obtener(direccionId, id);
        String limpio = normalizarNombre(nombre);
        if (repository.existsByDireccionIdAndNombreIgnoreCaseAndIdNot(direccionId, limpio, id)) {
            throw new IllegalArgumentException("Ya existe un tipo de materia con ese nombre");
        }
        boolean renombrado = !limpio.equals(tipo.getNombre());
        tipo.setNombre(limpio);
        if (activo != null) {
            tipo.setActivo(activo);
        }
        TipoMateria guardado = repository.save(tipo);
        if (renombrado) {
            materiaRepository.sincronizarNombreTipo(guardado.getId(), limpio);
        }
        return guardado;
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long direccionId, Long id) {
        TipoMateria tipo = obtener(direccionId, id);
        if (materiaRepository.existsByTipoMateriaId(id) || bloqueoRepository.existsByTipoMateriaId(id)) {
            throw new IllegalArgumentException(
                    "Este tipo tiene materias o lecciones bloqueadas asociadas; desactívalo en lugar de eliminarlo");
        }
        repository.delete(tipo);
    }

    /** Solo siembra cuando la dirección no tiene ningún tipo, para no revivir los que el director borró. */
    private void asegurarPredeterminados(Long direccionId) {
        if (repository.existsByDireccionId(direccionId)) {
            return;
        }
        Direccion direccion = direccion(direccionId);
        for (Predeterminado predeterminado : PREDETERMINADOS) {
            TipoMateria tipo = new TipoMateria();
            tipo.setDireccion(direccion);
            tipo.setNombre(predeterminado.nombre());
            tipo.setOrden(predeterminado.orden());
            tipo.setActivo(true);
            try {
                repository.saveAndFlush(tipo);
            } catch (DataIntegrityViolationException ignored) {
                // Otro request ya creó el predeterminado.
            }
        }
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public TipoMateria obtener(Long direccionId, Long id) {
        return repository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de materia no encontrado"));
    }

    private Direccion direccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }

    private int siguienteOrden(Long direccionId) {
        return repository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId).stream()
                .map(TipoMateria::getOrden)
                .filter(orden -> orden != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Indica el nombre del tipo de materia");
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > 60) {
            throw new IllegalArgumentException("El nombre no puede superar 60 caracteres");
        }
        return limpio;
    }

    public record Predeterminado(String nombre, int orden) {
    }
}
