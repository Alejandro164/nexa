package com.empresa.proyecto.service;

import com.empresa.proyecto.dto.EmpresaDTO;
import com.empresa.proyecto.entity.Empresa;
import com.empresa.proyecto.repository.EmpresaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Transactional(readOnly = true)
    public List<Empresa> obtenerTodas() {
        return empresaRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "empresas", key = "'todas'")
    public List<EmpresaDTO> obtenerTodasDTO() {
        return empresaRepository.findAll().stream()
                .map(EmpresaDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Empresa> buscarPorNombre(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return empresaRepository.findAll();
        }
        return empresaRepository.findByNombreContainingIgnoreCase(filtro.trim());
    }

    @CacheEvict(value = "empresas", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void evictAllCaches() {
    }
}
