package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.DocenteBloqueoLeccion;

public interface DocenteBloqueoLeccionRepository extends JpaRepository<DocenteBloqueoLeccion, Long> {

    Optional<DocenteBloqueoLeccion> findByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long institucionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    List<DocenteBloqueoLeccion> findByInstitucionIdAndPeriodoIdAndDocenteId(
            Long institucionId, Long periodoId, Long docenteId);

    @Query("SELECT b.docente.id FROM DocenteBloqueoLeccion b "
            + "WHERE b.institucion.id = :institucionId AND b.periodo.id = :periodoId "
            + "AND b.dia = :dia AND b.numeroLeccion = :numeroLeccion")
    List<Long> findDocenteIdsBloqueados(
            @Param("institucionId") Long institucionId, @Param("periodoId") Long periodoId,
            @Param("dia") String dia, @Param("numeroLeccion") Integer numeroLeccion);

    boolean existsByInstitucionIdAndPeriodoIdAndDocenteIdAndDiaAndNumeroLeccion(
            Long institucionId, Long periodoId, Long docenteId, String dia, Integer numeroLeccion);

    void deleteByDocenteId(Long docenteId);

    void deleteByInstitucionIdAndPeriodoId(Long institucionId, Long periodoId);
}
