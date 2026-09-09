package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TipoMateria;

public interface TipoMateriaRepository extends JpaRepository<TipoMateria, Long> {
    List<TipoMateria> findByActivoTrueOrderByOrdenAscNombreAsc();

    Optional<TipoMateria> findByCodigo(String codigo);

    Optional<TipoMateria> findByNombreIgnoreCase(String nombre);
}
