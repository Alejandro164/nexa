package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.ContactoExterno;

public interface ContactoExternoRepository extends JpaRepository<ContactoExterno, Long> {
    List<ContactoExterno> findByInstitucionIdAndActivoTrueOrderByNombreAsc(Long institucionId);

    Optional<ContactoExterno> findByIdAndInstitucionId(Long id, Long institucionId);
}
