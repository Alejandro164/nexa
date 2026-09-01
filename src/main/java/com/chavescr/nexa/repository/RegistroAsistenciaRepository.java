package com.chavescr.nexa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.RegistroAsistencia;

@Repository
public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    List<RegistroAsistencia> findByInstitucionIdOrderByFechaHoraDesc(Long institucionId);

    List<RegistroAsistencia> findByInstitucionIdAndFechaHoraBetweenOrderByFechaHoraDesc(
            Long institucionId, LocalDateTime inicio, LocalDateTime fin);

    List<RegistroAsistencia> findByUsuarioIdAndInstitucionIdOrderByFechaHoraDesc(
            Long usuarioId, Long institucionId);

    /** Igual que el anterior, pero solo para usuarios con alguno de los roles dados (ej. staff, sin padres). */
    @Query("SELECT DISTINCT r FROM RegistroAsistencia r JOIN r.usuario u JOIN u.roles rol " +
            "WHERE r.institucion.id = :institucionId AND r.fechaHora BETWEEN :inicio AND :fin " +
            "AND rol.nombre IN :roles ORDER BY r.fechaHora DESC")
    List<RegistroAsistencia> findByInstitucionIdAndFechaHoraBetweenAndRolesOrderByFechaHoraDesc(
            @Param("institucionId") Long institucionId, @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin, @Param("roles") List<String> roles);
}
