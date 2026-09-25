package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ConfiguracionDireccion;

public interface ConfiguracionDireccionRepository extends JpaRepository<ConfiguracionDireccion, Long> {

    Optional<ConfiguracionDireccion> findByDireccionId(Long direccionId);
}
