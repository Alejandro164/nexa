package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Mensaje;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    /** Los más recientes primero; el servicio los invierte para mostrarlos en orden cronológico. */
    @Query("SELECT m FROM Mensaje m JOIN FETCH m.autor LEFT JOIN FETCH m.respuestaA "
            + "WHERE m.conversacion.id = :conversacionId ORDER BY m.id DESC")
    List<Mensaje> recientes(@Param("conversacionId") Long conversacionId, Pageable limite);

    @Query("SELECT COALESCE(MAX(m.id), 0) FROM Mensaje m WHERE m.conversacion.id = :conversacionId")
    long ultimoId(@Param("conversacionId") Long conversacionId);

    @Query("SELECT m FROM Mensaje m JOIN FETCH m.autor JOIN FETCH m.conversacion WHERE m.id IN "
            + "(SELECT MAX(m2.id) FROM Mensaje m2 WHERE m2.conversacion.id IN :ids GROUP BY m2.conversacion.id)")
    List<Mensaje> ultimosPorConversacion(@Param("ids") Collection<Long> ids);

    @Query("SELECT c.id, COUNT(m) FROM Mensaje m JOIN m.conversacion c "
            + "WHERE c.id IN :ids AND m.autor.id <> :usuarioId "
            + "AND ((c.usuarioA.id = :usuarioId AND m.id > c.ultimoLeidoA) "
            + "  OR (c.usuarioB.id = :usuarioId AND m.id > c.ultimoLeidoB)) "
            + "GROUP BY c.id")
    List<Object[]> contarNoLeidosPorConversacion(@Param("ids") Collection<Long> ids,
            @Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.conversacion.id = :conversacionId "
            + "AND m.autor.id <> :destinatarioId AND m.id > :desde")
    long contarPendientes(@Param("conversacionId") Long conversacionId,
            @Param("destinatarioId") Long destinatarioId, @Param("desde") Long desde);

    @Query("SELECT m FROM Mensaje m JOIN FETCH m.conversacion c JOIN FETCH c.usuarioA JOIN FETCH c.usuarioB "
            + "JOIN FETCH c.direccion WHERE m.id = :id")
    Optional<Mensaje> findByIdConConversacion(@Param("id") Long id);
}
