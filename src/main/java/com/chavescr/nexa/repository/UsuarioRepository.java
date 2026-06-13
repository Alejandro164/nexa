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

       @Query("SELECT DISTINCT u FROM Usuario u JOIN u.instituciones i " +
                     "WHERE i.id = :institucionId AND u.activo = true ORDER BY u.nombre")
       List<Usuario> findActivosByInstitucionId(@Param("institucionId") Long institucionId);

       @Query("SELECT u FROM Usuario u JOIN u.instituciones i " +
                     "WHERE u.id = :usuarioId AND i.id = :institucionId AND u.activo = true")
       Optional<Usuario> findActivoByIdAndInstitucionId(@Param("usuarioId") Long usuarioId,
                     @Param("institucionId") Long institucionId);
}
