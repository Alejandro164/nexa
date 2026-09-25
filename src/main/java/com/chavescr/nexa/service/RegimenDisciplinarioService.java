package com.chavescr.nexa.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.entity.RegimenDisciplinario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.DireccionRepository;
import com.chavescr.nexa.repository.RegimenDisciplinarioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class RegimenDisciplinarioService {

    private static final Logger log = LoggerFactory.getLogger(RegimenDisciplinarioService.class);

    private final RegimenDisciplinarioRepository repository;
    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public RegimenDisciplinarioService(RegimenDisciplinarioRepository repository,
                                       DireccionRepository direccionRepository,
                                       UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<RegimenDisciplinario> listarTodos(Long direccionId) {
        return repository.findByDireccionIdOrderByFechaDesc(direccionId);
    }

    @Transactional(readOnly = true)
    public List<RegimenDisciplinario> listarPorTipo(Long direccionId, RegimenDisciplinario.TipoRegimen tipo) {
        return repository.findByDireccionIdAndTipoOrderByFechaDesc(direccionId, tipo);
    }

    @Transactional(readOnly = true)
    public RegimenDisciplinario obtenerPorId(Long direccionId, Long id) {
        return repository.findByIdAndDireccionId(id, direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado"));
    }

    public RegimenDisciplinario guardar(Long direccionId, RegimenDisciplinario datos) {
        if (datos.getMotivo() == null || datos.getMotivo().isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }
        RegimenDisciplinario registro = datos.getId() == null
                ? new RegimenDisciplinario()
                : obtenerPorId(direccionId, datos.getId());
        registro.setTipo(datos.getTipo());
        registro.setFuncionario(obtenerFuncionario(datos.getFuncionario().getId()));
        registro.setFecha(datos.getFecha());
        registro.setMotivo(datos.getMotivo().trim());
        registro.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        registro.setEstado(datos.getEstado());
        registro.setResolucion(datos.getResolucion() != null ? datos.getResolucion().trim() : null);
        registro.setFechaResolucion(datos.getFechaResolucion());
        registro.setDireccion(obtenerDireccion(direccionId));
        RegimenDisciplinario guardado = repository.save(registro);
        log.info("Régimen guardado: id={}, tipo={}", guardado.getId(), guardado.getTipo());
        return guardado;
    }

    public void eliminar(Long direccionId, Long id) {
        RegimenDisciplinario registro = obtenerPorId(direccionId, id);
        repository.delete(registro);
        log.info("Régimen eliminado: id={}", id);
    }

    public void cambiarEstado(Long direccionId, Long id, RegimenDisciplinario.EstadoRegimen nuevoEstado) {
        RegimenDisciplinario registro = obtenerPorId(direccionId, id);
        registro.setEstado(nuevoEstado);
        if (nuevoEstado == RegimenDisciplinario.EstadoRegimen.RESUELTO && registro.getFechaResolucion() == null) {
            registro.setFechaResolucion(java.time.LocalDate.now());
        }
        repository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarFuncionarios(Long direccionId) {
        return usuarioRepository.findActivosByDireccionId(direccionId);
    }

    private Usuario obtenerFuncionario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario no encontrado"));
    }

    private Direccion obtenerDireccion(Long direccionId) {
        return direccionRepository.findById(direccionId)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
    }
}
