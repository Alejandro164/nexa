package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
