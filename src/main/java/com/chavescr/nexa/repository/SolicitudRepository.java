package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Solicitud;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByPadreIdOrderByFechaSolicitudDesc(Long padreId);

    List<Solicitud> findByInstitucionIdOrderByFechaSolicitudDesc(Long institucionId);
}
