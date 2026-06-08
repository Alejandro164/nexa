package com.chavescr.nexa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.repository.InstitucionRepository;

@Service
public class InstitucionService {

    @Autowired
    private InstitucionRepository institucionRepository;

    public List<Institucion> findAll() {
        return institucionRepository.findAll();
    }

    public Optional<Institucion> findById(Long id) {
        return institucionRepository.findById(id);
    }

    public Institucion save(Institucion institucion) {
        return institucionRepository.save(institucion);
    }

    public void deleteById(Long id) {
        institucionRepository.deleteById(id);
    }

    public List<InstitucionDTO> obtenerTodasDTO() {
        return institucionRepository.findAll().stream()
                .map(InstitucionDTO::new)
                .toList();
    }
}
