package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.EntregaAcademicaDocente;

public interface EntregaAcademicaDocenteRepository extends JpaRepository<EntregaAcademicaDocente, Long> {

    Optional<EntregaAcademicaDocente> findByEntregaIdAndDocenteId(Long entregaId, Long docenteId);

    Optional<EntregaAcademicaDocente> findByIdAndEntrega_Direccion_Id(Long id, Long direccionId);

    List<EntregaAcademicaDocente> findByEntrega_Direccion_Id(Long direccionId);

    List<EntregaAcademicaDocente> findByEntrega_Direccion_IdAndDocenteId(Long direccionId, Long docenteId);

    List<EntregaAcademicaDocente> findByEntregaId(Long entregaId);

    boolean existsByEntregaId(Long entregaId);
}
