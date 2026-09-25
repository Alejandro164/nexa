package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Direccion;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    Optional<Direccion> findByCodigo(String codigo);

    Optional<Direccion> findByCedula(String cedula);

    List<Direccion> findByActivaTrueOrderByNombreAsc();

    List<Direccion> findByInstitucionId(Long institucionId);

    List<Direccion> findByInstitucionIsNull();

    boolean existsByInstitucionIsNull();

}
