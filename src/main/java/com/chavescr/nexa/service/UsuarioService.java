package com.chavescr.nexa.service;

import java.util.Collections;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.dto.UsuarioDTO;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.repository.UsuarioRepository;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usuarios", key = "'todos'")
    public List<UsuarioDTO> obtenerTodosDTO() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Usuario> buscarPorNombre(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return usuarioRepository.findAll();
        }
        return usuarioRepository.findByNombreOrEmail(filtro.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<InstitucionDTO> obtenerInstitucionesDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifierWithInstituciones(identifier)
                .map(usuario -> usuario.getInstituciones().stream()
                        .filter(Institucion::getActiva)
                        .map(InstitucionDTO::new)
                        .toList())
                .orElse(Collections.emptyList());
    }

    /** La última institución con la que trabajó el usuario autenticado, o null si nunca eligió una. */
    @Transactional(readOnly = true)
    public Long obtenerUltimaInstitucionIdDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifier(identifier)
                .map(Usuario::getUltimaInstitucion)
                .map(Institucion::getId)
                .orElse(null);
    }

    /** Recuerda la institución elegida para que se auto-seleccione en el próximo login. */
    public void actualizarUltimaInstitucion(Long usuarioId, Institucion institucion) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.setUltimaInstitucion(institucion);
            usuarioRepository.save(usuario);
        });
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerEstudiantesDelUsuarioActual() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByIdentifier(identifier)
                .map(padre -> usuarioRepository.findEstudiantesByPadreId(padre.getId()).stream()
                        .map(UsuarioDTO::new)
                        .toList())
                .orElse(Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public Usuario findByUsername(String username) {
        return usuarioRepository.findByEmailWithInstituciones(username).orElse(null);
    }

    @CacheEvict(value = "usuarios", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void evictAllCaches() {
    }
}
