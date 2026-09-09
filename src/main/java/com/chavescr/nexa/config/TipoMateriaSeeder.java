package com.chavescr.nexa.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.TipoMateria;
import com.chavescr.nexa.repository.MateriaRepository;
import com.chavescr.nexa.repository.TipoMateriaRepository;

@Component
public class TipoMateriaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TipoMateriaSeeder.class);

    private static final List<TipoCatalogo> TIPOS = List.of(
            new TipoCatalogo("BASICA", "Básica", 1),
            new TipoCatalogo("COMPLEMENTARIA", "Complementaria", 2),
            new TipoCatalogo("ESPECIALIDAD", "Especialidad", 3),
            new TipoCatalogo("TALLER", "Taller", 4),
            new TipoCatalogo("DEPORTIVA", "Deportiva", 5),
            new TipoCatalogo("MUSICA", "Música", 6),
            new TipoCatalogo("ARTE", "Arte", 7));

    private final TipoMateriaRepository tipoMateriaRepository;
    private final MateriaRepository materiaRepository;

    public TipoMateriaSeeder(TipoMateriaRepository tipoMateriaRepository, MateriaRepository materiaRepository) {
        this.tipoMateriaRepository = tipoMateriaRepository;
        this.materiaRepository = materiaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (TipoCatalogo catalogo : TIPOS) {
            TipoMateria tipo = tipoMateriaRepository.findByCodigo(catalogo.codigo()).orElseGet(TipoMateria::new);
            tipo.setCodigo(catalogo.codigo());
            tipo.setNombre(catalogo.nombre());
            tipo.setOrden(catalogo.orden());
            tipo.setActivo(true);
            tipoMateriaRepository.save(tipo);
        }

        int vinculadas = 0;
        for (Materia materia : materiaRepository.findAll()) {
            if (materia.getTipoMateria() != null || materia.getTipo() == null || materia.getTipo().isBlank()) {
                continue;
            }
            tipoMateriaRepository.findByNombreIgnoreCase(materia.getTipo().trim()).ifPresent(tipo -> {
                materia.setTipoMateria(tipo);
                materiaRepository.save(materia);
            });
            if (materia.getTipoMateria() != null) {
                vinculadas++;
            }
        }
        log.info("Catálogo de tipos de materia listo. Materias vinculadas: {}", vinculadas);
    }

    private record TipoCatalogo(String codigo, String nombre, int orden) {
    }
}
