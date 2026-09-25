package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chavescr.nexa.entity.Centro;

public interface CentroRepository extends JpaRepository<Centro, Long> {

    Optional<Centro> findByCedula(String cedula);

    @Query("SELECT DISTINCT c FROM Centro c LEFT JOIN FETCH c.instituciones ORDER BY c.nombre")
    List<Centro> findAllConInstituciones();
}
