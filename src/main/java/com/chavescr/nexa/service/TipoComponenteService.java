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

/**
 * Rubros de la nota (trabajo cotidiano, tareas, proyecto, pruebas). Son fijos: se crean solos por dirección y
 * ya no se administran; los porcentajes de cada uno se definen en los componentes de evaluación.
 */
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

    private Direccion direccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }

    private String emojiDe(ClaveComponente clave) {
        return PREDETERMINADOS.stream()
                .filter(item -> item.clave() == clave)
                .map(Predeterminado::emoji)
                .findFirst()
                .orElse("📝");
    }

    private record Predeterminado(ClaveComponente clave, String nombre, String emoji, int orden) {
    }
}
