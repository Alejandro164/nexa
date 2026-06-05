package com.empresa.proyecto.repository;

import com.empresa.proyecto.entity.NubeNodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NubeNodoRepository extends JpaRepository<NubeNodo, Long> {
    
    // Find all nodes inside a specific parent folder
    List<NubeNodo> findByPadreIdOrderByTipoAscNombreAsc(Long padreId);
    
    // Find all root nodes (where parent is null)
    List<NubeNodo> findByPadreIsNullOrderByTipoAscNombreAsc();
}
