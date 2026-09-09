package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.TipoAula;

public interface TipoAulaRepository extends JpaRepository<TipoAula, Long> {
    List<TipoAula> findByActivoTrueOrderByOrdenAscNombreAsc();

    Optional<TipoAula> findByCodigo(String codigo);

    Optional<TipoAula> findByNombreIgnoreCase(String nombre);
}
