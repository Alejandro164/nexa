package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.RebajaConducta;

public interface RebajaConductaRepository extends JpaRepository<RebajaConducta, Long> {

    Optional<RebajaConducta> findByDireccionId(Long direccionId);
}
