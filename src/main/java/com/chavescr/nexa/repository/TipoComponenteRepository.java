package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.ClaveComponente;
import com.chavescr.nexa.entity.TipoComponente;

@Repository
public interface TipoComponenteRepository extends JpaRepository<TipoComponente, Long> {

    List<TipoComponente> findByDireccionIdOrderByOrdenAscNombreAsc(Long direccionId);

    Optional<TipoComponente> findByIdAndDireccionId(Long id, Long direccionId);

    Optional<TipoComponente> findByDireccionIdAndClave(Long direccionId, ClaveComponente clave);

    boolean existsByDireccionIdAndNombreIgnoreCase(Long direccionId, String nombre);

    boolean existsByDireccionIdAndNombreIgnoreCaseAndIdNot(Long direccionId, String nombre, Long id);
}
