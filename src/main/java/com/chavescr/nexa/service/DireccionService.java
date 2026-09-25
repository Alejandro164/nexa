package com.chavescr.nexa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.dto.DireccionDTO;
import com.chavescr.nexa.entity.Direccion;
import com.chavescr.nexa.repository.DireccionRepository;

@Service
@Transactional
public class DireccionService {

    private final DireccionRepository direccionRepository;

    public DireccionService(DireccionRepository direccionRepository) {
        this.direccionRepository = direccionRepository;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<Direccion> findAll() {
        return direccionRepository.findAll();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public Optional<Direccion> findById(Long id) {
        return direccionRepository.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Direccion save(Direccion direccion) {
        direccion.setCedula(textoONulo(direccion.getCedula()));
        direccion.setNombre(textoONulo(direccion.getNombre()));
        direccion.setCodigo(textoONulo(direccion.getCodigo()));
        if (direccion.getActiva() == null) {
            direccion.setActiva(true);
        }
        if (direccion.getNombre() == null) {
            throw new IllegalArgumentException("El nombre de la dirección es obligatorio.");
        }
        if (direccion.getCedula() != null) {
            direccionRepository.findByCedula(direccion.getCedula())
                    .filter(existente -> direccion.getId() == null
                            || !existente.getId().equals(direccion.getId()))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException("Ya existe una dirección con esa cédula.");
                    });
        }
        return direccionRepository.save(direccion);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        direccionRepository.deleteById(id);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<DireccionDTO> obtenerTodasDTO() {
        return direccionRepository.findAll().stream()
                .map(DireccionDTO::new)
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
