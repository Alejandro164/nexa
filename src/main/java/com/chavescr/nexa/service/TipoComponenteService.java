package com.chavescr.nexa.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.TipoComponente;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.TipoComponenteRepository;

@Service
@Transactional
public class TipoComponenteService {

    private static final List<Predeterminado> PREDETERMINADOS = List.of(
            new Predeterminado(ClaveComponente.COTIDIANO, "Cotidiano", "📝", 1),
            new Predeterminado(ClaveComponente.TAREA, "Tareas", "📚", 2),
            new Predeterminado(ClaveComponente.PROYECTO, "Proyecto", "🎯", 3),
            new Predeterminado(ClaveComponente.EXAMEN, "Pruebas", "📋", 4));

    private final TipoComponenteRepository repository;
    private final DireccionRepository direccionRepository;

    public TipoComponenteService(TipoComponenteRepository repository, DireccionRepository direccionRepository) {
        this.repository = repository;
        this.direccionRepository = direccionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<TipoComponente> listar(Long direccionId) {
        asegurarPredeterminados(direccionId);
        List<TipoComponente> tipos = repository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId);
        for (TipoComponente tipo : tipos) {
            if (tipo.getClave() != null && (tipo.getEmoji() == null || tipo.getEmoji().isBlank())) {
                tipo.setEmoji(emojiDe(tipo.getClave()));
                repository.save(tipo);
            }
        }
        return tipos;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<TipoComponente> listarActivos(Long direccionId) {
        return listar(direccionId).stream()
                .filter(tipo -> Boolean.TRUE.equals(tipo.getActivo()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoComponente crear(Long direccionId, String nombre, String emoji, Boolean activo) {
        String limpio = normalizarNombre(nombre);
        if (repository.existsByDireccionIdAndNombreIgnoreCase(direccionId, limpio)) {
            throw new IllegalArgumentException("Ya existe un tipo de componente con ese nombre");
        }
        TipoComponente tipo = new TipoComponente();
        tipo.setDireccion(direccion(direccionId));
        tipo.setNombre(limpio);
        tipo.setEmoji(normalizarEmoji(emoji));
        tipo.setActivo(activo == null || activo);
        tipo.setOrden(siguienteOrden(direccionId));
        return repository.save(tipo);
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoComponente actualizar(Long direccionId, Long id, String nombre, String emoji, Boolean activo) {
        TipoComponente tipo = obtener(direccionId, id);
        String limpio = normalizarNombre(nombre);
        if (repository.existsByDireccionIdAndNombreIgnoreCaseAndIdNot(direccionId, limpio, id)) {
            throw new IllegalArgumentException("Ya existe un tipo de componente con ese nombre");
        }
        tipo.setNombre(limpio);
        if (emoji != null) {
            tipo.setEmoji(normalizarEmoji(emoji));
        }
        if (activo != null) {
            tipo.setActivo(activo);
        }
        return repository.save(tipo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long direccionId, Long id) {
        TipoComponente tipo = obtener(direccionId, id);
        if (tipo.isDelSistema()) {
            throw new IllegalArgumentException("Los tipos del sistema se desactivan, no se eliminan");
        }
        repository.delete(tipo);
    }

    private void asegurarPredeterminados(Long direccionId) {
        Direccion direccion = direccion(direccionId);
        for (Predeterminado predeterminado : PREDETERMINADOS) {
            if (repository.findByDireccionIdAndClave(direccionId, predeterminado.clave()).isPresent()) {
                continue;
            }
            if (repository.existsByDireccionIdAndNombreIgnoreCase(direccionId, predeterminado.nombre())) {
                continue;
            }
            TipoComponente tipo = new TipoComponente();
            tipo.setDireccion(direccion);
            tipo.setClave(predeterminado.clave());
            tipo.setNombre(predeterminado.nombre());
            tipo.setEmoji(predeterminado.emoji());
            tipo.setOrden(predeterminado.orden());
            tipo.setActivo(true);
            try {
                repository.saveAndFlush(tipo);
            } catch (DataIntegrityViolationException ignored) {
                // Otro request ya creó el predeterminado.
            }
        }
    }

    private TipoComponente obtener(Long direccionId, Long id) {
        return repository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de componente no encontrado"));
    }

    private Direccion direccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }

    private int siguienteOrden(Long direccionId) {
        return repository.findByDireccionIdOrderByOrdenAscNombreAsc(direccionId).stream()
                .map(TipoComponente::getOrden)
                .filter(orden -> orden != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private String emojiDe(ClaveComponente clave) {
        return PREDETERMINADOS.stream()
                .filter(item -> item.clave() == clave)
                .map(Predeterminado::emoji)
                .findFirst()
                .orElse("📝");
    }

    private String normalizarEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return null;
        }
        String limpio = emoji.trim();
        if (limpio.length() > 16) {
            throw new IllegalArgumentException("Elige un solo emoji");
        }
        return limpio;
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Indica el nombre del tipo de componente");
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > 60) {
            throw new IllegalArgumentException("El nombre no puede superar 60 caracteres");
        }
        return limpio;
    }

    private record Predeterminado(ClaveComponente clave, String nombre, String emoji, int orden) {
    }
}
