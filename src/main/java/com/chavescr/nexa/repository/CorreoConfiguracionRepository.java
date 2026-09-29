package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.CorreoConfiguracion;

@Repository
public interface CorreoConfiguracionRepository extends JpaRepository<CorreoConfiguracion, Long> {

    Optional<CorreoConfiguracion> findByDireccionId(Long direccionId);
}
