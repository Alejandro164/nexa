package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.EscalaNotas;

public interface EscalaNotasRepository extends JpaRepository<EscalaNotas, Long> {

    Optional<EscalaNotas> findByDireccionId(Long direccionId);
}
