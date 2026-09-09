package com.chavescr.nexa.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.Aula;
import com.chavescr.nexa.entity.TipoAula;
import com.chavescr.nexa.repository.AulaRepository;
import com.chavescr.nexa.repository.TipoAulaRepository;

@Component
public class TipoAulaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TipoAulaSeeder.class);

    private static final List<String[]> TIPOS = List.of(
            new String[] {"REGULAR", "Regular", "1"},
            new String[] {"LABORATORIO", "Laboratorio", "2"},
            new String[] {"TALLER", "Taller", "3"},
            new String[] {"COMPUTO", "Sala de Cómputo", "4"},
            new String[] {"ARTE", "Sala de Arte", "5"},
            new String[] {"MUSICA", "Sala de Música", "6"},
            new String[] {"AUDITORIO", "Auditorio", "7"},
            new String[] {"BIBLIOTECA", "Biblioteca", "8"},
            new String[] {"GIMNASIO", "Gimnasio", "9"});

    private final TipoAulaRepository tipoAulaRepository;
    private final AulaRepository aulaRepository;

    public TipoAulaSeeder(TipoAulaRepository tipoAulaRepository, AulaRepository aulaRepository) {
        this.tipoAulaRepository = tipoAulaRepository;
        this.aulaRepository = aulaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String[] tipo : TIPOS) {
            TipoAula catalogo = tipoAulaRepository.findByCodigo(tipo[0]).orElseGet(TipoAula::new);
            catalogo.setCodigo(tipo[0]);
            catalogo.setNombre(tipo[1]);
            catalogo.setOrden(Integer.parseInt(tipo[2]));
            catalogo.setActivo(true);
            tipoAulaRepository.save(catalogo);
        }
        log.info("Catálogo de tipos de aula listo");

        for (Aula aula : aulaRepository.findAll()) {
            if (aula.getTipoAula() != null || aula.getTipo() == null || aula.getTipo().isBlank()) {
                continue;
            }
            tipoAulaRepository.findByNombreIgnoreCase(aula.getTipo().trim()).ifPresent(tipo -> {
                aula.setTipoAula(tipo);
                aulaRepository.save(aula);
            });
        }
    }
}
