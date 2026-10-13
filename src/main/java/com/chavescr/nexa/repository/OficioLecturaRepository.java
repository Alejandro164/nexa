package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.OficioLectura;

public interface OficioLecturaRepository extends JpaRepository<OficioLectura, Long> {

    /** Filas {oficioId, usuarioId, fecha} de las lecturas de esos oficios. */
    @Query("SELECT l.oficio.id, l.usuario.id, l.fecha FROM OficioLectura l WHERE l.oficio.id IN :oficioIds")
    List<Object[]> lecturasDe(@Param("oficioIds") Collection<Long> oficioIds);

    boolean existsByOficioIdAndUsuarioId(Long oficioId, Long usuarioId);
}
