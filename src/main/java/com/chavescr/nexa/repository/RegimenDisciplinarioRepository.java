package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.RegimenDisciplinario;

public interface RegimenDisciplinarioRepository extends JpaRepository<RegimenDisciplinario, Long> {

    List<RegimenDisciplinario> findByInstitucionIdOrderByFechaDesc(Long institucionId);

    List<RegimenDisciplinario> findByInstitucionIdAndTipoOrderByFechaDesc(Long institucionId, RegimenDisciplinario.TipoRegimen tipo);

    java.util.Optional<RegimenDisciplinario> findByIdAndInstitucionId(Long id, Long institucionId);
}
