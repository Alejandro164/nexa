package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.ClassroomClase;

@Repository
public interface ClassroomClaseRepository extends JpaRepository<ClassroomClase, Long> {

    Optional<ClassroomClase> findByDireccionIdAndMateriaIdAndNivelId(Long direccionId, Long materiaId, Long nivelId);

    Optional<ClassroomClase> findByIdAndDireccionId(Long id, Long direccionId);
}
