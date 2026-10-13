package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chavescr.nexa.entity.Oficio;

public interface OficioRepository extends JpaRepository<Oficio, Long> {
    List<Oficio> findByDireccionIdOrderByFechaDesc(Long direccionId);

    Optional<Oficio> findByIdAndDireccionId(Long id, Long direccionId);

    long countByDireccionIdAndNumeroStartingWith(Long direccionId, String prefijo);

    /** Los oficios ya emitidos con todo lo que Comunicados necesita (destinatarios, redactor y documento). */
    @Query("SELECT DISTINCT o FROM Oficio o LEFT JOIN FETCH o.destinatarios d LEFT JOIN FETCH d.usuario "
            + "LEFT JOIN FETCH d.nivel LEFT JOIN FETCH o.redactadoPor LEFT JOIN FETCH o.nubeNodo "
            + "WHERE o.direccion.id = :direccionId AND o.estado = 'EMITIDO' ORDER BY o.fecha DESC, o.id DESC")
    List<Oficio> emitidosConDestinatarios(@Param("direccionId") Long direccionId);

    @Query("SELECT DISTINCT o FROM Oficio o LEFT JOIN FETCH o.destinatarios d LEFT JOIN FETCH d.usuario "
            + "LEFT JOIN FETCH d.nivel LEFT JOIN FETCH o.redactadoPor LEFT JOIN FETCH o.nubeNodo "
            + "WHERE o.id = :id AND o.direccion.id = :direccionId AND o.estado = 'EMITIDO'")
    Optional<Oficio> emitidoConDestinatarios(@Param("id") Long id, @Param("direccionId") Long direccionId);
}
