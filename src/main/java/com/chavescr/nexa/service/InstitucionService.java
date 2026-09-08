package com.chavescr.nexa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.InstitucionDTO;
import com.chavescr.nexa.entity.Institucion;
import com.chavescr.nexa.repository.InstitucionRepository;

@Service
@Transactional
public class InstitucionService {

    private final InstitucionRepository institucionRepository;

    public InstitucionService(InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Institucion> findAll() {
        return institucionRepository.findAll();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<Institucion> findById(Long id) {
        return institucionRepository.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Institucion save(Institucion institucion) {
        institucion.setCedula(textoONulo(institucion.getCedula()));
        institucion.setNombre(textoONulo(institucion.getNombre()));
        institucion.setCodigo(textoONulo(institucion.getCodigo()));
        if (institucion.getActiva() == null) {
            institucion.setActiva(true);
        }
        if (institucion.getNombre() == null) {
            throw new IllegalArgumentException("El nombre de la institución es obligatorio.");
        }
        if (institucion.getCedula() != null) {
            institucionRepository.findByCedula(institucion.getCedula())
                    .filter(existente -> institucion.getId() == null
                            || !existente.getId().equals(institucion.getId()))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException("Ya existe una institución con esa cédula.");
                    });
        }
        return institucionRepository.save(institucion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        institucionRepository.deleteById(id);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<InstitucionDTO> obtenerTodasDTO() {
        return institucionRepository.findAll().stream()
                .map(InstitucionDTO::new)
                .toList();
    }

    private static String textoONulo(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
