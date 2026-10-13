package com.chavescr.nexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    Optional<Notificacion> findFirstByUsuarioIdAndEnlaceAndLeidaFalse(Long usuarioId, String enlace);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true "
            + "WHERE n.usuario.id = :usuarioId AND n.enlace = :enlace AND n.leida = false")
    int marcarLeidasPorEnlace(@Param("usuarioId") Long usuarioId, @Param("enlace") String enlace);
}
