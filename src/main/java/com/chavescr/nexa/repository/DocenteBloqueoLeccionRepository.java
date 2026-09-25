package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.DocenteBloqueoLeccion;

public interface DocenteBloqueoLeccionRepository extends JpaRepository<DocenteBloqueoLeccion, Long> {

    Optional<DocenteBloqueoLeccion> findByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long direccionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    List<DocenteBloqueoLeccion> findByDireccionIdAndPeriodoIdAndDocenteId(
            Long direccionId, Long periodoId, Long docenteId);

    @Query("SELECT b.docente.id FROM DocenteBloqueoLeccion b "
            + "WHERE b.direccion.id = :direccionId AND b.periodo.id = :periodoId "
            + "AND b.dia = :dia AND b.numeroLeccion = :numeroLeccion")
    List<Long> findDocenteIdsBloqueados(
            @Param("direccionId") Long direccionId, @Param("periodoId") Long periodoId,
            @Param("dia") String dia, @Param("numeroLeccion") Integer numeroLeccion);

    boolean existsByDireccionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long direccionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    boolean existsByDireccionIdAndNumeroLeccionGreaterThan(Long direccionId, Integer numeroLeccion);

    boolean existsByDireccionIdAndDia(Long direccionId, String dia);

    void deleteByDocenteId(Long docenteId);

    void deleteByDireccionIdAndPeriodoId(Long direccionId, Long periodoId);
}
