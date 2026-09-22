package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.WhatsAppConfiguracion;

@Repository
public interface WhatsAppConfiguracionRepository extends JpaRepository<WhatsAppConfiguracion, Long> {

    Optional<WhatsAppConfiguracion> findByInstitucionId(Long institucionId);
}
