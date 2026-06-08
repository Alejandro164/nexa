package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Institucion;

@Repository
public interface InstitucionRepository extends JpaRepository<Institucion, Long> {

    Optional<Institucion> findByCodigo(String codigo);

}
