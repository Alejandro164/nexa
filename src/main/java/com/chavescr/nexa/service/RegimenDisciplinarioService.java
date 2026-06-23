package com.chavescr.nexa.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.RegimenDisciplinario;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.InstitucionRepository;
import com.chavescr.nexa.repository.RegimenDisciplinarioRepository;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class RegimenDisciplinarioService {

    private static final Logger log = LoggerFactory.getLogger(RegimenDisciplinarioService.class);

    private final RegimenDisciplinarioRepository repository;
    private final InstitucionRepository institucionRepository;
    private final UsuarioRepository usuarioRepository;

    public RegimenDisciplinarioService(RegimenDisciplinarioRepository repository,
                                       InstitucionRepository institucionRepository,
                                       UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.institucionRepository = institucionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<RegimenDisciplinario> listarTodos(Long institucionId) {
        return repository.findByInstitucionIdOrderByFechaDesc(institucionId);
    }

    @Transactional(readOnly = true)
    public List<RegimenDisciplinario> listarPorTipo(Long institucionId, RegimenDisciplinario.TipoRegimen tipo) {
        return repository.findByInstitucionIdAndTipoOrderByFechaDesc(institucionId, tipo);
    }

    @Transactional(readOnly = true)
    public RegimenDisciplinario obtenerPorId(Long institucionId, Long id) {
        return repository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado"));
    }

    public RegimenDisciplinario guardar(Long institucionId, RegimenDisciplinario datos) {
        if (datos.getMotivo() == null || datos.getMotivo().isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }
        RegimenDisciplinario registro = datos.getId() == null
                ? new RegimenDisciplinario()
                : obtenerPorId(institucionId, datos.getId());
        registro.setTipo(datos.getTipo());
        registro.setFuncionario(obtenerFuncionario(datos.getFuncionario().getId()));
        registro.setFecha(datos.getFecha());
        registro.setMotivo(datos.getMotivo().trim());
        registro.setDescripcion(datos.getDescripcion() != null ? datos.getDescripcion().trim() : null);
        registro.setEstado(datos.getEstado());
        registro.setResolucion(datos.getResolucion() != null ? datos.getResolucion().trim() : null);
        registro.setFechaResolucion(datos.getFechaResolucion());
        registro.setInstitucion(obtenerInstitucion(institucionId));
        RegimenDisciplinario guardado = repository.save(registro);
        log.info("Régimen guardado: id={}, tipo={}", guardado.getId(), guardado.getTipo());
        return guardado;
    }

    public void eliminar(Long institucionId, Long id) {
        RegimenDisciplinario registro = obtenerPorId(institucionId, id);
        repository.delete(registro);
        log.info("Régimen eliminado: id={}", id);
    }

    public void cambiarEstado(Long institucionId, Long id, RegimenDisciplinario.EstadoRegimen nuevoEstado) {
        RegimenDisciplinario registro = obtenerPorId(institucionId, id);
        registro.setEstado(nuevoEstado);
        if (nuevoEstado == RegimenDisciplinario.EstadoRegimen.RESUELTO && registro.getFechaResolucion() == null) {
            registro.setFechaResolucion(java.time.LocalDate.now());
        }
        repository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarFuncionarios(Long institucionId) {
        return usuarioRepository.findActivosByInstitucionId(institucionId);
    }

    private Usuario obtenerFuncionario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionario no encontrado"));
    }

    private Institucion obtenerInstitucion(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }
}
