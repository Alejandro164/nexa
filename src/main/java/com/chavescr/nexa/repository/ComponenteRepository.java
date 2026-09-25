package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;

public interface ComponenteRepository extends JpaRepository<Componente, Long> {

    List<Componente> findByDireccionIdAndClaveAndNivelIdAndMateriaIdOrderByIdAsc(
            Long direccionId, ClaveComponente clave, Long nivelId, Long materiaId);

    List<Componente> findByDireccionIdAndClaveAndNivelIdAndMateriaIdOrderByFechaAsc(
            Long direccionId, ClaveComponente clave, Long nivelId, Long materiaId);

    List<Componente> findByDireccionIdAndClaveAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
            Long direccionId, ClaveComponente clave, Long nivelId, Long materiaId, Long periodoId);

    Optional<Componente> findByIdAndDireccionId(Long id, Long direccionId);

    Optional<Componente> findByClaveAndOrigenId(ClaveComponente clave, Long origenId);
}
