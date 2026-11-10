package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TipoMateria;

public interface TipoMateriaRepository extends JpaRepository<TipoMateria, Long> {
    List<TipoMateria> findByDireccionIdOrderByOrdenAscNombreAsc(Long direccionId);

    List<TipoMateria> findByDireccionIdAndActivoTrueOrderByOrdenAscNombreAsc(Long direccionId);

    Optional<TipoMateria> findByIdAndDireccionId(Long id, Long direccionId);

    Optional<TipoMateria> findByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);

    boolean existsByDireccionId(Long direccionId);

    boolean existsByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);

    boolean existsByDireccionIdAndNombreIgnoreCaseAndIdNot(Long direccionId, String nombre, Long id);
}
