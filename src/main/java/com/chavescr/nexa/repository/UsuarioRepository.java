package com.chavescr.nexa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

       Optional<Usuario> findByEmail(String email);

       Optional<Usuario> findByCedula(String cedula);

       Optional<Usuario> findByUsuario(String usuario);

       @Query("SELECT COUNT(u) FROM Usuario u JOIN u.direcciones i WHERE i.id = :direccionId")
       long countByDireccionId(@Param("direccionId") Long direccionId);

       boolean existsByEmail(String email);

       /** Para el CRUD global de Usuarios (admin sin dirección seleccionada): todos, con direcciones cargadas. */
       @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.direcciones ORDER BY u.nombre")
       List<Usuario> findAllWithDirecciones();

       @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.direcciones WHERE u.id = :id")
       Optional<Usuario> findByIdWithDirecciones(@Param("id") Long id);

       /**
        * Busca un usuario por email, nombre de usuario o cédula.
        * Usado por UserDetailsService para permitir login con cualquiera de los tres.
        */
       @Query("SELECT u FROM Usuario u WHERE " +
                     "LOWER(u.email)   = LOWER(:identifier) OR " +
                     "LOWER(u.usuario) = LOWER(:identifier) OR " +
                     "u.cedula         = :identifier")
       Optional<Usuario> findByIdentifier(@Param("identifier") String identifier);

       @Query("SELECT u FROM Usuario u WHERE " +
                     "LOWER(u.nombre) LIKE %:filtro% OR " +
                     "LOWER(u.email)  LIKE %:filtro%")
       List<Usuario> findByNombreOrEmail(@Param("filtro") String filtro);

       @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.direcciones WHERE u.email = :email")
       Optional<Usuario> findByEmailWithDirecciones(@Param("email") String email);

       /**
        * Igual que {@link #findByIdentifier}, pero con las direcciones cargadas.
        * Usado para resolver al usuario autenticado, cuyo "name" en el SecurityContext
        * es el identificador de login (puede ser email, usuario o cédula), no siempre el email.
        */
       @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.direcciones WHERE " +
                     "LOWER(u.email)   = LOWER(:identifier) OR " +
                     "LOWER(u.usuario) = LOWER(:identifier) OR " +
                     "u.cedula         = :identifier")
       Optional<Usuario> findByIdentifierWithDirecciones(@Param("identifier") String identifier);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i " +
                     "WHERE i.id = :direccionId AND u.activo = true ORDER BY u.nombre")
       List<Usuario> findActivosByDireccionId(@Param("direccionId") Long direccionId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r " +
                     "WHERE i.id = :direccionId AND u.activo = true AND r.nombre = :rolNombre ORDER BY u.nombre")
       List<Usuario> findActivosByDireccionIdAndRol(@Param("direccionId") Long direccionId,
                     @Param("rolNombre") String rolNombre);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r " +
                     "WHERE i.id = :direccionId AND u.activo = true AND r.nombre IN :rolNombres ORDER BY u.nombre")
       List<Usuario> findActivosByDireccionIdAndRolIn(@Param("direccionId") Long direccionId,
                     @Param("rolNombres") List<String> rolNombres);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i " +
                     "WHERE i.id = :direccionId ORDER BY u.nombre")
       List<Usuario> findAllByDireccionId(@Param("direccionId") Long direccionId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r " +
                     "WHERE i.id = :direccionId AND r.nombre = :rolNombre ORDER BY u.nombre")
       List<Usuario> findAllByDireccionIdAndRol(@Param("direccionId") Long direccionId,
                     @Param("rolNombre") String rolNombre);

       @Query("SELECT u FROM Usuario u JOIN u.direcciones i " +
                     "WHERE u.id = :usuarioId AND i.id = :direccionId AND u.activo = true")
       Optional<Usuario> findActivoByIdAndDireccionId(@Param("usuarioId") Long usuarioId,
                     @Param("direccionId") Long direccionId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r LEFT JOIN FETCH u.nivelAcademico "
                     + "WHERE u.id = :usuarioId AND i.id = :direccionId AND u.activo = true "
                     + "AND r.nombre = 'ROLE_ESTUDIANTE'")
       Optional<Usuario> findEstudianteActivoConNivel(@Param("usuarioId") Long usuarioId,
                     @Param("direccionId") Long direccionId);

       @Query("SELECT DISTINCT e FROM Usuario p JOIN p.estudiantes e WHERE p.id = :padreId ORDER BY e.nombre")
       List<Usuario> findEstudiantesByPadreId(@Param("padreId") Long padreId);

       @Query("SELECT DISTINCT p FROM Usuario e JOIN e.padres p WHERE e.id = :estudianteId ORDER BY p.nombre")
       List<Usuario> findPadresByEstudianteId(@Param("estudianteId") Long estudianteId);

       @Query("SELECT e.id, p FROM Usuario e JOIN e.padres p WHERE e.id IN :ids")
       List<Object[]> findPadresByEstudianteIds(@Param("ids") Collection<Long> ids);

       @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Usuario p JOIN p.estudiantes e " +
                     "WHERE p.id = :padreId AND e.id = :estudianteId")
       boolean existeVinculoPadreEstudiante(@Param("padreId") Long padreId, @Param("estudianteId") Long estudianteId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r " +
                     "WHERE u.cedula = :cedula AND i.id = :direccionId AND r.nombre = 'ROLE_PADRE'")
       Optional<Usuario> findPadreByCedulaAndDireccionId(@Param("cedula") String cedula,
                     @Param("direccionId") Long direccionId);

       @Query("SELECT u FROM Usuario u WHERE u.nivelAcademico.id = :nivelId AND u.activo = true ORDER BY u.nombre")
       List<Usuario> findEstudiantesActivosByNivelId(@Param("nivelId") Long nivelId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r LEFT JOIN FETCH u.nivelAcademico n "
                     + "WHERE i.id = :direccionId AND u.activo = true AND r.nombre = 'ROLE_ESTUDIANTE' "
                     + "AND (:nivelId IS NULL OR n.id = :nivelId) "
                     + "AND (:grado IS NULL OR n.grado = :grado) "
                     + "ORDER BY u.nombre")
       List<Usuario> findEstudiantesActivosConNivel(@Param("direccionId") Long direccionId,
                     @Param("grado") Integer grado, @Param("nivelId") Long nivelId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.direcciones i JOIN u.roles r LEFT JOIN FETCH u.nivelAcademico n "
                     + "WHERE i.id = :direccionId AND u.activo = true AND r.nombre = 'ROLE_ESTUDIANTE' "
                     + "AND n.id IN :nivelIds "
                     + "AND (:nivelId IS NULL OR n.id = :nivelId) "
                     + "AND (:grado IS NULL OR n.grado = :grado) "
                     + "ORDER BY u.nombre")
       List<Usuario> findEstudiantesActivosConNivelEn(@Param("direccionId") Long direccionId,
                     @Param("nivelIds") Collection<Long> nivelIds, @Param("grado") Integer grado,
                     @Param("nivelId") Long nivelId);

       List<Usuario> findByActivoTrueOrderByNombreAsc();
}
