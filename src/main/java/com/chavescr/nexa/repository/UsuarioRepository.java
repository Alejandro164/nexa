package com.chavescr.nexa.repository;

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

       boolean existsByEmail(String email);

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

       @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.instituciones WHERE u.email = :email")
       Optional<Usuario> findByEmailWithInstituciones(@Param("email") String email);

       /**
        * Igual que {@link #findByIdentifier}, pero con las instituciones cargadas.
        * Usado para resolver al usuario autenticado, cuyo "name" en el SecurityContext
        * es el identificador de login (puede ser email, usuario o cédula), no siempre el email.
        */
       @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.instituciones WHERE " +
                     "LOWER(u.email)   = LOWER(:identifier) OR " +
                     "LOWER(u.usuario) = LOWER(:identifier) OR " +
                     "u.cedula         = :identifier")
       Optional<Usuario> findByIdentifierWithInstituciones(@Param("identifier") String identifier);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i " +
                     "WHERE i.id = :institucionId AND u.activo = true ORDER BY u.nombre")
       List<Usuario> findActivosByInstitucionId(@Param("institucionId") Long institucionId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i JOIN u.roles r " +
                     "WHERE i.id = :institucionId AND u.activo = true AND r.nombre = :rolNombre ORDER BY u.nombre")
       List<Usuario> findActivosByInstitucionIdAndRol(@Param("institucionId") Long institucionId,
                     @Param("rolNombre") String rolNombre);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i JOIN u.roles r " +
                     "WHERE i.id = :institucionId AND u.activo = true AND r.nombre IN :rolNombres ORDER BY u.nombre")
       List<Usuario> findActivosByInstitucionIdAndRolIn(@Param("institucionId") Long institucionId,
                     @Param("rolNombres") List<String> rolNombres);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i " +
                     "WHERE i.id = :institucionId ORDER BY u.nombre")
       List<Usuario> findAllByInstitucionId(@Param("institucionId") Long institucionId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i JOIN u.roles r " +
                     "WHERE i.id = :institucionId AND r.nombre = :rolNombre ORDER BY u.nombre")
       List<Usuario> findAllByInstitucionIdAndRol(@Param("institucionId") Long institucionId,
                     @Param("rolNombre") String rolNombre);

       @Query("SELECT u FROM Usuario u JOIN u.instituciones i " +
                     "WHERE u.id = :usuarioId AND i.id = :institucionId AND u.activo = true")
       Optional<Usuario> findActivoByIdAndInstitucionId(@Param("usuarioId") Long usuarioId,
                     @Param("institucionId") Long institucionId);

       @Query("SELECT DISTINCT e FROM Usuario p JOIN p.estudiantes e WHERE p.id = :padreId ORDER BY e.nombre")
       List<Usuario> findEstudiantesByPadreId(@Param("padreId") Long padreId);

       @Query("SELECT DISTINCT p FROM Usuario e JOIN e.padres p WHERE e.id = :estudianteId ORDER BY p.nombre")
       List<Usuario> findPadresByEstudianteId(@Param("estudianteId") Long estudianteId);

       @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Usuario p JOIN p.estudiantes e " +
                     "WHERE p.id = :padreId AND e.id = :estudianteId")
       boolean existeVinculoPadreEstudiante(@Param("padreId") Long padreId, @Param("estudianteId") Long estudianteId);

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i JOIN u.roles r " +
                     "WHERE u.cedula = :cedula AND i.id = :institucionId AND r.nombre = 'ROLE_PADRE'")
       Optional<Usuario> findPadreByCedulaAndInstitucionId(@Param("cedula") String cedula,
                     @Param("institucionId") Long institucionId);

       @Query("SELECT u FROM Usuario u WHERE u.nivelAcademico.id = :nivelId AND u.activo = true ORDER BY u.nombre")
       List<Usuario> findEstudiantesActivosByNivelId(@Param("nivelId") Long nivelId);
}
