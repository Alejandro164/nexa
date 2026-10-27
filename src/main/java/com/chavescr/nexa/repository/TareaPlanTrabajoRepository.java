package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.TareaPlanTrabajo;

public interface TareaPlanTrabajoRepository extends JpaRepository<TareaPlanTrabajo, Long> {

    List<TareaPlanTrabajo> findByMiembroIdOrderByFechaLimiteAsc(Long miembroId);

    List<TareaPlanTrabajo> findByPlanTrabajoIdOrderByFechaLimiteAsc(Long planTrabajoId);

    Optional<TareaPlanTrabajo> findByIdAndPlanTrabajoId(Long id, Long planTrabajoId);

    void deleteByMiembroId(Long miembroId);

    @Query("SELECT t FROM TareaPlanTrabajo t JOIN t.planTrabajo p WHERE p.direccion.id = :direccionId ORDER BY t.fechaLimite ASC")
    List<TareaPlanTrabajo> findByDireccionIdOrderByFechaLimiteAsc(@Param("direccionId") Long direccionId);

    Optional<TareaPlanTrabajo> findByIdAndPlanTrabajo_Direccion_Id(Long id, Long direccionId);

    List<TareaPlanTrabajo> findByEtiquetas_Id(Long etiquetaId);

    /** Tareas abiertas con un recordatorio configurado que todavía no se ha enviado. */
    @Query("SELECT t FROM TareaPlanTrabajo t WHERE t.recordatorio <> :ninguno AND t.recordatorioEnviado = false "
            + "AND t.estado <> :finalizado")
    List<TareaPlanTrabajo> findConRecordatorioPendiente(
            @Param("ninguno") TareaPlanTrabajo.RecordatorioTarea ninguno,
            @Param("finalizado") TareaPlanTrabajo.EstadoTarea finalizado);
}
