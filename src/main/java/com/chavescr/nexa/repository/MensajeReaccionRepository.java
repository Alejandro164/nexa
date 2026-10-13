package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.MensajeReaccion;

@Repository
public interface MensajeReaccionRepository extends JpaRepository<MensajeReaccion, Long> {

    @Query("SELECT r FROM MensajeReaccion r JOIN FETCH r.mensaje WHERE r.mensaje.id IN :ids ORDER BY r.id")
    List<MensajeReaccion> findByMensajeIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT r FROM MensajeReaccion r WHERE r.mensaje.id = :mensajeId AND r.usuario.id = :usuarioId "
            + "AND r.emoji = :emoji")
    Optional<MensajeReaccion> buscar(@Param("mensajeId") Long mensajeId, @Param("usuarioId") Long usuarioId,
            @Param("emoji") String emoji);

    @Query("SELECT COUNT(r) FROM MensajeReaccion r WHERE r.mensaje.conversacion.id = :conversacionId")
    long contarDeConversacion(@Param("conversacionId") Long conversacionId);

    @Query("SELECT COALESCE(MAX(r.id), 0) FROM MensajeReaccion r WHERE r.mensaje.conversacion.id = :conversacionId")
    long ultimoIdDeConversacion(@Param("conversacionId") Long conversacionId);
}
