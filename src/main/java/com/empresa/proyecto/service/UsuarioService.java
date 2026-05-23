package com.empresa.proyecto.service;

import com.empresa.proyecto.entity.Usuario;
import com.empresa.proyecto.repository.UsuarioRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usuarios", key = "'todos'")
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "usuarios", key = "#filtro?.trim()?.toLowerCase() ?: 'todos'")
    public List<Usuario> buscarPorNombre(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return usuarioRepository.findAll();
        }
        return usuarioRepository.findByNombreOrEmail(filtro.trim().toLowerCase());
    }

    @CacheEvict(value = "usuarios", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void evictAllCaches() {
    }
}
