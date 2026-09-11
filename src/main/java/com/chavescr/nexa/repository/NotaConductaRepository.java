package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.NotaConducta;

public interface NotaConductaRepository extends JpaRepository<NotaConducta, Long> {

    @Query("SELECT n.estudiante.id, n FROM NotaConducta n WHERE n.institucion.id = :institucionId "
            + "AND n.periodo.id = :periodoId AND n.estudiante.id IN :estudianteIds")
    List<Object[]> findDeEstudiantes(@Param("institucionId") Long institucionId,
            @Param("periodoId") Long periodoId, @Param("estudianteIds") Collection<Long> estudianteIds);

    Optional<NotaConducta> findByInstitucionIdAndPeriodoIdAndEstudianteId(
            Long institucionId, Long periodoId, Long estudianteId);

    @Query("SELECT n.estudiante.id FROM NotaConducta n WHERE n.institucion.id = :institucionId "
            + "AND n.periodo.id = :periodoId AND n.enviada = true AND n.estudiante.id IN :estudianteIds")
    List<Long> findEstudianteIdsEnviados(@Param("institucionId") Long institucionId,
            @Param("periodoId") Long periodoId, @Param("estudianteIds") Collection<Long> estudianteIds);
}
