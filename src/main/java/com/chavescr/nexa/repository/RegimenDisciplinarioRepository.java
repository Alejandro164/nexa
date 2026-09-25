package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.RegimenDisciplinario;

public interface RegimenDisciplinarioRepository extends JpaRepository<RegimenDisciplinario, Long> {

    List<RegimenDisciplinario> findByDireccionIdOrderByFechaDesc(Long direccionId);

    List<RegimenDisciplinario> findByDireccionIdAndTipoOrderByFechaDesc(Long direccionId, RegimenDisciplinario.TipoRegimen tipo);

    java.util.Optional<RegimenDisciplinario> findByIdAndDireccionId(Long id, Long direccionId);
}
