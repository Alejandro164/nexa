package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.AsistenteConfiguracion;

public interface AsistenteConfiguracionRepository extends JpaRepository<AsistenteConfiguracion, Long> {

    /** La configuración es una sola para todo el sistema. */
    Optional<AsistenteConfiguracion> findFirstByOrderByIdAsc();
}
