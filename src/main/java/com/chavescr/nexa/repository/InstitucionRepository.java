package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chavescr.nexa.entity.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {

    Optional<Institucion> findByCedula(String cedula);

    @Query("SELECT DISTINCT c FROM Institucion c LEFT JOIN FETCH c.direcciones ORDER BY c.nombre")
    List<Institucion> findAllConDirecciones();
}
