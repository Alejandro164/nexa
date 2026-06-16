package com.chavescr.nexa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Visita;

@Repository
public interface VisitaRepository extends JpaRepository<Visita, Long> {

    List<Visita> findByInstitucionIdOrderByFechaRegistroDesc(Long institucionId);

    List<Visita> findByInstitucionIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(
            Long institucionId, LocalDateTime inicio, LocalDateTime fin);

    List<Visita> findByIdentificacionAndInstitucionIdOrderByFechaRegistroDesc(
            String identificacion, Long institucionId);

    List<Visita> findByNombreVisitanteContainingIgnoreCaseAndInstitucionIdOrderByFechaRegistroDesc(
            String nombreVisitante, Long institucionId);

    @Query("SELECT v FROM Visita v WHERE v.institucion.id = :institucionId "
            + "AND (LOWER(v.nombreVisitante) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "OR v.identificacion = :filtro) "
            + "ORDER BY v.fechaRegistro DESC")
    List<Visita> buscarPorFiltro(@Param("filtro") String filtro, @Param("institucionId") Long institucionId);
}
