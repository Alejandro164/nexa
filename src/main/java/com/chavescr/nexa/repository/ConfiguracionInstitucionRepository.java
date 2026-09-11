package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ConfiguracionInstitucion;

public interface ConfiguracionInstitucionRepository extends JpaRepository<ConfiguracionInstitucion, Long> {

    Optional<ConfiguracionInstitucion> findByInstitucionId(Long institucionId);
}
