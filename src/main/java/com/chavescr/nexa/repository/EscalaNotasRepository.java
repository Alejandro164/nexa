package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.CicloEducativo;
import com.chavescr.nexa.entity.EscalaNotas;

public interface EscalaNotasRepository extends JpaRepository<EscalaNotas, Long> {
    Optional<EscalaNotas> findByDireccionIdAndCiclo(Long direccionId, CicloEducativo ciclo);

    List<EscalaNotas> findByDireccionId(Long direccionId);
}
