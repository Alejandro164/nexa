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

    List<Visita> findByDireccionIdOrderByFechaRegistroDesc(Long direccionId);

    List<Visita> findByDireccionIdAndFechaRegistroBetweenOrderByFechaRegistroDesc(
            Long direccionId, LocalDateTime inicio, LocalDateTime fin);

    List<Visita> findByIdentificacionAndDireccionIdOrderByFechaRegistroDesc(
            String identificacion, Long direccionId);

    List<Visita> findByNombreVisitanteContainingIgnoreCaseAndDireccionIdOrderByFechaRegistroDesc(
            String nombreVisitante, Long direccionId);

    @Query("SELECT v FROM Visita v WHERE v.direccion.id = :direccionId "
            + "AND (LOWER(v.nombreVisitante) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "OR v.identificacion = :filtro) "
            + "ORDER BY v.fechaRegistro DESC")
    List<Visita> buscarPorFiltro(@Param("filtro") String filtro, @Param("direccionId") Long direccionId);
}
