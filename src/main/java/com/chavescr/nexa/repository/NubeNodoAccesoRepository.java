package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.NubeNodoAcceso;

@Repository
public interface NubeNodoAccesoRepository extends JpaRepository<NubeNodoAcceso, Long> {

    List<NubeNodoAcceso> findByNodoIdOrderByFechaCompartidoDesc(Long nodoId);

    Optional<NubeNodoAcceso> findByNodoIdAndUsuarioId(Long nodoId, Long usuarioId);

    boolean existsByNodoIdAndUsuarioId(Long nodoId, Long usuarioId);

    void deleteByNodoIdAndUsuarioId(Long nodoId, Long usuarioId);
}
