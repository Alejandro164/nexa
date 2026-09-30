package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT c FROM Componente c
            JOIN FETCH c.materia
            JOIN FETCH c.nivel
            LEFT JOIN FETCH c.periodo
            WHERE c.direccion.id = :direccionId AND c.nivel.id IN :nivelIds
            """)
    List<Componente> findParaNotas(@Param("direccionId") Long direccionId,
            @Param("nivelIds") Collection<Long> nivelIds);
}
