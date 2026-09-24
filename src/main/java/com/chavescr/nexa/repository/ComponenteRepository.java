package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.Componente;

public interface ComponenteRepository extends JpaRepository<Componente, Long> {

    List<Componente> findByInstitucionIdAndClaveAndNivelIdAndMateriaIdOrderByIdAsc(
            Long institucionId, ClaveComponente clave, Long nivelId, Long materiaId);

    List<Componente> findByInstitucionIdAndClaveAndNivelIdAndMateriaIdOrderByFechaAsc(
            Long institucionId, ClaveComponente clave, Long nivelId, Long materiaId);

    List<Componente> findByInstitucionIdAndClaveAndNivelIdAndMateriaIdAndPeriodoIdOrderByIdAsc(
            Long institucionId, ClaveComponente clave, Long nivelId, Long materiaId, Long periodoId);

    Optional<Componente> findByIdAndInstitucionId(Long id, Long institucionId);

    Optional<Componente> findByClaveAndOrigenId(ClaveComponente clave, Long origenId);
}
