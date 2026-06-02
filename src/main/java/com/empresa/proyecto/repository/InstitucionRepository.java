package com.empresa.proyecto.repository;

import com.empresa.proyecto.entity.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
    
    Optional<Institucion> findByCodigo(String codigo);
    
}
