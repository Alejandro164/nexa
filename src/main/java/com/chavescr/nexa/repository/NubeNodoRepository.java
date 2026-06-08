package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.NubeNodo;

@Repository
public interface NubeNodoRepository extends JpaRepository<NubeNodo, Long> {

    // Find all nodes inside a specific parent folder
    List<NubeNodo> findByPadreIdOrderByTipoAscNombreAsc(Long padreId);

    // Find all root nodes (where parent is null)
    List<NubeNodo> findByPadreIsNullOrderByTipoAscNombreAsc();
}
