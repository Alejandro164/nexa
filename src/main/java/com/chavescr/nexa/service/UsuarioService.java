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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailWithInstituciones(email)
                .map(usuario -> usuario.getInstituciones().stream()
                        .filter(Institucion::getActiva)
                        .map(InstitucionDTO::new)
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
