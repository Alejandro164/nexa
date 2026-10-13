package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Conversacion;

@Repository
public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    /** Todas las conversaciones de la persona, de cualquiera de sus direcciones. */
    @Query("SELECT c FROM Conversacion c JOIN FETCH c.usuarioA JOIN FETCH c.usuarioB JOIN FETCH c.direccion "
            + "WHERE c.usuarioA.id = :usuarioId OR c.usuarioB.id = :usuarioId "
            + "ORDER BY c.fechaUltimoMensaje DESC, c.id DESC")
    List<Conversacion> listarDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT c FROM Conversacion c JOIN FETCH c.usuarioA JOIN FETCH c.usuarioB JOIN FETCH c.direccion "
            + "WHERE c.id = :id")
    Optional<Conversacion> findByIdConUsuarios(@Param("id") Long id);

    @Query("SELECT c FROM Conversacion c WHERE c.direccion.id = :direccionId "
            + "AND c.usuarioA.id = :usuarioAId AND c.usuarioB.id = :usuarioBId")
    Optional<Conversacion> buscarPar(@Param("direccionId") Long direccionId, @Param("usuarioAId") Long usuarioAId,
            @Param("usuarioBId") Long usuarioBId);
}
