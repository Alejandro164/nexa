package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Direccion;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    Optional<Direccion> findByCodigo(String codigo);

    Optional<Direccion> findByCedula(String cedula);

    List<Direccion> findByActivaTrueOrderByNombreAsc();

    @Query("SELECT d FROM Direccion d LEFT JOIN FETCH d.institucion WHERE d.id = :id")
    Optional<Direccion> findByIdWithInstitucion(@Param("id") Long id);

    @Query("SELECT d FROM Direccion d LEFT JOIN FETCH d.institucion WHERE d.institucion.id = :institucionId AND d.activa = true")
    List<Direccion> findActivasByInstitucionId(@Param("institucionId") Long institucionId);

    List<Direccion> findByInstitucionId(Long institucionId);

    List<Direccion> findByInstitucionIsNull();

    boolean existsByInstitucionIsNull();

}
